package com.example.iptvapp.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveEventClassifierTest {
    @Test
    fun identifiesExplicitLiveSportsCoverage() {
        assertTrue(
            isLikelyLiveSportsEvent(
                title = "Live: Arsenal v Chelsea",
                description = null,
                channelName = "Sky Sports Main Event",
                channelCategory = "UK | Sky Sports"
            )
        )
    }

    @Test
    fun identifiesCurrentMatchStyleTitlesWithoutLiveLabel() {
        assertTrue(
            isLikelyLiveSportsEvent(
                title = "Saturday Night Football: Bournemouth v Liverpool",
                description = null,
                channelName = "Sky Sports Premier League",
                channelCategory = "UK | Sky Sports"
            )
        )
    }

    @Test
    fun rejectsRecordedAndEditorialProgramming() {
        assertFalse(
            isLikelyLiveSportsEvent(
                title = "Live EFL Highlights",
                description = "All of today's action",
                channelName = "Sky Sports Football",
                channelCategory = "UK | Sky Sports"
            )
        )
        assertFalse(
            isLikelyLiveSportsEvent(
                title = "Premier League Preview",
                description = null,
                channelName = "Sky Sports Premier League",
                channelCategory = "UK | Sky Sports"
            )
        )
        assertFalse(
            isLikelyLiveSportsEvent(
                title = "Sky Sports News Live",
                description = null,
                channelName = "Sky Sports News",
                channelCategory = "UK | Sky Sports"
            )
        )
    }

    @Test
    fun rejectsLiveNonSportsProgramming() {
        assertFalse(
            isLikelyLiveSportsEvent(
                title = "Live breaking coverage",
                description = null,
                channelName = "World News",
                channelCategory = "News"
            )
        )
    }
}
