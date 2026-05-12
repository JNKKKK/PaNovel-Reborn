package cc.aoeiuv020.panovel.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import cc.aoeiuv020.json.AppJson
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.BookList
import cc.aoeiuv020.panovel.data.entity.NovelMinimal
import cc.aoeiuv020.panovel.util.safelyShow
import com.bumptech.glide.Glide
import kotlinx.serialization.encodeToString

object Share {
    private val paste = PasteUbuntu()
    private const val VERSION: Int = 3

    fun check(url: String): Boolean {
        return paste.check(url)
    }

    fun shareBookList(bookList: BookList, shareExpiration: Expiration): String {
        val novelList = DataManager.getNovelMinimalFromBookList(bookList.nId)
        return paste.upload(PasteUbuntu.PasteUbuntuData(exportBookList(bookList, novelList), expiration = shareExpiration))
    }

    fun exportBookList(bookList: BookList, novelList: List<NovelMinimal>): String {
        val bookListBean = SharedBookList(bookList.name, novelList, VERSION, bookList.uuid)
        return AppJson.encodeToString(bookListBean)
    }

    fun importBookList(text: String): SharedBookList {
        return AppJson.decodeFromString<SharedBookList>(text)
    }

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
