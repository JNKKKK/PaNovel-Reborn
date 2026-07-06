package cc.aoeiuv020.reader

import cc.aoeiuv020.pager.AnimMode

/**
 * 所有翻页动画，对应Pager的AnimMode,
 */
enum class AnimationMode {
    SIMULATION,
    COVER,
    SLIDE,
    NONE,
    SCROLL;

    fun toAnimMode(): AnimMode = when (this) {
        SIMULATION -> AnimMode.SIMULATION
        COVER -> AnimMode.COVER
        SLIDE -> AnimMode.SLIDE
        NONE -> AnimMode.NONE
        SCROLL -> AnimMode.SCROLL
    }

    companion object {
        @Suppress("unused")
        fun fromAnimMode(animMode: AnimMode): AnimationMode = when (animMode) {
            AnimMode.SIMULATION -> SIMULATION
            AnimMode.COVER -> COVER
            AnimMode.SLIDE -> SLIDE
            AnimMode.NONE -> NONE
            AnimMode.SCROLL -> SCROLL
        }
    }
}