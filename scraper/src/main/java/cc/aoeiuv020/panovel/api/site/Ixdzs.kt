package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext
import cc.aoeiuv020.shared.regex.pick

class Ixdzs : DslJsoupNovelContext() { init {
    site {
        name = "愛下電子書"
        baseUrl = "https://ixdzs.tw"
    }
    detailPageTemplate = "/read/%s/"
    contentPageTemplate = "/read/%s.html"
    search {
        get {
            url = "/bsearch"
            data {
                "q" to it
            }
        }
        document {
            items("ul.u-list > li", allowEmpty = true) {
                name("h3.bname > a")
                author("span.bauthor > a")
            }
        }
    }
    detail {
        document {
            novel {
                name("div.n-text > h1")
                author("a.bauthor")
            }
            image("div.n-img > img")
            update("meta[property=og:novel:update_time]", format = "yyyy-MM-dd'T'HH:mm:ssXXX") {
                it.attr("content")
            }
            introduction("p#intro") {
                it.html()
                    .replace("<br>", "\n")
                    .replace("<br/>", "\n")
                    .replace("<br />", "\n")
                    .replace("&nbsp;", " ")
                    .split("\n")
                    .map { line -> line.trim() }
                    .filter { line -> line.isNotEmpty() }
                    .joinToString("\n")
            }
        }
    }
    chapters {
        val bookId = findBookId(it)
        post {
            url = "/novel/html/"
            data {
                "bid" to bookId
            }
        }
        document {
            items("li > a") {
                name = root.text()
                extra = root.attr("href").pick("read/(\\d+/p\\d+)")[0]
            }
        }
    }
    content {
        val contentUrl = getNovelContentUrl(extra)
        val doc = parse(connect(contentUrl))
        doc.select("article.page-content section p").mapNotNull { p ->
            val text = p.text().trim()
            text.ifEmpty { null }
        }
    }
}}
