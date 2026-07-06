package cc.aoeiuv020.panovel.util

import android.content.Context

/**
 * 文件操作相关，
 */
fun Context.assetsRead(name: String): String = assets.open(name).reader().readText()