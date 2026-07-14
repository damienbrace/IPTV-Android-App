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
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy

@UnstableApi
class IptvPlayerFactory(private val context: Context) {
    fun createLivePlayer(format: LiveStreamFormat = LiveStreamFormat.HLS): ExoPlayer {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(PlaybackDefaults.USER_AGENT)
            .setConnectTimeoutMs(PlaybackDefaults.CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(PlaybackDefaults.READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(
                DefaultLoadErrorHandlingPolicy(PlaybackDefaults.LIVE_MIN_LOAD_RETRY_COUNT)
            )

        val minBufferMs = when (format) {
            LiveStreamFormat.HLS -> PlaybackDefaults.HLS_MIN_BUFFER_MS
            LiveStreamFormat.MPEG_TS -> PlaybackDefaults.MPEG_TS_MIN_BUFFER_MS
        }
        val maxBufferMs = when (format) {
            LiveStreamFormat.HLS -> PlaybackDefaults.HLS_MAX_BUFFER_MS
            LiveStreamFormat.MPEG_TS -> PlaybackDefaults.MPEG_TS_MAX_BUFFER_MS
        }
        val rebufferMs = when (format) {
            LiveStreamFormat.HLS -> PlaybackDefaults.HLS_REBUFFER_MS
            LiveStreamFormat.MPEG_TS -> PlaybackDefaults.MPEG_TS_REBUFFER_MS
        }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                PlaybackDefaults.LIVE_PLAYBACK_BUFFER_MS,
                rebufferMs
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
        channelName: String,
        format: LiveStreamFormat = LiveStreamFormat.HLS
    ): MediaItem {
        val resolvedStreamUrl = resolveLiveStreamUrl(streamUrl, format)
        val isHls = format == LiveStreamFormat.HLS
        val builder = MediaItem.Builder()
            .setUri(resolvedStreamUrl)
            .setMediaId(channelId)
            .setMimeType(MimeTypes.APPLICATION_M3U8.takeIf { isHls })
            .setTag(channelName)

        if (isHls) {
            builder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(PlaybackDefaults.LIVE_TARGET_OFFSET_MS)
                    .setMinOffsetMs(PlaybackDefaults.LIVE_MIN_OFFSET_MS)
                    .setMaxOffsetMs(PlaybackDefaults.LIVE_MAX_OFFSET_MS)
                    .setMinPlaybackSpeed(PlaybackDefaults.LIVE_MIN_PLAYBACK_SPEED)
                    .setMaxPlaybackSpeed(PlaybackDefaults.LIVE_MAX_PLAYBACK_SPEED)
                    .build()
            )
        }

        return builder.build()
    }
}
