package cc.aoeiuv020.panovel.api

/**
 * 书源首页可达性探测的原始结果，
 *
 * 只携带原始信号（状态码、少量响应头、一小段正文），
 * 不做任何判定，是否可用、是否被 Cloudflare 拦截等语义交给 app 层解释，
 * 以便探测逻辑与判定规则分离，
 */
class HomePageProbe(
    /**
     * 是否拿到了任何 HTTP 响应，
     * false 表示 DNS/TCP/TLS 失败或超时，
     */
    val reachable: Boolean,
    val code: Int = 0,
    val server: String? = null,
    val cfMitigated: String? = null,
    val bodySnippet: String = ""
)
