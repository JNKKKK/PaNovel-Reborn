package cc.aoeiuv020.irondb.impl

import cc.aoeiuv020.irondb.KeySerializer
import cc.aoeiuv020.shared.regex.compileRegex

/**
 * 简单替换路径分隔符，因此若是名字仅这一处不同，将产生冲突，
 */
class ReplaceFileSeparator(
        private val replaceWith: String = "."
) : KeySerializer {
    companion object {
        /**
         * fat32不支持这些字符，而sdcard基本上是fat32文件系统，
         */
        val NOT_SUPPORT_CHARACTER = compileRegex("[/\\:|=?\";\\[\\],^]")
    }
    override fun serialize(from: String): String {
        return from.replace(compileRegex("[/\\:|=?\";\\[\\],^]"), replaceWith)
    }
}