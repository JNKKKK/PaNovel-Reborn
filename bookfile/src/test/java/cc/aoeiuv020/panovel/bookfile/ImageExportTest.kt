package cc.aoeiuv020.panovel.bookfile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset

/**
 * 覆盖导出时的图片处理分支：网络图片保留、本地图片过滤、封面写入、图片内嵌，
 * 这些分支在普通往返测试里走不到，
 */
class ImageExportTest : ParserTest(TextParser::class) {

    private fun infoWith(image: String?, lines: List<String>) = Pair(
        LocalNovelInfo(
            author = "作者甲", name = "图片小说", image = image,
            introduction = "简介一行",
            chapters = listOf(LocalNovelChapter("第一章", "0")),
            requester = "UTF-8"
        ),
        object : ContentProvider {
            override fun getNovelContent(chapter: LocalNovelChapter): List<String> = lines
        }
    )

    /**
     * txt导出：网络图片保留成url, 本地图片(非http)变成[image]占位，普通文本原样，
     * 封面只在是网络地址时写入，
     */
    @Test
    fun textExportImages() {
        val (info, provider) = infoWith(
            image = "http://example.com/cover.jpg",
            lines = listOf(
                "![img](http://example.com/a.jpg)",
                "![img](file:///local/b.jpg)",
                "普通正文一行"
            )
        )
        val out = folder.newFile("img.txt")
        out.outputStream().use {
            TextExporter(it, Charset.forName("UTF-8")).export(info, provider) { _, _ -> }
        }

        val text = out.readText(Charset.forName("UTF-8"))
        // 网络封面写入，
        assertTrue("http cover kept", text.contains("封面：http://example.com/cover.jpg"))
        // 网络图片保留url,
        assertTrue("http image kept", text.contains("http://example.com/a.jpg"))
        // 本地图片过滤成占位，
        assertTrue("local image filtered to placeholder", text.contains("[image]"))
        // 普通文本保留，
        assertTrue("plain text kept", text.contains("普通正文一行"))
    }

    /**
     * 本地(非http)封面不写入txt, 这样换设备也不会留下无效路径，
     */
    @Test
    fun textExportSkipsLocalCover() {
        val (info, provider) = infoWith(
            image = "file:///local/cover.jpg",
            lines = listOf("普通正文")
        )
        val out = folder.newFile("img2.txt")
        out.outputStream().use {
            TextExporter(it, Charset.forName("UTF-8")).export(info, provider) { _, _ -> }
        }

        val text = out.readText(Charset.forName("UTF-8"))
        assertTrue("local cover should not be written", !text.contains("封面："))
    }

    /**
     * epub导出：内嵌图片资源，封面也写入，导出后能被自己的解析器读回，
     */
    @Test
    fun epubExportEmbedsImages() {
        // 用一个真实可读的图片文件，让封面和内嵌图片的openStream都能成功，不依赖网络，
        val img = folder.newFile("pic.jpg")
        img.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
        val imgUrl = img.toURI().toString()

        val info = LocalNovelInfo(
            author = "作者甲", name = "图片小说", image = imgUrl,
            introduction = "简介",
            chapters = listOf(LocalNovelChapter("第一章", "0")),
            requester = "UTF-8"
        )
        val provider = object : ContentProvider {
            override fun getNovelContent(chapter: LocalNovelChapter): List<String> = listOf(
                "![img]($imgUrl)",
                "正文段落"
            )
            // 默认getImage把字符串转URL即可，这里图片是file://能直接openStream,
        }

        val out = folder.newFile("img.epub")
        var done = false
        out.outputStream().use {
            EpubExporter(it).export(info, provider) { c, t -> if (c == t) done = true }
        }
        assertTrue("export completed", done)

        val parser = EpubParser(out, Charset.forName("UTF-8"))
        val reInfo = parser.parse()
        assertEquals("图片小说", reInfo.name)
        assertEquals(1, reInfo.chapters.size)
        // 封面图片内嵌成功，再解析能拿到封面，
        assertTrue("cover embedded", reInfo.image != null)

        val content = parser.getNovelContent(reInfo.chapters.first())
        // 第一行是内嵌图片，正文随后，
        assertTrue("first line is embedded image", content.first().startsWith("![img](jar:"))
        assertTrue("paragraph kept", content.any { it == "正文段落" })
    }
}
