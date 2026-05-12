package cc.aoeiuv020.irondb

import cc.aoeiuv020.irondb.impl.DatabaseImpl
import cc.aoeiuv020.irondb.impl.KotlinxSerializer
import cc.aoeiuv020.irondb.impl.ReplaceFileSeparator
import java.io.File

object Iron {
    fun db(
        base: File,
        dataSerializer: DataSerializer = KotlinxSerializer(),
        keySerializer: KeySerializer = ReplaceFileSeparator(),
        subSerializer: KeySerializer = keySerializer
    ): Database = DatabaseImpl(
        base = base,
        subSerializer = subSerializer,
        keySerializer = keySerializer,
        dataSerializer = dataSerializer
    )
}
