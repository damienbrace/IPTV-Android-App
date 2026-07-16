package com.example.iptvapp.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.epgSyncMetadataDataStore by preferencesDataStore(name = "epg_sync_metadata")

class EpgSyncMetadataStore(private val context: Context) {
    suspend fun isSyncDue(
        nowEpochMillis: Long = System.currentTimeMillis(),
        minimumIntervalMillis: Long = DAILY_SYNC_INTERVAL_MILLIS
    ): Boolean {
        val lastCompleted = context.epgSyncMetadataDataStore.data.first()[LAST_COMPLETED_AT] ?: 0L
        return isEpgSyncDue(lastCompleted, nowEpochMillis, minimumIntervalMillis)
    }

    suspend fun markCompleted(nowEpochMillis: Long = System.currentTimeMillis()) {
        context.epgSyncMetadataDataStore.edit { preferences ->
            preferences[LAST_COMPLETED_AT] = nowEpochMillis
        }
    }

    private companion object {
        val LAST_COMPLETED_AT = longPreferencesKey("last_completed_at")
        const val DAILY_SYNC_INTERVAL_MILLIS = 24L * 60L * 60L * 1_000L
    }
}

internal fun isEpgSyncDue(
    lastCompletedEpochMillis: Long,
    nowEpochMillis: Long,
    minimumIntervalMillis: Long
): Boolean = lastCompletedEpochMillis <= 0L ||
    nowEpochMillis < lastCompletedEpochMillis ||
    nowEpochMillis - lastCompletedEpochMillis >= minimumIntervalMillis
