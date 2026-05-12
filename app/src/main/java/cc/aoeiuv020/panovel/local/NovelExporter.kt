package cc.aoeiuv020.panovel.local

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import cc.aoeiuv020.panovel.App
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.backup.BackupPresenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.main.MainActivity
import cc.aoeiuv020.panovel.settings.LocationSettings
import cc.aoeiuv020.panovel.util.NotificationChannelId
import cc.aoeiuv020.panovel.util.NotifyLoopProxy
import cc.aoeiuv020.panovel.util.notNullOrReport
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import android.content.Intent
import android.os.Handler
import android.os.Looper
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset

/**
 * Created by AoEiuV020 on 2018.05.28-18:53:01.
 */
class NovelExporter(
        private val type: LocalNovelType,
        private val charset: Charset,
        private val file: File,
        private val progressCallback: (Int, Int) -> Unit
)  {
    companion object {
        const val NAME_FOLDER = "Export"

        fun export(context: Context, type: LocalNovelType, charset: Charset, novelManager: NovelManager) {
            val novel = novelManager.novel
            // 本地小说的site就是后缀，不要重复了，
            val fileName = if (novel.site.startsWith(".")) {
                novel.run { "$name.$author${type.suffix}" }
            } else {
                novel.run { "$name.$author.$site${type.suffix}" }
            }
            // 尝试导出到sd卡，没有就导出到私有目录，虽然这样的导出好像没什么意义，
            val baseFile = File(LocationSettings.exportLocation)
                    .apply { exists() || mkdirs() }
                    .takeIf { it.canWrite() }
                    ?: context.filesDir
                            .resolve(BackupPresenter.NAME_FOLDER)
                            .apply { exists() || mkdirs() }
            val file = baseFile.resolve(fileName)
            // 太早了Intent不能用，<-- 我也不知道这在说什么，
            val notificationBuilder: NotificationCompat.Builder by lazy {
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
                val notificationBuilder = NotificationCompat.Builder(context, NotificationChannelId.export)
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true)
                        .setContentTitle(context.getString(R.string.exporting_title_placeholder, novel.name))
                        .setContentIntent(pendingIntent)
                notificationBuilder.apply {
                    setSmallIcon(android.R.drawable.stat_sys_download)
                }
                notificationBuilder
            }
            val proxy = NotifyLoopProxy(context)
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                notificationBuilder.setProgress(0, 0, true)
                proxy.start(notificationBuilder.build())
            }
            NovelExporter(type, charset, file) { current, total ->
                Timber.d("exporting $current/$total")
                mainHandler.post {
                    if (current == total) {
                        notificationBuilder.setContentTitle(context.getString(R.string.export_title_complete_placeholder, novel.name))
                        notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.export_complete_big_placeholder, file.path)))
                        notificationBuilder.setProgress(total, current, false)
                        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                        proxy.complete(notificationBuilder.build())
                    } else {
                        notificationBuilder.setProgress(total, current, false)
                        proxy.modify(notificationBuilder.build())
                    }
                }
            }.export(novelManager)
        }
    }

    fun export(novelManager: NovelManager) {
        val novel = novelManager.novel
        val info = LocalNovelInfo(
                author = novel.author,
                name = novel.name,
                image = novel.image,
                introduction = novel.introduction,
                chapters = novelManager.requestChapters(false).map {
                    LocalNovelChapter(name = it.name, extra = it.extra)
                },
                requester = novel.chapters
        )
        val exporter = when (type) {
            LocalNovelType.TEXT -> TextExporter(file, charset)
            LocalNovelType.EPUB -> EpubExporter(file)
        }
        val contentProvider = object : ContentProvider {
            val container = DataManager.novelContentsCached(novel)
            override fun getNovelContent(chapter: LocalNovelChapter): List<String> {
                return if (container.contains(chapter.extra)) {
                    // 判断过章节存在了，这个必须非空，除非导出过程删除了缓存，
                    novelManager.getContent(chapter.extra).notNullOrReport()
                } else {
                    listOf()
                }
            }

            override fun getImage(extra: String): URL {
                return novelManager.getImage(extra)
            }

            private fun URL.isHttp() = protocol.startsWith("http")

            override fun openImage(url: URL): InputStream? {
                return if (url.isHttp()) {
                    Glide.with(App.context)
                            .asFile()
                            .load(url.toString())
                            .apply(RequestOptions().onlyRetrieveFromCache(true))
                            .submit()
                            .get()
                            ?.inputStream()
                } else {
                    url.openStream()
                }

            }
        }
        exporter.export(info, contentProvider, progressCallback)
    }
}