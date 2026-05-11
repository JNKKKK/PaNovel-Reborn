package cc.aoeiuv020.panovel.main

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import cc.aoeiuv020.base.jar.ownLinesString
import cc.aoeiuv020.panovel.BuildConfig
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.util.*
import cc.aoeiuv020.regex.compilePattern
import cc.aoeiuv020.regex.matches
import cc.aoeiuv020.regex.pick
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.net.URL
import java.util.concurrent.TimeUnit
import timber.log.Timber

/**
 *
 * Created by AoEiuV020 on 2018.03.25-04:00:29.
 */
object Check : Pref {
    override val name: String
        get() = "Check"
    private var cachedVersionName: String by Delegates.string("0")
    private const val CHANGE_LOG_URL =
        "https://raw.githubusercontent.com/AoEiuV020/PaNovel/master/app/src/main/assets/ChangeLog.txt"
    private const val COOLAPK_PAGE_URL = "https://www.coolapk.com/apk/cc.aoeiuv020.panovel"
    private const val COOLAPK_MARKET_PACKAGE_NAME = "com.coolapk.market"
    private var knownVersionName: String by Delegates.string("0")

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun getNewestVersionName(): String {
        return try {
            getCoolapkNewestVersionName()
        } catch (e: Exception) {
            Reporter.post("coolapk检查新版本失败", e)
            OkHttpClient().newCall(Request.Builder().url(LATEST_RELEASE_GITHUB).build())
                .execute().use { response ->
                    val json = response.body?.string() ?: ""
                    val obj = JsonParser.parseString(json).asJsonObject
                    obj.get("tag_name")?.asString ?: ""
                }
        }
    }

    private fun getCoolapkNewestVersionName(): String {
        return Jsoup.connect(COOLAPK_PAGE_URL).get().selectFirst("span.list_app_info")!!
            .text()
            .trim().also { versionName ->
                if (!versionName.matches("\\d*(\\.\\d*)*")) {
                    throw IllegalStateException("coolapk版本号异常, $versionName")
                }
            }
    }

    private fun getCoolapkChangeLog(): String {
        return Jsoup.connect(COOLAPK_PAGE_URL).get()
            .selectFirst("body > div > div:nth-child(2) > div.app_left > div.apk_left_two > div > div:nth-child(2) > p.apk_left_title_info")!!
            .ownLinesString()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun getChangeLogFromAssert(ctx: Context, fromVersion: String): String {
        return try {
            ctx.assets.open("ChangeLog.txt").bufferedReader().cutChangeLog(fromVersion)
        } catch (e: Exception) {
            val message = "剪取更新日志失败，\n"
            message + e.message
        }
    }

    private fun getChangeLog(currentVersionName: String): String = try {
        getCoolapkChangeLog()
    } catch (e: Exception) {
        try {
            URL(CHANGE_LOG_URL).openStream().bufferedReader().cutChangeLog(currentVersionName)
        } catch (e: Exception) {
            val message = "获取更新日志失败，\n"
            message + e.message
        }
    }

    private fun BufferedReader.cutChangeLog(fromVersion: String): String {
        val pattern = compilePattern("([0-9.]*):")
        return useLines { line ->
            line.takeWhile {
                try {
                    val (versionName) = it.pick(pattern)
                    VersionUtil.compare(versionName, fromVersion) > 0
                } catch (_: Exception) {
                    true
                }
            }.joinToString("\n")
        }
    }

    /**
     * @param tip 无更新或者更新失败是否提示，
     */
    fun asyncCheckVersion(ctx: Context, tip: Boolean = false) {
        scope.launch {
            try {
                data class CheckResult(val currentVersionName: String, val newestVersionName: String, val hasUpdate: Boolean, val changeLog: String?)
                val result = withContext(Dispatchers.IO) {
                    val currentVersionName = VersionUtil.getAppVersionName(ctx)
                    val newestVersionName = getNewestVersionName()
                    Timber.i("checkVersion $currentVersionName/$newestVersionName")
                    val hasUpdate = VersionUtil.compare(newestVersionName, currentVersionName) > 0
                            && VersionUtil.compare(newestVersionName, knownVersionName) > 0
                    val changeLog: String? = when {
                        // 有更新，网上获取更新日志，截取当前版本到网上最新的日志，
                        hasUpdate -> {
                            getChangeLog(currentVersionName)
                        }
                        // 已经更新，截取更新部分日志，从上次保存的版本到最新的日志，
                        VersionUtil.compare(currentVersionName, cachedVersionName) > 0 -> {
                            getChangeLogFromAssert(ctx, cachedVersionName)
                        }
                        // 没有更新也不是刚更新完，直接返回，
                        else -> null
                    }
                    CheckResult(currentVersionName, newestVersionName, hasUpdate, changeLog)
                }
                val currentVersionName = result.currentVersionName
                val newestVersionName = result.newestVersionName
                val hasUpdate = result.hasUpdate
                val changeLog = result.changeLog
                if (changeLog == null) {
                    if (tip) {
                        android.widget.Toast.makeText(ctx, ctx.getString(R.string.tip_no_new_version), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                // 缓存当前版本，以便更新后对比，
                cachedVersionName = currentVersionName
                if (hasUpdate) {
                    androidx.appcompat.app.AlertDialog.Builder(ctx)
                        .setTitle("有更新")
                        .setMessage(changeLog)
                        .setNeutralButton("忽略") { _, _ ->
                            knownVersionName = newestVersionName
                        }
                        .setPositiveButton("酷安") { _, _ ->
                            startCoolapk(ctx)
                        }
                        .setNegativeButton("Github") { _, _ ->
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_GITHUB)))
                        }
                        .create().safelyShow()
                } else {
                    androidx.appcompat.app.AlertDialog.Builder(ctx)
                        .setTitle("已更新")
                        .setMessage(changeLog)
                        .setPositiveButton(android.R.string.ok, null)
                        .create().safelyShow()
                }
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

    private fun startCoolapk(ctx: Context) {
        // 直接打开酷安市场app，
        // 如果不存在，改打开浏览器，
        val uri = Uri.parse("market://details?id=${ctx.packageName}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.`package` = COOLAPK_MARKET_PACKAGE_NAME
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.i("没安装酷安app,")
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_COOLAPK)))
        }
    }

    private const val RELEASE_COOLAPK = "https://www.coolapk.com/apk/167994"
    private const val RELEASE_GITHUB = "https://github.com/AoEiuV020/PaNovel/releases"
    private const val LATEST_RELEASE_GITHUB =
        "https://api.github.com/repos/AoEiuV020/PaNovel/releases/latest"
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
                    .setPositiveButton("酷安") { _, _ ->
                        startCoolapk(ctx)
                    }
                    .setNegativeButton("Github") { _, _ ->
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
