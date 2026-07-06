package cc.aoeiuv020.panovel.bookfile

import cc.aoeiuv020.shared.jsoup.textList
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class EpubExporterTest : ParserTest(EpubParser::class) {
    /**
     * 用打包进测试资源的epub跑一遍"解析→导出→再解析"的往返，不依赖作者本地文件，
     * 验证导出的epub能被自己的解析器重新识别，章节、元数据、正文都不丢，
     * 同时覆盖导出成XHTML的改动，
     */
    @Test
    fun roundTrip() {
        val src = getResource("/zero-to-one.epub") ?: return
        val charset = "UTF-8"
        val srcParser = EpubParser(src, Charset.forName(charset))
        val info = srcParser.parse()

        val out = folder.newFile("exported.epub")
        var isDone = false
        out.outputStream().use {
            EpubExporter(it).export(info, srcParser) { current, total ->
                if (current == total) isDone = true
            }
        }
        assertTrue("export should signal completion", isDone)

        // 导出的章节文件应该是.xhtml, 这样其他阅读器才认，
        val parser = EpubParser(out, Charset.forName(charset))
        val reInfo = parser.parse()

        assertEquals(info.name, reInfo.name)
        assertEquals(info.author, reInfo.author)
        // 章节数往返后保持一致，
        assertEquals(info.chapters.size, reInfo.chapters.size)

        // 导出的正文章节用的是.xhtml扩展名，
        reInfo.chapters.first { it.name == "第1章 未来的挑战" }.let { ch ->
            assertTrue("exported chapter should be .xhtml", ch.extra.endsWith(".xhtml"))
            val content = parser.getNovelContent(ch)
            assertEquals(25, content.size)
            assertEquals("第1章", content.first())
            assertEquals("未来的挑战", content[1])
            assertTrue("last line should be real text", content.last().isNotBlank())
        }
    }

    @Test
    fun jsoup() {
        val img = Element("img").attr("src", "a.jpg")
        // jsoup生成的img标签没有封闭，
        assertEquals("""<img src="a.jpg">""", img.outerHtml())
        Jsoup.parse(img.outerHtml(), "http://a.s").textList().forEach {
            assertEquals("![img](http://a.s/a.jpg)", it)
        }
    }
}