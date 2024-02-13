package io.github.spir0th.music

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
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
        Thread.setDefaultUncaughtExceptionHandler(this)
        Log.i(TAG, "Initialized ${getString(R.string.app_name)} version ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")

        if (preferences.getBoolean("dynamic_colors", true)) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e(TAG, "A fatal exception has occurred in the application, generating crash log.")
        val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val logDir = File(dataDir, "logs")
        val file = File(logDir, "crash-$dateTime.log")

        apply {
            logDir.mkdirs()
            file.createNewFile()
        }
        file.printWriter().use { out ->
            out.println(e.stackTraceToString())
        }

        Log.i(TAG, "A crash log has been generated, now exiting!")
        exitProcess(-1)
    }

    companion object {
        const val TAG = "App"
    }
}