package cc.aoeiuv020.panovel.server.dal.model

import cc.aoeiuv020.json.DateSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class QueryResponse(
        val chaptersCount: Int,
        @Serializable(with = DateSerializer::class)
        val checkUpdateTime: Date
)