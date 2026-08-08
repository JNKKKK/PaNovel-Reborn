package cc.aoeiuv020.mdict

import java.io.File

/**
 * 超级新华字典（`超级新华字典.mdx`）的 [Dictionary] 实现。
 *
 * **本文件是超级新华字典专属的**：这里集中了该词典（源自「KDIC 超级新华字典」，GBK/Html，
 * MDict 1.2）一切键名与正文的特有约定。通用的 MDX 文件读取见 [MdxDictionary]，
 * 通用义项模型见 [DictEntry]。将来引入别的内置词典时，照此再写一个 [Dictionary] 实现，
 * 不要把新词典的怪癖塞进通用类里。
 *
 * 该词典已实测确认的三种怪癖（都在本类里处理）：
 * 1. 记录正文格式为 `` `1`词头`2`拼音<br>释义…… ``；少数字（如「几」）在 `2` 之后
 *    不是拼音而是结构化正文，此时拼音置空、该段留在释义里。
 * 2. 多音/多义字被拆成数字后缀键：有的只有 `的1/的2/的3`（无裸键），有的是
 *    `义 + 义1/义2`（裸键+后缀）。义项 = 裸键记录 + 连续存在的 词1、词2…。
 * 3. 异形词/儿化写法用半角/全角逗号或分号合并进一个键：`蹬腿,蹬腿儿`、`堤岸，堤坝`、
 *    `料头；料头儿`，其拼音也同样逗号连写。查任一变体都应命中同一条记录。
 *    （顿号「、」不算变体分隔，多为成语/并列内容，避免误拆。）
 */
class XinhuaDictionary(file: File) : Dictionary {
    private val mdx = MdxDictionary(file)

    /** 变体别名（如「蹬腿」）→ 其合并键（「蹬腿,蹬腿儿」）；仅在无同名真实键时登记。 */
    private val aliasIndex: Map<String, String> = buildAliasIndex()

    private fun buildAliasIndex(): Map<String, String> {
        val alias = HashMap<String, String>()
        for (key in mdx.keys) {
            if (!VARIANT_SEPARATOR.containsMatchIn(key)) continue
            for (part in key.split(VARIANT_SEPARATOR)) {
                val variant = part.trim()
                if (variant.isEmpty() || variant == key) continue
                if (mdx.contains(variant)) continue // 真实键优先，
                alias.putIfAbsent(variant, key)       // 多个合并键落到同一变体时保留先见到的，
            }
        }
        return alias
    }

    override fun contains(word: String): Boolean =
        mdx.contains(word) || aliasIndex.containsKey(word) ||
            // 只有数字后缀键的多音字（如「的」无裸键，但有「的1」），
            mdx.contains(word + 1)

    override fun senses(word: String): List<DictEntry> {
        val entries = ArrayList<DictEntry>()
        // 1) 裸键（可能 0 条：的；可能多条：唵），
        rawRecordsOf(word).forEach { entries.add(parseRecord(it, word)) }
        // 2) 连续的数字后缀键 词1、词2…，遇到第一个缺失即停止；词头统一显示为无后缀的 word,
        var n = 1
        while (true) {
            val raw = mdx.lookup(word + n)
            if (raw.isEmpty()) break
            raw.forEach { entries.add(parseRecord(it, word).copy(headword = word)) }
            n++
        }
        return entries
    }

    /** 取某词的裸键记录：先查真实键，没有再走变体别名（异形词合并键）。 */
    private fun rawRecordsOf(word: String): List<String> {
        mdx.lookup(word).let { if (it.isNotEmpty()) return it }
        val mergedKey = aliasIndex[word] ?: return emptyList()
        return mdx.lookup(mergedKey)
    }

    override fun close() = mdx.close()

    private companion object {
        // 记录形如： `1`瞬间`2`shùnjiān<br>[in a twinkling...] 一眨眼的工夫...
        private val RECORD = Regex("""^`1`([\s\S]*?)`2`([\s\S]*)$""")
        private val FIRST_BR = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
        // 拼音字符集：拉丁字母、带声调的元音、ü/ê、空格与隔音号，以及异形词合并时的逗号/分号
        // （如「dēngtuǐ，dēngtuǐr」「dī àn，dībà」），不含数字/括号/汉字，
        private val PINYIN = Regex("""^[a-zA-ZüÜêÊÀ-ɏḀ-ỿ·'\-,，;； ]+$""")
        // 合并键里的变体分隔符：半角/全角逗号与分号。顿号、不算。
        private val VARIANT_SEPARATOR = Regex("[,，;；]")

        /**
         * 把一条原始记录解析成 [DictEntry]。无法识别 `` `1``/`` `2`` 标记时，
         * 整段作为释义、词头用传入的 [fallbackWord]。
         */
        fun parseRecord(raw: String, fallbackWord: String): DictEntry {
            val m = RECORD.find(raw)
                ?: return DictEntry(fallbackWord, null, raw.trim())
            val headword = m.groupValues[1].trim()
            val body = m.groupValues[2]
            val brIndex = FIRST_BR.find(body)
            if (brIndex == null) {
                // 整段没有 <br>，无法安全切出拼音，全部当释义，
                val candidate = body.trim()
                return if (isPinyin(candidate)) DictEntry(headword, candidate, "")
                else DictEntry(headword, null, candidate)
            }
            val head = body.substring(0, brIndex.range.first).trim()
            val rest = body.substring(brIndex.range.last + 1).trimStart()
            return if (isPinyin(head)) {
                DictEntry(headword, head, rest)
            } else {
                // 首段不是拼音（如「几」的「(1)」），保留原正文（含首段），拼音置空，
                DictEntry(headword, null, body.trim())
            }
        }

        private fun isPinyin(s: String): Boolean =
            s.isNotEmpty() && s.length <= 40 && PINYIN.matches(s)
    }
}
