package cc.aoeiuv020.panovel.settings

/**
 * Created by AoEiuV020 on 2018.05.26-17:07:30.
 *
 * 列表外观以前是可配置的，现在固定使用默认值（线性大列表、置顶背景色、小红点等），
 * 默认常量在各使用处直接内联，见 cc.aoeiuv020.panovel.list 包。
 */

enum class ItemAction {
    OpenDetail, ReadLastChapter, ReadContinue,
    RefineSearch, Refresh, MoreAction,
    Export, RemoveBookshelf, AddBookshelf,
    Cache,
    // 置顶，
    Pinned,
    CancelPinned,
    // 删除缓存，删除所有相关数据，
    CleanCache,
    CleanData,
    // 什么都不做，
    None,
}