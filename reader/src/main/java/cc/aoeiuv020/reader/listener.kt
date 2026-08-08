package cc.aoeiuv020.reader

import java.io.File

interface TextRequester {
    fun requestChapter(index: Int, refresh: Boolean = false): List<String>
    // 小说章节正文由一个个段落构成，一个段落可能是一行文字，也可能是一张图片，
    // 支持图片就判断一下，否则直接toString,
    fun requestParagraph(string: String): Any

    // 请求图片，回调图片文件，
    fun requestImage(
            image: Image,
            exceptionHandler: (Throwable) -> Unit = {
                it.printStackTrace()
            },
            block: (File) -> Unit
    )
}

/**
 * 阅读进度监听器，
 * 进度改变时调用，
 */
interface ReadingListener {
    fun onReading(chapter: Int, text: Int)
}

// 这东西有点多余，换成点中心的回调就可以了，
interface MenuListener {
    fun hide()
    fun show()
    fun toggle()
}

/**
 * 长按正文取词监听器，
 * @param lookahead 长按位置起、同段落内的一小段文字，供上层做贪婪匹配查词，
 */
interface CharacterLongPressListener {
    fun onCharacterLongPress(lookahead: String)
}

