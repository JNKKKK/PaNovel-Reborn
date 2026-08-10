package cc.aoeiuv020.panovel.settings

import android.os.Bundle
import androidx.preference.ListPreference
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.BuiltinDictionary

/**
 * 词典设置页：目前只有「选择词典」一项，用一个 [ListPreference] 展示，
 * 副标题（summary `%s`）即当前所选词典名，点击弹出单选框在内置词典间切换。
 *
 * 选项从 [BuiltinDictionary] 动态填充（枚举是唯一事实来源），存的是枚举名，
 * 与 [DictionarySettings] 用同一 SharedPreferences、同一 key（`selectedName`），
 * 切换后 [cc.aoeiuv020.panovel.data.DictionaryManager] 下次取词即读到新选择。
 * 弹窗样式与其它设置一致，由 [BasePreferenceFragment] 统一处理。
 */
class DictionaryPreferenceFragment : BasePreferenceFragment(DictionarySettings, R.xml.pref_dictionary) {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        findPreference<ListPreference>("selectedName")?.apply {
            val all = BuiltinDictionary.ALL
            entries = all.map { it.displayName }.toTypedArray()
            entryValues = all.map { it.name }.toTypedArray()
            // 确保首次进入就显示当前选择（存储里没值时用默认词典），
            value = DictionarySettings.selected.name
        }
    }
}
