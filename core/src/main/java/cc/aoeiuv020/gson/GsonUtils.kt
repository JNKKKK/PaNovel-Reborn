package cc.aoeiuv020.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object GsonUtils {
    val gsonBuilder: GsonBuilder = GsonBuilder()
    val gson: Gson by lazy { gsonBuilder.create() }
}

inline fun <reified T> type(): Type = object : TypeToken<T>() {}.type

fun <T> String.toBean(gson: Gson, type: Type): T = gson.fromJson(this, type)

inline fun <reified T> String.toBean(gson: Gson = GsonUtils.gson): T =
    gson.fromJson(this, object : TypeToken<T>() {}.type)

fun Any.toJson(gson: Gson = GsonUtils.gson): String = gson.toJson(this)
