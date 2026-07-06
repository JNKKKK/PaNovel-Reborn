package cc.aoeiuv020.shared.util.io

import java.util.*

fun BufferedRandomAccessFile.readLines(beginPos: Long, endPos: Long, charset: String): List<String> {
    seek(beginPos)
    val list = LinkedList<String>()
    while (filePointer < endPos) {
        @Suppress("DEPRECATION")
        readLine(charset)?.let {
            list.add(it)
        }
    }
    return list
}
