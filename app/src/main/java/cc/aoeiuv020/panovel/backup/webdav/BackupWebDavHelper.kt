package cc.aoeiuv020.panovel.backup.webdav

import android.app.Activity
import cc.aoeiuv020.panovel.backup.BackupHelper
import cc.aoeiuv020.panovel.util.Delegates
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber
import java.io.File


/**
 * Created by AoEiuV020 on 2021.04.25-12:35:51.
 */
class BackupWebDavHelper : BackupHelper {
    override val type: String
        get() = "WebDav"
    var server: String by Delegates.string("")
    var fileName: String by Delegates.string("PaNovel-Backup.zip")
    var username: String by Delegates.string("")
    var password: String by Delegates.string("")

    override fun ready(): Boolean {
        return server.takeIf { it.isNotBlank() }?.takeIf { runCatching { it.toHttpUrl() }.isSuccess } != null
                && fileName.takeIf { it.isNotBlank() } != null
                && username.takeIf { it.isNotBlank() } != null
                && password.takeIf { it.isNotBlank() } != null
    }

    override fun configPreview(): String {
        if (!ready()) {
            throw IllegalStateException("没配置好，")
        }
        return getUrl()
    }

    private fun getUrl(showPassword: Boolean = false): String {
        return server.toHttpUrl().newBuilder()
                .username(username)
                .password(if (showPassword) password else if (password.isEmpty()) "" else "***")
                .addPathSegment(fileName)
                .build()
                .toString()
    }

    override fun configActivity(): Class<out Activity> {
        return BackupWebDavConfigActivity::class.java
    }

    override fun restore(tempFile: File) = tryRun {
        Timber.i("import ${configPreview()}, file: $tempFile")
        val sardine: Sardine = initWebDav()
        sardine.get(getUrl(true)).use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
    }

    override fun backup(tempFile: File) = tryRun {
        Timber.i("export ${configPreview()}, file: $tempFile")
        tryRun {
            val sardine: Sardine = initWebDav()
            sardine.put(getUrl(true), tempFile, "application/zip")
        }
    }

    private fun initWebDav(): Sardine = tryRun {
        val sardine: Sardine = OkHttpSardine()
        sardine.setCredentials(username, password)
        if (!sardine.exists(server)) {
            //不存在目录即创建
            sardine.createDirectory(server);
        }

        sardine
    }

    fun test(server: String, username: String, password: String) = tryRun {
        val sardine: Sardine = OkHttpSardine()
        sardine.setCredentials(username, password)
        val exists = sardine.exists(server)
        Timber.i("$server ${if (exists) "exists" else "not exists"}")
    }

    private inline fun <T, R> T.tryRun(block: T.() -> R): R {
        try {
            return block()
        } catch (e: SardineException) {
            when (e.statusCode) {
                404 -> throw IllegalStateException("404 文件不存在", e)
                401 -> throw IllegalStateException("401 认证错误，用户名密码错误或者没有权限，也可能是服务器不支持Basic方式认证", e)
                else -> throw e
            }
        }
    }
}