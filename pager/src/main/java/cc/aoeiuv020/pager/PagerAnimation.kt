package cc.aoeiuv020.pager

import android.graphics.Canvas
import android.view.MotionEvent

interface PagerAnimation {
    fun draw(canvas: Canvas)
    fun scrollAnim()
    fun refresh()
    fun onTouchEvent(event: MotionEvent): Boolean
    /**
     * 滚动到下一页，不必支持，
     * @return 返回是否成功翻页，
     */
    fun scrollNext(): Boolean

    /**
     * 滚动到下一页，不必支持，
     * 传入滚动动画的起始点，
     */
    fun scrollNext(x: Float, y: Float): Boolean
    fun scrollPrev(): Boolean
    fun scrollPrev(x: Float, y: Float): Boolean
    fun setDurationMultiply(multiply: Float)

    /**
     * 把视图坐标（含留白）的一个点解析成命中的页与页内内容坐标，
     * 分页模式恒为当前页；滚动模式据 bitmap 的纵向偏移判断落在哪一页。
     * 无法命中（点在留白外等）时返回 null,
     * @param x,y 视图坐标系（含留白）下的触摸点，
     */
    fun hitTest(x: Float, y: Float): PageHit? = null
}