package io.github.spir0th.music

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors

class App : Application() {
    private lateinit var preferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        Log.i(TAG, "${getString(R.string.app_name)} ${BuildConfig.VERSION_CODE}")

        if (preferences.getBoolean("dynamic_colors", true)) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }

    companion object {
        const val TAG = "App"
    }
}