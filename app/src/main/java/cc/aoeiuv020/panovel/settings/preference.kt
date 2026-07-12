@file:Suppress("unused")

package cc.aoeiuv020.panovel.settings

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.SeekBarPreference
import cc.aoeiuv020.panovel.R
import kotlin.math.roundToInt
import androidx.preference.EditTextPreference as AndroidXEditTextPreference

open class EditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AndroidXEditTextPreference(context, attrs) {
    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return super.onGetDefaultValue(a, index)?.also {
            text = it.toString()
        }
    }

    override fun getSummary(): CharSequence? {
        return text ?: super.getSummary()
    }
}

class IntEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : EditTextPreference(context, attrs) {

    override fun persistString(value: String?): Boolean {
        val intVal = try {
            value?.toInt() ?: return false
        } catch (e: NumberFormatException) {
            return false
        }
        return persistInt(intVal)
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        val defaultInt = defaultReturnValue?.takeIf(String::isNotEmpty)?.toIntOrNull() ?: 0
        return getPersistedInt(defaultInt).toString()
    }
}

/**
 * 滑块设置项，界面上按 0-100 的百分比操作，
 * 但底层仍按 0.0-1.0 的 Float 持久化，和 ReaderSettings.centerPercent 保持兼容，
 * summary 显示当前百分比，如 “50%”，两端有特殊说明文案，
 */
class PercentSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SeekBarPreference(context, attrs) {
    init {
        // 百分比固定 0-100，
        min = 0
        max = 100
        showSeekBarValue = false
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        // xml 里 defaultValue 写 0.0-1.0 的小数，转成 0-100，
        return (a.getFloat(index, 0.5f) * 100).roundToInt()
    }

    override fun persistInt(value: Int): Boolean = persistFloat(value / 100f).also {
        // 框架变更值时只走 persistInt 不会 notifyChanged，这里主动刷新 summary，
        notifyChanged()
    }

    override fun getPersistedInt(defaultReturnValue: Int): Int =
        (getPersistedFloat(defaultReturnValue / 100f) * 100).roundToInt()

    // summary 跟随当前滑块值，两端给出行为说明，中间显示百分比，
    override fun getSummary(): CharSequence = when (value) {
        0 -> context.getString(R.string.center_percent_summary_none)
        100 -> context.getString(R.string.center_percent_summary_full)
        else -> "$value%"
    }
}
