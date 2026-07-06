package cc.aoeiuv020.panovel.bookfile

import cc.aoeiuv020.shared.jsoup.absSrc
import cc.aoeiuv020.shared.jsoup.absXlinkHref
import cc.aoeiuv020.shared.regex.pick
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.Charset

/**
 * Created by AoEiuV020 on 2018.06.16-17:52:10.
 */
class EpubParserTest : ParserTest(EpubParser::class) {
    /**
     * 用打包进测试资源的epub真正跑一遍解析，不依赖作者本地的文件，
     * 这样CI上也能跑到，回归时能抓到真问题，
     */
    @Test
    fun zeroToOne() {
        val file = getResource("/zero-to-one.epub") ?: return
        val charset = "UTF-8"
        val parser = EpubParser(file, Charset.forName(charset))
        val chapters = chapters(
                parser,
                // epub4j把整个名字放进firstname, lastname为空，所以trim后就是作者全名，
                author = "彼得·蒂尔",
                name = "从0到1:开启商业与未来的秘密（图文精编版） (奇点系列)",
                requester = charset,
                image = "cover.jpeg",
                // 这本epub没有dc:description, 封面页也没有简介，
                introduction = null
        )
        // 目录84个navPoint深度优先展开 + 封面页插到最前面，
        assertEquals(85, chapters.size)

        chapters.first().let {
            assertEquals("Cover", it.name)
            assertEquals("titlepage.xhtml", it.extra)
            val content = parser.getNovelContent(it)
            // 封面只有一行，是封面图片，
            assertEquals(1, content.size)
            // 图片地址里带临时文件路径，会变，只断言首尾，
            assertTrue("cover should be an image line", content.first().startsWith("![img](jar:"))
            assertTrue("cover should point at cover.jpeg", content.first().endsWith("/cover.jpeg)"))
        }

        // 第一个正文章节，验证正文提取以及"开头是章节名就去掉"的逻辑没有把正文吃掉，
        chapters[8].let {
            assertEquals("第1章 未来的挑战", it.name)
            assertEquals("text/part0008.html", it.extra)
            val content = parser.getNovelContent(it)
            assertEquals(25, content.size)
            assertEquals("第1章", content.first())
            assertEquals("未来的挑战", content[1])
            assertTrue("last line should be real text", content.last().isNotBlank())
        }
    }

    @Test
    fun regex() {
        val intro = "『内容简介：妖魔中的至高无上者，名为“大圣”。\n" +
                "     』"
        val res = intro.pick("内容简介：([^』]*)(』)?").first().trim()
        println(res)
    }

    /**
     * 验证自己实现的jar协议相对路径解析（absXlinkHref / absSrc），
     * 用合成的jar: baseUri, 不依赖真实文件，所以跨平台都能跑，
     */
    @Test
    fun jarUrlResolve() {
        // 一个固定的jar协议baseUri, 模拟epub内某章节文件的地址，
        val baseUri = "jar:file:/some/book.epub!/a/s/d"

        val image = Jsoup.parse("""
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1" width="100%" height="100%" viewBox="0 0 546 751" preserveAspectRatio="none">
                <image width="100%" height="100%" xlink:href="cover.jpeg"/>
            </svg>
            """, baseUri)
                .select("image").first()
        // Jsoup可以正确处理jar协议地址，
        assertEquals("jar:file:/some/book.epub!/a/s/cover.jpeg", image.absXlinkHref())

        val imageParent = Jsoup.parse("""
                <img src="../Image/Cover.jpg"/>
            """, baseUri)
                .select("img").first()
        // 自己封装的absSrc能正确解析上级目录相对路径，
        assertEquals("jar:file:/some/book.epub!/a/Image/Cover.jpg", imageParent.absSrc())
    }
}