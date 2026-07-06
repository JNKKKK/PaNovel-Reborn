package cc.aoeiuv020.panovel.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.settings.ItemAction
import java.util.*
import java.util.concurrent.TimeUnit

open class NovelListAdapter(
        /**
         * 初始化小说item时调用，用于书架列表隐藏添加书架按钮，
         */
        private val initItem: (NovelViewHolder) -> Unit = {},
        actionDoneListener: (ItemAction, NovelViewHolder) -> Unit = { _, _ -> },
        /**
         * 是否参与置顶：控制置顶/取消置顶菜单项和置顶高亮背景，历史列表不需要置顶，
         */
        private val supportPin: Boolean = true,
        private val onError: (String, Throwable) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<NovelListAdapter.BaseViewHolder>() {
    init {
        super.setHasStableIds(true)
    }

    private val actualActionDoneListener: (ItemAction, NovelViewHolder) -> Unit = { action, vh ->
        when (action) {
            // CleanData固定删除元素，无视传入的listener,
            ItemAction.CleanData -> remove(vh.layoutPosition)
            else -> actionDoneListener(action, vh)
        }
    }

    @Suppress("PropertyName")
    protected open var _data: MutableList<NovelManager> = mutableListOf()
    var data: List<NovelManager>
        get() = _data
        set(value) {
            val oldList = _data
            val newList = value.toMutableList()
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldList.size
                override fun getNewListSize() = newList.size
                override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                    return oldList[oldPos].novel.nId == newList[newPos].novel.nId
                }
                override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                    val old = oldList[oldPos].novel
                    val new = newList[newPos].novel
                    return old.name == new.name && old.readAtChapterName == new.readAtChapterName
                            // 置顶状态变化要重新绑定，否则置顶灰色背景不刷新，
                            && old.pinnedTime == new.pinnedTime
                }
            })
            _data = newList
            diffResult.dispatchUpdatesTo(this)
        }

    private var refreshTime = Date(TimeUnit.DAYS.toMillis(1))

    fun refresh() {
        refreshTime = Date()
        notifyItemRangeChanged(0, itemCount)
    }

    // 保存正在刷新的小说的id，避免重复刷新，以及view复用导致一直显示正在刷新中，
    // 一个列表共用一个，多个列表多个，
    private val refreshingNovelSet = mutableSetOf<Long>()

    // 用于服务器告知有更新时暂存，展示时再刷新，
    private val shouldRefreshSet = mutableSetOf<Long>()

    fun hasUpdate(hasUpdateList: List<Long>) {
        hasUpdateList.forEach {
            shouldRefreshSet.add(it)
        }
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.novel_item_big, parent, false)
        return NovelViewHolder(itemView, refreshingNovelSet, shouldRefreshSet
                , initItem, actualActionDoneListener, supportPin, onError)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val novel = _data[position]
        (holder as NovelViewHolder).apply(novel, refreshTime)
    }

    override fun getItemId(position: Int): Long {
        return data[position].novel.nId
    }


    fun addAll(list: List<NovelManager>) {
        val oldCount = itemCount
        _data.addAll(list)
        // 末尾插入，不会自动滚动；调用方FuzzySearchActivity另有保存/恢复滚动状态的处理，
        notifyItemRangeInserted(oldCount, itemCount - oldCount)
    }

    fun clear() {
        val oldSize = itemCount
        _data.clear()
        notifyItemRangeRemoved(0, oldSize)
    }

    fun remove(position: Int) {
        _data.removeAt(position)
        notifyItemRemoved(position)
    }

    fun move(from: Int, to: Int) {
        if (from == to || from !in _data.indices || to !in _data.indices) {
            // 位置不正确就直接返回，
            return
        }
        // ArrayList直接删除插入的话性能不行，但是无所谓了，
        val novel = _data.removeAt(from)
        _data.add(to, novel)
        notifyItemMoved(from, to)
    }

    abstract class BaseViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView)
}