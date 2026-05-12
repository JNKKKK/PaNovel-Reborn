package cc.aoeiuv020.panovel.refresher

import kotlinx.serialization.Serializable

@Serializable
class SharedBookList(
        val name: String,
        val list: List<NovelMinimal>,
        val version: Int,
        val uuid: String
)

@Serializable
data class NovelMinimal(
        var site: String,
        var author: String,
        var name: String,
        var detail: String
)
