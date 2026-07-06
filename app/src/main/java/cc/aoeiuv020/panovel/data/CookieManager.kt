@file:Suppress("DEPRECATION")

// 用到Cookies相关的不少过时方法，兼容低版本需要，

package cc.aoeiuv020.panovel.data

import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import timber.log.Timber

class CookieManager(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val cookieManager = CookieManager.getInstance()

    fun putCookie(domain: String, cookiePair: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setCookie(domain, cookiePair) {
                Timber.d("cookie has been set $it, <$${DataManager.cookie}>")
            }
        } else {
            cookieManager.setCookie(domain, cookiePair)
            Timber.d("cookie has been set, <$${DataManager.cookie}>")
        }

    }

    // 莫名，旧版刷新方法需要Context,
    // 方便起见，传入ctx可空，为空就不刷低版本的了，
    fun sync(context: Context?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.flush()
        } else {
            context ?: return
            val syncManager = CookieSyncManager.createInstance(context)
            syncManager.sync()
        }
    }

    fun getCookies(domain: String): String? {
        return cookieManager.getCookie(domain)
    }

    fun removeCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null)
        } else {
            cookieManager.removeAllCookie()
        }
    }
}