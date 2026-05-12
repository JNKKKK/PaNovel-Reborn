package cc.aoeiuv020.panovel.open

import android.content.Context
import android.net.Uri
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.local.ImportRequireValue
import cc.aoeiuv020.panovel.local.LocalNovelType
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.share.Share
import cc.aoeiuv020.panovel.util.uiInput
import cc.aoeiuv020.panovel.util.uiSelect
import timber.log.Timber
import kotlinx.coroutines.*

/**
 * 应用内打开相关的，
 * Created by AoEiuV020 on 2018.03.07-21:40:28.
 */
object OpenManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun open(context: Context, str: String, openListener: OpenListener) {
        // Uri.parse没有检测，不会抛异常，不正常的uri会在getScheme返回null,
        open(context, Uri.parse(str), openListener)
    }

    fun open(context: Context, uri: Uri, openListener: OpenListener) {
        switch(context, uri, openListener)
    }

    private fun switch(context: Context, uri: Uri, openListener: OpenListener) {
        openListener.onLoading(context.getString(R.string.judging))
        when {
            uri.scheme == null -> {
                openListener.onOtherCase(uri.toString())
            }
            !uri.scheme!!.startsWith("http") -> {
                // 协议不是http或https的话统统当成本地小说打开，
                openListener.onLoading(context.getString(R.string.local_novel_importing))
                scope.launch {
                    try {
                        val novel = withContext(Dispatchers.IO) {
                            DataManager.importLocalNovel(context, uri) { value, default ->
                                if (value == ImportRequireValue.TYPE) {
                                    val types = LocalNovelType.values()
                                    val items = types.map {
                                        when (it) {
                                            LocalNovelType.TEXT -> R.string.select_item_text
                                            LocalNovelType.EPUB -> R.string.select_item_epub
                                            else -> R.string.select_item_text
                                        }.let { resId -> context.getString(resId) }
                                    }.toTypedArray()
                                    val defaultIndex = types.indexOfFirst {
                                        it.suffix == default
                                    }
                                    context.uiSelect(context.getString(R.string.file_type), items, defaultIndex)?.let { selectIndex ->
                                        types[selectIndex].suffix
                                    }
                                } else {
                                    val name = when (value) {
                                        ImportRequireValue.TYPE -> R.string.file_type
                                        ImportRequireValue.CHARSET -> R.string.file_charset
                                        ImportRequireValue.AUTHOR -> R.string.author
                                        ImportRequireValue.NAME -> R.string.name
                                    }.let { resId -> context.getString(resId) }
                                    context.uiInput(name, default)
                                }
                            }
                        }
                        openListener.onLocalNovelImported(novel)
                    } catch (e: Exception) {
                        val message = "导入本地小说失败，"
                        Reporter.post(message, e)
                        Timber.e(e, message)
                        openListener.onError(message, e)
                    }
                }
            }
            Share.check(uri.toString()) -> {
                // 如果是书单分享的地址，直接添加书单，
                openListener.onLoading(context.getString(R.string.book_list_downloading))
                scope.launch {
                    try {
                        val count = withContext(Dispatchers.IO) {
                            Share.receiveBookList(uri.toString())
                        }
                        openListener.onBookListReceived(count)
                    } catch (e: Exception) {
                        val message = "获取书单失败，"
                        Reporter.post(message, e)
                        Timber.e(e, message)
                        openListener.onError(message, e)
                    }
                }
            }
            else -> // 如果可以从地址得到小说对象，打开详情页，
                scope.launch {
                    try {
                        val novel = withContext(Dispatchers.IO) {
                            DataManager.getNovelFromUrl(uri.toString())
                        }
                        openListener.onNovelOpened(novel)
                    } catch (e: Exception) {
                        val message = "不支持的地址或格式，"
                        Reporter.post(message, e)
                        Timber.e(e, message)
                        openListener.onError(message, e)
                    }
                }
        }
    }

    interface OpenListener {
        fun onLoading(status: String)
        fun onOtherCase(str: String)
        fun onBookListReceived(count: Int)
        fun onError(message: String, e: Throwable)
        fun onNovelOpened(novel: Novel)
        fun onLocalNovelImported(novel: Novel)
    }
}
