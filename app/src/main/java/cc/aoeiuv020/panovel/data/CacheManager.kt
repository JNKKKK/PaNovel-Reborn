package cc.aoeiuv020.panovel.data

import android.content.Context
import cc.aoeiuv020.irondb.Database
import cc.aoeiuv020.irondb.Iron
import cc.aoeiuv020.irondb.read
import cc.aoeiuv020.irondb.write
import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.data.entity.Novel
import java.util.concurrent.ConcurrentHashMap

class CacheManager(context: Context) {
    // 用强引用缓存已解析的 Database：键是装箱的 nId，没有其他强引用，
    // 用 WeakHashMap 会立刻被 GC 回收导致缓存形同虚设，每次都要重建 sub() 链并做多次文件系统 syscall，
    private val contentDBMap = ConcurrentHashMap<Long, Database>()
    // 所有缓存固定保存在应用私有目录 /data/data/cc.aoeiuv020.panovel/cache/novel
    private val root: Database = Iron.db(context.cacheDir.resolve(NAME_FOLDER))

    private fun getContentDB(novel: Novel) = contentDBMap.getOrPut(novel.nId) {
        root.sub(novel.site).sub(novel.author).sub(novel.name).sub(KEY_CONTENT)
    }

    private val chaptersDBMap = ConcurrentHashMap<Long, Database>()
    private fun getChaptersDB(novel: Novel): Database = chaptersDBMap.getOrPut(novel.nId) {
        root.sub(novel.site).sub(novel.author).sub(novel.name).sub(KEY_CHAPTERS)
    }

    fun saveChapters(novel: Novel, list: List<NovelChapter>) {
        getChaptersDB(novel).write(novel.nChapters, list)
    }

    fun loadChapters(novel: Novel): List<NovelChapter>? {
        return getChaptersDB(novel).read(novel.nChapters)
    }

    fun saveContent(novel: Novel, extra: String, text: List<String>) {
        getContentDB(novel).write(extra, text)
    }

    fun loadContent(novel: Novel, extra: String): List<String>? {
        return getContentDB(novel).read(extra)
    }


    fun novelContentCached(novel: Novel): Collection<String> {
        return getContentDB(novel).keysContainer()
    }

    fun cleanAll() {
        root.drop()
    }

    fun clean(novel: Novel) {
        getChaptersDB(novel).drop()
        getContentDB(novel).drop()
    }

    companion object {
        const val NAME_FOLDER = "novel"
        const val KEY_CHAPTERS = "chapters"
        const val KEY_CONTENT = "content"
    }
}
