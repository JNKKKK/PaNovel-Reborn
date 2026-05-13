package cc.aoeiuv020.panovel.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cc.aoeiuv020.panovel.data.dao.BookListDao
import cc.aoeiuv020.panovel.data.dao.NovelDao
import cc.aoeiuv020.panovel.data.dao.SiteDao
import cc.aoeiuv020.panovel.data.entity.BookList
import cc.aoeiuv020.panovel.data.entity.BookListItem
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.data.entity.Site

@Database(
        entities = [Novel::class, Site::class, BookListItem::class, BookList::class],
        version = 1
)
@TypeConverters(value = [DateTypeConverter::class])
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private var sInstance: AppDatabase? = null
        @Synchronized
        fun getInstance(context: Context): AppDatabase {
            val dbFile = context.getDatabasePath("PaNovel-app.db")
            return sInstance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                dbFile.path
            )
                .fallbackToDestructiveMigration()
                .build().also {
                    sInstance = it
                }
        }
    }

    abstract fun siteDao(): SiteDao
    abstract fun novelDao(): NovelDao
    abstract fun bookListDao(): BookListDao
}