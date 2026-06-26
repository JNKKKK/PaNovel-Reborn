package cc.aoeiuv020.reader

import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Build
import android.text.TextPaint
import android.util.TypedValue
import cc.aoeiuv020.pager.Pager
import cc.aoeiuv020.pager.BasePagerDrawer
import cc.aoeiuv020.pager.Size
import cc.aoeiuv020.reader.ReaderConfigName.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 *
 * Created by AoEiuV020 on 2017.12.03-04:09:17.
 */
@SuppressWarnings("SimpleDateFormat")
class ReaderDrawer(private val reader: Reader, private val novel: String, private val requester: TextRequester)
    : BasePagerDrawer() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val pagesCache: androidx.collection.LruCache<Int, List<Page>?> = androidx.collection.LruCache(8)
    private lateinit var titlePaint: TextPaint
    private lateinit var textPaint: TextPaint
    private lateinit var messagePaint: TextPaint
    private var backgroundImage: Bitmap? = null
    var chapterIndex = 0
    var pageIndex = 0
    private var sdf = try {
        SimpleDateFormat(reader.config.dateFormat)
    } catch (e: Exception) {
        SimpleDateFormat("HH:mm")
    }

    init {
        reader.config.listeners.add(object : ConfigChangedListener {
            private fun refresh() {
                reset()
                pager?.refresh()
            }

            override fun onConfigChanged(name: ReaderConfigName) {
                when (name) {
                    CenterPercent -> {
                        pager?.centerPercent = reader.config.centerPercent
                    }
                    Font -> {
                        textPaint.typeface = reader.config.font
                        titlePaint.typeface = reader.config.titleFont
                    }
                    AnimDurationMultiply -> {
                        pager?.animDurationMultiply = reader.config.animationSpeed
                    }
                    ReaderConfigName.AnimationMode -> {
                        pager?.animMode = reader.config.animationMode.toAnimMode()
                    }
                    BackgroundColor -> {
                        pager?.bgColor = reader.config.backgroundColor
                    }
                    ContentMargins -> {
                        pager?.margins = reader.config.contentMargins.toIMargins()
                    }
                    DateFormat -> {
                        // 这个不支持在阅读时改，到不了这里，
                        sdf = SimpleDateFormat(reader.config.dateFormat)
                    }
                    else -> {
                        // 其他设置在refresh里都重置了，
                    }
                }
                refresh()
            }
        })
    }

    override fun attach(pager: Pager, backgroundSize: Size, contentSize: Size) {
        super.attach(pager, backgroundSize, contentSize)

        reset()
    }

    private fun reset() {
        // 清空分页的缓存，
        pagesCache.evictAll()

        textPaint = TextPaint().apply {
            isAntiAlias = true
            color = reader.config.textColor
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, reader.config.textSize.toFloat(), reader.context.resources.displayMetrics)
            typeface = reader.config.font
        }
        titlePaint = TextPaint(textPaint).apply {
            typeface = reader.config.titleFont
        }
        backgroundImage = reader.config.backgroundImage?.let {
            try {
                BitmapFactory.decodeStream(reader.context.contentResolver.openInputStream(it))
            } catch (e: FileNotFoundException) {
                // 以防万一，说不定uri存在但是文件已经不存在了，
                null
            }
        }
        messagePaint = TextPaint(textPaint).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, reader.config.messageSize.toFloat(), reader.context.resources.displayMetrics)
        }
    }

    override fun drawCurrentPage(background: Canvas, content: Canvas) {
        Timber.d("drawCurrentPage <$chapterIndex, $pageIndex>")

        drawBackground(background)

        if (pager == null) {
            Timber.w("pager is null")
            return
        }

        if (chapterIndex !in reader.chapterList.indices) {
            // 打开小说时pager初始化会先画一两次，那时chapterList还没设置，属正常，不是警告，
            Timber.d("chapter index out of bounds <$chapterIndex/${reader.chapterList.size}>")
            return
        }

        val pages = pagesCache[chapterIndex]

        val page = initPages(content, pages)

        if (reader.config.paginationMargins.enabled) {
            drawPagination(background, pages)
        }
        if (reader.config.chapterNameMargins.enabled) {
            drawMessage(background, reader.chapterList[chapterIndex], reader.config.chapterNameMargins)
        }
        if (reader.config.bookNameMargins.enabled) {
            drawMessage(background, novel, reader.config.bookNameMargins)
        }
        if (reader.config.timeMargins.enabled) {
            drawTime(background)
        }
        if (reader.config.batteryMargins.enabled) {
            drawBattery(background)
        }

        page?.let {
            drawContent(content, page)
        }
    }

    private fun initPages(content: Canvas, pages: List<Page>?): Page? {
        reader.resetAutoRefresh()
        // 只用本地变量，防止pageIndex被多线程修改，
        var index = pageIndex
        val textHeight = textPaint.getFontMetricsInt(null)
        if (pages == null) {
            Timber.d("chapter $chapterIndex pages null")
            drawTextBottom(content, "正在获取章节...", 0f, textHeight.toFloat(), textPaint)
            request(chapterIndex)
            return null
        }
        if (pages.isEmpty()) {
            Timber.d("chapter $chapterIndex pages empty")
            var y = textHeight
            drawTextBottom(content, "本章空内容，", 0f, y.toFloat(), textPaint)
            y += textHeight
            drawTextBottom(content, "网络问题？", 0f, y.toFloat(), textPaint)
            y += textHeight
            drawTextBottom(content, "试试刷新？", 0f, y.toFloat(), textPaint)
            return null
        }

        // 下面两个判断和上面重复，主要是没加锁，重复判断避免万一，
        // 调小字体有可能出现页数变少，
        if (index > pages.lastIndex) {
            index = pages.lastIndex
        }
        // 往上翻章节时pageIndex会是负数，表示倒数，
        // 虽然没必要，但是尽量让pageIndex无论什么数都加上pages.size的整数倍到pages范围内，
/*
        while (pageIndex < 0) {
            pageIndex += pages.size
        }
*/
        if (index < 0) {
            index -= index / pages.size * pages.size
        }
        if (index < 0) {
            index += pages.size
        }

        pageIndex = index
        return pages[index]
    }


    private fun drawTime(canvas: Canvas) {
        val text = sdf.format(Date())
        val margins = reader.config.timeMargins
        drawMessage(canvas, text, margins)
    }

    private fun drawBattery(canvas: Canvas) {
        val intent = reader.context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!
        val battery = intent.getIntExtra("level", 0)
        val text = "$battery"
        val margins = reader.config.batteryMargins
        drawMessage(canvas, text, margins, true)
    }

    private fun drawPagination(canvas: Canvas, pages: List<Page>?) {
        val max = pages?.size ?: 0
        // 负数表示倒数，保留，不+1,
        val index = if (pageIndex >= 0) pageIndex + 1 else pageIndex
        val margins = reader.config.paginationMargins
        val text = "$index/$max"
        drawMessage(canvas, text, margins)
    }

    /**
     * 左右取大的，上下取大的，小的无视，
     * 哪个大就贴哪个，
     * 刚好50是居中，
     */
    private fun drawMessage(canvas: Canvas, text: String, margins: ItemMargins, isBattery: Boolean = false) {
        val textHeight = messagePaint.getFontMetricsInt(null).toFloat()
        val textWidth = messagePaint.measureText(text)
        val x: Float = if (margins.left > margins.right) {
            if (margins.left == 50) {
                canvas.width / 2 - textWidth / 2
            } else {
                canvas.width * margins.left / 100f
            }
        } else {
            if (margins.right == 50) {
                canvas.width / 2 - textWidth / 2
            } else {
                canvas.width - canvas.width * margins.right / 100f - textWidth
            }
        }
        val y: Float = if (margins.top > margins.bottom) {
            if (margins.top == 50) {
                canvas.height / 2 + textHeight / 2
            } else {
                canvas.height * margins.top / 100f + textHeight
            }
        } else {
            if (margins.bottom == 50) {
                canvas.height / 2 + textHeight / 2
            } else {
                canvas.height - canvas.height * margins.bottom / 100f
            }
        }
        if (isBattery) {
            // 画框框，
            messagePaint.style = Paint.Style.STROKE
            messagePaint.strokeWidth = textHeight / 20
            canvas.drawRect(RectF(x, y - textHeight, x + textWidth, y), messagePaint)
            // 画电池头部那个小点，
            messagePaint.style = Paint.Style.FILL
            canvas.drawRect(RectF(x + textWidth, y - textHeight / 4 * 3, x + textWidth + textWidth / 15, y - textHeight / 4 * 1), messagePaint)

            val a = 0.1f
            val bX = x + textWidth * a
            val bY = y - textHeight * a
            val mSize = messagePaint.textSize
            val bSize = mSize * (1 - 2 * a)
            messagePaint.textSize = bSize
            drawTextBottom(canvas, text, bX, bY, messagePaint)
            messagePaint.textSize = mSize
        } else {
            drawTextBottom(canvas, text, x, y, messagePaint)
        }
    }

    private fun drawContent(content: Canvas, page: Page) {
        val textHeight = textPaint.getFontMetricsInt(null)
        val lineSpacing = (reader.config.lineSpacing * reader.context.resources.displayMetrics.density).toInt()

        val width = content.width.toFloat()
        var y = 0
        val paragraphSpacing = (reader.config.paragraphSpacing * reader.context.resources.displayMetrics.density).toInt()
        page.lines.forEach { line ->
            Timber.v("draw height $y/${content.height}")
            when (line) {
                is Title -> {
                    y += textHeight
                    drawTextBottom(content, line.string, 0f, y.toFloat(), titlePaint)
                    y += lineSpacing
                }
                is String -> {
                    y += textHeight
                    if (reader.config.fitWidth
                            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // 调整字间距只有安卓5以上支持，

                        val textWidth = textPaint.measureText(line)
                        // 空白小于一个字才调整字间距，也就是这行确实填満了字的情况，
                        if ((width - textWidth) < textHeight) {
                            // 间距转int下取整处理以免最右一个字超出部分，
                            val spacing = ((width - textWidth) / (line.length - 1))
                                    .toInt().toFloat()
                            // 字间距一个单位是一字宽，
                            textPaint.letterSpacing = spacing / textPaint.textSize
                        }
                    }
                    drawTextBottom(content, line, 0f, y.toFloat(), textPaint)
                    // 去掉字间距以免影响后续计算，
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        textPaint.letterSpacing = 0f
                    }
                    y += lineSpacing
                }
                is ParagraphSpacing -> y += paragraphSpacing
            }
            if (reader.config.fitHeight) {
                y += page.fitLineSpacing
            }
        }
    }

    /**
     * @param y 文字矩形左下角的y值，不是基线的，
     */
    private fun drawTextBottom(canvas: Canvas, text: String, x: Float, y: Float, p: TextPaint) {
        // 减去基线距离，
        canvas.drawText(text, x, y - p.descent(), p)
    }

    private fun drawBackground(background: Canvas) {
        backgroundImage?.let {
            background.drawBitmap(it, null, Rect(0, 0, backgroundSize.width, backgroundSize.height), null)
        }
    }

    private val requestingList = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())
    private fun request(requestIndex: Int, refresh: Boolean = false, onComplete: (success: Boolean) -> Unit = {}): Job? {
        if (requestingList.contains(requestIndex)) {
            // 已经在异步请求章节了，
            onComplete(false)
            return null
        }
        // 失败时如果原本有内容就保留，避免刷新失败导致章节变空不可读，
        val previousPages = pagesCache[requestIndex]
        requestingList.add(requestIndex)
        Timber.d("$this lazyRequest $requestIndex, refresh = $refresh")
        return scope.launch {
            try {
                val pages = withContext(reader.ioDispatcher) {
                    val text = requester.requestChapter(requestIndex, refresh)
                    typesetting(reader.chapterList[requestIndex], text)
                }
                pagesCache.put(requestIndex, pages)
                requestingList.remove(requestIndex)
                Timber.d("request result $requestIndex == $chapterIndex")
                // 如果还在这个章节就刷新，
                if (requestIndex == chapterIndex) {
                    pager?.refresh()
                }
                onComplete(true)
            } catch (e: CancellationException) {
                // 用户取消刷新，保留原有内容，不回调不提示，
                requestingList.remove(requestIndex)
                throw e
            } catch (e: Exception) {
                val message = "小说章节获取失败：$requestIndex, ${reader.chapterList[requestIndex]}"
                Timber.e(e, message)
                if (previousPages != null && previousPages.isNotEmpty()) {
                    // 之前有内容就保留，比如刷新失败，
                    pagesCache.put(requestIndex, previousPages)
                } else {
                    // 缓存空的页面，到时候显示本章空内容，
                    pagesCache.put(requestIndex, listOf())
                }
                requestingList.remove(requestIndex)
                if (requestIndex == chapterIndex) {
                    pager?.refresh()
                }
                onComplete(false)
            }
        }
    }

    private fun typesetting(chapter: String, list: List<String>): List<Page> {
        // 额外使用一个textPaint以免被当前页渲染过程调整字间距影响到，
        val typesettingTextPaint = TextPaint(this.textPaint)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            typesettingTextPaint.letterSpacing = 0f
        }
        val pages = mutableListOf<Page>()
        var height = 0
        var fitHeight = 0
        val lines = mutableListOf<Any>()
        val lineSpacing = (reader.config.lineSpacing * reader.context.resources.displayMetrics.density).toInt()
        val paragraphSpacing = (reader.config.paragraphSpacing * reader.context.resources.displayMetrics.density).toInt()
        val textHeight = typesettingTextPaint.getFontMetricsInt(null)
        (listOf(chapter) + list).forEachIndexed { index, str ->
            // 不支持图片，得到段就直接转成String,
            val paragraph = if (index == 0) str else requester.requestParagraph(str).toString()
            var start = 0
            var count: Int
            while (start < paragraph.length) {
                height += textHeight
                Timber.v("typesetting height $height/${contentSize.height}")
                if (height > contentSize.height) {
                    // 铺满高度需要添加的间距，
                    // 转int下取整以免最后一行超出底部，
                    // 最后一个行间距也要删除，
                    val space = ((contentSize.height - fitHeight))
                    val fitLineSpacing = (space.toFloat() / (lines.size - 1))
                            .toInt()
                    height = textHeight
                    Timber.d("add lines size ${lines.size}")
                    pages.add(Page(ArrayList(lines), fitLineSpacing))
                    lines.clear()
                }
                count = typesettingTextPaint.breakText(paragraph.substring(start), true, contentSize.width.toFloat(), null)
                val line = paragraph.substring(start, start + count)
                if (index == 0) {
                    lines.add(Title(line))
                } else {
                    lines.add(line)
                }
                fitHeight = height
                // 行间距只加在文字下方，不会加在段间距下方，
                height += lineSpacing
                start += count
            }
            height += paragraphSpacing
            lines.add(ParagraphSpacing(paragraphSpacing))
        }
        // 多出来的最后一页，
        if (lines.isNotEmpty()) {
            Timber.d("add lines size ${lines.size}")
            pages.add(Page(ArrayList(lines)))
        }
        Timber.d("pages size = ${pages.size}")
        return pages
    }


    override fun scrollToPrev(): Boolean {
        val pages = pagesCache[chapterIndex]
        if (pages != null && pageIndex < 0) {
            // 负数转正，如果pages.size为0也无所谓了，
            pageIndex = pages.lastIndex
        }
        val prevPageIndex = pageIndex - 1
        if (pages != null && prevPageIndex in pages.indices) {
            pageIndex--
            reader.readingListener?.onReading(chapterIndex, pageIndex)
            return true
        }
        val prevChapterIndex = chapterIndex - 1
        if (prevChapterIndex in reader.chapterList.indices) {
            chapterIndex--
            pageIndex = -1
            // -1存起来应该也没关系，
            reader.readingListener?.onReading(chapterIndex, pageIndex)
            return true
        }
        reader.readingListener?.onReading(chapterIndex, pageIndex)
        return false
    }

    override fun scrollToNext(): Boolean {
        val pages = pagesCache[chapterIndex]
        val nextPageIndex = pageIndex + 1
        val nextChapterIndex = chapterIndex + 1
        if (pages != null && pageIndex >= 0 && nextPageIndex in pages.indices) {
            pageIndex++
            if (nextChapterIndex in reader.chapterList.indices && pagesCache.get(nextChapterIndex) == null) {
                // 提前缓存一章，
                request(nextChapterIndex)
            }
            reader.readingListener?.onReading(chapterIndex, pageIndex)
            return true
        }
        if (nextChapterIndex in reader.chapterList.indices) {
            chapterIndex++
            pageIndex = 0
            reader.readingListener?.onReading(chapterIndex, pageIndex)
            return true
        }
        reader.readingListener?.onReading(chapterIndex, pageIndex)
        return false
    }

    fun refreshCurrentChapter(onComplete: (success: Boolean) -> Unit = {}): Job? {
        // 不提前清空缓存，刷新失败时request会保留原有内容，成功时会被覆盖，
        return request(chapterIndex, true, onComplete)
    }
}