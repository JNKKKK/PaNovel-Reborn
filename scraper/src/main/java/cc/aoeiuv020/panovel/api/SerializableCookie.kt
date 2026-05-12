package cc.aoeiuv020.panovel.api

import kotlinx.serialization.Serializable
import okhttp3.Cookie

@Serializable
data class SerializableCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean,
    val persistent: Boolean
) {
    fun toCookie(): Cookie = Cookie.Builder()
        .name(name)
        .value(value)
        .apply { if (hostOnly) domain(domain) else domain(domain) }
        .path(path)
        .expiresAt(expiresAt)
        .apply { if (secure) secure() }
        .apply { if (httpOnly) httpOnly() }
        .build()

    companion object {
        fun from(cookie: Cookie) = SerializableCookie(
            name = cookie.name,
            value = cookie.value,
            domain = cookie.domain,
            path = cookie.path,
            expiresAt = cookie.expiresAt,
            secure = cookie.secure,
            httpOnly = cookie.httpOnly,
            hostOnly = cookie.hostOnly,
            persistent = cookie.persistent
        )
    }
}
