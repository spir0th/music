package io.github.spir0th.music

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import io.github.spir0th.music.utils.generateTraceLog
import io.github.spir0th.music.utils.setNightMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class App : Application(), Thread.UncaughtExceptionHandler {
    private lateinit var preferences: SharedPreferences
    override fun onCreate() {
        super.onCreate()
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.getString("theme", "system")?.let { setNightMode(it) }
        Log.i(TAG, "Initialized ${getString(R.string.app_name)} version ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        generateTraceLog(e)
    }

    companion object {
        const val TAG = "App"
    }
}