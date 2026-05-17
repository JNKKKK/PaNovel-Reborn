package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.RetryRule
import cc.aoeiuv020.panovel.api.SiteIntegrationTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.experimental.categories.Category

@Category(SiteIntegrationTest::class)
class DeqixsTest {
    @get:Rule val retryRule = RetryRule()
    private val context = Deqixs()

    @Test
    fun testSearch() {
        val list = context.searchNovelName("重生")
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
        val detail = context.getNovelDetail("126")
        assertEquals("捞尸人", detail.novel.name)
        assertEquals("纯洁滴小龙", detail.novel.author)
        assertNotNull(detail.image)
        assertTrue(detail.introduction.isNotBlank())
        println("detail: ${detail.novel.name} by ${detail.novel.author}")
        println("  image: ${detail.image}")
        println("  intro: ${detail.introduction.take(50)}...")
    }

    @Test
    fun testChapters() {
        val chapters = context.getNovelChaptersAsc("126")
        assertTrue("should have chapters", chapters.isNotEmpty())
        assertEquals("第一章", chapters.first().name)
        println("chapters: ${chapters.size}")
        println("  first: ${chapters.first().name} [${chapters.first().extra}]")
        println("  last: ${chapters.last().name}")
    }

    @Test
    fun testContent() {
        val content = context.getNovelContent("126/81417")
        assertTrue("should have content lines", content.isNotEmpty())
        assertTrue("first line should have text", content.first().isNotBlank())
        println("content lines: ${content.size}")
        println("  first: ${content.first().take(80)}...")
    }
}
