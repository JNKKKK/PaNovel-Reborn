package cc.aoeiuv020.shared.util

/**
 * 判断一个码点是否是汉字（含扩展区），用于长按取词时过滤掉标点、空白、拉丁字母等，
 * 避免对非汉字做无意义的词典查询。
 */
fun isHan(codePoint: Int): Boolean =
    Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN

fun Char.isHan(): Boolean = isHan(this.code)
