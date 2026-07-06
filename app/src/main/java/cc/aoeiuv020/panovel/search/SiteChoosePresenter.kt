package cc.aoeiuv020.panovel.search

import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.Site
import cc.aoeiuv020.panovel.report.Reporter
import timber.log.Timber
import kotlinx.coroutines.*

class SiteChoosePresenter : Presenter<SiteChooseActivity>() {
    fun start() {
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    DataManager.listSites()
                }
                view?.showSiteList(list)
            } catch (e: Exception) {
                val message = "加载网站列表失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun enabledChange(site: Site) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.siteEnabledChange(site)
                }
            } catch (e: Exception) {
                val message = "${if (site.enabled) "启用" else "禁用"}网站失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun pinned(site: Site) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.pinned(site)
                }
            } catch (e: Exception) {
                val message = "置顶网站失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun cancelPinned(site: Site) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.cancelPinned(site)
                }
            } catch (e: Exception) {
                val message = "取消置顶网站失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }
}
