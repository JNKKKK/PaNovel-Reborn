package cc.aoeiuv020.irondb.impl

import cc.aoeiuv020.irondb.DataSerializer
import cc.aoeiuv020.irondb.Database
import cc.aoeiuv020.irondb.FileWrapper
import cc.aoeiuv020.irondb.KeySerializer
import kotlinx.serialization.KSerializer
import java.io.File
import java.io.IOException

internal class DatabaseImpl(
    private val base: File,
    private val subSerializer: KeySerializer,
    private val keySerializer: KeySerializer,
    private val dataSerializer: DataSerializer
) : Database {
    private val keyLocker = KeyLocker()

    init {
        base.exists() || base.mkdirs() || throw IOException("failed mkdirs ${base.path}")
        base.canWrite() || throw IOException("failed write ${base.path}")
    }

    override fun sub(table: String) = DatabaseImpl(
        base = base.resolve(table).canonicalFile,
        subSerializer = subSerializer,
        keySerializer = keySerializer,
        dataSerializer = dataSerializer
    )

    override fun <T> write(key: String, value: T?, serializer: KSerializer<T>) {
        val serializedKey = keySerializer.serialize(key)
        base.run { exists() || mkdirs() }
        val file = base.resolve(serializedKey)
        keyLocker.runInAcquire(serializedKey) {
            if (value == null) {
                file.delete()
            } else {
                val data = dataSerializer.serialize(value, serializer)
                file.writeText(data)
            }
        }
    }

    override fun <T> read(key: String, serializer: KSerializer<T>): T? {
        val serializedKey = keySerializer.serialize(key)
        val file = base.resolve(serializedKey)
        return keyLocker.runInAcquire(serializedKey) {
            if (!file.exists()) {
                null
            } else {
                val string = file.readText()
                dataSerializer.deserialize(string, serializer)
            }
        }
    }

    override fun file(key: String): FileWrapper {
        val serializedKey = keySerializer.serialize(key)
        return FileWrapperImpl(serializedKey)
    }

    inner class FileWrapperImpl(
        private val serializedKey: String
    ) : FileWrapper {
        val file = base.resolve(serializedKey)
        override fun <T> use(block: (File) -> T): T {
            base.run { exists() || mkdirs() }
            return keyLocker.runInAcquire(serializedKey) {
                block(file)
            }
        }

        override fun delete(): Boolean {
            return keyLocker.runInAcquire(serializedKey) {
                file.delete()
            }
        }
    }

    override fun drop() {
        base.deleteRecursively()
    }

    override fun keysContainer(): Collection<String> = KeysContainer(base, keySerializer)
}
