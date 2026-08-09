package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.shared.json.AppJson
import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.api.NovelDetail
import cc.aoeiuv020.panovel.api.NovelItem
import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext
import kotlinx.serialization.json.*

/**
 * 一批共用同一套 `/api/` JSON 接口的“笔趣阁”镜像站的公共实现，
 * 各镜像只有站名和域名不同（见 [Bqg730]、[Bqg840]），其余抓取逻辑完全一致，
 */
open class BqgApiContext(
    private val siteName: String,
    private val siteBaseUrl: String,
) : DslJsoupNovelContext() { init {
    site {
        name = siteName
        baseUrl = siteBaseUrl
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
