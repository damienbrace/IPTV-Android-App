package com.example.iptvapp.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelGroupHistoryStoreTest {
    @Test
    fun frequentGroupsAreRankedByCountThenRecency() {
        val history = listOf("News", "Sports", "News", "Kids", "Sports", "News", "Kids")

        assertEquals(
            listOf("News", "Kids", "Sports"),
            rankFrequentGroups(history, limit = 4)
        )
    }

    @Test
    fun visitHistoryRoundTripsGroupNames() {
        val encoded = "[\"UK | Sports\",\"CA | News\"]"

        assertEquals(
            listOf("UK | Sports", "CA | News"),
            decodeGroupVisitHistory(encoded)
        )
    }

    @Test
    fun malformedHistoryIsIgnored() {
        assertEquals(emptyList<String>(), decodeGroupVisitHistory("not-json"))
    }
}
