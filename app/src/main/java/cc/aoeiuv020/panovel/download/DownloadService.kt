package cc.aoeiuv020.panovel.download

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.main.MainActivity
import cc.aoeiuv020.panovel.util.NotificationChannelId
import timber.log.Timber

/**
 * 下载期间常驻的前台服务，
 * 只负责把进程提升为前台，避免锁屏/切后台被系统限制（Doze/后台执行限制）而暂停下载，
 * 实际的下载逻辑仍在 [cc.aoeiuv020.panovel.data.DownloadManager]，
 * 由 DownloadManager 在有下载任务时启动、全部完成后停止，
 */
class DownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 无论什么指令都必须先调 startForeground，
        // 因为通过 startForegroundService 启动后，系统要求服务在 ~5 秒内进入前台，否则崩溃，
        // 即使是紧接着的停止指令，也要先进前台再停，避免下载秒完成时的启停竞态崩溃，
        startForegroundCompat()
        if (intent?.action == ACTION_STOP) {
            Timber.d("DownloadService stop")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            Timber.d("DownloadService start")
        }
        // 没有任务时不需要系统重建服务，DownloadManager 会在需要时重新启动，
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, NotificationChannelId.download)
                .setContentTitle(getString(R.string.download_service_running))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setOngoing(true)
                .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 0x0D010AD
        private const val ACTION_STOP = "cc.aoeiuv020.panovel.download.action.STOP"

        fun start(context: Context) {
            send(context, null)
        }

        fun stop(context: Context) {
            send(context, ACTION_STOP)
        }

        private fun send(context: Context, action: String?) {
            val intent = Intent(context, DownloadService::class.java).setAction(action)
            try {
                // 用 startForegroundService 而非 stopService：
                // 停止指令也走 onStartCommand，保证在 start 指令之后按顺序处理，
                // 服务总会先 startForeground 再停止，不会触发启停竞态崩溃，
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // Android 12+ 后台启动前台服务受限（ForegroundServiceStartNotAllowedException 同步抛出），
                // 失败时下载仍会继续（只是可能被系统限速），不影响功能，
                Timber.w(e, "startForegroundService for download failed, action=$action")
            }
        }
    }
}
