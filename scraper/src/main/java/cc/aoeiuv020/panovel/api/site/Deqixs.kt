package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.json.AppJson
import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext
import cc.aoeiuv020.regex.pick
import kotlinx.serialization.json.*

class Deqixs : DslJsoupNovelContext() { init {
    site {
        name = "得奇小说"
        baseUrl = "https://www.deqixs.co"
    }
    detailPageTemplate = "/books/%s/"
    contentPageTemplate = "/books/%s.html"
    search {
        get {
            url = "/modules/article/search.php"
            data {
                "searchkey" to it
            }
        }
        document {
            single("/books/\\d+/") {
                name("h1.booktitle")
                author("div.bookinfo p.booktag a.red")
            }
            items("#fengtui div.bookbox", allowEmpty = true) {
                name("h4.bookname > a")
                author("div.bookinfo div.author") {
                    it.text().removePrefix("作者：")
                }
            }
        }
    }
    detail {
        document {
            novel {
                name("h1.booktitle")
                author("div.bookinfo p.booktag a.red")
            }
            image("div.bookcover img.thumbnail")
            update("meta[property=og:novel:update_time]", format = "yyyy-MM-dd HH:mm:ss") {
                it.attr("content")
            }
            introduction("p.bookintro")
        }
    }
    chapters {
        document {
            items("#list-chapterAll dd > a")
        }
    }
    content {
        val chapterUrl = getNovelContentUrl(extra)
        val (aid, cid) = chapterUrl.pick("books/(\\d+)/(\\d+)\\.html")
        // visit chapter page first to establish cookies
        get { url = chapterUrl }
        response { it }
        // fetch the token script
        val encodedReferrer = java.net.URLEncoder.encode(chapterUrl, "UTF-8")
        val tokenUrl = "${site.baseUrl}/scripts/chapter.js.php?aid=$aid&cid=$cid&referrer=$encodedReferrer"
        get {
            url = tokenUrl
            header { referer = chapterUrl }
        }
        val tokenScript = response { it }
        val token = tokenScript.pick("chapterToken\\s*=\\s*'([^']+)'")[0]
        val timestamp = tokenScript.pick("timestamp\\s*=\\s*(\\d+)")[0]
        val nonce = tokenScript.pick("nonce\\s*=\\s*'([^']+)'")[0]
        // fetch chapter content via AJAX
        val ajaxUrl = "${site.baseUrl}/modules/article/ajax2.php?aid=$aid&cid=$cid&token=$token&timestamp=$timestamp&nonce=$nonce"
        get {
            url = ajaxUrl
            header {
                "X-Requested-With" to "XMLHttpRequest"
                referer = chapterUrl
            }
        }
        response {
            val json = AppJson.parseToJsonElement(it).jsonObject
            val status = json["status"]?.jsonPrimitive?.intOrNull ?: 0
            if (status != 1) {
                throw IllegalStateException("章节内容获取失败: ${json["message"]?.jsonPrimitive?.content}")
            }
            val data = json["data"]!!
            val dataObj = when (data) {
                is JsonObject -> data
                is JsonArray -> data[0].jsonObject
                else -> throw IllegalStateException("unexpected response")
            }
            val contentHtml = dataObj["content"]!!.jsonPrimitive.content
            contentHtml
                .replace("<br />", "\n")
                .replace("<br/>", "\n")
                .replace("<br>", "\n")
                .replace("&nbsp;", " ")
                .split("\n")
                .map { line -> line.trim() }
                .filter { line -> line.isNotEmpty() }
        }
    }
}}
