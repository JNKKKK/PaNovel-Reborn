package cc.aoeiuv020.panovel.backup.impl

import cc.aoeiuv020.panovel.App
import cc.aoeiuv020.panovel.backup.BackupOption
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.NovelMinimal
import cc.aoeiuv020.panovel.data.entity.NovelWithProgress
import cc.aoeiuv020.panovel.settings.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.io.File

/**
 * Created by AoEiuV020 on 2018.05.11-18:52:50.
 */
class BackupV1 : DefaultBackup() {
    private val gson: Gson = Gson()

    /**
     * Resolve a dot-separated path (stripping leading "$." if present) on a JsonElement.
     * Returns the value as a String, or "" if not found.
     */
    private fun jsonPath(element: JsonElement, path: String): String {
        val keys = (if (path.startsWith("$.")) path.substring(2) else path).split(".")
        var current: JsonElement = element
        for (key in keys) {
            if (!current.isJsonObject) return ""
            current = current.asJsonObject.get(key) ?: return ""
        }
        return if (current.isJsonPrimitive) current.asString else current.toString()
    }

    /**
     * Resolve a dot-separated path and return as JsonElement.
     */
    private fun jsonPathElement(element: JsonElement, path: String): JsonElement {
        val keys = (if (path.startsWith("$.")) path.substring(2) else path).split(".")
        var current: JsonElement = element
        for (key in keys) {
            if (!current.isJsonObject) return com.google.gson.JsonNull.INSTANCE
            current = current.asJsonObject.get(key) ?: return com.google.gson.JsonNull.INSTANCE
        }
        return current
    }

    /**
     * Resolve a dot-separated path and return as Int, defaulting to 0.
     */
    private fun jsonPathInt(element: JsonElement, path: String): Int {
        val value = jsonPathElement(element, path)
        return if (value.isJsonPrimitive) value.asInt else 0
    }

