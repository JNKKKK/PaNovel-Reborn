package cc.aoeiuv020.panovel.text

import android.content.Context
import android.content.res.ColorStateList
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.core.content.ContextCompat
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.util.hide
import cc.aoeiuv020.panovel.util.show
import java.util.concurrent.TimeUnit

class NovelContentsAdapter(
        val context: Context,
        val novel: Novel,
        val chapters: List<NovelChapter>,
        // 只用contains方法判断章节是否已经缓存，
        private var cachedList: Collection<String>
) : BaseAdapter() {
    // Chapter-name colors by state (DayNight via resources): current chapter = pink accent,
    // downloaded chapter = green, default = colorOnSurface (readable on the list surface).
    private val chapterColorList = ColorStateList(
        arrayOf(
            // isChecked代表阅读到的章节，
            intArrayOf(android.R.attr.state_checked),
            // isSelected代表已经缓存的章节，
            intArrayOf(-android.R.attr.state_checked, android.R.attr.state_selected),
            intArrayOf()
        ),
        intArrayOf(
            ContextCompat.getColor(context, R.color.colorAccent),
            ContextCompat.getColor(context, R.color.chapterDownloaded),
            ContextCompat.getColor(context, R.color.colorOnSurface)
        )
    )

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.novel_chapter_item, parent, false).apply {
                    findViewById<CheckedTextView>(R.id.name).setTextColor(chapterColorList)
                }
        val nameTextView = view.findViewById<CheckedTextView>(R.id.name)
        val tvUpdateTime = view.findViewById<TextView>(R.id.tvUpdateTime)
        val chapter = getItem(position)
        nameTextView.apply {
            text = chapter.name
            // isChecked代表阅读到的章节，
            isChecked = novel.readAtChapterIndex == position
            // isSelected代表已经缓存的章节，
            isSelected = cachedList.contains(chapter.extra)
        }
        tvUpdateTime.apply {
            val update = chapter.update
            if (update == null) {
                hide()
            } else {
                text = DateUtils.getRelativeTimeSpanString(update.time, System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(1))
                show()
            }
        }
        return view
    }

    override fun getItem(position: Int): NovelChapter = chapters[position]

    override fun getItemId(position: Int): Long = 0L

    override fun getCount(): Int = chapters.size
}