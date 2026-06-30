package cc.aoeiuv020.panovel.backup

import android.net.Uri
import cc.aoeiuv020.json.AppJson
import cc.aoeiuv020.panovel.backup.BackupOption.*
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.data.entity.NovelMinimal
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
    companion object {
        // 备份目录中各部分的子目录名，
        const val DIR_NOVELS = "Novels"
        const val DIR_BOOKLIST = "BookList"
        const val DIR_SETTINGS = "Settings"

        // 历史备份的小说数量，与历史页展示数量一致，
        const val HISTORY_COUNT = 30
    }

    @Synchronized
    override fun import(base: File): String {
        Timber.d("import from $base")
        val sb = StringBuilder()
        // 顺序：先恢复小说，再恢复书单（书单引用的小说此时已就位），最后设置，
        base.resolve(DIR_NOVELS).takeIf { it.isDirectory }?.let { folder ->
            runCatching {
                val count = importNovels(folder)
                sb.appendLine("成功导入小说: <$count>本，")
            }.onFailure { Timber.e(it, "导入小说失败，") }
        }
        base.resolve(DIR_BOOKLIST).takeIf { it.isDirectory }?.let { folder ->
            runCatching {
                val count = importBookList(folder)
                sb.appendLine("成功导入书单: <$count>条，")
            }.onFailure { Timber.e(it, "导入书单失败，") }
        }
        base.resolve(DIR_SETTINGS).takeIf { it.isDirectory }?.let { folder ->
            runCatching {
                val count = importSettings(folder)
                sb.appendLine("成功导入设置: <$count>项，")
            }.onFailure { Timber.e(it, "导入设置失败，") }
        }
        if (sb.isEmpty()) {
            sb.appendLine("备份文件里没有可导入的内容，")
        }
        return sb.toString()
    }

    @Synchronized
    override fun export(base: File, options: Set<BackupOption>): String {
        Timber.d("export to $base\n enable $options")
        val sb = StringBuilder()
        // 书架/书单/历史任一选中，就导出对应小说（去重）及其章节列表和缓存正文，
        if (Bookshelf in options || BookList in options || History in options) {
            runCatching {
                val count = exportNovels(base.resolve(DIR_NOVELS), options)
                sb.appendLine("成功导出小说: <$count>本，")
            }.onFailure { Timber.e(it, "导出小说失败，") }
        }
        if (BookList in options) {
            runCatching {
                val count = exportBookList(base.resolve(DIR_BOOKLIST))
                sb.appendLine("成功导出书单: <$count>条，")
            }.onFailure { Timber.e(it, "导出书单失败，") }
        }
        if (Settings in options) {
            runCatching {
                val count = exportSettings(base.resolve(DIR_SETTINGS))
                sb.appendLine("成功导出设置: <$count>项，")
            }.onFailure { Timber.e(it, "导出设置失败，") }
        }
        return sb.toString()
    }

    // ---- 小说（含章节列表和缓存正文） ----

    private fun exportNovels(folder: File, options: Set<BackupOption>): Int {
        // 按身份(site+author+name)去重收集需要备份的小说，
        // onShelf 记录是否应在导入后恢复到书架，只有选中了书架且确实在书架上才置true,
        val map = LinkedHashMap<String, Pair<Novel, Boolean>>()
        fun collect(novels: List<Novel>, onShelf: Boolean) {
            novels.forEach { novel ->
                val key = "${novel.site}|${novel.author}|${novel.name}"
                val prev = map[key]
                val shelf = onShelf || (prev?.second ?: false)
                map[key] = (prev?.first ?: novel) to shelf
            }
        }
        if (Bookshelf in options) {
            collect(DataManager.listBookshelfNovels(), onShelf = true)
        }
        if (History in options) {
            collect(DataManager.listHistoryNovels(HISTORY_COUNT), onShelf = false)
        }
        if (BookList in options) {
            DataManager.allBookList().forEach { bookList ->
                collect(DataManager.listBookListNovels(bookList.nId), onShelf = false)
            }
        }
        if (map.isEmpty()) {
            return 0
        }
        folder.mkdirs()
        map.values.forEachIndexed { index, (novel, onShelf) ->
            val backupNovel = toBackupNovel(novel, onShelf)
            folder.resolve(index.toString()).writeText(AppJson.encodeToString(backupNovel))
        }
        return map.size
    }

    private fun toBackupNovel(novel: Novel, onShelf: Boolean): BackupNovel {
        val chaptersExtra = novel.chapters
        // 只有获取过详情(chapters非空)才有章节缓存可读，
        val chapters = if (chaptersExtra != null) {
            DataManager.loadCachedChapters(novel) ?: emptyList()
        } else {
            emptyList()
        }
        // 逐章读取已缓存正文，没缓存的章节跳过，
        val contents = HashMap<String, List<String>>()
        chapters.forEach { chapter ->
            DataManager.loadCachedContent(novel, chapter.extra)?.let { text ->
                contents[chapter.extra] = text
            }
        }
        return BackupNovel(
            site = novel.site,
            author = novel.author,
            name = novel.name,
            detail = novel.detail,
            readAtChapterIndex = novel.readAtChapterIndex,
            readAtTextIndex = novel.readAtTextIndex,
            readAtChapterName = novel.readAtChapterName,
            readTime = novel.readTime.time,
            bookshelf = onShelf,
            pinnedTime = novel.pinnedTime.time,
            image = novel.image,
            introduction = novel.introduction,
            updateTime = novel.updateTime.time,
            chaptersCount = novel.chaptersCount,
            lastChapterName = novel.lastChapterName,
            chaptersExtra = chaptersExtra,
            chapters = chapters,
            contents = contents,
        )
    }

    private fun importNovels(folder: File): Int {
        val files = folder.listFiles() ?: return 0
        var count = 0
        files.forEach { file ->
            try {
                val backupNovel: BackupNovel = AppJson.decodeFromString(file.readText())
                if (importNovel(backupNovel)) {
                    count++
                }
            } catch (e: Exception) {
                Timber.e(e, "恢复小说失败，${file.name}")
            }
        }
        return count
    }

    private fun importNovel(backup: BackupNovel): Boolean {
        val novel = DataManager.queryOrNewNovel(
            NovelMinimal(backup.site, backup.author, backup.name, backup.detail)
        )
        if (!DataManager.checkSiteSupport(novel)) {
            // 网站不在支持列表（含本地小说），跳过，
            return false
        }
        novel.detail = backup.detail
        novel.readAtChapterIndex = backup.readAtChapterIndex
        novel.readAtTextIndex = backup.readAtTextIndex
        novel.readAtChapterName = backup.readAtChapterName
        novel.readTime = Date(backup.readTime)
        // 只增不减，避免导入历史备份时把书从书架上撤下来，
        if (backup.bookshelf) {
            novel.bookshelf = true
        }
        novel.pinnedTime = Date(backup.pinnedTime)
        novel.image = backup.image
        novel.introduction = backup.introduction
        novel.updateTime = Date(backup.updateTime)
        novel.chaptersCount = backup.chaptersCount
        novel.lastChapterName = backup.lastChapterName
        novel.chapters = backup.chaptersExtra
        DataManager.updateNovelAll(novel)
        // 恢复章节列表和缓存正文，
        if (backup.chaptersExtra != null && backup.chapters.isNotEmpty()) {
            DataManager.saveCachedChapters(novel, backup.chapters)
            backup.contents.forEach { (extra, text) ->
                DataManager.saveCachedContent(novel, extra, text)
            }
        }
        return true
    }

    // ---- 书单 ----

    private fun exportBookList(folder: File): Int {
        folder.mkdirs()
        return DataManager.allBookList().sumOf { bookList ->
            val novelList = DataManager.getNovelMinimalFromBookList(bookList.nId)
            // 文件名用唯一的id, 内容里已经带了书单名和uuid,
            folder.resolve(bookList.nId.toString())
                .writeText(Share.exportBookListJson(bookList, novelList))
            novelList.size
        }
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

    // ---- 设置 ----

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
