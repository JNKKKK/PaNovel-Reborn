package cc.aoeiuv020.panovel.share

import cc.aoeiuv020.panovel.data.entity.NovelMinimal
import kotlinx.serialization.Serializable

@Serializable
class SharedBookList(
        val name: String,
        val list: List<NovelMinimal>,
        val version: Int,
        val uuid: String
)
