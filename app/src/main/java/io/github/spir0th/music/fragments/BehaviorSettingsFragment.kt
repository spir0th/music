package io.github.spir0th.music.fragments

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity

class BehaviorSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_behavior, rootKey)
        val timeGetDuration = findPreference<EditTextPreference>("time_get_duration")

        timeGetDuration?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (newValue.toString().isEmpty()) {
                (preference as EditTextPreference).text = "0"
                return@OnPreferenceChangeListener false
            }

            return@OnPreferenceChangeListener true
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
            activity.supportActionBar?.title = getString(R.string.prefs_behavior)
        }
    }
}