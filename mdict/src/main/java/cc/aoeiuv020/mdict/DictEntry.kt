package cc.aoeiuv020.mdict

/**
 * 一条词典义项：词头、拼音（可能为空）、以及释义 HTML 片段（`<br>` + 文本）。
 */
data class DictEntry(
    val headword: String,
    val pinyin: String?,
    val definitionHtml: String,
) {
    companion object {
        // 原始记录形如： `1`瞬间`2`shùnjiān<br>[in a twinkling...] 一眨眼的工夫...
        // 少数条目在 `2` 之后不是拼音而是结构化正文（如 几 是 "(1)<br>幾<br>jī..."），
        // 这时拼音置空，该段文字仍留在释义里。
        private val RECORD = Regex("""^`1`([\s\S]*?)`2`([\s\S]*)$""")
        private val FIRST_BR = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
        // 拼音字符集：拉丁字母、带声调的元音、ü/ê、空格与隔音号，以及异形词合并时的逗号/分号
        // （如「dēngtuǐ，dēngtuǐr」「dī àn，dībà」），不含数字/括号/汉字，
        private val PINYIN = Regex("""^[a-zA-ZüÜêÊÀ-ɏḀ-ỿ·'\-,，;； ]+$""")

        /**
         * 把一条原始记录解析成 [DictEntry]。无法识别 `` `1``/`` `2`` 标记时，
         * 整段作为释义、词头用传入的 [fallbackWord]。
         */
        fun parse(raw: String, fallbackWord: String): DictEntry {
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
                // 首段不是拼音，保留原正文（含首段），拼音置空，
                DictEntry(headword, null, body.trim())
            }
        }

        private fun isPinyin(s: String): Boolean =
            s.isNotEmpty() && s.length <= 40 && PINYIN.matches(s)
    }
}
