package cc.aoeiuv020.panovel.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cc.aoeiuv020.panovel.BuildConfig
import cc.aoeiuv020.panovel.R
import android.content.Intent
import cc.aoeiuv020.panovel.main.MainActivity
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

object NotificationChannelId {
    const val default = BuildConfig.APPLICATION_ID + ".default"
    const val update = BuildConfig.APPLICATION_ID + ".update"
    const val download = BuildConfig.APPLICATION_ID + ".download"
    const val downloading = BuildConfig.APPLICATION_ID + ".downloading"
    const val export = BuildConfig.APPLICATION_ID + ".export"
}

/**
 * 初始化项目中用到的通知渠道，
 */
fun Context.initNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return
    }
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(
            NotificationChannel(
                    NotificationChannelId.default,
                    getString(R.string.channel_name_default),
                    NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = getString(R.string.channel_description_default)
            }
    )
    notificationManager.createNotificationChannel(
            NotificationChannel(
                    NotificationChannelId.update,
                    getString(R.string.channel_name_update),
                    NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = getString(R.string.channel_description_update)
            }
    )
    notificationManager.createNotificationChannel(
            NotificationChannel(
                    NotificationChannelId.download,
                    getString(R.string.channel_name_download),
                    NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = getString(R.string.channel_description_download)
            }
    )
    notificationManager.createNotificationChannel(
            NotificationChannel(
                    NotificationChannelId.downloading,
                    getString(R.string.channel_name_downloading),
                    NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = getString(R.string.channel_description_downloading)
            }
    )
    notificationManager.createNotificationChannel(
            NotificationChannel(
                    NotificationChannelId.export,
                    getString(R.string.channel_name_export),
                    NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = getString(R.string.channel_description_export)
            }
    )
}

class NotifyLoopProxy(
        context: Context,
        private val id: Int = (Math.random() * Int.MAX_VALUE).toInt(),
        private val loopDelay: Long = 300L
) {
    companion object {
        val DEFAULT_CANCEL_DELAY: Long = TimeUnit.SECONDS.toMillis(5)
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val manager by lazy { NotificationManagerCompat.from(context) }

    private var waiting = false
    private var done = false
    private var canceled = false
    private var mNotification: Notification? = null
    private var loopJob: Job? = null
    private var cancelJob: Job? = null

    private fun notifyCached() {
        val notification = mNotification
        mNotification = null
        notification?.let { n ->
            manager.notify(id, n)
        }
    }

    fun start(notification: Notification) {
        done = false
        canceled = false
        cancelJob?.cancel()
        loopJob?.cancel()
        mNotification = notification
        notifyCached()
        waiting = true
        loopJob = scope.launch {
            delay(loopDelay)
            if (mNotification != null) {
                notifyCached()
            }
            if (canceled) {
                cancelJob = scope.launch {
                    delay(DEFAULT_CANCEL_DELAY)
                    if (canceled) manager.cancel(id)
                }
            }
            waiting = false
        }
    }

    fun modify(notification: Notification) {
        if (done) return
        mNotification = notification
        if (!waiting) {
            notifyCached()
            waiting = true
            loopJob = scope.launch {
                delay(loopDelay)
                if (mNotification != null) {
                    notifyCached()
                }
                if (canceled) {
                    cancelJob = scope.launch {
                        delay(DEFAULT_CANCEL_DELAY)
                        if (canceled) manager.cancel(id)
                    }
                }
                waiting = false
            }
        }
    }

    fun complete(notification: Notification) {
        done = true
        mNotification = notification
        if (!waiting) {
            notifyCached()
        }
    }

    fun cancel(cancelDelay: Long = DEFAULT_CANCEL_DELAY) {
        if (canceled) return
        canceled = true
        if (!waiting) {
            cancelJob = scope.launch {
                delay(cancelDelay)
                if (canceled) manager.cancel(id)
            }
        }
    }

    fun error() {
        done = true
        waiting = false
        loopJob?.cancel()
    }
}

fun Context.notify(id: Int, text: String? = null, title: String? = null, icon: Int = R.mipmap.ic_launcher_foreground, time: Long? = null, bigText: String? = null, channelId: String = NotificationChannelId.default) {
    val intent = Intent(this, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
    bigText?.let {
        notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(it))
    }
    time?.let {
        notificationBuilder.setWhen(it)
    }
    notificationBuilder.apply {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setLargeIcon(getBitmapFromVectorDrawable(icon))
            setSmallIcon(R.mipmap.ic_launcher_round)
        } else {
            setSmallIcon(icon)
        }
    }
    val manager = NotificationManagerCompat.from(this)
    manager.notify(id, notificationBuilder.build())
}

fun Context.cancelNotify(id: Int) {
    val manager = NotificationManagerCompat.from(this)
    manager.cancel(id)
}

fun Context.cancelAllNotify() {
    val manager = NotificationManagerCompat.from(this)
    manager.cancelAll()
}
