package io.github.spir0th.music.activities

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import io.github.spir0th.music.utils.MediaUtils
import io.github.spir0th.music.utils.TimeUtils
import io.github.spir0th.music.utils.UiUtils

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
        UiUtils.setImmersiveMode(window, preferences.getBoolean("immersive", false))

        // Inflate activity view using ViewBinding
        binding = ActivityMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UiUtils.adjustSystemBarInsetsForView(binding.content, top=true, bottom=true)

        // Register listeners for player controls / activity callbacks
        registerControlListeners()
        registerCallbacks()
    }

    override fun onStart() {
        super.onStart()
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
            mediaController?.pause()
        }

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

    private fun updateMetadataUI(metadata: MediaMetadata? = mediaController?.mediaMetadata) {
        updateCoverArtUI() // Fetch cover art, visibility will be handled by this function.

        if (!metadata?.title.isNullOrEmpty()) {
            binding.playerTitle.text = metadata?.title
        } else {
            // if metadata title is unavailable, we use Uri.lastPathSegment instead
            // or use the app name and author if getTitleFromUri is also unavailable
            val uri = mediaController?.currentMediaItem?.localConfiguration?.uri

            if (uri != null) {
                binding.playerTitle.text = uri.lastPathSegment
                binding.playerCaption.text = String()
            } else {
                binding.playerTitle.text = getString(R.string.app_name)
                binding.playerCaption.text = getString(R.string.app_author)
            }
        }
        if (!metadata?.artist.isNullOrEmpty()) {
            binding.playerCaption.text = metadata?.artist
        }
    }

    private fun updateCoverArtUI(artworkData: ByteArray? = mediaController?.mediaMetadata?.artworkData) {
        val bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData!!.size)

        if (bitmap != null) {
            Glide.with(this)
                .load(bitmap)
                .transition(withCrossFade())
                .into(binding.playerCoverArt)

            showCoverArt()
        } else {
            hideCoverArt() // always hide cover art if there is nothing to provide with.
        }
    }

    private fun updatePlaybackDurationUI(position: Long? = mediaController?.currentPosition) {
        // parse current position into float (pain)
        val duration = mediaController?.duration!!
        var positionFloat = position?.plus(0.0f)?.div(duration)!!

        if (positionFloat > 1.0f) {
            positionFloat = 1.0f
        } else if (positionFloat < 0.0f) {
            positionFloat = 0.0f
        }

        binding.playerSlider.value = positionFloat

        // parse current position into text
        val microseconds = TimeUtils.convertMillisecondsToMicroseconds(position)
        val minutes = TimeUtils.convertMicrosecondsToMinutes(microseconds)
        val seconds = TimeUtils.convertMicrosecondsToSeconds(microseconds)

        if (microseconds >= 360) {
            val hours = TimeUtils.convertMicrosecondsToHours(microseconds)
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
                            Log.w(TAG, "Uri $it does not have persistence, making it persistent")
                            item = MediaItem.fromUri(MediaUtils.generateMediaPersistence(this, it))
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

    private fun doCleanupBeforeService() {
        if (mediaController?.mediaItemCount != 0) {
            return
        }

        Log.i(TAG, "Cleaning up audio persistence")
        MediaUtils.cleanMediaPersists(this)
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

    private fun showCoverArt() {
        binding.playerCoverArt.visibility = View.VISIBLE
        binding.playerCaption.setPadding(0, 0, 0, 4)
        binding.playerControls.setPadding(0, 4, 0, 0)
    }

    private fun hideCoverArt() {
        binding.playerCoverArt.visibility = View.GONE
        binding.playerCaption.setPadding(0, 0, 0, 0)
        binding.playerControls.setPadding(0, 0, 0, 0)
    }

    private fun registerControlListeners() {
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
            val microseconds = TimeUtils.convertMillisecondsToMicroseconds(milliseconds)
            val minutes = TimeUtils.convertMicrosecondsToMinutes(microseconds)
            val seconds = TimeUtils.convertMicrosecondsToSeconds(microseconds)

            if (microseconds >= 360) {
                val hours = TimeUtils.convertMicrosecondsToHours(microseconds)
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

    private fun registerCallbacks() {
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    companion object {
        const val TAG = "MusicActivity"
    }
}