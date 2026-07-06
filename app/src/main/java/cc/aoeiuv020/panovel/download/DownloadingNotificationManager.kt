package cc.aoeiuv020.panovel.download

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.main.MainActivity
import cc.aoeiuv020.panovel.settings.DownloadSettings
import android.content.Intent
import cc.aoeiuv020.panovel.util.NotificationChannelId
import cc.aoeiuv020.panovel.util.NotifyLoopProxy

class DownloadingNotificationManager(
        private val context: Context
) {

    private val enable: Boolean get() = DownloadSettings.downloadThreadProgress
    private val proxy: NotifyLoopProxy = NotifyLoopProxy(context)
    // 太早了Intent不能用，
    private val notificationBuilder: NotificationCompat.Builder by lazy {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notificationBuilder = NotificationCompat.Builder(context, NotificationChannelId.downloading)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
        notificationBuilder.apply {
            setSmallIcon(android.R.drawable.stat_sys_download)
        }
        notificationBuilder
    }

    // 进度百分比，
    private fun progress(offset: Long, length: Long): Int = (if (length <= 0) {
        0f
    } else {
        offset.toFloat() / length
    } * 100).toInt()


    fun downloadStart(novel: Novel, index: Int, name: String) {
        notificationBuilder.setContentTitle(novel.name)
        val offset = 0L
        val length = 0L
        val progress = progress(offset, length)
        notificationBuilder.setContentText(context.getString(R.string.chapter_downloading_placeholder, index, name, offset, length))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, progress, false)
        if (enable) {
            proxy.start(notificationBuilder.build())
        } else {
            // 以防万一通知开始了不结束，
            cancelNotification()
        }
    }

    fun downloading(index: Int, name: String, offset: Long, length: Long) {
        // 更新数据，下次通知自己读取，
        val progress = progress(offset, length)
        notificationBuilder.setContentText(context.getString(R.string.chapter_downloading_placeholder, index, name, offset, length))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, progress, length <= 0)
        if (enable) {
            proxy.modify(notificationBuilder.build())
        }
    }

    fun downloadComplete(index: Int, name: String) {
        notificationBuilder.setContentText(context.getString(R.string.chapter_download_complete_placeholder, index, name))
                .setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
        if (enable) {
            proxy.complete(notificationBuilder.build())
        } else {
            cancelNotification()
        }
    }

    fun downloadError(index: Int, name: String, message: String) {
        notificationBuilder.setContentText(context.getString(R.string.chapter_download_error_placeholder, index, name, message))
                .setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
        if (enable) {
            proxy.complete(notificationBuilder.build())
        } else {
            cancelNotification()
        }
    }

    fun cancelNotification(cancelDelay: Long = NotifyLoopProxy.DEFAULT_CANCEL_DELAY) {
        proxy.cancel(cancelDelay)
    }

}
