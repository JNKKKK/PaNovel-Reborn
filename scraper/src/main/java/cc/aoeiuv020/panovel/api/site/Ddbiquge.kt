package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.shared.jsoup.absHref
import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext
import java.util.Base64

class Ddbiquge : DslJsoupNovelContext() { init {
    site {
        name = "顶点笔趣阁"
        baseUrl = "https://www.ddbiquge.co"
    }
    bookIdRegex = "/(\\d+_\\d+)"
    bookIdWithChapterIdRegex = "/(\\d+_\\d+/\\d+)"
    detailPageTemplate = "/%s/"
    contentPageTemplate = "/%s.html"
    search {
        post {
            url = "/search.html"
            data {
                "s" to it
            }
        }
        document {
            items("#novel-search > li", allowEmpty = true) {
                name("span.s2 > a")
                author("span.s3 > a")
            }
        }
    }
    detail {
        document {
            novel {
                name("meta[property=og:novel:book_name]") {
                    it.attr("content")
                }
                author("meta[property=og:novel:author]") {
                    it.attr("content")
                }
            }
            image("meta[property=og:image]") {
                it.attr("content")
            }
            update("meta[property=og:novel:update_time]", format = "yyyy-MM-dd HH:mm:ss") {
                it.attr("content")
            }
            introduction("meta[property=og:description]") {
                it.attr("content")
            }
        }
    }
    chapters {
        val chapterUrl = getNovelChapterUrl(extra)
        val bookId = findBookId(extra)
        val allChapters = mutableListOf<NovelChapter>()
        val firstPage = parse(connect(chapterUrl))
        document(firstPage) {
            items("ul.yanqing_list:last-of-type li > a")
        }.let { allChapters.addAll(it) }
        val moreLink = firstPage.selectFirst("a.btn-mulu[href]")
        if (moreLink != null) {
            val listPage = try { parse(connect(moreLink.absUrl("href"))) } catch (_: Exception) { null }
            if (listPage != null) {
                val pageUrls = listPage.select(".page_num select option")
                    .map { it.attr("value") }
                    .filter { it.isNotBlank() }
                    .map { absUrl(it) }
                for (pageUrl in pageUrls) {
                    val doc = if (pageUrl == moreLink.absUrl("href")) listPage
                        else try { parse(connect(pageUrl)) } catch (_: Exception) { continue }
                    doc.select("ul.yanqing_list li > a").forEach { element ->
                        val chapterName = element.text()
                        val onclick = element.attr("onclick")
                        val chapterExtra = if (onclick.isNotBlank()) {
                            val cid = Regex("read_tz\\((\\d+)\\)").find(onclick)
                                ?.groupValues?.get(1) ?: return@forEach
                            "$bookId/$cid"
                        } else if (element.hasAttr("href")) {
                            findBookIdWithChapterId(element.absHref())
                        } else return@forEach
                        allChapters.add(NovelChapter(name = chapterName, extra = chapterExtra))
                    }
                }
            }
        }
        allChapters.distinctBy { it.extra }.sortedBy { ch ->
            Regex("\\d+").find(ch.extra.substringAfter("/"))?.value?.toIntOrNull() ?: 0
        }
    }
    content {
        val contentUrl = getNovelContentUrl(extra)
        val allParagraphs = mutableListOf<String>()
        var currentUrl: String? = contentUrl
        val chapterIdBase = extra.substringAfter("/")
        while (currentUrl != null) {
            val doc = parse(connect(currentUrl))
            doc.select("script").forEach { script ->
                val code = script.html()
                val match = Regex("qsbs\\.bb\\('([A-Za-z0-9+/=]+)'\\)").find(code)
                if (match != null) {
                    val decoded = String(Base64.getDecoder().decode(match.groupValues[1]), Charsets.UTF_8)
                    val text = decoded
                        .replace(Regex("<[^>]+>"), "")
                        .trim()
                    if (text.isNotEmpty() && !text.contains("请关闭浏览器阅读模式")) {
                        allParagraphs.add(text)
                    }
                }
            }
            val nextLink = doc.select("div.read_btn a").lastOrNull { it.text().contains("下一章") }
            currentUrl = if (nextLink != null) {
                val href = nextLink.absUrl("href")
                if (href.contains("${chapterIdBase}_")) href else null
            } else null
        }
        allParagraphs
    }
}}
