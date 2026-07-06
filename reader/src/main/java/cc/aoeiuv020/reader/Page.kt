package cc.aoeiuv020.reader

import java.util.*


data class Page(
        var lines: List<Any>,
        var fitLineSpacing: Int
) {
    constructor(lines: ArrayList<Any>) : this(lines, 0)
}