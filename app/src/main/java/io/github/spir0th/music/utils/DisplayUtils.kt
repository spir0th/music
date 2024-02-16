package io.github.spir0th.music.utils

import android.view.Display
import androidx.core.hardware.display.DisplayManagerCompat

fun DisplayManagerCompat.isScreenOn(): Boolean {
    displays.forEach {
        if (it.state == Display.STATE_ON) {
            return true
        }
    }

    return false
}