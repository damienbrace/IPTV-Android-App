package com.example.iptvapp.data.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface IptvDao {
    @Query("SELECT * FROM playlists ORDER BY lastUpdatedEpochMillis DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM channels ORDER BY number ASC")
    fun observeChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM epg_programs ORDER BY startsAtEpochMillis ASC")
    fun observePrograms(): Flow<List<EpgProgramEntity>>

    @Upsert
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Upsert
    suspend fun upsertChannels(channels: List<ChannelEntity>)

    @Upsert
    suspend fun upsertPrograms(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsForPlaylist(playlistId: String)

    @Query("UPDATE channels SET favorite = :favorite WHERE id = :channelId")
    suspend fun setFavorite(channelId: String, favorite: Boolean)

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannel(channelId: String): ChannelEntity?

    @Transaction
    suspend fun replaceChannelsForPlaylist(playlistId: String, channels: List<ChannelEntity>) {
        deleteChannelsForPlaylist(playlistId)
        upsertChannels(channels)
    }
}
