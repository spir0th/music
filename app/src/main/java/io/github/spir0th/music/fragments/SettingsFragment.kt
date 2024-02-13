package io.github.spir0th.music.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.transition.Fade
import androidx.transition.Slide
import io.github.spir0th.music.BuildConfig
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var preferences: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val experimental = findPreference<Preference>("experimental")!!
        experimental.isVisible = preferences.getBoolean("experiments", false)
    }

    override fun onStart() {
        super.onStart()

        if (requireActivity() is SettingsActivity) {
            val activity = requireActivity() as SettingsActivity
            activity.supportActionBar?.title = getString(R.string.settings_title)
        }
    }
}