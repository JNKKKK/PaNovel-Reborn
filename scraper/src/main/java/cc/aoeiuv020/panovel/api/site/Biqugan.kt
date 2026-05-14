package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext
import cc.aoeiuv020.panovel.api.firstThreeIntPattern
import cc.aoeiuv020.panovel.api.firstTwoIntPattern

class Biqugan : DslJsoupNovelContext() { init {
    site {
        name = "笔趣感"
        baseUrl = "https://www.biqugan.com"
    }
    bookIdRegex = firstTwoIntPattern
    bookIdWithChapterIdRegex = firstThreeIntPattern
    detailPageTemplate = "/%s/"
    contentPageTemplate = "/%s.html"
    search {
        get {
            url = "/search.php"
            data {
                "q" to it
            }
        }
        document {
            items(".box.hot dl", allowEmpty = true) {
                name("dd > h3 > a") {
                    it.text().replace(Regex("^\\[.+?]"), "").trim()
                }
                author("dd.book_other > span")
            }
        }
    }
    detail {
        document {
            novel {
                name(".book_info .info h1")
                author("meta[property=og:novel:author]") {
                    it.attr("content")
                }
            }
            image(".book_info img.img-thumbnail")
            update("meta[property=og:novel:update_time]", format = "yyyy-MM-dd HH:mm:ss") {
                it.attr("content")
            }
            introduction("#intro_pc") {
                it.text().removePrefix("简介：").trim()
            }
        }
    }
    chapters {
        val chapterUrl = getNovelChapterUrl(extra)
        val firstPage = parse(connect(chapterUrl))
        val allChapters = mutableListOf<NovelChapter>()
        document(firstPage) {
            items(".book_list2 ul li > a")
        }.let { allChapters.addAll(it) }
        val lastPageLink = firstPage.select(".pages a[aria-label=Next]").first()
        val pageCount = lastPageLink?.attr("href")
            ?.let { Regex("index_(\\d+)\\.html").find(it)?.groupValues?.get(1)?.toIntOrNull() }
            ?: 1
        for (page in 2..pageCount) {
            val pageUrl = "${chapterUrl}index_$page.html"
            val doc = parse(connect(pageUrl))
            document(doc) {
                items(".book_list2 ul li > a")
            }.let { allChapters.addAll(it) }
        }
        allChapters
    }
    content {
        val contentUrl = getNovelContentUrl(extra)
        val allParagraphs = mutableListOf<String>()
        val firstDoc = parse(connect(contentUrl))
        val (paragraphs, totalPages) = parseBiquganPage(firstDoc)
        allParagraphs.addAll(paragraphs)
        if (totalPages > 1) {
            val baseUrl = contentUrl.removeSuffix(".html")
            for (page in 2..totalPages) {
                val pageUrl = "${baseUrl}_$page.html"
                val doc = parse(connect(pageUrl))
                val (pageParagraphs, _) = parseBiquganPage(doc)
                allParagraphs.addAll(pageParagraphs)
            }
        }
        allParagraphs
    }
}}

private val biquganPageIndicatorRegex = Regex("第\\(\\d+/\\d+\\)页")
private val biquganTotalPagesRegex = Regex("第\\(\\d+/(\\d+)\\)页")

private fun parseBiquganPage(doc: org.jsoup.nodes.Document): Pair<List<String>, Int> {
    val article = doc.selectFirst("article") ?: return Pair(emptyList(), 1)
    val html = article.html()
    val totalPages = biquganTotalPagesRegex.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 1
    val lines = html.split(Regex("<br\\s*/?>"))
        .map { it.replace("&nbsp;", " ").replace(Regex("<[^>]+>"), "").trim() }
        .filter { it.isNotEmpty() && !biquganPageIndicatorRegex.containsMatchIn(it) }
    return Pair(lines, totalPages)
}
