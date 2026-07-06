package cc.aoeiuv020.panovel.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.api.NovelContext
import cc.aoeiuv020.panovel.data.entity.*
import cc.aoeiuv020.panovel.localbook.ImportRequireValue
import cc.aoeiuv020.panovel.util.notNullOrReport
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber
import java.util.*

/**
 * 封装多个数据库的联用，
 * 隐藏api模块的数据类，app只使用这里的数据库实体，
 */
object DataManager {
    lateinit var app: AppDatabaseManager
        private set
    lateinit var api: ApiManager
        private set
    lateinit var cookie: CookieManager
        private set
    lateinit var cache: CacheManager
        private set
    @SuppressLint("StaticFieldLeak")
    lateinit var local: LocalManager
        private set
    @SuppressLint("StaticFieldLeak")
    lateinit var download: DownloadManager
        private set
    private lateinit var appContext: Context

    @Synchronized
    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        app = AppDatabaseManager(context)
        api = ApiManager(context)
        cookie = CookieManager(context)
        cache = CacheManager(context)
        local = LocalManager(context)
        download = DownloadManager(context)
    }

    fun listBookshelf(): List<NovelManager> = app.listBookshelf().map { it.toManager() }

    fun getNovelManager(id: Long): NovelManager =
            app.query(id).toManager()

    private fun Novel.toManager() = if (site.startsWith(".")) {
        NovelManager(this, app, local.getNovelProvider(this), cache, download.dnmLocal)
    } else {
        NovelManager(this, app, api.getNovelProvider(this), cache, download.dnmLocal)
    }

    fun allNovelContexts() = api.contexts

    /**
     * 列出所有网站，
     */
    fun listSites(): List<Site> = app.db.siteDao().list()

    /**
     * 同步所有网站到数据库，app升级时调用一次就好，
     */
    fun syncSites(): Unit = app.db.runInTransaction<Unit> {
        val existsSites = app.db.siteDao().listAllSite()
        existsSites.forEach { site ->
            val context = try {
                api.getNovelContextByName(site.name)
            } catch (e: Exception) {
                app.db.siteDao().removeSite(site)
                return@forEach
            }
            var dirty = false
            if (site.hide != context.hide) {
                site.hide = context.hide
                dirty = true
            }
            if (site.baseUrl != context.site.baseUrl) {
                site.baseUrl = context.site.baseUrl
                dirty = true
            }
            // 如果有变化，就更新该字段，
            if (dirty) {
                app.db.siteDao().updateSite(site)
            }
        }
        // 新增的网站加入网站表，
        val existsSiteNameSet = existsSites.map { it.name }.toSet()
        allNovelContexts().filter { it.site.name !in existsSiteNameSet }.forEach { context ->
            context.site.run {
                app.newSite(name, baseUrl, context.upkeep, context.hide)
            }
        }
    }

    /**
     * @param author 作者名为空就不从数据库查询，
     */
    fun search(site: String, name: String, author: String?): List<NovelManager> {
        if (author != null) {
            // 如果有作者名，那结果只可能有一个，
            // 如果数据库里有了，就直接返回，
            app.query(site, author, name)?.also {
                return listOf(it.toManager())
            }
        }
        val context = api.getNovelContextByName(site)
        val resultList = api.search(context, name)
        return app.db.runInTransaction<List<NovelManager>> {
            resultList.map {
                // 搜索结果查询数据库看是否有这本，有就取出，没有就新建一个插入数据库，
                app.queryOrNewNovel(NovelMinimal(it)).toManager()
            }
        }
    }

    fun getNovelContextByName(site: String) = api.getNovelContextByName(site)

    @MainThread
    fun pushCookiesToWebView(context: NovelContext) {
        // 高版本的设置cookie的回调乱七八糟的，用不上，
        context.cookies.values.forEach { okhttpCookie ->
            val cookieString = okhttpCookie.toString()
            Timber.d("push cookie: <$cookieString>")
            // webView传入cookie一次只能一条，取出一次所有，
            // cookieString只有一条cookie, 可能包含domain, path之类，分号;分隔，webView这个可以识别，
            cookie.putCookie(context.site.baseUrl, cookieString)
        }
    }

    @WorkerThread
    fun syncCookies(context: Context?) = cookie.sync(context)

    /**
     * 这里有NovelContext拿到cookies后的文件操作，
     * 但是WebView cookies操作只能在主线程，索性都放主线程，cookie量很小不费时，
     */
    @MainThread
    fun pullCookiesFromWebView(context: NovelContext) {
        val httpUrl = context.site.baseUrl.toHttpUrl()
        // webView传入cookie一次只能一条，取出一次所有，
        cookie.getCookies(context.site.baseUrl)?.split(";")?.mapNotNull { cookiePair ->
            Timber.d("pull cookie: <$cookiePair>")
            // 取出来的cookiePair只有name=value，Cookie.parse一定能通过，也因此可能有超时信息拿不出来的问题，
            Cookie.parse(httpUrl, cookiePair)?.let { cookie ->
                cookie.name to cookie
            }
        }?.let { cookiesList ->
            context.putCookies(cookiesList.toMap())
        }
    }

    fun getNovelFromUrl(site: String, url: String): NovelManager {
        return api.getNovelFromUrl(getNovelContextByName(site), url).let {
            // 搜索结果查询数据库看是否有这本，有就取出，没有就新建一个插入数据库，
            app.queryOrNewNovel(NovelMinimal(it)).toManager()
        }
    }

    fun query(site: String, author: String, name: String, detail: String): NovelManager {
        return app.queryOrNewNovel(NovelMinimal(site, author, name, detail)).toManager()
    }

    fun removeWebViewCookies() = cookie.removeCookies()

    fun removeNovelContextCookies(site: String) = api.removeCookies(getNovelContextByName(site))
    fun pinned(site: Site) {
        site.pinnedTime = Date()
        app.updatePinnedTime(site)
    }

    fun cancelPinned(site: Site) {
        site.pinnedTime = Date(0)
        app.updatePinnedTime(site)
    }

    fun updateReadStatus(novel: Novel) = app.updateReadStatus(novel)

    /**
     * @return 返回该小说已经缓存的章节列表，
     * 考虑到缓存可能对key进行了单向加密，
     * 只用于contains判断特定章节是否已经缓存，不用于读取章节信息，
     */
    fun novelContentsCached(novel: Novel): Collection<String> = cache.novelContentCached(novel)

    fun siteEnabledChange(site: Site) = app.siteEnabledChange(site)

    fun history(historyCount: Int): List<NovelManager> = app.history(historyCount).map { it.toManager() }

    fun getBookList(bookListId: Long): BookList = app.getBookList(bookListId)

    fun isEmpty(): Boolean = app.isEmpty()

    /**
     * 列表中的小说在该书单里的包含情况，
     */
    fun inBookList(bookListId: Long, list: List<NovelManager>) =
            // 多费一个map,
            app.inBookList(bookListId, list.map { it.novel })

    fun getNovelFromBookList(bookListId: Long): List<Novel> = app.getNovelFromBookList(bookListId)
    fun getNovelManagerFromBookList(bookListId: Long): List<NovelManager> =
            getNovelFromBookList(bookListId).map { it.toManager() }

    // 不包括本地小说，
    fun getNovelMinimalFromBookList(bookListId: Long): List<NovelMinimal> = app.getNovelMinimalFromBookList(bookListId)

    fun allBookList() = app.allBookList()
    fun renameBookList(bookList: BookList, name: String) = app.renameBookList(bookList, name)
    fun copyBookList(bookList: BookList, name: String) = app.copyBookList(bookList, name)
    fun removeBookList(bookList: BookList) = app.removeBookList(bookList)
    fun newBookList(name: String) = app.newBookList(name)

    /**
     * @throws IllegalArgumentException 不支持的地址直接抛异常，
     */
    fun getNovelFromUrl(url: String): Novel = api.getNovelFromUrl(api.getNovelContextByUrl(url), url).let {
        // 搜索结果查询数据库看是否有这本，有就取出，没有就新建一个插入数据库，
        app.queryOrNewNovel(NovelMinimal(it))
    }

    fun importBookList(name: String, list: List<NovelMinimal>, uuid: String = UUID.randomUUID().toString()) =
            app.importBookList(name, list, uuid)

    fun addToBookshelf(bookList: BookList) {
        app.addBookshelf(bookList)
    }

    fun removeFromBookshelf(bookList: BookList) {
        app.removeBookshelf(bookList)
    }

    fun cleanAllCache() {
        cache.cleanAll()
        api.cleanCache()
    }

    fun cleanBookshelf() = app.cleanBookshelf()

    fun cleanBookList() = app.cleanBookList()

    fun cleanHistory() = app.cleanHistory()

    fun removeAllCookies() {
        removeWebViewCookies()
        syncCookies(appContext)
        listSites().forEach {
            removeNovelContextCookies(it.name)
        }
    }

    /**
     * 返回书架上有更新的小说列表，
     * 按收到更新的时间倒序排列，
     */
    fun hasUpdateNovelList(): List<Novel> = app.hasUpdateNovelList()

    /**
     * @param requestInput 有的需要让用户输入决定，比如编码，作者名，小说名，还有文件类型，
     */
    @WorkerThread
    fun importLocalNovel(context: Context, uri: Uri, requestInput: (ImportRequireValue, String) -> String?): Novel {
        val (novel, chapterList) = context.contentResolver.openInputStream(uri).notNullOrReport(uri.toString()).use { input ->
            local.importLocalNovel(input, uri.toString(), requestInput)
        }
        app.queryOrNewNovel(NovelMinimal(novel)).let { exists ->
            // 如果同bookId的小说已经存在，就覆盖全部字段，
            // 只需要保留一个id,
            novel.id = exists.id
            app.updateAll(novel)
        }
        // 导入时已经解析了一遍章节列表，缓存起来，不能白解析了，
        // 统一转换成api模块的章节格式再存，
        cache.saveChapters(novel, chapterList)
        Timber.d("importLocalNovel result: $novel")
        return novel
    }

    @WorkerThread
    fun downloadAll() {
        Timber.d("downloadAll called")
        download.downloadAll(listBookshelf())
    }

    // ---- 备份/恢复用的底层入口 ----

    /**
     * 备份用，列出书架上的小说原始实体，
     */
    fun listBookshelfNovels(): List<Novel> = app.listBookshelf()

    /**
     * 备份用，列出最近阅读的小说原始实体，
     * @param count 与历史页展示的数量一致，
     */
    fun listHistoryNovels(count: Int): List<Novel> = app.history(count)

    /**
     * 备份用，列出某书单中的小说原始实体，
     */
    fun listBookListNovels(bookListId: Long): List<Novel> = app.getNovelFromBookList(bookListId)

    /**
     * 备份用，读取小说已缓存的章节列表，没有就返回空，
     */
    fun loadCachedChapters(novel: Novel): List<NovelChapter>? = cache.loadChapters(novel)

    /**
     * 备份用，读取小说某章已缓存的正文，没有就返回空，
     */
    fun loadCachedContent(novel: Novel, extra: String): List<String>? = cache.loadContent(novel, extra)

    /**
     * 恢复用，查询或新建小说，返回带id的原始实体，
     */
    fun queryOrNewNovel(novelMinimal: NovelMinimal): Novel = app.queryOrNewNovel(novelMinimal)

    /**
     * 恢复用，网站是否仍在支持列表，
     */
    fun checkSiteSupport(novel: Novel): Boolean = app.checkSiteSupport(novel)

    /**
     * 恢复用，覆盖更新小说的所有字段，
     */
    fun updateNovelAll(novel: Novel) = app.updateAll(novel)

    /**
     * 恢复用，写入小说的章节列表缓存，
     */
    fun saveCachedChapters(novel: Novel, list: List<NovelChapter>) = cache.saveChapters(novel, list)

    /**
     * 恢复用，写入小说某章的正文缓存，
     */
    fun saveCachedContent(novel: Novel, extra: String, text: List<String>) =
            cache.saveContent(novel, extra, text)

}
