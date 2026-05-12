package cc.aoeiuv020.reader.simple

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import cc.aoeiuv020.reader.R
import cc.aoeiuv020.reader.hide
import cc.aoeiuv020.reader.show
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.properties.Delegates

internal class PageHolder(private val reader: SimpleReader) {
    private val context = reader.context as Activity
    private val requester = reader.requester
    val itemView: View = View.inflate(context, R.layout.simple_view_pager_item, null)
    var position: Int = 0
    private val textRecyclerView: androidx.recyclerview.widget.RecyclerView = itemView.findViewById(R.id.textRecyclerView)
    private val layoutManager: androidx.recyclerview.widget.LinearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
    private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    val ntrAdapter = PageRecyclerAdapter(reader)
    private var textProgress: Int? = null
    private var index: Int by Delegates.notNull()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        textRecyclerView.layoutManager = layoutManager
        textRecyclerView.adapter = ntrAdapter
        textRecyclerView.apply {
            layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                setMargins(leftMargin,
                        reader.config.contentMargins.top.run { (toFloat() / 100 * this@PageHolder.context.window.decorView.height).toInt() },
                        rightMargin,
                        reader.config.contentMargins.bottom.run { (toFloat() / 100 * this@PageHolder.context.window.decorView.height).toInt() })
            }
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    reader.readingListener?.onReading(reader.currentChapter, getTextProgress())
                }
            })
        }
    }

    fun request(index: Int, refresh: Boolean = false) {
        this.index = index
        val chapter = reader.chapterList[index]
        progressBar.show()
        ntrAdapter.clear()
        ntrAdapter.setChapterName(chapter)
        scope.launch {
            try {
                val novelText = withContext(reader.ioDispatcher) {
                    requester.requestChapter(index, refresh)
                }
                showText(novelText)
            } catch (e: Exception) {
                val message = "获取小说文本失败，"
                Timber.e(e, message)
                showError(message, e)
            }
        }
    }

    fun destroy() {
        scope.cancel()
    }

    fun refresh() {
        request(index, true)
    }

    private fun showText(text: List<String>) {
        ntrAdapter.data = text
        textProgress?.let {
            textRecyclerView.run {
                post { scrollToPosition(it) }
            }
            textProgress = null
        }
        progressBar.hide()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun showError(message: String, e: Throwable) {
        progressBar.hide()
    }

    fun notifyMarginsChanged() {
        textRecyclerView.apply {
            post {
                layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                    setMargins(leftMargin,
                            reader.config.contentMargins.top.run { (toFloat() / 100 * itemView.height).toInt() },
                            rightMargin,
                            reader.config.contentMargins.bottom.run { (toFloat() / 100 * itemView.height).toInt() })
                }
            }
        }
    }

    fun setTextProgress(textProgress: Int) {
        Timber.d("setTextProgress $textProgress")
        this.textProgress = textProgress
        textRecyclerView.scrollToPosition(textProgress)
    }

    fun getTextProgress(): Int {
        return layoutManager.findLastVisibleItemPosition().also {
            Timber.d("getTextProgress $it")
        }
    }

    fun getTextCount(): Int = ntrAdapter.itemCount
}
