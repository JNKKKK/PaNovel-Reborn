package cc.aoeiuv020.panovel.util

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cc.aoeiuv020.panovel.server.ServerAddress
import cc.aoeiuv020.panovel.server.dal.model.Config
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DnsUtilsAndroidTest {

    @Before
    fun init() {
        DnsUtils.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testBean() {
        val config: Config = DnsUtils.txtToBean(ServerAddress.CONFIG_HOST)
        println(config)
        assertTrue(config.minVersion.isNotEmpty())
    }
}