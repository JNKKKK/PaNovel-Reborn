package cc.aoeiuv020.panovel.share

import cc.aoeiuv020.panovel.data.entity.NovelMinimal
import kotlinx.serialization.Serializable

@Serializable
class BookListV3(
        val name: String,
        val list: List<NovelMinimal>,
        val version: Int,
        val uuid: String
)

@Serializable
class BookListV2(
        val name: String,
        val list: List<NovelMinimal>,
        val version: Int
)

@Serializable
class BookListV1(
        val name: String,
        val list: List<LegacyNovel>
)

@Serializable
class LegacyNovel(
        val name: String,
        val author: String,
        val site: String,
        val requester: LegacyRequester
)

@Serializable
class LegacyRequester(
        val type: String,
        val extra: String
)
