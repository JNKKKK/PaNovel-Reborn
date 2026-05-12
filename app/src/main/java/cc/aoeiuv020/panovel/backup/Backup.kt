package cc.aoeiuv020.panovel.backup

import java.io.File

interface Backup {
    fun import(base: File, options: Set<BackupOption>): String
    fun export(base: File, options: Set<BackupOption>): String
}