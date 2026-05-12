package cc.aoeiuv020.irondb

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Database {
    fun sub(table: String): Database
    fun <T> write(key: String, value: T?, serializer: KSerializer<T>)
    fun <T> read(key: String, serializer: KSerializer<T>): T?
    fun file(key: String): FileWrapper
    fun drop()
    fun keysContainer(): Collection<String>
}

inline fun <reified T> Database.write(key: String, value: T) =
    write(key, value, serializer<T>())

inline fun <reified T> Database.read(key: String): T? =
    read(key, serializer<T>())

inline fun <reified T> Database.delegate(key: String? = null): ReadWriteProperty<Any, T?> =
    DatabaseProperty(this, key, serializer<T>())

class DatabaseProperty<T>(
    private val database: Database,
    private val key: String?,
    private val serializer: KSerializer<T>
) : ReadWriteProperty<Any, T?> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        val realKey = key ?: property.name
        return database.read(realKey, serializer)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        val realKey = key ?: property.name
        database.write(realKey, value, serializer)
    }
}
