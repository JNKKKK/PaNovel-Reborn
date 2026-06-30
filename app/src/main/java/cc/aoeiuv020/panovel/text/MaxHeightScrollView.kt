package cc.aoeiuv020.panovel.text

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * 限制最大高度的 ScrollView，超出时内部滚动，
 * 原生 ScrollView 不支持 android:maxHeight，所以自定义，
 * 设置阅读设置对话框的最大高度，露出更多正文方便即时预览，
 */
class MaxHeightScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ScrollView(context, attrs, defStyleAttr) {
    private val maxHeightPx: Int

    init {
        val a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.maxHeight))
        val attrMaxHeight = a.getDimensionPixelSize(0, 0)
        a.recycle()
        // 没有指定就默认用屏幕高度的一半，
        maxHeightPx = attrMaxHeight.takeIf { it > 0 }
            ?: (resources.displayMetrics.heightPixels * 0.5f).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val limitedHeightSpec = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, limitedHeightSpec)
    }
}
