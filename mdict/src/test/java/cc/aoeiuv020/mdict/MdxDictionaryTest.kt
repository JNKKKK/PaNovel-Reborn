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

    // ---- MDX 2.0 + Encrypted=2 + UTF-16（现代汉语词典）格式层测试 ----
    // 与上面共用同一个读取器，验证 8 字节长度字段、zlib 压缩+加密的键索引、
    // 2 字节键/记录终止符都能正确解析。文件不存在则跳过。
    private val v2File = File("../app/src/main/assets/dict/现代汉语词典第5版.mdx")

    private inline fun withV2(block: (MdxDictionary) -> Unit) {
        assumeTrue("v2 dictionary asset not present, skipping", v2File.exists())
        MdxDictionary(v2File).use(block)
    }

    @Test
    fun v2KeysAndEncryptedIndexDecoded() = withV2 { mdx ->
        // 键索引经 zlib 压缩且 Encrypted=2 加密，能解出 6 万多个键即证明解密/解压正确，
        assertTrue(mdx.keys.size > 60000)
        assertTrue(mdx.keys.contains("瞬"))
        assertTrue(mdx.keys.contains("蹬腿"))
    }

    @Test
    fun v2Utf16RecordRoundTrips() = withV2 { mdx ->
        // UTF-16 记录以 2 字节 00 00 分隔，读出的正文应含该词典特有的 navy 词头字体，
        val rec = mdx.lookup("瞬").single()
        assertTrue(rec.contains("navy"))
        assertTrue(rec.contains("瞬"))
    }

    @Test
    fun v2RepeatedKeyReturnsAllRecords() = withV2 { mdx ->
        // 「的」是重复键，读取器应原样返回它的多条记录（拆分/解释是词典层的事），
        assertEquals(5, mdx.lookup("的").size)
    }
}
