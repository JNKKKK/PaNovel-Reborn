package cc.aoeiuv020.panovel.data.entity

import cc.aoeiuv020.panovel.api.NovelItem
import kotlinx.serialization.Serializable

@Serializable
data class NovelMinimal(
        /**
         * 网站名，
         * 必须存在，不可空，一本小说至少要有["site", "author“， ”name", "detail"],
         * 不外键到网站表，那张表不稳定，
         */
        var site: String,
        /**
         * 作者名，
         * 必须存在，不可空，一本小说至少要有["site", "author“， ”name", "detail"],
         */
        var author: String,
        /**
         * 小说名，
         * 必须存在，不可空，一本小说至少要有["site", "author“， ”name", "detail"],
         */
        var name: String,
        /**
         * 用于请求小说详情页的额外信息，
         * 必须存在，不可空，一本小说至少要有["site", "author“， ”name", "detail"],
         * [cc.aoeiuv020.panovel.api.NovelItem.extra]
         */
        var detail: String
) {
    constructor(novelItem: NovelItem)
            : this(novelItem.site, novelItem.author, novelItem.name, novelItem.extra)

    constructor(novel: Novel)
            : this(novel.site, novel.author, novel.name, novel.detail)

    constructor(novel: NovelWithProgress)
            : this(novel.site, novel.author, novel.name, novel.detail)

    constructor(novel: NovelWithProgressAndPinnedTime)
            : this(novel.site, novel.author, novel.name, novel.detail)
}