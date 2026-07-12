package cc.aoeiuv020.shared.jsoup

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URL

class JsoupKtTest {
    @Test
    fun p() {
        val html = """<p>a</p><p>b</p>"""
        val root = Jsoup.parse(html)
        root.body().textList().forEach {
            println(it)
        }
    }

    @Test
    fun span() {
        val html = """<p><span>a</span><span>b</span>a</p>"""
        val root = Jsoup.parse(html)
        root.body().textList().forEach {
            println(it)
        }
        root.body().text().let { println(it) }
    }

    @Test
    fun parentUrl() {
        val file = File("/home/aoeiuv/tmp/panovel/epub/yidm/Re：从零开始的异世界生活_第十一卷.epub")
                .toURI().toString()
        // file: 前缀在 Windows 上会带盘符（file:/C:/...），只校验协议和路径结尾，保持跨平台，
        assertTrue("should be a file uri", file.startsWith("file:/"))
        assertTrue("should end with the epub path", file.endsWith("/Re：从零开始的异世界生活_第十一卷.epub"))
        val url = "jar:$file!/OEBPS/Text/CoverPage.xhtml"
        val html = """<img src="../Image/Cover.jpg"></img>"""
        val root = Jsoup.parse(html, url)
        root.select("img").forEach {
            assertEquals("../Image/Cover.jpg", it.attr("src"))
            // jsoup解析jar协议上一级会出现多余的斜杆/, 和它自己额外的处理有关，
            // 这个斜杆在不同平台上不一定出现（Linux 有、Windows 没有），归一化后再比较，保持跨平台，
            assertEquals("jar:$file!/OEBPS/Image/Cover.jpg", it.attr("abs:src").replaceFirst("jar:/", "jar:"))
            assertEquals("jar:$file!/OEBPS/Image/Cover.jpg", URL(URL(url), "../Image/Cover.jpg").toString())
        }
        assertEquals("![img](jar:$file!/OEBPS/Image/Cover.jpg)", root.body().textList().single())
    }
}