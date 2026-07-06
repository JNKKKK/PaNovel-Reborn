package cc.aoeiuv020.pager

import android.graphics.Canvas

class BlankPagerDrawer : BasePagerDrawer() {
    override fun drawCurrentPage(background: Canvas, content: Canvas) {
        background.drawColor(0xffffffff.toInt())
    }

    override fun scrollToPrev(): Boolean {
        return true
    }

    override fun scrollToNext(): Boolean {
        return true
    }
}