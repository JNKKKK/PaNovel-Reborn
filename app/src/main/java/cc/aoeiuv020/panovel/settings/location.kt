package cc.aoeiuv020.panovel.settings

import cc.aoeiuv020.panovel.backup.BackupPresenter
import cc.aoeiuv020.panovel.data.CacheManager
import cc.aoeiuv020.panovel.local.NovelExporter
import cc.aoeiuv020.panovel.util.Delegates
import cc.aoeiuv020.panovel.util.Pref

/**
 * Created by AoEiuV020 on 2018.12.31-20:41:11.
 */
@Suppress("unused")
object LocationSettings : Pref {
    override val name: String
        get() = "Location"
    var cacheLocation: String by Delegates.string(context.cacheDir.resolve(CacheManager.NAME_FOLDER).absolutePath)
    var backupLocation: String by Delegates.string(sdcardResolve(BackupPresenter.NAME_FOLDER))
    var exportLocation: String by Delegates.string(sdcardResolve(NovelExporter.NAME_FOLDER))

    // 优先SD卡，不可用就私有目录,
    private fun sdcardResolve(name: String): String = (
            context.getExternalFilesDir(null)
                    ?.resolve(name)
                    ?.apply { exists() || mkdirs() }
                    ?.takeIf { it.canWrite() }
                    ?: context.filesDir.resolve(name)
            ).absolutePath

}