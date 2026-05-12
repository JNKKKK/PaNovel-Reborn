package cc.aoeiuv020.panovel.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceFragmentCompat
import timber.log.Timber
import java.io.File
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * SharedPreferences相关Delegate的封装，
 *
 * Created by AoEiuV020 on 2018.05.17-14:58:10.
 */

/**
 * 指定包含一个SharedPreferences，用于Delegate，
 */
interface Pref {
    val name: String
    val context: Context
        get() = PrefContext.appContext
    val sharedPreferencesName: String
        get() = PrefContext.appContext.packageName + "_$name"
    val sharedPreferences: SharedPreferences
        get() = PrefContext.appContext.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
}

abstract class SubPref(
        pref: Pref,
        subName: String
) : Pref {
    override val name: String = pref.name + "_$subName"
}

/**
 * 给这个设置页面绑定这个Pref,
 * 但是默认值没法处理，两边都要写，
 */
fun PreferenceFragmentCompat.attach(pref: Pref) {
    preferenceManager.sharedPreferencesName = pref.sharedPreferencesName
}

/**
 * 所有Delegate从这里获取，
 */
@Suppress("unused")
object Delegates {
    fun string(default: kotlin.String, key: kotlin.String? = null) =
            PrefDelegate.String(default, key)

    fun int(default: Int, key: String? = null) =
            PrefDelegate.Int(default, key)

    // 尽量用int，没什么必要long,
    fun long(default: Long, key: String? = null) =
            PrefDelegate.Long(default, key)

    fun float(default: Float, key: String? = null) =
            PrefDelegate.Float(default, key)

    fun boolean(default: Boolean, key: String? = null) =
            PrefDelegate.Boolean(default, key)

    /**
     * enum枚举保存字符串，读取时用gson解析，
     * 不带引号的字符串也能用gson解析，
     */
    inline fun <reified T : Enum<*>> enum(default: T, key: kotlin.String? = null) =
            PrefDelegate.Enum.new(default, key)

    inline fun <reified T : kotlin.Any> any(default: T, key: kotlin.String? = null) =
            PrefDelegate.Any.new(default, key)

    fun uri(key: String? = null) = UriDelegate(key)
}

/**
 * 文件相关的用这个，
 * Uri可以从文件得到，也可以打开写入文件，
 * ${context.cacheDir}/UriDelegate/${pref.name}/${key ?: property.name}
 */
class UriDelegate(
        private val key: kotlin.String? = null
) : ReadWriteProperty<Pref, android.net.Uri?> {
    companion object {
        private const val KEY_URI_DELEGATE = "UriDelegate"
    }

    private fun getFile(thisRef: Pref, property: KProperty<*>): File {
        return PrefContext.appContext.filesDir.resolve(KEY_URI_DELEGATE)
                .resolve(thisRef.name)
                .apply { mkdirs() }
                .resolve(key ?: property.name)
    }

    private var backField: Uri? = null
    override fun getValue(thisRef: Pref, property: KProperty<*>): android.net.Uri? {
        if (backField != null) {
            return backField
        }
        val file = getFile(thisRef, property)
        if (!file.exists()) {
            return null
        }
        backField = Uri.fromFile(file)
        return backField
    }

    override fun setValue(thisRef: Pref, property: KProperty<*>, value: android.net.Uri?) {
        if (getValue(thisRef, property) == value) {
            return
        }
        // 先赋值为空，之后通过getValue拿uri, 因为有个判断，这个不为空就拿不到文件，
        backField = null
        val file = getFile(thisRef, property)
        if (value == null) {
            if (!file.delete()) {
                throw Exception("delete failed,")
            }
        } else {
            file.outputStream().use { output ->
                PrefContext.appContext.contentResolver.openInputStream(value)!!.use { input ->
                    input.copyTo(output)
                }
                output.flush()
            }

            backField = getValue(thisRef, property)
        }
    }
}

/**
 * 一个Delegate只用在一个字段，
 * 只用在原始类型，
 * 不要用在自定义的Serializable类，
 * 非空，
 *
 * @param key 如果key为空，直接用成员变量名，不受混淆影响，
 */
