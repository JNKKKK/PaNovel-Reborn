package cc.aoeiuv020.panovel.data

import android.content.Context
import android.view.View
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.download.DownloadNotificationManager
import cc.aoeiuv020.panovel.download.DownloadingNotificationManager
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.settings.DownloadSettings
import cc.aoeiuv020.panovel.util.safelyShow
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import timber.log.Timber

/**
 * Created by AoEiuV020 on 2018.10.06-19:05:33.
 */
class DownloadManager(
        private val context: Context
)  {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val dnmLocal = object : ThreadLocal<DownloadingNotificationManager>() {
        override fun initialValue(): DownloadingNotificationManager {
            return DownloadingNotificationManager(context)
        }
    }

    fun downloadAll(list: List<NovelManager>) {
        // 一本一本顺序下载，避免多本并发把站点请求量翻倍、绕过下载限速间隔，
        scope.launch {
            for (novelManager in list) {
                Timber.d("downloadAll ${novelManager.novel.name}")
                downloadSuspending(novelManager, 0, Int.MAX_VALUE)
            }
        }
    }

    fun download(novelManager: NovelManager, fromIndex: Int, count: Int) {
        scope.launch {
            downloadSuspending(novelManager, fromIndex, count)
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
        val defaultCount = DownloadSettings.downloadCount.takeIf { it >= 0 }
                ?: 50
        val layout = View.inflate(context, R.layout.dialog_download_count, null)
        val countInput = layout.findViewById<EditText>(R.id.editText).apply {
            setText(defaultCount.toString())
        }
        val fromRadioGroup = layout.findViewById<RadioGroup>(R.id.rgFrom)
        if (fromFirst) {
            fromRadioGroup.check(R.id.rbFromFirst)
        } else {
            fromRadioGroup.check(R.id.rbFromCurrent)
        }
        val rememberCheckBox = layout.findViewById<CheckBox>(R.id.checkBox)
        fun remember() {
            if (rememberCheckBox.isChecked) {
                countInput.text.toString().toIntOrNull()?.let {
                    DownloadSettings.downloadCount = it
                }
            }
        }
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.download_chapters_count)
            .setView(layout)
            .setNeutralButton(R.string.all) { _, _ ->
                remember()
                val fromIndex = if (fromRadioGroup.checkedRadioButtonId == R.id.rbFromFirst) {
                    0
                } else {
                    currentIndex
                }
                download(novelManager, fromIndex, Int.MAX_VALUE)
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                remember()
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
            .setNegativeButton(android.R.string.cancel, null)
            .create().safelyShow()
        return true
    }

}
