package cc.aoeiuv020.jsonpath

import com.google.gson.JsonParser

object JsonPathUtils {
    var gson: com.google.gson.Gson = com.google.gson.Gson()
}

class JsonPath(private val json: String) {
    fun get(path: String): String {
        val element = JsonParser.parseString(json)
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has(path)) {
                val value = obj.get(path)
                return if (value.isJsonPrimitive) value.asString else value.toString()
            }
        }
        if (element.isJsonArray) {
            val arr = element.asJsonArray
            val index = path.toIntOrNull()
            if (index != null && index < arr.size()) {
                val value = arr.get(index)
                return if (value.isJsonPrimitive) value.asString else value.toString()
            }
        }
        return ""
    }

    inline fun <reified T> get(path: String, gson: com.google.gson.Gson = JsonPathUtils.gson): T {
        val raw = get(path)
        return gson.fromJson(raw, T::class.java)
    }
}

val String.jsonPath: JsonPath get() = JsonPath(this)

fun <T> JsonPath.get(path: String, clazz: Class<T>, gson: com.google.gson.Gson = JsonPathUtils.gson): T {
    val raw = get(path)
    return gson.fromJson(raw, clazz)
}
