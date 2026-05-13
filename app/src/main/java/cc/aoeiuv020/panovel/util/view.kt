@file:Suppress("unused")
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package cc.aoeiuv020.panovel.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.BaseBundle
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.EditText
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.report.Reporter
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit


/**
 *
 * Created by AoEiuV020 on 2017.10.02-21:50:34.
 */

fun Context.loading(dialog: ProgressDialogCompat, id: Int) =
    loading(dialog, getString(R.string.loading, getString(id)))

fun Context.loading(dialog: ProgressDialogCompat, str: String) {
    dialog.setMessage(str)
    dialog.show()
}

fun Context.alertError(dialog: AlertDialog, str: String, e: Throwable) =
    alert(dialog, str + "\n" + e.message)

fun Context.alert(dialog: AlertDialog, messageId: Int) = alert(dialog, getString(messageId))
fun Context.alert(dialog: AlertDialog, messageId: Int, titleId: Int) =
    alert(dialog, getString(messageId), getString(titleId))

fun Context.alert(dialog: AlertDialog, message: String, title: String? = null) = dialog.apply {
    setMessage(message)
    title?.let {
        setTitle(title)
    }
    show()
}

fun View.hide() {
    visibility = View.GONE
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.setSize(size: Int) {
    layoutParams = layoutParams.also {
        it.height = size
        it.width = size
    }
}

fun View.setHeight(height: Int) {
    layoutParams = layoutParams.also { it.height = height }
}

fun Context.changeColor(initial: Int, callback: (color: Int) -> Unit) {
    val layout = View.inflate(this, R.layout.dialog_editor, null)
    val etColor = layout.findViewById<EditText>(R.id.editText).apply {
        setText(java.lang.Integer.toHexString(initial).uppercase())
    }
    AlertDialog.Builder(this)
        .setTitle(R.string.colorARGB)
        .setView(layout)
        .setNeutralButton(R.string.picker) { _, _ ->
            alertColorPicker(initial, callback)
        }
        .setPositiveButton(android.R.string.ok) { _, _ ->
            try {
                val iColor = etColor.text.toString().toLong(16).toInt()
                callback(iColor)
            } catch (e: NumberFormatException) {
            }
        }
        .setNegativeButton(android.R.string.cancel, null)
        .create().safelyShow()
}


fun Context.alertColorPicker(initial: Int, callback: (color: Int) -> Unit) =
    ColorPickerDialogBuilder.with(this)
        .initialColor(initial)
        .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
        .setOnColorChangedListener(callback)
        .setPositiveButton(android.R.string.yes) { _, color, _ -> callback(color) }
        // 因为取消前可能已经选了颜色，所以要设置一次初始的颜色，
        .setNegativeButton(android.R.string.cancel) { _, _ -> callback(initial) }
        .build().apply {
            // 去除对话框的灰背景，
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }.safelyShow()

/**
 * https://stackoverflow.com/a/38244327/5615186
 */
fun Context.getBitmapFromVectorDrawable(drawableId: Int): Bitmap {
    var drawable = ContextCompat.getDrawable(this, drawableId)!!
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        drawable = DrawableCompat.wrap(drawable).mutate()
    }

    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}

fun EditText.showKeyboard() {
    if (hasFocus()) {
        clearFocus()
    }
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, 0)
}

@Suppress("UNCHECKED_CAST")
fun Bundle.toMap(): Map<String, Any?> =
    BaseBundle::class.java.getDeclaredField("mMap").apply { isAccessible = true }
        .get(this) as Map<String, *>

// 从保存的状态或者传入的intent中拿String,
fun Activity.getStringExtra(key: String, savedInstanceState: Bundle? = null): String? =
    savedInstanceState?.run { getString(key) }
        ?: intent.getStringExtra(key)

fun Activity.setBrightness(brightness: Int) {
    if (brightness < 0) {
        setBrightnessFollowSystem()
    } else {
        window.attributes = window.attributes.apply {
            screenBrightness = minOf(255, brightness) / 255f
        }
    }
}

fun Activity.setBrightnessFollowSystem() {
    window.attributes = window.attributes.apply {
        screenBrightness = -1f
    }
}

/**
 * 没有图片的小说统一用这个填充图片地址，
 * 展示的时候换成内置的图片，
 */
val noCover: String get() = "https://www.snwx8.com/modules/article/images/nocover.jpg"

/**
 * 不希望展示对话框失败导致崩溃，
 */
fun Dialog.safelyShow(): DialogInterface {
    try {
        show()
    } catch (e: Exception) {
        val message = "展示对话框失败，"
        Reporter.post(message, e)
    }
    return this
}


/**
 * 简单提示一些信息，
 */
@UiThread
fun Context.tip(
    s: String
) {
    AlertDialog.Builder(this)
        .setMessage(s)
        .setPositiveButton(android.R.string.ok, null)
        .show()

}

/**
 * 简单二次确认一些信息，
 */
@UiThread
fun Context.confirm(
    s: String,
    onConfirm: Runnable
) {
    AlertDialog.Builder(this)
        .setMessage(s)
        .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm.run() }
        .setNegativeButton(android.R.string.cancel, null)
        .show()

}

@WorkerThread
fun Context.uiSelect(
    name: String,
    items: Array<String>,
    default: Int,
    timeout: Long = TimeUnit.MINUTES.toMillis(1)
): Int? = runBlocking {
    withTimeoutOrNull(timeout) {
        suspendCancellableCoroutine { cont ->
            val mainScope = CoroutineScope(Dispatchers.Main)
            mainScope.launch {
                val dialog = AlertDialog.Builder(this@uiSelect).apply {
                    setTitle(this@uiSelect.getString(R.string.select_placeholder, name))
                    setSingleChoiceItems(items, default) { d, which ->
                        d.dismiss()
                        if (cont.isActive) cont.resume(which, null)
                    }
                    setOnCancelListener {
                        if (cont.isActive) cont.resume(null, null)
                    }
                }.create().safelyShow()
                cont.invokeOnCancellation {
                    dialog.dismiss()
                }
            }
        }
    }
}

@WorkerThread
fun Context.uiInput(
    name: String,
    default: String,
    timeout: Long = TimeUnit.MINUTES.toMillis(1),
    multiLine: Boolean = false
): String? = runBlocking {
    withTimeoutOrNull(timeout) {
        suspendCancellableCoroutine { cont ->
            val mainScope = CoroutineScope(Dispatchers.Main)
            mainScope.launch {
                val layout = View.inflate(this@uiInput, R.layout.dialog_editor, null)
                val etName = layout.findViewById<EditText>(R.id.editText)
                if (multiLine) {
                    etName.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                }
                etName.setText(default)
                val dialog = AlertDialog.Builder(this@uiInput)
                    .setTitle(this@uiInput.getString(R.string.input_placeholder, name))
                    .setView(layout)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        if (cont.isActive) cont.resume(etName.text.toString(), null)
                    }
                    .setOnCancelListener {
                        if (cont.isActive) cont.resume(null, null)
                    }
                    .create().safelyShow()
                cont.invokeOnCancellation {
                    dialog.dismiss()
                }
            }
        }
    }
}

@UiThread
fun WebView.onActivityDestroy() {
    (parent as? ViewGroup)?.removeView(this)
    clearHistory()
    clearCache(true)
    loadUrl("about:blank")
    freeMemory()
    pauseTimers()
    destroy()
}