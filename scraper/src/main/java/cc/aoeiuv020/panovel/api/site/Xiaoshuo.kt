package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext

class Xiaoshuo : DslJsoupNovelContext() { init {
    site {
        name = "繁體小說網"
        baseUrl = "https://www.xiaoshuo.com.tw"
    }
    charset = "GBK"
    detailPageTemplate = "/%s/"
    contentPageTemplate = "/%s.html"
    search {
        val searchUrl = absUrl("/modules/article/search.php?searchkey=" +
            java.net.URLEncoder.encode(it, "GBK"))
        // site returns 403 + cookie on first request; retry with the cookie
        get { url = searchUrl }
        response { it }
        val doc = parse(connect(searchUrl), "GBK")
        document(doc) {
            items("li.search-item", allowEmpty = true) {
                name("ul.search-info li:first-child a")
                author("ul.search-info li:nth-child(3)") {
                    it.text().removePrefix("作者：")
                }
            }
        }
    }
    detail {
        document {
            novel {
                name("h1.bookTitle")
                author("meta[property=og:novel:author]") {
                    it.attr("content")
                }
            }
            image("meta[property=og:image]") {
                it.attr("content")
            }
            update("meta[property=og:novel:update_time]", format = "yyyy-MM-dd HH:mm") {
                it.attr("content")
            }
            introduction("p#bookIntro")
        }
    }
    chapters {
        document {
            items("#list-chapterAll dd > a")
        }
    }
    content {
        document {
            items("div#htmlContent") {
                it.select("script, div:has(script)").remove()
                it.html()
                    .split("<br>", "<br />", "<br/>")
                    .map { line ->
                        line.replace("&nbsp;", " ")
                            .replace(Regex("<[^>]+>"), "")
                            .trim()
                    }
                    .filter { line ->
                        line.isNotEmpty() &&
                            !line.contains("mianhuatang") &&
                            !line.contains("一秒記住")
                    }
            }
        }
    }
}}
