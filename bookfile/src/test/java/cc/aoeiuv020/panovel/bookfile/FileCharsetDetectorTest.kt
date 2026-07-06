package cc.aoeiuv020.panovel.bookfile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 覆盖编码探测，TXT导入时用它给用户提供默认编码，
 */
class FileCharsetDetectorTest : ParserTest(TextParser::class) {

    @Test
    fun detectUtf8() {
        val file = getResource("/panovel.txt") ?: return
        file.inputStream().use { input ->
            assertEquals("UTF-8", FileCharsetDetector.guessStreamEncoding(input))
        }
    }

    @Test
    fun detectGbk() {
        val file = getResource("/zxcs.txt") ?: return
        file.inputStream().use { input ->
            // 简体中文提示下，知轩藏书的txt识别为GB2312,
            assertEquals("GB2312", FileCharsetDetector.guessStreamEncoding(input, FileCharsetDetector.SIMPLIFIED_CHINESE))
        }
    }

    @Test
    fun detectAscii() {
        // 纯ascii内容应当被识别为ASCII,
        val file = folder.newFile("ascii.txt")
        file.writeText("hello world\nthis is plain ascii text\n", Charsets.US_ASCII)
        file.inputStream().use { input ->
            assertEquals("ASCII", FileCharsetDetector.guessStreamEncoding(input))
        }
    }

    @Test
    fun detectReturnsSomethingForEpubOpf() {
        // 拿epub的内容流也应当能给出一个非空猜测，不崩，
        val epub = getResource("/zero-to-one.epub") ?: return
        val charset = Previewer(epub).guessCharset(LocalNovelType.EPUB)
        assertNotNull(charset)
    }
}
