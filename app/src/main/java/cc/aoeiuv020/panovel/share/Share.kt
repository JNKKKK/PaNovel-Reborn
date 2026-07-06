package cc.aoeiuv020.panovel.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import cc.aoeiuv020.shared.json.AppJson
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.entity.BookList
import cc.aoeiuv020.panovel.data.entity.NovelMinimal
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.serialization.encodeToString

object Share {

    fun isShareContent(text: String): Boolean = ShareCodec.isShareContent(text)

    fun encode(bookList: BookList): String {
        val novelList = DataManager.getNovelMinimalFromBookList(bookList.nId)
        val shared = SharedBookList(bookList.name, novelList, bookList.uuid)
        return ShareCodec.encode(shared)
    }

    fun decode(text: String): SharedBookList = ShareCodec.decode(text)

    fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        if (!ShareCodec.fitsInQrCode(content)) return null
        return BarcodeEncoder().encodeBitmap(content, BarcodeFormat.QR_CODE, size, size)
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("PaNovel", text))
    }

    fun getClipboardText(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    }

    fun importShareContent(text: String): Int {
        val shared = ShareCodec.decode(text)
        DataManager.importBookList(shared.name, shared.list, shared.uuid)
        return shared.list.size
    }

    fun exportBookListJson(bookList: BookList, novelList: List<NovelMinimal>): String {
        val shared = SharedBookList(bookList.name, novelList, bookList.uuid)
        return AppJson.encodeToString(shared)
    }

    fun importBookListJson(text: String): SharedBookList {
        return AppJson.decodeFromString<SharedBookList>(text)
    }
}
