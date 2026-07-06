package cc.aoeiuv020.panovel.history


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cc.aoeiuv020.panovel.MvpView
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.databinding.NovelItemListBinding
import cc.aoeiuv020.panovel.list.NovelListAdapter
import cc.aoeiuv020.panovel.main.MainActivity


/**
 * 绝大部分照搬书架，
 */
class HistoryFragment : androidx.fragment.app.Fragment(), MvpView {
    private var _binding: NovelItemListBinding? = null
    private val binding get() = _binding!!

    private val novelListAdapter by lazy {
        NovelListAdapter(supportPin = false, onError = ::showError)
    }
    private val presenter: HistoryPresenter = HistoryPresenter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = NovelItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvNovel.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
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

    override fun onStart() {
        super.onStart()
        refresh()
    }

    private fun refresh() {
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
        binding.srlRefresh.isRefreshing = false
        (activity as? MainActivity)?.showError(message, e)
    }
}