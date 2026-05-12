package cc.aoeiuv020.panovel.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import cc.aoeiuv020.panovel.BuildConfig
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.util.*
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import timber.log.Timber

object Check : Pref {
    override val name: String
        get() = "Check"
    private var knownVersionName: String by Delegates.string("0")

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private data class ReleaseInfo(val versionName: String, val body: String)

    private fun getReleasesNewerThan(currentVersion: String): List<ReleaseInfo> {
        val client = OkHttpClient()
        val request = Request.Builder().url(RELEASES_GITHUB_API).build()
        return client.newCall(request).execute().use { response ->
            val json = response.body?.string() ?: "[]"
            val array = JsonParser.parseString(json).asJsonArray
            array.mapNotNull { element ->
                val obj = element.asJsonObject
                val versionName = obj.get("tag_name")?.asString?.removePrefix("v") ?: return@mapNotNull null
                val body = obj.get("body")?.asString ?: ""
                ReleaseInfo(versionName, body)
            }.filter { VersionUtil.compare(it.versionName, currentVersion) > 0 }
        }
    }

    fun asyncCheckVersion(ctx: Context, tip: Boolean = false) {
        scope.launch {
            try {
                data class CheckResult(val newestVersionName: String, val hasUpdate: Boolean, val changeLog: String?)
                val result = withContext(Dispatchers.IO) {
                    val currentVersionName = VersionUtil.getAppVersionName(ctx)
                    val releases = getReleasesNewerThan(currentVersionName)
                    val newest = releases.firstOrNull()
                    Timber.i("checkVersion $currentVersionName/${newest?.versionName ?: "none"}")
                    if (newest == null || VersionUtil.compare(newest.versionName, knownVersionName) <= 0) {
                        CheckResult("0", false, null)
                    } else {
                        val changeLog = releases.joinToString("\n\n") { "${it.versionName}:\n${it.body}" }
                        CheckResult(newest.versionName, true, changeLog)
                    }
                }
                if (result.changeLog == null) {
                    if (tip) {
                        android.widget.Toast.makeText(ctx, ctx.getString(R.string.tip_no_new_version), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                androidx.appcompat.app.AlertDialog.Builder(ctx)
                    .setTitle("有更新 ${result.newestVersionName}")
                    .setMessage(result.changeLog)
                    .setNeutralButton("忽略") { _, _ ->
                        knownVersionName = result.newestVersionName
                    }
                    .setPositiveButton("Github") { _, _ ->
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_GITHUB)))
                    }
                    .create().safelyShow()
            } catch (e: Exception) {
                val message = "检测更新失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                if (tip) {
                    android.widget.Toast.makeText(ctx, ctx.getString(R.string.tip_update_failed), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private const val RELEASE_GITHUB = "https://github.com/JNKKKK/PaNovel/releases"
    private const val RELEASES_GITHUB_API =
        "https://api.github.com/repos/JNKKKK/PaNovel/releases"
    private var ignoreSignatureCheck: Boolean by Delegates.boolean(false)
    private var signature: String by Delegates.string("")

    /**
     * @return 忽略或者通过都返回true,
     */
    private fun checkSignature(ctx: Context): Boolean {
        Timber.i("checkSignature " + BuildConfig.SIGNATURE)
        if (TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - ctx.packageManager.getPackageInfo(
                    ctx.packageName,
                    0
                ).firstInstallTime
            ) < 7
        ) {
            // 7天内不检查签名，
            return true
        }
        @Suppress("SENSELESS_COMPARISON")
        if (BuildConfig.SIGNATURE == null) {
            return true
        }
        if (ignoreSignatureCheck) {
            return true
        }
        val apkSign = signature.takeIf(String::isNotEmpty)
            ?: SignatureUtil.getAppSignature(ctx).also { signature = it }
        Timber.i("apkSign = $apkSign")
        return BuildConfig.SIGNATURE.equals(apkSign, ignoreCase = true)
    }

    fun asyncCheckSignature(ctx: Context) {
        scope.launch {
            try {
                val passed = withContext(Dispatchers.IO) { checkSignature(ctx) }
                if (passed) {
                    return@launch
                }
                androidx.appcompat.app.AlertDialog.Builder(ctx)
                    .setTitle("签名不正确")
                    .setMessage("你可能用了假app,")
                    .setNeutralButton("忽略") { _, _ ->
                        ignoreSignatureCheck = true
                    }
                    .setPositiveButton("Github") { _, _ ->
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_GITHUB)))
                    }
                    .create().safelyShow()
            } catch (e: Exception) {
                val message = "检查签名出错"
                Timber.e(e, message)
                Reporter.post(message, e)
            }
        }
    }
}
