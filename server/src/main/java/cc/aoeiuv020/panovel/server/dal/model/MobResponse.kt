package cc.aoeiuv020.panovel.server.dal.model

import cc.aoeiuv020.panovel.server.common.ErrorCode
import cc.aoeiuv020.panovel.server.common.toBean
import cc.aoeiuv020.panovel.server.common.toJson
import kotlinx.serialization.Serializable

@Serializable
class MobResponse(
        var code: Int = ErrorCode.UNKNOWN_ERROR.code,
        val data: String = "{}"
) {
    companion object {
        inline fun <reified T> success(data: T): MobResponse {
            return MobResponse(ErrorCode.SUCCESS.code, data.toJson())
        }

        fun error(error: ErrorCode = ErrorCode.UNKNOWN_ERROR): MobResponse {
            return MobResponse(error.code)
        }
    }

    inline fun <reified T> getRealData(): T {
        return data.toBean()
    }

    fun isSuccess(): Boolean {
        return code == ErrorCode.SUCCESS.code
    }
}
