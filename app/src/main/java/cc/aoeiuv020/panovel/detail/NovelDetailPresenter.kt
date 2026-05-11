package cc.aoeiuv020.panovel.detail

import android.content.Intent
import android.net.Uri
import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.qrcode.QrCodeManager
import cc.aoeiuv020.panovel.report.Reporter
import timber.log.Timber
import kotlinx.coroutines.*

/**
 *
 * Created by AoEiuV020 on 2017.10.03-18:10:45.
 */
class NovelDetailPresenter(
        id: Long
) : Presenter<NovelDetailActivity>() {
    // 第一次使用要在异步线程，
    private val novelManager: NovelManager by lazy {
        DataManager.getNovelManager(id)
    }
    private val novel: Novel get() = novelManager.novel

    fun start() {
        requestDetail(false)
    }

    fun refresh() {
        requestDetail(true)
    }

    private fun requestDetail(refresh: Boolean) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 这里初始化novelManager，有数据库查询所以必须异步，
                    val novelManager = novelManager
                    if (!refresh) {
                        // 打开首次加载时先展示本地数据，
                        withContext(Dispatchers.Main) {
                            view?.showNovelDetail(novelManager.novel)
                        }
                    }
                    novelManager.requestDetail(refresh)
                }
                view?.showNovelDetail(novelManager.novel)
            } catch (e: Exception) {
                val message = "获取小说<${novel.bookId}>详情失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun share() {
        scope.launch {
            try {
                val (url, qrCode) = withContext(Dispatchers.IO) {
                    val url = novelManager.getDetailUrl()
                    val qrCode = QrCodeManager.generate(url)
                    url to qrCode
                }
                view?.showSharedUrl(url, qrCode)
            } catch (e: Exception) {
                val message = "获取小说<${novel.bookId}>详情页地址失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun browse() {
        try {
            val url = novelManager.getDetailUrl()
            // 只支持打开网络地址，本地小说不支持调用其他app打开，
            url.takeIf { it.startsWith("http") }
                    ?.also { view?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    ?: view?.showError("本地小说不支持外部打开，")
        } catch (e: Exception) {
            val message = "获取小说《${novel.name}》<${novel.site}, ${novel.detail}>详情页地址失败，"
            // 按理说每个网站的extra都是设计好的，可以得到完整地址的，
            Reporter.post(message, e)
            Timber.e(e, message)
            view?.showError(message, e)
        }
    }

    fun updateBookshelf(checked: Boolean) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    novelManager.updateBookshelf(checked)
                }
            } catch (e: Exception) {
                val message = "${if (novel.bookshelf) "添加" else "删除"}书架《${novel.name}》失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }
}
