package io.github.spir0th.music.activities

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import io.github.spir0th.music.R
import io.github.spir0th.music.databinding.ActivitySettingsBinding
import io.github.spir0th.music.fragments.SettingsFragment
import io.github.spir0th.music.utils.UiUtils
import java.lang.ClassCastException

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.settingsToolbar)
        UiUtils.adjustSystemBarInsetsForView(binding.settingsToolbar, top=true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fragment = pref.fragment?.let { supportFragmentManager.fragmentFactory.instantiate(classLoader, it) }
        fragment?.arguments = pref.extras

        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment!!)
            .addToBackStack(null)
            .commit()

        return true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        var value: Any = "UNKNOWN"

        try {
            value = sharedPreferences?.getString(key, String())!!
        } catch (_: ClassCastException) {}
        try {
            value = sharedPreferences?.getStringSet(key, setOf())!!
        } catch (_: ClassCastException) {}
        try {
            value = sharedPreferences?.getBoolean(key, false)!!
        } catch (_: ClassCastException) {}
        try {
            value = sharedPreferences?.getInt(key, -1)!!
        } catch (_: ClassCastException) {}

        Log.i(TAG, "$key: $value")
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            finish()
        }

        return super.onSupportNavigateUp()
    }

    companion object {
        const val TAG = "SettingsActivity"
    }
}