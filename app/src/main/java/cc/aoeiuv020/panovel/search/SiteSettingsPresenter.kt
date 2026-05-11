package cc.aoeiuv020.panovel.search

import cc.aoeiuv020.panovel.Presenter
import cc.aoeiuv020.panovel.api.NovelContext
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.util.notNullOrReport
import cc.aoeiuv020.regex.compileRegex
import okhttp3.Cookie
import okhttp3.Headers
import okhttp3.HttpUrl
import java.nio.charset.Charset
import timber.log.Timber
import kotlinx.coroutines.*

/**
 * Created by AoEiuV020 on 2019.05.01-19:56:11.
 */
class SiteSettingsPresenter(
        private val site: String
) : Presenter<SiteSettingsActivity>() {
    private lateinit var context: NovelContext

    fun start() {
        scope.launch {
            try {
                context = withContext(Dispatchers.IO) {
                    DataManager.getNovelContextByName(site)
                }
                view?.init()
            } catch (e: Exception) {
                val message = "读取网站失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
                view?.finish()
            }
        }
    }

    fun setCookie(input: (String) -> String?, success: () -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val oldCookie = context.cookies.values.joinToString("; ") {
                        it.run { "${name()}=${value()}" }
                    }
                    val newCookie = input(oldCookie)
                            ?: return@withContext null
                    val cookieMap = newCookie.split(";").mapNotNull { cookiePair ->
                        Timber.d("pull cookie: <$cookiePair>")
                        // 取出来的cookiePair只有name=value，Cookie.parse一定能通过，也因此可能有超时信息拿不出来的问题，
                        Cookie.parse(HttpUrl.parse(context.site.baseUrl).notNullOrReport(), cookiePair)?.let { cookie ->
                            cookie.name() to cookie
                        }
                    }.toMap()
                    context.replaceCookies(cookieMap)
                    cookieMap
                } ?: return@launch
                success()
            } catch (e: Exception) {
                val message = "设置Cookie失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun setHeader(input: (String) -> String?, success: () -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val old = context.headers.toList().joinToString("\n") { (name, value) ->
                        "$name: $value"
                    }
                    val new = input(old)
                            ?: return@withContext null
                    val headers: Headers = Headers.of(*new.split(compileRegex("\n|(: *)")).toTypedArray())
                    context.replaceHeaders(headers)
                    headers
                } ?: return@launch
                success()
            } catch (e: Exception) {
                val message = "设置Header失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun setCharset(input: (String) -> String?, success: () -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val old = context.forceCharset ?: context.charset ?: context.defaultCharset
                    val new = input(old)
                            ?: return@withContext null
                    if (new.isNotBlank()) {
                        Charset.forName(new)
                    }
                    context.forceCharset = new
                    new
                } ?: return@launch
                success()
            } catch (e: Exception) {
                val message = "设置编码失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                view?.showError(message, e)
            }
        }
    }

    fun getReason(): String {
        return context.reason
    }

    fun isUpkeep(): Boolean {
        return context.upkeep
    }
}
