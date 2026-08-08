package cc.aoeiuv020.mdict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** DictEntry 解析纯逻辑测试，不依赖词典文件，始终运行。 */
class DictEntryTest {
    @Test
    fun pinyinLifted() {
        val e = DictEntry.parse("`1`瞬间`2`shùnjiān<br>[in a twinkling] 一眨眼", "瞬间")
        assertEquals("瞬间", e.headword)
        assertEquals("shùnjiān", e.pinyin)
        assertTrue(e.definitionHtml.startsWith("[in a twinkling]"))
        // 拼音已提到词头，不应残留在释义里，
        assertTrue(!e.definitionHtml.contains("shùnjiān"))
    }

    @Test
    fun toneMarksAllowed() {
        val e = DictEntry.parse("`1`爱`2`ài<br>喜欢", "爱")
        assertEquals("ài", e.pinyin)
    }

    @Test
    fun uUmlautAllowed() {
        val e = DictEntry.parse("`1`女`2`nǚ<br>女性", "女")
        assertEquals("nǚ", e.pinyin)
    }

    @Test
    fun commaJoinedVariantPinyin() {
        // 异形词合并键：词头与拼音都用逗号连写，拼音仍应识别并提取，
        val e = DictEntry.parse("`1`蹬腿,蹬腿儿`2`dēngtuǐ，dēngtuǐr<br>伸腿", "蹬腿")
        assertEquals("蹬腿,蹬腿儿", e.headword)
        assertEquals("dēngtuǐ，dēngtuǐr", e.pinyin)
        assertEquals("伸腿", e.definitionHtml)
    }

    @Test
    fun structuredBodyKeepsAllAsDefinition() {
        // 几：首段是 "(1)" 而非拼音，pinyin 置空，正文保留完整，
        val raw = "`1`几`2`(1)<br>幾<br>jī<br>苗头"
        val e = DictEntry.parse(raw, "几")
        assertEquals("几", e.headword)
        assertNull(e.pinyin)
        assertTrue(e.definitionHtml.contains("(1)"))
        assertTrue(e.definitionHtml.contains("苗头"))
    }

    @Test
    fun malformedFallsBackToRaw() {
        val e = DictEntry.parse("没有标记的原始文本", "某")
        assertEquals("某", e.headword)
        assertNull(e.pinyin)
        assertEquals("没有标记的原始文本", e.definitionHtml)
    }

    @Test
    fun hanDetection() {
        assertTrue('瞬'.isHan())
        assertTrue('间'.isHan())
        assertTrue(!'　'.isHan()) // 全角空格（段首缩进）
        assertTrue(!'，'.isHan()) // 标点
        assertTrue(!'a'.isHan())
        assertTrue(!' '.isHan())
    }
}
