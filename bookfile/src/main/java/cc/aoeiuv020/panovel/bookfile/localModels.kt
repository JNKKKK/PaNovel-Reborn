package cc.aoeiuv020.panovel.bookfile

import io.documentnode.epub4j.epub.EpubReader
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

data class LocalNovelInfo(
        val author: String?,
        val name: String?,
        /**
         * 小说封面，
         */
        val image: String?,
        val introduction: String?,
        /**
         * 章节列表不能null, 解析前可以给个空LinkedList，
         */
        val chapters: List<LocalNovelChapter>,
        val requester: String?
)

/**
 * 本地小说的章节，
 */
data class LocalNovelChapter(
        /**
         * 章节名不包括小说名，
         */
        val name: String,
        val extra: String
)

/**
 * 本地小说的类型，
 *
 * 每种类型自带解析/导出/猜编码的工厂，新增格式只要在这里加一个枚举项，
 * 不用再去各调用点补 `when (type)` 分支（见 [Previewer]、LocalNovelProvider、NovelExporter）。
 */
enum class LocalNovelType(
        val suffix: String,
        /**
         * 用于Storage Access Framework新建文档时的MIME类型，
         */
        val mime: String,
        /**
         * 解析器工厂：从文件和编码构造对应的 [LocalNovelParser]，
         */
        val newParser: (File, Charset) -> LocalNovelParser,
        /**
         * 导出器工厂：EPUB 忽略编码，
         */
        val newExporter: (OutputStream, Charset) -> LocalNovelExporter,
        /**
         * 猜编码时要嗅探的输入流：纯文本直接读文件，EPUB 读其 opf 资源，
         */
        val openCharsetSource: (File) -> InputStream,
) {
    TEXT(
            suffix = ".txt",
            mime = "text/plain",
            newParser = { file, charset -> TextParser(file, charset) },
            newExporter = { out, charset -> TextExporter(out, charset) },
            openCharsetSource = { file -> file.inputStream() },
    ),
    EPUB(
            suffix = ".epub",
            mime = "application/epub+zip",
            newParser = { file, charset -> EpubParser(file, charset) },
            newExporter = { out, _ -> EpubExporter(out) },
            openCharsetSource = { file ->
                file.inputStream().use { EpubReader().readEpub(it) }.opfResource.inputStream
            },
    );

    companion object {
        /** 按文件名/URI 中出现的后缀匹配类型，匹配不到返回 null， */
        fun fromSuffix(nameOrUri: String): LocalNovelType? =
                values().firstOrNull { nameOrUri.contains(it.suffix) }
    }
}
