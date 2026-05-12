package cc.aoeiuv020.panovel.backup.impl

import cc.aoeiuv020.json.AppJson
import cc.aoeiuv020.panovel.backup.BackupOption
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.NovelMinimal
import cc.aoeiuv020.panovel.data.entity.NovelWithProgress
import cc.aoeiuv020.panovel.settings.*
import kotlinx.serialization.json.*
import timber.log.Timber
import java.io.File

/**
 * Created by AoEiuV020 on 2018.05.11-18:52:50.
 */
class BackupV1 : DefaultBackup() {

    /**
     * Resolve a dot-separated path (stripping leading "$." if present) on a JsonElement.
     * Returns the value as a String, or "" if not found.
     */
    private fun jsonPath(element: JsonElement, path: String): String {
        val keys = (if (path.startsWith("$.")) path.substring(2) else path).split(".")
        var current: JsonElement = element
        for (key in keys) {
            if (current !is JsonObject) return ""
            current = current.jsonObject[key] ?: return ""
        }
        return if (current is JsonPrimitive) current.jsonPrimitive.content else current.toString()
    }

    /**
     * Resolve a dot-separated path and return as JsonElement.
     */
    private fun jsonPathElement(element: JsonElement, path: String): JsonElement? {
        val keys = (if (path.startsWith("$.")) path.substring(2) else path).split(".")
        var current: JsonElement = element
        for (key in keys) {
            if (current !is JsonObject) return null
            current = current.jsonObject[key] ?: return null
        }
        return current
    }

    /**
     * Resolve a dot-separated path and return as Int, defaulting to 0.
     */
    private fun jsonPathInt(element: JsonElement, path: String): Int {
        val value = jsonPathElement(element, path)
        return if (value is JsonPrimitive) value.int else 0
    }

    override fun import(file: File, option: BackupOption): Int {
        Timber.d("import $option")

        return when (option) {
            BackupOption.Bookshelf -> {
                val array = AppJson.parseToJsonElement(file.readText()).jsonArray
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
                val array = AppJson.parseToJsonElement(file.readText()).jsonArray
                array.onEach { element ->
                    val obj = element.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content ?: ""
                    val listArray = obj["list"]!!.jsonArray
                    val list = listArray.map { item ->
                        val itemObj = item.jsonObject
                        NovelMinimal(
                                site = jsonPath(itemObj, "$.site"),
                                author = jsonPath(itemObj, "$.author"),
                                name = jsonPath(itemObj, "$.name"),
                                detail = jsonPath(itemObj, "$.requester.extra"))
                    }
                    DataManager.importBookList(name, list)
                }.size
            }
            BackupOption.Settings -> {
                val map = mapOf<String, (JsonElement) -> Unit>(
                    "backPressOutOfFullScreen" to { value ->
                        ReaderSettings.backPressOutOfFullScreen = value.jsonPrimitive.boolean
                    },
                    "adEnabled" to { _ -> },
                    "BookSmallLayout" to { value -> ListSettings.largeView = !value.jsonPrimitive.boolean },
                    "fullScreenClickNextPage" to { value ->
                        ReaderSettings.fullScreenClickNextPage = value.jsonPrimitive.boolean
                    },
                    "volumeKeyScroll" to { value ->
                        ReaderSettings.volumeKeyScroll = value.jsonPrimitive.boolean
                    },
                    "reportCrash" to { value -> OtherSettings.reportCrash = value.jsonPrimitive.boolean },
                    "subscribeNovelUpdate" to { value ->
                        ServerSettings.notifyNovelUpdate = value.jsonPrimitive.boolean
                    },
                    "bookshelfRedDotNotifyNotReadOrNewChapter" to { value ->
                        ListSettings.dotNotifyUpdate = value.jsonPrimitive.boolean
                    },
                    "bookshelfRedDotColor" to { value -> ListSettings.dotColor = value.jsonPrimitive.int },
                    "bookshelfRedDotSize" to { value -> ListSettings.dotSize = value.jsonPrimitive.float },
                    "fullScreenDelay" to { value -> ReaderSettings.fullScreenDelay = value.jsonPrimitive.int },
                    "textSize" to { value -> ReaderSettings.textSize = value.jsonPrimitive.int },
                    "lineSpacing" to { value -> ReaderSettings.lineSpacing = value.jsonPrimitive.int },
                    "paragraphSpacing" to { value ->
                        ReaderSettings.paragraphSpacing = value.jsonPrimitive.int
                    },
                    "messageSize" to { value -> ReaderSettings.messageSize = value.jsonPrimitive.int },
                    "autoRefreshInterval" to { value ->
                        ReaderSettings.autoRefreshInterval = value.jsonPrimitive.int
                    },
                    "textColor" to { value -> ReaderSettings.textColor = value.jsonPrimitive.int },
                    "backgroundColor" to { value -> ReaderSettings.backgroundColor = value.jsonPrimitive.int },
                    "historyCount" to { value -> GeneralSettings.historyCount = value.jsonPrimitive.int },
                    "downloadThreadCount" to { value ->
                        DownloadSettings.downloadThreadsLimit = value.jsonPrimitive.int
                    },
                    "chapterColorDefault" to { value ->
                        OtherSettings.chapterColorDefault = value.jsonPrimitive.int
                    },
                    "chapterColorCached" to { value ->
                        OtherSettings.chapterColorCached = value.jsonPrimitive.int
                    },
                    "chapterColorReadAt" to { value ->
                        OtherSettings.chapterColorReadAt = value.jsonPrimitive.int
                    },
                    "animationSpeed" to { value -> ReaderSettings.animationSpeed = value.jsonPrimitive.float },
                    "centerPercent" to { value -> ReaderSettings.centerPercent = value.jsonPrimitive.float },
                    "dateFormat" to { value -> ReaderSettings.dateFormat = value.jsonPrimitive.content },
                    "animationMode" to { value ->
                        ReaderSettings.animationMode = enumValueOf<cc.aoeiuv020.reader.AnimationMode>(value.jsonPrimitive.content)
                    },
                    "shareExpiration" to { value ->
                        OtherSettings.shareExpiration = enumValueOf<cc.aoeiuv020.panovel.share.Expiration>(value.jsonPrimitive.content)
                    },
                    "contentMargins" to { value -> ReaderSettings.contentMargins.import(value.jsonPrimitive.content) },
                    "paginationMargins" to { value -> ReaderSettings.paginationMargins.import(value.jsonPrimitive.content) },
                    "bookNameMargins" to { value -> ReaderSettings.bookNameMargins.import(value.jsonPrimitive.content) },
                    "chapterNameMargins" to { value ->
                        ReaderSettings.chapterNameMargins.import(
                            value.jsonPrimitive.content
                        )
                    },
                    "timeMargins" to { value -> ReaderSettings.timeMargins.import(value.jsonPrimitive.content) },
                    "batteryMargins" to { value -> ReaderSettings.batteryMargins.import(value.jsonPrimitive.content) }
                )
                var count = 0
                val settingsObj = AppJson.parseToJsonElement(file.readText()).jsonObject
                settingsObj.forEach { (key, value) ->
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
                "enabled" to { value -> enabled = value.jsonPrimitive.boolean },
                "left" to { value -> left = value.jsonPrimitive.int },
                "top" to { value -> top = value.jsonPrimitive.int },
                "right" to { value -> right = value.jsonPrimitive.int },
                "bottom" to { value -> bottom = value.jsonPrimitive.int }
        )
        AppJson.parseToJsonElement(json).jsonObject.forEach { (key, value) ->
            map[key]?.invoke(value)
        }
    }
}
