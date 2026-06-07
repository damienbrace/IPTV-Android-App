package com.example.iptvapp.data.repository

import androidx.compose.ui.graphics.Color
import com.example.iptvapp.data.model.Channel
import com.example.iptvapp.data.model.GuideProgram
import com.example.iptvapp.data.model.IptvHomeState
import com.example.iptvapp.data.model.IptvPlaylist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeIptvRepository : IptvRepository {
    private val channels = listOf(
        Channel("seven-news", 1, "Seven News", "7", Color(0xFFE92B2B), "News", "7:00 - 8:00pm", 0.58f, sampleStream(1)),
        Channel("nine-news", 2, "9 News", "9", Color(0xFF2F9CFF), "News", "7:00 - 8:00pm", 0.58f, sampleStream(2)),
        Channel("ten-news", 3, "10 News First", "10", Color(0xFF286CFF), "News", "7:00 - 8:00pm", 0.58f, sampleStream(3)),
        Channel("abc-news", 4, "ABC News", "ABC", Color(0xFFF2F5FA), "News", "7:00 - 8:00pm", 0.54f, sampleStream(4), favorite = true),
        Channel("sbs-world-news", 5, "SBS World News", "SBS", Color(0xFFF2F5FA), "News", "7:00 - 8:00pm", 0.57f, sampleStream(5), favorite = true),
        Channel("sky-news-live", 6, "Sky News Live", "sky", Color(0xFFE4E8F0), "News", "7:00 - 8:00pm", 0.52f, sampleStream(6)),
        Channel("espn-live", 7, "ESPN Live", "ESPN", Color(0xFFFF3838), "Sports", "6:30 - 8:30pm", 0.64f, sampleStream(7)),
        Channel("fox-sports-503", 8, "Fox Sports 503", "FOX", Color(0xFFF2F5FA), "Sports", "7:00 - 9:00pm", 0.47f, sampleStream(8)),
        Channel("nickelodeon", 9, "Nickelodeon", "nick", Color(0xFFFF981F), "Kids", "7:00 - 8:00pm", 0.33f, sampleStream(9)),
        Channel("discovery-channel", 10, "Discovery Channel", "D", Color(0xFFDDE3EB), "Lifestyle", "7:00 - 8:00pm", 0.42f, sampleStream(10))
    )

    private val guidePrograms = channels.mapIndexed { index, channel ->
        GuideProgram(
            channel = channel,
            primaryTitle = when (channel.id) {
                "ten-news" -> "The Project"
                "abc-news" -> "7.30"
                "sbs-world-news" -> "World News"
                "espn-live" -> "SportsCenter"
                "fox-sports-503" -> "Live: NRL 360"
                "nickelodeon" -> "SpongeBob SquarePants"
                "discovery-channel" -> "Gold Rush"
                else -> channel.name
            },
            secondaryTitle = when (index) {
                0 -> "Home and Away"
                1 -> "A Current Affair"
                2 -> "The Cheap Seats"
                3 -> "Question Time"
                4 -> "Inside Story"
                8 -> "The Loud House"
                else -> ""
            },
            startsAtHalfHour = index % 3 == 2
        )
    }

    private val state = MutableStateFlow(
        IptvHomeState(
            channels = channels,
            guidePrograms = guidePrograms,
            playlists = emptyList(),
            recentSearches = listOf("Seven", "ESPN", "Discovery", "News", "Sports"),
            categories = listOf("All Channels", "Favourites", "News", "Sports", "Kids")
        )
    )

    override val homeState: Flow<IptvHomeState> = state

    override suspend fun addPlaylist(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<IptvPlaylist> {
        val playlist = IptvPlaylist(
            id = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "playlist" },
            name = name,
            serverUrl = serverUrl,
            username = username,
            lastUpdated = "Just now",
            connected = true
        )
        state.update { current ->
            current.copy(playlists = current.playlists.filterNot { it.id == playlist.id } + playlist)
        }
        return Result.success(playlist)
    }

    override suspend fun testPlaylistConnection(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Unit> {
        return when {
            serverUrl.isBlank() -> Result.failure(IllegalArgumentException("Server URL is required"))
            username.isBlank() -> Result.failure(IllegalArgumentException("Username is required"))
            password.isBlank() -> Result.failure(IllegalArgumentException("Password is required"))
            else -> Result.success(Unit)
        }
    }

    override suspend fun toggleFavorite(channelId: String) {
        state.update { current ->
            val updatedChannels = current.channels.map { channel ->
                if (channel.id == channelId) channel.copy(favorite = !channel.favorite) else channel
            }
            val updatedGuide = current.guidePrograms.map { program ->
                val channel = updatedChannels.first { it.id == program.channel.id }
                program.copy(channel = channel)
            }
            current.copy(channels = updatedChannels, guidePrograms = updatedGuide)
        }
    }

    override suspend fun getChannel(channelId: String): Channel? {
        return state.value.channels.firstOrNull { it.id == channelId }
    }

    private fun sampleStream(index: Int): String {
        return "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"
    }
}
