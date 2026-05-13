package cc.aoeiuv020.panovel.text

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import cc.aoeiuv020.panovel.databinding.ActivityNovelTextBinding
import cc.aoeiuv020.panovel.settings.ReaderSettings
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
        return WindowInsetsControllerCompat(window, binding.flContent)
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
        if (ReaderSettings.fullScreen) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = 0xff000000.toInt()
            }
        }
        mVisible = true
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        hide()
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
