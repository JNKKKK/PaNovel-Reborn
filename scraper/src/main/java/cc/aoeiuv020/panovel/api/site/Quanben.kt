package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext

class Quanben : DslJsoupNovelContext() { init {
    site {
        name = "全本小說網"
        baseUrl = "https://big5.quanben.io"
    }
    detailPageTemplate = "/n/%s/"
    contentPageTemplate = "/%s.html"
    bookIdRegex = "/n/([^/]+)"
    search {
        get {
            url = "/index.php"
            data {
                "c" to "book"
                "a" to "search"
                "keywords" to it
            }
        }
        document {
            items("div.list2", allowEmpty = true) {
                name("h3 > a")
                author("p > span[itemprop=author]")
            }
        }
    }
    detail {
        document {
            novel {
                name("h1")
                author("span[itemprop=author]")
            }
            image("img[itemprop=image]")
            introduction("div.description > p")
        }
    }
    chapters {
        val bookId = findBookId(it)
        get {
            url = "/amp/n/$bookId/list.html"
        }
        document {
            items("ul.list3 > li > a") {
                name = root.text()
                extra = root.attr("href")
                    .replace("/amp/", "/")
                    .removeSuffix(".html")
                    .removePrefix("/")
            }
        }
    }
    content {
        val contentUrl = getNovelContentUrl(extra)
        val doc = parse(connect(contentUrl))
        doc.select("div#content > p").mapNotNull { p ->
            val text = p.text().trim()
            text.ifEmpty { null }
        }
    }
}}
