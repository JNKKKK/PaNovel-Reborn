package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.api.NovelDetail
import cc.aoeiuv020.panovel.api.NovelItem
import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat

class Snapd : DslJsoupNovelContext() {
    private val apiHost = "https://www.bqg474.cc"
    private val json = Json { ignoreUnknownKeys = true }

    private fun fetchJson(path: String): String {
        return responseBody(connect("$apiHost$path")).string()
    }

    override fun findBookId(extra: String): String {
        val readIdRegex = Regex("/read/(\\d+)")
        val match = readIdRegex.find(extra) ?: return extra
        val readId = match.groupValues[1]
        val detailUrl = absUrl("/read/$readId/")
        val doc = parse(connect(detailUrl))
        val imgUrl = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: return readId
        return imgUrl.substringAfterLast("/").substringBefore(".")
    }

init {
    site {
        name = "笔趣阁snapd"
        baseUrl = "https://www.snapd.net"
    }
    hostList.add("bqg474.cc")

    search {
        val body = fetchJson("/api/search?q=${java.net.URLEncoder.encode(it, "UTF-8")}")
        val result = json.decodeFromString<SearchResult>(body)
        result.data.map { item ->
            NovelItem(
                site = site.name,
                name = item.title,
                author = item.author,
                extra = item.id
            )
        }
    }

    detail {
        val apiId = findBookId(extra)
        val body = fetchJson("/api/book?id=$apiId")
        val book = json.decodeFromString<BookDetail>(body)
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        NovelDetail(
            novel = NovelItem(
                site = site.name,
                name = book.title,
                author = book.author,
                extra = book.dirid
            ),
            image = "https://www.snapd.net/bookimg/${book.dirid.toInt() / 1000}/${book.dirid}.jpg",
            update = runCatching { sdf.parse(book.lastupdate) }.getOrNull(),
            introduction = book.intro.trim(),
            extra = book.dirid
        )
    }

    chapters {
        val apiId = findBookId(extra)
        val body = fetchJson("/api/booklist?id=$apiId")
        val result = json.decodeFromString<BookList>(body)
        result.list.mapIndexedNotNull { index, name ->
            runCatching {
                NovelChapter(
                    name = name,
                    extra = "$apiId/${index + 1}"
                )
            }.getOrNull()
        }
    }

    content {
        val parts = extra.split("/")
        val apiId = parts[0]
        val chapterId = parts[1]
        val body = fetchJson("/api/chapter?id=$apiId&chapterid=$chapterId")
        val chapter = json.decodeFromString<ChapterContent>(body)
        chapter.txt.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }
}}

@Serializable
private data class SearchResult(
    val data: List<SearchItem> = emptyList()
)

@Serializable
private data class SearchItem(
    val id: String,
    val title: String,
    val author: String
)

@Serializable
private data class BookDetail(
    val title: String,
    val author: String,
    val intro: String,
    val lastupdate: String = "",
    val dirid: String
)

@Serializable
private data class BookList(
    val list: List<String>
)

@Serializable
private data class ChapterContent(
    val txt: String
)
