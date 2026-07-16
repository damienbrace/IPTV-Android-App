package com.example.iptvapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.iptvapp.data.local.ChannelGroupHistoryStore
import com.example.iptvapp.data.local.CountryFilterPreferencesStore
import com.example.iptvapp.data.local.ViewingHistoryStore
import com.example.iptvapp.data.model.CountryGroupFilter
import com.example.iptvapp.data.model.IptvHomeState
import com.example.iptvapp.data.repository.IptvRepository
import com.example.iptvapp.data.repository.LocalIptvRepository
import com.example.iptvapp.sync.PlaylistSyncScheduler
import com.example.iptvapp.sync.EpgSyncStatusStore
import com.example.iptvapp.sync.EpgSyncMetadataStore
import com.example.iptvapp.sync.EpgSyncPriorityController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
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

data class GuideLoadState(
    val channelIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val completedChannels: Int = 0,
    val totalChannels: Int = 0
) {
    val progress: Float
        get() = if (totalChannels > 0) {
            completedChannels.toFloat() / totalChannels.toFloat()
        } else {
            0f
        }
}

class MainViewModel(
    application: Application,
    private val repository: IptvRepository = LocalIptvRepository(application)
) : AndroidViewModel(application) {
    private val channelGroupHistoryStore = ChannelGroupHistoryStore(application)
    private val countryFilterPreferencesStore = CountryFilterPreferencesStore(application)
    private val viewingHistoryStore = ViewingHistoryStore(application)
    private val epgSyncMetadataStore = EpgSyncMetadataStore(application)
    private var guideRefreshJob: Job? = null
    private var guidePreloadJob: Job? = null
    private var frequentGuidePreloadStarted = false
    private var guideRequestToken = 0L

    companion object {
        private val UNTRACKED_GROUPS = setOf(
            "All Channels",
            "Favourites",
            "Recently Watched",
            "Live Sports"
        )
        private const val FREQUENT_GUIDE_PRELOAD_DELAY_MILLIS = 1_500L
        private const val FREQUENT_GUIDE_GROUP_GAP_MILLIS = 250L

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

    val frequentChannelGroups: StateFlow<List<String>> = channelGroupHistoryStore.frequentGroups
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val recentlyWatchedChannelIds: StateFlow<List<String>> = viewingHistoryStore
        .recentlyWatchedChannelIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val enabledCountryFilters: StateFlow<List<CountryGroupFilter>> = countryFilterPreferencesStore
        .enabledFilters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CountryGroupFilter.DefaultFilters
        )

    val epgSyncStatus = EpgSyncStatusStore.status

    fun updateCountryFilters(filters: List<CountryGroupFilter>) {
        viewModelScope.launch {
            countryFilterPreferencesStore.saveEnabledFilters(filters)
        }
    }

    private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

    private val _playlistSaveState = MutableStateFlow<PlaylistSaveState>(PlaylistSaveState.Idle)
    val playlistSaveState: StateFlow<PlaylistSaveState> = _playlistSaveState.asStateFlow()

    private val _playlistRefreshState = MutableStateFlow<PlaylistRefreshState>(PlaylistRefreshState.Idle)
    val playlistRefreshState: StateFlow<PlaylistRefreshState> = _playlistRefreshState.asStateFlow()

    private val _guideLoadState = MutableStateFlow(GuideLoadState())
    val guideLoadState: StateFlow<GuideLoadState> = _guideLoadState.asStateFlow()

    fun addPlaylist(name: String, serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _playlistSaveState.value = PlaylistSaveState.Saving
            val result = repository.addPlaylist(name, serverUrl, username, password)
            _playlistSaveState.value = result.fold(
                onSuccess = {
                    _connectionTestState.value = ConnectionTestState.Idle
                    PlaylistSyncScheduler.syncNow(getApplication())
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
            EpgSyncStatusStore.start()
            val result = repository.refreshPlaylist(playlistId, EpgSyncStatusStore::update)
            _playlistRefreshState.value = result.fold(
                onSuccess = {
                    epgSyncMetadataStore.markCompleted()
                    EpgSyncStatusStore.complete()
                    PlaylistRefreshState.Success(playlistId)
                },
                onFailure = { error ->
                    EpgSyncStatusStore.fail(error.message ?: "Guide sync failed.")
                    PlaylistRefreshState.Error(playlistId, error.message ?: "Unable to resync playlist")
                }
            )
        }
    }

    fun refreshGuide(channelIds: List<String>) {
        guidePreloadJob?.cancel()
        guideRefreshJob?.cancel()
        val requestToken = ++guideRequestToken
        val requestedIds = channelIds.toSet()
        guideRefreshJob = viewModelScope.launch {
            EpgSyncPriorityController.beginForegroundGuideLoad()
            _guideLoadState.value = GuideLoadState(
                channelIds = requestedIds,
                isLoading = true
            )
            try {
                repository.refreshGuide(channelIds) { completed, total ->
                    if (requestToken == guideRequestToken) {
                        val currentState = _guideLoadState.value
                        if (
                            currentState.totalChannels != total ||
                            completed >= currentState.completedChannels
                        ) {
                            _guideLoadState.value = GuideLoadState(
                                channelIds = requestedIds,
                                isLoading = true,
                                completedChannels = completed,
                                totalChannels = total
                            )
                        }
                    }
                }
            } finally {
                EpgSyncPriorityController.endForegroundGuideLoad()
                if (requestToken == guideRequestToken) {
                    _guideLoadState.value = GuideLoadState(channelIds = requestedIds)
                }
            }
        }
    }

    fun recordChannelGroupVisit(group: String) {
        if (group in UNTRACKED_GROUPS) return
        viewModelScope.launch {
            channelGroupHistoryStore.recordVisit(group)
        }
    }

    fun recordChannelWatch(channelId: String) {
        viewModelScope.launch {
            viewingHistoryStore.recordChannel(channelId)
        }
    }

    fun preloadFrequentGuideGroups(channelGroups: List<List<String>>) {
        if (channelGroups.isEmpty() || frequentGuidePreloadStarted) return
        frequentGuidePreloadStarted = true
        guidePreloadJob?.cancel()
        guidePreloadJob = viewModelScope.launch {
            delay(FREQUENT_GUIDE_PRELOAD_DELAY_MILLIS)
            if (guideRefreshJob?.isActive == true) return@launch
            channelGroups.forEach { channelIds ->
                repository.preloadGuide(channelIds)
                delay(FREQUENT_GUIDE_GROUP_GAP_MILLIS)
            }
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
