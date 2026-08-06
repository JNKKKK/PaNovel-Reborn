package cc.aoeiuv020.panovel.text

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.databinding.ActivityNovelTextBinding
import cc.aoeiuv020.panovel.settings.ReaderSettings
import cc.aoeiuv020.panovel.util.addSystemBarScrims
import cc.aoeiuv020.panovel.util.hide
import cc.aoeiuv020.panovel.util.show
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("MemberVisibilityCanPrivate", "unused")
abstract class NovelTextBaseFullScreenActivity : AppCompatActivity() {
    protected lateinit var binding: ActivityNovelTextBinding
    private var hideJob: Job? = null
    private var showJob: Job? = null
    private var delayedHideJob: Job? = null

    private fun getInsetsController(): WindowInsetsControllerCompat {
        // Build from the decor view: some OEMs only honor the navigation-bar appearance
        // bit when the controller is anchored to the decor view, not a content view.
        return WindowInsetsControllerCompat(window, window.decorView)
    }

    // The reader's system bars are dark (via the scrims), so the system must draw light
    // (white) icons. The modern WindowInsetsController appearance flags below are the
    // spec-correct way, but some devices (e.g. Motorola 3-button nav) ignore them for the
    // nav bar and tint nav icons purely from Window.navigationBarColor's luminance. A dark
    // navigationBarColor is the only lever that yields white icons there, so keep it
    // (deprecated) alongside the modern flags so the result is correct everywhere.
    @Suppress("DEPRECATION")
    private fun applyDarkSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.navigationBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
        getInsetsController().apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun applyFullScreenFlags() {
        if (ReaderSettings.fullScreen) {
            val controller = getInsetsController()
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showControls() {
        binding.appBar.show()
        binding.fullscreenContentControls.visibility = View.VISIBLE
    }

    protected var mVisible: Boolean = false

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNovelTextBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Reader-only: paint the (forced-transparent) status and nav bars a solid dark
        // color. Scrims track the live insets, so in fullscreen/immersive mode they
        // collapse to 0 and the reading canvas stays truly edge-to-edge.
        // Put the window in edge-to-edge mode unconditionally; the nav-bar appearance API
        // is only reliable there (the non-fullscreen branch previously left the window in
        // legacy decor-fits mode, where OEMs handle the appearance bit inconsistently).
        WindowCompat.setDecorFitsSystemWindows(window, false)
        addSystemBarScrims()
        applyDarkSystemBars()
        if (ReaderSettings.fullScreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        mVisible = true
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Re-assert after the window is laid out; appearance flags set in onCreate can be
        // reset during the first layout pass.
        applyDarkSystemBars()
        hide()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // System-bar appearance is commonly reset on focus changes (and the reader hides/
        // shows the bars), so assert the dark-bar / white-icon appearance again here.
        if (hasFocus) {
            applyDarkSystemBars()
        }
    }

    override fun onRestart() {
        super.onRestart()

        if (!mVisible) {
            hide()
        }
    }

    fun toggle() {
        if (mVisible) {
            hide()
        } else {
            if (binding.fullscreenContentControls.visibility != View.GONE) {
                hide()
            } else {
                show()
            }
        }
    }

    fun fullScreen() {
        binding.appBar.hide()
        mVisible = false
        showJob?.cancel()
        hideJob = lifecycleScope.launch {
            delay(UI_ANIMATION_DELAY.toLong())
            applyFullScreenFlags()
        }
    }

    fun hide() {
        Timber.d("hide")
        binding.fullscreenContentControls.visibility = View.GONE
        fullScreen()
    }

    protected open fun show() {
        Timber.d("show")
        if (ReaderSettings.fullScreen) {
            val controller = getInsetsController()
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        mVisible = true
        hideJob?.cancel()
        showJob = lifecycleScope.launch {
            delay(UI_ANIMATION_DELAY.toLong())
            showControls()
        }
    }

    private fun delayedHide(delayMillis: Int) {
        delayedHideJob?.cancel()
        delayedHideJob = lifecycleScope.launch {
            delay(delayMillis.toLong())
            hide()
        }
    }

    companion object {
        private val AUTO_HIDE = true
        private val AUTO_HIDE_DELAY_MILLIS = 3000
        private val UI_ANIMATION_DELAY get() = ReaderSettings.fullScreenDelay
    }
}
