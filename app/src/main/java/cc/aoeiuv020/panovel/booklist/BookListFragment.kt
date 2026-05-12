@file:Suppress("DEPRECATION")

package cc.aoeiuv020.panovel.booklist

import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import cc.aoeiuv020.panovel.MvpView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.entity.BookList
import cc.aoeiuv020.panovel.databinding.NovelItemListBinding
import cc.aoeiuv020.panovel.main.MainActivity
import cc.aoeiuv020.panovel.share.Share
import cc.aoeiuv020.panovel.util.loading
import cc.aoeiuv020.panovel.util.notNullOrReport
import cc.aoeiuv020.panovel.util.safelyShow
import cc.aoeiuv020.panovel.util.showKeyboard
import androidx.appcompat.app.AlertDialog

/**
 *
 * Created by AoEiuV020 on 2017.11.22-14:07:56.
 */
class BookListFragment : androidx.fragment.app.Fragment(), MvpView {
    private var _binding: NovelItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var progressDialog: ProgressDialog
    private val itemListener: BookListFragmentAdapter.ItemListener = object : BookListFragmentAdapter.ItemListener {
        override fun onClick(vh: BookListFragmentAdapter.ViewHolder) {
            BookListActivity.start(requireContext(), vh.bookList.nId)
        }

        override fun onLongClick(vh: BookListFragmentAdapter.ViewHolder): Boolean {
            val list = listOf(R.string.remove to { remove(vh) },
                    R.string.rename to { rename(vh.bookList) },
                    R.string.copy to { copy(vh.bookList) },
                    R.string.share to { shareBookList(vh.bookList) },
                    R.string.add_bookshelf to { addBookshelf(vh.bookList) },
                    R.string.remove_bookshelf to { removeBookshelf(vh.bookList) })
            AlertDialog.Builder(requireContext())
                .setTitle(requireContext().getString(R.string.action))
                .setItems(list.unzip().first.map { requireContext().getString(it) }.toTypedArray()) { _, i ->
                    list[i].second.invoke()
                }.show()
            return true
        }
    }

    fun showRemoveBookshelfComplete() {
        showComplete(requireContext().getString(R.string.remove_bookshelf_complete))
        (activity as? MainActivity)?.refreshBookshelf()
    }

    fun showRemoving() {
        requireContext().loading(progressDialog, R.string.removing_bookshelf)
    }

    private fun removeBookshelf(bookList: BookList) {
        presenter.removeBookshelf(bookList)
    }

    fun showAddBookshelfComplete() {
        showComplete(requireContext().getString(R.string.add_bookshelf_complete))
        (activity as? MainActivity)?.refreshBookshelf()
    }

    fun showAdding() {
        requireContext().loading(progressDialog, R.string.removing_bookshelf)
    }

    private fun addBookshelf(bookList: BookList) {
        presenter.addBookshelf(bookList)
    }

    private val mAdapter = BookListFragmentAdapter(itemListener)

    private fun remove(vh: BookListFragmentAdapter.ViewHolder) {
        presenter.remove(vh.bookList)
    }

    private fun copy(bookList: BookList) {
        val layout = View.inflate(requireContext(), R.layout.dialog_editor, null)
        val etName = layout.findViewById<EditText>(R.id.editText)
        etName.setText(bookList.name)
        etName.setSelection(0, etName.text.length)
        etName.hint = bookList.name
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.copy)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = etName.text.toString()
                if (name.isNotEmpty()) {
                    presenter.copyBookList(bookList, name)
                }
            }
            .create().safelyShow()
        etName.post { etName.showKeyboard() }
    }

    private fun rename(bookList: BookList) {
        val layout = View.inflate(requireContext(), R.layout.dialog_editor, null)
        val etName = layout.findViewById<EditText>(R.id.editText)
        etName.setText(bookList.name)
        etName.setSelection(0, etName.text.length)
        etName.hint = bookList.name
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.rename)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = etName.text.toString()
                if (name.isNotEmpty()) {
                    presenter.renameBookList(bookList, name)
                }
            }
            .create().safelyShow()
        etName.post { etName.showKeyboard() }
    }

    fun shareBookList(bookList: BookList) {
        presenter.shareBookList(bookList)
    }

    private val presenter: BookListOverviewPresenter = BookListOverviewPresenter()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = NovelItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        progressDialog = ProgressDialog(context)
        // Note: 这里不是小说列表，固定用LinearLayoutManager，
        binding.rvNovel.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.rvNovel.adapter = mAdapter
        binding.srlRefresh.setOnRefreshListener {
            refresh()
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
        // 开始查询书单列表，
        // 每次切到这个页面就刷新，
        refresh()
    }

    fun refresh() {
        binding.srlRefresh.isRefreshing = true
        presenter.refresh()
    }

    fun showBookListList(list: List<BookList>) {
        binding.srlRefresh.isRefreshing = false
        mAdapter.data = list
    }

    fun showUploading() {
        requireContext().loading(progressDialog, getString(R.string.uploading))
    }

    fun showSharedUrl(url: String, qrCode: String) {
        progressDialog.dismiss()
        Share.alert(context!!, url, qrCode)
    }

    fun newBookList() {
        val context = activity.notNullOrReport()
        val layout = View.inflate(context, R.layout.dialog_editor, null)
        val etName = layout.findViewById<EditText>(R.id.editText)
        AlertDialog.Builder(context)
            .setTitle(R.string.add_book_list)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = etName.text.toString()
                if (name.isNotEmpty()) {
                    presenter.newBookList(etName.text.toString())
                }
            }
            .create().safelyShow()
        etName.post { etName.showKeyboard() }
    }

    fun showComplete(message: String) {
        binding.srlRefresh.isRefreshing = false
        progressDialog.dismiss()
        (activity as? MainActivity)?.showMessage(message)
    }

    fun showError(message: String, e: Throwable) {
        binding.srlRefresh.isRefreshing = false
        progressDialog.dismiss()
        (activity as? MainActivity)?.showError(message, e)
    }
}