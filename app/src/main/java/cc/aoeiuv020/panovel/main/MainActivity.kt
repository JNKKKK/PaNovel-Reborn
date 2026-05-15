package cc.aoeiuv020.panovel.main

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import cc.aoeiuv020.panovel.settings.SettingsActivity
import cc.aoeiuv020.panovel.share.Share
import cc.aoeiuv020.panovel.util.ProgressDialogCompat
import cc.aoeiuv020.panovel.util.cancelAllNotify
import cc.aoeiuv020.panovel.util.initNotificationChannel
import cc.aoeiuv020.panovel.util.loading
import cc.aoeiuv020.panovel.util.safelyShow
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var progressDialog: ProgressDialogCompat

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { handleScannedContent(it) }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchScanner()
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

        Check.asyncCheckSignature(this)
        Check.asyncCheckVersion(this)
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
                        .setPositiveButton(R.string.import_backup) { _, _ ->
                            BackupActivity.start(this@MainActivity)
                        }
                        .setNeutralButton(R.string.search) { _, _ ->
                            FuzzySearchActivity.start(this@MainActivity)
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchScanner()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(getString(R.string.scan))
            setBeepEnabled(false)
        }
        scanLauncher.launch(options)
    }

    private fun handleScannedContent(content: String) {
        if (Share.isShareContent(content)) {
            lifecycleScope.launch {
                try {
                    val count = withContext(Dispatchers.IO) {
                        Share.importShareContent(content)
                    }
                    openListener.onBookListReceived(count)
                } catch (e: Exception) {
                    Reporter.post("导入书单失败", e)
                    showError("导入书单失败，", e)
                }
            }
        } else if (content.startsWith("http")) {
            OpenManager.open(this, content, openListener)
        } else {
            showMessage(getString(R.string.unsupported_qr_content))
        }
    }

    private fun importLocalNovel() {
        openDocumentLauncher.launch(arrayOf("*/*"))
    }

    private fun importShareLink() {
        val text = Share.getClipboardText(this)
        if (text.isNullOrBlank()) {
            showMessage(getString(R.string.invalid_share_content))
            return
        }
        if (Share.isShareContent(text)) {
            lifecycleScope.launch {
                try {
                    val count = withContext(Dispatchers.IO) {
                        Share.importShareContent(text)
                    }
                    openListener.onBookListReceived(count)
                } catch (e: Exception) {
                    Reporter.post("导入书单失败", e)
                    showError("导入书单失败，", e)
                }
            }
        } else if (text.startsWith("http")) {
            OpenManager.open(this, text, openListener)
        } else {
            showMessage(getString(R.string.invalid_share_content))
        }
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> SettingsActivity.start(this)
            R.id.search -> FuzzySearchActivity.start(this)
            R.id.scan -> scan()
            R.id.importLocal -> importLocalNovel()
            R.id.importShare -> importShareLink()
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
