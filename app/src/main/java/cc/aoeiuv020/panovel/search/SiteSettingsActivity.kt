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
import android.content.Intent
import android.widget.Toast
import cc.aoeiuv020.panovel.util.uiInput
import timber.log.Timber

class SiteSettingsActivity : AppCompatActivity(), IView {
    companion object {
        fun start(ctx: Context, site: String) {
            ctx.startActivity(Intent(ctx, SiteSettingsActivity::class.java).putExtra("site", site))
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
        Timber.d("receive site: $siteName")
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
        Toast.makeText(this, message + e, Toast.LENGTH_SHORT).show()
    }

    fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
