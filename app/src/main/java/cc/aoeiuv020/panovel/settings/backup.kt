package cc.aoeiuv020.panovel.settings

import cc.aoeiuv020.panovel.util.Delegates
import cc.aoeiuv020.panovel.util.Pref

/**
 * Created by AoEiuV020 on 2021.04.25-13:42:29.
 */
object BackupSettings : Pref {
    override val name: String
        get() = "Backup"

    // 备份时各内容的勾选状态，记住用户上次的选择，
    var cbBookshelf: Boolean by Delegates.boolean(true)
    var cbBookList: Boolean by Delegates.boolean(true)
    var cbHistory: Boolean by Delegates.boolean(true)
    var cbSettings: Boolean by Delegates.boolean(true)
}
