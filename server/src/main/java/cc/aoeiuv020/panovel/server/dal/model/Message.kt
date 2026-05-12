package cc.aoeiuv020.panovel.server.dal.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
        val title: String?,
        val content: String?
)