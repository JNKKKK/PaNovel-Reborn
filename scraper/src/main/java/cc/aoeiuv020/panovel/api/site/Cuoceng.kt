package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext

class Cuoceng : DslJsoupNovelContext() { init {
    site {
        name = "错层小说"
        baseUrl = "https://m.cuoceng.com"
    }
    header {
        userAgent = defaultUserAgentMobile
    }
    bookIdRegex = "/([a-f0-9-]{36})"
    bookIdWithChapterIdRegex = "/([a-f0-9-]{36}/[a-f0-9-]{36})"
    detailPageTemplate = "/book/%s.html"
    chaptersPageTemplate = "/book/chapter/%s.html"
    contentPageTemplate = "/book/%s.html"
    search {
        get {
            url = "/book/so/$it.html"
        }
        document {
            items("div.bookbox", allowEmpty = true) {
                name("h2.bookname > a")
                author("div.bookinfo > div.author") {
                    it.text().removePrefix("作者：")
                }
            }
        }
    }
    detail {
        document {
            novel {
                name("h1.booktitle")
                author("p.booktag > a.red")
            }
            image("div.bookcover img.thumbnail")
            update("p.booktime", format = "yyyy-MM-dd HH:mm:ss") {
                it.text().removePrefix("更新时间：")
            }
            introduction("p.bookintro")
        }
    }
    chapters {
        val chapterUrl = getNovelChapterUrl(extra)
        val firstPage = parse(connect(chapterUrl))
        val allChapters = mutableListOf<NovelChapter>()
        document(firstPage) {
            items("#allchapter dd > a")
        }.let { allChapters.addAll(it) }
        val pageCount = firstPage.select("select#linkIndex option").size
        for (page in 2..pageCount) {
            val pageUrl = chapterUrl.removeSuffix(".html") + "/$page.html"
            val doc = parse(connect(pageUrl))
            document(doc) {
                items("#allchapter dd > a")
            }.let { allChapters.addAll(it) }
        }
        allChapters
    }
    content {
        document {
            items("#content > p")
        }
    }
}}
