package io.github.spir0th.music.fragments

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.processphoenix.ProcessPhoenix
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
                .setPositiveButton(R.string.dialog_dynamic_colors_restart_positive) { dialog, _ ->
                    dialog.dismiss() // Dismiss dialog

                    // Restart application
                    val intent = Intent(requireContext(), SettingsActivity::class.java)
                    ProcessPhoenix.triggerRebirth(requireContext(), intent)
                }
                .setNegativeButton(R.string.dialog_dynamic_colors_restart_negative) { dialog, _ ->
                    dialog.dismiss() // Only dismiss dialog
                }
                .setCancelable(false)
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