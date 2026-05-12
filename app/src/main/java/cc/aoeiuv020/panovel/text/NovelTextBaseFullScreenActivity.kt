package cc.aoeiuv020.panovel.text

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.databinding.ActivityNovelTextBinding
import cc.aoeiuv020.panovel.settings.ReaderSettings
import cc.aoeiuv020.panovel.util.hide
import cc.aoeiuv020.panovel.util.show
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 全屏Activity，绝大部分代码是自动生成的，
 * 分离出来仅供activity_novel_text使用，
 * Created by AoEiuV020 on 2017.09.15-17:38.
 */
@Suppress("MemberVisibilityCanPrivate", "unused")
abstract class NovelTextBaseFullScreenActivity : AppCompatActivity() {
    protected lateinit var binding: ActivityNovelTextBinding
    private var hideJob: Job? = null
    private var showJob: Job? = null
    private var delayedHideJob: Job? = null

    @SuppressLint("InlinedApi")
    private fun applyFullScreenFlags() {
        if (ReaderSettings.fullScreen) {
            binding.flContent.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
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
            binding.flContent.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
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
