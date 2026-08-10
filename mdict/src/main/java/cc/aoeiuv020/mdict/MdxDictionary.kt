package cc.aoeiuv020.mdict

import org.anarres.lzo.LzoAlgorithm
import org.anarres.lzo.LzoLibrary
import org.anarres.lzo.lzo_uintp
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.zip.Inflater

/**
 * 只读的 MDX（MDict）词典读取器 —— **通用**，只负责 MDX 文件格式本身，不理解任何具体
 * 词典的正文/键名约定。它把「键 → 该键的原始记录文本」暴露出来，具体记录里的标记、
 * 多音字拆分、异形词合并等约定由各词典自己的 [Dictionary] 实现去解释
 * （见 [XinhuaDictionary]、[XiandaiDictionary]）。
 *
 * 已支持并实测的格式：
 * - **1.2 版**：长度字段 4 字节、键索引未压缩、记录偏移 4 字节（如 GBK 的超级新华字典）。
 * - **2.0 版**：长度字段 8 字节、键索引整块 zlib 压缩、记录偏移 8 字节；键索引块可带
 *   `Encrypted=2` 加密（RIPEMD-128 派生密钥 + MDX fast-decrypt，见 [mdxDecrypt]），
 *   如 UTF-16 的现代汉语词典第 5 版。
 *
 * 编码支持 GBK/UTF-8/UTF-16/Big5；块压缩 none/LZO1X/zlib 均可。UTF-16 词典的键与记录
 * 用 2 字节 `00 00` 作分隔，其它编码用单字节 `00`。遇到 3.0 及以上版本抛出明确异常。
 *
 * 构造时把「键 → 记录偏移」索引整体读入内存（键块很小），记录块则按需解压并用
 * LRU 缓存，避免把十几 MB 的正文全部驻留内存。实例持有文件句柄，用完需 [close]。
 */
class MdxDictionary(private val file: File) : Closeable {
    private val logger = LoggerFactory.getLogger(MdxDictionary::class.java)

    private val raf = RandomAccessFile(file, "r")
    private val charset: Charset

    /** UTF-16 词典的键/记录以 2 字节 `00 00` 分隔，其它编码用单字节 `00`。 */
    private val termWidth: Int

    /** 键 → 该键所有记录在「拼接后的解压记录空间」中的起始偏移；一个键可能对应多条记录。 */
    private val index: HashMap<String, LongArray>

