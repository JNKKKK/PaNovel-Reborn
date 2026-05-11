@file:Suppress("unused")

package org.jetbrains.anko

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

val Context.ctx: Context get() = this
val Activity.ctx: Activity get() = this

fun Context.toast(message: CharSequence) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.toast(resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}

fun Context.longToast(message: CharSequence) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Context.browse(url: String, newTask: Boolean = false): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (newTask) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}

fun Context.email(email: String, subject: String = "", text: String = ""): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
    if (subject.isNotEmpty()) intent.putExtra(Intent.EXTRA_SUBJECT, subject)
    if (text.isNotEmpty()) intent.putExtra(Intent.EXTRA_TEXT, text)
    return try {
        startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}

inline fun <reified T : Activity> Context.startActivity(vararg params: Pair<String, Any?>) {
    val intent = Intent(this, T::class.java)
    params.forEach { (key, value) ->
        when (value) {
            is Int -> intent.putExtra(key, value)
            is Long -> intent.putExtra(key, value)
            is String -> intent.putExtra(key, value)
            is Boolean -> intent.putExtra(key, value)
            is Float -> intent.putExtra(key, value)
            is Double -> intent.putExtra(key, value)
            is java.io.Serializable -> intent.putExtra(key, value)
        }
    }
    startActivity(intent)
}

inline fun <reified T : Activity> Context.intentFor(vararg params: Pair<String, Any?>): Intent {
    val intent = Intent(this, T::class.java)
    params.forEach { (key, value) ->
        when (value) {
            is Int -> intent.putExtra(key, value)
            is Long -> intent.putExtra(key, value)
            is String -> intent.putExtra(key, value)
            is Boolean -> intent.putExtra(key, value)
            is Float -> intent.putExtra(key, value)
            is Double -> intent.putExtra(key, value)
            is java.io.Serializable -> intent.putExtra(key, value)
        }
    }
    return intent
}

fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
fun Context.dip(value: Float): Int = (value * resources.displayMetrics.density).toInt()

fun Context.runOnUiThread(action: () -> Unit) {
    if (this is Activity) {
        this.runOnUiThread(action)
    } else {
        CoroutineScope(Dispatchers.Main).launch { action() }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> T.doAsync(
    noinline exceptionHandler: ((Throwable) -> Unit) = {},
    noinline task: AnkoAsyncContext<T>.() -> Unit
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

@Suppress("NOTHING_TO_INLINE")
inline fun <T> T.doAsync(
    noinline exceptionHandler: ((Throwable) -> Unit) = {},
    executorService: java.util.concurrent.ExecutorService,
    noinline task: AnkoAsyncContext<T>.() -> Unit
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

class AnkoAsyncContext<T>(val weakRef: T) {
    fun uiThread(action: (T) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            action(weakRef)
        }
    }
}

// Alert dialog builder DSL
class AlertBuilder(private val ctx: Context) {
    var title: CharSequence? = null
    var titleResource: Int = 0
    var message: CharSequence? = null
    var customView: android.view.View? = null
    private var positiveButton: Pair<CharSequence, (() -> Unit)?>? = null
    private var negativeButton: Pair<CharSequence, (() -> Unit)?>? = null
    private var neutralButton: Pair<CharSequence, (() -> Unit)?>? = null
    private var onCancelListener: (() -> Unit)? = null

    fun yesButton(handler: () -> Unit) {
        positiveButton = ctx.getString(android.R.string.ok) to handler
    }

    fun okButton(handler: () -> Unit) = yesButton(handler)

    fun cancelButton(handler: () -> Unit) {
        negativeButton = ctx.getString(android.R.string.cancel) to handler
    }

    fun positiveButton(resId: Int, handler: () -> Unit) {
        positiveButton = ctx.getString(resId) to handler
    }

    fun negativeButton(resId: Int, handler: () -> Unit) {
        negativeButton = ctx.getString(resId) to handler
    }

    fun neutralPressed(text: CharSequence, handler: (DialogInterface) -> Unit) {
        neutralButton = text to { handler(object : DialogInterface {
            override fun cancel() {}
            override fun dismiss() {}
        }) }
    }

    fun neutralPressed(resId: Int, handler: (DialogInterface) -> Unit) {
        neutralPressed(ctx.getString(resId), handler)
    }

    fun onCancelled(handler: () -> Unit) {
        onCancelListener = handler
    }

    fun show(): DialogInterface {
        return build().apply { show() }
    }

    fun build(): AlertDialog {
        val builder = AlertDialog.Builder(ctx)
        if (titleResource != 0) builder.setTitle(titleResource)
        title?.let { builder.setTitle(it) }
        message?.let { builder.setMessage(it) }
        customView?.let { builder.setView(it) }
        positiveButton?.let { (text, handler) ->
            builder.setPositiveButton(text) { _, _ -> handler?.invoke() }
        }
        negativeButton?.let { (text, handler) ->
            builder.setNegativeButton(text) { _, _ -> handler?.invoke() }
        }
        neutralButton?.let { (text, handler) ->
            builder.setNeutralButton(text) { _, _ -> handler?.invoke() }
        }
        onCancelListener?.let { handler ->
            builder.setOnCancelListener { handler() }
        }
        return builder.create()
    }
}

fun Context.alert(init: AlertBuilder.() -> Unit): AlertBuilder {
    return AlertBuilder(this).apply(init)
}

fun Context.alert(message: CharSequence, title: CharSequence? = null, init: AlertBuilder.() -> Unit = {}): AlertBuilder {
    return AlertBuilder(this).apply {
        this.message = message
        title?.let { this.title = it }
        init()
    }
}

fun Context.alert(messageId: Int, init: AlertBuilder.() -> Unit = {}): AlertBuilder {
    return AlertBuilder(this).apply {
        this.message = ctx.getString(messageId)
        init()
    }
}

fun Context.selector(title: CharSequence?, items: List<CharSequence>, onClick: (DialogInterface, Int) -> Unit) {
    AlertDialog.Builder(this).apply {
        title?.let { setTitle(it) }
        setItems(items.toTypedArray()) { dialog, which -> onClick(dialog, which) }
    }.show()
}
