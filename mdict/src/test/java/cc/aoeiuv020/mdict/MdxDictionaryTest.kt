package cc.aoeiuv020.mdict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * 针对随 app 打包的 `超级新华字典.mdx` 的读取测试。
 * Gradle 运行测试时工作目录是模块目录（mdict/），词典在 app 的 assets 下。
 * 若文件不存在（比如未拉取该二进制资源的 CI）则跳过，不判失败。
 */
class MdxDictionaryTest {
    private val dictFile = File("../app/src/main/assets/dict/超级新华字典.mdx")

    private inline fun withDict(block: (MdxDictionary) -> Unit) {
        assumeTrue("dictionary asset not present, skipping", dictFile.exists())
        MdxDictionary(dictFile).use(block)
    }

    @Test
    fun singleCharacter() = withDict { mdx ->
        val entries = mdx.lookup("瞬")
        assertEquals(1, entries.size)
        val e = DictEntry.parse(entries[0], "瞬")
        assertEquals("瞬", e.headword)
        assertEquals("shùn", e.pinyin)
        assertTrue(e.definitionHtml.contains("眨眼"))
    }

    @Test
    fun proverb() = withDict { mdx ->
        val entries = mdx.lookup("瞬间")
        assertEquals(1, entries.size)
        val e = DictEntry.parse(entries[0], "瞬间")
        assertEquals("瞬间", e.headword)
        assertEquals("shùnjiān", e.pinyin)
    }

    @Test
    fun greedyBoundary() = withDict { mdx ->
        // 长按取词的贪婪扩展：瞬、瞬间 命中，瞬间引 不命中，
        assertTrue(mdx.contains("瞬"))
        assertTrue(mdx.contains("瞬间"))
        assertTrue(!mdx.contains("瞬间引"))
        assertTrue(mdx.lookup("瞬间引").isEmpty())
    }

    @Test
    fun multipleEntries() = withDict { mdx ->
        // 唵 在词典中有两个义项，
        val entries = mdx.lookup("唵")
        assertEquals(2, entries.size)
    }

    @Test
    fun structuredBodyWithoutPinyin() = withDict { mdx ->
        // 几 的正文以 "(1)<br>幾<br>jī..." 开头，首段不是拼音，pinyin 应为 null，
        val e = DictEntry.parse(mdx.lookup("几").first(), "几")
        assertEquals("几", e.headword)
        assertNull(e.pinyin)
    }

    @Test
    fun numericSuffixSenses() = withDict { mdx ->
        // 多音/多义字被拆成数字后缀键，两种形态都要能查到：
        // 的：没有裸键，只有 的1/的2/的3，
        assertTrue(mdx.lookup("的").isEmpty())
        assertTrue(mdx.lookup("的1").isNotEmpty())
        assertTrue(mdx.lookup("的2").isNotEmpty())
        assertTrue(mdx.lookup("的3").isNotEmpty())
        assertTrue(mdx.lookup("的4").isEmpty())
        // 义：裸键 + 义1/义2 都存在，
        assertTrue(mdx.lookup("义").isNotEmpty())
        assertTrue(mdx.lookup("义1").isNotEmpty())
        assertTrue(mdx.lookup("义2").isNotEmpty())
        assertTrue(mdx.lookup("义3").isEmpty())
    }
}
