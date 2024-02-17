package io.github.spir0th.music.fragments

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity
import io.github.spir0th.music.utils.convert
import io.github.spir0th.music.utils.setNightMode

class AppearanceSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey)
        val theme = findPreference<ListPreference>("theme")

        theme?.setOnPreferenceChangeListener { _, newValue ->
            requireActivity().application.convert().setNightMode(newValue as String)
            true
        }
    }

    override fun onStart() {
        super.onStart()

        if (requireActivity() is SettingsActivity) {
            val activity = requireActivity() as SettingsActivity
            activity.supportActionBar?.title = getString(R.string.prefs_appearance)
        }
    }
}