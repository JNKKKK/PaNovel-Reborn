package cc.aoeiuv020.irondb

import java.io.File

interface FileWrapper {
    fun <T> use(block: (File) -> T): T
    fun delete(): Boolean
}