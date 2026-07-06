package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext
import cc.aoeiuv020.shared.regex.pick

class Hongsheng : DslJsoupNovelContext() {
    override fun getNovelChapterUrl(extra: String): String {
        val bookId = extra.substringAfter("/")
        return absUrl("/wap/?pbcode/chapter/$bookId/")
    }

init {
    site {
        name = "宏声小说"
        baseUrl = "https://8.140.232.133"
    }
    bookIdRegex = "contents/(\\d+/\\d+)"
    bookIdWithChapterIdRegex = "article/(\\d+)"
    detailPageTemplate = "/wap/?pbcode/contents/%s.html"
    contentPageTemplate = "/wap/?pbcode/article/%s/"
    search {
        get {
            url = "/wap/?pbcode/so/${java.net.URLEncoder.encode(it, "UTF-8")}/"
        }
        document {
            items("li.subject-item", allowEmpty = true) {
                name("div.info h2 > a")
                author("div.info div.pub > a:first-child")
            }
        }
    }
    detail {
        document {
            novel {
                name("h1 > span")
                author("#info a[href*=author]")
            }
            image("#mainpic img")
            update("#info", format = "yyyy-MM-dd HH:mm:ss") {
                val text = it.text()
                text.pick("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})")[0]
            }
            introduction("div.indent.blank20 div.intro")
        }
    }
    chapters {
        val chapterUrl = getNovelChapterUrl(extra)
        val allChapters = mutableListOf<NovelChapter>()
        var currentUrl: String? = chapterUrl
        while (currentUrl != null) {
            val doc = parse(connect(currentUrl))
            document(doc) {
                items("ul.cataloglist li > a")
            }.let { allChapters.addAll(it) }
            val nextLink = doc.select("#next a").first()
            currentUrl = if (nextLink != null && !nextLink.attr("href").contains("javascript")) {
                nextLink.absUrl("href")
            } else {
                null
            }
        }
        allChapters
    }
    content {
        document {
            items("#bodybox > p")
        }
    }
}}
