package io.github.spir0th.music.fragments

import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity

class AppearanceSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey)
        val dynamicColors = findPreference<SwitchPreferenceCompat>("dynamic_colors")

        dynamicColors?.setOnPreferenceChangeListener { _, _ ->
            Toast.makeText(requireContext(), R.string.settings_restart, Toast.LENGTH_LONG).show()
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