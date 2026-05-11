package cc.aoeiuv020.string

data class StringPair(val first: String, val second: String)

fun String.divide(delimiter: Char): StringPair {
    val index = indexOf(delimiter)
    if (index == -1) return StringPair(this, "")
    return StringPair(substring(0, index), substring(index + 1))
}

fun String.lastDivide(delimiter: Char): StringPair {
    val index = lastIndexOf(delimiter)
    if (index == -1) return StringPair(this, "")
    return StringPair(substring(0, index), substring(index + 1))
}
