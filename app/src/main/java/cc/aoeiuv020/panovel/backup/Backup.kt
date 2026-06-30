package cc.aoeiuv020.panovel.backup

import java.io.File

interface Backup {
    /**
     * 从备份目录恢复，备份里有什么就恢复什么，不再按选项过滤，
     */
    fun import(base: File): String

    /**
     * 按选项导出到备份目录，
     */
    fun export(base: File, options: Set<BackupOption>): String
}
