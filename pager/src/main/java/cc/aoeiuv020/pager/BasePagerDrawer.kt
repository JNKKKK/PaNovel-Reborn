package cc.aoeiuv020.pager

abstract class BasePagerDrawer : PagerDrawer {
    var pager: Pager? = null
    protected lateinit var backgroundSize: Size
    protected lateinit var contentSize: Size

    override fun attach(pager: Pager, backgroundSize: Size, contentSize: Size) {
        this.pager = pager
        this.backgroundSize = backgroundSize
        this.contentSize = contentSize
    }

    override fun detach() {
        this.pager = null
    }
}