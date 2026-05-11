package cc.aoeiuv020.reader.simple

import android.view.View
import android.view.ViewGroup
import timber.log.Timber
import java.util.*

internal class NovelTextPagerAdapter(private val simpleReader: SimpleReader) : androidx.viewpager.widget.PagerAdapter() {
    private val chapters get() = simpleReader.chapterList
    private val unusedHolders: LinkedList<PageHolder> = LinkedList()
    private val usedHolders: LinkedList<PageHolder> = LinkedList()
    private var current: PageHolder? = null

    override fun isViewFromObject(view: View, obj: Any) = (obj as PageHolder).itemView === view
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val holder = if (unusedHolders.isNotEmpty()) {
            unusedHolders.pop()
        } else {
            PageHolder(simpleReader)
        }.also { usedHolders.push(it) }
        val chapter = chapters[position]
        Timber.d("instantiate $position $chapter")
        container.addView(holder.itemView)
        holder.position = position
        holder.request(position)
        return holder
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, obj: Any) {
        super.setPrimaryItem(container, position, obj)
        Timber.d("viewpager current position $position")
        current = obj as? PageHolder
    }

    fun getCurrentTextCount(): Int? = current?.getTextCount()

    private var textProgress: Int? = null
    fun getCurrentTextProgress(): Int? = current?.getTextProgress() ?: textProgress
    fun setCurrentTextProgress(textProgress: Int) {
        this.textProgress = textProgress
        Timber.d("setCurrentTextProgress position ${current?.position}")
        current?.setTextProgress(textProgress)
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        Timber.d("destroy $position")
        val holder = obj as PageHolder
        val view = holder.itemView
        container.removeView(view)
        holder.destroy()
        holder.let {
            usedHolders.remove(it)
            unusedHolders.push(holder)
        }
    }

    override fun getCount() = chapters.size

    fun refreshCurrentChapter() {
        current?.refresh()
    }

    fun notifyAllItemDataSetChanged() {
        (usedHolders + unusedHolders).forEach {
            it.ntrAdapter.notifyDataSetChanged()
        }
    }

    fun notifyAllItemMarginsChanged() {
        (usedHolders + unusedHolders).forEach {
            it.notifyMarginsChanged()
        }
    }

    /**
     * 内容的上下左右间距改变时上面两个都要通知，
     */
    fun notifyAllItemContentSpacingChanged() {
        // 左右，
        notifyAllItemDataSetChanged()
        // 上下，
        notifyAllItemMarginsChanged()
    }

}