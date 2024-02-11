package io.github.spir0th.music.activities

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.MoreExecutors
import io.github.spir0th.music.R
import io.github.spir0th.music.databinding.ActivityMusicBinding
import io.github.spir0th.music.services.PlaybackService
import io.github.spir0th.music.utils.MediaUtils
import io.github.spir0th.music.utils.UiUtils

class MusicPersistentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMusicBinding
    private lateinit var preferences: SharedPreferences
    private val durationLoopHandler: Handler? = Looper.myLooper()?.let { Handler(it) }
    private var durationLoopRunnable: Runnable? = null
    private var mediaController: MediaController? = null
    private var mediaListener: Player.Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        UiUtils.setImmersiveMode(window, preferences.getBoolean("immersive", false))

        // Inflate activity view using ViewBinding
        binding = ActivityMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UiUtils.adjustSystemBarInsetsForView(binding.content, top=true, bottom=true)

        // Register click listeners for player controls / activity callbacks
        registerControlsClickListener()
        registerCallbacks()
    }

    override fun onStart() {
        super.onStart()

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            binding.playerControls.visibility = View.VISIBLE
            updateFromMediaIfLoaded()
            registerControllerListener()
            handleIncomingIntents()
                                     },
            MoreExecutors.directExecutor()
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterControllerListener()
        mediaController?.release()
    }

    private fun updateMetadataUI(metadata: MediaMetadata? = mediaController?.mediaMetadata) {
        updateCoverArtUI() // Fetch cover art, visibility will be handled by this function.

        if (!metadata?.title.isNullOrEmpty()) {
            binding.playerTitle.text = metadata?.title
        } else {
            // if metadata title is unavailable, we use MediaUtils.getTitleFromContentUri instead
            // or use the app name and author if getTitleFromUri is also unavailable
            val uri = mediaController?.currentMediaItem?.localConfiguration?.uri

            if (uri != null) {
                binding.playerTitle.text = MediaUtils.getFilenameFromUri(uri)
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

    private fun updateCoverArtUI(uri: Uri? = mediaController?.currentMediaItem?.localConfiguration?.uri) {
        val cover = MediaUtils.getCoverArtFromUri(this, uri)

        if (cover != null) {
            if (preferences.getBoolean("color_extract", true)) {
                Palette.from(cover).generate {
                    val color = it?.getVibrantColor(Color.WHITE)!!
                    binding.playerSeekbar.progressTintList = ColorStateList.valueOf(color)
                }
            } else {
                binding.playerSeekbar.progressTintList = ColorStateList.valueOf(Color.WHITE)
            }
            Glide.with(this)
                .load(cover)
                .transition(withCrossFade())
                .into(binding.playerCoverArt)

            showCoverArt()
        } else {
            binding.playerSeekbar.progressTintList = ColorStateList.valueOf(Color.WHITE)
            hideCoverArt() // always hide cover art if there is nothing to provide with.
        }
    }

    private fun updatePlaybackDurationUI(position: Long? = mediaController?.currentPosition) {
        binding.playerSeekbar.max = mediaController?.duration?.toInt() ?: 0
        binding.playerSeekbar.progress = position?.toInt() ?: 0

        // parse current position into text
        val totalSeconds = position!! / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        if (totalSeconds >= 360) {
            val hours = totalSeconds / 360
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

        Glide.with(this@MusicPersistentActivity).load(drawable).into(binding.playerPlayback)
    }

    private fun handleIncomingIntents() {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                if (intent.type?.startsWith("audio/") == true) {
                    intent?.data?.let {
                        val item = MediaItem.fromUri(it)

                        if (mediaController?.currentMediaItem != item) {
                            Log.i(TAG, "Playing audio from incoming persistent intent data: $it")
                            mediaController?.stop()
                            mediaController?.addMediaItem(item)
                            mediaController?.seekTo(mediaController!!.mediaItemCount - 1, 0)
                            mediaController?.play()
                        } else {
                            Log.w(TAG, "Incoming intent data received but is already added into queue.")
                        }
                    }
                }
            }
        }
    }

    private fun updateFromMediaIfLoaded() {
        if (mediaController?.currentMediaItem == null) {
            return
        }

        updateMetadataUI()
        updatePlaybackStateUI()
        updatePlaybackSkipUI()
        updatePlaybackDurationUI()
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

    private fun registerControllerListener() {
        mediaListener = object: Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                intent?.data = mediaItem?.localConfiguration?.uri
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
        }

        mediaController?.addListener(mediaListener as Player.Listener)
    }

    private fun unregisterControllerListener() {
        mediaListener?.let { mediaController?.removeListener(it) }
    }

    private fun registerControlsClickListener() {
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
        binding.playerSeekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                stopDurationLoopHandler()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                startDurationLoopHandler()

                if (seekBar != null) {
                    mediaController?.seekTo(seekBar.progress.toLong())
                }
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
        const val TAG = "MusicPersistentActivity"
    }
}