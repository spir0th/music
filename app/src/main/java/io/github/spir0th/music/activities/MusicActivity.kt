package io.github.spir0th.music.activities

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import io.github.spir0th.music.R
import io.github.spir0th.music.databinding.ActivityMusicBinding
import io.github.spir0th.music.utils.MediaUtils
import io.github.spir0th.music.utils.UiUtils

class MusicActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMusicBinding
    private lateinit var preferences: SharedPreferences
    private val loopHandler: Handler? = Looper.myLooper()?.let { Handler(it) }
    private var loopRunnable: Runnable? = null
    private var audioUri: Uri? = null
    private var player: MediaPlayer? = null

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

        // Initialize media player and handle incoming intents
        initializePlayer()
        handleIncomingIntents()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyPlayer()
    }

    private fun initializePlayer() {
        player = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build())
        }
        player?.setOnErrorListener { _, _, _ ->
            exitPlayerFromError()
            return@setOnErrorListener true
        }
        player?.setOnPreparedListener {
            binding.playerControls.visibility = View.VISIBLE
            fetchCoverArt(audioUri)
            fetchMetadata(audioUri)
            startLoopHandler()
            it.start()
        }
    }

    private fun destroyPlayer() {
        stopLoopHandler()
        player?.release()
        player = null
    }

    private fun exitPlayerFromError() {
        Toast.makeText(applicationContext, R.string.player_file_error, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun fetchCoverArt(uri: Uri?) {
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
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.playerCoverArt)

            showCoverArt()
        } else {
            binding.playerSeekbar.progressTintList = ColorStateList.valueOf(Color.WHITE)
            hideCoverArt() // always hide cover art if there is nothing to provide with.
        }
    }

    private fun fetchMetadata(uri: Uri?) {
        binding.playerTitle.text = MediaUtils.getTitleFromUri(this, uri)
        binding.playerCaption.text = MediaUtils.getArtistFromUri(this, uri)
    }

    private fun updatePlaybackDurationUI() {
        binding.playerSeekbar.max = player?.duration ?: 0
        binding.playerSeekbar.progress = player?.currentPosition ?: 0

        // parse current position into text
        val totalSeconds = player?.currentPosition!! / 1000
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
        // Always disable player skip controls
        binding.playerSkipPrevious.visibility = View.GONE
        binding.playerSkipNext.visibility = View.GONE
    }

    private fun updatePlaybackStateUI() {
        val drawable: Int = if (player?.isPlaying == true) {
            Log.v(MusicPersistentActivity.TAG, "PlaybackState set to STATE_PLAYING")
            R.drawable.baseline_pause_24
        } else {
            Log.v(MusicPersistentActivity.TAG, "PlaybackState set to STATE_PAUSED")
            R.drawable.baseline_play_arrow_24
        }

        Glide.with(this).load(drawable).into(binding.playerPlayback)
    }

    private fun handleIncomingIntents() {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                if (intent.type?.startsWith("audio/") == true) {
                    intent?.data?.let {
                        if (intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION == Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION &&
                            preferences.getBoolean("use_persistence", true)) {
                            // Let MusicPersistentActivity handle intents with persistable URIs
                            // (Make sure persistence playback is enabled)
                            val newIntent = intent!!.apply {
                                setClass(this@MusicActivity, MusicPersistentActivity::class.java)
                            }

                            startActivity(newIntent)
                            finish()
                        } else {
                            Log.i(TAG, "Playing audio from incoming intent data: $it")
                            audioUri = it
                            player?.setDataSource(this@MusicActivity, it)

                            try {
                                player?.prepareAsync()
                            } catch (_: IllegalStateException) {
                                exitPlayerFromError()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startLoopHandler() {
        loopRunnable = Runnable {
            updatePlaybackDurationUI()
            updatePlaybackSkipUI()
            updatePlaybackStateUI()
            loopRunnable?.let { loopHandler?.post(it) }
        }

        loopRunnable?.let { loopHandler?.post(it) }
    }

    private fun stopLoopHandler() {
        loopRunnable?.let { loopHandler?.removeCallbacks(it) }
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

    private fun registerControlsClickListener() {
        binding.playerPlayback.setOnClickListener {
            if (player?.isPlaying == false) {
                player?.start()
            } else {
                player?.pause()
            }
        }
        binding.playerSeekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                stopLoopHandler()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                startLoopHandler()
                seekBar?.progress?.let { player?.seekTo(it) }
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