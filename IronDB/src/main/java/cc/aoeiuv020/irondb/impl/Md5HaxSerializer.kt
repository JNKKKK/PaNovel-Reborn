package cc.aoeiuv020.irondb.impl

import cc.aoeiuv020.irondb.KeySerializer
import java.security.MessageDigest

/**
 * 慎用，多级使用md5进行序列化可能导致路径过长，
 * 序列化成utf-8转md5转16进制的小写，
 *
 * Created by AoEiuV020 on 2018.05.27-16:17:39.
 */
class Md5HaxSerializer : KeySerializer {
    override fun serialize(from: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(from.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}