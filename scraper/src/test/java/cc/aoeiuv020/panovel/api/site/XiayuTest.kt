package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.RetryRule
import cc.aoeiuv020.panovel.api.SiteIntegrationTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.experimental.categories.Category

@Category(SiteIntegrationTest::class)
class XiayuTest {
    @get:Rule val retryRule = RetryRule()
    private val context = Xiayu()

    @Test
    fun testSearch() {
        val list = context.searchNovelName("西游")
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
        val detail = context.getNovelDetail("1Q63")
        assertEquals("西游之吞天大熊猫", detail.novel.name)
        assertEquals("愤怒的大熊猫", detail.novel.author)
        assertNotNull(detail.image)
        assertTrue(detail.introduction.isNotBlank())
        println("detail: ${detail.novel.name} by ${detail.novel.author}")
        println("  image: ${detail.image}")
        println("  intro: ${detail.introduction.take(50)}...")
    }

    @Test
    fun testChapters() {
        val chapters = context.getNovelChaptersAsc("1Q63")
        assertTrue("should have chapters", chapters.isNotEmpty())
        assertTrue("should have all chapters", chapters.size > 500)
        println("chapters: ${chapters.size}")
        println("  first: ${chapters.first().name} [${chapters.first().extra}]")
        println("  last: ${chapters.last().name}")
    }

    @Test
    fun testContent() {
        val content = context.getNovelContent("1Q63/xWqQZ")
        assertTrue("should have content lines", content.isNotEmpty())
        assertTrue("should have many content lines", content.size > 10)
        assertTrue("first line should have text", content.first().isNotBlank())
        println("content lines: ${content.size}")
        println("  first: ${content.first().take(80)}...")
    }

    @Test
    fun testContentBrFormat() {
        val content = context.getNovelContent("MA69/rYQQA")
        assertTrue("should have content lines", content.isNotEmpty())
        assertTrue("should have many content lines", content.size > 10)
        assertTrue("first line should have text", content.first().isNotBlank())
        println("content (br format) lines: ${content.size}")
        println("  first: ${content.first().take(80)}...")
    }
}
