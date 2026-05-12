package cc.aoeiuv020.irondb.impl

import cc.aoeiuv020.irondb.DataSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class KotlinxSerializer(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
) : DataSerializer {
    override fun <T> serialize(value: T, serializer: KSerializer<T>): String =
        json.encodeToString(serializer, value)

    override fun <T> deserialize(string: String, serializer: KSerializer<T>): T =
        json.decodeFromString(serializer, string)
}