sealed class PrefDelegate<T>(
        private val key: kotlin.String?
) : ReadWriteProperty<Pref, T> {
    final override fun getValue(thisRef: Pref, property: KProperty<*>): T {
        val realKey = key ?: property.name
        return getValue(thisRef.sharedPreferences, realKey).also {
            Timber.d("${property.name} > $it")
        }
    }

    final override fun setValue(thisRef: Pref, property: KProperty<*>, value: T) {
        val realKey = key ?: property.name
        Timber.d("$realKey < $value")
        thisRef.sharedPreferences
                .edit()
                .also { setValue(it, realKey, value) }
                .apply()
    }

    abstract fun getValue(sp: SharedPreferences, key: kotlin.String): T
    abstract fun setValue(editor: SharedPreferences.Editor, key: kotlin.String, value: T)

    class String(
            private val default: kotlin.String,
            key: kotlin.String?
    ) : PrefDelegate<kotlin.String>(key) {
        override fun getValue(sp: SharedPreferences, key: kotlin.String): kotlin.String {
            return sp.getString(key, default)!!
        }

        override fun setValue(editor: SharedPreferences.Editor, key: kotlin.String, value: kotlin.String) {
            editor.putString(key, value)
        }
    }

    class Int(
            private val default: kotlin.Int,
            key: kotlin.String? = null
    ) : PrefDelegate<kotlin.Int>(key) {
        override fun getValue(sp: SharedPreferences, key: kotlin.String): kotlin.Int {
            return sp.getInt(key, default)
        }

        override fun setValue(editor: SharedPreferences.Editor, key: kotlin.String, value: kotlin.Int) {
            editor.putInt(key, value)
        }
    }

    class Long(
            private val default: kotlin.Long,
            key: kotlin.String? = null
    ) : PrefDelegate<kotlin.Long>(key) {
        override fun getValue(sp: SharedPreferences, key: kotlin.String): kotlin.Long {
            return sp.getLong(key, default)
        }

        override fun setValue(editor: SharedPreferences.Editor, key: kotlin.String, value: kotlin.Long) {
            editor.putLong(key, value)
        }
    }

    class Float(
            private val default: kotlin.Float,
            key: kotlin.String? = null
    ) : PrefDelegate<kotlin.Float>(key) {
        override fun getValue(sp: SharedPreferences, key: kotlin.String): kotlin.Float {
            return sp.getFloat(key, default)
        }

        override fun setValue(editor: SharedPreferences.Editor, key: kotlin.String, value: kotlin.Float) {
            editor.putFloat(key, value)
        }
    }

    class Boolean(
            private val default: kotlin.Boolean,
            key: kotlin.String? = null
    ) : PrefDelegate<kotlin.Boolean>(key) {
        override fun getValue(sp: SharedPreferences, key: kotlin.String): kotlin.Boolean {
            return sp.getBoolean(key, default)
        }

        override fun setValue(editor: SharedPreferences.Editor, key: kotlin.String, value: kotlin.Boolean) {
            editor.putBoolean(key, value)
        }
    }

    class Enum<T : kotlin.Enum<*>>(
            private val default: T,
            key: kotlin.String? = null,
            private val type: Class<T>
    ) : PrefDelegate<T>(key) {
        companion object {
            inline fun <reified T : kotlin.Enum<*>> new(default: T, key: kotlin.String? = null) = Enum(default, key, T::class.java)
        }

        override fun getValue(sp: SharedPreferences, key: kotlin.String): T {
            val stored = sp.getString(key, null) ?: return default
            return type.enumConstants?.firstOrNull { it.name == stored } ?: default
        }

        override fun setValue(editor: SharedPreferences.Editor, key: kotlin.String, value: T) {
            editor.putString(key, value.name)
        }
    }

    class Any<T : kotlin.Any>(
            private val default: T,
            key: kotlin.String? = null,
            private val serializer: kotlinx.serialization.KSerializer<T>
    ) : PrefDelegate<T>(key) {
        companion object {
            inline fun <reified T : kotlin.Any> new(default: T, key: kotlin.String? = null) =
                Any(default, key, kotlinx.serialization.serializer<T>())
        }

        override fun getValue(sp: SharedPreferences, key: kotlin.String): T {
            val stored = sp.getString(key, null) ?: return default
            return try {
                cc.aoeiuv020.json.AppJson.decodeFromString(serializer, stored)
            } catch (_: Exception) {
                default
            }
        }

        override fun setValue(editor: SharedPreferences.Editor, key: kotlin.String, value: T) {
            editor.putString(key, cc.aoeiuv020.json.AppJson.encodeToString(serializer, value))
        }
    }
}

