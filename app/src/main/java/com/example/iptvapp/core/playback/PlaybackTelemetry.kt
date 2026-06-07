package com.example.iptvapp.core.playback

import android.os.SystemClock
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackTelemetrySnapshot(
    val channelId: String? = null,
    val channelName: String? = null,
    val startupMs: Long? = null,
    val rebufferCount: Int = 0,
    val channelSwitchCount: Int = 0,
    val errorCount: Int = 0,
    val lastError: String? = null
)

object PlaybackDiagnosticsStore {
    private val _recentSnapshots = MutableStateFlow<List<PlaybackTelemetrySnapshot>>(emptyList())
    val recentSnapshots: StateFlow<List<PlaybackTelemetrySnapshot>> = _recentSnapshots.asStateFlow()

    fun record(snapshot: PlaybackTelemetrySnapshot) {
        _recentSnapshots.value = listOf(snapshot) + _recentSnapshots.value.filterNot {
            it.channelId == snapshot.channelId && it.channelName == snapshot.channelName
        }
            .take(19)
    }
}

class PlaybackTelemetryRecorder(
    private val timeProvider: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private var currentChannelId: String? = null
    private var loadStartedAtMs: Long = 0L
    private var readyForCurrentItem = false
    private var lastState: Int = Player.STATE_IDLE
    private var snapshot = PlaybackTelemetrySnapshot()

    fun onChannelLoad(channelId: String, channelName: String): PlaybackTelemetrySnapshot {
        val previousChannelId = currentChannelId
        currentChannelId = channelId
        loadStartedAtMs = timeProvider()
        readyForCurrentItem = false
        lastState = Player.STATE_BUFFERING
        snapshot = snapshot.copy(
            channelId = channelId,
            channelName = channelName,
            startupMs = null,
            channelSwitchCount = snapshot.channelSwitchCount + if (previousChannelId == null || previousChannelId == channelId) 0 else 1,
            lastError = null
        )
        PlaybackDiagnosticsStore.record(snapshot)
        return snapshot
    }

    fun onPlaybackStateChanged(state: Int): PlaybackTelemetrySnapshot {
        if (state == Player.STATE_BUFFERING && readyForCurrentItem && lastState != Player.STATE_BUFFERING) {
            snapshot = snapshot.copy(rebufferCount = snapshot.rebufferCount + 1)
        }

        if (state == Player.STATE_READY && !readyForCurrentItem) {
            readyForCurrentItem = true
            snapshot = snapshot.copy(startupMs = timeProvider() - loadStartedAtMs)
        }

        lastState = state
        PlaybackDiagnosticsStore.record(snapshot)
        return snapshot
    }

    fun onError(message: String): PlaybackTelemetrySnapshot {
        snapshot = snapshot.copy(
            errorCount = snapshot.errorCount + 1,
            lastError = message
        )
        PlaybackDiagnosticsStore.record(snapshot)
        return snapshot
    }

    fun snapshot(): PlaybackTelemetrySnapshot = snapshot
}
