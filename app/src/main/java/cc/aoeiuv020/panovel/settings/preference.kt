@file:Suppress("unused")

package cc.aoeiuv020.panovel.settings

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
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

class FloatEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : EditTextPreference(context, attrs) {

    override fun persistString(value: String?): Boolean {
        val floatVal = try {
            value?.toFloat() ?: return false
        } catch (e: NumberFormatException) {
            return false
        }
        return persistFloat(floatVal)
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        val defaultFloat = defaultReturnValue?.takeIf(String::isNotEmpty)?.toFloatOrNull() ?: 0f
        return getPersistedFloat(defaultFloat).toString()
    }
}

class ColorPickerPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : androidx.preference.Preference(context, attrs) {
    private var currentColor: Int = 0

    init {
        widgetLayoutResource = android.R.layout.simple_list_item_1
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        val defaultString = a.getString(index) ?: return null
        return defaultString.lowercase().let {
            if (it.startsWith("0x")) {
                it.removePrefix("0x").toLong(radix = 16)
            } else {
                it.toLong()
            }
        }.toInt()
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        currentColor = getPersistedInt((defaultValue as? Int) ?: 0)
    }

    fun setValue(color: Int) {
        currentColor = color
        persistInt(color)
        notifyChanged()
    }

    override fun onClick() {
        com.flask.colorpicker.builder.ColorPickerDialogBuilder.with(context)
            .initialColor(currentColor)
            .wheelType(com.flask.colorpicker.ColorPickerView.WHEEL_TYPE.CIRCLE)
            .setPositiveButton(android.R.string.ok) { _, color, _ ->
                setValue(color)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .build()
            .show()
    }
}
