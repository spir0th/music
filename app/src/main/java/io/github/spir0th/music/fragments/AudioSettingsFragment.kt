package io.github.spir0th.music.fragments

import android.content.ComponentName
import android.os.Bundle
import android.text.InputType
import androidx.media3.common.AudioAttributes
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.MoreExecutors
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.SettingsActivity
import io.github.spir0th.music.services.PlaybackService
import io.github.spir0th.music.utils.cleanMediaPersists

class AudioSettingsFragment : PreferenceFragmentCompat() {
    private var mediaController: MediaController? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_audio, rootKey)
        val audioFocus = findPreference<CheckBoxPreference>("audio_focus")
        val timeGetDuration = findPreference<EditTextPreference>("time_get_duration")
        val cleanPersistence = findPreference<Preference>("clean_persistence")

        audioFocus?.setOnPreferenceChangeListener { _, newValue ->
            mediaController?.setAudioAttributes(AudioAttributes.DEFAULT, newValue as Boolean)
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

        val sessionToken = SessionToken(requireContext(), ComponentName(requireContext(), PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(requireContext(), sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        super.onStop()
        mediaController?.release()
    }
}