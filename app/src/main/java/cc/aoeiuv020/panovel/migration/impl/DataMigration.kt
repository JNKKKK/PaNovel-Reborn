package cc.aoeiuv020.panovel.migration.impl

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonParser
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.NovelMinimal
import cc.aoeiuv020.panovel.data.entity.NovelWithProgress
import cc.aoeiuv020.panovel.migration.Migration
import cc.aoeiuv020.panovel.settings.*
import cc.aoeiuv020.panovel.util.VersionName

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import timber.log.Timber
import java.io.File

/**
 * Created by AoEiuV020 on 2018.05.30-19:21:52.
 */
class DataMigration : Migration() {
    private val gson: Gson = Gson()
    override val to: VersionName = VersionName("2.2.2")
    override val message: String = "书架列表，书单列表，设置，"

    override fun migrate(ctx: Context, from: VersionName) {
        import(ctx.getExternalFilesDir(null))
        import(ctx.filesDir)
    }

    private fun import(base: File) {
        importBookshelf(base)
        importBookList(base)
        importSettings(base)
    }

    private fun importMargins(base: File, margins: Margins) {
        val map = mapOf<String, (JsonElement) -> Unit>(
                "enabled" to { value -> margins.enabled = value.asBoolean },
                "left" to { value -> margins.left = value.asInt },
                "top" to { value -> margins.top = value.asInt },
                "right" to { value -> margins.right = value.asInt },
                "bottom" to { value -> margins.bottom = value.asInt }
        )
        base.listFiles()?.forEach { file ->
            val setter = map[file.name] ?: return@forEach
            gson.fromJson(file.readText(), JsonElement::class.java).let(setter)
        }
    }


    private fun importSettings(base: File) {
        val list = base.resolve("Settings").listFiles() ?: return
        val map = mapOf<String, (JsonElement) -> Unit>(
            "backPressOutOfFullScreen" to { value ->
                ReaderSettings.backPressOutOfFullScreen = value.asBoolean
            },
            "adEnabled" to { value -> AdSettings.adEnabled = value.asBoolean },
            "BookSmallLayout" to { value -> ListSettings.largeView = !value.asBoolean },
            "fullScreenClickNextPage" to { value ->
                ReaderSettings.fullScreenClickNextPage = value.asBoolean
            },
            "volumeKeyScroll" to { value -> ReaderSettings.volumeKeyScroll = value.asBoolean },
            "reportCrash" to { value -> OtherSettings.reportCrash = value.asBoolean },
            "bookshelfRedDotColor" to { value -> ListSettings.dotColor = value.asInt },
            "bookshelfRedDotSize" to { value -> ListSettings.dotSize = value.asFloat },
            "fullScreenDelay" to { value -> ReaderSettings.fullScreenDelay = value.asInt },
            "textSize" to { value -> ReaderSettings.textSize = value.asInt },
            "lineSpacing" to { value -> ReaderSettings.lineSpacing = value.asInt },
            "paragraphSpacing" to { value -> ReaderSettings.paragraphSpacing = value.asInt },
            "messageSize" to { value -> ReaderSettings.messageSize = value.asInt },
            "autoRefreshInterval" to { value -> ReaderSettings.autoRefreshInterval = value.asInt },
            "textColor" to { value -> ReaderSettings.textColor = value.asInt },
            "backgroundColor" to { value -> ReaderSettings.backgroundColor = value.asInt },
            "historyCount" to { value -> GeneralSettings.historyCount = value.asInt },
            "downloadThreadCount" to { value ->
                DownloadSettings.downloadThreadsLimit = value.asInt
            },
            "chapterColorDefault" to { value -> OtherSettings.chapterColorDefault = value.asInt },
            "chapterColorCached" to { value -> OtherSettings.chapterColorCached = value.asInt },
            "chapterColorReadAt" to { value -> OtherSettings.chapterColorReadAt = value.asInt },
            "animationSpeed" to { value -> ReaderSettings.animationSpeed = value.asFloat },
            "centerPercent" to { value -> ReaderSettings.centerPercent = value.asFloat },
            "dateFormat" to { value -> ReaderSettings.dateFormat = value.asString },
            "animationMode" to { value -> ReaderSettings.animationMode = gson.fromJson(value.asString, cc.aoeiuv020.reader.AnimationMode::class.java) },
            "shareExpiration" to { value ->
                OtherSettings.shareExpiration = gson.fromJson(value.asString, cc.aoeiuv020.panovel.share.Expiration::class.java)
            }
        )
        val marginsMap = mapOf(
                "contentMargins" to ReaderSettings.contentMargins,
                "paginationMargins" to ReaderSettings.paginationMargins,
                "bookNameMargins" to ReaderSettings.bookNameMargins,
                "chapterNameMargins" to ReaderSettings.chapterNameMargins,
                "timeMargins" to ReaderSettings.timeMargins,
                "batteryMargins" to ReaderSettings.batteryMargins
        )
        list.forEach { file ->
            val setter = map[file.name] ?: return@forEach
            if (file.isFile) {
                gson.fromJson(file.readText(), JsonElement::class.java).let(setter)
            } else if (file.isDirectory) {
                val margins = marginsMap[file.name] ?: return@forEach
                importMargins(file, margins)
            }
        }
        list.forEach { file ->
            // 导入字体和背景图，
            when (file.name) {
                "font" -> ReaderSettings.font = Uri.fromFile(file)
                "backgroundImage" -> ReaderSettings.backgroundImage = Uri.fromFile(file)
            }
        }
    }

