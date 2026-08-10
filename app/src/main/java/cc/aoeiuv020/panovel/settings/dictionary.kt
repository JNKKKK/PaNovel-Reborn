package cc.aoeiuv020.panovel.settings

import cc.aoeiuv020.panovel.data.BuiltinDictionary
import cc.aoeiuv020.panovel.util.Delegates
import cc.aoeiuv020.panovel.util.Pref

/**
 * 词典相关设置。目前只有「选择词典」一项：记录当前使用的内置词典。
 *
 * 存的是 [BuiltinDictionary] 的枚举名（字符串），读时映射回枚举、无法识别时回退默认，
 * 这样将来增删内置词典都不会因旧值失效而崩溃。[cc.aoeiuv020.panovel.data.DictionaryManager]
 * 读这里决定打开哪部词典。
 */
object DictionarySettings : Pref {
    override val name: String
        get() = "Dictionary"

    /** 当前选择的内置词典枚举名；默认见 [BuiltinDictionary.DEFAULT]。 */
    private var selectedName: String by Delegates.string(BuiltinDictionary.DEFAULT.name)

    var selected: BuiltinDictionary
        get() = BuiltinDictionary.ALL.firstOrNull { it.name == selectedName } ?: BuiltinDictionary.DEFAULT
        set(value) {
            selectedName = value.name
        }
}
