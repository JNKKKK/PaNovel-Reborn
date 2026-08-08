package cc.aoeiuv020.mdict

/**
 * 一条词典义项，**与具体词典无关**的通用数据模型：词头、拼音（可能为空）、
 * 以及释义 HTML 片段（`<br>` + 文本，交给 UI 用 Html.fromHtml 渲染）。
 *
 * 如何从某词典的原始记录解析出 [DictEntry] 是词典特有的，放在各自的 [Dictionary]
 * 实现里（超级新华字典的记录格式见 [XinhuaDictionary]），本类不含任何解析逻辑。
 */
data class DictEntry(
    val headword: String,
    val pinyin: String?,
    val definitionHtml: String,
)
