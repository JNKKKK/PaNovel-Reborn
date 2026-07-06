package cc.aoeiuv020.reader

import android.view.View

fun View.setHeight(height: Int) {
    layoutParams = layoutParams.also { it.height = height }
}

fun View.hide() {
    visibility = View.GONE
}

fun View.show() {
    visibility = View.VISIBLE
}
