package cc.aoeiuv020.panovel.util

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import cc.aoeiuv020.panovel.R

class ProgressDialogCompat(context: Context) {
    private val view: View = View.inflate(context, R.layout.dialog_progress, null)
    private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
    private val dialog: AlertDialog = AlertDialog.Builder(context)
        .setView(view)
        .setCancelable(true)
        .create()
    private var onCancelListener: (() -> Unit)? = null

    fun setOnCancelListener(listener: () -> Unit) {
        onCancelListener = listener
        dialog.setOnCancelListener { listener() }
    }

    fun setMessage(message: String) {
        tvMessage.text = message
    }

    fun show() {
        if (!dialog.isShowing) {
            dialog.show()
        } else {
            tvMessage.text = tvMessage.text
        }
    }

    fun dismiss() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    val isShowing: Boolean get() = dialog.isShowing
}
