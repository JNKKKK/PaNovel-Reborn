package cc.aoeiuv020.irondb

import kotlinx.serialization.KSerializer
import java.io.File

interface KeySerializer {
    fun serialize(from: String): String
}

interface DataSerializer {
    fun <T> serialize(value: T, serializer: KSerializer<T>): String
    fun <T> deserialize(string: String, serializer: KSerializer<T>): T
}
