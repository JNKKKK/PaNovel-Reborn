package cc.aoeiuv020.panovel.history

import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.report.Reporter
import timber.log.Timber
import kotlinx.coroutines.*

class HistoryPresenter : Presenter<HistoryFragment>() {

    private fun requestHistory() {
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    // 历史记录数以前是可配置的，现在固定展示最近30条，
                    DataManager.history(30)
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
