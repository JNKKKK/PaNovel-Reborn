package cc.aoeiuv020.panovel.bookfile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

/**
 * 覆盖TextExporter的txt导出，用打包进资源的txt跑"解析→导出→再解析"往返，
 * 不依赖本地文件，CI上也能跑，
 */
class TextExporterTest : ParserTest(TextParser::class) {

    @Test
    fun roundTrip() {
        val src = getResource("/panovel.txt") ?: return
        val charset = "UTF-8"
        val srcParser = TextParser(src, Charset.forName(charset))
        val info = srcParser.parse()

        val out = folder.newFile("exported.txt")
        var isDone = false
        out.outputStream().use {
            TextExporter(it, Charset.forName(charset)).export(info, srcParser) { current, total ->
                if (current == total) isDone = true
            }
        }
        assertTrue("export should signal completion", isDone)
        assertTrue("exported file should not be empty", out.length() > 0)

        val parser = TextParser(out, Charset.forName(charset))
        val reInfo = parser.parse()

        // 元数据往返保持，
        assertEquals(info.name, reInfo.name)
        assertEquals(info.author, reInfo.author)
        assertEquals(info.introduction, reInfo.introduction)

        // 导出会跳过空章节，所以再解析后只剩有正文的章节，原始8章里有一章是空的，
        assertEquals(7, reInfo.chapters.size)

        // 第一章正文往返不丢，
        reInfo.chapters.first().let {
            assertEquals("有趣的书评同人小故事", it.name)
            val content = parser.getNovelContent(it)
            assertEquals(11, content.size)
            assertEquals("感觉这些有趣的书评小故事被埋没下去好可惜，我会慢慢整理出来的。", content.first())
            assertEquals("——————————————————————————————", content.last())
        }
    }

    /**
     * 用非默认编码(GBK)往返，验证导出/解析都尊重传入的charset，
     */
    @Test
    fun roundTripGbk() {
        val src = getResource("/zxcs.txt") ?: return
        val charset = "GBK"
        val srcParser = TextParser(src, Charset.forName(charset))
        val info = srcParser.parse()

        val out = folder.newFile("exported-gbk.txt")
        out.outputStream().use {
            TextExporter(it, Charset.forName(charset)).export(info, srcParser) { _, _ -> }
        }

        val reInfo = TextParser(out, Charset.forName(charset)).parse()
        assertEquals("与千年女鬼同居的日子", reInfo.name)
        assertEquals("卜非", reInfo.author)
        assertEquals(info.introduction, reInfo.introduction)
    }
}
