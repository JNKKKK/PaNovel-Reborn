package cc.aoeiuv020.panovel.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.settings.ItemAction
import cc.aoeiuv020.panovel.settings.ListSettings
import java.util.*
import java.util.concurrent.TimeUnit

open class NovelListAdapter(
        /**
         * 初始化小说item时调用，用于书架列表隐藏添加书架按钮，
         */
        private val initItem: (NovelViewHolder) -> Unit = {},
        actionDoneListener: (ItemAction, NovelViewHolder) -> Unit = { _, _ -> },
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

    // 当前设置对应的item布局，作为viewType使用，这样改了视图设置回来后，
    // viewType变化会让RecyclerView重建ViewHolder，而不会把列表布局的view复用给格子item，
    @LayoutRes
    private fun currentLayout(): Int = when {
        ListSettings.gridView && ListSettings.largeView -> R.layout.novel_item_grid_big
        ListSettings.gridView && !ListSettings.largeView -> R.layout.novel_item_grid_small
        !ListSettings.gridView && ListSettings.largeView -> R.layout.novel_item_big
        !ListSettings.gridView && !ListSettings.largeView -> R.layout.novel_item_small
        else -> R.layout.novel_item_big
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

    // viewType就是布局res，保证不同布局的ViewHolder不会互相复用，
    @LayoutRes
    override fun getItemViewType(position: Int): Int = currentLayout()

    override fun onCreateViewHolder(parent: ViewGroup, @LayoutRes viewType: Int): BaseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return NovelViewHolder(itemView, ListSettings.dotColor, ListSettings.dotSize, refreshingNovelSet, shouldRefreshSet
                , initItem, actualActionDoneListener, onError)
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
        // TODO: 要看看会不要自动滚到底部，不要滚，
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