package com.example.iptvapp.core.playback

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class PlaybackTelemetrySnapshot(
    val channelId: String? = null,
    val channelName: String? = null,
    val startupMs: Long? = null,
    val rebufferCount: Int = 0,
    val channelSwitchCount: Int = 0,
    val errorCount: Int = 0,
    val lastError: String? = null,
    val source: String? = null,
    val playbackState: String = "Idle",
    val resolution: String? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val audioChannelCount: Int? = null,
    val audioSampleRateHz: Int? = null,
    val bandwidthEstimateBitsPerSecond: Long? = null,
    val bufferedDurationMs: Long = 0L,
    val liveOffsetMs: Long? = null,
    val liveOffsetEstimated: Boolean = false,
    val droppedFrames: Int = 0
)

data class PlaybackMetrics(
    val source: String,
    val playbackState: String,
    val resolution: String?,
    val videoCodec: String?,
    val audioCodec: String?,
    val audioChannelCount: Int?,
    val audioSampleRateHz: Int?,
    val bandwidthEstimateBitsPerSecond: Long?,
    val bufferedDurationMs: Long,
    val liveOffsetMs: Long?,
    val liveOffsetEstimated: Boolean,
    val droppedFrames: Int
)

data class LiveOffsetMeasurement(
    val durationMs: Long?,
    val estimated: Boolean
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
            lastError = null,
            source = null,
            playbackState = "Buffering",
            resolution = null,
            videoCodec = null,
            audioCodec = null,
            audioChannelCount = null,
            audioSampleRateHz = null,
            bandwidthEstimateBitsPerSecond = null,
            bufferedDurationMs = 0L,
            liveOffsetMs = null,
            liveOffsetEstimated = false,
            droppedFrames = 0
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

    fun onMetrics(metrics: PlaybackMetrics): PlaybackTelemetrySnapshot {
        snapshot = snapshot.copy(
            source = metrics.source,
            playbackState = metrics.playbackState,
            resolution = metrics.resolution,
            videoCodec = metrics.videoCodec,
            audioCodec = metrics.audioCodec,
            audioChannelCount = metrics.audioChannelCount,
            audioSampleRateHz = metrics.audioSampleRateHz,
            bandwidthEstimateBitsPerSecond = metrics.bandwidthEstimateBitsPerSecond,
            bufferedDurationMs = metrics.bufferedDurationMs,
            liveOffsetMs = metrics.liveOffsetMs,
            liveOffsetEstimated = metrics.liveOffsetEstimated,
            droppedFrames = metrics.droppedFrames
        )
        PlaybackDiagnosticsStore.record(snapshot)
        return snapshot
    }

    fun snapshot(): PlaybackTelemetrySnapshot = snapshot
}

internal fun playbackStateLabel(state: Int, isPlaying: Boolean): String = when (state) {
    Player.STATE_BUFFERING -> "Buffering"
    Player.STATE_READY -> if (isPlaying) "Playing" else "Paused"
    Player.STATE_ENDED -> "Ended"
    else -> "Idle"
}

internal fun resolutionLabel(width: Int, height: Int): String? {
    return if (width > 0 && height > 0) "${width}x$height" else null
}

internal fun codecLabel(sampleMimeType: String?, codecs: String?): String? {
    val codec = codecs
        ?.substringBefore(',')
        ?.substringBefore('.')
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.uppercase()
    return codec ?: sampleMimeType
        ?.substringAfter('/')
        ?.takeIf { it.isNotBlank() }
        ?.uppercase()
}

internal fun formatBitrate(bitsPerSecond: Long?): String {
    if (bitsPerSecond == null || bitsPerSecond <= 0L) return "Unknown"
    return if (bitsPerSecond >= 1_000_000L) {
        "%.1f Mbps".format(Locale.US, bitsPerSecond / 1_000_000.0)
    } else {
        "${bitsPerSecond / 1_000L} Kbps"
    }
}

internal fun formatPlaybackDuration(durationMs: Long?): String {
    if (durationMs == null || durationMs < 0L) return "Unknown"
    return if (durationMs >= 1_000L) {
        "%.1f s".format(Locale.US, durationMs / 1_000.0)
    } else {
        "$durationMs ms"
    }
}

internal fun resolveLiveOffset(
    nativeLiveOffsetMs: Long,
    isLive: Boolean,
    durationMs: Long,
    currentPositionMs: Long
): LiveOffsetMeasurement {
    if (nativeLiveOffsetMs != C.TIME_UNSET && nativeLiveOffsetMs >= 0L) {
        return LiveOffsetMeasurement(nativeLiveOffsetMs, estimated = false)
    }
    if (!isLive || durationMs == C.TIME_UNSET || durationMs <= 0L || currentPositionMs < 0L) {
        return LiveOffsetMeasurement(durationMs = null, estimated = false)
    }
    return LiveOffsetMeasurement(
        durationMs = (durationMs - currentPositionMs).coerceAtLeast(0L),
        estimated = true
    )
}
