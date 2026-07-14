package com.example.iptvapp.data.model

import androidx.compose.ui.graphics.Color

data class Channel(
    val id: String,
    val number: Int,
    val name: String,
    val logo: String,
    val logoColor: Color,
    val category: String,
    val currentProgramTime: String,
    val progress: Float,
    val streamUrl: String,
    val logoUrl: String? = null,
    val favorite: Boolean = false
)

data class GuideProgramBlock(
    val title: String,
    val time: String,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val progress: Float = 0f,
    val isCurrent: Boolean = false,
    val isLiveEvent: Boolean = false
)

data class GuideProgram(
    val channel: Channel,
    val primaryTitle: String,
    val secondaryTitle: String,
    val primaryTime: String = "",
    val secondaryTime: String = "",
    val progress: Float = 0f,
    val startsAtHalfHour: Boolean = false,
    val timeline: List<GuideProgramBlock> = emptyList()
)

data class IptvPlaylist(
    val id: String,
    val name: String,
    val serverUrl: String,
    val username: String,
    val lastUpdated: String,
    val connected: Boolean
)

data class IptvHomeState(
    val channels: List<Channel>,
    val guidePrograms: List<GuideProgram>,
    val playlists: List<IptvPlaylist>,
    val recentSearches: List<String>,
    val categories: List<String>
)
