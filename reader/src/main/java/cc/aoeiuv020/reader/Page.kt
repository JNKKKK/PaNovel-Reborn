package cc.aoeiuv020.reader

import java.util.*


/**
 *
 * Created by AoEiuV020 on 2017.12.08-00:48:07.
 */
data class Page(
        var lines: List<Any>,
        var fitLineSpacing: Int
) {
    constructor(lines: ArrayList<Any>) : this(lines, 0)
}