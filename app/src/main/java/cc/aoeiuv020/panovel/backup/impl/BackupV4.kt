package cc.aoeiuv020.panovel.backup.impl

import android.net.Uri
import cc.aoeiuv020.json.AppJson
import cc.aoeiuv020.panovel.backup.BackupOption
import cc.aoeiuv020.panovel.util.PrefContext
import cc.aoeiuv020.panovel.backup.BackupOption.*
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.NovelMinimal
import cc.aoeiuv020.panovel.data.entity.NovelWithProgressAndPinnedTime
import cc.aoeiuv020.panovel.settings.*
import cc.aoeiuv020.panovel.share.Share
import cc.aoeiuv020.panovel.util.Pref
import cc.aoeiuv020.panovel.util.notNullOrReport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import timber.log.Timber
import java.io.File
import java.util.*

class BackupV4 : DefaultBackup() {
    override fun import(file: File, option: BackupOption): Int {
        Timber.d("import $option from $file")
        return when (option) {
            Bookshelf -> importBookshelf(file)
            BookList -> importBookList(file)
            Progress -> importProgress(file)
            Settings -> importSettings(file)
        }
    }

    private fun importProgress(file: File): Int {
        return file.useLines { s ->
            s.map { line ->
                val a = line.split(',')
                NovelWithProgressAndPinnedTime(
                    a[0],
                    a[1],
                    a[2],
                    a[3],
                    a[4].toInt(),
                    a[5].toInt(),
                    Date(a[6].toLong())
                )
            }.let {
                DataManager.importNovelWithProgress(it)
            }
        }
    }

