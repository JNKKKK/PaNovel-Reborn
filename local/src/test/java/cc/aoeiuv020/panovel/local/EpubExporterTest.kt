package cc.aoeiuv020.panovel.local

import cc.aoeiuv020.base.jar.textList
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.Charset

/**
 * Created by AoEiuV020 on 2018.06.19-22:57:24.
 */
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
        EpubExporter(out).export(info, srcParser) { current, total ->
            if (current == total) isDone = true
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
    fun yidm() {
        val file = getFile("/home/aoeiuv/tmp/panovel/epub/yidm/Re：从零开始的异世界生活_第十一卷.epub") ?: return
        val charset = "UTF-8"
        val tmpParser = EpubParser(file, Charset.forName(charset))
        val info = tmpParser.parse()
        val tmpFile = File("/tmp/fff.epub")
        var isDone = false
        var lastP = -1
        EpubExporter(tmpFile).export(info, tmpParser) { current, total ->
            if (current == total) {
                // 以防万一，多一个isDone判断避免结束会调顺序出问题时不能通知结束，
                isDone = true
            }
            // 进度分成一百份，
            val max = 100
            val progress = (current.toFloat() / total * max).toInt()
            if (progress > lastP && !isDone) {
                lastP = progress
                println("exporting $current/$total")
            }
            if (isDone) {
                println("exported")
            }
        }
        val parser = EpubParser(tmpFile, Charset.forName(info.requester))
        val chapters = chapters(
                parser,
                author = null,
                name = "Re：从零开始的异世界生活-第十一卷-迷糊动漫",
                requester = charset,
                image = "OEBPS/Cover.jpg",
                introduction = null
        )
        assertEquals(12, chapters.size)
        chapters.first().let {
            assertEquals("封面", it.name)
            val content = parser.getNovelContent(it)
            assertEquals("![img](jar:${tmpFile.toURI()}!/OEBPS/image0.jpg)", content.first())
            assertEquals( content.first(), content.last())
            assertEquals(1, content.size)
        }
        chapters[1].let {
            assertEquals("书名", it.name)
            val content = parser.getNovelContent(it)
            assertEquals("Re：从零开始的异世界生活", content.first())
            assertEquals("插画: 大塚真一郎", content.last())
            assertEquals(4, content.size)
        }
        chapters.last().let {
            assertEquals("第十一卷 后记", it.name)
            val content = parser.getNovelContent(it)
            assertEquals("后记", content.first())
            assertEquals("![img](jar:${tmpFile.toURI()}!/OEBPS/image20.jpg)", content.last())
            assertEquals(24, content.size)
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