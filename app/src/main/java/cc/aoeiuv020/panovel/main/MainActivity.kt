package cc.aoeiuv020.panovel.main

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.backup.BackupActivity
import cc.aoeiuv020.panovel.booklist.BookListFragment
import cc.aoeiuv020.panovel.bookshelf.BookshelfFragment
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.databinding.ActivityMainBinding
import cc.aoeiuv020.panovel.detail.NovelDetailActivity
import cc.aoeiuv020.panovel.history.HistoryFragment
import cc.aoeiuv020.panovel.open.OpenManager
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.search.FuzzySearchActivity
import cc.aoeiuv020.panovel.search.SiteChooseActivity
import cc.aoeiuv020.panovel.settings.OtherSettings
import cc.aoeiuv020.panovel.settings.SettingsActivity
import cc.aoeiuv020.panovel.util.ProgressDialogCompat
import cc.aoeiuv020.panovel.util.cancelAllNotify
import cc.aoeiuv020.panovel.util.initNotificationChannel
import cc.aoeiuv020.panovel.util.loading
import cc.aoeiuv020.panovel.util.safelyShow
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import timber.log.Timber
/**
 *
 * Created by AoEiuV020 on 2017.10.15-15:53:19.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var progressDialog: ProgressDialogCompat

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.extras?.getString("SCAN_RESULT")?.let {
            OpenManager.open(this, it, openListener)
        }
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { OpenManager.open(this, it, openListener) }
    }
    private val bookshelfFragment: BookshelfFragment?
        get() = supportFragmentManager.fragments.firstOrNull { it is BookshelfFragment } as BookshelfFragment?
    private val bookListFragment: BookListFragment?
        get() = supportFragmentManager.fragments.firstOrNull { it is BookListFragment } as BookListFragment?
    private val historyFragment: HistoryFragment?
        get() = supportFragmentManager.fragments.firstOrNull { it is HistoryFragment } as HistoryFragment?

    private val openListener: OpenManager.OpenListener = object : OpenManager.OpenListener {
        override fun onOtherCase(str: String) {
            progressDialog.dismiss()
            // 打开的不是网址，就直接精确搜索，
            FuzzySearchActivity.start(this@MainActivity, str)
        }

        override fun onNovelOpened(novel: Novel) {
            progressDialog.dismiss()
            NovelDetailActivity.start(this@MainActivity, novel)
        }

        override fun onLocalNovelImported(novel: Novel) {
            progressDialog.dismiss()
            bookshelfFragment?.refresh()
            showMessage("导入小说<${novel.bookId}>")
        }

        override fun onBookListReceived(count: Int) {
            progressDialog.dismiss()
            bookListFragment?.refresh()
            showMessage("添加书单，共${count}本，")
        }

        override fun onError(message: String, e: Throwable) {
            progressDialog.dismiss()
            showError(message, e)
        }

        override fun onLoading(status: String) {
            loading(progressDialog, status)
        }
    }

    fun refreshBookshelf() {
        bookshelfFragment?.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        initNotificationChannel()
        requestNotificationPermission()

        progressDialog = ProgressDialogCompat(this)

        initWidget()
        syncSites()
        checkEmpty()

        // 异步检查签名，
        Check.asyncCheckSignature(this)

        // 异步检查是否有更新，
        Check.asyncCheckVersion(this)
        // 异步获取可能存在的, 我放在网上想推给用户的消息，
        // DevMessage was removed
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun syncSites() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.syncSites()
                }
            } catch (e: Exception) {
                Reporter.post("同步网站列表失败", e)
                Timber.e(e, "同步网站列表失败")
            }
        }
    }

    private fun checkEmpty() {
        lifecycleScope.launch {
            try {
                val isEmpty = withContext(Dispatchers.IO) {
                    DataManager.isEmpty()
                }
                if (isEmpty) {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(getString(R.string.tip_data_empty))
                        .setPositiveButton(R.string.sImport) { _, _ ->
                            BackupActivity.start(this@MainActivity)
                        }
                        .setNeutralButton(R.string.search) { _, _ ->
                            FuzzySearchActivity.start(this@MainActivity, "异世界")
                        }
                        .show()
                }
            } catch (t: Exception) {
                Reporter.unreachable(t)
            }
        }
    }

    private fun initWidget() {
        val tabs = listOf(
            R.string.bookshelf to { BookshelfFragment() },
            R.string.book_list to { BookListFragment() },
            R.string.history to { HistoryFragment() }
        )

        binding.container.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabs.size
            override fun createFragment(position: Int): Fragment = tabs[position].second()
        }

        TabLayoutMediator(binding.tabLayout, binding.container) { tab, position ->
            tab.text = getString(tabs[position].first)
        }.attach()

        binding.container.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 1) {
                    binding.fab.show()
                } else {
                    binding.fab.hide()
                }
            }
        })

        binding.fab.setOnClickListener { _ ->
            bookListFragment?.newBookList()
        }
    }

    override fun onResume() {
        super.onResume()
        // 回到主页时清空所有通知，包括小说更新通知和其他导出下载等通知，
        cancelAllNotify()
    }

    override fun onDestroy() {
        if (::progressDialog.isInitialized) {
            progressDialog.dismiss()
        }
        super.onDestroy()
    }

    private fun showExplain() {
        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.explain))
            setMessage(assets.open("Explain.txt").reader().readText())
            setPositiveButton(android.R.string.ok, null)
        }.create().safelyShow()
    }

    private fun scan() {
        val intent = Intent("com.google.zxing.client.android.SCAN")
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE")
        try {
            scanLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            android.widget.Toast.makeText(this, "没安装zxing二维码扫描器，", android.widget.Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            android.widget.Toast.makeText(this, "没权限？这里是调用zxing扫码，", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun open() {
        val layout = View.inflate(this@MainActivity, R.layout.dialog_editor, null)
        val etName = layout.findViewById<android.widget.EditText>(R.id.editText)
        etName.hint = getString(R.string.main_open_hint)
        AlertDialog.Builder(this@MainActivity).apply {
            setTitle(R.string.open)
            setView(layout)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val url = etName.text.toString()
                if (url.isNotEmpty()) {
                    OpenManager.open(this@MainActivity, url, openListener)
                }
            }
            setNeutralButton(R.string.local_novel) { _, _ ->
                openDocumentLauncher.launch(arrayOf("*/*"))
            }
        }.create().safelyShow()
    }

    private fun downloadAll() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.downloadAll()
                }
            } catch (e: Exception) {
                val message = "全部下载失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                showError(message, e)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.scan)?.isVisible = OtherSettings.scan
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> SettingsActivity.start(this)
            R.id.search -> FuzzySearchActivity.start(this)
            R.id.scan -> scan()
            R.id.open -> open()
            R.id.cacheAll -> downloadAll()
            R.id.source -> SiteChooseActivity.start(this)
            R.id.explain -> showExplain()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private val snack: Snackbar by lazy {
        Snackbar.make(binding.fab, "", Snackbar.LENGTH_SHORT)
    }

    fun showMessage(message: String) {
        snack.setText(message)
        snack.show()
    }

    fun showError(message: String, e: Throwable) {
        progressDialog.dismiss()
        snack.setText(message + e.message)
        snack.show()
    }
}
