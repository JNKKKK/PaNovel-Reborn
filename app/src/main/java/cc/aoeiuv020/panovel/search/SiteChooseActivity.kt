package cc.aoeiuv020.panovel.search

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cc.aoeiuv020.panovel.MvpView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.entity.Site
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.databinding.ActivitySiteChooseBinding
import cc.aoeiuv020.panovel.util.applyBottomNavBarInsetPadding
import cc.aoeiuv020.panovel.util.showWithNeutralSurface
import kotlinx.coroutines.launch

class SiteChooseActivity : AppCompatActivity(), MvpView {
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SiteChooseActivity::class.java))
        }
    }

    private lateinit var binding: ActivitySiteChooseBinding
    private lateinit var presenter: SiteChoosePresenter
    private var adapter: SiteListAdapter? = null

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
                    }
            )
            AlertDialog.Builder(this@SiteChooseActivity)
                .setTitle(getString(R.string.select))
                .setItems(actions.map { getString(it.first) }.toTypedArray()) { _, i ->
                    actions[i].second()
                }.showWithNeutralSurface()
            return true
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySiteChooseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.source)

        // Note: 这里不是小说列表，固定用LinearLayoutManager，
        binding.rvSiteList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvSiteList.applyBottomNavBarInsetPadding()

        presenter = SiteChoosePresenter()
        presenter.attach(this)

        presenter.start()
        observeAvailability()
    }

    override fun onStart() {
        super.onStart()
        // 每次打开/回到书源页，给当天为红的书源再探一次机会（成功则转黄），
        // 已在探测中的书源会被 AvailabilityManager 去重跳过，
        DataManager.availability.reprobeFailuresAsync()
    }

    fun showSiteList(siteList: List<Site>) {
        val adapter = SiteListAdapter(siteList, itemListener).also {
            it.updateHistory(DataManager.availability.history.value)
        }
        this.adapter = adapter
        binding.rvSiteList.adapter = adapter
    }

    /**
     * 观察可用性探测结果：若面板打开时当天的探测刚好完成，状态条会实时刷新，
     * 否则也会用已有历史立即渲染，
     */
    private fun observeAvailability() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                DataManager.availability.history.collect { history ->
                    adapter?.updateHistory(history)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }


    fun showError(message: String, e: Throwable) {
        Toast.makeText(this, message + e, Toast.LENGTH_SHORT).show()
    }
}
