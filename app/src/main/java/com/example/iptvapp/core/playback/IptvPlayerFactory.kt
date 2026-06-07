package com.example.iptvapp.core.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

@UnstableApi
class IptvPlayerFactory(private val context: Context) {
    fun createLivePlayer(): ExoPlayer {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(PlaybackDefaults.USER_AGENT)
            .setConnectTimeoutMs(PlaybackDefaults.CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(PlaybackDefaults.READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                PlaybackDefaults.LIVE_MIN_BUFFER_MS,
                PlaybackDefaults.LIVE_MAX_BUFFER_MS,
                PlaybackDefaults.LIVE_PLAYBACK_BUFFER_MS,
                PlaybackDefaults.LIVE_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(PlaybackDefaults.SEEK_BACK_INCREMENT_MS)
            .setSeekForwardIncrementMs(PlaybackDefaults.SEEK_FORWARD_INCREMENT_MS)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true
                )
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
    }

    fun buildLiveMediaItem(
        streamUrl: String,
        channelId: String,
        channelName: String
    ): MediaItem {
        return MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(channelId)
            .setMimeType(MimeTypes.APPLICATION_M3U8.takeIf { streamUrl.endsWith(".m3u8", ignoreCase = true) })
            .setTag(channelName)
            .build()
    }
}
