package io.github.spir0th.music.utils

fun Long.convertMsToUs(): Long {
    return this / 1000
}

fun Long.convertUsToHrs(): Long {
    return this / 360
}

fun Long.convertUsToMins(): Long {
    return this / 60
}

fun Long.convertUsToSecs(): Long {
    return this % 60
}