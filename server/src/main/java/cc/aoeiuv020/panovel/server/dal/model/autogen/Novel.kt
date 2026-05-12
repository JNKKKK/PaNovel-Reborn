package cc.aoeiuv020.panovel.server.dal.model.autogen

import cc.aoeiuv020.json.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Novel(
    var id: Int? = null,
    var site: String? = null,
    var author: String? = null,
    var name: String? = null,
    var detail: String? = null,
    var chaptersCount: Int? = null,
    @Serializable(with = DateSerializer::class)
    var receiveUpdateTime: Date? = null,
    @Serializable(with = DateSerializer::class)
    var checkUpdateTime: Date? = null
) {
    val bookId: String get() = "$site.$author.$name"
}
