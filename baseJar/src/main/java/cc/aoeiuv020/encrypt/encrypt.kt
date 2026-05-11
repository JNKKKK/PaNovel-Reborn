package cc.aoeiuv020.encrypt

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * MD5 hash of a String, returns raw bytes.
 */
fun String.md5(): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(this.toByteArray(Charsets.UTF_8))
}

/**
 * SHA-1 hash of a String, returns raw bytes.
 */
fun String.sha1(): ByteArray {
    val md = MessageDigest.getInstance("SHA-1")
    return md.digest(this.toByteArray(Charsets.UTF_8))
}

/**
 * Convert a ByteArray to lowercase hex string.
 */
fun ByteArray.hex(): String {
    return joinToString("") { "%02x".format(it) }
}

/**
 * Convert a String's UTF-8 bytes to lowercase hex string.
 */
fun String.hex(): String {
    return this.toByteArray(Charsets.UTF_8).hex()
}

/**
 * Decode a Base64-encoded String to ByteArray.
 */
fun String.base64Decode(): ByteArray {
    return Base64.getDecoder().decode(this)
}

/**
 * Decrypt a ByteArray using a symmetric cipher.
 *
 * @param key the secret key bytes
 * @param iv the initialization vector bytes
 * @param algorithm the cipher algorithm (e.g. "AES")
 * @param mode the cipher mode (e.g. "CBC")
 * @param padding the padding scheme (e.g. "NoPadding", "PKCS5Padding")
 * @return decrypted bytes
 */
fun ByteArray.cipherDecrypt(
    key: ByteArray,
    iv: ByteArray,
    algorithm: String,
    mode: String,
    padding: String
): ByteArray {
    val transformation = "$algorithm/$mode/$padding"
    val cipher = Cipher.getInstance(transformation)
    val secretKey = SecretKeySpec(key, algorithm)
    val ivSpec = IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
    return cipher.doFinal(this)
}
