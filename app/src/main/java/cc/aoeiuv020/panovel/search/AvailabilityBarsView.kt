package cc.aoeiuv020.panovel.search

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.availability.ProbeStatus

/**
 * 书源可用性状态条：一排等宽圆角竖条，旧在左、新在右，
 * 绿=可用，琥珀=被拦截，红=不可用，灰=当天未采样，
 * 纯展示控件，数据换算见 [cc.aoeiuv020.panovel.data.availability.AvailabilityManager.barsFor]。
 */
class AvailabilityBarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private val barGap = 3f * density
    private val barRadius = 2f * density
    private val rect = RectF()

    private val colorOk = ContextCompat.getColor(context, R.color.availabilityOk)
    private val colorRecovered = ContextCompat.getColor(context, R.color.availabilityRecovered)
    private val colorFail = ContextCompat.getColor(context, R.color.availabilityFail)
    private val colorUnknown = ContextCompat.getColor(context, R.color.availabilityUnknown)

    private var statuses: List<ProbeStatus> = emptyList()

    fun setStatuses(statuses: List<ProbeStatus>) {
        this.statuses = statuses
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count = statuses.size
        if (count == 0) return

        val top = paddingTop.toFloat()
        val bottom = (height - paddingBottom).toFloat()
        if (bottom <= top) return

        val available = width - paddingLeft - paddingRight
        val totalGap = barGap * (count - 1)
        val barWidth = (available - totalGap) / count
        if (barWidth <= 0f) return

        var left = paddingLeft.toFloat()
        for (status in statuses) {
            paint.color = colorFor(status)
            rect.set(left, top, left + barWidth, bottom)
            canvas.drawRoundRect(rect, barRadius, barRadius, paint)
            left += barWidth + barGap
        }
    }

    private fun colorFor(status: ProbeStatus): Int = when (status) {
        ProbeStatus.OK -> colorOk
        ProbeStatus.RECOVERED -> colorRecovered
        ProbeStatus.FAIL -> colorFail
        ProbeStatus.UNKNOWN -> colorUnknown
    }
}
