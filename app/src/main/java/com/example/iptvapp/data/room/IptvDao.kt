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

    @Query("SELECT * FROM playlists ORDER BY lastUpdatedEpochMillis DESC")
    suspend fun getPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM channels ORDER BY number ASC")
    fun observeChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM epg_programs WHERE channelId IN (:channelIds) ORDER BY startsAtEpochMillis ASC")
    fun observePrograms(channelIds: List<String>): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM channels WHERE id IN (:channelIds)")
    suspend fun getChannels(channelIds: List<String>): List<ChannelEntity>

    @Query("SELECT * FROM epg_programs WHERE channelId IN (:channelIds) ORDER BY startsAtEpochMillis ASC")
    suspend fun getPrograms(channelIds: List<String>): List<EpgProgramEntity>

    @Upsert
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Upsert
    suspend fun upsertChannels(channels: List<ChannelEntity>)

    @Upsert
    suspend fun upsertPrograms(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsForPlaylist(playlistId: String)

    @Query("DELETE FROM epg_programs WHERE channelId IN (:channelIds)")
    suspend fun deleteProgramsForChannels(channelIds: List<String>)

    @Query("DELETE FROM epg_programs WHERE channelId IN (SELECT id FROM channels WHERE playlistId = :playlistId)")
    suspend fun deleteProgramsForPlaylist(playlistId: String)

    @Query("UPDATE channels SET favorite = :favorite WHERE id = :channelId")
    suspend fun setFavorite(channelId: String, favorite: Boolean)

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannel(channelId: String): ChannelEntity?

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylist(playlistId: String): PlaylistEntity?

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistRow(playlistId: String)

    @Transaction
    suspend fun replaceChannelsForPlaylist(playlistId: String, channels: List<ChannelEntity>) {
        deleteChannelsForPlaylist(playlistId)
        upsertChannels(channels)
    }

    @Transaction
    suspend fun replaceProgramsForChannels(channelIds: List<String>, programs: List<EpgProgramEntity>) {
        if (channelIds.isNotEmpty()) {
            channelIds.chunked(SQL_VARIABLE_BATCH_SIZE).forEach { ids ->
                deleteProgramsForChannels(ids)
            }
        }
        if (programs.isNotEmpty()) {
            upsertPrograms(programs)
        }
    }

    @Transaction
    suspend fun deletePlaylist(playlistId: String) {
        deleteProgramsForPlaylist(playlistId)
        deleteChannelsForPlaylist(playlistId)
        deletePlaylistRow(playlistId)
    }

    companion object {
        private const val SQL_VARIABLE_BATCH_SIZE = 500
    }
}
