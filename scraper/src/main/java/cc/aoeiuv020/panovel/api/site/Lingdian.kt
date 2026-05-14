package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext

class Lingdian : DslJsoupNovelContext() { init {
    site {
        name = "零点看书"
        baseUrl = "http://23.225.121.243"
    }
    bookIdRegex = "/ldks/(\\d+)"
    bookIdWithChapterIdRegex = "/ldks/(\\d+/\\d+)"
    detailPageTemplate = "/ldks/%s/"
    contentPageTemplate = "/ldks/%s.html"
    search {
        get {
            url = "/ar.php"
            data {
                "keyWord" to it
            }
        }
        document {
            items("ul.txt-list-row5 li:has(span.s2 > a)", allowEmpty = true) {
                name("span.s2 > a")
                author("span.s4")
            }
        }
    }
    detail {
        document {
            novel {
                name("div.info h1")
                author("div.info div.fix p:first-child") {
                    it.text().removePrefix("作者：")
                }
            }
            image("div.imgbox img")
            update("meta[property=og:novel:update_time]", format = "yyyy-MM-dd HH:mm") {
                it.attr("content")
            }
            introduction("div.desc")
        }
    }
    chapters {
        val chapterUrl = getNovelChapterUrl(extra)
        val allChapters = mutableListOf<cc.aoeiuv020.panovel.api.NovelChapter>()
        val firstDoc = parse(connect(chapterUrl))
        val pageUrls = mutableListOf(chapterUrl)
        firstDoc.select("select[name=pageselect] option").drop(1).forEach { option ->
            val value = option.attr("value")
            if (value.isNotBlank()) {
                pageUrls.add(absUrl(value))
            }
        }
        fun parseChaptersFromPage(doc: org.jsoup.nodes.Document): List<cc.aoeiuv020.panovel.api.NovelChapter> {
            val sectionBoxes = doc.select("div.section-box")
            val targetBox = sectionBoxes.last() ?: return emptyList()
            return targetBox.select("ul.section-list li > a").mapNotNull { el ->
                runCatching {
                    cc.aoeiuv020.panovel.api.NovelChapter(
                        name = el.text(),
                        extra = findBookIdWithChapterId(el.absUrl("href"))
                    )
                }.getOrNull()
            }
        }
        allChapters.addAll(parseChaptersFromPage(firstDoc))
        for (url in pageUrls.drop(1)) {
            val doc = parse(connect(url))
            allChapters.addAll(parseChaptersFromPage(doc))
        }
        allChapters
    }
    content {
        val contentUrl = getNovelContentUrl(extra)
        val allParagraphs = mutableListOf<String>()
        var currentUrl: String? = contentUrl
        while (currentUrl != null) {
            val doc = parse(connect(currentUrl))
            val contentDiv = doc.selectFirst("div#content")!!
            contentDiv.select("script, h1.title").remove()
            val html = contentDiv.html()
            val pageHeaderPattern = Regex("^.+\\(第\\d+/\\d+页\\)$")
            html.split("<br>", "<br />", "<br/>").forEach { line ->
                val text = line.replace("&nbsp;", " ")
                    .replace(Regex("<[^>]+>"), "")
                    .trim()
                if (text.isNotEmpty() && !pageHeaderPattern.matches(text)) {
                    allParagraphs.add(text)
                }
            }
            val nextLink = doc.select("div.section-opt a").firstOrNull { it.text().contains("下一页") }
            currentUrl = if (nextLink != null) {
                nextLink.absUrl("href")
            } else {
                null
            }
        }
        allParagraphs
    }
}}
