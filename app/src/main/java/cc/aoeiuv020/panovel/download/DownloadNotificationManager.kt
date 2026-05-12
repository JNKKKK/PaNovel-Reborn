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

class DownloadNotificationManager(
        private val context: Context,
        novel: Novel
) {

    private val enable: Boolean get() = DownloadSettings.downloadProgress
    private val proxy: NotifyLoopProxy = NotifyLoopProxy(context)
    // 太早了Intent不能用，
    private val notificationBuilder: NotificationCompat.Builder by lazy {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        val notificationBuilder = NotificationCompat.Builder(context, NotificationChannelId.download)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setContentTitle(novel.name)
                .setContentIntent(pendingIntent)
        notificationBuilder.apply {
            setSmallIcon(android.R.drawable.stat_sys_download)
        }
        notificationBuilder
    }

    fun downloadStart(left: Int) {
        val exists = 0
        val downloads = 0
        val errors = 0
        val progress = ((exists + downloads + errors).toFloat() / ((exists + downloads + errors) + left) * 100).toInt()
        notificationBuilder.setContentText(context.getString(R.string.downloading_placeholder, exists, downloads, errors, left))
                .setProgress(100, progress, false)
        if (enable) {
            proxy.start(notificationBuilder.build())
        } else {
            // 以防万一通知开始了不结束，
            cancelNotification()
        }
    }

    fun downloading(exists: Int, downloads: Int, errors: Int, left: Int) {
        val progress = ((exists + downloads + errors).toFloat() / ((exists + downloads + errors) + left) * 100).toInt()
        notificationBuilder.setContentText(context.getString(R.string.downloading_placeholder, exists, downloads, errors, left))
                .setProgress(100, progress, false)
        if (enable) {
            proxy.modify(notificationBuilder.build())
        } else {
            // 以防万一通知开始了不结束，
            cancelNotification()
        }
    }

    fun downloadComplete(exists: Int, downloads: Int, errors: Int) {
        notificationBuilder.setContentText(context.getString(R.string.download_complete_placeholder, exists, downloads, errors))
                .setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
        if (enable) {
            proxy.complete(notificationBuilder.build())
        } else {
            // 以防万一通知开始了不结束，
            cancelNotification()
        }
    }

    fun cancelNotification(cancelDelay: Long = NotifyLoopProxy.DEFAULT_CANCEL_DELAY) {
        proxy.cancel(cancelDelay)
    }

    @Suppress("UNUSED_PARAMETER")
    fun error(message: String, t: Throwable) {
        // 出意外了直接停止通知循环，
        proxy.error()
    }

}
