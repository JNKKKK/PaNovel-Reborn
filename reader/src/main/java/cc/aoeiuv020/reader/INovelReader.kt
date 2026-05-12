package cc.aoeiuv020.reader

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 *
 * Created by AoEiuV020 on 2017.12.01-02:13:39.
 */
interface INovelReader {
    val context: Context

    var novel: String

    var readingListener: ReadingListener?
    var menuListener: MenuListener?

    var requester: TextRequester
    var chapterList: List<String>

    var currentChapter: Int
    var textProgress: Int
    val maxTextProgress: Int

    val config: ReaderConfig

    fun refreshCurrentChapter()

    fun scrollNext(): Boolean
    fun scrollPrev(): Boolean

    fun destroy()
}

abstract class BaseNovelReader(override var novel: String, override var requester: TextRequester) : INovelReader {
    override var readingListener: ReadingListener? = null
    override var menuListener: MenuListener? = null
    override var chapterList: List<String> = emptyList()
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override fun scrollNext(): Boolean = false
    override fun scrollPrev(): Boolean = false
}

