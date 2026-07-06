package cc.aoeiuv020.reader

import android.content.Context
import android.view.ViewGroup
import cc.aoeiuv020.pager.Pager
import kotlinx.coroutines.*

class Reader(override var context: Context, novel: String, private val parent: ViewGroup, requester: TextRequester, override var config: ReaderConfig)
    : BaseNovelReader(novel, requester) {
    private val pageView: Pager = Pager(context)
    private val drawer = ReaderDrawer(this, novel, requester)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoRefreshJob: Job? = null
    override val maxTextProgress: Int
        get() = drawer.pagesCache[currentChapter]?.lastIndex ?: 0
    override var currentChapter: Int
        get() = drawer.chapterIndex
        set(value) {
            drawer.apply {
                chapterIndex = value
                pageIndex = 0
                pager?.refresh()
            }
        }
    override var textProgress: Int
        get() = drawer.pageIndex
        set(value) {
            drawer.apply {
                pageIndex = value
                pager?.refresh()
            }
        }

    init {
        pageView.centerPercent = config.centerPercent
        pageView.animDurationMultiply = config.animationSpeed
        pageView.bgColor = config.backgroundColor
        pageView.animMode = config.animationMode.toAnimMode()
        pageView.margins = config.contentMargins.toIMargins()
        pageView.drawer = drawer
        pageView.actionListener = object : Pager.ActionListener {
            override fun onCenterClick() {
                menuListener?.toggle()
            }

            override fun onPagePrev() {
                menuListener?.hide()
            }

            override fun onPageNext() {
                menuListener?.hide()
            }

        }
        parent.addView(pageView)

        startAutoRefresh()
    }

    private var autoRefreshLeftTime = config.autoRefreshInterval

    fun resetAutoRefresh() {
        autoRefreshLeftTime = config.autoRefreshInterval
    }

    private fun startAutoRefresh() {
        if (config.autoRefreshInterval == 0) return
        autoRefreshJob = scope.launch {
            while (isActive) {
                delay(1000)
                autoRefreshLeftTime--
                if (autoRefreshLeftTime == 0) {
                    drawer.pager?.refresh()
                    resetAutoRefresh()
                }
            }
        }
    }

    override fun refreshCurrentChapter(onComplete: (success: Boolean) -> Unit): Job? {
        return drawer.refreshCurrentChapter(onComplete)
    }

    override fun scrollNext(): Boolean = pageView.scrollNext()
    override fun scrollPrev(): Boolean = pageView.scrollPrev()

    override fun destroy() {
        scope.cancel()
        pageView.drawer.detach()
        parent.removeView(pageView)
    }
}