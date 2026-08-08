package cc.aoeiuv020.pager

/**
 * 一次命中测试的结果：命中的页标识 [pageTag]（对 pager 不透明，由绘制方 [PagerDrawer] 编解码）
 * 以及在该页「内容坐标系」（已减去留白、且相对该页左上角）下的坐标 [contentX]/[contentY]。
 *
 * 滚动模式下同屏可能有两页，[pageTag] 用于区分点到的是哪一页；分页模式下恒为当前页。
 */
data class PageHit(
    val pageTag: Long,
    val contentX: Float,
    val contentY: Float,
)
