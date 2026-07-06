package cc.aoeiuv020.panovel.bookfile

import java.io.File

/**
 * 调用parse方法解析后能拿出其他数据，
 */
abstract class LocalNovelParser(
        protected val file: File
) : ContentProvider {

    abstract fun parse(): LocalNovelInfo
}