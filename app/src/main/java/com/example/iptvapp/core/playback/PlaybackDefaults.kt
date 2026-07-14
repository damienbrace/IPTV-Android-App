package com.example.iptvapp.core.playback

import androidx.media3.common.C

object PlaybackDefaults {
    const val USER_AGENT = "StreamHubTV/1.0"
    const val CONNECT_TIMEOUT_MS = 8_000
    const val READ_TIMEOUT_MS = 12_000

    // The provider exposes six roughly 10-second HLS segments, so stay three segments behind live.
    const val HLS_MIN_BUFFER_MS = 25_000
    const val HLS_MAX_BUFFER_MS = 45_000
    const val HLS_REBUFFER_MS = 6_000
    const val MPEG_TS_MIN_BUFFER_MS = 6_000
    const val MPEG_TS_MAX_BUFFER_MS = 18_000
    const val MPEG_TS_REBUFFER_MS = 3_000
    const val LIVE_PLAYBACK_BUFFER_MS = 1_000
    const val LIVE_MIN_LOAD_RETRY_COUNT = 8
    const val LIVE_TARGET_OFFSET_MS = 30_000L
    const val LIVE_MIN_OFFSET_MS = 20_000L
    const val LIVE_MAX_OFFSET_MS = 45_000L
    const val LIVE_MIN_PLAYBACK_SPEED = 0.98f
    const val LIVE_MAX_PLAYBACK_SPEED = 1.02f
    const val SEEK_BACK_INCREMENT_MS = 10_000L
    const val SEEK_FORWARD_INCREMENT_MS = 10_000L
    const val DEFAULT_STREAM_TYPE = C.CONTENT_TYPE_HLS
}
