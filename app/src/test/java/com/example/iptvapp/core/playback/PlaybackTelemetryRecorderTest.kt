package com.example.iptvapp.core.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackTelemetryRecorderTest {
    @Test
    fun recordsStartupTimeAndRebuffers() {
        var now = 1_000L
        val recorder = PlaybackTelemetryRecorder(timeProvider = { now })

        val initial = recorder.onChannelLoad("channel-1", "News")
        assertEquals("channel-1", initial.channelId)
        assertEquals("News", initial.channelName)
        assertNull(initial.startupMs)

        now = 1_420L
        val ready = recorder.onPlaybackStateChanged(Player.STATE_READY)
        assertEquals(420L, ready.startupMs)
        assertEquals(0, ready.rebufferCount)

        recorder.onPlaybackStateChanged(Player.STATE_BUFFERING)
        recorder.onPlaybackStateChanged(Player.STATE_READY)
        assertEquals(1, recorder.snapshot().rebufferCount)
    }

    @Test
    fun countsChannelSwitchesAndErrors() {
        val recorder = PlaybackTelemetryRecorder(timeProvider = { 10L })

        recorder.onChannelLoad("channel-1", "News")
        recorder.onChannelLoad("channel-2", "Sports")
        val error = recorder.onError("Timeout")

        assertEquals(1, error.channelSwitchCount)
        assertEquals(1, error.errorCount)
        assertEquals("Timeout", error.lastError)
    }

    @Test
    fun recordsDetailedPlaybackMetrics() {
        val recorder = PlaybackTelemetryRecorder(timeProvider = { 10L })
        recorder.onChannelLoad("channel-1", "Sports")

        val snapshot = recorder.onMetrics(
            PlaybackMetrics(
                source = "HLS",
                playbackState = "Playing",
                resolution = "1920x1080",
                videoCodec = "AVC1",
                audioCodec = "MP4A",
                audioChannelCount = 2,
                audioSampleRateHz = 48_000,
                bandwidthEstimateBitsPerSecond = 8_200_000L,
                bufferedDurationMs = 14_500L,
                liveOffsetMs = 22_000L,
                liveOffsetEstimated = false,
                droppedFrames = 3
            )
        )

        assertEquals("HLS", snapshot.source)
        assertEquals("Playing", snapshot.playbackState)
        assertEquals("1920x1080", snapshot.resolution)
        assertEquals(14_500L, snapshot.bufferedDurationMs)
        assertEquals(22_000L, snapshot.liveOffsetMs)
        assertEquals(3, snapshot.droppedFrames)
    }

    @Test
    fun formatsDiagnosticValues() {
        assertEquals("8.2 Mbps", formatBitrate(8_200_000L))
        assertEquals("640 Kbps", formatBitrate(640_000L))
        assertEquals("14.5 s", formatPlaybackDuration(14_500L))
        assertEquals("420 ms", formatPlaybackDuration(420L))
        assertEquals("1920x1080", resolutionLabel(1920, 1080))
        assertEquals("AVC1", codecLabel("video/avc", "avc1.640028"))
    }

    @Test
    fun usesNativeLiveOffsetWhenStreamExposesIt() {
        val measurement = resolveLiveOffset(
            nativeLiveOffsetMs = 12_000L,
            isLive = true,
            durationMs = 60_000L,
            currentPositionMs = 45_000L
        )

        assertEquals(12_000L, measurement.durationMs)
        assertEquals(false, measurement.estimated)
    }

    @Test
    fun estimatesDistanceToLiveEdgeWhenWallClockOffsetIsMissing() {
        val measurement = resolveLiveOffset(
            nativeLiveOffsetMs = androidx.media3.common.C.TIME_UNSET,
            isLive = true,
            durationMs = 60_000L,
            currentPositionMs = 45_000L
        )

        assertEquals(15_000L, measurement.durationMs)
        assertEquals(true, measurement.estimated)
    }
}
