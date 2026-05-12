package cc.aoeiuv020.panovel.util

import cc.aoeiuv020.panovel.BuildConfig
import cc.aoeiuv020.panovel.server.ServerAddress
import cc.aoeiuv020.panovel.server.dal.model.Config
import cc.aoeiuv020.panovel.server.dal.model.Message
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder

class DnsUtilsTest {

    @Test
    fun testBean() {
        val config: Config = DnsUtils.txtToBean(ServerAddress.CONFIG_HOST)
        println(config)
        assertTrue(VersionName(config.minVersion) <= VersionName(BuildConfig.VERSION_NAME))
    }

    @Test
    fun makeMessage() {
        val msg = Message(
            "测试通知",
            "test"
        )
        println("title=${urle(msg.title)}&content=${urle(msg.content)}")
    }

    private fun urle(s: String?) =
        URLEncoder.encode(s, "utf8")
}