package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext

class Biquge365 : DslJsoupNovelContext() { init {
    site {
        name = "笔趣阁365"
        baseUrl = "https://www.biquge365.net"
    }
    detailPageTemplate = "/book/%s/"
    contentPageTemplate = "/chapter/%s.html"
    search {
        post {
            url = "/s.php"
            data {
                "type" to "articlename"
                "s" to it
            }
        }
        document {
            items("ul.search > li:not(.fen)", allowEmpty = true) {
                name("span.name > a")
                author("span.zuo > a")
            }
        }
    }
    detail {
        document {
            novel {
                name("div.right_border > h1")
                author("div.xinxi > span.x1 > a[href^=/author/]")
            }
            image("div.zhutu > img")
            introduction("div.x3") {
                it.text()
                    .replace(Regex("--.*$"), "")
                    .trim()
            }
        }
    }
    chapters {
        val bookId = findBookId(it)
        get {
            url = "/newbook/$bookId/"
        }
        document {
            items("ul.info > li > a")
        }
    }
    content {
        val contentUrl = getNovelContentUrl(extra)
        val doc = parse(connect(contentUrl))
        doc.select("div#txt.txt").first()!!.let { div ->
            div.html()
                .replace("<br>", "\n")
                .replace("<br/>", "\n")
                .replace("<br />", "\n")
                .replace("&nbsp;", " ")
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("<p style=") && !it.startsWith("<") }
        }
    }
}}
