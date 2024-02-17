package io.github.spir0th.music.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity

class AppearanceSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey)
        val dynamicColors = findPreference<SwitchPreferenceCompat>("dynamic_colors")

        dynamicColors?.setOnPreferenceChangeListener { _, _ ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_dynamic_colors_restart_title)
                .setMessage(R.string.dialog_dynamic_colors_restart_message)
                .setPositiveButton(R.string.dialog_dynamic_colors_restart_positive) { _, _ ->
                    (requireActivity() as SettingsActivity).restartApplication() // Restart self
                }
                .setNegativeButton(R.string.dialog_dynamic_colors_restart_negative) { _, _ -> }
                .show()

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