package cc.aoeiuv020.panovel.backup

import android.net.Uri
import cc.aoeiuv020.json.AppJson
import cc.aoeiuv020.panovel.backup.BackupOption.*
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.NovelMinimal
import cc.aoeiuv020.panovel.data.entity.NovelWithProgressAndPinnedTime
import cc.aoeiuv020.panovel.settings.*
import cc.aoeiuv020.panovel.share.Share
import cc.aoeiuv020.panovel.util.Pref
import cc.aoeiuv020.panovel.util.PrefContext
import cc.aoeiuv020.panovel.util.notNullOrReport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import timber.log.Timber
import java.io.File
import java.util.*

class BackupImpl : Backup {

    @Synchronized
    override fun import(base: File, options: Set<BackupOption>): String {
        Timber.d("import from $base\n enable $options")
        val sb = StringBuilder()
        options.forEach { option ->
            val name = getOptionName(option)
            try {
                val count = import(base.resolve(option.name), option)
                sb.appendLine("成功导入$name: <$count>条，")
            } catch (e: Exception) {
                Timber.e(e, "读取[$name]失败，")
            }
        }
        return sb.toString()
    }

    @Synchronized
    override fun export(base: File, options: Set<BackupOption>): String {
        Timber.d("export to $base\n enable $options")
        val sb = StringBuilder()
        options.forEach { option ->
            val name = getOptionName(option)
            try {
                val count = export(base.resolve(option.name), option)
                sb.appendLine("成功导出$name: <$count>条，")
            } catch (e: Exception) {
                Timber.e(e, "写入[$name]失败，")
            }
        }
        return sb.toString()
    }

    private fun getOptionName(option: BackupOption): String = when (option) {
        Bookshelf -> "书架"
        BookList -> "书单"
        Progress -> "进度"
        Settings -> "设置"
    }

    private fun import(file: File, option: BackupOption): Int {
        Timber.d("import $option from $file")
        return when (option) {
            Bookshelf -> importBookshelf(file)
            BookList -> importBookList(file)
            Progress -> importProgress(file)
            Settings -> importSettings(file)
        }
    }

    private fun export(file: File, option: BackupOption): Int {
        Timber.d("export $option to $file")
        return when (option) {
            Bookshelf -> exportBookshelf(file)
            BookList -> exportBookList(file)
            Progress -> exportProgress(file)
            Settings -> exportSettings(file)
        }
    }

    private fun importProgress(file: File): Int {
        return file.useLines { s ->
            s.map { line ->
                val a = line.split(',')
                NovelWithProgressAndPinnedTime(
                    a[0], a[1], a[2], a[3],
                    a[4].toInt(), a[5].toInt(),
                    Date(a[6].toLong())
                )
            }.let {
                DataManager.importNovelWithProgress(it)
            }
        }
    }

    private fun importSettings(folder: File): Int {
        val list = folder.listFiles()
        return list.notNullOrReport().sumOf { file ->
            when (file.name) {
                "Ad" -> 0
                // 全局设置已移除，旧备份里的General忽略，
                "General" -> 0
                // 列表设置已移除，旧备份里的List忽略，
                "List" -> 0
                // 杂项设置已移除，旧备份里的Other忽略，
                "Other" -> 0
                "Reader" -> importPref(ReaderSettings, file)
                "Download" -> importPref(DownloadSettings, file)
                "Interface" -> 0
                // 路径设置已移除，旧备份里的Location忽略，
                "Location" -> 0
                "Server" -> 0
                "Reader_BatteryMargins" -> importPref(ReaderSettings.batteryMargins, file)
                "Reader_BookNameMargins" -> importPref(ReaderSettings.bookNameMargins, file)
                "Reader_ChapterNameMargins" -> importPref(ReaderSettings.chapterNameMargins, file)
                "Reader_ContentMargins" -> importPref(ReaderSettings.contentMargins, file)
                "Reader_PaginationMargins" -> importPref(ReaderSettings.paginationMargins, file)
                "Reader_TimeMargins" -> importPref(ReaderSettings.timeMargins, file)
                "backgroundImage" -> 1.also { ReaderSettings.backgroundImage = Uri.fromFile(file) }
                "lastBackgroundImage" -> 1.also {
                    ReaderSettings.lastBackgroundImage = Uri.fromFile(file)
                }
                "font" -> 1.also { ReaderSettings.font = Uri.fromFile(file) }
                else -> 0
            }
        }
    }

