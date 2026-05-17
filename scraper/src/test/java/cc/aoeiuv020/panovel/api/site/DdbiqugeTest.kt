package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.RetryRule
import cc.aoeiuv020.panovel.api.SiteIntegrationTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.experimental.categories.Category

@Category(SiteIntegrationTest::class)
class DdbiqugeTest {
    @get:Rule val retryRule = RetryRule()
    private val context = Ddbiquge()

    @Test
    fun testSearch() {
        val list = context.searchNovelName("斗破苍穹")
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
        val detail = context.getNovelDetail("0_84")
        assertEquals("一级沉寂", detail.novel.name)
        assertEquals("九阶幻方", detail.novel.author)
        assertTrue(detail.introduction.isNotBlank())
        println("detail: ${detail.novel.name} by ${detail.novel.author}")
        println("  intro: ${detail.introduction.take(50)}...")
    }

    @Test
    fun testChapters() {
        val chapters = context.getNovelChaptersAsc("0_84")
        assertTrue("should have chapters", chapters.isNotEmpty())
        assertTrue("should have many chapters", chapters.size > 100)
        println("chapters: ${chapters.size}")
        println("  first: ${chapters.first().name} [${chapters.first().extra}]")
        println("  last: ${chapters.last().name}")
    }

    @Test
    fun testContent() {
        val content = context.getNovelContent("0_84/4540")
        assertTrue("should have content lines", content.isNotEmpty())
        assertTrue("first line should have text", content.first().isNotBlank())
        assertTrue("multi-page chapter should have many lines", content.size > 10)
        println("content lines: ${content.size}")
        println("  first: ${content.first().take(80)}...")
    }

    @Test
    fun testChaptersLargeNovel() {
        val chapters = context.getNovelChaptersAsc("26_26646")
        assertTrue("should have chapters", chapters.isNotEmpty())
        println("chapters: ${chapters.size}")
        println("  first: ${chapters.first().name} [${chapters.first().extra}]")
        println("  last: ${chapters.last().name} [${chapters.last().extra}]")
        assertTrue("should have all 427 chapters", chapters.size >= 427)
    }
}
