package cc.aoeiuv020.panovel.bookfile

interface LocalNovelExporter {
    fun export(info: LocalNovelInfo, contentProvider: ContentProvider, progressCallback: (Int, Int) -> Unit)
}