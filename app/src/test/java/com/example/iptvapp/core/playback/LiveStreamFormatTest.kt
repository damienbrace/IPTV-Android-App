package com.example.iptvapp.core.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveStreamFormatTest {
    @Test
    fun convertsXtreamHlsUrlToMpegTs() {
        val url = "https://example.com/live/user/password/11002.m3u8"

        assertEquals(
            "https://example.com/live/user/password/11002.ts",
            resolveLiveStreamUrl(url, LiveStreamFormat.MPEG_TS)
        )
    }

    @Test
    fun preservesQueryParametersWhenChangingFormat() {
        val url = "https://example.com/live/11002.m3u8?token=abc"

        assertEquals(
            "https://example.com/live/11002.ts?token=abc",
            resolveLiveStreamUrl(url, LiveStreamFormat.MPEG_TS)
        )
    }

    @Test
    fun convertsMpegTsUrlBackToHls() {
        val url = "https://example.com/live/user/password/11002.ts"

        assertEquals(
            "https://example.com/live/user/password/11002.m3u8",
            resolveLiveStreamUrl(url, LiveStreamFormat.HLS)
        )
    }

    @Test
    fun onlyOffersSwitchingForKnownLiveExtensions() {
        assertTrue(supportsLiveStreamFormatSwitch("https://example.com/live/11002.m3u8"))
        assertTrue(supportsLiveStreamFormatSwitch("https://example.com/live/11002.ts"))
        assertFalse(supportsLiveStreamFormatSwitch("https://example.com/watch/11002"))
    }
}
