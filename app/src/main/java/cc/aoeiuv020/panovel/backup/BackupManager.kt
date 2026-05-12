package cc.aoeiuv020.panovel.backup

import android.content.Context
import cc.aoeiuv020.panovel.util.PrefContext
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import java.io.File

class BackupManager {
    companion object {
        const val NAME_TEMP = "PaNovel-Backup-00.zip"
        const val FOLDER_TEMP = "PaNovel-Backup-00"
        const val NAME_VERSION = "version"
        const val CURRENT_VERSION = 4
    }

    private val context: Context = PrefContext.appContext
    private val backup: Backup = BackupImpl()

    private fun getTempFile() =
        context.cacheDir.resolve(NAME_TEMP).apply { exists() && delete() }

    private fun getTempFolder() =
        context.cacheDir.resolve(FOLDER_TEMP)
            .apply { exists() && deleteRecursively() }
            .apply { mkdirs() }

    @Synchronized
    fun import(options: Set<BackupOption>, restore: (File) -> Unit): String {
        val folder = getTempFolder()
        val tempFile = getTempFile()
        try {
            restore(tempFile)
        } catch (e: Exception) {
            throw IllegalStateException("恢复失败，" + e.message, e)
        }
        try {
            val zipFile = ZipFile(tempFile)
            zipFile.extractAll(folder.canonicalPath)
            tempFile.delete()
        } catch (e: ZipException) {
            throw IllegalStateException("zip文件解压失败，" + e.message, e)
        }
        val result = backup.import(folder, options)
        folder.deleteRecursively()
        return result
    }

    @Synchronized
    fun export(options: Set<BackupOption>, backupTo: (File) -> Unit): String {
        val folder = getTempFolder()
        folder.resolve(NAME_VERSION).writeText(CURRENT_VERSION.toString())
        val result = backup.export(folder, options)

        val tempFile = getTempFile()
        val zipFile = ZipFile(tempFile)
        zipFile.addFolder(folder, ZipParameters().apply {
            isIncludeRootFolder = false
        })
        folder.deleteRecursively()

        try {
            backupTo(tempFile)
        } catch (e: Exception) {
            throw IllegalStateException("备份失败，" + e.message, e)
        }
        tempFile.delete()
        return result
    }
}