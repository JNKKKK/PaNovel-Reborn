package cc.aoeiuv020.panovel.data

import android.content.Context
import android.view.View
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.download.DownloadNotificationManager
import cc.aoeiuv020.panovel.download.DownloadService
import cc.aoeiuv020.panovel.download.DownloadingNotificationManager
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.settings.DownloadSettings
import cc.aoeiuv020.panovel.util.applyNeutralSurface
import cc.aoeiuv020.panovel.util.safelyShow
import android.widget.EditText
import android.widget.RadioGroup
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import timber.log.Timber

class DownloadManager(
        private val context: Context
)  {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 正在进行的下载任务数，用来控制前台服务的启停，
    // 第一个任务开始时启动前台服务，全部完成后停止，
    private val activeDownloads = AtomicInteger(0)

    val dnmLocal = object : ThreadLocal<DownloadingNotificationManager>() {
        override fun initialValue(): DownloadingNotificationManager {
            return DownloadingNotificationManager(context)
        }
    }

    fun downloadAll(list: List<NovelManager>) {
        // 一本一本顺序下载，避免多本并发把站点请求量翻倍、绕过下载限速间隔，
        scope.launch {
            withForegroundService {
                for (novelManager in list) {
                    Timber.d("downloadAll ${novelManager.novel.name}")
                    downloadSuspending(novelManager, 0, Int.MAX_VALUE)
                }
            }
        }
    }

    fun download(novelManager: NovelManager, fromIndex: Int, count: Int) {
        scope.launch {
            withForegroundService {
                downloadSuspending(novelManager, fromIndex, count)
            }
        }
    }

    /**
     * 在下载期间保持前台服务运行，避免锁屏/切后台被系统限制而暂停下载，
     * 支持并发的多个下载任务，最后一个结束时才停止服务，
     * 只在 scope 的 Dispatchers.Main 上调用，计数与启停都是单线程串行的，
     */
    private inline fun withForegroundService(block: () -> Unit) {
        // 先进 try 再自增，保证自增和 finally 里的自减一一对应，不会因异常泄漏计数，
        try {
            if (activeDownloads.getAndIncrement() == 0) {
                DownloadService.start(context)
            }
            block()
        } finally {
            if (activeDownloads.decrementAndGet() == 0) {
                DownloadService.stop(context)
            }
        }
    }

    private suspend fun downloadSuspending(novelManager: NovelManager, fromIndex: Int, count: Int) {
        if (count <= 0) return
        val novel = novelManager.novel
        run {
            try {
                withContext(Dispatchers.IO) {
                    val chapters = novelManager.requestChapters(false)
                    val cachedList = novelManager.novelContentsCached()
                    val size = chapters.size
                    val last = minOf(size - fromIndex, count) + fromIndex
                    var exists = 0
                    var downloads = 0
                    var errors = 0
                    val left = AtomicInteger(last - fromIndex)
                    if (left.get() <= 0) return@withContext
                    val nextIndex = AtomicInteger(fromIndex)
                    val threadsLimit = maxOf(1, DownloadSettings.downloadThreadsLimit)
                    Timber.d("download start <$fromIndex/$size> * $threadsLimit")
                    val downloadNotification = DownloadNotificationManager(context, novel)
                    withContext(Dispatchers.Main) {
                        downloadNotification.downloadStart(left.get())
                    }
                    val rateMutex = Mutex()
                    var lastRequestTime = 0L
                    val interval = DownloadSettings.downloadInterval.toLong()
                    // 同时启动多个线程下载，
                    // 判断一下，线程数不要过多，
                    val jobs = List(minOf(threadsLimit, left.get())) {
                        async(Dispatchers.IO) {
                            val thread = Thread.currentThread().name
                            // 每次循环最后再获取，
                            var index = nextIndex.getAndIncrement()
                            // 如果presenter已经detach说明离开了这个页面，不继续下载，
                            // 正在下载的章节不中断，
                            // 上面判断过，线程数不会过多，一进来index会小于size,
                            while (index < last) {
                                Timber.d("$thread downloading $index")
                                val chapter = chapters[index]
                                if (cachedList.contains(chapter.extra)) {
                                    ++exists
                                } else {
                                    try {
                                        rateMutex.withLock {
                                            if (interval > 0) {
                                                val elapsed = System.currentTimeMillis() - lastRequestTime
                                                if (elapsed < interval) delay(interval - elapsed)
                                            }
                                            novelManager.requestContent(index, chapter, false)
                                            lastRequestTime = System.currentTimeMillis()
                                        }
                                        ++downloads
                                    } catch (e: Exception) {
                                        val message = "缓存<${novel.bookId}.$index>章节失败，"
                                        Reporter.post(message, e)
                                        Timber.e(e, message)
                                        ++errors
                                    }
                                }
                                val tmpLeft = left.decrementAndGet()
                                withContext(Dispatchers.Main) {
                                    Timber.d("download $index, left $tmpLeft")
                                    downloadNotification.downloading(exists, downloads, errors, tmpLeft)
                                }
                                index = nextIndex.getAndIncrement()
                            }
                            withContext(Dispatchers.Main) {
                                downloadNotification.downloadComplete(exists, downloads, errors)
                                // 5秒后删除下载结果通知，
                                downloadNotification.cancelNotification(TimeUnit.SECONDS.toMillis(5))
                            }
                        }
                    }
                    jobs.forEach { it.await() }
                }
            } catch (e: Exception) {
                val message = "下载失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
            }
        }
    }

    // 不能用全局application的Context弹对话框，
    // WindowManager$BadTokenException: Unable to add window -- token null is not for an application
    fun askDownload(context: Context, novelManager: NovelManager, currentIndex: Int, fromFirst: Boolean): Boolean {
        val layout = View.inflate(context, R.layout.dialog_download_count, null)
        val countInput = layout.findViewById<EditText>(R.id.editText).apply {
            setText(DEFAULT_ASK_DOWNLOAD_COUNT.toString())
        }
        val fromRadioGroup = layout.findViewById<RadioGroup>(R.id.rgFrom)
        if (fromFirst) {
            fromRadioGroup.check(R.id.rbFromFirst)
        } else {
            fromRadioGroup.check(R.id.rbFromCurrent)
        }
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.cache_chapters_count)
            .setView(layout)
            .setNeutralButton(R.string.all) { _, _ ->
                val fromIndex = if (fromRadioGroup.checkedRadioButtonId == R.id.rbFromFirst) {
                    0
                } else {
                    currentIndex
                }
                download(novelManager, fromIndex, Int.MAX_VALUE)
            }
            .setPositiveButton(R.string.confirm) { _, _ ->
                val count = countInput.text.toString().toIntOrNull() ?: 0
                val realCount = if (count == 0) {
                    Int.MAX_VALUE
                } else {
                    count
                }
                val fromIndex = if (fromRadioGroup.checkedRadioButtonId == R.id.rbFromFirst) {
                    0
                } else {
                    currentIndex
                }
                download(novelManager, fromIndex, realCount)
            }
            .setNegativeButton(R.string.cancel, null)
            .create().apply { setOnShowListener { applyNeutralSurface() } }.safelyShow()
        return true
    }

    companion object {
        /**
         * 缓存对话框默认预填的章节数，
         */
        private const val DEFAULT_ASK_DOWNLOAD_COUNT = 50
    }

}