    override fun import(file: File, option: BackupOption): Int {
        Timber.d("import $option")

        return when (option) {
            BackupOption.Bookshelf -> {
                val array = JsonParser.parseString(file.readText()).asJsonArray
                val list = array.map { element ->
                    NovelWithProgress(
                            site = jsonPath(element, "$.item.site"),
                            author = jsonPath(element, "$.item.author"),
                            name = jsonPath(element, "$.item.name"),
                            detail = jsonPath(element, "$.item.requester.extra"),
                            readAtChapterIndex = jsonPathInt(element, "$.progress.chapter"),
                            readAtTextIndex = jsonPathInt(element, "$.progress.text"))
                }
                DataManager.importBookshelfWithProgress(list)
                list.size
            }
            BackupOption.BookList -> {
                val array = JsonParser.parseString(file.readText()).asJsonArray
                array.onEach { element ->
                    val obj = element.asJsonObject
                    val name = obj.get("name")?.asString ?: ""
                    val listArray = obj.getAsJsonArray("list")
                    val list = listArray.map { item ->
                        val itemObj = item.asJsonObject
                        NovelMinimal(
                                site = jsonPath(itemObj, "$.site"),
                                author = jsonPath(itemObj, "$.author"),
                                name = jsonPath(itemObj, "$.name"),
                                detail = jsonPath(itemObj, "$.requester.extra"))
                    }
                    DataManager.importBookList(name, list)
                }.size()
            }
            BackupOption.Settings -> {
                val map = mapOf<String, (JsonElement) -> Unit>(
                    "backPressOutOfFullScreen" to { value ->
                        ReaderSettings.backPressOutOfFullScreen = value.asBoolean
                    },
                    "adEnabled" to { value -> AdSettings.adEnabled = value.asBoolean },
                    "BookSmallLayout" to { value -> ListSettings.largeView = !value.asBoolean },
                    "fullScreenClickNextPage" to { value ->
                        ReaderSettings.fullScreenClickNextPage = value.asBoolean
                    },
                    "volumeKeyScroll" to { value ->
                        ReaderSettings.volumeKeyScroll = value.asBoolean
                    },
                    "reportCrash" to { value -> OtherSettings.reportCrash = value.asBoolean },
                    "subscribeNovelUpdate" to { value ->
                        ServerSettings.notifyNovelUpdate = value.asBoolean
                    },
                    "bookshelfRedDotNotifyNotReadOrNewChapter" to { value ->
                        ListSettings.dotNotifyUpdate = value.asBoolean
                    },
                    "bookshelfRedDotColor" to { value -> ListSettings.dotColor = value.asInt },
                    "bookshelfRedDotSize" to { value -> ListSettings.dotSize = value.asFloat },
                    "fullScreenDelay" to { value -> ReaderSettings.fullScreenDelay = value.asInt },
                    "textSize" to { value -> ReaderSettings.textSize = value.asInt },
                    "lineSpacing" to { value -> ReaderSettings.lineSpacing = value.asInt },
                    "paragraphSpacing" to { value ->
                        ReaderSettings.paragraphSpacing = value.asInt
                    },
                    "messageSize" to { value -> ReaderSettings.messageSize = value.asInt },
                    "autoRefreshInterval" to { value ->
                        ReaderSettings.autoRefreshInterval = value.asInt
                    },
                    "textColor" to { value -> ReaderSettings.textColor = value.asInt },
                    "backgroundColor" to { value -> ReaderSettings.backgroundColor = value.asInt },
                    "historyCount" to { value -> GeneralSettings.historyCount = value.asInt },
                    "downloadThreadCount" to { value ->
                        DownloadSettings.downloadThreadsLimit = value.asInt
                    },
                    "chapterColorDefault" to { value ->
                        OtherSettings.chapterColorDefault = value.asInt
                    },
                    "chapterColorCached" to { value ->
                        OtherSettings.chapterColorCached = value.asInt
                    },
                    "chapterColorReadAt" to { value ->
                        OtherSettings.chapterColorReadAt = value.asInt
                    },
                    "animationSpeed" to { value -> ReaderSettings.animationSpeed = value.asFloat },
                    "centerPercent" to { value -> ReaderSettings.centerPercent = value.asFloat },
                    "dateFormat" to { value -> ReaderSettings.dateFormat = value.asString },
                    "animationMode" to { value ->
                        ReaderSettings.animationMode = App.gson.fromJson(value.asString, cc.aoeiuv020.reader.AnimationMode::class.java)
                    },
                    "shareExpiration" to { value ->
                        OtherSettings.shareExpiration = App.gson.fromJson(value.asString, cc.aoeiuv020.panovel.share.Expiration::class.java)
                    },
                    "contentMargins" to { value -> ReaderSettings.contentMargins.import(value.asString) },
                    "paginationMargins" to { value -> ReaderSettings.paginationMargins.import(value.asString) },
                    "bookNameMargins" to { value -> ReaderSettings.bookNameMargins.import(value.asString) },
                    "chapterNameMargins" to { value ->
                        ReaderSettings.chapterNameMargins.import(
                            value.asString
                        )
                    },
                    "timeMargins" to { value -> ReaderSettings.timeMargins.import(value.asString) },
                    "batteryMargins" to { value -> ReaderSettings.batteryMargins.import(value.asString) }
                )
                var count = 0
                val settingsObj = JsonParser.parseString(file.readText()).asJsonObject
                settingsObj.entrySet().forEach { (key, value) ->
                    try {
                        map[key]?.let {
                            it(value)
                            ++count
                        }
                    } catch (e: Exception) {
                        // 只是一个设置读取失败的话可以继续，
                        Timber.e(e, "设置<$key>读取失败，")
                    }
                }
                count
            }
            else -> 0
        }
    }

    fun Margins.import(json: String) {
        val map = mapOf<String, (JsonElement) -> Unit>(
                "enabled" to { value -> enabled = value.asBoolean },
                "left" to { value -> left = value.asInt },
                "top" to { value -> top = value.asInt },
                "right" to { value -> right = value.asInt },
                "bottom" to { value -> bottom = value.asInt }
        )
        App.gson.fromJson(json, JsonObject::class.java).entrySet().forEach { (key, value) ->
            map[key]?.invoke(value)
        }
    }
}
