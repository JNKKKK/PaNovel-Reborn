package cc.aoeiuv020.panovel.booklist

import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.report.Reporter
import kotlinx.coroutines.*
import timber.log.Timber

/**
 *
 * Created by AoEiuV020 on 2017.11.22-15:47:37.
 */
class BookListActivityPresenter(private val bookListId: Long) : Presenter<BookListActivity>() {

    fun start() {
        scope.launch {
            try {
                val bookList = withContext(Dispatchers.IO) {
                    DataManager.getBookList(bookListId)
                }
                view?.showBookList(bookList)
            } catch (e: Exception) {
                val message = "查找书单<$bookListId>失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showBookListNotFound(message, e)
            }
        }
    }

    fun refresh() {
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    DataManager.getNovelManagerFromBookList(bookListId)
                }
                view?.showNovelList(list)
            } catch (e: Exception) {
                val message = "读取书单<$bookListId>中的小说列表失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun askUpdate(novelList: List<NovelManager>) {
        scope.launch {
            try {
                val resultList = withContext(Dispatchers.IO) {
                    DataManager.askUpdate(novelList)
                }
                view?.showAskUpdateResult(resultList)
            } catch (e: Exception) {
                val message = "询问服务器小说列表更新失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.askUpdateError(message, e)
            }
        }
    }

    fun add(novelManager: NovelManager) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    novelManager.addToBookList(bookListId)
                }
            } catch (e: Exception) {
                val message = "添加小说到书单<$bookListId>失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun remove(novelManager: NovelManager) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    novelManager.removeFromBookList(bookListId)
                }
            } catch (e: Exception) {
                val message = "从书单<$bookListId>删除小说失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun addFromHistory() {
        scope.launch {
            try {
                val (list, nameArray, containsArray) = withContext(Dispatchers.IO) {
                    val list = DataManager.history(50)
                    val nameArray = list.map { it.novel.bookId }.toTypedArray()
                    val containsArray = DataManager.inBookList(bookListId, list).toBooleanArray()
                    Triple(list, nameArray, containsArray)
                }
                view?.selectToAdd(list, nameArray, containsArray)
            } catch (e: Exception) {
                val message = "查询历史记录中的小说在书单<$bookListId>中的包含情况失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun addFromBookshelf() {
        scope.launch {
            try {
                val (list, nameArray, containsArray) = withContext(Dispatchers.IO) {
                    val list = DataManager.listBookshelf()
                    val nameArray = list.map { it.novel.bookId }.toTypedArray()
                    val containsArray = DataManager.inBookList(bookListId, list).toBooleanArray()
                    Triple(list, nameArray, containsArray)
                }
                view?.selectToAdd(list, nameArray, containsArray)
            } catch (e: Exception) {
                val message = "查询书架中的小说在书单<$bookListId>中的包含情况失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }
}
