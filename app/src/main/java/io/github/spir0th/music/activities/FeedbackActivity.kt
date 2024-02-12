package io.github.spir0th.music.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import io.github.spir0th.music.BuildConfig
import io.github.spir0th.music.R
import io.github.spir0th.music.databinding.ActivityFeedbackBinding
import io.github.spir0th.music.utils.UiUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.abs

class FeedbackActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeedbackBinding
    private lateinit var supabaseClient: SupabaseClient

    private var isUploadingFeedback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.feedbackToolbar)
        UiUtils.adjustSystemBarInsetsForView(binding.feedbackToolbar, top=true)
        supportActionBar?.setTitle(R.string.feedback_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onSupportNavigateUp()
            }
        })
        binding.feedbackSubmit.setOnClickListener {
            if (binding.feedbackInputField.editText?.text.isNullOrEmpty()) {
                binding.feedbackInputField.error = getString(R.string.feedback_input_field_error_empty)
                return@setOnClickListener
            } else {
                binding.feedbackInputField.error = null
            }

            val id = abs((0..999999999999).random())
            val filename = "feedback_$id.txt"
            val bucket = supabaseClient.storage.from("feedbacks")
            val output = generateFeedbackOutput()

            MainScope().launch {
                try {
                    bucket.uploadAsFlow(filename, output.toByteArray()).collect {
                        when (it) {
                            is UploadStatus.Progress -> {
                                isUploadingFeedback = true
                                binding.feedbackProgressIndicator.visibility = View.VISIBLE
                                binding.feedbackInputField.isEnabled = false
                                binding.feedbackSubmit.isEnabled = false
                            }
                            is UploadStatus.Success -> {
                                isUploadingFeedback = false
                                Snackbar.make(binding.root, R.string.feedback_submit_success, Snackbar.LENGTH_LONG).show()
                                binding.feedbackInputField.editText?.text?.clear()
                            }
                            else -> {}
                        }
                    }
                } catch (_: Exception) {
                    Snackbar.make(binding.root, R.string.feedback_submit_fail, Snackbar.LENGTH_LONG).show()
                } finally {
                    binding.feedbackProgressIndicator.visibility = View.GONE
                    binding.feedbackInputField.clearFocus()
                    binding.feedbackInputField.isEnabled = true
                    binding.feedbackSubmit.isEnabled = true
                }
            }
        }

        initializeSupabase()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (isUploadingFeedback) {
            return false
        }

        finish()
        return super.onSupportNavigateUp()
    }

    private fun generateFeedbackOutput(): String {
        val outputBuilder = StringBuilder()
        val separator = System.getProperty("line.separator")
        outputBuilder.append("${getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE}) $separator")
        outputBuilder.append("${BuildConfig.APPLICATION_ID} $separator")
        outputBuilder.append("-------------------------------------------- $separator")
        outputBuilder.append("${binding.feedbackInputField.editText?.text} $separator")
        outputBuilder.append("-------------------------------------------- $separator")
        outputBuilder.append("Feedback submitted from a ${Build.MODEL} running Android ${Build.VERSION.RELEASE} $separator")
        return outputBuilder.toString()
    }

    @Suppress("KotlinConstantConditions")
    private fun initializeSupabase() {
        if (BuildConfig.supabaseApiKey == "null") {
            Toast.makeText(this, R.string.feedback_supabase_key_not_provided, Toast.LENGTH_LONG).show()
            onBackPressedDispatcher.onBackPressed()
            return
        }
        supabaseClient = createSupabaseClient(
            "https://ljauujzzqitfprkpuuxk.supabase.co",
            BuildConfig.supabaseApiKey) {
            install(Storage)
        }
    }
}