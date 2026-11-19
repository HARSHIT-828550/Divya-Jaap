package com.example.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.example.MainActivity
import com.example.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

@OptIn(UnstableApi::class)
class MantraPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var remainingRepeats: Int? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val left = remainingRepeats
                    if (left == null) {
                        // Infinite loop handled by REPEAT_MODE_ONE, fallback safety
                        exoPlayer.seekTo(0)
                        exoPlayer.play()
                    } else if (left > 1) {
                        remainingRepeats = left - 1
                        exoPlayer.seekTo(0)
                        exoPlayer.play()
                    }
                }
            }
        })

        player = exoPlayer

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopButton = CommandButton.Builder(CommandButton.ICON_STOP)
            .setDisplayName("Stop")
            .setSessionCommand(SessionCommand(ACTION_STOP_PLAYBACK, Bundle.EMPTY))
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.notification_channel_playback)
            .build()
        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .setMediaButtonPreferences(listOf(stopButton))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(SessionCommand(ACTION_STOP_PLAYBACK, Bundle.EMPTY))
                        .add(SessionCommand(ACTION_SET_REPEAT_COUNT, Bundle.EMPTY))
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    return when (customCommand.customAction) {
                        ACTION_STOP_PLAYBACK -> {
                            remainingRepeats = 0
                            exoPlayer.stop()
                            exoPlayer.clearMediaItems()
                            stopSelf()
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        ACTION_SET_REPEAT_COUNT -> {
                            val count = args.getInt(EXTRA_REPEAT_COUNT, -1)
                            remainingRepeats = if (count <= 0) null else count
                            exoPlayer.repeatMode = if (count <= 0) {
                                Player.REPEAT_MODE_ONE
                            } else {
                                Player.REPEAT_MODE_OFF
                            }
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        else -> super.onCustomCommand(session, controller, customCommand, args)
                    }
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaSession?.player
        if (currentPlayer == null || !currentPlayer.playWhenReady || currentPlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_playback),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Ongoing Mantra Guru audio controls"
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "divya_jaap_playback"
        const val ACTION_STOP_PLAYBACK = "com.example.audio.STOP_PLAYBACK"
        const val ACTION_SET_REPEAT_COUNT = "com.example.audio.SET_REPEAT_COUNT"
        const val EXTRA_REPEAT_COUNT = "extra_repeat_count"
    }
}

data class BackgroundPlaybackState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentTitle: String? = null
)

class MantraBackgroundAudioClient(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(BackgroundPlaybackState())
    val state: StateFlow<BackgroundPlaybackState> = _state.asStateFlow()

    fun playAudioFile(
        file: File,
        title: String,
        subtitle: String,
        repeatCount: Int?,
        speed: Float
    ) {
        ensureController { mediaController ->
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist("Divya Jaap Mantra Guru")
                .setSubtitle(subtitle)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(file))
                .setMediaMetadata(metadata)
                .build()

            val bundle = Bundle().apply {
                putInt(MantraPlaybackService.EXTRA_REPEAT_COUNT, repeatCount ?: -1)
            }
            mediaController.sendCustomCommand(
                SessionCommand(MantraPlaybackService.ACTION_SET_REPEAT_COUNT, Bundle.EMPTY),
                bundle
            )
            mediaController.setMediaItem(mediaItem)
            mediaController.playbackParameters = PlaybackParameters(speed.coerceIn(0.5f, 2.0f))
            mediaController.prepare()
            mediaController.play()
            _state.value = BackgroundPlaybackState(
                isConnected = true,
                isPlaying = true,
                currentTitle = title
            )
        }
    }

    fun pause() {
        controller?.pause()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun resume() {
        controller?.play()
        _state.value = _state.value.copy(isPlaying = true)
    }

    fun stop() {
        controller?.let { mc ->
            mc.sendCustomCommand(
                SessionCommand(MantraPlaybackService.ACTION_STOP_PLAYBACK, Bundle.EMPTY),
                Bundle.EMPTY
            )
            mc.stop()
            mc.clearMediaItems()
        }
        _state.value = _state.value.copy(isPlaying = false, currentTitle = null)
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    private fun ensureController(onReady: (MediaController) -> Unit) {
        controller?.let {
            onReady(it)
            return
        }
        val token = SessionToken(context, ComponentName(context, MantraPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val mc = future.get()
                    mc.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _state.value = _state.value.copy(
                                isConnected = true,
                                isPlaying = isPlaying
                            )
                        }
                    })
                    controller = mc
                    _state.value = _state.value.copy(isConnected = true)
                    onReady(mc)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}
