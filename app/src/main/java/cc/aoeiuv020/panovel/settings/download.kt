@file:Suppress("DEPRECATION")

package cc.aoeiuv020.panovel.settings

import cc.aoeiuv020.panovel.util.Delegates
import cc.aoeiuv020.panovel.util.Pref

/**
 * Created by AoEiuV020 on 2018.11.11-11:52:46.
 */
object DownloadSettings : Pref {
    override val name: String
        get() = "Download"
    /**
     * 下载线程数，
     */
    var downloadThreadsLimit: Int by Delegates.int(4)
    /**
     * 下载进度用通知方式展示进度，
     */
    var downloadProgress: Boolean by Delegates.boolean(true)
    /**
     * 下载线程具体进度用通知方式展示进度，
     */
    var downloadThreadProgress: Boolean by Delegates.boolean(false)
    /**
     * 书架小说刷新章节列表后如果新增章节数小于等于该值就自动缓存新章节，
     */
    var autoDownloadCount: Int by Delegates.int(2)
    /**
     * 每次下载章节之间的间隔（毫秒），防止请求过快被封，
     * 0表示不等待，
     */
    var downloadInterval: Int by Delegates.int(500)

}