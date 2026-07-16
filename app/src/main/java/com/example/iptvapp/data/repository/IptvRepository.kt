package com.example.iptvapp.data.repository

import com.example.iptvapp.data.model.Channel
import com.example.iptvapp.data.model.IptvHomeState
import com.example.iptvapp.data.model.IptvPlaylist
import kotlinx.coroutines.flow.Flow

interface IptvRepository {
    val homeState: Flow<IptvHomeState>

    suspend fun addPlaylist(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<IptvPlaylist>

    suspend fun testPlaylistConnection(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Unit>

    suspend fun refreshPlaylist(
        playlistId: String,
        onProgress: (progress: Float?, message: String) -> Unit = { _, _ -> }
    ): Result<Unit>

    suspend fun refreshGuide(
        channelIds: List<String>,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Unit>

    suspend fun preloadGuide(channelIds: List<String>): Result<Unit>

    suspend fun deletePlaylist(playlistId: String)

    suspend fun toggleFavorite(channelId: String)

    suspend fun getChannel(channelId: String): Channel?
}
