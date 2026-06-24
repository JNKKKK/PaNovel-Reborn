package cc.aoeiuv020.panovel.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.data.DataManager
import kotlinx.coroutines.*
import timber.log.Timber

class DetailChaptersFragment : Fragment() {
    companion object {
        private const val ARG_NOVEL_ID = "novelId"

        fun newInstance(novelId: Long): DetailChaptersFragment {
            return DetailChaptersFragment().apply {
                arguments = Bundle().apply { putLong(ARG_NOVEL_ID, novelId) }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var adapter: ChapterAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_detail_chapters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvChapters)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = ChapterAdapter()
        rv.adapter = adapter

        val novelId = arguments?.getLong(ARG_NOVEL_ID) ?: return
        progressBar.visibility = View.VISIBLE
        scope.launch {
            try {
                val chapters = withContext(Dispatchers.IO) {
                    val manager = DataManager.getNovelManager(novelId)
                    manager.requestChapters(false)
                }
                adapter?.submitList(chapters)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load chapters")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        scope.cancel()
        super.onDestroyView()
    }

    private class ChapterAdapter : RecyclerView.Adapter<ChapterAdapter.VH>() {
        private var chapters: List<NovelChapter> = emptyList()

        fun submitList(list: List<NovelChapter>) {
            chapters = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.novel_chapter_item, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(chapters[position])
        }

        override fun getItemCount(): Int = chapters.size

        private class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val name: TextView = view.findViewById(R.id.name)
            private val updateTime: TextView = view.findViewById(R.id.tvUpdateTime)

            fun bind(chapter: NovelChapter) {
                name.text = chapter.name
                val update = chapter.update
                if (update == null) {
                    updateTime.visibility = View.GONE
                } else {
                    updateTime.visibility = View.VISIBLE
                    updateTime.text = android.text.format.DateUtils.getRelativeTimeSpanString(
                        update.time, System.currentTimeMillis(),
                        java.util.concurrent.TimeUnit.SECONDS.toMillis(1)
                    )
                }
            }
        }
    }
}
