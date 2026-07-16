package com.example.iptvapp.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EpgSyncStatus(
    val isRunning: Boolean = false,
    val progressPercent: Int? = null,
    val message: String = "Daily guide sync is scheduled.",
    val isError: Boolean = false
)

object EpgSyncStatusStore {
    private val _status = MutableStateFlow(EpgSyncStatus())
    val status: StateFlow<EpgSyncStatus> = _status.asStateFlow()

    fun start(message: String = "Preparing guide sync...") {
        _status.value = EpgSyncStatus(isRunning = true, message = message)
    }

    fun update(progress: Float?, message: String) {
        _status.value = EpgSyncStatus(
            isRunning = true,
            progressPercent = progress
                ?.coerceIn(0f, 1f)
                ?.times(100f)
                ?.toInt(),
            message = message
        )
    }

    fun complete() {
        _status.value = EpgSyncStatus(
            progressPercent = 100,
            message = "Full guide sync complete."
        )
    }

    fun pauseForForegroundGuide() {
        _status.value = _status.value.copy(
            message = "Paused while loading the selected group..."
        )
    }

    fun resumeAfterForegroundGuide() {
        _status.value = _status.value.copy(
            message = "Resuming full guide sync..."
        )
    }

    fun fail(message: String) {
        _status.value = EpgSyncStatus(
            message = message,
            isError = true
        )
    }
}
