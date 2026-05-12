package cc.aoeiuv020.panovel.booklist

import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.BookList
import cc.aoeiuv020.panovel.qrcode.QrCodeGenerator
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.settings.OtherSettings
import cc.aoeiuv020.panovel.share.Share
import kotlinx.coroutines.*
import timber.log.Timber

/**
 *
 * Created by AoEiuV020 on 2017.11.22-14:31:17.
 */
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
        view?.showUploading()
        scope.launch {
            try {
                val (url, qrCode) = withContext(Dispatchers.IO) {
                    val url = Share.shareBookList(bookList, OtherSettings.shareExpiration)
                    val qrCode = QrCodeGenerator.generate(url)
                    Pair(url, qrCode)
                }
                view?.showSharedUrl(url, qrCode)
            } catch (e: Exception) {
                val message = "上传书单失败，"
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
                // 干脆整个刷新，没必要找麻烦，
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
                // 干脆整个刷新，没必要找麻烦，
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
                // 干脆整个刷新，没必要找麻烦，
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
                // 干脆整个刷新，没必要找麻烦，
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
