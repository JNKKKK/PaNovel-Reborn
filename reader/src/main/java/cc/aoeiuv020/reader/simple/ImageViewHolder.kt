package cc.aoeiuv020.reader.simple

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cc.aoeiuv020.reader.*

internal class ImageViewHolder(
        itemView: View,
        private val prAdapter: PageRecyclerAdapter
) : PageRecyclerAdapter.ViewHolder(itemView) {
    companion object {
        fun create(ctx: Context, parent: ViewGroup, prAdapter: PageRecyclerAdapter): ImageViewHolder {
            val view = LayoutInflater.from(ctx).inflate(R.layout.simple_image_item, parent, false)
            return ImageViewHolder(view, prAdapter)
        }
    }

    private val ctx: Context = itemView.context
    private val tvPage = itemView.findViewById<android.widget.TextView>(R.id.tvPage)
    private val ivImage = itemView.findViewById<com.github.chrisbanes.photoview.PhotoView>(R.id.ivImage)

    fun setImage(reader: INovelReader, index: Int, image: Image) {
        tvPage.text = (index + 1).toString()
        tvPage.setTextColor(reader.config.textColor)
        ivImage.apply {
            post {
                layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                    setMargins((prAdapter.mLeftSpacing.toFloat() / 100 * itemView.width).toInt(),
                            topMargin,
                            (prAdapter.mRightSpacing.toFloat() / 100 * itemView.width).toInt(),
                            (prAdapter.mParagraphSpacing * ctx.resources.displayMetrics.density).toInt())
                }
            }

        }
        itemView.layoutParams = itemView.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        ivImage.setImageDrawable(null)
        tvPage.show()
        ivImage.tag = index
        prAdapter.reader.requester.requestImage(image) { file ->
            if (ivImage.tag != index) {
                return@requestImage
            }
            tvPage.hide()
            itemView.layoutParams = itemView.layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            ivImage.setImageURI(Uri.fromFile(file))
        }
    }
}
