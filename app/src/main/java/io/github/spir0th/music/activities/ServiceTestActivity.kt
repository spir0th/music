package io.github.spir0th.music.activities

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import io.github.spir0th.music.R
import io.github.spir0th.music.databinding.ActivityServiceTestBinding
import io.github.spir0th.music.services.PlaybackService
import io.github.spir0th.music.utils.adjustForSystemBarInsets

class ServiceTestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityServiceTestBinding
    private var mediaController: MediaController? = null
    private val loopHandler: Handler? = Looper.myLooper()?.let { Handler(it) }
    private var loopRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityServiceTestBinding.inflate(layoutInflater)
        binding.toolbar.adjustForSystemBarInsets(top=true)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.service_info_title)

        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@ServiceTestActivity, SettingsActivity::class.java))
                finish()
            }
        })
        loopRunnable = Runnable {
            updateInfo()
            loopRunnable?.let { loopHandler?.postDelayed(it, 1000) }
        }.also {
            loopHandler?.post(it)
        }
        binding.clearMediaItems.setOnClickListener {
            if (mediaController == null) {
                Toast.makeText(this, R.string.service_info_clear_media_items_fail, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (mediaController?.mediaItemCount!! > 0) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_clear_media_items_title)
                    .setMessage(R.string.dialog_clear_media_items_message)
                    .setPositiveButton(R.string.dialog_clear_media_items_positive) { _, _ ->
                        clearMediaItems()
                    }
                    .setNegativeButton(R.string.dialog_clear_media_items_negative) { _, _ -> }
                    .show()
            } else {
                clearMediaItems()
            }
        }
        binding.createNewItem.setOnClickListener {
            if (mediaController == null) {
                Toast.makeText(this, R.string.service_info_create_media_item_fail, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val uriEditText = EditText(this).apply {
                isSingleLine = true
            }
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_create_media_item_title)
                .setView(uriEditText)
                .setPositiveButton(R.string.dialog_create_media_item_positive) { _, _ ->
                    mediaController!!.addMediaItem(MediaItem.fromUri(uriEditText.text.toString()))
                    Toast.makeText(this, R.string.service_info_create_media_item_success, Toast.LENGTH_LONG).show()
                }
                .setNegativeButton(R.string.dialog_create_media_item_negative) { _, _ -> }
                .show()
        }
        binding.connectSession.setOnClickListener {
            connectSessionService(true)
        }
        binding.disconnectSession.setOnClickListener {
            disconnectSessionService(true)
        }

        connectSessionService()
    }

    override fun onDestroy() {
        super.onDestroy()
        loopRunnable?.let { loopHandler?.removeCallbacks(it) }
        disconnectSessionService()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun updateInfo() {
        val builder = StringBuilder()
        builder.appendLine(getString(R.string.service_info_running, isServiceRunning()))
        builder.appendLine(getString(R.string.service_info_connected, mediaController != null))
        builder.appendLine(getString(R.string.service_info_previous, mediaController?.hasPreviousMediaItem()))
        builder.appendLine(getString(R.string.service_info_next, mediaController?.hasNextMediaItem()))
        builder.appendLine(getString(R.string.service_info_playback_state, mediaController?.playbackState))
        builder.appendLine(getString(R.string.service_info_media_count, mediaController?.mediaItemCount))
        builder.appendLine(getString(R.string.service_info_media_index, mediaController?.currentMediaItemIndex))
        builder.appendLine(getString(R.string.service_info_duration, mediaController?.duration))
        builder.appendLine(getString(R.string.service_info_duration_position, mediaController?.currentPosition))
        builder.appendLine(getString(R.string.service_info_device_volume_max, mediaController?.deviceInfo?.maxVolume))
        builder.appendLine(getString(R.string.service_info_device_volume_min, mediaController?.deviceInfo?.minVolume))
        builder.appendLine(getString(R.string.service_info_device_volume_current, mediaController?.deviceVolume))
        binding.info.text = builder
    }

    private fun clearMediaItems() {
        val count = mediaController?.mediaItemCount
        mediaController?.clearMediaItems()
        Toast.makeText(this, getString(R.string.service_info_clear_media_items_success, count), Toast.LENGTH_LONG).show()
    }

    private fun connectSessionService(notify: Boolean = false) {
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()

            if (notify) {
                Toast.makeText(this, R.string.service_info_connect_session_success, Toast.LENGTH_LONG).show()
            } else {
                Log.i(TAG, getString(R.string.service_info_disconnect_session_success))
            }
        }, MoreExecutors.directExecutor())
    }

    private fun disconnectSessionService(notify: Boolean = false) {
        mediaController?.release()
        mediaController = null

        if (notify) {
            Toast.makeText(this, R.string.service_info_disconnect_session_success, Toast.LENGTH_LONG).show()
        } else {
            Log.i(TAG, getString(R.string.service_info_disconnect_session_success))
        }
    }

    @Suppress("Deprecation")
    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val serviceClass = PlaybackService::class.java

        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name.equals(service.service.className)) {
                return true
            }
        }

        return false
    }

    companion object {
        const val TAG = "ServiceTestActivity"
    }
}