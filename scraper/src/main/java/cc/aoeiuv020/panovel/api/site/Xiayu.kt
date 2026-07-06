package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.shared.jsoup.href
import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext
import cc.aoeiuv020.shared.regex.pick

class Xiayu : DslJsoupNovelContext() { init {
    site {
        name = "夏雨书屋"
        baseUrl = "https://www.xiayushuwu.com"
    }
    bookIdRegex = "novel/([^/]+)"
    bookIdWithChapterIdRegex = "chapter/([^/]+)"
    detailPageTemplate = "/novel/%s"
    contentPageTemplate = "/novel/%s"
    getNovelContentUrl { extra ->
        val parts = extra.split("/")
        "/novel/${parts[0]}/chapter/${parts[1]}"
    }
    search {
        get {
            url = "/search"
            data {
                "keyword" to it
            }
        }
        document {
            items("article.novel-card", allowEmpty = true) {
                name("div.novel-info h3")
                author("p.novel-author") {
                    it.text().removePrefix("作者：")
                }
                extra("a.novel-link") {
                    it.href().pick("novel/([^/]+)")[0]
                }
            }
        }
    }
    detail {
        document {
            novel {
                name("h1.novel-title")
                author("div.novel-meta-info a[itemprop=author] span[itemprop=name]")
            }
            image("div.novel-cover-large img")
            update("div.novel-meta-info", format = "yyyy-MM-dd HH:mm:ss") {
                it.text().pick("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})")[0]
            }
            introduction("div.intro-text")
        }
    }
    chapters {
        val detailUrl = getNovelDetailUrl(extra)
        val bookId = findBookId(extra)
        val firstPage = parse(connect(detailUrl))
        val allChapters = mutableListOf<NovelChapter>()
        document(firstPage) {
            items("ul.chapter-list li > a") {
                name = root.text()
                extra = "$bookId/${root.href().pick("chapter/([^/]+)")[0]}"
            }
        }.let { allChapters.addAll(it) }
        val pageUrls = firstPage.select("a.page-range-link[href*=chapters]")
            .map { it.absUrl("href") }.distinct()
        for (pageUrl in pageUrls) {
            val doc = parse(connect(pageUrl))
            document(doc) {
                items("ul.chapter-list li > a") {
                    name = root.text()
                    extra = "$bookId/${root.href().pick("chapter/([^/]+)")[0]}"
                }
            }.let { allChapters.addAll(it) }
        }
        allChapters
    }
    content {
        val doc = parse(connect(getNovelContentUrl(extra)))
        val body = doc.selectFirst("#chapterBody") ?: return@content emptyList()
        val paragraphs = body.select("p")
        if (paragraphs.isNotEmpty()) {
            paragraphs.map { it.text().trim() }.filter { it.isNotEmpty() }
        } else {
            body.html().split(Regex("<br\\s*/?>"))
                .map { it.replace("&nbsp;", " ").replace(Regex("<[^>]+>"), "").trim() }
                .filter { it.isNotEmpty() }
        }
    }
}}
