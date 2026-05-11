package cc.aoeiuv020.panovel.bookshelf

import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.report.Reporter
import timber.log.Timber
import kotlinx.coroutines.*

/**
 *
 * Created by AoEiuV020 on 2017.10.14-21:54.
 */
class BookshelfPresenter : Presenter<BookshelfFragment>() {

    private fun requestBookshelf() {
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    DataManager.listBookshelf()
                }
                view?.showNovelList(list)
            } catch (e: Exception) {
                val message = "获取书架列表失败，"
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

    fun refresh() {
        requestBookshelf()
    }
}
