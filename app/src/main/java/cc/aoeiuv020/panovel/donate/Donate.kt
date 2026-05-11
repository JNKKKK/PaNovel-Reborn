package cc.aoeiuv020.panovel.donate

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.util.notNullOrReport
import cc.aoeiuv020.panovel.util.safelyShow


/**
 *
 * Created by AoEiuV020 on 2017.11.25-12:55:16.
 */
sealed class Donate {
    companion object {
        val paypal = Paypal()
        val alipay = Alipay()
        val weChatPay = WeChatPay()
    }

    abstract fun pay(context: Context)

    class Paypal : Donate() {
        companion object {
            private val name = "AoEiuV020"
        }

        override fun pay(context: Context) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/$name")))
        }
    }

    /**
     * https://github.com/didikee/AndroidDonate/blob/master/donate/src/main/java/android/didikee/donate/AlipayDonate.java
     */
    class Alipay : Donate() {
        companion object {
            private val payCode = "FKX01135QSJ7NYBPR0PK01"
            private val scanUri = "alipayqr://platformapi/startapp?saId=10000007"
            private val redCode = "685703214"
        }

        override fun pay(context: Context) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://QR.ALIPAY.COM/$payCode")))
        }

        fun open(context: Context) {
            try {
                val packageManager = context.packageManager
                val intent = packageManager.getLaunchIntentForPackage("com.eg.android.AlipayGphone")
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "你好像没有安装支付宝", Toast.LENGTH_SHORT).show()
            }
        }

        @SuppressLint("SetTextI18n")
        fun red(context: Context) {
            AlertDialog.Builder(context)
                .setMessage("打开支付宝首页搜“$redCode”领红包，\n或者截图到支付宝扫码")
                .setPositiveButton("支付宝") { _, _ ->
                    open(context)
                }
                .setNegativeButton(R.string.copy) { _, _ ->
                    val cm: ClipboardManager = getSystemService(context, ClipboardManager::class.java).notNullOrReport()
                    cm.setPrimaryClip(ClipData.newPlainText("alipayRedCode", redCode))
                    open(context)
                }
                .create().safelyShow()
        }
    }

    /**
     * https://github.com/didikee/AndroidDonate/blob/master/donate/src/main/java/android/didikee/donate/WeiXinDonate.java
     */
    class WeChatPay : Donate() {
        companion object {
            private val qrcodeId = R.mipmap.qrcode_wechatpay
            private val TENCENT_PACKAGE_NAME = "com.tencent.mm"
            private val TENCENT_ACTIVITY_BIZSHORTCUT = "com.tencent.mm.action.BIZSHORTCUT"
            private val TENCENT_EXTRA_ACTIVITY_BIZSHORTCUT = "LauncherUI.From.Scaner.Shortcut"
        }

        override fun pay(context: Context) {
            val ivQR = ImageView(context)
            ivQR.setImageResource(qrcodeId)
            AlertDialog.Builder(context)
                .setView(ivQR)
                .setPositiveButton(R.string.jump_to_we_chat) { _, _ ->
                    val intent = Intent(TENCENT_ACTIVITY_BIZSHORTCUT)
                    intent.`package` = TENCENT_PACKAGE_NAME
                    intent.putExtra(TENCENT_EXTRA_ACTIVITY_BIZSHORTCUT, true)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "你好像没有安装微信", Toast.LENGTH_SHORT).show()
                    }
                }
                .create().safelyShow()
        }
    }

}