package io.github.spir0th.music.activities

import android.animation.LayoutTransition
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
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
import io.github.spir0th.music.utils.adjustPaddingForSystemBarInsets
import io.github.spir0th.music.utils.setImmersiveMode
import io.github.spir0th.music.utils.visibilityChanged

class MusicActivity : AppCompatActivity(), Player.Listener {
    private lateinit var binding: ActivityMusicBinding
    private lateinit var preferences: SharedPreferences
    private val loopHandler: Handler? = Looper.myLooper()?.let { Handler(it) }
    private var loopRunnable: Runnable? = null
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Inflate activity view using ViewBinding
        binding = ActivityMusicBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

        // Make player title's marquee text effect work
        binding.playerTitle.isSelected = true

        // Register listeners for activity callbacks / player controls
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        // Toggle immersive mode if any of these checks are true
        if (preferences.getBoolean("immersive", false)) {
            WindowCompat.getInsetsController(window, window.decorView).setImmersiveMode(true)
        } else if (preferences.getBoolean("immersive_on_landscape", true)) {
            // Depend on the screen orientation instead if respective preference is ticked off
            // Immersive mode may also not be enabled if "immersive_on_landscape" is turned off
            WindowCompat.getInsetsController(window, window.decorView)
                .setImmersiveMode(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        }
        // Toggle player control transitions if true
        if (preferences.getBoolean("transitions", true)) {
            binding.playerControls.layoutTransition = LayoutTransition()
        }
        // Register player control listeners
        binding.playerIndicator.visibilityChanged { view ->
            val background = (binding.playerDim.background as TransitionDrawable).apply {
                resetTransition()
            }
            when (view.visibility) {
                View.VISIBLE -> {
                    background.startTransition(100)
                }
                View.GONE -> {
                    background.reverseTransition(100)
                }
                View.INVISIBLE -> {
                    // Do nothing
                }
            }
        }
        binding.playerPlayback.setOnClickListener {
            if (mediaController?.isPlaying == true) {
                mediaController!!.pause()
            } else {
                mediaController?.play()
            }
        }
        binding.playerSlider.setLabelFormatter { value ->
            val duration = mediaController?.duration ?: 0
            parsePlaybackDurationToString(((value + 0.0) * duration).toLong())
        }
        binding.playerSlider.addOnSliderTouchListener(object: Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                if (preferences.getBoolean("transitions", true)) {
                    binding.playerSeekPosition.visibility = View.GONE
                }

                stopLoopHandler()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                binding.playerSeekPosition.visibility = View.VISIBLE
                val duration = mediaController?.duration ?: 0
                mediaController?.seekTo(((slider.value + 0.0) * duration).toLong())
                startLoopHandler()
            }
        })
        // Connect activity to media session
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(this)
            updateFromServiceIfLoaded()
            handleIncomingIntents()
        },
            MoreExecutors.directExecutor()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaController?.removeListener(this)
        mediaController?.release()
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        updateMetadataUI(mediaMetadata)
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        super.onIsLoadingChanged(isLoading)
        updateIndicatorUI(isLoading)
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

    private fun parsePlaybackDurationToString(milliseconds: Long): String {
        val microseconds = milliseconds / 1000
        val minutes = microseconds / 60
        val seconds = microseconds % 60

        if (microseconds >= 360) {
            val hours = microseconds / 360
            return getString(R.string.player_seek_format_hrs, hours, minutes, String.format("%1$02d", seconds))
        }

        return getString(R.string.player_seek_format, minutes, String.format("%1$02d", seconds))
    }

    private fun updateMetadataUI(metadata: MediaMetadata = mediaController?.mediaMetadata ?: MediaMetadata.EMPTY) {
        val artworkData = metadata.artworkData ?: byteArrayOf(1)
        val artworkBitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)

        if (preferences.getBoolean("metadata", true) && artworkBitmap != null) {
            Glide.with(this)
                .load(artworkBitmap)
                .transition(withCrossFade())
                .into(binding.playerCoverArt)

            binding.playerCoverArt.visibility = View.VISIBLE
        } else {
            binding.playerCoverArt.visibility = View.GONE
        }
        if (preferences.getBoolean("metadata", true) && metadata.title?.isNotEmpty() == true) {
            binding.playerTitle.text = metadata.title
        } else if (mediaController?.currentMediaItem?.localConfiguration?.uri != null) {
            // if metadata title is unavailable, we use Uri.lastPathSegment instead
            // or use the app name and author if getTitleFromUri is also unavailable
            val uri = mediaController?.currentMediaItem?.localConfiguration?.uri
            binding.playerTitle.text = uri?.lastPathSegment
        }
        if (preferences.getBoolean("metadata", true) && metadata.artist?.isNotEmpty() == true) {
            binding.playerCaption.text = metadata.artist
        } else {
            binding.playerCaption.text = binding.playerTitle.text
            binding.playerTitle.text = String()
        }
    }

    private fun updateIndicatorUI(isLoading: Boolean = mediaController?.isLoading ?: false) {
        if (isLoading) {
            binding.playerIndicator.visibility = View.VISIBLE
        } else {
            binding.playerIndicator.visibility = View.GONE
        }
    }

    private fun updatePlaybackDurationUI(position: Long = mediaController?.currentPosition ?: 0) {
        val duration = mediaController?.duration ?: 0
        var positionFloat = (position + 0.0f) / duration // convert position into float (pain)

        if (positionFloat > 1.0f) {
            positionFloat = 1.0f
        } else if (positionFloat < 0.0f) {
            positionFloat = 0.0f
        }

        binding.playerSlider.value = positionFloat
        binding.playerSeekPosition.text = parsePlaybackDurationToString(position)
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
            startLoopHandler()
        } else {
            stopLoopHandler()
        }

        Glide.with(this).load(drawable).into(binding.playerPlayback)
    }

    private fun updateFromServiceIfLoaded() {
        if (mediaController?.currentMediaItem == null) {
            return
        }

        Log.i(TAG, "Started update from service if loaded")
        updateIndicatorUI()
        updateMetadataUI()
        updatePlaybackStateUI()
        updatePlaybackDurationUI()
    }

    private fun handleIncomingIntents() {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent?.data?.let {
                    val item = MediaItem.fromUri(it)
                    Log.i(TAG, "Playing URI from intent: $it")
                    intent?.data = null
                    mediaController?.setMediaItem(item)
                    mediaController?.prepare()
                }
            }
        }
    }

    private fun startLoopHandler() {
        val delayMs = preferences.getString("time_get_duration", "0")!!.toLong()

        loopRunnable = Runnable {
            updatePlaybackDurationUI()
            loopRunnable?.let { loopHandler?.postDelayed(it, delayMs) }
        }

        loopRunnable?.let { loopHandler?.post(it) }
    }

    private fun stopLoopHandler() {
        loopRunnable?.let { loopHandler?.removeCallbacks(it) }
    }

    companion object {
        const val TAG = "MusicActivity"
    }
}