    private fun importPref(pref: Pref, file: File): Int {
        val editor = pref.sharedPreferences.edit()
        var count = 0
        AppJson.parseToJsonElement(file.readText()).jsonObject.forEach { (key, value) ->
            val prim = value.jsonPrimitive
            when (key) {
                "animationMode" -> editor.putString(key, prim.content)
                "onCheckUpdateClick" -> editor.putString(key, prim.content)
                "onDotClick" -> editor.putString(key, prim.content)
                "onDotLongClick" -> editor.putString(key, prim.content)
                "onItemClick" -> editor.putString(key, prim.content)
                "onItemLongClick" -> editor.putString(key, prim.content)
                "onLastChapterClick" -> editor.putString(key, prim.content)
                "onNameClick" -> editor.putString(key, prim.content)
                "onNameLongClick" -> editor.putString(key, prim.content)

                "adEnabled" -> {}
                "keepScreenOn" -> editor.putBoolean(key, prim.boolean)
                "fullScreen" -> editor.putBoolean(key, prim.boolean)
                // 已固定不可配置，旧备份里的值忽略，
                "backPressOutOfFullScreen" -> {}
                // 已移除该设置，旧备份里的值忽略，
                "fullScreenClickNextPage" -> {}
                "fitWidth" -> editor.putBoolean(key, prim.boolean)
                "fitHeight" -> editor.putBoolean(key, prim.boolean)
                "volumeKeyScroll" -> editor.putBoolean(key, prim.boolean)
                "animationSpeed" -> editor.putFloat(key, prim.float)
                "centerPercent" -> editor.putFloat(key, prim.float)
                // 已固定不可配置，旧备份里的值忽略，
                "autoSaveReadStatus" -> {}
                "brightness" -> editor.putInt(key, prim.int)
                "autoRefreshInterval" -> editor.putInt(key, prim.int)
                "backgroundColor" -> editor.putInt(key, prim.int)
                "lastBackgroundColor" -> editor.putInt(key, prim.int)
                "downloadThreadsLimit" -> DownloadSettings.downloadThreadsLimit = prim.int
                "autoDownloadCount" -> DownloadSettings.autoDownloadCount = prim.int
                // 已固定不可配置，旧备份里的值忽略，
                "fullScreenDelay" -> {}
                "lineSpacing" -> editor.putInt(key, prim.int)
                "messageSize" -> editor.putInt(key, prim.int)
                "paragraphSpacing" -> editor.putInt(key, prim.int)
                "textColor" -> editor.putInt(key, prim.int)
                "lastTextColor" -> editor.putInt(key, prim.int)
                "textSize" -> editor.putInt(key, prim.int)
                // 已固定不可配置，旧备份里的值忽略，
                "dateFormat" -> {}
                "segmentIndentationCount" -> editor.putString(key, prim.content)
                "enabled" -> editor.putBoolean(key, prim.boolean)
                "dotNotifyUpdate" -> --count
                "bottom" -> editor.putInt(key, prim.int)
                "left" -> editor.putInt(key, prim.int)
                "right" -> editor.putInt(key, prim.int)
                "top" -> editor.putInt(key, prim.int)
                else -> --count
            }
            ++count
        }
        editor.apply()
        return count
    }

    private fun importBookList(folder: File): Int =
        folder.listFiles().notNullOrReport().sumOf { file ->
            val bookListBean = Share.importBookListJson(file.readText())
            DataManager.importBookList(
                bookListBean.name,
                bookListBean.list,
                bookListBean.uuid
            )
            bookListBean.list.size
        }

    private fun importBookshelf(file: File): Int {
        val list: List<NovelMinimal> = AppJson.decodeFromString(file.readText())
        DataManager.importBookshelf(list)
        return list.size
    }

    private fun exportProgress(file: File): Int {
        val list = DataManager.exportNovelProgress().map {
            NovelWithProgressAndPinnedTime(it)
        }
        var count = 0
        file.outputStream().bufferedWriter().use { output ->
            list.forEach { n ->
                if (n.readAtChapterIndex > 0 || n.readAtTextIndex > 0) {
                    output.appendLine(
                        listOf(
                            n.site, n.author, n.name, n.detail,
                            n.readAtChapterIndex, n.readAtTextIndex,
                            n.pinnedTime.time
                        ).joinToString(",")
                    )
                    count++
                }
            }
        }
        return count
    }

    private fun exportBookshelf(file: File): Int {
        val list = DataManager.listBookshelf().map {
            NovelMinimal(it.novel)
        }
        file.writeText(AppJson.encodeToString(list))
        return list.size
    }

    private fun exportBookList(folder: File): Int {
        folder.mkdirs()
        return DataManager.allBookList().sumOf { bookList ->
            val fileName = "${bookList.id}|${bookList.name}"
            val novelList = DataManager.getNovelMinimalFromBookList(bookList.nId)
            folder.resolve(fileName).writeText(Share.exportBookListJson(bookList, novelList))
            novelList.size
        }
    }

    private fun exportSettings(folder: File): Int {
        folder.mkdirs()
        @Suppress("RemoveExplicitTypeArguments")
        var count = listOf<Pref>(
            ReaderSettings,
            DownloadSettings,
            ReaderSettings.batteryMargins,
            ReaderSettings.bookNameMargins,
            ReaderSettings.chapterNameMargins,
            ReaderSettings.contentMargins,
            ReaderSettings.paginationMargins,
            ReaderSettings.timeMargins
        ).sumOf { pref ->
            pref.sharedPreferences.all.also { map ->
                val jsonObj = buildJsonObject {
                    map.forEach { (k, v) ->
                        when (v) {
                            is Boolean -> put(k, v)
                            is Int -> put(k, v)
                            is Long -> put(k, v)
                            is Float -> put(k, v)
                            is String -> put(k, v)
                            else -> {}
                        }
                    }
                }
                folder.resolve(pref.name).writeText(jsonObj.toString())
            }.size
        }
        val backgroundImage = ReaderSettings.backgroundImage
        if (backgroundImage != null) {
            folder.resolve("backgroundImage").outputStream().use { output ->
                PrefContext.appContext.contentResolver.openInputStream(backgroundImage)!!.use { input ->
                    input.copyTo(output)
                }
                output.flush()
            }
            count++
        }
        val lastBackgroundImage = ReaderSettings.lastBackgroundImage
        if (lastBackgroundImage != null) {
            folder.resolve("lastBackgroundImage").outputStream().use { output ->
                PrefContext.appContext.contentResolver.openInputStream(lastBackgroundImage)!!
                    .use { input ->
                        input.copyTo(output)
                    }
                output.flush()
            }
            count++
        }
        val font = ReaderSettings.font
        if (font != null) {
            folder.resolve("font").outputStream().use { output ->
                PrefContext.appContext.contentResolver.openInputStream(font)!!.use { input ->
                    input.copyTo(output)
                }
                output.flush()
            }
            count++
        }
        return count
    }
}
