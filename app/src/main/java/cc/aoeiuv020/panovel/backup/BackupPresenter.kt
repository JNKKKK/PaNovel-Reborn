package cc.aoeiuv020.panovel.backup

import android.net.Uri
import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.util.PrefContext
import cc.aoeiuv020.panovel.util.notNullOrReport
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File

/**
 * Created by AoEiuV020 on 2018.05.11-12:39:10.
 */
class BackupPresenter : Presenter<BackupActivity>() {

    private val context = PrefContext.appContext

    private val backupManager = BackupManager()

    /**
     * 从用户选择的备份文件恢复，
     */
    fun import(uri: Uri) {
        scope.launch {
            try {
                Timber.d("import: $uri")
                val result = withContext(Dispatchers.IO) {
                    backupManager.import { tempFile: File ->
                        context.contentResolver.openInputStream(uri)
                            .notNullOrReport(uri.toString())
                            .use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                    output.flush()
                                }
                            }
                    }
                }
                view?.showImportSuccess(result)
            } catch (e: Exception) {
                val message = "导入失败，"
                Timber.e(e, message)
                Reporter.post(message, e)
                view?.showError(message, e)
            }
        }
    }

    /**
     * 按勾选的内容导出备份到用户选择的位置，
     */
    fun export(options: Set<BackupOption>, uri: Uri) {
        scope.launch {
            try {
                Timber.d("export: $uri, options: $options")
                val result = withContext(Dispatchers.IO) {
                    backupManager.export(options) { tempFile: File ->
                        context.contentResolver.openOutputStream(uri)
                            .notNullOrReport(uri.toString())
                            .use { output ->
                                tempFile.inputStream().use { input ->
                                    input.copyTo(output)
                                    output.flush()
                                }
                            }
                    }
                }
                view?.showExportSuccess(result)
            } catch (e: Exception) {
                val message = "导出失败，"
                Timber.e(e, message)
                Reporter.post(message, e)
                view?.showError(message, e)
            }
        }
    }
}
