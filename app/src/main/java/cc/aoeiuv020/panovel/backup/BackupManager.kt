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

        // 备份格式版本，
        // 5起改为新格式：小说连同章节列表和缓存正文一起备份，按内容(书架/书单/历史)选择，
        // 不再兼容5以前的旧格式，
        const val CURRENT_VERSION = 5
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
    fun import(restore: (File) -> Unit): String {
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
        val version = folder.resolve(NAME_VERSION).takeIf { it.exists() }
            ?.readText()?.trim()?.toIntOrNull() ?: 0
        if (version != CURRENT_VERSION) {
            folder.deleteRecursively()
            throw IllegalStateException(
                "备份版本不兼容（备份版本$version，当前支持$CURRENT_VERSION），请用同版本应用导出的备份，"
            )
        }
        val result = backup.import(folder)
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