    /** 词典中的全部原始键（未经任何拆分/归一），供 [Dictionary] 实现构建自己的别名/索引。 */
    val keys: Set<String> get() = index.keys

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
            "UTF-16", "UTF16" -> Charsets.UTF_16LE
            "GBK", "GB2312", "GB18030" -> Charset.forName("GBK")
            "BIG5", "BIG-5" -> Charset.forName("Big5")
            else -> Charset.forName(encoding)
        }
        termWidth = if (charset == Charsets.UTF_16LE) 2 else 1
        val version = ATTR_VERSION.find(header)?.groupValues?.get(1)?.toFloatOrNull() ?: 1.2f
        require(version < 3.0f) {
            "unsupported MDX version $version; only 1.x/2.x are implemented"
        }
        val v2 = version >= 2.0f
        // Encrypted 可能是 No/Yes/0/1/2/3；仅第 2 位（键索引加密）我们处理，
        val encrypted = when (val e = ATTR_ENCRYPTED.find(header)?.groupValues?.get(1)?.trim()) {
            null, "", "No" -> 0
            "Yes" -> 1
            else -> e.toIntOrNull() ?: 0
        }
        // 长度字段与偏移字段的宽度：2.0 版一律 8 字节，1.2 版一律 4 字节，
        val numW = if (v2) 8 else 4

        // ---- keyword section ----
        val numKeyBlocks = readNumber(v2).toInt()
        readNumber(v2) // numEntries, unused
        val keyInfoDecompSize = if (v2) readNumber(v2) else 0L // 仅 2.0 版有该字段，
        val keyInfoSize = readNumber(v2)
        readNumber(v2) // keyBlockSize (total), unused
        if (v2) raf.skipBytes(4) // 2.0 版：以上 5 个数的 adler32 校验，跳过，

        // 键索引：1.2 版未压缩直接用；2.0 版整块 zlib 压缩（可能先经 Encrypted=2 加密）。
        val keyInfoRaw = ByteArray(keyInfoSize.toInt())
        raf.readFully(keyInfoRaw)
        val keyIndex: ByteArray = if (v2) {
            val decrypted = if (encrypted and 0x02 != 0) mdxDecrypt(keyInfoRaw) else keyInfoRaw
            decompressBlock(decrypted, keyInfoDecompSize.toInt())
        } else {
            keyInfoRaw
        }

        // 解析键索引，得到每个键块的压缩/解压大小，
        val keyBlockSizes = IntArray(numKeyBlocks)
        val keyBlockDecompSizes = IntArray(numKeyBlocks)
        run {
            var p = 0
            val headW = if (v2) 2 else 1           // 首/尾键长字段宽度，
            val textTerm = if (v2) 1 else 0        // 2.0 版首/尾键文本含 1 个终止单元，
            val utf16 = termWidth == 2
            for (i in 0 until numKeyBlocks) {
                p += numW // entries-in-block count, unused
                val firstSize = readSizeField(keyIndex, p, headW); p += headW
                p += textUnitBytes(firstSize + textTerm, utf16)
                val lastSize = readSizeField(keyIndex, p, headW); p += headW
                p += textUnitBytes(lastSize + textTerm, utf16)
                keyBlockSizes[i] = readNumberBE(keyIndex, p, numW).toInt(); p += numW
                keyBlockDecompSizes[i] = readNumberBE(keyIndex, p, numW).toInt(); p += numW
            }
        }

        // 逐个键块解压 -> 键 => 偏移列表，
        val builder = HashMap<String, MutableList<Long>>(1 shl 16)
        for (i in 0 until numKeyBlocks) {
            val comp = ByteArray(keyBlockSizes[i])
            raf.readFully(comp)
            val block = decompressBlock(comp, keyBlockDecompSizes[i])
            parseKeyBlock(block, builder, numW)
        }
        index = HashMap(builder.size * 4 / 3 + 1)
        for ((k, v) in builder) index[k] = v.toLongArray()

        // ---- record section ----
        val numRecordBlocks = readNumber(v2).toInt()
        readNumber(v2) // numEntries, unused
        readNumber(v2) // record index length, unused
        readNumber(v2) // record blocks length, unused
        val blocks = ArrayList<RecordBlock>(numRecordBlocks)
        // 先读完记录索引（每块 comp/decomp 各 numW 字节），再定位到记录数据起点，
        val recordIndex = ByteArray(numRecordBlocks * numW * 2)
        raf.readFully(recordIndex)
        var compOffset = raf.filePointer
        var decompStart = 0L
        run {
            var p = 0
            for (i in 0 until numRecordBlocks) {
                val compSize = readNumberBE(recordIndex, p, numW).toInt(); p += numW
                val decompSize = readNumberBE(recordIndex, p, numW).toInt(); p += numW
                blocks.add(RecordBlock(compOffset, compSize, decompStart, decompSize))
                compOffset += compSize
                decompStart += decompSize
            }
        }
        recordBlocks = blocks.toTypedArray()
        logger.debug("mdx loaded: v{}, {} words, {} record blocks", version, index.size, recordBlocks.size)
    }

    /**
     * 精确查询一个键对应的全部原始记录文本（不解释内容，形如超级新华字典的
     * `` `1`词`2`拼音<br>释义 ``；其它词典可能是别的格式）。
     * 键不存在时返回空列表。查询线程安全由调用方保证（同一实例串行使用）。
     */
    fun lookup(word: String): List<String> {
        val offsets = index[word] ?: return emptyList()
        return offsets.map { readRecord(it) }
    }

    /** 键是否存在（精确匹配，不做任何拆分/归一）。 */
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
        val end = findTerminator(block, local)
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

    private fun parseKeyBlock(block: ByteArray, out: HashMap<String, MutableList<Long>>, offsetWidth: Int) {
        var p = 0
        while (p < block.size) {
            val offset = readNumberBE(block, p, offsetWidth); p += offsetWidth
            val end = findTerminator(block, p)
            val key = String(block, p, end - p, charset)
            out.getOrPut(key) { ArrayList(1) }.add(offset)
            p = end + termWidth
        }
    }

    /** 从 [from] 起找到下一个键/记录终止符（UTF-16 为 2 字节 `00 00`，否则单字节 `00`）的起点。 */
    private fun findTerminator(block: ByteArray, from: Int): Int {
        var end = from
        if (termWidth == 2) {
            while (end + 1 < block.size && !(block[end].toInt() == 0 && block[end + 1].toInt() == 0)) end += 2
        } else {
            while (end < block.size && block[end].toInt() != 0) end++
        }
        return end
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

    /**
     * 解密 MDX 2.0 的加密键索引块（`Encrypted=2`）：密钥 = RIPEMD-128(comp[4:8] + 95 36 00 00)，
     * 前 8 字节（类型 + adler32）原样保留，其余用 MDX 特有的 fast-decrypt 就地还原。
     */
    private fun mdxDecrypt(comp: ByteArray): ByteArray {
        val salt = byteArrayOf(comp[4], comp[5], comp[6], comp[7], 0x95.toByte(), 0x36, 0x00, 0x00)
        val key = Ripemd128.hash(salt)
        val out = comp.copyOf()
        var previous = 0x36
        for (i in 8 until out.size) {
            val b = out[i].toInt() and 0xFF
            var t = ((b ushr 4) or (b shl 4)) and 0xFF
            val ki = (i - 8) % key.size
            t = t xor previous xor ((i - 8) and 0xFF) xor (key[ki].toInt() and 0xFF)
            previous = b
            out[i] = t.toByte()
        }
        return out
    }

    /** 从当前文件位置读取一个长度/偏移数：2.0 版 8 字节、1.2 版 4 字节，均大端。 */
    private fun readNumber(v2: Boolean): Long = if (v2) readLongBE() else readIntBE().toLong() and 0xFFFFFFFFL

    private fun readIntBE(): Int {
        val a = raf.read(); val b = raf.read(); val c = raf.read(); val d = raf.read()
        if ((a or b or c or d) < 0) throw EOFException()
        return (a shl 24) or (b shl 16) or (c shl 8) or d
    }

    private fun readLongBE(): Long {
        var r = 0L
        repeat(8) {
            val x = raf.read()
            if (x < 0) throw EOFException()
            r = (r shl 8) or x.toLong()
        }
        return r
    }

    override fun close() {
        raf.close()
    }

    private companion object {
        private const val RECORD_BLOCK_CACHE_SIZE = 16
        private val ATTR_ENCODING = Regex("""Encoding="([^"]*)"""")
        private val ATTR_VERSION = Regex("""RequiredEngineVersion="([^"]*)"""")
        private val ATTR_ENCRYPTED = Regex("""Encrypted="([^"]*)"""")
        private val lzo = LzoLibrary.getInstance().newDecompressor(LzoAlgorithm.LZO1X, null)

        /** UTF-16 每个文本单元占 2 字节，否则 1 字节。 */
        private fun textUnitBytes(units: Int, utf16: Boolean): Int = if (utf16) units * 2 else units

        private fun readSizeField(b: ByteArray, off: Int, width: Int): Int =
            if (width == 2) ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)
            else b[off].toInt() and 0xFF

        /** 从字节数组按大端读一个 [width] 字节（4 或 8）的无符号数。 */
        private fun readNumberBE(b: ByteArray, off: Int, width: Int): Long {
            var r = 0L
            for (i in 0 until width) r = (r shl 8) or (b[off + i].toLong() and 0xFF)
            return r
        }
    }
}
