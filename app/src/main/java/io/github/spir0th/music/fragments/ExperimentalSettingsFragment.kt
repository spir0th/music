package io.github.spir0th.music.fragments

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity
import io.github.spir0th.music.utils.convert
import io.github.spir0th.music.utils.restart
import java.io.File
class ExperimentalSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_experimental, rootKey)
        val logs = findPreference<Preference>("logs")
        val forceRestart = findPreference<Preference>("force_restart")

        logs?.setOnPreferenceClickListener {
            val logDir = File(requireContext().dataDir, "logs")

            if (logDir.list() == null || logDir.list()?.size!! < 1) {
                Toast.makeText(requireContext(), R.string.prefs_experimental_logs_empty, Toast.LENGTH_LONG).show()
                return@setOnPreferenceClickListener false
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_logs_title)
                .setItems(logDir.list()) { _, item ->
                    val filename = logDir.list()?.get(item) ?: String()
                    val file = File(logDir, filename)

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(filename)
                        .setMessage(file.readText())
                        .setNegativeButton(R.string.dialog_logs_negative) { _, _ -> }
                        .show()
                }
                .setNegativeButton(R.string.dialog_logs_negative) { _, _ -> }
                .setNeutralButton(R.string.dialog_logs_neutral) { _, _ ->
                    val count = logDir.list()?.size
                    logDir.deleteRecursively()
                    Toast.makeText(requireContext(), getString(R.string.dialog_logs_clear, count), Toast.LENGTH_LONG).show()
                }
                .show()

            true
        }
        forceRestart?.setOnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_restart_title)
                .setPositiveButton(R.string.dialog_restart_positive) { _, _ ->
                    requireActivity().application.convert().restart()
                }
                .setNegativeButton(R.string.dialog_restart_negative) { _, _ -> }
                .show()

            true
        }
    }

    override fun onStart() {
        super.onStart()

        if (requireActivity() is SettingsActivity) {
            val activity = requireActivity() as SettingsActivity
            activity.supportActionBar?.title = getString(R.string.prefs_experimental)
        }
    }
}