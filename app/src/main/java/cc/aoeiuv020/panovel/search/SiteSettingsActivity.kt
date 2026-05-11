package cc.aoeiuv020.panovel.search

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import cc.aoeiuv020.panovel.IView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.databinding.ActivitySiteSettingsBinding
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.util.show
import cc.aoeiuv020.panovel.util.tip
import cc.aoeiuv020.panovel.util.uiInput
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class SiteSettingsActivity : AppCompatActivity(), IView, AnkoLogger {
    companion object {
        fun start(ctx: Context, site: String) {
            ctx.startActivity<SiteSettingsActivity>("site" to site)
        }
    }

    private lateinit var binding: ActivitySiteSettingsBinding
    private lateinit var siteName: String
    private lateinit var presenter: SiteSettingsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySiteSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        siteName = intent?.getStringExtra("site") ?: run {
            Reporter.unreachable()
            finish()
            return
        }
        debug { "receive site: $siteName" }
        title = siteName

        presenter = SiteSettingsPresenter(siteName)
        presenter.attach(this)

        presenter.start()
    }

    fun init() {
        if (!presenter.isUpkeep()) {
            binding.llStopUpkeep.show()
            binding.llStopUpkeep.setOnClickListener {
                tip(presenter.getReason())
            }
        }
        binding.llCookie.setOnClickListener {
            presenter.setCookie({ cookies ->
                uiInput(getString(R.string.cookie), cookies)
            }, {
                showMessage(getString(R.string.tip_set_cookie_success))
            })
        }
        binding.llHeader.setOnClickListener {
            presenter.setHeader({ header ->
                uiInput(getString(R.string.header), header, multiLine = true)
            }, {
                showMessage(getString(R.string.tip_set_header_success))
            })
        }
        binding.llCharset.setOnClickListener {
            presenter.setCharset({
                uiInput(getString(R.string.site_charset), it)
            }, {
                showMessage(getString(R.string.tip_set_header_success))
            })
        }
    }

    fun showError(message: String, e: Throwable) {
        toast(message + e)
    }

    fun showMessage(message: String) {
        toast(message)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
