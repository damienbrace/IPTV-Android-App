package com.example.iptvapp.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSportsClassifierTest {
    @Test
    fun acceptsProviderSuperscriptLiveMarker() {
        assertTrue(
            isCurrentLiveSportsTitle(
                title = "Major League Cricket: Eliminator ᴸᶦᵛᵉ",
                channelName = "Sky Sports Cricket HD",
                channelCategory = "UK | Sky Sports"
            )
        )
    }

    @Test
    fun rejectsOrdinaryAsciiLiveText() {
        assertFalse(
            isCurrentLiveSportsTitle(
                title = "Major League Cricket: Eliminator Live",
                channelName = "Sky Sports Cricket HD",
                channelCategory = "UK | Sky Sports"
            )
        )
    }

    @Test
    fun rejectsVisuallySimilarButDifferentSuperscriptMarker() {
        assertFalse(
            isCurrentLiveSportsTitle(
                title = "Major League Cricket ᴸⁱᵛᵉ",
                channelName = "Sky Sports Cricket HD",
                channelCategory = "UK | Sky Sports"
            )
        )
    }

    @Test
    fun rejectsTitlesWithoutProviderMarker() {
        assertFalse(
            isCurrentLiveSportsTitle(
                title = "Premier League: Watford v Liverpool",
                channelName = "Sky Sports Premier League",
                channelCategory = "UK | Sky Sports"
            )
        )
    }

    @Test
    fun rejectsLiveNewsEvenOnSportsNamedChannel() {
        assertFalse(
            isCurrentLiveSportsTitle(
                title = "Local 12 News at 11 ᴸᶦᵛᵉ",
                channelName = "NFL Teams: CBS Bengals",
                channelCategory = "USA | NFL Teams"
            )
        )
    }

    @Test
    fun rejectsHorseRacing() {
        assertFalse(
            isCurrentLiveSportsTitle(
                title = "Australian Racing ᴸᶦᵛᵉ",
                channelName = "Sky Sports Racing HD",
                channelCategory = "UK | Sky Sports"
            )
        )
    }

    @Test
    fun acceptsMotorRacing() {
        assertTrue(
            isCurrentLiveSportsTitle(
                title = "Formula 1 British Grand Prix ᴸᶦᵛᵉ",
                channelName = "Sky Sports F1 HD",
                channelCategory = "UK | Sky Sports"
            )
        )
    }

    @Test
    fun rejectsSportsNewsAndHighlights() {
        assertFalse(
            isCurrentLiveSportsTitle(
                title = "SportsCentre ᴸᶦᵛᵉ",
                channelName = "TSN 1",
                channelCategory = "CA | Sports"
            )
        )
        assertFalse(
            isCurrentLiveSportsTitle(
                title = "Cricket Highlights ᴸᶦᵛᵉ",
                channelName = "Sky Sports Cricket",
                channelCategory = "UK | Sky Sports"
            )
        )
    }
}
