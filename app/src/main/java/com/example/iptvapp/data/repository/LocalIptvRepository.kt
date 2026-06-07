package com.example.iptvapp.data.repository

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        val playlistPasswordsById = playlistEntities.mapNotNull { playlist ->
            runCatching { playlist.id to credentialVault.decrypt(playlist.encryptedPassword) }.getOrNull()
        }.toMap()
        val liveChannelEntities = channelEntities.filter { it.streamKind == STREAM_KIND_LIVE }
        val channels = liveChannelEntities.map { channel ->
            channel.toChannel(
                playlist = playlistsById[channel.playlistId],
                password = playlistPasswordsById[channel.playlistId]
            )
        }.ifEmpty { sampleChannels }
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
                addAll(channels.map { it.category }.distinct())
            }
        )
    }.flowOn(Dispatchers.Default)

    override suspend fun addPlaylist(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<IptvPlaylist> = withContext(Dispatchers.IO) {
        runCatching {
            xcodesApiClient.testConnection(serverUrl, username, password).getOrThrow()
            val playlistId = name.toPlaylistId()
            val playlist = IptvPlaylist(
                id = playlistId,
                name = name,
                serverUrl = serverUrl,
                username = username,
                lastUpdated = "Just now",
                connected = true
            )
            val channelEntities = fetchChannelEntities(playlistId, serverUrl, username, password)

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

    override suspend fun refreshPlaylist(playlistId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
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
        val playlist = dao.getPlaylist(channel.playlistId)
        val password = playlist?.let {
            runCatching { credentialVault.decrypt(it.encryptedPassword) }.getOrNull()
        }
        return channel.toChannel(playlist, password)
    }

    suspend fun refreshSavedPlaylists(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val playlists = dao.getPlaylists()
            playlists.forEach { playlist ->
                val password = credentialVault.decrypt(playlist.encryptedPassword)
                refreshPlaylist(playlist, password)
            }
        }
    }

    private suspend fun refreshPlaylist(playlist: PlaylistEntity, password: String) {
        xcodesApiClient.testConnection(playlist.serverUrl, playlist.username, password).getOrThrow()
        val channelEntities = fetchChannelEntities(playlist.id, playlist.serverUrl, playlist.username, password)

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

    private suspend fun fetchChannelEntities(
        playlistId: String,
        serverUrl: String,
        username: String,
        password: String
    ): List<ChannelEntity> {
        val liveCategories = xcodesApiClient.fetchLiveCategories(serverUrl, username, password).getOrElse { emptyList() }
        val categoryNamesById = liveCategories.associate { it.id to it.name }

        val liveStreams = xcodesApiClient.fetchLiveStreams(serverUrl, username, password).getOrElse { emptyList() }

        return buildList {
            liveStreams.forEach { stream ->
                add(
                    stream.toChannelEntity(
                        playlistId = playlistId,
                        number = size + 1,
                        category = categoryNamesById[stream.categoryId] ?: "Live TV",
                        serverUrl = serverUrl
                    )
                )
            }
        }
    }

    private fun XcodesLiveStream.toChannelEntity(
        playlistId: String,
        number: Int,
        category: String,
        serverUrl: String
    ): ChannelEntity {
        return ChannelEntity(
            id = "$playlistId-live-$streamId",
            playlistId = playlistId,
            streamId = streamId,
            streamKind = STREAM_KIND_LIVE,
            containerExtension = null,
            number = number,
            name = name,
            logoUrl = xcodesApiClient.buildLogoUrl(serverUrl, streamIcon),
            epgChannelId = epgChannelId,
            category = category,
            streamUrl = "",
            favorite = false
        )
    }

    private fun ChannelEntity.toChannel(playlist: PlaylistEntity?, password: String?): Channel {
        val streamUrl = if (playlist != null && password != null && streamId != null) {
            when (streamKind) {
                STREAM_KIND_MOVIE -> xcodesApiClient.buildMovieStreamUrl(
                    playlist.serverUrl,
                    playlist.username,
                    password,
                    streamId,
                    containerExtension
                )
                else -> xcodesApiClient.buildLiveStreamUrl(playlist.serverUrl, playlist.username, password, streamId)
            }
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
        val liveChannels = channelEntities.filter {
            it.streamKind == STREAM_KIND_LIVE && it.streamId != null
        }
        val now = System.currentTimeMillis()
        val xmltvChannels = liveChannels
        val xmltvResult = xcodesApiClient.fetchXmltvPrograms(
            serverUrl = serverUrl,
            username = username,
            password = password,
            targetChannelIds = xmltvChannels.mapNotNull { it.epgChannelId }.toSet(),
            targetChannelNames = xmltvChannels.map { it.name }.toSet(),
            windowStartEpochMillis = now - EPG_PAST_WINDOW_MILLIS,
            windowEndEpochMillis = now + EPG_FUTURE_WINDOW_MILLIS
        )
        xmltvResult.exceptionOrNull()?.let { error ->
            Log.w(TAG, "XMLTV EPG fetch failed", error)
        }
        val xmltvPrograms = xmltvResult.getOrElse { emptyList() }

        val channelsByEpgId = xmltvChannels
            .groupBy { it.epgChannelId.orEmpty() }
            .filterKeys { it.isNotBlank() }
        val channelsWithGuideKeys = xmltvChannels
            .map { channel -> channel to channel.name.normalizedGuideKey() }
            .filter { (_, guideKey) -> guideKey.isNotBlank() }
        val channelsByGuideKey = channelsWithGuideKeys.groupBy({ (_, guideKey) -> guideKey }, { (channel, _) -> channel })
        val channelsByGuideToken = channelsWithGuideKeys.groupBy { (_, guideKey) -> guideKey.substringBefore(' ') }
        val xmltvEntities = xmltvPrograms.flatMap { program ->
            val programGuideKey = (program.channelName ?: program.channelId).normalizedGuideKey()
            val exactMatches = channelsByEpgId[program.channelId].orEmpty()
            val nameMatches = if (exactMatches.isEmpty()) {
                channelsByGuideKey[programGuideKey].orEmpty()
            } else {
                emptyList()
            }
            val fuzzyMatches = if (exactMatches.isEmpty() && nameMatches.isEmpty()) {
                programGuideKey
                    .split(' ')
                    .asSequence()
                    .flatMap { token -> channelsByGuideToken[token].orEmpty().asSequence() }
                    .distinctBy { (channel, _) -> channel.id }
                    .filter { (_, channelGuideKey) ->
                        channelGuideKey.length >= MIN_FUZZY_GUIDE_KEY_LENGTH &&
                            (programGuideKey.contains(channelGuideKey) || channelGuideKey.contains(programGuideKey))
                    }
                    .map { (channel, _) -> channel }
                    .toList()
            } else {
                emptyList()
            }
            (exactMatches + nameMatches + fuzzyMatches).distinctBy { it.id }.mapIndexed { index, channel ->
                EpgProgramEntity(
                    id = "${channel.id}-${program.startsAtEpochMillis}-xmltv-$index",
                    channelId = channel.id,
                    title = program.title,
                    description = program.description,
                    startsAtEpochMillis = program.startsAtEpochMillis,
                    endsAtEpochMillis = program.endsAtEpochMillis
                )
            }
        }

        val channelsWithXmltv = xmltvEntities.map { it.channelId }.toSet()
        val shortEpgChannels = liveChannels
            .filter { it.id !in channelsWithXmltv }
            .groupBy { it.category }
            .values
            .flatMap { channelsInCategory -> channelsInCategory.take(12) }
            .distinctBy { it.id }
            .take(MAX_EPG_SYNC_CHANNELS)
        val shortEpgEntities = shortEpgChannels.flatMap { channel ->
            val streamId = channel.streamId ?: return@flatMap emptyList()
            xcodesApiClient.fetchShortEpg(serverUrl, username, password, streamId).getOrElse { emptyList() }
                .mapIndexed { index, program ->
                    EpgProgramEntity(
                        id = "${channel.id}-${program.startsAtEpochMillis}-short-$index",
                        channelId = channel.id,
                        title = program.title,
                        description = program.description,
                        startsAtEpochMillis = program.startsAtEpochMillis,
                        endsAtEpochMillis = program.endsAtEpochMillis
                    )
                }
        }
        val syncedChannelIds = (xmltvChannels.map { it.id } + shortEpgChannels.map { it.id }).distinct()
        Log.i(
            TAG,
            "EPG sync result: live=${liveChannels.size}, epgIds=${xmltvChannels.count { !it.epgChannelId.isNullOrBlank() }}, " +
                "xmltvPrograms=${xmltvPrograms.size}, xmltvRows=${xmltvEntities.size}, xmltvChannels=${channelsWithXmltv.size}, " +
                "shortChannels=${shortEpgChannels.size}, shortRows=${shortEpgEntities.size}"
        )
        dao.replaceProgramsForChannels(syncedChannelIds, xmltvEntities + shortEpgEntities)
    }

    private fun List<EpgProgramEntity>.toGuidePrograms(
        channels: List<Channel>,
        channelsById: Map<String, Channel>
    ): List<GuideProgram> {
        val now = System.currentTimeMillis()
        val programsByChannel = groupBy { it.channelId }
        return channels.map { channel ->
            val programs = programsByChannel[channel.id]
                .orEmpty()
                .filter { it.title.isUsableGuideTitle() }
                .sortedBy { it.startsAtEpochMillis }
            val currentProgram = programs.firstOrNull {
                it.startsAtEpochMillis <= now && it.endsAtEpochMillis > now
            }
            val primaryProgram = currentProgram
                ?: programs.firstOrNull { it.startsAtEpochMillis > now }
                ?: programs.lastOrNull { it.endsAtEpochMillis <= now }
            val nextProgram = programs.firstOrNull {
                primaryProgram != null && it.startsAtEpochMillis > primaryProgram.startsAtEpochMillis
            }
            GuideProgram(
                channel = channel,
                primaryTitle = primaryProgram?.title ?: "",
                secondaryTitle = nextProgram?.title ?: "",
                primaryTime = primaryProgram?.timeRange().orEmpty(),
                secondaryTime = nextProgram?.timeRange().orEmpty(),
                progress = currentProgram?.progressAt(now) ?: 0f,
                startsAtHalfHour = primaryProgram?.startsAtHalfHour() ?: false
            )
        }.filter { channelsById.containsKey(it.channel.id) }
    }

    private fun List<Channel>.toGuidePrograms(): List<GuideProgram> {
        return map { channel ->
            GuideProgram(
                channel = channel,
                primaryTitle = "",
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

    private fun String.isUsableGuideTitle(): Boolean {
        val normalized = lowercase().replace(Regex("\\s+"), " ").trim()
        return normalized.isNotBlank() &&
            normalized !in setOf(
                "epg",
                "no epg",
                "no info",
                "no information",
                "no programme information",
                "program information not available",
                "programme information not available",
                "not available",
                "n/a",
                "na"
            )
    }

    private fun String.normalizedGuideKey(): String {
        return lowercase()
            .replace("&", " and ")
            .replace(Regex("\\b(uk|us|usa|ca|au|nz|ie)\\s*\\|"), " ")
            .replace(Regex("\\b(fhd|hd|uhd|sd|hevc|h265|50fps|60fps)\\b"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun EpgProgramEntity.timeRange(): String {
        return "${startsAtEpochMillis.formatTime()} - ${endsAtEpochMillis.formatTime()}"
    }

    private fun EpgProgramEntity.progressAt(now: Long): Float {
        val duration = (endsAtEpochMillis - startsAtEpochMillis).coerceAtLeast(1L)
        return ((now - startsAtEpochMillis).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    private fun EpgProgramEntity.startsAtHalfHour(): Boolean {
        return Instant.ofEpochMilli(startsAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .minute == 30
    }

    private fun Long.formatTime(): String {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .format(TimeFormatter)
            .lowercase()
    }

    private companion object {
        const val STREAM_KIND_LIVE = "live"
        const val STREAM_KIND_MOVIE = "movie"
        const val MAX_EPG_SYNC_CHANNELS = 420
        const val EPG_PAST_WINDOW_MILLIS = 24L * 60L * 60L * 1_000L
        const val EPG_FUTURE_WINDOW_MILLIS = 24L * 60L * 60L * 1_000L
        const val MIN_FUZZY_GUIDE_KEY_LENGTH = 8
        const val TAG = "StreamHubEpg"
        val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    }
}
