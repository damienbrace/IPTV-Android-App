package com.example.iptvapp.core.playback

import android.os.SystemClock
import androidx.media3.common.Player

data class PlaybackTelemetrySnapshot(
    val channelId: String? = null,
    val startupMs: Long? = null,
    val rebufferCount: Int = 0,
    val channelSwitchCount: Int = 0,
    val errorCount: Int = 0,
    val lastError: String? = null
)

class PlaybackTelemetryRecorder {
    private var currentChannelId: String? = null
    private var loadStartedAtMs: Long = 0L
    private var readyForCurrentItem = false
    private var lastState: Int = Player.STATE_IDLE
    private var snapshot = PlaybackTelemetrySnapshot()

    fun onChannelLoad(channelId: String): PlaybackTelemetrySnapshot {
        val previousChannelId = currentChannelId
        currentChannelId = channelId
        loadStartedAtMs = SystemClock.elapsedRealtime()
        readyForCurrentItem = false
        lastState = Player.STATE_BUFFERING
        snapshot = snapshot.copy(
            channelId = channelId,
            startupMs = null,
            channelSwitchCount = snapshot.channelSwitchCount + if (previousChannelId == null || previousChannelId == channelId) 0 else 1,
            lastError = null
        )
        return snapshot
    }

    fun onPlaybackStateChanged(state: Int): PlaybackTelemetrySnapshot {
        if (state == Player.STATE_BUFFERING && readyForCurrentItem && lastState != Player.STATE_BUFFERING) {
            snapshot = snapshot.copy(rebufferCount = snapshot.rebufferCount + 1)
        }

        if (state == Player.STATE_READY && !readyForCurrentItem) {
            readyForCurrentItem = true
            snapshot = snapshot.copy(startupMs = SystemClock.elapsedRealtime() - loadStartedAtMs)
        }

        lastState = state
        return snapshot
    }

    fun onError(message: String): PlaybackTelemetrySnapshot {
        snapshot = snapshot.copy(
            errorCount = snapshot.errorCount + 1,
            lastError = message
        )
        return snapshot
    }

    fun snapshot(): PlaybackTelemetrySnapshot = snapshot
}
