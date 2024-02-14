package io.github.spir0th.music.fragments

import android.os.Bundle
import android.text.InputType
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity
import io.github.spir0th.music.utils.cleanMediaPersists

class AudioSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_audio, rootKey)
        val audioFocus = findPreference<CheckBoxPreference>("audio_focus")
        val timeGetDuration = findPreference<EditTextPreference>("time_get_duration")
        val cleanPersistence = findPreference<Preference>("clean_persistence")

        audioFocus?.setOnPreferenceChangeListener { _, _ ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_audio_focus_restart_title)
                .setMessage(R.string.dialog_audio_focus_restart_message)
                .setPositiveButton(R.string.dialog_audio_focus_restart_positive) { _, _ ->
                    (requireActivity() as SettingsActivity).restartApplication() // Restart application
                }
                .setNegativeButton(R.string.dialog_audio_focus_restart_negative) { _, _ -> }
                .setCancelable(false)
                .show()

            true
        }
        timeGetDuration?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (newValue.toString().isEmpty()) {
                (preference as EditTextPreference).text = "0"
                return@OnPreferenceChangeListener false
            }

            true
        }
        timeGetDuration?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
            val text = it.text

            if (text.isNullOrEmpty() || text == "0") {
                return@SummaryProvider "Immediate"
            }

            "${text}ms"
        }
        timeGetDuration?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
        }
        cleanPersistence?.setOnPreferenceClickListener {
            // This preference must ask the user first if they want to clean persistence right now
            // because cleaning it requires a restart of the application.
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_clean_persistence_title)
                .setMessage(R.string.dialog_clean_persistence_message)
                .setPositiveButton(R.string.dialog_clean_persistence_positive) { _, _ ->
                    // Clean persistent files, then restart self
                    requireContext().cleanMediaPersists()
                    (requireActivity() as SettingsActivity).restartApplication()
                }
                .setNegativeButton(R.string.dialog_clean_persistence_negative) { _, _ -> }
                .setCancelable(false)
                .show()

            true
        }
    }

    override fun onStart() {
        super.onStart()

        if (requireActivity() is SettingsActivity) {
            val activity = requireActivity() as SettingsActivity
            activity.supportActionBar?.title = getString(R.string.prefs_audio)
        }
    }
}