package cc.aoeiuv020.panovel.server

import android.content.Context
import androidx.annotation.WorkerThread
import cc.aoeiuv020.base.jar.notZero
import cc.aoeiuv020.panovel.App
import com.google.gson.JsonParser
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.server.common.bookId
import cc.aoeiuv020.panovel.server.dal.model.Config
import cc.aoeiuv020.panovel.server.dal.model.Message
import cc.aoeiuv020.panovel.server.dal.model.QueryResponse
import cc.aoeiuv020.panovel.server.dal.model.autogen.Novel
import cc.aoeiuv020.panovel.server.service.NovelService
import cc.aoeiuv020.panovel.server.service.impl.NovelServiceImpl
import cc.aoeiuv020.panovel.settings.ServerSettings
import cc.aoeiuv020.panovel.util.*
import kotlinx.coroutines.*
import timber.log.Timber

/**
 *
 * Created by AoEiuV020 on 2018.04.06-02:37:52.
 */
object ServerManager  {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var novelService: NovelService? = null
    private var outOfVersion: Boolean = false
    private var disabled: Boolean = false
    var config: Config? = null

    fun downloadUpdate(ctx: Context, extra: String) {
        Timber.d("downloadUpdate $extra")
        scope.launch {
            try {
                val (localNovel, hasUpdate) = withContext(Dispatchers.IO) {
                    val remoteNovel: Novel = run {
                        val obj = JsonParser.parseString(extra).asJsonObject
                        val novelElement = obj.get("novel")
                        val novelStr = if (novelElement.isJsonPrimitive) novelElement.asString else novelElement.toString()
                        App.gson.fromJson(novelStr, Novel::class.java)
                    }
                    requireNotNull(remoteNovel.site)
                    requireNotNull(remoteNovel.author)
                    requireNotNull(remoteNovel.name)
                    requireNotNull(remoteNovel.detail)
                    requireNotNull(remoteNovel.chaptersCount)
                    DataManager.receiveUpdate(remoteNovel)
                }
                if (!hasUpdate || !ServerSettings.notifyNovelUpdate) {
                    // 没有更新或者不通知更新就不继续，
                    return@launch
                }
                Timber.d("notifyPinnedOnly: ${ServerSettings.notifyPinnedOnly}")
                Timber.d("pinnedTime: ${localNovel.pinnedTime}")
                Timber.d("pinnedTime.notZero: ${localNovel.pinnedTime.notZero()}")
                if (ServerSettings.notifyPinnedOnly && localNovel.pinnedTime.notZero() == null) {
                    return@launch
                }
                Timber.d("notify update: $localNovel")
                if (ServerSettings.singleNotification) {
                    val bitText = withContext(Dispatchers.IO) {
                        DataManager.hasUpdateNovelList()
                                .joinToString("\n") {
                                    it.run { "$name: $lastChapterName" }
                                }
                    }
                    ctx.notify(id = 2,
                            text = localNovel.lastChapterName,
                            title = ctx.getString(R.string.notify_has_update_title_placeholder, localNovel.name),
                            bigText = bitText,
                            time = localNovel.receiveUpdateTime.notZero()?.time,
                            channelId = NotificationChannelId.update)
                } else {
                    ctx.notify(id = localNovel.nId.toInt(),
                            text = localNovel.lastChapterName,
                            title = ctx.getString(R.string.notify_has_update_title_placeholder, localNovel.name),
                            time = localNovel.receiveUpdateTime.notZero()?.time,
                            channelId = NotificationChannelId.update)
                }
            } catch (e: Exception) {
                val message = "更新通知解析失败,"
                Reporter.post(message, e)
                Timber.e(e, message)
            }
        }
    }

    fun queryList(novelMap: Map<Long, Novel>): Map<Long, QueryResponse> {
        Timber.d("queryList ：${novelMap.map { "${it.key}=${it.value.bookId}" }}")
        val service = getService() ?: return emptyMap()
        return service.queryList(novelMap).also {
            Timber.d("查询小说更新返回: $it")
        }
    }

    fun touch(novel: Novel) {
        // 意义不大，禁用了，
    }

    fun message(): Message? {
        Timber.d("message ：")
        return try {
            DnsUtils.txtToBean(ServerAddress.MESSAGE_HOST)
        } catch (e: Exception) {
            Timber.w("get message failed: ${ServerAddress.MESSAGE_HOST}: $e")
            null
        }
    }

    @Synchronized
    @WorkerThread
    private fun getService(): NovelService? {
        Timber.d("getService <$novelService, $outOfVersion>")
        // 已经创建就直接返回，
        novelService?.let { return it }
        // 如果版本过低，直接返回空，不继续，
        if (outOfVersion) return null
        // 暂时禁用了，
        if (disabled) return null

        val config: Config
        try {
            config = DnsUtils.txtToBean(ServerAddress.CONFIG_HOST)
        } catch (e: Exception) {
            Timber.w("get config failed: ${ServerAddress.CONFIG_HOST}: $e")
            disabled = true
            return null
        }
        this.config = config

        val apiUrl: String = config.apiUrl.takeIf { !it.isNullOrEmpty() } ?: run {
            disabled = true
            return null
        }
        val minVersion = VersionName(config.minVersion)
        val currentVersion = VersionName(VersionUtil.getAppVersionName(App.ctx))
        Timber.i("getService minVersion $minVersion/$currentVersion")
        if (currentVersion < minVersion) {
            // 如果版本过低，直接返回空，不继续，
            outOfVersion = true
            return null
        }
        val serverAddress =
            ServerAddress.new(ServerSettings.serverAddress.takeIf { !it.isNullOrEmpty() } ?: apiUrl)
        Timber.i("server: " + serverAddress.baseUrl)
        return NovelServiceImpl(serverAddress)
    }
}
