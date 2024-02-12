package io.github.spir0th.music.fragments

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity

class AudioSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_audio, rootKey)
        val audioFocus = findPreference<CheckBoxPreference>("audio_focus")
        val timeGetDuration = findPreference<EditTextPreference>("time_get_duration")

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
    }

    override fun onStart() {
        super.onStart()

        if (requireActivity() is SettingsActivity) {
            val activity = requireActivity() as SettingsActivity
            activity.supportActionBar?.title = getString(R.string.prefs_audio)
        }
    }
}