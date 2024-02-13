package io.github.spir0th.music.fragments

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.spir0th.music.BuildConfig
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity

class AboutFragment : PreferenceFragmentCompat() {
    private lateinit var preferences: SharedPreferences
    private var nameClickCount = 0

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_about, rootKey)
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        parsePreferenceStringPlaceholders()

        val github = findPreference<Preference>("github")
        val name = findPreference<Preference>("name")

        github?.setOnPreferenceClickListener {
            val cTabsIntent = CustomTabsIntent.Builder().build()
            cTabsIntent.launchUrl(requireActivity(), Uri.parse(getString(R.string.prefs_about_dev_github_url)))
            true
        }
        name?.setOnPreferenceClickListener {
            nameClickCount += 1

            if (nameClickCount >= 5) {
                if (!preferences.getBoolean("experiments", false)) {
                    Toast.makeText(requireContext(), R.string.prefs_about_experiments_enabled, Toast.LENGTH_LONG).show()
                    preferences.edit().putBoolean("experiments", true).apply()
                    (requireActivity() as SettingsActivity).restartApplication()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dialog_disable_experimental_features_title)
                        .setMessage(R.string.dialog_disable_experimental_features_message)
                        .setPositiveButton(R.string.dialog_disable_experimental_features_positive) { _, _ ->
                            Toast.makeText(requireContext(), R.string.prefs_about_experiments_disabled, Toast.LENGTH_LONG).show()
                            preferences.edit().putBoolean("experiments", false).apply()
                            (requireActivity() as SettingsActivity).restartApplication()
                        }
                        .setNegativeButton(R.string.dialog_disable_experimental_features_negative) { _, _ -> }
                        .setCancelable(false)
                        .show()
                }

                nameClickCount = 0
            }

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