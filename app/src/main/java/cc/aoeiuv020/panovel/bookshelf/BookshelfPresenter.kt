package cc.aoeiuv020.panovel.bookshelf

import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.report.Reporter
import timber.log.Timber
import kotlinx.coroutines.*

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

    fun refresh() {
        requestBookshelf()
    }
}
