package com.appnova.novatube.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import com.appnova.novatube.youtube.StreamDetails

object NovaPlayerHolder {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    @Volatile
    private var exoPlayer: ExoPlayer? = null

    private var pendingSeekListener: Player.Listener? = null

    @OptIn(UnstableApi::class)
    fun getOrCreate(context: Context): ExoPlayer {
        return exoPlayer ?: synchronized(this) {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 12_000,
                    /* maxBufferMs = */ 45_000,
                    /* bufferForPlaybackMs = */ 350,
                    /* bufferForPlaybackAfterRebufferMs = */ 900
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            exoPlayer ?: ExoPlayer.Builder(context.applicationContext)
                .setLoadControl(loadControl)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build()
                .also { exoPlayer = it }
        }
    }

    @OptIn(UnstableApi::class)
    fun playStream(
        context: Context,
        details: StreamDetails,
        selectedVideoUrl: String?,
        isMuxedStream: Boolean = false,
        speed: Float,
        loop: Boolean,
        startPositionMs: Long = 0L
    ) {
        val player = getOrCreate(context)
        pendingSeekListener?.let { player.removeListener(it) }
        pendingSeekListener = null

        val metadata = MediaMetadata.Builder()
            .setTitle(details.title)
            .setArtist(details.channelName)
            .setArtworkUri(Uri.parse(details.thumbnailUrl))
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(12_000)

        val factory = DefaultMediaSourceFactory(context.applicationContext)
            .setDataSourceFactory(httpDataSourceFactory)

        val videoUrl = selectedVideoUrl ?: details.muxedUrl ?: details.videoOnlyUrl
        val audioUrl = details.audioOnlyUrl

        player.stop()

        // If using a single muxed stream (e.g., 360p Fast Start or HLS), play directly
        // with 1 HTTP connection for instant startup. Otherwise merge adaptive video + M4A audio.
        if (!isMuxedStream && selectedVideoUrl != null && selectedVideoUrl != details.muxedUrl && audioUrl != null) {
            val videoItem = MediaItem.Builder()
                .setUri(selectedVideoUrl)
                .setMediaId(details.videoId)
                .setMediaMetadata(metadata)
                .build()
            val audioItem = MediaItem.Builder()
                .setUri(audioUrl)
                .build()
            val merged = MergingMediaSource(
                factory.createMediaSource(videoItem),
                factory.createMediaSource(audioItem)
            )
            player.setMediaSource(merged)
        } else {
            val singleUrl = videoUrl ?: audioUrl ?: return
            val item = MediaItem.Builder()
                .setUri(singleUrl)
                .setMediaId(details.videoId)
                .setMediaMetadata(metadata)
                .build()
            player.setMediaSource(factory.createMediaSource(item))
        }

        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setPlaybackSpeed(speed)

        if (startPositionMs > 0L) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        player.removeListener(this)
                        if (pendingSeekListener === this) {
                            pendingSeekListener = null
                        }
                        player.seekTo(startPositionMs)
                        player.play()
                    }
                }
            }
            pendingSeekListener = listener
            player.addListener(listener)
        }

        player.prepare()
        player.playWhenReady = true
        player.play()
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
