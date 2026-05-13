package cc.aoeiuv020.panovel.backup

import android.net.Uri
import android.os.Build
import android.os.Environment
import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.util.PrefContext
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.backup.webdav.BackupWebDavHelper
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.settings.LocationSettings
import cc.aoeiuv020.panovel.util.notNullOrReport
import cc.aoeiuv020.regex.pick
import java.io.File
import java.io.FileNotFoundException
import java.io.FilenameFilter
import java.io.IOException
import java.util.*
import timber.log.Timber
import kotlinx.coroutines.*

/**
 * Created by AoEiuV020 on 2018.05.11-12:39:10.
 */
class BackupPresenter : Presenter<BackupActivity>() {
    companion object {
        const val NAME_FOLDER = "Backup"
        private const val NAME_TEMPLATE = "PaNovel-Backup-##.zip"
        val NAME_FORMAT = NAME_TEMPLATE.replace("##", "%d")
        private val NAME_MATCHER = Regex(NAME_TEMPLATE.replace("##", "(\\d+)"))
        val NAME_PATTERN = NAME_MATCHER.pattern
        val FILENAME_FILTER = FilenameFilter { _, name ->
            name.matches(NAME_MATCHER)
        }
    }

    private val context = PrefContext.appContext

    private val backupManager = BackupManager()
    private val backupHelperMap: Map<Int, BackupHelper> = mapOf(R.id.rbDefaultWebDav to BackupWebDavHelper())

    fun start() {
        scope.launch {
            try {
                val baseFile = File(LocationSettings.backupLocation)
                        .apply { exists() || mkdirs() }
                        .takeIf { it.canWrite() }
                        ?: context.filesDir
                                .resolve(NAME_FOLDER)
                                .apply { exists() || mkdirs() }
                val indexList: List<Int> = (baseFile.list(FILENAME_FILTER) ?: emptyArray())
                        .map {
                            val (index) = it.pick(NAME_PATTERN)
                            index.toInt()
                        }.sorted()
                val max = indexList.lastOrNull() ?: 1
                val next = max + 1
                val defaultOldName = String.format(Locale.ENGLISH, NAME_FORMAT, max)
                val defaultOldUri = baseFile.resolve(defaultOldName).let { Uri.fromFile(it) }.toString()
                val defaultNewName = String.format(Locale.ENGLISH, NAME_FORMAT, next)
                val defaultNewUri = baseFile.resolve(defaultNewName).let { Uri.fromFile(it) }.toString()

                view?.showDefaultPath(defaultOldUri, defaultNewUri)

                val defaultOtherName = String.format(Locale.ENGLISH, NAME_FORMAT, 1)
                val defaultOtherUri = Environment.getExternalStorageDirectory()
                        .resolve(defaultOtherName)
                        .let { Uri.fromFile(it) }
                        .toString()
                view?.showOtherPath(defaultOtherUri)

                backupHelperMap.forEach { entry ->
                    if (entry.value.ready()) {
                        view?.showBackupHint(entry.key, entry.value.configPreview())
                    }
                }
            } catch (e: Exception) {
                val message = "寻找路径失败，"
                Reporter.post(message, e)
                view?.runOnUiThread {
                    view?.showError(message, e)
                }
            }
        }
    }

    fun import() {
        scope.launch {
            try {
                val options = view.notNullOrReport().getCheckedOption()
                val restore: (File) -> Unit
                val selectedHelper: BackupHelper? = view.notNullOrReport().getSelectedId().let { backupHelperMap[it] }
                if (selectedHelper != null) {
                    if (selectedHelper.ready()) {
                        Timber.d("import: ${selectedHelper.type}")
                        restore = { file: File -> selectedHelper.restore(file) }
                    } else {
                        view?.startConfig(selectedHelper)
                        throw IllegalStateException("先前往配置")
                    }
                } else {
                    val uri: Uri = view.notNullOrReport().getSelectPath()
                    Timber.d("import: $uri")
                    restore = { tempFile: File ->
                        try {
                            context.contentResolver.openInputStream(uri)
                        } catch (e: FileNotFoundException) {
                            if (e.message?.contains("Permission denied") == true
                                    || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()))) {
                                view?.requestPermissions()
                                throw IllegalStateException("没有权限，", e)
                            } else {
                                throw IOException("文件不存在或不可读", e)
                            }
                        }.notNullOrReport().use { input ->
                            Timber.d("开始导入，")
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                                output.flush()
                            }
                        }
                    }
                }
                val result = backupManager.import(options, restore)
                view?.showImportSuccess(result)
            } catch (e: Exception) {
                val message = "导入失败，"
                Timber.e(e, message)
                Reporter.post(message, e)
                view?.runOnUiThread {
                    view?.showError(message, e)
                }
            }
        }
    }

    fun export() {
        scope.launch {
            try {
                val options = view.notNullOrReport().getCheckedOption()
                val backup: (File) -> Unit
                val selectedExportHelper: BackupHelper? = view.notNullOrReport().getSelectedId().let { backupHelperMap[it] }
                if (selectedExportHelper != null) {
                    if (selectedExportHelper.ready()) {
                        Timber.d("export: ${selectedExportHelper.type}")
                        backup = { file: File -> selectedExportHelper.backup(file) }
                    } else {
                        view?.startConfig(selectedExportHelper)
                        throw IllegalStateException("先前往配置")
                    }
                } else {
                    val uri: Uri = view.notNullOrReport().getSelectPath()
                    Timber.d("export: $uri")
                    backup = { tempFile: File ->
                        try {
                            context.contentResolver.openOutputStream(uri)
                        } catch (e: FileNotFoundException) {
                            if (e.message?.contains("Permission denied") == true
                                    || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()))) {
                                view?.requestPermissions()
                                throw IllegalStateException("没有权限，", e)
                            } else {
                                throw IOException("文件不可写", e)
                            }
                        } catch (e: SecurityException) {
                            view?.requestPermissions()
                            throw IllegalStateException("没有权限，", e)
                        }.notNullOrReport().use { output ->
                            // 这里貌似不会抛没权限的异常，
                            Timber.d("开始导出，")
                            tempFile.inputStream().use { input ->
                                input.copyTo(output)
                                output.flush()
                            }
                        }
                    }
                }
                val result = backupManager.export(options, backup)
                view?.showExportSuccess(result)
            } catch (e: Exception) {
                val message = "导出失败，"
                Timber.e(e, message)
                Reporter.post(message, e)
                view?.runOnUiThread {
                    view?.showError(message, e)
                }
            }
        }
    }

    fun getHelper(id: Int): BackupHelper? {
        return backupHelperMap[id]
    }
}