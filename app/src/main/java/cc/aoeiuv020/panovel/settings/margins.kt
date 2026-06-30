package cc.aoeiuv020.panovel.settings

import cc.aoeiuv020.panovel.util.Delegates
import cc.aoeiuv020.panovel.util.SubPref
import cc.aoeiuv020.reader.ItemMargins

/**
 * Created by AoEiuV020 on 2018.05.26-20:36:04.
 */

class Margins(subName: String,
              private val defaultEnabled: Boolean,
              private val defaultLeft: Int,
              private val defaultTop: Int,
              private val defaultRight: Int,
              private val defaultBottom: Int
) : SubPref(ReaderSettings, subName), ItemMargins {
    // 保存在App.context.packageName + "_ReaderSettings" + "_$name"
    /**
     * 对应的东西是否显示，
     * 除了小说内容，其他都支持不显示，
     */
    override var enabled: Boolean by Delegates.boolean(defaultEnabled)
    override var left: Int by Delegates.int(defaultLeft)
    override var top: Int by Delegates.int(defaultTop)
    override var right: Int by Delegates.int(defaultRight)
    override var bottom: Int by Delegates.int(defaultBottom)

    /**
     * 恢复成构造时传入的默认值，供设置弹窗“恢复默认”使用，
     */
    fun resetToDefault() {
        enabled = defaultEnabled
        left = defaultLeft
        top = defaultTop
        right = defaultRight
        bottom = defaultBottom
    }

    override fun toString(): String {
        return "Margins($left, $top, $right, $bottom)"
    }
}

