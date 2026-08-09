package cc.aoeiuv020.panovel.data.availability

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import cc.aoeiuv020.panovel.api.HomePageProbe
import cc.aoeiuv020.panovel.api.NovelContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * 书源可用性面板的数据来源，
 *
 * 模型：每个用户打开 app 的自然日采样一次，把每个书源首页的可达性存成一条历史记录，
 * 面板展示最近 14 个采样的绿/黄/红/灰竖条，数据全部来自本地，可再生（见 [AvailabilityStore]），
 *
 * 采样规则：
 * - 每次探测自带重试：判为不可用时会再试 [PROBE_ATTEMPTS] 次，仍失败才记为红（[ProbeStatus.FAIL]）。
 * - 当天首次（app 启动）对所有未隐藏书源各采一条，有两道闸：无网络不采、今天已采过不再全采。
 * - 当天已有采样后，后续 app 启动 / 每次打开书源页，会**只重探当天为红的书源**再给一次机会：
 *   若这次成功，把当天记录升级为黄（[ProbeStatus.RECOVERED]）并当天冻结不再重探；仍失败则保持红、
 *   下次继续有机会。黄和绿都不参与重探。
 * - 同一书源若已有探测在进行中，重复触发会跳过它（[inProgress] 去重）。
 */
class AvailabilityManager(
    context: Context,
    private val contextsProvider: () -> List<NovelContext>,
) {
    @SuppressLint("StaticFieldLeak")
    private val appContext = context.applicationContext
    private val store = AvailabilityStore(appContext)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 初始为空，构造后立刻在后台线程读盘并发布，避免 app 启动时在主线程反序列化，
    private val _history = MutableStateFlow<Map<String, List<ProbeRecord>>>(emptyMap())

    /** 书源名 -> 历史记录（旧到新），随探测完成而更新，供面板实时刷新， */
    val history: StateFlow<Map<String, List<ProbeRecord>>> = _history.asStateFlow()

    // 正在探测中的书源名，跨触发点去重，避免同一书源被并发重复探测，
    private val inProgress: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val probeMutex = Any()

    init {
        scope.launch {
            val loaded = store.loadHistory()
            // 只在还没有探测结果发布时才用磁盘快照播种，避免慢速读盘覆盖此间已完成的探测结果，
            synchronized(probeMutex) {
                if (_history.value.isEmpty()) {
                    _history.value = loaded
                }
            }
        }
    }

    /**
     * app 启动时调用：当天还没采过就全量采样一遍；已经采过则只重探当天为红的书源，
     */
    fun probeOnStartAsync() {
        scope.launch {
            try {
                val today = todayEpochDay()
                if (store.loadLastProbeDay() == today) {
                    reprobeTodayFailures(today)
                } else {
                    probeAll(today)
                }
            } catch (e: Exception) {
                Timber.e(e, "书源可用性探测失败")
            }
        }
    }

    /**
     * 打开书源页时调用：只重探当天为红的书源再给一次机会，
     */
    fun reprobeFailuresAsync() {
        scope.launch {
            try {
                reprobeTodayFailures(todayEpochDay())
            } catch (e: Exception) {
                Timber.e(e, "书源可用性重探失败")
            }
        }
    }

    /** 全量采样：给所有未隐藏书源各追加一条当天记录， */
    private suspend fun probeAll(today: Long) {
        if (!isNetworkAvailable()) {
            Timber.d("availability: no network, skip probe")
            return
        }
        val sites = contextsProvider().filter { !it.hide }
        Timber.d("availability: probing ${sites.size} sites for day $today")

        val probed = probeSites(sites, asReprobe = false)
        if (probed.isNotEmpty()) {
            updateHistory { merged ->
                probed.forEach { (name, status) ->
                    merged[name] = upsertToday(merged[name], today, status)
                }
            }
        }
        // 只有全量采样才推进"今天已采过"的标记，重探不改它，
        // 即使这轮没探到结果（书源全被并发去重跳过 / 无未隐藏书源）也要推进，否则当天每次启动都重入全量路径，
        store.saveLastProbeDay(today)
        Timber.d("availability: probe done, ${probed.size} results recorded")
    }

    /** 重探当天为红的书源，成功则升级为黄（RECOVERED）， */
    private suspend fun reprobeTodayFailures(today: Long) {
        if (!isNetworkAvailable()) {
            Timber.d("availability: no network, skip reprobe")
            return
        }
        val history = store.loadHistory()
        val reds = contextsProvider().filter { site ->
            !site.hide && history[site.site.name]?.lastOrNull()
                ?.let { it.epochDay == today && it.status == ProbeStatus.FAIL } == true
        }
        if (reds.isEmpty()) {
            Timber.d("availability: no red sites to reprobe for day $today")
            return
        }
        Timber.d("availability: reprobing ${reds.size} red sites for day $today")

        val probed = probeSites(reds, asReprobe = true)
        if (probed.isEmpty()) return

        updateHistory { merged ->
            probed.forEach { (name, status) ->
                // 重探只会把红升级为黄；仍失败就保持红不动，
                if (status == ProbeStatus.RECOVERED) {
                    merged[name] = upsertToday(merged[name], today, status)
                }
            }
        }
        Timber.d("availability: reprobe done, ${probed.count { it.second == ProbeStatus.RECOVERED }} recovered")
    }

    /**
     * 并发探测给定书源，带重试与"进行中"去重，返回实际探到结果的 (书源名, 状态) 列表，
     * 被去重跳过的书源不在返回列表里，
     * @param asReprobe true 时把探测成功记为 [ProbeStatus.RECOVERED]（黄），否则记为 [ProbeStatus.OK]（绿），
     */
    private suspend fun probeSites(
        sites: List<NovelContext>,
        asReprobe: Boolean,
    ): List<Pair<String, ProbeStatus>> {
        val semaphore = Semaphore(MAX_CONCURRENCY)
        return withContext(Dispatchers.IO) {
            sites.map { site ->
                async {
                    val name = site.site.name
                    // 已在探测中就跳过，避免并发触发重复探测同一书源，
                    if (!inProgress.add(name)) return@async null
                    try {
                        val ok = probeReachableWithRetry(site, semaphore)
                        val status = when {
                            !ok -> ProbeStatus.FAIL
                            asReprobe -> ProbeStatus.RECOVERED
                            else -> ProbeStatus.OK
                        }
                        name to status
                    } finally {
                        inProgress.remove(name)
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    /**
     * 探测一个书源首页是否可用，判为不可用时重试，共尝试 [PROBE_ATTEMPTS] 次，
     * 每次尝试各自持有一个并发许可，重试前的等待在许可之外进行，避免失败书源在 sleep 期间占着并发槽，
     * 确定性失败（被 Cloudflare 拦截 / 4xx 客户端错误）不重试——800ms 内不会有不同结果，白白多打一次请求，
     * @return true 表示可用（绿/黄的前提），false 表示重试后仍不可用，
     */
    private suspend fun probeReachableWithRetry(site: NovelContext, semaphore: Semaphore): Boolean {
        repeat(PROBE_ATTEMPTS) { attempt ->
            val probe = semaphore.withPermit { site.probeHomePage() }
            if (classify(probe) == ProbeStatus.OK) return true
            // 确定性失败重试无意义；只对可能是瞬时问题的失败（超时/5xx/空正文）再给一次机会，
            if (!isRetriable(probe)) return false
            if (attempt < PROBE_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        return false
    }

    /** 在磁盘历史基线上原子地改一批记录并发布，避免与构造时异步读盘竞争覆盖旧历史， */
    private fun updateHistory(mutate: (MutableMap<String, List<ProbeRecord>>) -> Unit) {
        synchronized(probeMutex) {
            val merged = store.loadHistory().toMutableMap()
            mutate(merged)
            store.saveHistory(merged)
            _history.value = merged
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        private const val MAX_CONCURRENCY = 6
        private const val MAX_RECORDS_PER_SITE = 30

        // 每次探测最多尝试次数（含首次），判为不可用才重试，
        private const val PROBE_ATTEMPTS = 2
        private const val RETRY_DELAY_MS = 800L
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

        /**
         * 把当天记录写入某书源历史：末条已是今天则替换（重探升级/覆盖），否则追加，
         * 追加后按 [MAX_RECORDS_PER_SITE] 截断，
         */
        internal fun upsertToday(
            records: List<ProbeRecord>?,
            today: Long,
            status: ProbeStatus,
        ): List<ProbeRecord> {
            val list = records.orEmpty()
            val record = ProbeRecord(today, status)
            val updated = if (list.lastOrNull()?.epochDay == today) {
                list.dropLast(1) + record
            } else {
                list + record
            }
            return updated.takeLast(MAX_RECORDS_PER_SITE)
        }

        /**
         * 本地时区下的天序号（1970-01-01 为 0）。minSdk 24 无 java.time 脱糖，用毫秒算，
         * 用当前时刻的时区偏移（含夏令时）把 UTC 毫秒挪到本地墙上时间再整除一天，
         */
        private fun todayEpochDay(): Long {
            val now = System.currentTimeMillis()
            val offset = TimeZone.getDefault().getOffset(now)
            return Math.floorDiv(now + offset, MILLIS_PER_DAY)
        }

        /**
         * 把一次探测的原始信号判定为可用 [ProbeStatus.OK] 或不可用 [ProbeStatus.FAIL]，
         * 暂不支持 Cloudflare，被 CF 拦截视同不可用，
         */
        fun classify(probe: HomePageProbe): ProbeStatus = when {
            !probe.reachable -> ProbeStatus.FAIL
            isCloudflareChallenge(probe) -> ProbeStatus.FAIL
            probe.code in 200..299 && probe.bodySnippet.isNotBlank() -> ProbeStatus.OK
            else -> ProbeStatus.FAIL
        }

        private fun isCloudflareChallenge(probe: HomePageProbe): Boolean {
            // cf-mitigated 头出现即表示被挑战/拦截，最可靠的信号，
            if (!probe.cfMitigated.isNullOrBlank()) return true
            // 其余启发式仅在 server 头确为 cloudflare 时才考虑：
            // 否则正常页面正文里恰好含 "Just a moment"/"challenge-platform" 等字样会被误判为拦截，
            val serverIsCf = probe.server?.contains("cloudflare", ignoreCase = true) == true
            if (!serverIsCf) return false
            if (probe.code == 403 || probe.code == 503) return true
            // 挑战页 body 特征——仅在 CF 前置时才作数，且用更专属的标记降低误伤，
            val body = probe.bodySnippet
            return body.contains("__cf_chl") ||
                body.contains("cf_chl_opt") ||
                body.contains("cf-browser-verification") ||
                body.contains("challenge-platform") ||
                (body.contains("Just a moment") && body.contains("challenge")) ||
                body.contains("Attention Required! | Cloudflare")
        }

        /**
         * 判断一次失败是否值得重试：只有可能是瞬时的失败才重试，确定性失败重试无意义，
         * - 不可达（DNS/TCP/TLS/超时）、5xx、2xx 但空正文：可能瞬时，重试，
         * - 被 Cloudflare 拦截、4xx 客户端错误：确定性失败，不重试，
         */
        private fun isRetriable(probe: HomePageProbe): Boolean {
            if (!probe.reachable) return true
            if (isCloudflareChallenge(probe)) return false
            if (probe.code in 400..499) return false
            return true
        }

        /**
         * 把某书源的历史记录换算成固定 [slots] 个状态（旧在左、新在右），
         * 不足的左侧补 UNKNOWN（灰条），超出的取最近 [slots] 条，
         */
        fun barsFor(records: List<ProbeRecord>?, slots: Int): List<ProbeStatus> {
            val recent = records.orEmpty().takeLast(slots).map { it.status }
            val padding = slots - recent.size
            return if (padding > 0) {
                List(padding) { ProbeStatus.UNKNOWN } + recent
            } else {
                recent
            }
        }
    }
}
