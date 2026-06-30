package cc.aoeiuv020.panovel.backup

import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.data.entity.Novel
import kotlinx.serialization.Serializable

/**
 * 备份中的一本小说，
 * 除了小说本身的元信息和阅读进度，还带上已缓存的章节列表和正文，
 * 让导入后无需联网即可继续阅读已缓存内容，
 *
 * 时间统一用毫秒时间戳，避免依赖额外的序列化器，
 */
@Serializable
data class BackupNovel(
    val site: String,
    val author: String,
    val name: String,
    val detail: String,

    // 阅读进度，
    val readAtChapterIndex: Int = 0,
    val readAtTextIndex: Int = 0,
    val readAtChapterName: String = Novel.VALUE_NULL,
    val readTime: Long = 0,

    // 是否在书架，以及书架排序用的置顶时间，
    // 只有备份了书架时才为true, 避免只备份历史时把书又塞回书架，
    val bookshelf: Boolean = false,
    val pinnedTime: Long = 0,

    // 详情页信息，导入后无需再次联网获取详情，
    val image: String = Novel.VALUE_NULL,
    val introduction: String = Novel.VALUE_NULL,
    val updateTime: Long = 0,
    val chaptersCount: Int = 0,
    val lastChapterName: String = Novel.VALUE_NULL,
    /**
     * 请求章节列表用的extra, 也就是[Novel.chapters],
     * 同时是章节缓存的key, 为空表示从没获取过详情，此时章节列表和正文都为空，
     */
    val chaptersExtra: String? = null,

    // 已缓存的章节列表，可能为空，
    val chapters: List<NovelChapter> = emptyList(),
    // 已缓存的正文，按章节extra索引，只包含确实缓存了的章节，
    val contents: Map<String, List<String>> = emptyMap(),
)
