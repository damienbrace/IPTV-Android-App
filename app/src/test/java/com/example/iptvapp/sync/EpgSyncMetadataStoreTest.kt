package com.example.iptvapp.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgSyncMetadataStoreTest {
    @Test
    fun `sync is due when it has never completed`() {
        assertTrue(isEpgSyncDue(0L, nowEpochMillis = DAY, minimumIntervalMillis = DAY))
    }

    @Test
    fun `sync is not due before daily interval expires`() {
        assertFalse(isEpgSyncDue(DAY, nowEpochMillis = DAY * 2 - 1L, minimumIntervalMillis = DAY))
    }

    @Test
    fun `sync is due when daily interval expires`() {
        assertTrue(isEpgSyncDue(DAY, nowEpochMillis = DAY * 2, minimumIntervalMillis = DAY))
    }

    @Test
    fun `sync is due after device clock moves backwards`() {
        assertTrue(isEpgSyncDue(DAY * 2, nowEpochMillis = DAY, minimumIntervalMillis = DAY))
    }

    private companion object {
        const val DAY = 24L * 60L * 60L * 1_000L
    }
}
