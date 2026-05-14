package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext

class Sudugu : DslJsoupNovelContext() { init {
    site {
        name = "速读谷"
        baseUrl = "https://www.sudugu.org"
    }
    detailPageTemplate = "/%s/"
    contentPageTemplate = "/%s.html"
    search {
        get {
            url = "/i/sor.aspx"
            data {
                "key" to it
            }
        }
        document {
            items("div.item", allowEmpty = true) {
                name("div.itemtxt h3 > a")
                author("div.itemtxt a[href*=zuozhe]") {
                    it.text().removePrefix("作者：")
                }
            }
        }
    }
    detail {
        document {
            novel {
                name("div.itemtxt h1 > a")
                author("div.itemtxt a[href*=zuozhe]") {
                    it.text().removePrefix("作者：")
                }
            }
            image("div.item > a > img")
            update("h2#dir > span", format = "yyyy-MM-dd HH:mm:ss") {
                it.text().removePrefix("更新时间：")
            }
            introduction("div.des.bb p")
        }
    }
    chapters {
        val chapterUrl = getNovelChapterUrl(extra)
        val firstPage = parse(connect(chapterUrl))
        val allChapters = mutableListOf<NovelChapter>()
        document(firstPage) {
            items("div#list ul li > a")
        }.let { allChapters.addAll(it) }
        val pageCount = firstPage.select("select#pageSelect option").size
        for (page in 2..pageCount) {
            val pageUrl = "${chapterUrl}p-$page.html"
            val doc = parse(connect(pageUrl))
            document(doc) {
                items("div#list ul li > a")
            }.let { allChapters.addAll(it) }
        }
        allChapters
    }
    content {
        val contentUrl = getNovelContentUrl(extra)
        val allParagraphs = mutableListOf<String>()
        var currentUrl: String? = contentUrl
        while (currentUrl != null) {
            val doc = parse(connect(currentUrl))
            doc.select("div.con p").forEach { p ->
                val text = p.text().trim()
                if (text.isNotEmpty()) {
                    allParagraphs.add(text)
                }
            }
            val nextLink = doc.select("div.prenext span:last-child a").first()
            currentUrl = if (nextLink != null && nextLink.text().contains("下一页")) {
                nextLink.absUrl("href")
            } else {
                null
            }
        }
        allParagraphs
    }
}}
