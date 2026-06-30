package cc.aoeiuv020.panovel.booklist

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cc.aoeiuv020.panovel.MvpView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.data.entity.BookList
import cc.aoeiuv020.panovel.list.NovelListAdapter
import cc.aoeiuv020.panovel.list.NovelViewHolder
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.databinding.ActivityBookListBinding
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog as AndroidXAlertDialog
import cc.aoeiuv020.panovel.util.safelyShow
import com.google.android.material.snackbar.Snackbar

/**
 *
 * Created by AoEiuV020 on 2017.11.22-14:49:22.
 */
class BookListActivity : AppCompatActivity(), MvpView {
    companion object {
        private const val EXTRA_BOOK_LIST_ID = "bookListId"

        fun start(context: Context, bookListId: Long) {
            context.startActivity(Intent(context, BookListActivity::class.java).putExtra(EXTRA_BOOK_LIST_ID, bookListId))
        }
    }

    private lateinit var binding: ActivityBookListBinding
    // onCreate里赋值，必须有值，
    private var bookListId: Long = -1
    private lateinit var presenter: BookListDetailPresenter

    private val novelListAdapter by lazy {
        NovelListAdapter(initItem = { vh ->
            // 长按弹出删除菜单，只要这个就够了，
            vh.itemView.setOnLongClickListener {
                onItemLongClick(vh)
            }
        }, onError = ::showError)
    }

    private fun onItemLongClick(vh: NovelViewHolder): Boolean {
        val list = listOf(R.string.remove to {
            presenter.remove(vh.novelManager)
            this@BookListActivity.novelListAdapter.remove(vh.layoutPosition)
        })
        AndroidXAlertDialog.Builder(this)
            .setTitle(getString(R.string.action))
            .setItems(list.unzip().first.map { getString(it) }.toTypedArray()) { _, i ->
                list[i].second.invoke()
            }.show()
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(EXTRA_BOOK_LIST_ID, bookListId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bookListId = savedInstanceState?.getLong(EXTRA_BOOK_LIST_ID, -1L)
                ?.takeIf { it != -1L }
                ?: intent?.getLongExtra(EXTRA_BOOK_LIST_ID, -1L)?.takeIf { it != -1L }
                ?: run {
            Reporter.unreachable()
            Toast.makeText(this, "不存在，", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.includeNovelList.rvNovel.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        presenter = BookListDetailPresenter(bookListId)
        binding.includeNovelList.rvNovel.adapter = novelListAdapter
        binding.includeNovelList.srlRefresh.setOnRefreshListener {
            forceRefresh()
        }

        presenter.attach(this)
        // 查询书单名，只用来改title, 没什么大用，
        presenter.start()
    }

    override fun onRestart() {
        // 阅读后回来时要刷新，
        refresh()
        super.onRestart()
    }

    override fun onDestroy() {
        if (::presenter.isInitialized) {
            presenter.detach()
        }
        super.onDestroy()
    }

    fun showBookList(bookList: BookList) {
        // 书单对象只有这个用了，不需要存起来，
        title = bookList.name
        // 确实找到书单了再刷新列表，
        refresh()
    }

    fun showBookListNotFound(message: String, e: Throwable) {
        // 不应该出现书单找不到的问题，
        showError(message, e)
        finish()
    }

    private fun refresh() {
        binding.includeNovelList.srlRefresh.isRefreshing = true
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
        binding.includeNovelList.srlRefresh.isRefreshing = false
    }

    fun selectToAdd(list: List<NovelManager>, nameArray: Array<String>, containsArray: BooleanArray) {
        AlertDialog.Builder(this)
                .setTitle(R.string.contents)
                .setMultiChoiceItems(nameArray, containsArray) { _, i, isChecked ->
                    if (isChecked) {
                        presenter.add(list[i])
                    } else {
                        presenter.remove(list[i])
                    }
                }.setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    refresh()
                }
                .create().apply {
                    listView.isFastScrollEnabled = true
                }.safelyShow()
    }


    private fun add() {
        // 这些操作应该很块，不提示了，
        val list = listOf(R.string.bookshelf to {
            presenter.addFromBookshelf()
        }, R.string.history to {
            presenter.addFromHistory()
        })
        AndroidXAlertDialog.Builder(this)
            .setTitle(getString(R.string.add_from))
            .setItems(list.unzip().first.map { getString(it) }.toTypedArray()) { _, i ->
                list[i].second.invoke()
            }.show()
    }

    private val snack: Snackbar by lazy {
        Snackbar.make(binding.includeNovelList.rvNovel, "", Snackbar.LENGTH_SHORT)
    }

    fun showError(message: String, e: Throwable) {
        binding.includeNovelList.srlRefresh.isRefreshing = false
        snack.setText(message + e.message)
        snack.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_book_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.add -> add()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}