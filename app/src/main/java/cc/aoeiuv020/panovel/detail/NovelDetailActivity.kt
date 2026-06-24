@file:Suppress("DEPRECATION")

package cc.aoeiuv020.panovel.detail

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
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

/**
 *
 * Created by AoEiuV020 on 2017.10.03-18:10:37.
 */
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

        val id = intent?.getLongExtra(Novel.KEY_ID, -1L)
        Timber.d("receive id: $id")
        if (id == null || id == -1L) {
            Reporter.unreachable()
            finish()
            return
        }
        novelId = id

        binding.toolbarLayout.title = id.toString()

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
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        val isLocal = novel?.isLocalNovel == true
        menu.findItem(R.id.share)?.isVisible = !isLocal
        menu.findItem(R.id.browse)?.isVisible = !isLocal
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

