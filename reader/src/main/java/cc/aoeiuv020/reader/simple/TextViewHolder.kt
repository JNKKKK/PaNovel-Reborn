package cc.aoeiuv020.reader.simple

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cc.aoeiuv020.reader.R
import cc.aoeiuv020.reader.setHeight
import org.jetbrains.anko.debug
import org.jetbrains.anko.dip

internal class TextViewHolder internal constructor(
        itemView: View,
        private val prAdapter: PageRecyclerAdapter
) : PageRecyclerAdapter.ViewHolder(itemView) {
    private val ctx: Context = itemView.context
    private val divider: View = itemView.findViewById(R.id.divider)
    private val textView: TextView = itemView.findViewById(R.id.textView)
    fun setText(string: String) {
        divider.setHeight(ctx.dip(prAdapter.mLineSpacing) + ctx.dip(prAdapter.mParagraphSpacing))
        textView.apply {
            prAdapter.debug { "initMargin <${prAdapter.mLeftSpacing}, ${prAdapter.mRightSpacing}>" }
            text = string
            typeface = if (layoutPosition == 0) {
                prAdapter.reader.config.titleFont
            } else {
                prAdapter.reader.config.font
            }
            textSize = prAdapter.mTextSize.toFloat()
            post {
                requestLayout()
            }
            setTextColor(prAdapter.mTextColor)
            setLineSpacing(context.dip(prAdapter.mLineSpacing).toFloat(), 1.toFloat())
            post {
                layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                    setMargins((prAdapter.mLeftSpacing.toFloat() / 100 * itemView.width).toInt(),
                            topMargin,
                            (prAdapter.mRightSpacing.toFloat() / 100 * itemView.width).toInt(),
                            bottomMargin)
                }
            }
        }
    }
}
