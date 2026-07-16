package com.example.iptvapp.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewingHistoryStoreTest {
    @Test
    fun mostRecentChannelIsFirstAndDuplicatesAreRemoved() {
        assertEquals(
            listOf("sports", "news", "movies"),
            updateViewingHistory(
                history = listOf("news", "sports", "movies"),
                channelId = "sports",
                limit = 20
            )
        )
    }

    @Test
    fun historyIsLimitedToRequestedSize() {
        assertEquals(
            listOf("new", "one", "two"),
            updateViewingHistory(
                history = listOf("one", "two", "three"),
                channelId = "new",
                limit = 3
            )
        )
    }

    @Test
    fun malformedStoredHistoryIsIgnored() {
        assertEquals(emptyList<String>(), decodeViewingHistory("not-json"))
    }
}
