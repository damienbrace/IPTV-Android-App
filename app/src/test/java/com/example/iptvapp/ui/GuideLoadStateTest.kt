package com.example.iptvapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideLoadStateTest {
    @Test
    fun calculatesProgressFromCompletedChannels() {
        val state = GuideLoadState(
            isLoading = true,
            completedChannels = 12,
            totalChannels = 24
        )

        assertEquals(0.5f, state.progress)
    }

    @Test
    fun reportsZeroUntilTotalIsKnown() {
        assertEquals(0f, GuideLoadState(isLoading = true).progress)
    }
}
