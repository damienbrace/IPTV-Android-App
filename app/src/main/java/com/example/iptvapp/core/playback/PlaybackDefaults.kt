package com.example.iptvapp.core.playback

import androidx.media3.common.C

object PlaybackDefaults {
    const val USER_AGENT = "StreamHubTV/1.0"
    const val CONNECT_TIMEOUT_MS = 8_000
    const val READ_TIMEOUT_MS = 12_000
    const val LIVE_MIN_BUFFER_MS = 1_500
    const val LIVE_MAX_BUFFER_MS = 8_000
    const val LIVE_PLAYBACK_BUFFER_MS = 700
    const val LIVE_REBUFFER_MS = 1_200
    const val SEEK_BACK_INCREMENT_MS = 10_000L
    const val SEEK_FORWARD_INCREMENT_MS = 10_000L
    const val DEFAULT_STREAM_TYPE = C.CONTENT_TYPE_HLS
}
