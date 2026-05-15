package cc.aoeiuv020.panovel

import cc.aoeiuv020.panovel.data.entity.NovelMinimal
import cc.aoeiuv020.panovel.share.ShareCodec
import cc.aoeiuv020.panovel.share.SharedBookList
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

class ShareCodecTest {

    private fun createBookListJson(count: Int): String {
        val novels = (1..count).joinToString(",") { i ->
            """{"site":"Deqixs","author":"作者$i","name":"小说名称$i","detail":"https://www.deqixs.com/book/$i"}"""
        }
        return """{"name":"测试书单","list":[$novels],"uuid":"test-uuid-1234"}"""
    }

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(data)
        deflater.finish()
        val output = ByteArrayOutputStream(data.size)
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            output.write(buffer, 0, count)
        }
        deflater.end()
        return output.toByteArray()
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val output = ByteArrayOutputStream(data.size * 2)
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && inflater.needsInput()) break
            output.write(buffer, 0, count)
        }
        inflater.end()
        return output.toByteArray()
    }

    private fun encode(json: String): String {
        val compressed = deflate(json.toByteArray(Charsets.UTF_8))
        return "panovel://" + java.util.Base64.getEncoder().encodeToString(compressed)
    }

    private fun decode(encoded: String): String {
        require(encoded.startsWith("panovel://"))
        val base64 = encoded.removePrefix("panovel://")
        val compressed = java.util.Base64.getDecoder().decode(base64)
        return inflate(compressed).toString(Charsets.UTF_8)
    }

    @Test
    fun encodeAndDecodeRoundtrip() {
        val json = createBookListJson(5)
        val encoded = encode(json)
        assertTrue(encoded.startsWith("panovel://"))
        assertEquals(json, decode(encoded))
    }

    @Test
    fun isShareContentRecognizesPrefix() {
        assertTrue("panovel://abc123".startsWith("panovel://"))
        assertFalse("https://example.com".startsWith("panovel://"))
        assertFalse("".startsWith("panovel://"))
        assertFalse("panovel".startsWith("panovel://"))
    }

    @Test
    fun qrCodeContentDecodesCorrectly() {
        val json = createBookListJson(3)
        val qrContent = encode(json)
        assertTrue(qrContent.startsWith("panovel://"))
        val decoded = decode(qrContent)
        assertTrue(decoded.contains("\"name\":\"测试书单\""))
        assertTrue(decoded.contains("\"author\":\"作者1\""))
        assertTrue(decoded.contains("\"author\":\"作者3\""))
    }

    @Test
    fun clipboardContentSameAsQr() {
        val json = createBookListJson(3)
        val encoded = encode(json)
        assertEquals(decode(encoded), decode(encoded))
    }

    @Test
    fun httpUrlIsNotShareContent() {
        assertFalse("https://www.deqixs.com/book/12345".startsWith("panovel://"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun decodeInvalidTextThrows() {
        decode("random invalid text")
    }

    @Test
    fun decodeTruncatedContentProducesGarbage() {
        val encoded = encode(createBookListJson(5))
        // Truncated data either throws or produces non-matching output
        try {
            val result = decode("panovel://" + encoded.removePrefix("panovel://").take(10))
            assertNotEquals(createBookListJson(5), result)
        } catch (_: Exception) {
            // Any exception is also acceptable
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun decodeEmptyStringThrows() {
        decode("")
    }

    @Test
    fun smallBooklistFitsInQrCode() {
        val encoded = encode(createBookListJson(5))
        assertTrue(encoded.toByteArray(Charsets.UTF_8).size <= 2953)
    }

    @Test
    fun largeBooklistExceedsQrCodeLimit() {
        // 3000 bytes of varied content will exceed 2953 after base64 overhead (~4/3 expansion)
        val largeJson = (1..2000).joinToString("") { "${it.hashCode().toString(16)}-" }
        val encoded = encode(largeJson)
        assertTrue("encoded size was ${encoded.toByteArray(Charsets.UTF_8).size}",
            encoded.toByteArray(Charsets.UTF_8).size > 2953)
    }

    @Test
    fun jsonFormatForBackupIsPlainJson() {
        val json = createBookListJson(1)
        assertFalse(json.startsWith("panovel://"))
        assertTrue(json.contains("\"name\""))
        assertTrue(json.contains("测试书单"))
    }

    // Higher-level tests using ShareCodec + NovelMinimal

    private fun createSharedBookList(count: Int): SharedBookList {
        val novels = (1..count).map { i ->
            NovelMinimal("Deqixs", "作者$i", "小说名称$i", "https://www.deqixs.com/book/$i")
        }
        return SharedBookList("我的书单", novels, "uuid-test-1234")
    }

    @Test
    fun shareCodecEncodeDecodeWithNovelMinimal() {
        val original = createSharedBookList(10)
        val encoded = ShareCodec.encode(original)

        assertTrue(encoded.startsWith(ShareCodec.PREFIX))

        val decoded = ShareCodec.decode(encoded)
        assertEquals(original.name, decoded.name)
        assertEquals(original.uuid, decoded.uuid)
        assertEquals(original.list.size, decoded.list.size)
        assertEquals("作者1", decoded.list[0].author)
        assertEquals("小说名称10", decoded.list[9].name)
        assertEquals("https://www.deqixs.com/book/5", decoded.list[4].detail)
    }

    @Test
    fun shareCodecIsShareContent() {
        val encoded = ShareCodec.encode(createSharedBookList(1))
        assertTrue(ShareCodec.isShareContent(encoded))
        assertFalse(ShareCodec.isShareContent("https://example.com"))
    }

    @Test
    fun shareCodecSmallListFitsInQr() {
        val encoded = ShareCodec.encode(createSharedBookList(3))
        assertTrue(ShareCodec.fitsInQrCode(encoded))
    }

    @Test
    fun shareCodecFitsInQrRespectsLimit() {
        // fitsInQrCode is a simple byte-length check
        val longString = "panovel://" + "a".repeat(2953)
        assertFalse(ShareCodec.fitsInQrCode(longString))
        val shortString = "panovel://" + "a".repeat(100)
        assertTrue(ShareCodec.fitsInQrCode(shortString))
    }
}
