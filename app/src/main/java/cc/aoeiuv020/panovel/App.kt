package cc.aoeiuv020.panovel

import android.app.ActivityManager
import android.os.Process
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.shared.ssl.TLSSocketFactory
import cc.aoeiuv020.shared.ssl.TrustManagerUtils
import com.bumptech.glide.Glide
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.properties.Delegates


@Suppress("MemberVisibilityCanPrivate")
class App : MultiDexApplication() {
    companion object {
        lateinit var container: AppContainer
            private set

        var isMainProcess: Boolean by Delegates.notNull()

        val json = cc.aoeiuv020.shared.json.AppJson
    }

    override fun onCreate() {
        super.onCreate()
        cc.aoeiuv020.panovel.util.PrefContext.init(applicationContext)
        container = AppContainer(applicationContext)
        isMainProcess = isMainProcess()

        timber.log.Timber.plant(timber.log.Timber.DebugTree())

        initJson()

        initDataSources()

        initSsl()

        initVector()

        initGlide()

        initJar()

    }

    /**
     * JSON initialization - no longer needed after removing custom JsonPathUtils.
     */
    private fun initJson() {
        // No-op: custom JsonPathUtils removed, using Gson directly.
    }

    /**
     * 低版本api(<=20)默认不能用矢量图的selector, 要这样设置，
     * 还有ContextCompat.getDrawable也不行，
     * it's not a BUG, it's a FEATURE,
     * https://issuetracker.google.com/issues/37100284
     *
     * 这个设置只对AppCompatActivity有效，其他context没用，
     */
    private fun initVector() {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    /**
     * android4连接https可能抛SSLHandshakeException，各种毛病，
     * 只这样不能完全修复，但是app里主要是用okhttp3, 那边配置好了，
     */
    private fun initSsl() {
        HttpsURLConnection.setDefaultSSLSocketFactory(
            TLSSocketFactory(
                TrustManagerUtils.include(
                    emptySet()
                )
            )
        )
    }

    private fun initJar() {
        // 禁用默认缓存连接，对任意URLConnection实例使用都可以，
        // 否则jar打开的文件不会被关闭，从而导致文件被覆盖了依然能读到旧文件，
        // 多少会影响性能，
        URL("jar:file:/fake.jar!/fake.file").openConnection().defaultUseCaches = false
    }

    private fun initGlide() {
        Glide.get(applicationContext).registry
    }

    private fun initDataSources() {
        DataManager.init(applicationContext)
    }


    private fun getCurrentProcessName(): String {
        val pid = Process.myPid()
        var processName = ""
        val manager = applicationContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (process in manager.runningAppProcesses) {
            if (process.pid == pid) {
                processName = process.processName
            }
        }
        return processName
    }

    private fun isMainProcess(): Boolean {
        return applicationContext.packageName == getCurrentProcessName()
    }

}
