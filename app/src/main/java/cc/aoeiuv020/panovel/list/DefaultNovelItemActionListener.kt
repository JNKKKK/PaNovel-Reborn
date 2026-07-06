package cc.aoeiuv020.panovel.list

import cc.aoeiuv020.shared.util.interrupt
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.detail.NovelDetailActivity
import cc.aoeiuv020.panovel.localbook.CreateDocumentActivity
import cc.aoeiuv020.panovel.bookfile.LocalNovelType
import cc.aoeiuv020.panovel.localbook.NovelExporter
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.search.FuzzySearchActivity
import cc.aoeiuv020.panovel.settings.ItemAction
import cc.aoeiuv020.panovel.settings.ItemAction.*
import cc.aoeiuv020.panovel.text.NovelTextActivity
import cc.aoeiuv020.panovel.util.uiInput
import cc.aoeiuv020.panovel.util.uiSelect
import java.nio.charset.UnsupportedCharsetException
import java.util.concurrent.TimeUnit
import timber.log.Timber
import kotlinx.coroutines.*
import androidx.appcompat.app.AlertDialog

class DefaultNovelItemActionListener(
        private val actionDoneListener: (ItemAction, NovelViewHolder) -> Unit = { _, _ -> },
        // 历史列表不参与置顶，隐藏置顶/取消置顶操作，
        private val supportPin: Boolean = true,
        private val onError: (String, Throwable) -> Unit
) : NovelItemActionListener {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun on(action: ItemAction, vh: NovelViewHolder): Boolean {
        Timber.d("doing $action at ${vh.novel.name}")
        when (action) {
            ReadLastChapter -> NovelTextActivity.start(vh.context, vh.novel, -1)
            ReadContinue -> NovelTextActivity.start(vh.context, vh.novel)
            OpenDetail -> NovelDetailActivity.start(vh.context, vh.novel)
            RefineSearch -> FuzzySearchActivity.start(vh.context, vh.novel)
            Export -> exportNovel(vh)
            AddBookshelf -> setBookshelf(vh, true)
            RemoveBookshelf -> setBookshelf(vh, false)
            Refresh -> vh.refresh()
            Cache -> download(vh)
            Pinned -> pinned(vh)
            CancelPinned -> cancelPinned(vh)
            CleanCache -> cleanCache(vh)
            CleanData -> cleanData(vh)
            MoreAction -> {
                val list = listOfNotNull(
                        R.string.refresh to Refresh,

                        R.string.refine_search to RefineSearch,
                        R.string.export to Export,
                        R.string.cache to Cache,
                        // 置顶/取消置顶互斥，只显示当前状态对应的操作，历史列表不显示，
                        if (!supportPin) {
                            null
                        } else if (vh.novel.pinnedTime.time > TimeUnit.DAYS.toMillis(1)) {
                            R.string.cancel_pinned to CancelPinned
                        } else {
                            R.string.pinned to Pinned
                        },

                        if (vh.novel.bookshelf) {
                            R.string.remove_bookshelf to RemoveBookshelf
                        } else {
                            R.string.add_bookshelf to AddBookshelf
                        },
                        R.string.clean_this_novel to CleanData
                )
                val items = list.unzip().first.map { vh.context.getString(it) }.toTypedArray()
                AlertDialog.Builder(vh.context)
                    .setTitle(vh.context.getString(R.string.title_more_action))
                    .setItems(items) { _, i ->
                        on(list[i].second, vh)
                    }
                    .show()

            }
            // 返回false不消费长按事件，
            None -> return false
        }
        // 置顶/取消置顶是异步持久化，等写库完成后自行回调，避免列表在写库前就按旧数据刷新，
        if (action != Pinned && action != CancelPinned) {
            actionDoneListener(action, vh)
        }
        return true
    }

    override fun onDotClick(vh: NovelViewHolder) {
        on(Refresh, vh)
    }

    override fun onCheckUpdateClick(vh: NovelViewHolder) {
        on(Refresh, vh)
    }

    override fun onNameClick(vh: NovelViewHolder) {
        on(OpenDetail, vh)
    }

    override fun onLastChapterClick(vh: NovelViewHolder) {
        on(ReadLastChapter, vh)
    }

    override fun onItemClick(vh: NovelViewHolder) {
        on(ReadContinue, vh)
    }

    override fun onItemLongClick(vh: NovelViewHolder): Boolean {
        return on(MoreAction, vh)
    }

    override fun onStarChanged(vh: NovelViewHolder, star: Boolean) {
        // 爱心控件已经在点击时切换了选中状态，这里只持久化，不再回设界面，
        persistBookshelf(vh, star)
    }

    /**
     * 菜单触发的加入/移出书架，与爱心控件不同，需要主动同步界面选中状态，
     */
    private fun setBookshelf(vh: NovelViewHolder, star: Boolean) {
        vh.setStarChecked(star)
        persistBookshelf(vh, star)
    }

    private fun persistBookshelf(vh: NovelViewHolder, star: Boolean) {
        val novelManager = vh.novelManager
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    novelManager.updateBookshelf(star)
                }
            } catch (e: Exception) {
                val message = "${if (star) "添加" else "删除"}书架《${vh.novel.name}》失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                onError(message, e)
            }
        }
    }

    override fun refreshChapters(vh: NovelViewHolder) {
        val novelManager = vh.novelManager
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    novelManager.requestChapters(true)
                }
                vh.refreshed(novelManager)
            } catch (e: Exception) {
                val message = "刷新小说《${vh.novel.name}》失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                // 失败也停止显示正在刷新，
                vh.refreshed(novelManager)
                onError(message, e)
            }
        }
    }

    private fun download(vh: NovelViewHolder) {
        DataManager.download.askDownload(vh.context, vh.novelManager, vh.novel.readAtChapterIndex, true)
    }

    private fun pinned(vh: NovelViewHolder) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vh.novelManager.pinned()
                }
                // 写库完成后再刷新列表，此时内存中的pinnedTime已更新，
                actionDoneListener(Pinned, vh)
            } catch (e: Exception) {
                val message = "置顶小说《${vh.novel.name}》失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                onError(message, e)
            }
        }
    }

    private fun cancelPinned(vh: NovelViewHolder) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vh.novelManager.cancelPinned()
                }
                actionDoneListener(CancelPinned, vh)
            } catch (e: Exception) {
                val message = "取消置顶小说《${vh.novel.name}》失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                onError(message, e)
            }
        }
    }

    private fun cleanCache(vh: NovelViewHolder) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vh.novelManager.cleanCache()
                }
            } catch (e: Exception) {
                val message = "清除小说缓存<${vh.novel.bookId}>失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                onError(message, e)
            }
        }
    }

    private fun cleanData(vh: NovelViewHolder) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vh.novelManager.cleanData()
                }
            } catch (e: Exception) {
                val message = "清除小说数据<${vh.novel.bookId}>失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                onError(message, e)
            }
        }
    }

    private fun exportNovel(vh: NovelViewHolder) {
        scope.launch {
            try {
                val context = vh.context
                val (type, charset) = withContext(Dispatchers.IO) {
                    val types = LocalNovelType.values()
                    val items = types.map { type ->
                        when (type) {
                            LocalNovelType.TEXT -> R.string.select_item_text
                            LocalNovelType.EPUB -> R.string.select_item_epub
                        }.let { context.getString(it) }
                    }.toTypedArray()
                    // 默认导出txt,
                    val defaultIndex = 0
                    val type = context.uiSelect(context.getString(R.string.file_type), items, defaultIndex)?.let { selectIndex ->
                        types[selectIndex]
                    } ?: return@withContext null
                    val charset = if (type == LocalNovelType.TEXT) {
                        context.uiInput(context.getString(R.string.file_charset), Charsets.UTF_8.name())?.let {
                            try {
                                charset(it)
                            } catch (e: UnsupportedCharsetException) {
                                interrupt(context.getString(R.string.tip_not_support_charset, it))
                            }
                        } ?: return@withContext null
                    } else {
                        Charsets.UTF_8
                    }
                    type to charset
                } ?: return@launch

                // 弹出系统"另存为"，让用户每次自行选择保存位置，文件名按现有规则预填，
                val suggestedName = NovelExporter.fileName(vh.novel, type)
                val target = CreateDocumentActivity.createDocument(context, type.mime, suggestedName)
                        ?: return@launch
                withContext(Dispatchers.IO) {
                    NovelExporter.export(context, type, charset, vh.novelManager, target)
                }
            } catch (e: Exception) {
                val message = "导出小说<${vh.novel.bookId}>失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                onError(message, e)
            }
        }
    }

}
