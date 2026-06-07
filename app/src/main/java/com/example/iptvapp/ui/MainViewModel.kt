package com.example.iptvapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvapp.data.model.IptvHomeState
import com.example.iptvapp.data.repository.FakeIptvRepository
import com.example.iptvapp.data.repository.IptvRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: IptvRepository = FakeIptvRepository()
) : ViewModel() {
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

    fun addPlaylist(name: String, serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            repository.addPlaylist(name, serverUrl, username, password)
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(channelId)
        }
    }
}
