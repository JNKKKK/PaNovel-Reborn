package cc.aoeiuv020.mdict

import java.io.Closeable

/**
 * 一部词典的通用抽象 —— 屏蔽具体 MDX 的键名/正文约定，只暴露「查一个词、拿到它的义项」。
 *
 * 上层（长按取词的贪婪匹配、UI）只依赖这个接口，因此将来引入新的内置词典、或在设置里
 * 让用户切换不同内置 MDX，只需再写一个 [Dictionary] 实现即可，上层无需改动。
 *
 * 现有实现：[XinhuaDictionary]（超级新华字典.mdx）。
 */
interface Dictionary : Closeable {
    /**
     * 查询 [word] 的全部义项（多音字/多义项/异形词已由实现方合并、归一）。
     * 词不存在时返回空列表。[word] 应由调用方先做好繁→简等归一化。
     */
    fun senses(word: String): List<DictEntry>

    /** [word] 是否收录（用于长按取词时判断是否继续贪婪扩展）。 */
    fun contains(word: String): Boolean
}
