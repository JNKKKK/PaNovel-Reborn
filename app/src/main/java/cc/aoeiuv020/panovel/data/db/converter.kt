package cc.aoeiuv020.panovel.data.db

import androidx.room.TypeConverter
import java.util.*


class DateTypeConverter {

    @TypeConverter
    fun toDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun toLong(value: Date?): Long? {
        return value?.time
    }
}