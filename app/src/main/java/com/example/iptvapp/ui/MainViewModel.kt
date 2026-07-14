package com.example.iptvapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.iptvapp.data.model.IptvHomeState
import com.example.iptvapp.data.repository.IptvRepository
import com.example.iptvapp.data.repository.LocalIptvRepository
import com.example.iptvapp.sync.PlaylistSyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data object Success : ConnectionTestState
    data class Error(val message: String) : ConnectionTestState
}

sealed interface PlaylistSaveState {
    data object Idle : PlaylistSaveState
    data object Saving : PlaylistSaveState
    data object Success : PlaylistSaveState
    data class Error(val message: String) : PlaylistSaveState
}

sealed interface PlaylistRefreshState {
    data object Idle : PlaylistRefreshState
    data class Syncing(val playlistId: String) : PlaylistRefreshState
    data class Success(val playlistId: String) : PlaylistRefreshState
    data class Error(val playlistId: String, val message: String) : PlaylistRefreshState
}

class MainViewModel(
    application: Application,
    private val repository: IptvRepository = LocalIptvRepository(application)
) : AndroidViewModel(application) {
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY]
                    ?: error("MainViewModel requires an Application")
                MainViewModel(application)
            }
        }
    }

    init {
        PlaylistSyncScheduler.schedule(application)
    }

    val homeState: StateFlow<IptvHomeState> = repository.homeState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = IptvHomeState(
            channels = emptyList(),
            guidePrograms = emptyList(),
            playlists = emptyList(),
            recentSearches = emptyList(),
            categories = emptyList()
        )
    )

    private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

    private val _playlistSaveState = MutableStateFlow<PlaylistSaveState>(PlaylistSaveState.Idle)
    val playlistSaveState: StateFlow<PlaylistSaveState> = _playlistSaveState.asStateFlow()

    private val _playlistRefreshState = MutableStateFlow<PlaylistRefreshState>(PlaylistRefreshState.Idle)
    val playlistRefreshState: StateFlow<PlaylistRefreshState> = _playlistRefreshState.asStateFlow()

    fun addPlaylist(name: String, serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _playlistSaveState.value = PlaylistSaveState.Saving
            val result = repository.addPlaylist(name, serverUrl, username, password)
            _playlistSaveState.value = result.fold(
                onSuccess = {
                    _connectionTestState.value = ConnectionTestState.Idle
                    PlaylistSaveState.Success
                },
                onFailure = { error ->
                    PlaylistSaveState.Error(error.message ?: "Unable to save playlist")
                }
            )
        }
    }

    fun testPlaylistConnection(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _connectionTestState.value = ConnectionTestState.Testing
            val result = repository.testPlaylistConnection(serverUrl, username, password)
            _connectionTestState.value = result.fold(
                onSuccess = { ConnectionTestState.Success },
                onFailure = { error ->
                    ConnectionTestState.Error(error.message ?: "Connection failed")
                }
            )
        }
    }

    fun clearConnectionTest() {
        _connectionTestState.value = ConnectionTestState.Idle
    }

    fun clearPlaylistSaveState() {
        _playlistSaveState.value = PlaylistSaveState.Idle
    }

    fun refreshPlaylist(playlistId: String) {
        viewModelScope.launch {
            _playlistRefreshState.value = PlaylistRefreshState.Syncing(playlistId)
            val result = repository.refreshPlaylist(playlistId)
            _playlistRefreshState.value = result.fold(
                onSuccess = { PlaylistRefreshState.Success(playlistId) },
                onFailure = { error ->
                    PlaylistRefreshState.Error(playlistId, error.message ?: "Unable to resync playlist")
                }
            )
        }
    }

    fun refreshGuide(channelIds: List<String>) {
        viewModelScope.launch {
            repository.refreshGuide(channelIds)
        }
    }

    fun clearPlaylistRefreshState() {
        _playlistRefreshState.value = PlaylistRefreshState.Idle
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(channelId)
        }
    }
}
