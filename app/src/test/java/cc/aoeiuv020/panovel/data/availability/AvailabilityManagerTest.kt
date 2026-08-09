package cc.aoeiuv020.panovel.data.availability

import cc.aoeiuv020.panovel.api.HomePageProbe
import cc.aoeiuv020.panovel.data.availability.AvailabilityManager.Companion.barsFor
import cc.aoeiuv020.panovel.data.availability.AvailabilityManager.Companion.classify
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 探测判定与状态条换算的单元测试，两者都是纯函数，编码了产品决策（严格成功 + 识别 Cloudflare），
 */
class AvailabilityManagerTest {

    @Test
    fun `unreachable is FAIL`() {
        assertEquals(ProbeStatus.FAIL, classify(HomePageProbe(reachable = false)))
    }

    @Test
    fun `2xx with body is OK`() {
        assertEquals(
            ProbeStatus.OK,
            classify(HomePageProbe(reachable = true, code = 200, bodySnippet = "<html>正文</html>"))
        )
    }

    @Test
    fun `2xx with empty body is FAIL (strict success)`() {
        assertEquals(
            ProbeStatus.FAIL,
            classify(HomePageProbe(reachable = true, code = 200, bodySnippet = "   "))
        )
    }

    @Test
    fun `non-2xx without cloudflare is FAIL`() {
        assertEquals(
            ProbeStatus.FAIL,
            classify(HomePageProbe(reachable = true, code = 404, bodySnippet = "not found"))
        )
    }

    @Test
    fun `cloudflare 200 challenge page is FAIL not OK`() {
        // 暂不支持 CF：挑战页返回 200 + 真实 body，纯严格成功会误判为 OK，应判为不可用，
        val probe = HomePageProbe(
            reachable = true,
            code = 200,
            server = "cloudflare",
            bodySnippet = "<title>Just a moment...</title><div class=\"challenge-platform\">"
        )
        assertEquals(ProbeStatus.FAIL, classify(probe))
    }

    @Test
    fun `cf-mitigated header means FAIL`() {
        val probe = HomePageProbe(
            reachable = true,
            code = 403,
            cfMitigated = "challenge",
            bodySnippet = ""
        )
        assertEquals(ProbeStatus.FAIL, classify(probe))
    }

    @Test
    fun `cloudflare 503 is FAIL`() {
        val probe = HomePageProbe(
            reachable = true,
            code = 503,
            server = "cloudflare",
            bodySnippet = "error"
        )
        assertEquals(ProbeStatus.FAIL, classify(probe))
    }

    @Test
    fun `site merely fronted by cloudflare but serving content is OK`() {
        // 用 CDN 但正常返回内容的站不应被判为拦截，
        val probe = HomePageProbe(
            reachable = true,
            code = 200,
            server = "cloudflare",
            bodySnippet = "<html><body>小说列表</body></html>"
        )
        assertEquals(ProbeStatus.OK, classify(probe))
    }

    @Test
    fun `non-cloudflare page containing marker text is OK not FAIL`() {
        // 正文里恰好含 CF 挑战页字样、但 server 头不是 cloudflare 的正常站点不应被误判为拦截，
        val probe = HomePageProbe(
            reachable = true,
            code = 200,
            server = "nginx",
            bodySnippet = "<article>Just a moment... the challenge-platform update is live</article>"
        )
        assertEquals(ProbeStatus.OK, classify(probe))
    }

    @Test
    fun `cloudflare server with challenge body is FAIL`() {
        // server 头确为 cloudflare 且 body 带挑战标记，判为拦截，
        val probe = HomePageProbe(
            reachable = true,
            code = 200,
            server = "cloudflare",
            bodySnippet = "<div id=\"cf-browser-verification\">"
        )
        assertEquals(ProbeStatus.FAIL, classify(probe))
    }

    @Test
    fun `barsFor pads left with UNKNOWN when history is short`() {
        val records = listOf(
            ProbeRecord(10, ProbeStatus.OK),
            ProbeRecord(11, ProbeStatus.FAIL),
        )
        val bars = barsFor(records, slots = 5)
        assertEquals(
            listOf(
                ProbeStatus.UNKNOWN,
                ProbeStatus.UNKNOWN,
                ProbeStatus.UNKNOWN,
                ProbeStatus.OK,
                ProbeStatus.FAIL,
            ),
            bars
        )
    }

    @Test
    fun `barsFor keeps newest when history exceeds slots`() {
        val records = (0L until 10L).map { ProbeRecord(it, ProbeStatus.OK) } +
            ProbeRecord(10, ProbeStatus.RECOVERED)
        val bars = barsFor(records, slots = 3)
        assertEquals(3, bars.size)
        // 最新一条在最右，且应保留，
        assertEquals(ProbeStatus.RECOVERED, bars.last())
    }

    @Test
    fun `barsFor with null history is all UNKNOWN`() {
        val bars = barsFor(null, slots = 4)
        assertEquals(List(4) { ProbeStatus.UNKNOWN }, bars)
    }

    @Test
    fun `upsertToday appends when last record is an earlier day`() {
        val history = listOf(ProbeRecord(100, ProbeStatus.OK))
        val updated = AvailabilityManager.upsertToday(history, today = 101, status = ProbeStatus.FAIL)
        assertEquals(
            listOf(ProbeRecord(100, ProbeStatus.OK), ProbeRecord(101, ProbeStatus.FAIL)),
            updated
        )
    }

    @Test
    fun `upsertToday replaces today's record on reprobe recovery (red to yellow)`() {
        // 当天已是红，重探成功要把当天那条升级为黄，而不是新增一条，
        val history = listOf(
            ProbeRecord(100, ProbeStatus.OK),
            ProbeRecord(101, ProbeStatus.FAIL),
        )
        val updated = AvailabilityManager.upsertToday(
            history, today = 101, status = ProbeStatus.RECOVERED
        )
        assertEquals(
            listOf(ProbeRecord(100, ProbeStatus.OK), ProbeRecord(101, ProbeStatus.RECOVERED)),
            updated
        )
        // 关键：没有因为重探而多出一条，
        assertEquals(2, updated.size)
    }

    @Test
    fun `upsertToday from empty history starts a new list`() {
        val updated = AvailabilityManager.upsertToday(null, today = 5, status = ProbeStatus.OK)
        assertEquals(listOf(ProbeRecord(5, ProbeStatus.OK)), updated)
    }
}
