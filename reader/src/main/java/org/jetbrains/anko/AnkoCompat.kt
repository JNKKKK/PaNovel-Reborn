@file:Suppress("unused")

package org.jetbrains.anko

import android.content.Context
import kotlinx.coroutines.*
import timber.log.Timber

interface AnkoLogger {
    val loggerTag: String get() = javaClass.simpleName
}

inline fun AnkoLogger.verbose(message: () -> Any?) {
    Timber.tag(loggerTag).v(message()?.toString() ?: "null")
}

inline fun AnkoLogger.debug(message: () -> Any?) {
    Timber.tag(loggerTag).d(message()?.toString() ?: "null")
}

inline fun AnkoLogger.info(message: () -> Any?) {
    Timber.tag(loggerTag).i(message()?.toString() ?: "null")
}

inline fun AnkoLogger.warn(message: () -> Any?) {
    Timber.tag(loggerTag).w(message()?.toString() ?: "null")
}

inline fun AnkoLogger.error(message: () -> Any?) {
    Timber.tag(loggerTag).e(message()?.toString() ?: "null")
}

inline fun AnkoLogger.error(message: String, e: Throwable) {
    Timber.tag(loggerTag).e(e, message)
}

fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
fun Context.dip(value: Float): Int = (value * resources.displayMetrics.density).toInt()
fun Context.sp(value: Int): Int = (value * resources.displayMetrics.scaledDensity).toInt()
fun Context.sp(value: Float): Int = (value * resources.displayMetrics.scaledDensity).toInt()

fun <T> T.doAsync(
    exceptionHandler: ((Throwable) -> Unit) = {},
    executorService: java.util.concurrent.ExecutorService,
    task: AnkoAsyncContext<T>.() -> Unit
): Job {
    val context = AnkoAsyncContext(this)
    return CoroutineScope(executorService.asCoroutineDispatcher()).launch {
        try {
            context.task()
        } catch (e: Exception) {
            exceptionHandler(e)
        }
    }
}

fun <T> T.doAsync(
    exceptionHandler: ((Throwable) -> Unit) = {},
    task: AnkoAsyncContext<T>.() -> Unit
): Job {
    val context = AnkoAsyncContext(this)
    return CoroutineScope(Dispatchers.IO).launch {
        try {
            context.task()
        } catch (e: Exception) {
            exceptionHandler(e)
        }
    }
}

class AnkoAsyncContext<T>(val weakRef: T) {
    fun uiThread(action: (T) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            action(weakRef)
        }
    }
}
