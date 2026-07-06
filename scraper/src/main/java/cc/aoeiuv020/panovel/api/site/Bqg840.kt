package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.shared.json.AppJson
import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.api.NovelDetail
import cc.aoeiuv020.panovel.api.NovelItem
import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext
import kotlinx.serialization.json.*

class Bqg840 : DslJsoupNovelContext() { init {
    site {
        name = "笔趣阁840"
        baseUrl = "https://www.bqg840.xyz"
    }
    bookIdRegex = null
    bookIdWithChapterIdRegex = null
    search {
        get {
            url = "/api/search"
            data {
                "q" to it
            }
        }
        response { body ->
            val json = AppJson.parseToJsonElement(body).jsonObject
            val data = json["data"]?.jsonArray ?: return@response emptyList()
            data.map { item ->
                val obj = item.jsonObject
                NovelItem(
                    site = site.name,
                    name = obj["title"]!!.jsonPrimitive.content,
                    author = obj["author"]!!.jsonPrimitive.content,
                    extra = obj["id"]!!.jsonPrimitive.content
                )
            }
        }
    }
    detail {
        val bookId = extra
        get {
            url = "/api/book"
            data {
                "id" to bookId
            }
        }
        response { body ->
            val obj = AppJson.parseToJsonElement(body).jsonObject
            val novel = NovelItem(
                site = site.name,
                name = obj["title"]!!.jsonPrimitive.content,
                author = obj["author"]!!.jsonPrimitive.content,
                extra = obj["dirid"]!!.jsonPrimitive.content
            )
            NovelDetail(
                novel = novel,
                image = null,
                update = null,
                introduction = obj["intro"]?.jsonPrimitive?.content?.trim() ?: "",
                extra = obj["dirid"]!!.jsonPrimitive.content
            )
        }
    }
    chapters {
        val bookId = extra
        get {
            url = "/api/booklist"
            data {
                "id" to bookId
            }
        }
        response { body ->
            val json = AppJson.parseToJsonElement(body).jsonObject
            val list = json["list"]?.jsonArray ?: return@response emptyList()
            list.mapIndexed { index, element ->
                NovelChapter(
                    name = element.jsonPrimitive.content,
                    extra = "$bookId/${index + 1}"
                )
            }
        }
    }
    content {
        val parts = extra.split("/")
        val bookId = parts[0]
        val chapterId = parts[1]
        get {
            url = "/api/chapter"
            data {
                "id" to bookId
                "chapterid" to chapterId
            }
        }
        response { body ->
            val obj = AppJson.parseToJsonElement(body).jsonObject
            val txt = obj["txt"]?.jsonPrimitive?.content ?: ""
            txt.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }
}}
