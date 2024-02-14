package io.github.spir0th.music.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding

fun WindowInsetsControllerCompat.setImmersiveMode(toggle: Boolean) {
    if (toggle) {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    } else {
        show(WindowInsetsCompat.Type.systemBars())
    }
}
fun View.adjustForSystemBarInsets(left: Boolean = false,
                                 top: Boolean = false,
                                 right: Boolean = false,
                                 bottom: Boolean = false) {
    val (initialLeft, initialTop, initialRight, initialBottom) =
        listOf(paddingLeft, paddingTop, paddingRight, paddingBottom)

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

        v.updatePadding(
            left = initialLeft + (if (left) insets.left else 0),
            top = initialTop + (if (top) insets.top else 0),
            right = initialRight + (if (right) insets.right else 0),
            bottom = initialBottom + (if (bottom) insets.bottom else 0)
        )

        windowInsets
    }
}