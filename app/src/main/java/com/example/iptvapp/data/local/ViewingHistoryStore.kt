package com.example.iptvapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.io.IOException

private val Context.viewingHistoryDataStore by preferencesDataStore(name = "viewing_history")

class ViewingHistoryStore(private val context: Context) {
    val recentlyWatchedChannelIds: Flow<List<String>> = context.viewingHistoryDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            decodeViewingHistory(preferences[RECENT_CHANNEL_IDS])
        }

    suspend fun recordChannel(channelId: String) {
        if (channelId.isBlank()) return
        context.viewingHistoryDataStore.edit { preferences ->
            val updatedHistory = updateViewingHistory(
                history = decodeViewingHistory(preferences[RECENT_CHANNEL_IDS]),
                channelId = channelId,
                limit = MAX_RECENT_CHANNELS
            )
            preferences[RECENT_CHANNEL_IDS] = JSONArray(updatedHistory).toString()
        }
    }

    private companion object {
        val RECENT_CHANNEL_IDS = stringPreferencesKey("recent_channel_ids")
        const val MAX_RECENT_CHANNELS = 20
    }
}

internal fun decodeViewingHistory(encoded: String?): List<String> {
    if (encoded.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(encoded)
        buildList {
            repeat(array.length()) { index ->
                array.optString(index)
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
    }.getOrDefault(emptyList())
}

internal fun updateViewingHistory(
    history: List<String>,
    channelId: String,
    limit: Int
): List<String> {
    if (channelId.isBlank() || limit <= 0) return emptyList()
    return buildList {
        add(channelId)
        history.asSequence()
            .filter { it.isNotBlank() && it != channelId }
            .take(limit - 1)
            .forEach(::add)
    }
}
