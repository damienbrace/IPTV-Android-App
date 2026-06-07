package com.example.iptvapp.data.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.iptvapp.data.model.Channel
import com.example.iptvapp.data.model.GuideProgram
import com.example.iptvapp.data.model.IptvHomeState
import com.example.iptvapp.data.model.IptvPlaylist
import com.example.iptvapp.data.remote.XcodesApiClient
import com.example.iptvapp.data.remote.XcodesLiveStream
import com.example.iptvapp.data.room.ChannelEntity
import com.example.iptvapp.data.room.IptvDatabase
import com.example.iptvapp.data.room.PlaylistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class LocalIptvRepository(
    context: Context,
    private val xcodesApiClient: XcodesApiClient = XcodesApiClient()
) : IptvRepository {
    private val dao = IptvDatabase.getInstance(context).iptvDao()
    private val sampleChannels = SampleIptvData.channels

    override val homeState: Flow<IptvHomeState> = combine(
        dao.observePlaylists(),
        dao.observeChannels(),
        dao.observePrograms()
    ) { playlistEntities, channelEntities, _ ->
        val channels = channelEntities.map { it.toChannel() }.ifEmpty { sampleChannels }
        IptvHomeState(
            channels = channels,
            guidePrograms = channels.toGuidePrograms(),
            playlists = playlistEntities.map { it.toPlaylist() },
            recentSearches = listOf("Seven", "ESPN", "Discovery", "News", "Sports"),
            categories = buildList {
                add("All Channels")
                add("Favourites")
                addAll(channels.map { it.category }.distinct().take(12))
            }
        )
    }

    override suspend fun addPlaylist(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<IptvPlaylist> {
        return runCatching {
            xcodesApiClient.testConnection(serverUrl, username, password).getOrThrow()
            val categories = xcodesApiClient.fetchLiveCategories(serverUrl, username, password).getOrElse { emptyList() }
            val categoryNamesById = categories.associate { it.id to it.name }
            val liveStreams = xcodesApiClient.fetchLiveStreams(serverUrl, username, password).getOrThrow()
            val playlistId = name.toPlaylistId()
            val playlist = IptvPlaylist(
                id = playlistId,
                name = name,
                serverUrl = serverUrl,
                username = username,
                lastUpdated = "Just now",
                connected = true
            )
            val channelEntities = liveStreams.mapIndexed { index, stream ->
                stream.toChannelEntity(
                    playlistId = playlistId,
                    number = index + 1,
                    category = categoryNamesById[stream.categoryId] ?: "Live TV",
                    serverUrl = serverUrl,
                    username = username,
                    password = password
                )
            }

            dao.upsertPlaylist(
                PlaylistEntity(
                    id = playlist.id,
                    name = playlist.name,
                    serverUrl = playlist.serverUrl,
                    username = playlist.username,
                    encryptedPassword = password,
                    lastUpdatedEpochMillis = System.currentTimeMillis(),
                    connected = true
                )
            )
            dao.replaceChannelsForPlaylist(playlist.id, channelEntities)
            playlist
        }
    }

    override suspend fun testPlaylistConnection(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Unit> {
        return xcodesApiClient.testConnection(serverUrl, username, password).map { }
    }

    override suspend fun toggleFavorite(channelId: String) {
        val channel = dao.getChannel(channelId) ?: return
        dao.setFavorite(channelId, !channel.favorite)
    }

    override suspend fun getChannel(channelId: String): Channel? {
        return dao.getChannel(channelId)?.toChannel()
    }

    private fun XcodesLiveStream.toChannelEntity(
        playlistId: String,
        number: Int,
        category: String,
        serverUrl: String,
        username: String,
        password: String
    ): ChannelEntity {
        return ChannelEntity(
            id = "$playlistId-live-$streamId",
            playlistId = playlistId,
            number = number,
            name = name,
            logoUrl = streamIcon,
            category = category,
            streamUrl = xcodesApiClient.buildLiveStreamUrl(serverUrl, username, password, streamId),
            favorite = false
        )
    }

    private fun ChannelEntity.toChannel(): Channel {
        return Channel(
            id = id,
            number = number,
            name = name,
            logo = name.toLogoText(),
            logoColor = Color.White,
            category = category,
            currentProgramTime = "Live now",
            progress = 0f,
            streamUrl = streamUrl,
            favorite = favorite
        )
    }

    private fun PlaylistEntity.toPlaylist(): IptvPlaylist {
        return IptvPlaylist(
            id = id,
            name = name,
            serverUrl = serverUrl,
            username = username,
            lastUpdated = "Saved",
            connected = connected
        )
    }

    private fun List<Channel>.toGuidePrograms(): List<GuideProgram> {
        return take(80).map { channel ->
            GuideProgram(
                channel = channel,
                primaryTitle = channel.name,
                secondaryTitle = "",
                startsAtHalfHour = false
            )
        }
    }

    private fun String.toPlaylistId(): String {
        return lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "playlist" }
    }

    private fun String.toLogoText(): String {
        val words = trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> "TV"
            words.size == 1 -> words.first().take(3).uppercase()
            else -> words.take(2).joinToString("") { it.take(1) }.uppercase()
        }
    }
}
