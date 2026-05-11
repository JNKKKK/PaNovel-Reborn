package cc.aoeiuv020.panovel.search

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import cc.aoeiuv020.panovel.IView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.detail.NovelDetailActivity
import cc.aoeiuv020.panovel.databinding.ActivitySingleSearchBinding
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.util.onActivityDestroy
import cc.aoeiuv020.panovel.util.safelyShow
import org.jetbrains.anko.*

/**
 * 负责单个网站的搜索功能，
 * 还有登录也在这里，
 */
class SingleSearchActivity : AppCompatActivity(), IView, AnkoLogger {
    private lateinit var binding: ActivitySingleSearchBinding

    companion object {
        fun start(ctx: Context, site: String) {
            ctx.startActivity<SingleSearchActivity>("site" to site)
        }
    }

    private lateinit var siteName: String
    private lateinit var presenter: SingleSearchPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        siteName = intent?.getStringExtra("site") ?: run {
            Reporter.unreachable()
            finish()
            return
        }
        debug { "receive site: $siteName" }
        title = siteName

        binding.srlRefresh.isRefreshing = true
        binding.srlRefresh.setOnRefreshListener {
            binding.wvSite.reload()
            binding.srlRefresh.isRefreshing = false
        }

        initWebView()

        presenter = SingleSearchPresenter(siteName)
        presenter.attach(this)

        presenter.start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.wvSite.apply {
            webViewClient = MyWebViewClient()
            webChromeClient = WebChromeClient()
            settings.apply {
                javaScriptEnabled = true
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    @Suppress("DEPRECATION")
                    databasePath = ctx.cacheDir.resolve("webView").path
                }
                databaseEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                setAppCacheEnabled(true)
            }
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAcceptThirdPartyCookies(binding.wvSite, true)
            }
        }
    }

    override fun onDestroy() {
        binding.wvSite.onActivityDestroy()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        debug {
            "onNewIntent ${intent.data}"
        }
        // 以防万一，从别的浏览器跳到这里时不要显示下拉刷新中，
        binding.srlRefresh.isRefreshing = false
        binding.wvSite.loadUrl(intent.data.toString())
    }

    private inner class MyWebViewClient : WebViewClient(), AnkoLogger {
        // 过时代替方法使用要api>=21,
        @Suppress("OverridingDeprecatedMember")
        override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
            debug { "shouldOverrideUrlLoading $url" }
            if (url == null || url.startsWith("http")) {
                return false
            }
            // 自定义协议之类的调用其他app打开，
            // 主要是为了支持QQ一键登录的wtloginmqq协议，拉起QQ，
            return try {
                // wtloginmqq://ptlogin/qlogin?p=https%3A%2F%2Fssl.ptlogin2.qq.com%2Fjump%3Fu1%3Dhttps%253A%252F%252Flogin.book.qq.com%252Flogin%252Fqqptcallback%253Ftype%253Dwap%2526appid%253D13%2526areaid%253D1%2526auto%253D1%2526ticket%253D1%2526ajaxdm%253Dyuewen%2526returnurl%253Dhttps%25253A%25252F%25252Fm.qidian.com%25252Fuser%25253Ffrom%25253Dlogin%26pt_report%3D1%26style%3D9%26pt_ua%3D5447A8135284D842CEE5CFD29AF3FB09%26pt_browser%3DChrome&schemacallback=googlechrome%3A%2F%2F
                // https://ssl.ptlogin2.qq.com/jump?u1=https%3A%2F%2Flogin.book.qq.com%2Flogin%2Fqqptcallback%3Ftype%3Dwap%26appid%3D13%26areaid%3D1%26auto%3D1%26ticket%3D1%26ajaxdm%3Dyuewen%26returnurl%3Dhttps%253A%252F%252Fm.qidian.com%252Fuser%253Ffrom%253Dlogin&pt_report=1&style=9&pt_ua=5447A8135284D842CEE5CFD29AF3FB09&pt_browser=Chrome
                // schemacallback=googlechrome%3A%2F%2F
                if (url.startsWith("wtloginmqq://")) {
                    // 把schemacallback破坏掉，否则有可能会自动判断浏览器然后选择性的跳回调，直接跳到chrome,
                    val newUrl = url.replace("schemacallback", "s")
                    browse(newUrl)
                } else {
                    browse(url)
                }
            } catch (e: Exception) {
                val message = "解析地址($url)失败，"
                Reporter.post(message, e)
                browse(url)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        presenter.pushCookies()
    }

    override fun onPause() {
        presenter.pullCookies()

        super.onPause()
    }

    fun showRemoveCookiesDone() {
        alert(
                title = getString(R.string.success),
                message = ctx.getString(R.string.message_cookies_removed)
        ) {
            okButton { }
        }.safelyShow()
    }

    fun getCurrentUrl(): String? = binding.wvSite.url

    fun openPage(url: String) {
        binding.srlRefresh.isRefreshing = false
        binding.wvSite.loadUrl(url)
    }

    private fun open() {
        // TODO: 这里需要个loading dialog,
        getCurrentUrl()?.let { url ->
            presenter.open(url)
        }
    }

    private fun removeCookies() {
        presenter.removeCookies()
    }

    fun openNovelDetail(novel: Novel) {
        NovelDetailActivity.start(ctx, novel)
    }

    fun showError(message: String, e: Throwable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed) {
            // 太丑了，
            return
        }
        alert(
                title = ctx.getString(R.string.error),
                message = message + e.message
        ) {
            okButton { }
        }.safelyShow()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        if (binding.wvSite.canGoBack()) {
            binding.wvSite.goBack()
        } else {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_single_search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> FuzzySearchActivity.startSingleSite(ctx, siteName)
            R.id.browse -> getCurrentUrl()?.let { browse(it) }
            R.id.open -> open()
            R.id.close -> finish()
            R.id.removeCookies -> removeCookies()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
