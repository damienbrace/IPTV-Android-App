package com.example.iptvapp.sync

object EpgSyncPriorityController {
    private val monitor = Object()
    private var foregroundGuideRequests = 0

    fun beginForegroundGuideLoad() {
        synchronized(monitor) {
            foregroundGuideRequests++
        }
        if (EpgSyncStatusStore.status.value.isRunning) {
            EpgSyncStatusStore.pauseForForegroundGuide()
        }
    }

    fun endForegroundGuideLoad() {
        val shouldResume = synchronized(monitor) {
            foregroundGuideRequests = (foregroundGuideRequests - 1).coerceAtLeast(0)
            if (foregroundGuideRequests == 0) {
                monitor.notifyAll()
                true
            } else {
                false
            }
        }
        if (shouldResume && EpgSyncStatusStore.status.value.isRunning) {
            EpgSyncStatusStore.resumeAfterForegroundGuide()
        }
    }

    fun awaitBackgroundTurn() {
        synchronized(monitor) {
            while (foregroundGuideRequests > 0) {
                monitor.wait(250L)
            }
        }
    }
}
