package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext

class Bqquge : DslJsoupNovelContext() { init {
    site {
        name = "笔趣阁"
        baseUrl = "https://www.bqquge.com"
    }
    detailPageTemplate = "/%s"
    contentPageTemplate = "/%s"
    search {
        get {
            url = "/so/$it"
        }
        document {
            items("div.item", allowEmpty = true) {
                name("div.itemtxt h3 > a")
                author("div.itemtxt a[href^=/zuozhe/]") {
                    it.text().removePrefix("作者：")
                }
            }
        }
    }
    detail {
        document {
            novel {
                name("div.bookdetail div.booktxt h1")
                author("div.bookdetail div.booktxt a[href^=/zuozhe/]") {
                    it.text().removePrefix("作者：")
                }
            }
            image("div.bookdetail > img")
            update("div.bookdetail div.booktxt p:contains(更新：)", format = "yyyy-MM-dd HH:mm:ss") {
                it.text().removePrefix("更新：")
            }
            introduction("div.des p")
        }
    }
    chapters {
        document {
            items("div#list ul li > a")
        }
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
