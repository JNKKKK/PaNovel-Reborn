package cc.aoeiuv020.panovel.search

import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.settings.GeneralSettings
import cc.aoeiuv020.panovel.util.ChineseNormalizer
import timber.log.Timber
import kotlinx.coroutines.*
/**
 *
 * Created by AoEiuV020 on 2017.10.22-18:18:58.
 */
class FuzzySearchPresenter : Presenter<FuzzySearchActivity>() {
    var name: String? = null
    private var author: String? = null
    private var site: String? = null
    private var searchJob: Job? = null

    fun singleSite(site: String) {
        this.site = site
    }

    fun search(name: String, author: String? = null) {
        this.name = name
        this.author = author
        searchActual(name, author)
    }

    private fun searchActual(name: String, author: String?) {
        Timber.d("search <$name, $author>")
        searchJob?.cancel()
        searchJob = scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    site?.let {
                        DataManager.search(it, name, author)
                    } ?: run {
                        val sites = DataManager.listSites().filter { it.enabled }
                        val ite = sites.iterator()
                        val jobs = List(GeneralSettings.searchThreadsLimit) {
                            async(Dispatchers.IO) {
                                while (view != null && synchronized(ite) { ite.hasNext() }) {
                                    val site = synchronized(ite) { ite.next() }
                                    Timber.d("${Thread.currentThread().name} search ${site.name}")
                                    try {
                                        val novelManagers = DataManager.search(site.name, name, author).filter {
                                            val novel = it.novel
                                            if (author == null) {
                                                ChineseNormalizer.normalize(novel.name).lowercase()
                                                    .contains(ChineseNormalizer.normalize(name).lowercase())
                                            } else {
                                                novel.name == name && novel.author == author
                                            }
                                        }
                                        withContext(Dispatchers.Main) {
                                            view?.addResult(novelManagers)
                                        }
                                    } catch (e: Exception) {
                                        val message = "搜索<${site.name}, $name, $author>失败，"
                                        Reporter.post(message, e)
                                        // 单个网站搜索失败不中断，
                                    }
                                }
                            }
                        }
                        jobs.forEach { it.await() }
                        null
                    }
                }
                result?.let { view?.addResult(it) }
                view?.showOnComplete()
            } catch (e: Exception) {
                val message = "搜索<$name, $author>失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }
}
