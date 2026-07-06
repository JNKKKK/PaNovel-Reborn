package cc.aoeiuv020.pager.animation

import android.view.View
import cc.aoeiuv020.pager.IMargins

data class AnimationConfig(
        var width: Int,
        var height: Int,
        var margins: IMargins,
        var view: View,
        var listener: PageAnimation.OnPageChangeListener,
        var durationMultiply: Float
)