package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext

class MockSite : DslJsoupNovelContext() { init {
    site {
        name = "示例站点"
        baseUrl = "https://example.com"
        logo = "https://example.com/logo.png"
    }
    search {
        get { "/search?q=$it" }
        document {
            items("div.result") {
                name("> a")
                author("> span.author")
            }
        }
    }
    detail {
        document {
            novel {
                name("h1")
                author("span.author")
            }
            image("img.cover")
            introduction("div.intro")
        }
    }
    chapters {
        document {
            items("ul.chapters > li > a")
            lastUpdate("span.update", format = "yyyy-MM-dd")
        }
    }
    content {
        document {
            items("div.content > p")
        }
    }
}}
