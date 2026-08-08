package cc.aoeiuv020.panovel.data.availability

import android.content.Context
import cc.aoeiuv020.irondb.Iron
import cc.aoeiuv020.irondb.read
import cc.aoeiuv020.irondb.write

/**
 * 可用性历史的本地持久化，
 *
 * 数据是可再生的遥测（每天探测一次即可重建），不进 Room 也不进备份，
 * 用 IronDB 存整块 blob：
 * - 书源名里可能带 `/ : .` 等字符，IronDB 的 key 序列化会替换掉，无法作为独立 key，
 *   且 keysContainer 不支持遍历，所以整个历史 map 存成一个 key，
 * - 与 CacheManager/LocalManager 用 IronDB 存可再生数据的做法一致，
 */
class AvailabilityStore(context: Context) {
    private val db = Iron.db(context.filesDir.resolve(DIR))

    fun loadHistory(): Map<String, List<ProbeRecord>> =
        db.read<Map<String, List<ProbeRecord>>>(KEY_HISTORY) ?: emptyMap()

    fun saveHistory(history: Map<String, List<ProbeRecord>>) {
        db.write(KEY_HISTORY, history)
    }

    /** 上次成功探测的本地天序号，用于每天只探测一次，未探测过为 null， */
    fun loadLastProbeDay(): Long? = db.read<Long>(KEY_LAST_PROBE_DAY)

    fun saveLastProbeDay(epochDay: Long) {
        db.write(KEY_LAST_PROBE_DAY, epochDay)
    }

    companion object {
        private const val DIR = "availability"
        private const val KEY_HISTORY = "history"
        private const val KEY_LAST_PROBE_DAY = "lastProbeDay"
    }
}
