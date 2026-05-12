package cc.aoeiuv020.panovel.server.dal.model

import kotlinx.serialization.Serializable

@Serializable
data class Config(
        val apiUrl: String?,
        val minVersion: String,
        val qqGroup: String?
)