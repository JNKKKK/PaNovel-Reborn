package cc.aoeiuv020.panovel.bookfile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.Charset

/**
 * Previewer是app导入本地小说的入口，覆盖类型识别、编码猜测和分发解析，
 */
class PreviewerTest : ParserTest(TextParser::class) {

    @Test
    fun guessType() {
        // getResource拷出来的临时文件没有后缀，所以显式传uri来测后缀识别逻辑，
        val file = getResource("/panovel.txt") ?: return
        assertEquals(LocalNovelType.EPUB, Previewer(file, "file:///x/book.epub").guessType())
        assertEquals(LocalNovelType.TEXT, Previewer(file, "file:///x/book.txt").guessType())
        // 没有可识别后缀就返回null, 交给外面进一步判断，
        assertNull(Previewer(file, "file:///x/book").guessType())
    }

    @Test
    fun guessCharsetText() {
        val utf = getResource("/panovel.txt") ?: return
        assertEquals("UTF-8", Previewer(utf).guessCharset(LocalNovelType.TEXT))

        val gbk = getResource("/zxcs.txt") ?: return
        // 知轩藏书的txt是GB2312系，
        assertEquals("GB2312", Previewer(gbk).guessCharset(LocalNovelType.TEXT))
    }

    @Test
    fun guessCharsetEpub() {
        val epub = getResource("/zero-to-one.epub") ?: return
        // epub解析opf判断编码，
        assertEquals("UTF-8", Previewer(epub).guessCharset(LocalNovelType.EPUB))
    }

    @Test
    fun parseDispatchesToEpub() {
        val epub = getResource("/zero-to-one.epub") ?: return
        val info = Previewer(epub).parse(LocalNovelType.EPUB, Charset.forName("UTF-8"))
        assertEquals("从0到1:开启商业与未来的秘密（图文精编版） (奇点系列)", info.name)
        assertEquals("彼得·蒂尔", info.author)
    }

    @Test
    fun parseDispatchesToText() {
        val txt = getResource("/panovel.txt") ?: return
        val info = Previewer(txt).parse(LocalNovelType.TEXT, Charset.forName("UTF-8"))
        assertEquals("修真聊天群", info.name)
        assertEquals("圣骑士的传说", info.author)
    }
}
