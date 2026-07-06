package cc.aoeiuv020.panovel.bookshelf

import android.annotation.TargetApi
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.util.hide
import cc.aoeiuv020.panovel.util.setSize
import cc.aoeiuv020.panovel.util.show

class RefreshingDotView : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    @TargetApi(21)
    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    private val pbRefreshing: ProgressBar
    private val ivDot: ImageView

    init {
        View.inflate(context, R.layout.view_refreshing_dot, this)
        pbRefreshing = findViewById(R.id.pbRefreshing)
        ivDot = findViewById(R.id.ivDot)
    }

    fun refreshing() {
        pbRefreshing.show()
        ivDot.hide()
    }

    fun refreshed(hasNew: Boolean) {
        pbRefreshing.hide()
        if (hasNew) {
            ivDot.show()
        } else {
            ivDot.hide()
        }
    }

    fun setDotColor(dotColor: Int) {
        ivDot.setColorFilter(dotColor)
    }

    fun setDotSize(dotSize: Int) {
        ivDot.setSize(dotSize)
    }
}