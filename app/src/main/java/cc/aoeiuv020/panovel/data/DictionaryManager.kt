package cc.aoeiuv020.panovel.data

import android.content.Context
import cc.aoeiuv020.mdict.DictEntry
import cc.aoeiuv020.mdict.MdxDictionary
import cc.aoeiuv020.mdict.isHan
import cc.aoeiuv020.shared.util.ChineseNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * 长按取词的查询结果：一个或多个按长度递增排列的命中词，
 * 例如在「瞬间引来」上点「瞬」得到 [瞬, 瞬间]，默认展示最长的「瞬间」。
 */
data class DictResult(val words: List<DictWord>) {
    val isEmpty get() = words.isEmpty()
}

/** 一个命中词及其全部义项。 */
data class DictWord(val word: String, val entries: List<DictEntry>)

/**
 * 词典查询管理器，封装随 app 打包的 `超级新华字典.mdx`。
 *
 * 词典文件以 asset 形式打包，首次查询时复制到 filesDir 得到可随机访问的文件，
 * 再构造 [MdxDictionary]。全部 I/O 都在 [Dispatchers.IO] 上完成。
 */
class DictionaryManager(private val context: Context) {
    private val mutex = Mutex()
    @Volatile
    private var dictionary: MdxDictionary? = null

    private suspend fun getDictionary(): MdxDictionary {
        dictionary?.let { return it }
        return mutex.withLock {
            dictionary ?: withContext(Dispatchers.IO) {
                val file = ensureDictFile()
                MdxDictionary(file).also { dictionary = it }
            }
        }
    }

    /**
     * 把 asset 里的词典复制到 filesDir（首次或版本变化时），返回可随机访问的文件。
     *
     * 不用 assets.openFd 判断大小：该 asset 在 APK 里是压缩存储的，openFd 会抛
     * FileNotFoundException（不能作为文件描述符打开）。改用一个版本标记文件判断是否已复制，
     * 词典内容更新时提升 [ASSET_VERSION] 即可触发重新复制。
     */
    private fun ensureDictFile(): File {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val out = File(dir, ASSET_NAME)
        val marker = File(dir, "$ASSET_NAME.version")
        val already = out.exists() && marker.exists() && marker.readText().trim() == ASSET_VERSION
        if (already) return out
        Timber.d("copying dictionary asset to ${out.absolutePath}")
        context.assets.open("$ASSET_DIR/$ASSET_NAME").use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        marker.writeText(ASSET_VERSION)
        return out
    }

    /**
     * 从 [lookahead]（长按位置起始，含后续若干字符）贪婪匹配：
     * 依次尝试前 1、2、3… 个字符，命中就继续加长，遇到第一个不命中即停止。
     * 命中的词做繁→简归一化后查询。返回按长度递增排列的命中词及义项。
     */
    suspend fun lookupGreedy(lookahead: String): DictResult {
        if (lookahead.isEmpty() || !lookahead[0].isHan()) return DictResult(emptyList())
        val mdx = getDictionary()
        return withContext(Dispatchers.IO) {
            val words = ArrayList<DictWord>()
            val sb = StringBuilder()
            for (ch in lookahead) {
                // 一旦遇到非汉字（标点、空白、英文等）就不再扩展，
                if (!ch.isHan()) break
                sb.append(ch)
                val query = ChineseNormalizer.normalize(sb.toString())
                val entries = collectSenses(mdx, query)
                if (entries.isEmpty()) break
                words.add(DictWord(query, entries))
            }
            DictResult(words)
        }
    }

    /**
     * 收集一个词的全部义项。词典对多音/多义字有两种拆分方式，都要处理：
     * - 直接以 [word] 为键（可能有多条记录，如「唵」）；
     * - 把义项拆成带数字后缀的键 [word]1、[word]2 …（如「的1/的2/的3」「义/义1/义2」）。
     * 最终义项 = 裸键记录 + word1、word2… 依次连续存在的部分，词头统一显示为无后缀的 [word]。
     */
    private fun collectSenses(mdx: MdxDictionary, word: String): List<DictEntry> {
        val entries = ArrayList<DictEntry>()
        // 裸键（可能 0 条：的；可能多条：唵），
        mdx.lookup(word).forEach { entries.add(DictEntry.parse(it, word)) }
        // 连续的数字后缀键，遇到第一个缺失即停止，
        var n = 1
        while (true) {
            val raw = mdx.lookup(word + n)
            if (raw.isEmpty()) break
            // 词头统一用无后缀的 word（词典里是「的1」，展示成「的」），
            raw.forEach { entries.add(DictEntry.parse(it, word).copy(headword = word)) }
            n++
        }
        return entries
    }

    private companion object {
        const val DIR_NAME = "dictionary"
        const val ASSET_DIR = "dict"
        const val ASSET_NAME = "超级新华字典.mdx"
        // 词典内容更新时提升此版本号即可触发重新复制，
        const val ASSET_VERSION = "1"
    }
}
