package cc.aoeiuv020.panovel.booklist

import android.graphics.Bitmap
import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.BookList
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.share.Share
import kotlinx.coroutines.*
import timber.log.Timber

class BookListOverviewPresenter : Presenter<BookListFragment>() {
    fun refresh() {
        requestBookListList()
    }

    private fun requestBookListList() {
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    DataManager.allBookList()
                }
                view?.showBookListList(list)
            } catch (e: Exception) {
                val message = "查询书单列表失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun shareBookList(bookList: BookList) {
        scope.launch {
            try {
                val (encodedText, qrBitmap) = withContext(Dispatchers.IO) {
                    val encoded = Share.encode(bookList)
                    val bitmap = Share.generateQrBitmap(encoded)
                    Pair(encoded, bitmap)
                }
                view?.showShareResult(encodedText, qrBitmap)
            } catch (e: Exception) {
                val message = "分享书单失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun renameBookList(bookList: BookList, newName: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.renameBookList(bookList, newName)
                }
                view?.refresh()
            } catch (e: Exception) {
                val message = "书单重命名失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun copyBookList(bookList: BookList, newName: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.copyBookList(bookList, newName)
                }
                view?.refresh()
            } catch (e: Exception) {
                val message = "书单复制失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun remove(bookList: BookList) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.removeBookList(bookList)
                }
                view?.refresh()
            } catch (e: Exception) {
                val message = "删除书单失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun newBookList(name: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.newBookList(name)
                }
                view?.refresh()
            } catch (e: Exception) {
                val message = "添加书单失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun removeBookshelf(bookList: BookList) {
        view?.showRemoving()
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.removeFromBookshelf(bookList)
                }
                view?.showRemoveBookshelfComplete()
            } catch (e: Exception) {
                val message = "从书架移出书单中的小说失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun addBookshelf(bookList: BookList) {
        view?.showAdding()
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataManager.addToBookshelf(bookList)
                }
                view?.showAddBookshelfComplete()
            } catch (e: Exception) {
                val message = "加入书单中的小说到书架失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }
}
