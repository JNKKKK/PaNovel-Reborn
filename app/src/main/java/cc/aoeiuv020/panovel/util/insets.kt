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
 * Paints a solid strip behind the status bar (and optionally the navigation bar), each
 * sized to the live system-bar inset, so the transparent status bar shows a solid color
 * instead of the content behind it.
 *
 * Added to the window decor as top/bottom overlays. When the bars are hidden (e.g. the
 * reader's immersive mode) the insets go to 0 and the strips collapse automatically.
 *
 * Intended for the fullscreen reader, which sets its own `statusBarColor` (ignored under
 * forced edge-to-edge) and would otherwise show a transparent status bar over the page
 * content. The nav bar is left to the system default (translucent contrast scrim) unless
 * [navColorRes] is given.
 */
fun AppCompatActivity.addSystemBarScrims(
    statusColorRes: Int = R.color.colorAppBar,
    navColorRes: Int? = null,
) {
    val decor = window.decorView as ViewGroup
    val content = findViewById<View>(android.R.id.content)

    val statusScrim = View(this).apply {
        setBackgroundColor(ContextCompat.getColor(this@addSystemBarScrims, statusColorRes))
    }
    decor.addView(statusScrim, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP))

    val navScrim = navColorRes?.let { colorRes ->
        View(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@addSystemBarScrims, colorRes))
        }.also {
            decor.addView(it, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.BOTTOM))
        }
    }

    ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        if (statusScrim.layoutParams.height != top) {
            statusScrim.layoutParams = statusScrim.layoutParams.also { it.height = top }
        }
        if (navScrim != null && navScrim.layoutParams.height != bottom) {
            navScrim.layoutParams = navScrim.layoutParams.also { it.height = bottom }
        }
        insets
    }
    ViewCompat.requestApplyInsets(content)
}

/**
 * Pads [this] view's bottom so its last item comes to rest just above the system nav bar
 * (or the IME when the keyboard is open), then can still scroll through the padded region
 * (`clipToPadding = false`). Any bottom padding already set in XML is preserved.
 *
 * Apply to the scrolling view itself (e.g. the RecyclerView), not a wrapping
 * SwipeRefreshLayout, so the refresh spinner isn't pushed down.
 *
 * The pad is the amount this view's frame actually *overlaps* the nav-bar/IME region, not
 * the raw inset. On edge-to-edge screens the view extends to the window bottom, so the
 * overlap is the full inset and the last item lands right above the bar. On screens where
 * an ancestor already ends the content above the nav bar (e.g. the main screen, whose
 * solid nav bar means content isn't drawn behind it), the overlap is 0 and nothing is
 * added — otherwise the raw inset would stack on top of the ancestor's, leaving a gap a
 * full nav-bar-height tall. Measured post-layout because it needs the view's laid-out
 * frame; the inset object alone always reports the full inset regardless of ancestors.
 */
fun ViewGroup.applyBottomNavBarInsetPadding() {
    clipToPadding = false
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val systemBarBottom = maxOf(navBottom, imeBottom)
        v.post {
            val loc = IntArray(2)
            v.getLocationInWindow(loc)
            val viewBottom = loc[1] + v.height
            val barTop = v.rootView.height - systemBarBottom
            val overlap = (viewBottom - barTop).coerceIn(0, systemBarBottom)
            v.updatePadding(bottom = initialBottom + overlap)
        }
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/**
 * Pads [this] view's bottom by the raw navigation-bar inset (or the IME inset when the
 * keyboard is open). Unlike [applyBottomNavBarInsetPadding], this uses the inset value
 * directly (no post-layout geometry), so it's correct on the very first inset pass even
 * before the view is measured — use it for a bottom-anchored bar that always sits at the
 * window bottom and paints its own background into the padded region (so the padding
 * becomes a solid colored strip behind the translucent nav bar). Any XML bottom padding is
 * preserved and the inset is added on top.
 */
fun View.applyBottomNavBarInsetPaddingDirect() {
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        v.updatePadding(bottom = initialBottom + maxOf(navBottom, imeBottom))
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
