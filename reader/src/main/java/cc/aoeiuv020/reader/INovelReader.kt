package cc.aoeiuv020.reader

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

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

    /**
     * 刷新当前章节正文，
     * @param onComplete 刷新结束回调，参数为是否成功，失败时保留原有缓存内容，被取消时不回调，
     * @return 刷新任务的Job, 可用于取消刷新（比如网络超时太久用户想取消），没有可刷新的章节时返回null,
     */
    fun refreshCurrentChapter(onComplete: (success: Boolean) -> Unit = {}): Job?

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

