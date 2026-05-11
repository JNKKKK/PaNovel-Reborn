package cc.aoeiuv020.panovel.history

import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.NovelManager
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.settings.GeneralSettings
import timber.log.Timber
import kotlinx.coroutines.*

/**
 * 绝大部分照搬书架，
 * Created by AoEiuV020 on 2017.10.15-18:11:15.
 */
class HistoryPresenter : Presenter<HistoryFragment>() {

    private fun requestHistory() {
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    DataManager.history(GeneralSettings.historyCount)
                }
                view?.showNovelList(list)
            } catch (e: Exception) {
                val message = "获取历史列表失败，"
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
        requestHistory()
    }
}
