package cc.aoeiuv020.panovel.util

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

import cc.aoeiuv020.panovel.R

/**
 * Edge-to-edge helpers.
 *
 * Since targetSdk 35+ Android forces edge-to-edge, scrollable content draws behind the
 * navigation bar. Without handling, the last list item ends up behind the nav bar and
 * can't be tapped. [applyBottomNavBarInsetPadding] fixes that for a scrolling view.
 */

/**
 * Paints solid strips behind the status bar and navigation bar, each sized to the live
 * system-bar inset, so the transparent system bars show a solid color instead of the
 * content behind them.
 *
 * Added to the window decor as top/bottom overlays. When the bars are hidden (e.g. the
 * reader's immersive mode) the insets go to 0 and the strips collapse automatically.
 *
 * Intended for the fullscreen reader, which sets its own `statusBarColor` (ignored under
 * forced edge-to-edge) and would otherwise show a transparent status bar and a
 * translucent nav bar over the page content.
 */
fun AppCompatActivity.addSystemBarScrims(
    statusColorRes: Int = R.color.colorPrimary,
    navColorRes: Int = R.color.colorPrimary,
) {
    val decor = window.decorView as ViewGroup
    val content = findViewById<View>(android.R.id.content)

    val statusScrim = View(this).apply {
        setBackgroundColor(ContextCompat.getColor(this@addSystemBarScrims, statusColorRes))
    }
    val navScrim = View(this).apply {
        setBackgroundColor(ContextCompat.getColor(this@addSystemBarScrims, navColorRes))
    }
    decor.addView(statusScrim, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP))
    decor.addView(navScrim, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.BOTTOM))

    ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        if (statusScrim.layoutParams.height != top) {
            statusScrim.layoutParams = statusScrim.layoutParams.also { it.height = top }
        }
        if (navScrim.layoutParams.height != bottom) {
            navScrim.layoutParams = navScrim.layoutParams.also { it.height = bottom }
        }
        insets
    }
    ViewCompat.requestApplyInsets(content)
}

/**
 * Pads [this] view's bottom by the navigation-bar inset (and the IME inset when the
 * keyboard is open) so its last item comes to rest above the system nav bar. Sets
 * `clipToPadding = false` so items still scroll through the padded region.
 *
 * Apply to the scrolling view itself (e.g. the RecyclerView), not a wrapping
 * SwipeRefreshLayout, so the refresh spinner isn't pushed down. Any bottom padding
 * already set in XML is preserved and the inset is added on top of it.
 */
fun ViewGroup.applyBottomNavBarInsetPadding() {
    clipToPadding = false
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        v.updatePadding(bottom = initialBottom + maxOf(navBottom, imeBottom))
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
