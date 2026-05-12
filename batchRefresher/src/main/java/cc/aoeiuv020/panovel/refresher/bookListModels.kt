package cc.aoeiuv020.panovel.refresher

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

@Serializable
data class NovelMinimal(
        var site: String,
        var author: String,
        var name: String,
        var detail: String
)
