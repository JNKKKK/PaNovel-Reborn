package cc.aoeiuv020.panovel.share

import cc.aoeiuv020.json.AppJson
import kotlinx.serialization.encodeToString
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

object ShareCodec {
    const val PREFIX = "panovel://"
    const val QR_MAX_BYTES = 2953

    fun encode(shared: SharedBookList): String {
        val json = AppJson.encodeToString(shared)
        val compressed = deflate(json.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.getEncoder().encodeToString(compressed)
    }

    fun decode(text: String): SharedBookList {
        require(text.startsWith(PREFIX)) { "Invalid share content" }
        val base64 = text.removePrefix(PREFIX)
        val compressed = Base64.getDecoder().decode(base64)
        val json = inflate(compressed).toString(Charsets.UTF_8)
        return AppJson.decodeFromString<SharedBookList>(json)
    }

    fun isShareContent(text: String): Boolean = text.startsWith(PREFIX)

    fun fitsInQrCode(encoded: String): Boolean =
        encoded.toByteArray(Charsets.UTF_8).size <= QR_MAX_BYTES

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
            if (count == 0 && inflater.needsInput()) error("Incomplete compressed data")
            output.write(buffer, 0, count)
        }
        inflater.end()
        return output.toByteArray()
    }
}
