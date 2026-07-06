package cc.aoeiuv020.pager

import android.graphics.Canvas

interface PagerDrawer {

    fun attach(pager: Pager, backgroundSize: Size, contentSize: Size)

    fun drawCurrentPage(background: Canvas, content: Canvas)

    fun scrollToPrev(): Boolean

    fun scrollToNext(): Boolean

    fun detach()
}