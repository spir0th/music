package io.github.spir0th.music.fragments

import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.spir0th.music.BuildConfig
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity

class AboutFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_about, rootKey)
        parsePreferenceStringPlaceholders()

        val github = findPreference<Preference>("github")!!

        github.setOnPreferenceClickListener {
            val cTabsIntent = CustomTabsIntent.Builder().build()
            cTabsIntent.launchUrl(requireActivity(), Uri.parse(getString(R.string.prefs_about_dev_github_url)))
            true
        }
    }

    override fun onStart() {
        super.onStart()

        if (requireActivity() is SettingsActivity) {
            val activity = requireActivity() as SettingsActivity
            activity.supportActionBar?.title = getString(R.string.prefs_about)
        }
    }

    private fun parsePreferenceStringPlaceholders() {
        val name = findPreference<Preference>("name")!!
        val version = BuildConfig.VERSION_NAME
        name.summary = getString(R.string.prefs_about_version, version)
    }
}