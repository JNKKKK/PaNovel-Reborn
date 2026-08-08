package cc.aoeiuv020.mdict

import org.anarres.lzo.LzoAlgorithm
import org.anarres.lzo.LzoLibrary
import org.anarres.lzo.lzo_uintp
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.zip.Inflater

/**
 * 只读的 MDX（MDict）词典读取器。
 *
 * 目标词典 `超级新华字典.mdx` 已经过实测确认：未加密、GBK 编码、Html 正文、
 * MDict 1.2 版布局（长度字段为 4 字节，键索引未压缩，记录偏移为 4 字节），
 * 键块/记录块均使用 LZO1X 压缩。本类完整支持 1.2 版；遇到 2.0 及以上版本会
 * 抛出明确异常（当前打包的词典不会触发）。
 *
 * 构造时把「词 → 记录偏移」索引整体读入内存（键块很小），记录块则按需解压并用
 * LRU 缓存，避免把十几 MB 的正文全部驻留内存。实例持有文件句柄，用完需 [close]。
 */
class MdxDictionary(private val file: File) : Closeable {
    private val logger = LoggerFactory.getLogger(MdxDictionary::class.java)

    private val raf = RandomAccessFile(file, "r")
    private val charset: Charset

    /** 词 → 该词所有记录在「拼接后的解压记录空间」中的起始偏移；一个词可能有多个义项。 */
    private val index: HashMap<String, LongArray>

    /** 记录块目录，按解压后的累计起点排序，便于二分定位。 */
    private val recordBlocks: Array<RecordBlock>

