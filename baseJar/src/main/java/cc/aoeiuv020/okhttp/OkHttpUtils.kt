package cc.aoeiuv020.okhttp

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object OkHttpUtils {
    var client: OkHttpClient = OkHttpClient.Builder().build()

    fun get(url: String): Call {
        val request = Request.Builder().url(url).build()
        return client.newCall(request)
    }
}

fun get(url: String): Call = OkHttpUtils.get(url)

fun OkHttpClient.get(url: String): Call {
    val request = Request.Builder().url(url).build()
    return this.newCall(request)
}

fun Call.string(): String {
    val response = this.execute()
    return response.body?.string() ?: ""
}

fun Response.charset(): String? {
    return body?.contentType()?.charset()?.name()
}

fun Response.url(): String {
    return request.url.toString()
}

fun OkHttpClient.Builder.sslAllowAll(): OkHttpClient.Builder {
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustAllCerts, SecureRandom())
    return sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
}
