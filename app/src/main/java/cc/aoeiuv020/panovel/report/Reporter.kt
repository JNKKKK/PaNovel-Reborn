package cc.aoeiuv020.panovel.report

import timber.log.Timber

object Reporter {
    fun unreachable() {
        post("不可到达，")
    }

    fun <T> notNullOrReport(t: T?, value: String): T {
        if (t == null) {
            val message = "<$value>不可空，"
            val e = IllegalArgumentException(message)
            post(message, e)
            throw e
        }
        return t
    }

    fun post(message: String) {
        Timber.e(IllegalStateException(message), message)
    }

    fun unreachable(e: Throwable) {
        post("不可到达，", e)
    }

    fun post(message: String, e: Throwable) {
        Timber.e(e, message)
    }
}