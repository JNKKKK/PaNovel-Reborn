package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.RetryRule
import cc.aoeiuv020.panovel.api.SiteIntegrationTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.experimental.categories.Category

@Category(SiteIntegrationTest::class)
class HongshengTest {
    @get:Rule val retryRule = RetryRule()
    private val context = Hongsheng()

    @Test
    fun testSearch() {
        val list = context.searchNovelName("凡间")
        assertTrue("search should return results", list.isNotEmpty())
        list.first().let {
            assertTrue(it.name.isNotBlank())
            assertTrue(it.author.isNotBlank())
            assertTrue(it.extra.isNotBlank())
        }
        println("search results: ${list.size}")
        list.take(3).forEach { println("  ${it.name} by ${it.author} [${it.extra}]") }
    }

    @Test
    fun testDetail() {
        val detail = context.getNovelDetail("66/8719")
        assertEquals("凡间狱", detail.novel.name)
        assertEquals("风尘散人", detail.novel.author)
        assertNotNull(detail.image)
        assertTrue(detail.introduction.isNotBlank())
        println("detail: ${detail.novel.name} by ${detail.novel.author}")
        println("  image: ${detail.image}")
        println("  intro: ${detail.introduction.take(50)}...")
    }

    @Test
    fun testChapters() {
        val chapters = context.getNovelChaptersAsc("66/8719")
        assertTrue("should have chapters", chapters.isNotEmpty())
        println("chapters: ${chapters.size}")
        println("  first: ${chapters.first().name} [${chapters.first().extra}]")
        println("  last: ${chapters.last().name}")
    }

    @Test
    fun testContent() {
        val content = context.getNovelContent("6848")
        assertTrue("should have content lines", content.isNotEmpty())
        assertTrue("first line should have text", content.first().isNotBlank())
        println("content lines: ${content.size}")
        println("  first: ${content.first().take(80)}...")
    }
}
