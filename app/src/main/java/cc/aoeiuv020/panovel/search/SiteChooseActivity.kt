package cc.aoeiuv020.panovel.search

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import cc.aoeiuv020.panovel.IView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.entity.Site
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import cc.aoeiuv020.panovel.databinding.ActivitySiteChooseBinding
import java.util.*

class SiteChooseActivity : AppCompatActivity(), IView {
    companion object {
        fun start(ctx: Context) {
            ctx.startActivity(Intent(ctx, SiteChooseActivity::class.java))
        }
    }

    private lateinit var binding: ActivitySiteChooseBinding
    private lateinit var presenter: SiteChoosePresenter

    private val itemListener = object : SiteListAdapter.ItemListener {
        override fun onEnabledChanged(site: Site) {
            presenter.enabledChange(site)
        }

        override fun onSiteSelect(site: Site) {
            SingleSearchActivity.start(this@SiteChooseActivity, site.name)
        }

        override fun onItemLongClick(vh: SiteListAdapter.ViewHolder): Boolean {
            val actions: List<Pair<Int, () -> Unit>> = listOf(
                    if (vh.site.enabled) {
                        R.string.disable
                    } else {
                        R.string.enable
                    } to {
                        vh.site.enabled = !vh.site.enabled
                        vh.cbEnabled.isChecked = vh.site.enabled
                        presenter.enabledChange(vh.site)
                    },
                    R.string.pinned to {
                        vh.site.pinnedTime = Date()
                        (binding.rvSiteList.adapter as SiteListAdapter).move(vh.layoutPosition, 0)
                        presenter.pinned(vh.site)
                    },
                    R.string.cancel_pinned to {
                        vh.site.pinnedTime = Date(0)
                        presenter.cancelPinned(vh.site)
                    }
            )
            AlertDialog.Builder(this@SiteChooseActivity)
                .setTitle(getString(R.string.select))
                .setItems(actions.map { getString(it.first) }.toTypedArray()) { _, i ->
                    actions[i].second()
                }.show()
            return true
        }

        override fun onSettingsClick(site: Site) {
            SiteSettingsActivity.start(this@SiteChooseActivity, site.name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySiteChooseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Note: 这里不是小说列表，固定用LinearLayoutManager，
        binding.rvSiteList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        presenter = SiteChoosePresenter()
        presenter.attach(this)

        presenter.start()
    }

    fun showSiteList(siteList: List<Site>) {
        val adapter = SiteListAdapter(siteList, itemListener)
        binding.rvSiteList.adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_site_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> FuzzySearchActivity.start(this)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun showError(message: String, e: Throwable) {
        Toast.makeText(this, message + e, Toast.LENGTH_SHORT).show()
    }
}
