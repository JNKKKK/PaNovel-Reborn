package cc.aoeiuv020.mdict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * 现代汉语词典第 5 版特有语义的测试：走公开的 [Dictionary] 接口（[XiandaiDictionary]），
 * 覆盖 MDX 2.0/加密读取之上的词头/拼音解析、重复键多音字、正文清洗等该词典的怪癖。
 * 词典文件不存在时跳过（同 [MdxDictionaryTest]）。
 */
class XiandaiDictionaryTest {
    private val dictFile = File("../app/src/main/assets/dict/现代汉语词典第5版.mdx")

    private inline fun withDict(block: (Dictionary) -> Unit) {
        assumeTrue("dictionary asset not present, skipping", dictFile.exists())
        XiandaiDictionary(dictFile).use(block)
    }

    @Test
    fun singleCharacter() = withDict { dict ->
        val e = dict.senses("瞬").single()
        assertEquals("瞬", e.headword)
        assertEquals("shùn", e.pinyin)
        assertTrue(e.definitionHtml.contains("眨眼"))
    }

    @Test
    fun bracketedHeadwordStripped() = withDict { dict ->
        // 词头带书名号【蹬腿】，解析后应去掉书名号，拼音含插入号 ∥，
        val e = dict.senses("蹬腿").single()
        assertEquals("蹬腿", e.headword)
        assertEquals("dēnɡ∥tuǐ", e.pinyin)
    }

    @Test
    fun polyphoneRepeatedKeys() = withDict { dict ->
        // 「的」是重复键：5 条记录、各一个读音；词头都为「的」，
        val de = dict.senses("的")
        assertEquals(5, de.size)
        assertTrue(de.all { it.headword == "的" })
        // 其中应出现 dí / dì 等读音（轻声 de 那条拼音夹在结构化正文里，允许为空），
        assertTrue(de.any { it.pinyin == "dí" })
        assertTrue(de.any { it.pinyin == "dì" })
    }

    @Test
    fun variantParenLiftedBeforePinyin() = withDict { dict ->
        // 「几」的多音记录里，异体括注(幾)在拼音前；应被提到释义最前面、拼音正确切出，
        val entries = dict.senses("几")
        assertEquals(3, entries.size)
        val ji3 = entries.first { it.pinyin == "jǐ" }
        assertTrue(ji3.definitionHtml.startsWith("(幾)") || ji3.definitionHtml.startsWith("（幾）"))
    }

    @Test
    fun proverbWithFullwidthComma() = withDict { dict ->
        val e = dict.senses("一不做，二不休").single()
        assertEquals("一不做，二不休", e.headword)
        assertEquals("yī bù zuò，èr bù xiū", e.pinyin)
    }

    @Test
    fun definitionCleaned() = withDict { dict ->
        // 清洗后不应残留全角空格的字体标签、伪标签、span，
        val defs = dict.senses("蹬腿") + dict.senses("上") + dict.senses("啊")
        assertTrue(defs.isNotEmpty())
        defs.forEach { e ->
            assertFalse("残留全角空格字体标签", e.definitionHtml.contains("<font　face"))
            assertFalse("残留 span 标签", e.definitionHtml.contains("<span"))
            assertFalse("残留语体伪标签 <书>", e.definitionHtml.contains("<书>"))
            assertFalse("残留语体伪标签 <方>", e.definitionHtml.contains("<方>"))
        }
    }

    @Test
    fun greedyBoundary() = withDict { dict ->
        assertTrue(dict.contains("瞬"))
        assertFalse(dict.contains("瞬间引"))
        assertTrue(dict.senses("瞬间引").isEmpty())
    }

    @Test
    fun structuredBodyWithoutPinyin() = withDict { dict ->
        // 轻声「的」(de) 那条以「1·de …」结构化正文起头，切不出规范拼音时应留空、正文保留，
        val de = dict.senses("的")
        assertTrue(de.any { it.pinyin == null })
    }
}