    private fun importSettings(folder: File): Int {
        val list = folder.listFiles()
        return list.notNullOrReport().sumBy { file ->
            when (file.name) {
                "Ad" -> 0
                "General" -> importPref(GeneralSettings, file)
                "List" -> importPref(ListSettings, file)
                "Other" -> importPref(OtherSettings, file)
                "Reader" -> importPref(ReaderSettings, file)
                "Download" -> importPref(DownloadSettings, file)
                "Interface" -> importPref(InterfaceSettings, file)
                "Location" -> importPref(LocationSettings, file)
                "Server" -> importPref(ServerSettings, file)
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
                // 枚举，保存字符串，
                "animationMode" -> editor.putString(key, prim.content)
                "shareExpiration" -> editor.putString(key, prim.content)
                "onCheckUpdateClick" -> editor.putString(key, prim.content)
                "onDotClick" -> editor.putString(key, prim.content)
                "onDotLongClick" -> editor.putString(key, prim.content)
                "onItemClick" -> editor.putString(key, prim.content)
                "onItemLongClick" -> editor.putString(key, prim.content)
                "onLastChapterClick" -> editor.putString(key, prim.content)
                "onNameClick" -> editor.putString(key, prim.content)
                "onNameLongClick" -> editor.putString(key, prim.content)
                "bookshelfOrderBy" -> editor.putString(key, prim.content)

                "adEnabled" -> {}
                "keepScreenOn" -> editor.putBoolean(key, prim.boolean)
                "fullScreen" -> editor.putBoolean(key, prim.boolean)
                "backPressOutOfFullScreen" -> editor.putBoolean(key, prim.boolean)
                "fullScreenClickNextPage" -> editor.putBoolean(key, prim.boolean)
                "fitWidth" -> editor.putBoolean(key, prim.boolean)
                "fitHeight" -> editor.putBoolean(key, prim.boolean)
                "gridView" -> editor.putBoolean(key, prim.boolean)
                "largeView" -> editor.putBoolean(key, prim.boolean)
                "pinnedBackgroundColor" -> editor.putInt(key, prim.int)
                "refreshOnSearch" -> editor.putBoolean(key, prim.boolean)
                "reportCrash" -> editor.putBoolean(key, prim.boolean)
                "qidianshuju" -> editor.putBoolean(key, prim.boolean)
                "sp7" -> editor.putBoolean(key, prim.boolean)
                "qidiantu" -> editor.putBoolean(key, prim.boolean)
                "volumeKeyScroll" -> editor.putBoolean(key, prim.boolean)
                "tabGravityCenter" -> editor.putBoolean(key, prim.boolean)
                "animationSpeed" -> editor.putFloat(key, prim.float)
                "centerPercent" -> editor.putFloat(key, prim.float)
                "dotSize" -> editor.putFloat(key, prim.float)
                "autoSaveReadStatus" -> editor.putInt(key, prim.int)
                "brightness" -> editor.putInt(key, prim.int)
                "autoRefreshInterval" -> editor.putInt(key, prim.int)
                "backgroundColor" -> editor.putInt(key, prim.int)
                "lastBackgroundColor" -> editor.putInt(key, prim.int)
                "chapterColorCached" -> editor.putInt(key, prim.int)
                "chapterColorDefault" -> editor.putInt(key, prim.int)
                "chapterColorReadAt" -> editor.putInt(key, prim.int)
                "dotColor" -> editor.putInt(key, prim.int)
                "searchThreadsLimit" -> editor.putInt(key, prim.int)
                // 下载相关设置以前是在GeneralSettings里，
                // TODO: 可以干脆都改成这样，
                "downloadThreadsLimit" -> DownloadSettings.downloadThreadsLimit = prim.int
                "downloadCount" -> DownloadSettings.downloadCount = prim.int
                "autoDownloadCount" -> DownloadSettings.autoDownloadCount = prim.int
                "fullScreenDelay" -> editor.putInt(key, prim.int)
                "historyCount" -> editor.putInt(key, prim.int)
                "lineSpacing" -> editor.putInt(key, prim.int)
                "messageSize" -> editor.putInt(key, prim.int)
                "paragraphSpacing" -> editor.putInt(key, prim.int)
                "textColor" -> editor.putInt(key, prim.int)
                "lastTextColor" -> editor.putInt(key, prim.int)
                "textSize" -> editor.putInt(key, prim.int)
                "dateFormat" -> editor.putString(key, prim.content)
                "segmentIndentation" -> editor.putString(key, prim.content)
                "enabled" -> editor.putBoolean(key, prim.boolean)
                "serverAddress" -> editor.putString(key, prim.content)
                "notifyNovelUpdate" -> editor.putBoolean(key, prim.boolean)
                "askUpdate" -> editor.putBoolean(key, prim.boolean)
                "singleNotification" -> editor.putBoolean(key, prim.boolean)
                "notifyPinnedOnly" -> editor.putBoolean(key, prim.boolean)
                "dotNotifyUpdate" -> editor.putBoolean(key, prim.boolean)
                "subscriptToast" -> editor.putBoolean(key, prim.boolean)
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
        folder.listFiles().notNullOrReport().sumBy { file ->
            val bookListBean = Share.importBookList(file.readText())
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

    override fun export(file: File, option: BackupOption): Int {
        Timber.d("export $option to $file")
        return when (option) {
            Bookshelf -> exportBookshelf(file)
            BookList -> exportBookList(file)
            Progress -> exportProgress(file)
            Settings -> exportSettings(file)
        }
    }

    // 无头csv格式，其实就是逗号分隔，
    private fun exportProgress(file: File): Int {
        val list = DataManager.exportNovelProgress().map {
            NovelWithProgressAndPinnedTime(it)
        }
        var count = 0
        file.outputStream().bufferedWriter().use { output ->
            list.forEach { n ->
                if (n.readAtChapterIndex > 0 || n.readAtTextIndex > 0) {
                    output.appendln(
                        listOf(
                            n.site,
                            n.author,
                            n.name,
                            n.detail,
                            n.readAtChapterIndex,
                            n.readAtTextIndex,
                            n.pinnedTime.time
                        ).joinToString(",")
                    )
                    count++
                }
            }
        }
        return count
    }

    // 书架只导出一个文件，
    private fun exportBookshelf(file: File): Int {
        val list = DataManager.listBookshelf().map {
            NovelMinimal(it.novel)
        }
        file.writeText(AppJson.encodeToString(list))
        return list.size
    }

    // 书单分多个文件导出，一个书单一个文件，
    private fun exportBookList(folder: File): Int {
        folder.mkdirs()
        return DataManager.allBookList().sumBy { bookList ->
            // 书单名允许重复，所以拼接上id，
            val fileName = "${bookList.id}|${bookList.name}"
            // 只取小说必须的几个参数，相关数据类不能被混淆，
            // 不包括本地小说，
            val novelList = DataManager.getNovelMinimalFromBookList(bookList.nId)
            folder.resolve(fileName).writeText(Share.exportBookList(bookList, novelList))
            novelList.size
        }
    }

    // 设置分多个文件导出，
    private fun exportSettings(folder: File): Int {
        folder.mkdirs()
        @Suppress("RemoveExplicitTypeArguments")
        var count = listOf<Pref>(
            GeneralSettings, ListSettings, OtherSettings, ReaderSettings,
            DownloadSettings, InterfaceSettings, LocationSettings, ServerSettings,
            ReaderSettings.batteryMargins,
            ReaderSettings.bookNameMargins,
            ReaderSettings.chapterNameMargins,
            ReaderSettings.contentMargins,
            ReaderSettings.paginationMargins,
            ReaderSettings.timeMargins
        ).sumBy { pref ->
            // 直接从sp读map, 不受几个Settings混淆影响，
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
        // 导出背景图片，
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
        // 导出前一次设置的背景图片，
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
        // 导出字体，
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
