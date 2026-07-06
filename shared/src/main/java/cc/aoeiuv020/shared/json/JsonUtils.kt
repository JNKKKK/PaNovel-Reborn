package cc.aoeiuv020.shared.json

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    encodeDefaults = true
}

inline fun <reified T> String.decodeJson(): T = AppJson.decodeFromString(this)
inline fun <reified T> T.encodeJson(): String = AppJson.encodeToString(this)
