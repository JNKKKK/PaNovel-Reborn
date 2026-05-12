package cc.aoeiuv020.panovel.server.common

import cc.aoeiuv020.json.AppJson
import kotlinx.serialization.encodeToString

inline fun <reified T> String.toBean(): T = AppJson.decodeFromString(this)
inline fun <reified T> T.toJson(): String = AppJson.encodeToString(this)
