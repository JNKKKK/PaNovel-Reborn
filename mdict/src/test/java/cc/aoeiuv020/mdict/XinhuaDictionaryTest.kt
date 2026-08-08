package cc.aoeiuv020.mdict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * 超级新华字典特有语义的测试：走公开的 [Dictionary] 接口（[XinhuaDictionary]），
 * 覆盖记录解析、多音字数字后缀、异形词合并键、逗号连写拼音等该词典的怪癖。
 * 词典文件不存在时跳过（同 [MdxDictionaryTest]）。
 */
class XinhuaDictionaryTest {
    private val dictFile = File("../app/src/main/assets/dict/超级新华字典.mdx")

    private inline fun withDict(block: (Dictionary) -> Unit) {
        assumeTrue("dictionary asset not present, skipping", dictFile.exists())
        XinhuaDictionary(dictFile).use(block)
    }

    @Test
    fun singleCharacter() = withDict { dict ->
        // 瞬 有裸键 + 瞬1 两个义项（读音都是 shùn），senses 应把它们合并，词头都为「瞬」，
        val entries = dict.senses("瞬")
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.headword == "瞬" })
        assertTrue(entries.all { it.pinyin == "shùn" })
        assertTrue(entries[0].definitionHtml.contains("眨眼"))
    }

    @Test
    fun proverb() = withDict { dict ->
        val e = dict.senses("瞬间").single()
        assertEquals("瞬间", e.headword)
        assertEquals("shùnjiān", e.pinyin)
    }

    @Test
    fun greedyBoundary() = withDict { dict ->
        assertTrue(dict.contains("瞬"))
        assertTrue(dict.contains("瞬间"))
        assertTrue(!dict.contains("瞬间引"))
        assertTrue(dict.senses("瞬间引").isEmpty())
    }

    @Test
    fun structuredBodyWithoutPinyin() = withDict { dict ->
        // 几 的正文以 "(1)<br>幾<br>jī..." 开头，首段不是拼音，pinyin 应为 null，
        val e = dict.senses("几").first()
        assertEquals("几", e.headword)
        assertNull(e.pinyin)
    }

    @Test
    fun numericSuffixSenses() = withDict { dict ->
        // 的：没有裸键，只有 的1/的2/的3 —— senses 应合并成 3 个义项，词头都显示「的」，
        val de = dict.senses("的")
        assertEquals(3, de.size)
        assertTrue(de.all { it.headword == "的" })
        assertEquals(listOf("de", "dí", "dì"), de.map { it.pinyin })
        assertTrue(dict.contains("的"))
        // 义：裸键 + 义1/义2 —— 合并成 3 个义项，
        assertEquals(3, dict.senses("义").size)
    }

    @Test
    fun variantAliasKeys() = withDict { dict ->
        // 异形词/儿化写法合并进一个键（逗号/全角逗号/分号分隔），各变体都应能查到，
        assertTrue(dict.contains("蹬腿"))    // 蹬腿,蹬腿儿
        assertTrue(dict.contains("蹬腿儿"))
        assertTrue(dict.contains("堤岸"))    // 堤岸，堤坝
        assertTrue(dict.contains("堤坝"))
        // 查任一变体拿到的应是同一条合并记录，
        assertEquals(dict.senses("堤岸"), dict.senses("堤坝"))
    }

    @Test
    fun variantPinyinLifted() = withDict { dict ->
        // 合并键的拼音也是逗号连写，应能识别并提到词头，
        assertEquals("dēngtuǐ，dēngtuǐr", dict.senses("蹬腿").first().pinyin)
    }
}
