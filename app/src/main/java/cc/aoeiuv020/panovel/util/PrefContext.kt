package cc.aoeiuv020.panovel.util

import android.content.Context

object PrefContext {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
