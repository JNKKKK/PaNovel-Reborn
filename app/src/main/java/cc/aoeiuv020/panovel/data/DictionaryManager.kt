package cc.aoeiuv020.panovel.data

import android.content.Context
import cc.aoeiuv020.mdict.DictEntry
import cc.aoeiuv020.mdict.Dictionary
import cc.aoeiuv020.mdict.XinhuaDictionary
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
 * 一部内置词典的描述：asset 路径 + 构造方式。
 *
 * 新增内置词典时在 [BuiltinDictionary.ALL] 里加一项即可；将来在设置里做「切换词典」时，
 * [DictionaryManager] 只需按用户所选的 [BuiltinDictionary] 打开对应实现。asset 内容更新时
 * 提升 [version] 触发重新复制到 filesDir。
 *
 * @param open 把复制到本地的文件构造成通用的 [Dictionary]；不同词典有各自的实现
 *             （超级新华字典是 [XinhuaDictionary]）。
 */
enum class BuiltinDictionary(
    val assetName: String,
    val version: String,
    val open: (File) -> Dictionary,
) {
    XINHUA("超级新华字典.mdx", version = "1", open = ::XinhuaDictionary);

    companion object {
        /** 当前默认（且目前唯一）内置词典。将来设置项就从 [entries] 里选。 */
        val DEFAULT = XINHUA
        val ALL: List<BuiltinDictionary> get() = entries
    }
}

/**
 * 词典查询管理器：管理内置词典 asset 的落地与打开，并提供长按取词的贪婪匹配。
 *
 * 本类**与具体词典无关**——只依赖 [Dictionary] 抽象。词典文件以 asset 形式打包，首次查询时
 * 复制到 filesDir 得到可随机访问的文件，再由对应 [BuiltinDictionary] 打开。全部 I/O 在
 * [Dispatchers.IO] 上完成。将来支持用户切换词典时，把 [current] 换成可变、按设置选择即可。
 */
class DictionaryManager(private val context: Context) {
    private val mutex = Mutex()
    @Volatile
    private var dictionary: Dictionary? = null

    // 当前使用的内置词典；目前固定为默认词典，将来可由设置项决定，
    private val current: BuiltinDictionary = BuiltinDictionary.DEFAULT

    private suspend fun getDictionary(): Dictionary {
        dictionary?.let { return it }
        return mutex.withLock {
            dictionary ?: withContext(Dispatchers.IO) {
                val file = ensureDictFile(current)
                current.open(file).also { dictionary = it }
            }
        }
    }

    /**
     * 把 asset 里的词典复制到 filesDir（首次或版本变化时），返回可随机访问的文件。
     *
     * 不用 assets.openFd 判断大小：该 asset 在 APK 里是压缩存储的，openFd 会抛
     * FileNotFoundException（不能作为文件描述符打开）。改用一个版本标记文件判断是否已复制，
     * 词典内容更新时提升该 [BuiltinDictionary.version] 即可触发重新复制。
     */
    private fun ensureDictFile(dict: BuiltinDictionary): File {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val out = File(dir, dict.assetName)
        val marker = File(dir, "${dict.assetName}.version")
        val already = out.exists() && marker.exists() && marker.readText().trim() == dict.version
        if (already) return out
        Timber.d("copying dictionary asset to ${out.absolutePath}")
        context.assets.open("$ASSET_DIR/${dict.assetName}").use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        marker.writeText(dict.version)
        return out
    }

    /**
     * 从 [lookahead]（长按位置起始，含后续若干字符）贪婪匹配：
     * 依次尝试前 1、2、3… 个字符，命中就继续加长，遇到第一个不命中即停止。
     * 命中的词做繁→简归一化后查询。返回按长度递增排列的命中词及义项。
     *
     * 与具体词典无关：多音字/异形词等怪癖已在 [Dictionary] 实现内部消化。
     */
    suspend fun lookupGreedy(lookahead: String): DictResult {
        if (lookahead.isEmpty() || !lookahead[0].isHan()) return DictResult(emptyList())
        val dict = getDictionary()
        return withContext(Dispatchers.IO) {
            val words = ArrayList<DictWord>()
            val sb = StringBuilder()
            for (ch in lookahead) {
                // 一旦遇到非汉字（标点、空白、英文等）就不再扩展，
                if (!ch.isHan()) break
                sb.append(ch)
                val query = ChineseNormalizer.normalize(sb.toString())
                val entries = dict.senses(query)
                if (entries.isEmpty()) break
                words.add(DictWord(query, entries))
            }
            DictResult(words)
        }
    }

    private companion object {
        const val DIR_NAME = "dictionary"
        const val ASSET_DIR = "dict"
    }
}
