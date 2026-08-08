package cc.aoeiuv020.panovel.data.availability

import kotlinx.serialization.Serializable

/**
 * 书源某一天的可达性采样结果，
 */
@Serializable
enum class ProbeStatus {
    /** 未采样（当天没探测），展示为灰条， */
    UNKNOWN,

    /** 可用：首次探测就拿到正常内容，绿条， */
    OK,

    /**
     * 恢复：当天先探测为不可用（红），之后同一天的重试探测又成功了，黄条，
     * 恢复状态当天不再重试，保持黄色，
     */
    RECOVERED,

    /**
     * 不可用：无法连接、返回错误，或被 Cloudflare 拦截（暂不支持 CF，视同不可用），红条，
     * 当天可被后续的重试探测再给一次机会，
     */
    FAIL,
}

/**
 * 一条采样记录，[epochDay] 为本地时区的天序号（1970-01-01 为 0），
 * 每个书源每天最多一条，
 */
@Serializable
data class ProbeRecord(
    val epochDay: Long,
    val status: ProbeStatus,
)
