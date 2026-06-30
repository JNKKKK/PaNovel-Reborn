package cc.aoeiuv020.panovel.backup

/**
 * 备份可选内容，
 * 书架/书单/历史选中后，对应的小说会连同章节列表和已缓存正文一起备份，
 *
 * Created by AoEiuV020 on 2018.05.11-19:32:55.
 */
enum class BackupOption {
    Bookshelf,
    BookList,
    History,
    Settings,
}
