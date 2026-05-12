package com.tencent.bugly.crashreport

import android.content.Context

/**
 * Stub for Bugly CrashReport SDK.
 * The original dependency (com.tencent.bugly:crashreport:2.6.6) has a corrupt aar.
 * This stub provides no-op implementations to allow compilation.
 */
object CrashReport {
    @JvmStatic
    fun initCrashReport(context: Context, appId: String, debug: Boolean) {
        // no-op
    }

    @JvmStatic
    fun setIsDevelopmentDevice(context: Context, isDev: Boolean) {
        // no-op
    }

    @JvmStatic
    fun setUserId(userId: String) {
        // no-op
    }

    @JvmStatic
    fun postCatchedException(e: Throwable) {
        // no-op
    }
}
