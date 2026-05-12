package cc.aoeiuv020.panovel.util

import cc.aoeiuv020.panovel.report.Reporter

inline fun <reified T> T?.notNullOrReport(): T =
        notNullOrReport(T::class.simpleName ?: "Unknown")

fun <T> T?.notNullOrReport(value: String): T =
        Reporter.notNullOrReport(this, value)
