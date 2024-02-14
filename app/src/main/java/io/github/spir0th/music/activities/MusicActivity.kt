package io.github.spir0th.music.activities

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.MoreExecutors
import io.github.spir0th.music.R
import io.github.spir0th.music.databinding.ActivityMusicBinding
import io.github.spir0th.music.services.PlaybackService
import io.github.spir0th.music.utils.adjustForSystemBarInsets
import io.github.spir0th.music.utils.convertMsToUs
import io.github.spir0th.music.utils.convertUsToHrs
import io.github.spir0th.music.utils.convertUsToMins
import io.github.spir0th.music.utils.convertUsToSecs
import io.github.spir0th.music.utils.cleanMediaPersists
import io.github.spir0th.music.utils.generateMediaPersistence
import io.github.spir0th.music.utils.setImmersiveMode

class MusicActivity : AppCompatActivity(), Player.Listener {
    private lateinit var binding: ActivityMusicBinding
    private lateinit var preferences: SharedPreferences
    private val durationLoopHandler: Handler? = Looper.myLooper()?.let { Handler(it) }
    private var durationLoopRunnable: Runnable? = null
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Inflate activity view using ViewBinding
        binding = ActivityMusicBinding.inflate(layoutInflater)
        binding.root.adjustForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

        // Register listeners for activity callbacks / player controls
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        binding.playerPlayback.setOnClickListener {
            if (mediaController?.isPlaying == true) {
                mediaController!!.pause()
            } else {
                mediaController?.play()
            }
        }
        binding.playerSkipPrevious.setOnClickListener {
            mediaController?.seekToPrevious()
        }
        binding.playerSkipNext.setOnClickListener {
            mediaController?.seekToNext()
        }
        binding.playerSlider.setLabelFormatter { value ->
            // copied from updatePlaybackDurationUI
            val milliseconds = ((value + 0.0) * mediaController!!.duration).toLong()
            val microseconds = milliseconds.convertMsToUs()
            val minutes = microseconds.convertUsToMins()
            val seconds = microseconds.convertUsToSecs()

            if (microseconds >= 360) {
                val hours = microseconds.convertUsToHrs()
                "$hours:$minutes:${String.format("%1$02d", seconds)}"
            }

            "$minutes:${String.format("%1$02d", seconds)}"
        }
        binding.playerSlider.addOnSliderTouchListener(object: Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                stopDurationLoopHandler()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                startDurationLoopHandler()
                val milliseconds = ((slider.value + 0.0) * mediaController!!.duration).toLong()
                Log.i(TAG, "Player slider value moved from ${mediaController?.currentPosition}ms to ${milliseconds}ms")
                mediaController?.seekTo(milliseconds)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        // Toggle immersive mode if any of these checks are true
        if (preferences.getBoolean("immersive", false)) {
            WindowCompat.getInsetsController(window, window.decorView).setImmersiveMode(true)
        } else if (preferences.getBoolean("immersive_on_landscape", true)) {
            // Depend on the screen orientation instead if respective preference is ticked off
            // Immersive mode may also not be enabled if "immersive_on_landscape" is turned off
            WindowCompat.getInsetsController(window, window.decorView)
                .setImmersiveMode(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        }

        // Connect activity to media session
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(this)
            binding.playerControls.visibility = View.VISIBLE
            doCleanupBeforeService()
            updateFromServiceIfLoaded()
            handleIncomingIntents()
                                     },
            MoreExecutors.directExecutor()
        )
    }

    override fun onStop() {
        super.onStop()

        if (!preferences.getBoolean("background_playback", true)) {
            // If background playback is disabled, then pause when user goes off the activity
            mediaController?.pause()
        }

        // Remove player listeners and disconnect from activity
        mediaController?.removeListener(this)
        mediaController?.release()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        binding.playerControls.visibility = View.VISIBLE
        updatePlaybackSkipUI()
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        updateMetadataUI(mediaMetadata)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        updatePlaybackStateUI(isPlaying)
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Snackbar.make(binding.root, R.string.player_file_error, Snackbar.LENGTH_LONG).show()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
        updatePlaybackDurationUI(newPosition.positionMs)
    }

    private fun updateMetadataUI(metadata: MediaMetadata = mediaController?.mediaMetadata ?: MediaMetadata.EMPTY) {
        updateCoverArtUI() // Fetch cover art, visibility will be handled by this function.
        toggleMetadata(true)

        if (metadata.title?.isNotEmpty() == true) {
            binding.playerTitle.text = metadata.title
        } else if (mediaController?.currentMediaItem?.localConfiguration?.uri != null) {
            // if metadata title is unavailable, we use Uri.lastPathSegment instead
            // or use the app name and author if getTitleFromUri is also unavailable
            val uri = mediaController?.currentMediaItem?.localConfiguration?.uri
            binding.playerTitle.text = uri?.lastPathSegment
        } else {
            toggleMetadata(false, 0)
        }
        if (metadata.artist?.isNotEmpty() == true) {
            binding.playerCaption.text = metadata.artist
        } else {
            toggleMetadata(false, 1)
        }
    }

    private fun updateCoverArtUI(artworkData: ByteArray = mediaController?.mediaMetadata?.artworkData ?: byteArrayOf(1)) {
        val bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)

        if (bitmap != null) {
            Glide.with(this)
                .load(bitmap)
                .transition(withCrossFade())
                .into(binding.playerCoverArt)

            toggleCoverArt(true)
        } else {
            toggleCoverArt(false)
        }
    }

    private fun updatePlaybackDurationUI(position: Long = mediaController?.currentPosition ?: 0) {
        // parse current position into float (pain)
        val duration = mediaController?.duration ?: 0
        var positionFloat = (position + 0.0f) / duration

        if (positionFloat > 1.0f) {
            positionFloat = 1.0f
        } else if (positionFloat < 0.0f) {
            positionFloat = 0.0f
        }

        binding.playerSlider.value = positionFloat

        // parse current position into text
        val microseconds = position.convertMsToUs()
        val minutes = microseconds.convertUsToMins()
        val seconds = microseconds.convertUsToSecs()

        if (microseconds >= 360) {
            val hours = microseconds.convertUsToHrs()
            binding.playerSeekPosition.text = getString(R.string.player_seek_format_hrs, hours, minutes, String.format("%1$02d", seconds))
        } else {
            binding.playerSeekPosition.text = getString(R.string.player_seek_format, minutes, String.format("%1$02d", seconds))
        }
    }

    private fun updatePlaybackSkipUI() {
        if (!preferences.getBoolean("dynamic_player_controls", true)) {
            binding.playerSkipNext.visibility = View.VISIBLE
            return
        }
        if (mediaController?.hasNextMediaItem() == true) {
            binding.playerSkipNext.visibility = View.VISIBLE
        } else {
            binding.playerSkipNext.visibility = View.GONE
        }
    }

    private fun updatePlaybackStateUI(isPlaying: Boolean = mediaController?.isPlaying == true) {
        val drawable: Int = if (isPlaying) {
            Log.v(TAG, "PlaybackState set to STATE_PLAYING")
            R.drawable.baseline_pause_24
        } else {
            Log.v(TAG, "PlaybackState set to STATE_PAUSED")
            R.drawable.baseline_play_arrow_24
        }
        if (isPlaying) {
            startDurationLoopHandler()
        } else {
            stopDurationLoopHandler()
        }

        Glide.with(this@MusicActivity).load(drawable).into(binding.playerPlayback)
    }

    private fun updateFromServiceIfLoaded() {
        if (mediaController?.currentMediaItem == null) {
            return
        }

        Log.i(TAG, "Started update from service if loaded")
        updateMetadataUI()
        updatePlaybackStateUI()
        updatePlaybackSkipUI()
        updatePlaybackDurationUI()
    }

    private fun handleIncomingIntents() {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                if (intent.type?.startsWith("audio/") == true) {
                    intent?.data?.let {
                        var item = MediaItem.fromUri(it)

                        if (intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) {
                            // If the intent flags doesn't have FLAG_GRANT_PERSISTABLE_URI_PERMISSION set
                            // that means the audio content will be temporary and we'll be copying it to our
                            // cache directory so that it can be played without permission issues.
                            if (preferences.getBoolean("non_persistent_playback", true)) {
                                Log.w(TAG, "Uri $it does not have persistence, making it persistent")
                                item = MediaItem.fromUri(it.generateMediaPersistence(this))
                            } else {
                                Log.e(TAG, "Uri $it is non-persistent, but non-persistence playback is disabled. Exit!")
                                Toast.makeText(this, R.string.player_non_persistence_disabled, Toast.LENGTH_LONG).show()
                                onBackPressedDispatcher.onBackPressed()
                                return
                            }
                        }
                        if (mediaController?.currentMediaItem != item) {
                            Log.i(TAG, "Adding audio from incoming intent data: ${item.localConfiguration?.uri}")
                            intent?.data = null
                            mediaController?.stop()
                            mediaController?.addMediaItem(item)
                            mediaController?.seekTo(mediaController!!.mediaItemCount - 1, 0)
                            mediaController?.play()
                        } else {
                            Log.w(TAG, "Incoming intent data received but is already added into queue.")
                            if (mediaController?.isPlaying == false) mediaController?.play() else 0
                        }
                    }
                }
            }
        }
    }

    private fun doCleanupBeforeService() {
        if (mediaController?.mediaItemCount != 0) {
            return
        }

        Log.i(TAG, "Cleaning up audio persistence")
        cleanMediaPersists()
    }

    private fun startDurationLoopHandler() {
        val delayMs = preferences.getString("time_get_duration", "0")!!.toLong()

        durationLoopRunnable = Runnable {
            updatePlaybackDurationUI()
            durationLoopRunnable?.let { durationLoopHandler?.postDelayed(it, delayMs) }
        }

        durationLoopRunnable?.let { durationLoopHandler?.post(it) }
    }

    private fun stopDurationLoopHandler() {
        durationLoopRunnable?.let { durationLoopHandler?.removeCallbacks(it) }
    }

    private fun toggleCoverArt(show: Boolean) {
        if (show) {
            binding.playerCoverArt.visibility = View.VISIBLE
        } else {
            binding.playerCoverArt.visibility = View.GONE
        }
    }

    private fun toggleMetadata(show: Boolean, applyTo: Int = -1) {
        // "applyTo" is an integer with the value of -1, 0, or 1
        // The following integer is equivalent to:
        //      -1 = Applies to both title and caption
        //      0 = Only applies to title
        //      1 = Only applies to caption
        when (applyTo) {
            -1 -> {
                if (show) {
                    binding.playerTitle.visibility = View.VISIBLE
                    binding.playerCaption.visibility = View.VISIBLE
                } else {
                    binding.playerTitle.visibility = View.GONE
                    binding.playerCaption.visibility = View.GONE
                }
            }
            0 -> {
                if (show) {
                    binding.playerTitle.visibility = View.VISIBLE
                } else {
                    binding.playerTitle.visibility = View.GONE
                }
            }
            1 -> {
                if (show) {
                    binding.playerCaption.visibility = View.VISIBLE
                } else {
                    binding.playerCaption.visibility = View.GONE
                }
            }
        }
    }

    companion object {
        const val TAG = "MusicActivity"
    }
}