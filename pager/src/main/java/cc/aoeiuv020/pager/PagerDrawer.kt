package cc.aoeiuv020.pager

import android.graphics.Canvas

interface PagerDrawer {

    fun attach(pager: Pager, backgroundSize: Size, contentSize: Size)

    fun drawCurrentPage(background: Canvas, content: Canvas)

    fun scrollToPrev(): Boolean

    fun scrollToNext(): Boolean

    fun detach()

    /**
     * 当前正要绘制的页的不透明标识（编码章节+页码等），供命中测试回传时定位到具体页。
     * 滚动模式下动画会在每次 drawCurrent 后读取它并记到对应 bitmap 上。
     */
    fun currentPageTag(): Long = 0L

    /**
     * 把一次命中（页标识 + 页内内容坐标）解析成结果，命中不到文字时返回 null,
     * 具体解码由实现方（reader）完成，pager 不关心含义，
     */
    fun hitTest(hit: PageHit): String? = null
}