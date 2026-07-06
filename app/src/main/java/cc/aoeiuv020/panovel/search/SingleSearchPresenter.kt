@file:Suppress("DEPRECATION")


package cc.aoeiuv020.panovel.search

import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.report.Reporter
import timber.log.Timber
import kotlinx.coroutines.*
class SingleSearchPresenter(
        private val site: String
) : Presenter<SingleSearchActivity>() {
    fun start() {
        Timber.d("start,")
    }

    fun pushCookies() {
        Timber.d("pushCookies,")
        val context = DataManager.getNovelContextByName(site)
        DataManager.pushCookiesToWebView(context)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.syncCookies(view)
                }
                if (view?.getCurrentUrl() == null) {
                    Timber.d("open home page ${context.homePage}")
                    view?.openPage(context.homePage)
                }
            } catch (e: Exception) {
                val message = "传递cookies给浏览器失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun pullCookies() {
        Timber.d("pullCookies,")
        val context = DataManager.getNovelContextByName(site)
        DataManager.pullCookiesFromWebView(context)
    }

    fun open(currentUrl: String) {
        Timber.d("open <$currentUrl>,")
        scope.launch {
            try {
                val novel = withContext(Dispatchers.IO) {
                    try {
                        DataManager.getNovelFromUrl(site, currentUrl)
                                .novel
                    } catch (e: Exception) {
                        throw IllegalArgumentException("不支持的地址，", e)
                    }
                }
                view?.openNovelDetail(novel)
            } catch (e: Exception) {
                val message = "打开地址<$currentUrl>失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun removeCookies() {
        Timber.d("removeCookies,")
        DataManager.removeWebViewCookies()
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.syncCookies(view)
                    DataManager.removeNovelContextCookies(site)
                }
                view?.showRemoveCookiesDone()
            } catch (e: Exception) {
                val message = "删除cookies失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }
}
