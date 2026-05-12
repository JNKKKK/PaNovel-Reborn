package cc.aoeiuv020.panovel.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import cc.aoeiuv020.json.AppJson
import cc.aoeiuv020.panovel.BuildConfig
import kotlinx.serialization.json.*
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

    private fun getAllReleases(): List<ReleaseInfo> {
        val client = OkHttpClient()
        val request = Request.Builder().url(RELEASES_GITHUB_API).build()
        return client.newCall(request).execute().use { response ->
            val json = response.body?.string() ?: "[]"
            val array = AppJson.parseToJsonElement(json).jsonArray
            array.mapNotNull { element ->
                val obj = element.jsonObject
                val versionName = obj["tag_name"]?.jsonPrimitive?.content?.removePrefix("v") ?: return@mapNotNull null
                val body = obj["body"]?.jsonPrimitive?.content ?: ""
                ReleaseInfo(versionName, body)
            }
        }
    }

    private fun getReleasesNewerThan(currentVersion: String): List<ReleaseInfo> {
        return getAllReleases().filter { VersionUtil.compare(it.versionName, currentVersion) > 0 }
    }

    fun asyncLoadChangeLog(onResult: (String) -> Unit) {
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val releases = getAllReleases()
                    Timber.d("Loaded ${releases.size} releases")
                    releases.forEach { Timber.d("Release ${it.versionName}: body=${it.body}") }
                    releases.joinToString("\n\n") { release ->
                        val body = release.body.ifEmpty { "(no notes)" }
                        "${release.versionName}\n${body}"
                    }
                }
                onResult(text.ifEmpty { "暂无更新日志" })
            } catch (e: Exception) {
                Timber.e(e, "获取更新日志失败")
                onResult("获取更新日志失败: ${e.message}")
            }
        }
    }

    fun asyncCheckVersion(context: Context, tip: Boolean = false) {
        scope.launch {
            try {
                data class CheckResult(val newestVersionName: String, val hasUpdate: Boolean, val changeLog: String?)
                val result = withContext(Dispatchers.IO) {
                    val currentVersionName = VersionUtil.getAppVersionName(context)
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
                        android.widget.Toast.makeText(context, context.getString(R.string.tip_no_new_version), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("有更新 ${result.newestVersionName}")
                    .setMessage(result.changeLog)
                    .setNeutralButton("忽略") { _, _ ->
                        knownVersionName = result.newestVersionName
                    }
                    .setPositiveButton("Github") { _, _ ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_GITHUB)))
                    }
                    .create().safelyShow()
            } catch (e: Exception) {
                val message = "检测更新失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                if (tip) {
                    android.widget.Toast.makeText(context, context.getString(R.string.tip_update_failed), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private const val RELEASE_GITHUB = "https://github.com/JNKKKK/PaNovel-Reborn/releases"
    private const val RELEASES_GITHUB_API =
        "https://api.github.com/repos/JNKKKK/PaNovel-Reborn/releases"
    private var ignoreSignatureCheck: Boolean by Delegates.boolean(false)
    private var signature: String by Delegates.string("")

    /**
     * @return 忽略或者通过都返回true,
     */
    private fun checkSignature(context: Context): Boolean {
        Timber.i("checkSignature " + BuildConfig.SIGNATURE)
        if (TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - context.packageManager.getPackageInfo(
                    context.packageName,
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
            ?: SignatureUtil.getAppSignature(context).also { signature = it }
        Timber.i("apkSign = $apkSign")
        return BuildConfig.SIGNATURE.equals(apkSign, ignoreCase = true)
    }

    fun asyncCheckSignature(context: Context) {
        scope.launch {
            try {
                val passed = withContext(Dispatchers.IO) { checkSignature(context) }
                if (passed) {
                    return@launch
                }
                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("签名不正确")
                    .setMessage("你可能用了假app,")
                    .setNeutralButton("忽略") { _, _ ->
                        ignoreSignatureCheck = true
                    }
                    .setPositiveButton("Github") { _, _ ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_GITHUB)))
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