    /**
     * Helper to get a string field from a JsonObject, stripping "$." prefix from path.
     */
    private fun jsonField(obj: com.google.gson.JsonObject, path: String): String {
        val key = if (path.startsWith("$.")) path.substring(2) else path
        val value = obj.get(key) ?: return ""
        return if (value.isJsonPrimitive) value.asString else value.toString()
    }

    /**
     * Helper to get a typed field from a JsonObject, stripping "$." prefix from path.
     */
    private fun jsonFieldElement(obj: com.google.gson.JsonObject, path: String): JsonElement {
        val key = if (path.startsWith("$.")) path.substring(2) else path
        return obj.get(key) ?: com.google.gson.JsonNull.INSTANCE
    }

    // 旧版requester有两种情况，一个对象包含extra或者竖线|分隔类名和extra,
    private fun getDetailFromRequester(requester: JsonElement): String =
            if (requester.isJsonObject) {
                val obj = requester.asJsonObject
                val value = obj.get("extra")
                if (value == null || value.isJsonNull) "" else if (value.isJsonPrimitive) value.asString else value.toString()
            } else {
                requester.asString.substringAfter('|', "")
            }

    private fun importBookshelf(base: File) {
        val progress = base.resolve("Progress")
        val list = base.resolve("Bookshelf").listFiles()?.map {
            val obj = JsonParser.parseString(it.readText()).asJsonObject
            val novel = NovelWithProgress(
                    site = jsonField(obj, "$.site"),
                    author = jsonField(obj, "$.author"),
                    name = jsonField(obj, "$.name"),
                    detail = getDetailFromRequester(jsonFieldElement(obj, "$.requester")))
            try {
                progress.resolve(novel.run { "$name.$author.$site" })
                        .takeIf { f -> f.exists() }
                        ?.let { f ->
                            val progressObj = JsonParser.parseString(f.readText()).asJsonObject
                            novel.readAtChapterIndex = jsonField(progressObj, "chapter").toIntOrNull() ?: 0
                            novel.readAtTextIndex = jsonField(progressObj, "text").toIntOrNull() ?: 0
                        }
            } catch (e: Exception) {
                Timber.e(e, "旧版书架中的小说<${novel.run { "$name.$author.$site" }}>阅读进度读取失败,")
                // 进度次要，异常不抛出去，
            }
            novel
        } ?: return
        DataManager.importBookshelfWithProgress(list)
    }

    private fun importBookList(base: File) {
        base.resolve("BookList").listFiles()?.forEach {
            val obj = JsonParser.parseString(it.readText()).asJsonObject
            val name = jsonField(obj, "$.name")
            val listArray = obj.getAsJsonArray("list")
            val list = listArray.map { element ->
                val itemObj = element.asJsonObject
                NovelMinimal(
                        site = jsonField(itemObj, "$.site"),
                        author = jsonField(itemObj, "$.author"),
                        name = jsonField(itemObj, "$.name"),
                        detail = getDetailFromRequester(jsonFieldElement(itemObj, "$.requester")))
            }
            DataManager.importBookList(name, list)
        }
    }
}