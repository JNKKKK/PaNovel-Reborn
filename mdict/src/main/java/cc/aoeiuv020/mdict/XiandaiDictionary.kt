package cc.aoeiuv020.mdict

import java.io.File

/**
 * 现代汉语词典第 5 版（`现代汉语词典第5版.mdx`）的 [Dictionary] 实现。
 *
 * **本文件是现代汉语词典专属的**：这里集中了该词典（MDX 2.0、UTF-16、`Encrypted=2`、
 * Html 正文）一切键名与正文的特有约定。通用的 MDX 文件读取（含 2.0/加密）见 [MdxDictionary]，
 * 通用义项模型见 [DictEntry]。将来引入别的内置词典时，照此再写一个 [Dictionary] 实现，
 * 不要把新词典的怪癖塞进通用类里。
 *
 * 已实测确认的记录格式与怪癖（都在本类里处理）：
 * 1. 记录以词头字体打头：`` <font size=+2 color="navy">词头</font> ``，其中约 85% 的词头带
 *    书名号 `【…】`（如 `【蹬腿】`），解析时去掉书名号。极少数记录（<0.1%）没有这个字体
 *    包裹（多是同字多义靠 `<br>` 连排），此时词头回退成查询词、整段留在释义里。
 * 2. 词头之后依次是：可选的 `<sup>N </sup>`（多音序号）、可选的异体/繁体括注（如 `(幾)`、
 *    `（紅）`、`(阿、呵)`）、然后才是拼音。拼音用 IPA 风格字母（`ɑ ɡ ň`）、轻声点 `·`、
 *    插入号 `∥`、成语里的全角逗号（`yī bù zuò，èr bù xiū`）。异体括注提到释义最前面保留。
 * 3. 多音/多义字是**重复键**（不是超级新华那样的数字后缀），例如「的」有 5 条记录、
 *    「啊」有 5 条，各是一个读音/义项；[senses] 直接返回该键的全部记录、各自解析。
 *    没有异形词合并键。
 * 4. 正文里三处需要清洗（见 [cleanDefinition]）：例句字体标签混入全角空格
 *    （`<font　face>`）、语体标记写成伪标签（`<书>`/`<方>`/`<口>`，Html 会丢弃）、
 *    词性标记是硬编码灰底的嵌套 `<span>`（夜间与阅读页背景不搭）。
 */
class XiandaiDictionary(file: File) : Dictionary {
    private val mdx = MdxDictionary(file)

    override fun contains(word: String): Boolean = mdx.contains(word)

    override fun senses(word: String): List<DictEntry> =
        mdx.lookup(word).map { parseRecord(it, word) }

    override fun close() = mdx.close()

    private companion object {
        // 词头字体：<font size=+2 color="navy">【词头】</font>，
        private val HEADWORD = Regex("""^<font\b[^>]*color="navy"[^>]*>([\s\S]*?)</font>\s*""")
        // 多音序号上标，可能出现在词头后、异体括注前，
        private val SUP = Regex("""^<sup>[\s\S]*?</sup>\s*""", RegexOption.IGNORE_CASE)
        // 异体/繁体括注：半/全角括号里全是汉字或顿号（如 (幾)、（紅）、(阿、呵)），
        private val VARIANT = Regex("""^[（(][一-鿿、]+[）)]\s*""")
        // 拼音字符集：拉丁字母、带调元音（Latin-1/Ext-A/IPA 到 U+02AF）、轻声点 ·、插入号 ∥、
        // 隔音撇号、连字符、空格，以及成语拼音里的全角逗号。不含数字/汉字/括号/尖括号，
        private val PINYIN = Regex("""^[A-Za-z·À-ʯ’'∥， \-]+""")
        private val PINYIN_HAS_LETTER = Regex("""[A-Za-zÀ-ʯ]""")
        // 语体伪标签 <书>/<方>/<口> 等（尖括号里 1~3 个汉字），
        private val PSEUDO_TAG = Regex("""<([一-鿿]{1,3})>""")
        private val SPAN_TAG = Regex("""</?span[^>]*>""", RegexOption.IGNORE_CASE)
        private val MULTI_SPACE = Regex("""[ \t]{2,}""")

        /**
         * 把一条原始记录解析成 [DictEntry]。识别不出词头字体时（极少数），
         * 词头用传入的 [fallbackWord]、整段留作释义。
         */
        fun parseRecord(raw: String, fallbackWord: String): DictEntry {
            var headword = fallbackWord
            var body = raw
            HEADWORD.find(raw)?.let { m ->
                headword = m.groupValues[1].removeSurrounding("【", "】").trim().ifEmpty { fallbackWord }
                body = raw.substring(m.range.last + 1)
            }

            // 依次剥掉词头后的装饰（多音序号上标 / 异体括注），异体括注提到释义前保留，
            var variantPrefix = ""
            while (true) {
                body = body.trimStart()
                val sup = SUP.find(body)
                if (sup != null) {
                    body = body.substring(sup.range.last + 1)
                    continue
                }
                val variant = VARIANT.find(body)
                if (variant != null) {
                    variantPrefix += variant.value.trim() + " "
                    body = body.substring(variant.range.last + 1)
                    continue
                }
                break
            }

            var pinyin: String? = null
            val pm = PINYIN.find(body)
            if (pm != null) {
                val cand = pm.value.trim()
                if (cand.length in 1..60 && PINYIN_HAS_LETTER.containsMatchIn(cand)) {
                    pinyin = cand
                    body = body.substring(pm.range.last + 1)
                }
            }

            val definition = cleanDefinition(variantPrefix + body)
            return DictEntry(headword, pinyin, definition)
        }

        /** 清洗释义 HTML，使其在 `Html.fromHtml` 下渲染干净、且不带硬编码配色（见类头注释）。 */
        fun cleanDefinition(html: String): String {
            var s = html
            // 例句字体标签里混入全角空格（<font　face>），归一成半角让 Html 正确识别标签，
            s = s.replace("<font　face", "<font face")
            // 语体伪标签转成与正文里已有写法一致的〈…〉文本（否则 Html 当未知标签丢弃），
            s = PSEUDO_TAG.replace(s) { "〈${it.groupValues[1]}〉" }
            // 词性标记原是硬编码灰底的嵌套 <span>，去标签留文字（如 名/动/助），
            s = SPAN_TAG.replace(s, "")
            s = s.replace("\r", "")
            // 合并去 span 后残留的多余空白，
            s = MULTI_SPACE.replace(s, " ")
            return s.trim()
        }
    }
}
