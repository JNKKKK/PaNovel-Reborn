@file:Suppress("DEPRECATION")

package cc.aoeiuv020.panovel.detail

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cc.aoeiuv020.panovel.IView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.share.Share
import cc.aoeiuv020.panovel.text.NovelTextActivity
import cc.aoeiuv020.panovel.databinding.ActivityNovelDetailBinding
import cc.aoeiuv020.panovel.util.alert
import cc.aoeiuv020.panovel.util.alertError
import cc.aoeiuv020.panovel.util.noCover
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.AppBarLayout
import android.content.Intent
import timber.log.Timber

/**
 *
 * Created by AoEiuV020 on 2017.10.03-18:10:37.
 */
class NovelDetailActivity : AppCompatActivity(), IView {
    companion object {
        fun start(ctx: Context, novel: Novel) {
            ctx.startActivity(Intent(ctx, NovelDetailActivity::class.java).putExtra(Novel.KEY_ID, novel.nId))
        }
    }

    private lateinit var binding: ActivityNovelDetailBinding
    private lateinit var alertDialog: AlertDialog
    private lateinit var presenter: NovelDetailPresenter
    private var novel: Novel? = null
    private var isRefreshEnable = false

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


        binding.toolbarLayout.title = id.toString()

        binding.fabRead.setOnClickListener {
            NovelTextActivity.start(this, id)
        }

        binding.srlRefresh.setOnRefreshListener {
            refresh()
        }
        // 拉到顶部才允许下拉刷新，
        // 为了支持内部嵌套列表，
        binding.srlRefresh.setOnChildScrollUpCallback { _, _ -> !isRefreshEnable }
        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _: AppBarLayout, verticalOffset: Int ->
            isRefreshEnable = verticalOffset == 0
        })
        binding.srlRefresh.isRefreshing = true

        presenter = NovelDetailPresenter(id)
        presenter.attach(this)
        presenter.start()
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    fun showNovelDetail(novel: Novel) {
        binding.srlRefresh.isRefreshing = false
        this.novel = novel
        binding.toolbarLayout.title = novel.name
        // TODO: 调整上半部分展示内容，作者名网站名什么都加上，
        // TODO: 下面考虑用viewPager两页实现简介和目录，
        binding.tvIntroduction.text = novel.introduction
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
        return true
    }

    fun showSharedUrl(url: String, qrCode: String) {
        Share.alert(this, url, qrCode)
    }

}

