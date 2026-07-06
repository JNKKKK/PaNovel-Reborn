package cc.aoeiuv020.panovel.bookfile

import cc.aoeiuv020.shared.util.toURL
import java.io.InputStream
import java.net.URL

interface ContentProvider {
    fun getNovelContent(chapter: LocalNovelChapter): List<String>

    // 封面默认存完整url, 但是epub要存包内相对路径，否则针对临时文件解析的图片不可用，
    fun getImage(extra: String): URL = toURL(extra)

    // 从url中拿输入流，可以继承为后判断是否要提供这张图片，比如导出时没缓存的图片就不导出了，
    fun openImage(url: URL): InputStream? = url.openStream()
}