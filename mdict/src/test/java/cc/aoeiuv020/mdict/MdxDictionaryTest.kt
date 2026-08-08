package cc.aoeiuv020.mdict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * 通用 MDX 读取器的测试（只验证文件格式层面：键存在、原始记录、精确匹配），
 * 不涉及任何具体词典的正文约定——那些在 [XinhuaDictionaryTest]。
 *
 * 用随 app 打包的 `超级新华字典.mdx` 作样本文件。Gradle 运行测试时工作目录是模块目录
 * （mdict/），词典在 app 的 assets 下；文件不存在（如未拉取该二进制的 CI）则跳过。
 */
class MdxDictionaryTest {
    private val dictFile = File("../app/src/main/assets/dict/超级新华字典.mdx")

    private inline fun withMdx(block: (MdxDictionary) -> Unit) {
        assumeTrue("dictionary asset not present, skipping", dictFile.exists())
        MdxDictionary(dictFile).use(block)
    }

    @Test
    fun exactKeyLookupReturnsRawRecord() = withMdx { mdx ->
        // 精确键返回原始记录文本（不解释内容），
        val records = mdx.lookup("瞬")
        assertEquals(1, records.size)
        assertTrue(records[0].startsWith("`1`瞬`2`"))
    }

    @Test
    fun missingKeyIsEmpty() = withMdx { mdx ->
        assertTrue(!mdx.contains("瞬间引"))
        assertTrue(mdx.lookup("瞬间引").isEmpty())
    }

    @Test
    fun multiRecordKey() = withMdx { mdx ->
        // 唵 这个键下有两条记录，读取器应原样返回两条，
        assertEquals(2, mdx.lookup("唵").size)
    }

    @Test
    fun exposesKeys() = withMdx { mdx ->
        assertTrue(mdx.keys.isNotEmpty())
        assertTrue(mdx.keys.contains("瞬"))
        // 合并键作为原始键存在（拆分是词典层的事，不在读取器里做），
        assertTrue(mdx.keys.contains("蹬腿,蹬腿儿"))
        assertTrue(!mdx.keys.contains("蹬腿"))
    }
}
