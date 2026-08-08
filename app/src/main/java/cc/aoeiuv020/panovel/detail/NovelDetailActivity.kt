@file:Suppress("DEPRECATION")

package cc.aoeiuv020.panovel.detail

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs
import cc.aoeiuv020.panovel.MvpView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.text.NovelTextActivity
import cc.aoeiuv020.panovel.databinding.ActivityNovelDetailBinding
import cc.aoeiuv020.panovel.util.alert
import cc.aoeiuv020.panovel.util.alertError
import cc.aoeiuv020.panovel.util.noCover
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.tabs.TabLayoutMediator
import android.content.Intent
import timber.log.Timber

class NovelDetailActivity : AppCompatActivity(), MvpView {
    companion object {
        fun start(context: Context, novel: Novel) {
            context.startActivity(Intent(context, NovelDetailActivity::class.java).putExtra(Novel.KEY_ID, novel.nId))
        }
    }

    private lateinit var binding: ActivityNovelDetailBinding
    private lateinit var alertDialog: AlertDialog
    private lateinit var presenter: NovelDetailPresenter
    private var novel: Novel? = null
    private var novelId: Long = -1L

    private var introductionFragment: DetailIntroductionFragment? = null
    private var chaptersFragment: DetailChaptersFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alertDialog = AlertDialog.Builder(this).create()

        binding = ActivityNovelDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupEdgeToEdgeInsets()

        val id = intent?.getLongExtra(Novel.KEY_ID, -1L)
        Timber.d("receive id: $id")
        if (id == null || id == -1L) {
            Reporter.unreachable()
            finish()
            return
        }
        novelId = id

        binding.toolbarLayout.title = id.toString()

        setupCollapsingBarAppearance()
        setupViewPager()

        binding.fabRead.setOnClickListener {
            NovelTextActivity.start(this, id)
        }

        binding.srlRefresh.isEnabled = false
        binding.srlRefresh.isRefreshing = true

        presenter = NovelDetailPresenter(id)
        presenter.attach(this)
        presenter.start()
    }

    private fun setupViewPager() {
        val tabTitles = listOf(
            getString(R.string.detail),
            getString(R.string.contents)
        )

        introductionFragment = DetailIntroductionFragment.newInstance("")
        chaptersFragment = DetailChaptersFragment.newInstance(novelId)

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> introductionFragment!!
                else -> chaptersFragment!!
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    // targetSdk 36 forces edge-to-edge, so the status/nav bars are transparent and content
    // draws behind them (window flags/statusBarColor are ignored). Rather than let the
    // CoordinatorLayout consume the top inset (its collapsing toolbar mishandles it and the
    // content over-scrolls under the toolbar), pad the OUTER root by the status-bar inset:
    // its colorAppBar background then paints a solid status-bar strip and pushes the whole
    // CoordinatorLayout below the status bar, so the collapsing toolbar sees no top inset and
    // its collapsed height stays correct. The read FAB is lifted by the nav-bar inset so it
    // clears the bottom system bar.
    private fun setupEdgeToEdgeInsets() {
        val fabBaseMargin = (binding.fabRead.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.detailRoot) { root, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            root.updatePadding(top = top)
            binding.fabRead.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = fabBaseMargin + bottom
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.detailRoot)
    }

    // True once the collapsing header is collapsed enough that the (light, in day) app-bar
    // scrim shows: then the toolbar icons + status-bar icons must match the bar; while
    // expanded they sit over the dark cover image and stay white.
    private var appBarCollapsed = false

    private fun setupCollapsingBarAppearance() {
        binding.appBar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                // Collapsed once the scrim would be (nearly) fully shown.
                val range = appBarLayout.totalScrollRange
                val collapsed = range > 0 && abs(verticalOffset) >= range - binding.toolbar.height
                if (collapsed != appBarCollapsed) {
                    appBarCollapsed = collapsed
                    applyAppBarIconAppearance()
                }
            }
        )
        applyAppBarIconAppearance()
    }

    // Toolbar icon color (navigation arrow, overflow, menu items) flips with the collapse
    // state: white over the expanded cover image, dark-on-light bar when collapsed (via
    // colorOnAppBar). The system status bar is a separate concern (see below) — it now sits
    // on the solid colorAppBar strip, not the cover image, so it follows DayNight only.
    private fun applyAppBarIconAppearance() {
        val iconColor = if (appBarCollapsed) {
            ContextCompat.getColor(this, R.color.colorOnAppBar)
        } else {
            Color.WHITE
        }
        binding.toolbar.navigationIcon?.setTint(iconColor)
        // The overflow (3-dots) icon is a separate drawable from the menu-item icons.
        binding.toolbar.overflowIcon?.mutate()?.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        binding.toolbar.menu?.let { menu ->
            for (i in 0 until menu.size()) {
                menu.getItem(i).icon?.mutate()?.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
            }
        }
        // Status-bar icons follow DayNight, independent of collapse: the status bar is the
        // solid colorAppBar strip (light in day / dark at night), never the cover image, so
        // day → dark icons, night → light icons.
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = resources.getBoolean(R.bool.appBarLightStatusBar)
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    fun showNovelDetail(novel: Novel) {
        binding.srlRefresh.isRefreshing = false
        val wasNull = this.novel == null
        this.novel = novel
        if (wasNull) invalidateOptionsMenu()
        binding.toolbarLayout.title = novel.name

        binding.tvAuthor.text = novel.author
        binding.tvSite.text = if (novel.isLocalNovel) getString(R.string.local_novel) else novel.site
        if (novel.chaptersCount > 0) {
            binding.tvChapterCount.text = getString(R.string.chapter_count_format, novel.chaptersCount)
        }

        introductionFragment?.updateText(novel.introduction)

        if (novel.image == noCover) {
            binding.image.setImageResource(R.mipmap.no_cover)
        } else {
            Glide.with(this.applicationContext)
                    .load(novel.image)
                    .apply(RequestOptions().apply {
                        error(R.mipmap.no_cover)
                    })
                    .into(binding.image)
        }
        binding.fabRead.setOnClickListener {
            NovelTextActivity.start(this, novel)
        }
        binding.fabStar.isChecked = novel.bookshelf
        binding.fabStar.setOnClickListener {
            binding.fabStar.toggle()
            presenter.updateBookshelf(binding.fabStar.isChecked)
        }
    }

    fun showError(message: String, e: Throwable? = null) {
        binding.srlRefresh.isRefreshing = false
        if (e == null) {
            alert(alertDialog, message)
        } else {
            alertError(alertDialog, message, e)
        }
    }

    private fun refresh() {
        binding.srlRefresh.isRefreshing = true
        presenter.refresh()
    }

    private fun share() {
        presenter.share()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.browse -> presenter.browse()
            R.id.refresh -> refresh()
            R.id.share -> share()
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        val isLocal = novel?.isLocalNovel == true
        menu.findItem(R.id.share)?.isVisible = !isLocal
        menu.findItem(R.id.browse)?.isVisible = !isLocal
        // Tint the freshly-inflated icons to match the current collapse state.
        applyAppBarIconAppearance()
        return true
    }

    fun shareNovelUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

}

