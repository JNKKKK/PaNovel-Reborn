package cc.aoeiuv020.panovel.bookshelf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cc.aoeiuv020.panovel.MvpView
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.databinding.NovelItemListBinding
import cc.aoeiuv020.panovel.list.NovelListAdapter
import cc.aoeiuv020.panovel.main.MainActivity
import cc.aoeiuv020.panovel.settings.ItemAction.Pinned
import cc.aoeiuv020.panovel.settings.ItemAction.RemoveBookshelf
import cc.aoeiuv020.panovel.settings.ListSettings
import cc.aoeiuv020.panovel.util.hide
import cc.aoeiuv020.panovel.util.show

/**
 *
 * Created by AoEiuV020 on 2017.10.15-17:22:28.
 */
class BookshelfFragment : androidx.fragment.app.Fragment(), MvpView {
    private var _binding: NovelItemListBinding? = null
    private val binding get() = _binding!!

    private val novelListAdapter: NovelListAdapter by lazy {
        NovelListAdapter(initItem = {
            // 以防万一加上问号?支持视图中没有小红点的情况，
            // 显示小红点控件，包括代表正在刷新的圆形进度条，
            it.refreshingDot?.show()
            // 隐藏用于添加书架的按钮，
            it.star?.hide()
        }, actionDoneListener = { action, vh ->
            when (action) {
                RemoveBookshelf -> novelListAdapter.remove(vh.layoutPosition)
                Pinned -> novelListAdapter.move(vh.layoutPosition, 0)
                else -> {
                }
            }
        }, onError = ::showError)
    }
    private val presenter: BookshelfPresenter = BookshelfPresenter()
    private var firstResume = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = NovelItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvNovel.layoutManager = if (ListSettings.gridView) {
            androidx.recyclerview.widget.GridLayoutManager(requireContext(), if (ListSettings.largeView) 3 else 5)
        } else {
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        }
        binding.rvNovel.adapter = novelListAdapter
        binding.srlRefresh.setOnRefreshListener {
            forceRefresh()
        }

        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        _binding = null
        super.onDestroyView()
    }


    override fun onResume() {
        super.onResume()
        if (firstResume) {
            firstResume = false
            forceRefresh()
        } else {
            refresh()
        }
    }

    fun refresh() {
        binding.srlRefresh.isRefreshing = true
        presenter.refresh()
    }

    /**
     * 强行刷新，重新下载小说详情，主要是看最新章，
     */
    private fun forceRefresh() {
        novelListAdapter.refresh()
        refresh()
    }

    fun showNovelList(list: List<NovelManager>) {
        novelListAdapter.data = list
        binding.srlRefresh.isRefreshing = false
    }

    fun showError(message: String, e: Throwable) {
        // 按理说到这里已经不会是正在刷新的状态了，
        // 鬼知道发生了什么，反正这里就是npe了一次，导入旧版备份数据后回到书架时崩溃，
        _binding?.srlRefresh?.isRefreshing = false
        (activity as? MainActivity)?.showError(message, e)
    }
}