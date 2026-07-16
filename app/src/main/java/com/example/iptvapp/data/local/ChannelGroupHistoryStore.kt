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

private val Context.channelGroupHistoryDataStore by preferencesDataStore(
    name = "channel_group_history"
)

class ChannelGroupHistoryStore(private val context: Context) {
    val frequentGroups: Flow<List<String>> = context.channelGroupHistoryDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            rankFrequentGroups(
                history = decodeGroupVisitHistory(preferences[VISIT_HISTORY]),
                limit = MAX_FREQUENT_GROUPS
            )
        }

    suspend fun recordVisit(group: String) {
        if (group.isBlank()) return
        context.channelGroupHistoryDataStore.edit { preferences ->
            val history = decodeGroupVisitHistory(preferences[VISIT_HISTORY])
            val updatedHistory = (history + group).takeLast(MAX_VISIT_HISTORY)
            preferences[VISIT_HISTORY] = JSONArray(updatedHistory).toString()
        }
    }

    private companion object {
        val VISIT_HISTORY = stringPreferencesKey("visit_history")
        const val MAX_VISIT_HISTORY = 48
        const val MAX_FREQUENT_GROUPS = 4
    }
}

internal fun decodeGroupVisitHistory(encoded: String?): List<String> {
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

internal fun rankFrequentGroups(history: List<String>, limit: Int): List<String> {
    if (limit <= 0) return emptyList()
    data class VisitStats(var count: Int = 0, var lastIndex: Int = -1)

    val statsByGroup = mutableMapOf<String, VisitStats>()
    history.forEachIndexed { index, group ->
        if (group.isNotBlank()) {
            statsByGroup.getOrPut(group) { VisitStats() }.apply {
                count++
                lastIndex = index
            }
        }
    }
    return statsByGroup.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, VisitStats>> { it.value.count }
                .thenByDescending { it.value.lastIndex }
        )
        .take(limit)
        .map { it.key }
}
