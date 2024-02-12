package io.github.spir0th.music.fragments

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.processphoenix.ProcessPhoenix
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity
import io.github.spir0th.music.utils.MediaUtils


class AudioSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_audio, rootKey)
        val audioFocus = findPreference<CheckBoxPreference>("audio_focus")
        val timeGetDuration = findPreference<EditTextPreference>("time_get_duration")
        val cleanPersistence = findPreference<Preference>("clean_persistence")

        audioFocus?.setOnPreferenceChangeListener { _, _ ->
            Toast.makeText(requireContext(), R.string.settings_restart, Toast.LENGTH_LONG).show()
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
                .setPositiveButton(R.string.dialog_clean_persistence_positive) { dialog, _ ->
                    // Dismiss current dialog then clean the persistent files
                    dialog.dismiss()
                    MediaUtils.cleanMediaPersists(requireContext())

                    // Start an instance of this activity and kill this one (basically restart self)
                    val intent = Intent(requireContext(), SettingsActivity::class.java)
                    ProcessPhoenix.triggerRebirth(requireContext(), intent)
                }
                .setNegativeButton(R.string.dialog_clean_persistence_negative) { dialog, _ ->
                    // Only dismiss dialog
                    dialog.dismiss()
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
            activity.supportActionBar?.title = getString(R.string.prefs_audio)
        }
    }
}