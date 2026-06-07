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
import com.example.iptvapp.data.room.EpgProgramEntity
import com.example.iptvapp.data.room.IptvDatabase
import com.example.iptvapp.data.room.PlaylistEntity
import com.example.iptvapp.data.security.CredentialVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class LocalIptvRepository(
    context: Context,
    private val xcodesApiClient: XcodesApiClient = XcodesApiClient(),
    private val credentialVault: CredentialVault = CredentialVault()
) : IptvRepository {
    private val dao = IptvDatabase.getInstance(context).iptvDao()
    private val sampleChannels = SampleIptvData.channels

    override val homeState: Flow<IptvHomeState> = combine(
        dao.observePlaylists(),
        dao.observeChannels(),
        dao.observePrograms()
    ) { playlistEntities, channelEntities, programEntities ->
        val playlistsById = playlistEntities.associateBy { it.id }
        val channels = channelEntities.map { it.toChannel(playlistsById[it.playlistId]) }.ifEmpty { sampleChannels }
        val channelsById = channels.associateBy { it.id }
        IptvHomeState(
            channels = channels,
            guidePrograms = if (channelEntities.isEmpty() || programEntities.isEmpty()) {
                channels.toGuidePrograms()
            } else {
                programEntities.toGuidePrograms(channels, channelsById)
            },
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
                    encryptedPassword = credentialVault.encrypt(password),
                    lastUpdatedEpochMillis = System.currentTimeMillis(),
                    connected = true
                )
            )
            dao.replaceChannelsForPlaylist(playlist.id, channelEntities)
            syncShortEpg(
                channelEntities = channelEntities,
                serverUrl = serverUrl,
                username = username,
                password = password
            )
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

    override suspend fun refreshPlaylist(playlistId: String): Result<Unit> {
        return runCatching {
            val playlist = dao.getPlaylist(playlistId) ?: error("Playlist not found")
            val password = credentialVault.decrypt(playlist.encryptedPassword)
            refreshPlaylist(playlist, password)
        }
    }

    override suspend fun deletePlaylist(playlistId: String) {
        dao.deletePlaylist(playlistId)
    }

    override suspend fun toggleFavorite(channelId: String) {
        val channel = dao.getChannel(channelId) ?: return
        dao.setFavorite(channelId, !channel.favorite)
    }

    override suspend fun getChannel(channelId: String): Channel? {
        val channel = dao.getChannel(channelId) ?: return null
        return channel.toChannel(dao.getPlaylist(channel.playlistId))
    }

    suspend fun refreshSavedPlaylists(): Result<Unit> {
        return runCatching {
            val playlists = dao.getPlaylists()
            playlists.forEach { playlist ->
                val password = credentialVault.decrypt(playlist.encryptedPassword)
                refreshPlaylist(playlist, password)
            }
        }
    }

    private suspend fun refreshPlaylist(playlist: PlaylistEntity, password: String) {
        xcodesApiClient.testConnection(playlist.serverUrl, playlist.username, password).getOrThrow()
        val categories = xcodesApiClient.fetchLiveCategories(playlist.serverUrl, playlist.username, password).getOrElse { emptyList() }
        val categoryNamesById = categories.associate { it.id to it.name }
        val liveStreams = xcodesApiClient.fetchLiveStreams(playlist.serverUrl, playlist.username, password).getOrThrow()
        val channelEntities = liveStreams.mapIndexed { index, stream ->
            stream.toChannelEntity(
                playlistId = playlist.id,
                number = index + 1,
                category = categoryNamesById[stream.categoryId] ?: "Live TV",
                serverUrl = playlist.serverUrl,
                username = playlist.username,
                password = password
            )
        }

        dao.upsertPlaylist(
            playlist.copy(
                lastUpdatedEpochMillis = System.currentTimeMillis(),
                connected = true
            )
        )
        dao.replaceChannelsForPlaylist(playlist.id, channelEntities)
        syncShortEpg(
            channelEntities = channelEntities,
            serverUrl = playlist.serverUrl,
            username = playlist.username,
            password = password
        )
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
            streamId = streamId,
            number = number,
            name = name,
            logoUrl = streamIcon,
            category = category,
            streamUrl = "",
            favorite = false
        )
    }

    private fun ChannelEntity.toChannel(playlist: PlaylistEntity?): Channel {
        val streamUrl = if (playlist != null && streamId != null) {
            val password = credentialVault.decrypt(playlist.encryptedPassword)
            xcodesApiClient.buildLiveStreamUrl(playlist.serverUrl, playlist.username, password, streamId)
        } else {
            this.streamUrl
        }

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
            logoUrl = logoUrl,
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

    private suspend fun syncShortEpg(
        channelEntities: List<ChannelEntity>,
        serverUrl: String,
        username: String,
        password: String
    ) {
        val targetChannels = channelEntities.filter { it.streamId != null }.take(30)
        val programs = targetChannels.flatMap { channel ->
            val streamId = channel.streamId ?: return@flatMap emptyList()
            xcodesApiClient.fetchShortEpg(serverUrl, username, password, streamId).getOrElse { emptyList() }
                .mapIndexed { index, program ->
                    EpgProgramEntity(
                        id = "${channel.id}-${program.startsAtEpochMillis}-$index",
                        channelId = channel.id,
                        title = program.title,
                        description = program.description,
                        startsAtEpochMillis = program.startsAtEpochMillis,
                        endsAtEpochMillis = program.endsAtEpochMillis
                    )
                }
        }
        dao.replaceProgramsForChannels(targetChannels.map { it.id }, programs)
    }

    private fun List<EpgProgramEntity>.toGuidePrograms(
        channels: List<Channel>,
        channelsById: Map<String, Channel>
    ): List<GuideProgram> {
        val programsByChannel = groupBy { it.channelId }
        return channels.take(80).map { channel ->
            val programs = programsByChannel[channel.id].orEmpty()
            GuideProgram(
                channel = channel,
                primaryTitle = programs.getOrNull(0)?.title ?: channel.name,
                secondaryTitle = programs.getOrNull(1)?.title ?: "",
                startsAtHalfHour = false
            )
        }.filter { channelsById.containsKey(it.channel.id) }
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
