package cc.aoeiuv020.panovel.history

import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.settings.GeneralSettings
import timber.log.Timber
import kotlinx.coroutines.*

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

    fun refresh() {
        requestHistory()
    }
}