    /** 最近解压过的记录块缓存，key 为块下标。 */
    private val blockCache = object : LinkedHashMap<Int, ByteArray>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, ByteArray>?): Boolean =
            size > RECORD_BLOCK_CACHE_SIZE
    }

    private class RecordBlock(
        val compOffset: Long,   // 压缩数据在文件中的起点，
        val compSize: Int,
        val decompStart: Long,  // 解压后正文在拼接空间中的起点，
        val decompSize: Int,
    )

    init {
        // ---- header ----
        val headerLen = readIntBE()
        val headerBytes = ByteArray(headerLen)
        raf.readFully(headerBytes)
        val header = String(headerBytes, Charsets.UTF_16LE)
        raf.skipBytes(4) // header adler32

        val encoding = ATTR_ENCODING.find(header)?.groupValues?.get(1)?.trim().orEmpty()
        charset = when (encoding.uppercase()) {
            "", "UTF-8", "UTF8" -> Charsets.UTF_8
            "UTF-16" -> Charsets.UTF_16LE
            "GBK", "GB2312", "GB18030" -> Charset.forName("GBK")
            "BIG5", "BIG-5" -> Charset.forName("Big5")
            else -> Charset.forName(encoding)
        }
        val version = ATTR_VERSION.find(header)?.groupValues?.get(1)?.toFloatOrNull() ?: 1.2f
        require(version < 2.0f) {
            "unsupported MDX version $version; only < 2.0 is implemented"
        }

        // ---- keyword section (1.2: four 4-byte big-endian counts, no checksum) ----
        val numKeyBlocks = readIntBE()
        readIntBE() // numEntries, unused
        val keyIndexLen = readIntBE()
        readIntBE() // keyBlocksLen, unused

        // key index (uncompressed in 1.2)
        val keyIndex = ByteArray(keyIndexLen)
        raf.readFully(keyIndex)
        val keyBlockSizes = IntArray(numKeyBlocks) // 压缩大小，
        val keyBlockDecompSizes = IntArray(numKeyBlocks)
        run {
            var p = 0
            for (i in 0 until numKeyBlocks) {
                p += 4 // entries-in-block count, unused
                val firstSize = keyIndex[p].toInt() and 0xFF; p += 1 + firstSize
                val lastSize = keyIndex[p].toInt() and 0xFF; p += 1 + lastSize
                keyBlockSizes[i] = readIntBE(keyIndex, p); p += 4
                keyBlockDecompSizes[i] = readIntBE(keyIndex, p); p += 4
            }
        }

        // key blocks -> word => offsets
        val builder = HashMap<String, MutableList<Long>>(1 shl 16)
        for (i in 0 until numKeyBlocks) {
            val comp = ByteArray(keyBlockSizes[i])
            raf.readFully(comp)
            val block = decompressBlock(comp, keyBlockDecompSizes[i])
            parseKeyBlock(block, builder)
        }
        index = HashMap(builder.size * 4 / 3 + 1)
        for ((k, v) in builder) index[k] = v.toLongArray()

        // ---- record section ----
        val numRecordBlocks = readIntBE()
        readIntBE() // numEntries, unused
        readIntBE() // record index length, unused
        readIntBE() // record blocks length, unused
        val blocks = ArrayList<RecordBlock>(numRecordBlocks)
        // 先读完记录索引（每块 comp/decomp 各 4 字节），再定位到记录数据起点，
        val recordIndex = ByteArray(numRecordBlocks * 8)
        raf.readFully(recordIndex)
        var compOffset = raf.filePointer
        var decompStart = 0L
        run {
            var p = 0
            for (i in 0 until numRecordBlocks) {
                val compSize = readIntBE(recordIndex, p); p += 4
                val decompSize = readIntBE(recordIndex, p); p += 4
                blocks.add(RecordBlock(compOffset, compSize, decompStart, decompSize))
                compOffset += compSize
                decompStart += decompSize
            }
        }
        recordBlocks = blocks.toTypedArray()
        logger.debug("mdx loaded: {} words, {} record blocks", index.size, recordBlocks.size)
    }

    /**
     * 查询一个词的全部义项原始正文（形如 `` `1`词`2`拼音<br>释义 ``）。
     * 词不存在时返回空列表。查询线程安全由调用方保证（同一实例串行使用）。
     */
    fun lookup(word: String): List<String> {
        val offsets = index[word] ?: return emptyList()
        return offsets.map { readRecord(it) }
    }

    /** 词是否存在（用于长按取词时贪婪扩展匹配）。 */
    fun contains(word: String): Boolean = index.containsKey(word)

    private fun readRecord(offset: Long): String {
        // 二分定位所在记录块，
        var lo = 0
        var hi = recordBlocks.size - 1
        var bi = 0
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val b = recordBlocks[mid]
            if (offset < b.decompStart) hi = mid - 1
            else if (offset >= b.decompStart + b.decompSize) lo = mid + 1
            else {
                bi = mid; break
            }
        }
        val block = getRecordBlock(bi)
        val local = (offset - recordBlocks[bi].decompStart).toInt()
        var end = local
        while (end < block.size && block[end].toInt() != 0) end++
        return String(block, local, end - local, charset)
    }

    private fun getRecordBlock(bi: Int): ByteArray {
        blockCache[bi]?.let { return it }
        val b = recordBlocks[bi]
        val comp = ByteArray(b.compSize)
        synchronized(raf) {
            raf.seek(b.compOffset)
            raf.readFully(comp)
        }
        val block = decompressBlock(comp, b.decompSize)
        blockCache[bi] = block
        return block
    }

    private fun parseKeyBlock(block: ByteArray, out: HashMap<String, MutableList<Long>>) {
        var p = 0
        while (p < block.size) {
            val offset = readIntBE(block, p).toLong() and 0xFFFFFFFFL; p += 4
            var end = p
            while (end < block.size && block[end].toInt() != 0) end++
            val key = String(block, p, end - p, charset)
            out.getOrPut(key) { ArrayList(1) }.add(offset)
            p = end + 1
        }
    }

    /**
     * 解压一个 MDict 块：前 4 字节为压缩类型（小端首字节），随后 4 字节 adler32，其余为压缩数据。
     * 0=无压缩，1=LZO1X，2=zlib。
     */
    private fun decompressBlock(comp: ByteArray, decompSize: Int): ByteArray {
        val type = comp[0].toInt() and 0xFF
        val bodyOff = 8
        val bodyLen = comp.size - bodyOff
        return when (type) {
            0 -> comp.copyOfRange(bodyOff, comp.size)
            1 -> {
                val out = ByteArray(decompSize)
                val outLen = lzo_uintp(decompSize)
                lzo.decompress(comp, bodyOff, bodyLen, out, 0, outLen)
                if (outLen.value == decompSize) out else out.copyOf(outLen.value)
            }
            2 -> {
                val inflater = Inflater()
                inflater.setInput(comp, bodyOff, bodyLen)
                val out = ByteArray(decompSize)
                var total = 0
                while (!inflater.finished() && total < decompSize) {
                    val n = inflater.inflate(out, total, decompSize - total)
                    if (n == 0) break
                    total += n
                }
                inflater.end()
                if (total == decompSize) out else out.copyOf(total)
            }
            else -> throw IllegalStateException("unknown MDX block compression type $type")
        }
    }

    private fun readIntBE(): Int {
        val a = raf.read(); val b = raf.read(); val c = raf.read(); val d = raf.read()
        if ((a or b or c or d) < 0) throw java.io.EOFException()
        return (a shl 24) or (b shl 16) or (c shl 8) or d
    }

    override fun close() {
        raf.close()
    }

    private companion object {
        private const val RECORD_BLOCK_CACHE_SIZE = 16
        private val ATTR_ENCODING = Regex("""Encoding="([^"]*)"""")
        private val ATTR_VERSION = Regex("""RequiredEngineVersion="([^"]*)"""")
        private val lzo = LzoLibrary.getInstance().newDecompressor(LzoAlgorithm.LZO1X, null)

        private fun readIntBE(b: ByteArray, off: Int): Int =
            ((b[off].toInt() and 0xFF) shl 24) or
                ((b[off + 1].toInt() and 0xFF) shl 16) or
                ((b[off + 2].toInt() and 0xFF) shl 8) or
                (b[off + 3].toInt() and 0xFF)
    }
}
