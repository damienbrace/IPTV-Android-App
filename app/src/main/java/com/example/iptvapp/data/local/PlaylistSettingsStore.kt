package com.example.iptvapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playlistDataStore by preferencesDataStore(name = "playlist_settings")

data class PlaylistSettings(
    val selectedPlaylistId: String? = null,
    val lastSyncIso: String? = null,
    val lowLatencyPlayback: Boolean = true
)

class PlaylistSettingsStore(private val context: Context) {
    val settings: Flow<PlaylistSettings> = context.playlistDataStore.data.map { preferences ->
        PlaylistSettings(
            selectedPlaylistId = preferences[SELECTED_PLAYLIST_ID],
            lastSyncIso = preferences[LAST_SYNC_ISO],
            lowLatencyPlayback = preferences[LOW_LATENCY_PLAYBACK] ?: true
        )
    }

    suspend fun selectPlaylist(playlistId: String) {
        context.playlistDataStore.edit { preferences ->
            preferences[SELECTED_PLAYLIST_ID] = playlistId
        }
    }

    suspend fun updateLastSync(isoTimestamp: String) {
        context.playlistDataStore.edit { preferences ->
            preferences[LAST_SYNC_ISO] = isoTimestamp
        }
    }

    suspend fun setLowLatencyPlayback(enabled: Boolean) {
        context.playlistDataStore.edit { preferences ->
            preferences[LOW_LATENCY_PLAYBACK] = enabled
        }
    }

    private companion object {
        val SELECTED_PLAYLIST_ID = stringPreferencesKey("selected_playlist_id")
        val LAST_SYNC_ISO = stringPreferencesKey("last_sync_iso")
        val LOW_LATENCY_PLAYBACK = booleanPreferencesKey("low_latency_playback")
    }
}
