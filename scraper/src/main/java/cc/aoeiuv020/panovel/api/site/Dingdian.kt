package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.NovelItem
import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class Dingdian : DslJsoupNovelContext() { init {
    site {
        name = "顶点小说网"
        baseUrl = "https://www.dingdian-xiaoshuo.com"
    }
    detailPageTemplate = "/n/%s/"
    contentPageTemplate = "/%s.html"
    bookIdRegex = "/n/([^/]+)"
    search {
        get {
            url = "/"
            data {
                "c" to "book"
                "a" to "search.json2"
                "callback" to "search"
                "keywords" to it
                "b" to dingdianEncode(it)
            }
            header {
                referer = "${site.baseUrl}/search.html"
            }
        }
        response { body ->
            val json = body.removePrefix("search(").removeSuffix(");")
            val contentMatch = Regex("\"content\":\"(.*)\"").find(json)
            val raw = contentMatch?.groupValues?.get(1) ?: ""
            val html = raw
                .replace("\\/", "/")
                .replace("\\r\\n", "")
                .replace("\\\"", "\"")
                .replace(Regex("\\\\u([0-9a-fA-F]{4})")) {
                    it.groupValues[1].toInt(16).toChar().toString()
                }
            if (html.contains("class=\"tip\"")) return@response emptyList()
            val doc = Jsoup.parse(html, site.baseUrl)
            doc.select("div.box[itemscope]").mapNotNull { el ->
                val link = el.selectFirst("h2.title a[href]") ?: return@mapNotNull null
                val name = link.text().trim()
                val href = link.attr("href")
                val author = el.selectFirst("span[itemprop=author]")?.text()?.trim() ?: ""
                if (name.isEmpty() || href.isEmpty()) return@mapNotNull null
                NovelItem(
                    site = this@Dingdian.site.name,
                    name = name,
                    author = author,
                    extra = href
                )
            }
        }
    }
    detail {
        document {
            novel {
                name("h1.title[itemprop=name], h1[itemprop=name]")
                author("span[itemprop=author]")
            }
            image("img[itemprop=image], img.pic")
            introduction("p.description[itemprop=description], p.description")
        }
    }
    chapters {
        val bookId = findBookId(it)
        get {
            url = "/n/$bookId/xiaoshuo.html"
        }
        document {
            items("ul.list > li > a") {
                name = root.text()
                extra = root.attr("href")
                    .removeSuffix(".html")
                    .removePrefix("/")
            }
        }
    }
    content {
        val contentUrl = getNovelContentUrl(extra)
        val doc = parse(connect(contentUrl))
        doc.select("div#content > p, div.articlebody > p").mapNotNull { p ->
            val text = p.text().trim()
            text.ifEmpty { null }
        }
    }
}}

private val dingdianStaticChars = "KXhw7UT1B0a9kQDKZsjIASmOezxYG4CHo5Jyfg2b8FLpEvRr3WtVnlqMidu6cN"

private fun dingdianEncode(keywords: String): String {
    val encoded = URLEncoder.encode(keywords, "UTF-8")
    val sb = StringBuilder()
    for (ch in encoded) {
        val idx = dingdianStaticChars.indexOf(ch)
        val code = if (idx == -1) ch else dingdianStaticChars[(idx + 3) % 62]
        val r1 = dingdianStaticChars[(Math.random() * 62).toInt()]
        val r2 = dingdianStaticChars[(Math.random() * 62).toInt()]
        sb.append(r1).append(code).append(r2)
    }
    return sb.toString()
}
