package cc.aoeiuv020.panovel.text

import android.content.Context
import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.report.Reporter
import timber.log.Timber
import kotlinx.coroutines.*

/**
 *
 * Created by AoEiuV020 on 2017.10.03-19:06:50.
 */
class NovelTextPresenter(
        id: Long
) : Presenter<NovelTextActivity>() {
    private val novelManager: NovelManager by lazy {
        DataManager.getNovelManager(id)
    }
    private val novel: Novel get() = novelManager.novel

    fun start() {
        requestNovel()
    }

    private fun requestNovel() {
        scope.launch {
            try {
                val novel = withContext(Dispatchers.IO) {
                    novelManager.novel
                }
                view?.showNovel(novel)
            } catch (e: Exception) {
                val message = "获取小说详情失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showNovelNotFound(message, e)
            }
        }
    }

    fun requestContent(index: Int, chapter: NovelChapter, refresh: Boolean): List<String> {
        return novelManager.requestContent(index, chapter, refresh)
    }

    fun askDownload(context: Context, currentIndex: Int) {
        view?.also {
            DataManager.download.askDownload(context, novelManager, currentIndex, false)
        }
    }

    fun requestChapters(refresh: Boolean = false) {
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    novelManager.requestChapters(refresh)
                }
                view?.showChaptersAsc(list)
            } catch (e: Exception) {
                val message = "加载小说<${novel.bookId}>章节列表失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun updateBookshelf(checked: Boolean) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    novelManager.updateBookshelf(checked)
                }
            } catch (e: Exception) {
                val message = "${if (novel.bookshelf) "添加" else "删除"}书架《${novel.bookId}》失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun saveReadStatus(novel: Novel) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    novelManager.saveReadStatus()
                }
            } catch (e: Exception) {
                val message = "保存<${novel.bookId}>阅读进度失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun getContentUrl(chapter: NovelChapter): String {
        return novelManager.getContentUrl(chapter)
    }

    fun loadContents() {
        scope.launch {
            try {
                val cachedList = withContext(Dispatchers.IO) {
                    novelManager.novelContentsCached()
                }
                view?.showContents(cachedList)
            } catch (e: Exception) {
                val message = "加载小说正文缓存列表失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun getDetailUrl(): String {
        return novelManager.getDetailUrl()
    }
}
