package cc.aoeiuv020.panovel.share

import android.content.Context
import android.view.View
import cc.aoeiuv020.panovel.App
import com.google.gson.reflect.TypeToken
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.BookList
import cc.aoeiuv020.panovel.data.entity.NovelMinimal
import cc.aoeiuv020.panovel.util.safelyShow
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.util.*

/**
 *
 * Created by AoEiuV020 on 2018.03.07-19:14:09.
 */
object Share {
    private val paste = PasteUbuntu()
    /**
     * 对不同版本的分享结果进行支持，
     * 当前第二版，
     * 第一版的没有版本号，
     */
    private const val VERSION: Int = 3

    fun check(url: String): Boolean {
        return paste.check(url)
    }

    fun shareBookList(bookList: BookList, shareExpiration: Expiration): String {
        val novelList = DataManager.getNovelMinimalFromBookList(bookList.nId)
        return paste.upload(PasteUbuntu.PasteUbuntuData(exportBookList(bookList, novelList), expiration = shareExpiration))
    }

    fun exportBookList(bookList: BookList, novelList: List<NovelMinimal>): String {
        val bookListBean = BookListBean(bookList.name, novelList, VERSION, bookList.uuid)
        return App.gson.toJson(bookListBean)
    }

    fun importBookList(text: String): BookListBean {
        val bookListJson = App.gson.fromJson(text, JsonObject::class.java)
        val version = bookListJson.get("version")?.asJsonPrimitive?.asInt
        return when (version) {
            3 -> {
                App.gson.fromJson(bookListJson, object : TypeToken<BookListBean>() {}.type)
            }
            2 -> {
                App.gson.fromJson<BookListBean2>(bookListJson, object : TypeToken<BookListBean2>() {}.type).let {
                    BookListBean(it.name, it.list, it.version, UUID.randomUUID().toString())
                }
            }
            1 -> {
                // 旧版version为null,
                val bookListBean1: BookListBean1 = App.gson.fromJson(bookListJson, object : TypeToken<BookListBean1>() {}.type)
                BookListBean(bookListBean1.name, bookListBean1.list.map {
                    // 旧版的extra为完整地址，直接拿来，就算写进数据库了，刷新详情页后也会被新版的bookId覆盖，
                    NovelMinimal(site = it.site, author = it.author, name = it.name, detail = it.requester.extra)
                }, VERSION, UUID.randomUUID().toString())
            }
            else -> throw IllegalStateException("APP版本太低")
        }
    }

    /**
     * @return 返回导入的书单中的小说数量，
     */
    fun receiveBookList(url: String): Int {
        val text = paste.download(url)
        val bookListBean = importBookList(text)
        DataManager.importBookList(bookListBean.name, bookListBean.list, bookListBean.uuid)
        return bookListBean.list.size
    }

    fun alert(context: Context, url: String, qrCode: String) {
        val layout = View.inflate(context, R.layout.dialog_shared, null)
        layout.findViewById<TextView>(R.id.tvUrl).apply {
            text = url
            setTextIsSelectable(true)
            setOnClickListener {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
        Glide.with(context.applicationContext).load(qrCode).into(layout.findViewById<ImageView>(R.id.ivQrCode))
        AlertDialog.Builder(context)
            .setTitle(R.string.share)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, null)
            .create().safelyShow()
    }
}