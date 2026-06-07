package com.example.iptvapp.data.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val serverUrl: String,
    val username: String,
    val encryptedPassword: String,
    val lastUpdatedEpochMillis: Long,
    val connected: Boolean
)

@Entity(
    tableName = "channels",
    indices = [
        Index("playlistId"),
        Index("category"),
        Index("name")
    ]
)
data class ChannelEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val number: Int,
    val name: String,
    val logoUrl: String?,
    val category: String,
    val streamUrl: String,
    val favorite: Boolean
)

@Entity(
    tableName = "epg_programs",
    indices = [
        Index("channelId"),
        Index(value = ["channelId", "startsAtEpochMillis", "endsAtEpochMillis"])
    ]
)
data class EpgProgramEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val title: String,
    val description: String?,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long
)
