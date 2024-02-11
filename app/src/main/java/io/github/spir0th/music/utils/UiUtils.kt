package io.github.spir0th.music.utils

import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding

object UiUtils {
    @JvmStatic fun setImmersiveMode(window: Window, toggle: Boolean) {
        val wInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (toggle) {
            wInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            wInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            wInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    @JvmStatic fun adjustSystemBarInsetsForView(view: View,
                                                left: Boolean = false,
                                                top: Boolean = false,
                                                right: Boolean = false,
                                                bottom: Boolean = false) {
        val (initialLeft, initialTop, initialRight, initialBottom) =
            listOf(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
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
}