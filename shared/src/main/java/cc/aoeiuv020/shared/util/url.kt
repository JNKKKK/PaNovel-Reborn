@file:Suppress("DEPRECATION")

package cc.aoeiuv020.shared.util

import java.net.MalformedURLException
import java.net.URL

fun path(url: String): String = try {
    URL(url).path
} catch (e: MalformedURLException) {
    url
}

fun toURL(spec: String): URL = URL(spec)

fun toURL(base: URL, spec: String): URL = URL(base, spec)
