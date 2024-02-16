package io.github.spir0th.music.services

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.preference.PreferenceManager
import com.google.common.util.concurrent.ListenableFuture
import io.github.spir0th.music.BuildConfig
import io.github.spir0th.music.activities.MusicActivity

class PlaybackService : MediaSessionService(), MediaSession.Callback {
    private lateinit var preferences: SharedPreferences
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val intent = Intent(this, MusicActivity::class.java)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, preferences.getBoolean("audio_focus", true))
            .build()
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
            .setCallback(this)
            .build()

        mediaSession?.player?.playWhenReady = true
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val player = mediaSession?.player!!

        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        if (controller.packageName != BuildConfig.APPLICATION_ID) {
            return MediaSession.ConnectionResult.reject()
        }

        return super.onConnect(session, controller)
    }

    @UnstableApi
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        return super.onPlaybackResumption(mediaSession, controller)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
}