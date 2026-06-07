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
}
