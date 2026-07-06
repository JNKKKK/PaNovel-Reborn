package cc.aoeiuv020.panovel.api

import org.junit.Test

class NovelContextTest {
    @Test
    fun count() {
        println(NovelContext.getAllSite().count { !it.hide })
    }
}