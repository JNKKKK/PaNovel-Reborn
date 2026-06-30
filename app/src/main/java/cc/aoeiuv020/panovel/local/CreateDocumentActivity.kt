package cc.aoeiuv020.panovel.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 透明的桥接Activity，包装Storage Access Framework的新建文档(ACTION_CREATE_DOCUMENT)，
 * 让深处只持有Context的调用方（如列表项的导出动作）也能弹出系统"另存为"并拿到结果Uri,
 * 免存储权限，用户每次自行选择保存位置和文件名，
 *
 * 用法见[createDocument]挂起函数，风格参考已有的[cc.aoeiuv020.panovel.util.uiSelect],
 */
class CreateDocumentActivity : ComponentActivity() {
    // StartActivityForResult不在注册时读intent, 避免字段初始化早于intent赋值的坑，
    private val launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
    ) { result ->
        deliver(token, result.data?.data)
        finish()
    }

    private val token: String by lazy { intent.getStringExtra(EXTRA_TOKEN) ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val createIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = intent.getStringExtra(EXTRA_MIME) ?: "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, intent.getStringExtra(EXTRA_SUGGESTED_NAME) ?: "")
            }
            launcher.launch(createIntent)
        }
    }

    companion object {
        private const val EXTRA_MIME = "mime"
        private const val EXTRA_SUGGESTED_NAME = "suggestedName"
        private const val EXTRA_TOKEN = "token"

        // 桥接挂起函数与一次性Activity结果，token匹配避免并发串台，
        private val pending = mutableMapOf<String, (Uri?) -> Unit>()
        private var counter = 0

        private fun deliver(token: String, uri: Uri?) {
            pending.remove(token)?.invoke(uri)
        }

        /**
         * 弹出系统新建文档界面，返回用户选择的目标Uri, 取消则返回null,
         *
         * @param mime 文档MIME类型，决定系统建议的后缀，
         * @param suggestedName 预填的文件名，
         */
        suspend fun createDocument(context: Context, mime: String, suggestedName: String): Uri? =
                suspendCancellableCoroutine { cont ->
                    val token = "createDoc${counter++}"
                    pending[token] = { uri ->
                        if (cont.isActive) cont.resume(uri)
                    }
                    cont.invokeOnCancellation { pending.remove(token) }
                    context.startActivity(Intent(context, CreateDocumentActivity::class.java).apply {
                        if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(EXTRA_MIME, mime)
                        putExtra(EXTRA_SUGGESTED_NAME, suggestedName)
                        putExtra(EXTRA_TOKEN, token)
                    })
                }
    }
}
