package cc.aoeiuv020.panovel.localbook

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import cc.aoeiuv020.panovel.bookfile.ContentProvider
import cc.aoeiuv020.panovel.bookfile.EpubExporter
import cc.aoeiuv020.panovel.bookfile.LocalNovelChapter
import cc.aoeiuv020.panovel.bookfile.LocalNovelInfo
import cc.aoeiuv020.panovel.bookfile.LocalNovelType
import cc.aoeiuv020.panovel.bookfile.TextExporter
import cc.aoeiuv020.panovel.util.PrefContext
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.main.MainActivity
import cc.aoeiuv020.panovel.util.NotificationChannelId
import cc.aoeiuv020.panovel.util.NotifyLoopProxy
import cc.aoeiuv020.panovel.util.notNullOrReport
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import android.content.Intent
import timber.log.Timber
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.nio.charset.Charset

class NovelExporter(
        private val type: LocalNovelType,
        private val charset: Charset,
        private val out: OutputStream,
        private val progressCallback: (Int, Int) -> Unit
)  {
    companion object {
        /**
         * 导出文件的默认文件名，供调用方在弹出系统"另存为"时预填，
         * 本地小说的site就是后缀，不要重复了，
         */
        fun fileName(novel: Novel, type: LocalNovelType): String = if (novel.site.startsWith(".")) {
            novel.run { "$name.$author${type.suffix}" }
        } else {
            novel.run { "$name.$author.$site${type.suffix}" }
        }

        /**
         * 导出到用户通过Storage Access Framework选择的[target], 免存储权限，
         */
        fun export(context: Context, type: LocalNovelType, charset: Charset, novelManager: NovelManager, target: Uri) {
            val novel = novelManager.novel
            val displayName = fileName(novel, type)
            // 太早了Intent不能用，<-- 我也不知道这在说什么，
            val notificationBuilder: NotificationCompat.Builder by lazy {
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
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
            val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            scope.launch {
                notificationBuilder.setProgress(0, 0, true)
                proxy.start(notificationBuilder.build())
            }
            val output = context.contentResolver.openOutputStream(target)
                    ?: throw IOException("无法打开导出目标，")
            output.use { stream ->
                NovelExporter(type, charset, stream) { current, total ->
                    Timber.d("exporting $current/$total")
                    scope.launch {
                        if (current == total) {
                            notificationBuilder.setContentTitle(context.getString(R.string.export_title_complete_placeholder, novel.name))
                            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.export_complete_big_placeholder, displayName)))
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
            LocalNovelType.TEXT -> TextExporter(out, charset)
            LocalNovelType.EPUB -> EpubExporter(out)
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
                    Glide.with(PrefContext.appContext)
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
