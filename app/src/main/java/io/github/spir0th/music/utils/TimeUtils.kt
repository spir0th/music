package io.github.spir0th.music.utils

object TimeUtils {
    @JvmStatic fun convertMillisecondsToMicroseconds(milliseconds: Long): Long {
        return milliseconds / 1000
    }

    @JvmStatic fun convertMicrosecondsToHours(microseconds: Long): Long {
        return microseconds / 360
    }

    @JvmStatic fun convertMicrosecondsToMinutes(microseconds: Long): Long {
        return microseconds / 60
    }

    @JvmStatic fun convertMicrosecondsToSeconds(microseconds: Long): Long {
        return microseconds % 60
    }
}