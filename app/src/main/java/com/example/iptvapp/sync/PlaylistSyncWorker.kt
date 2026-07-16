package com.example.iptvapp.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.iptvapp.data.repository.LocalIptvRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaylistSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return workerMutex.withLock {
            val repository = LocalIptvRepository(applicationContext)
            if (!repository.hasSavedPlaylists()) return@withLock Result.success()
            val metadataStore = EpgSyncMetadataStore(applicationContext)
            val forceSync = inputData.getBoolean(PlaylistSyncScheduler.FORCE_SYNC_INPUT, false)
            if (!forceSync && !metadataStore.isSyncDue()) return@withLock Result.success()

            EpgSyncStatusStore.start()
            repository.refreshSavedPlaylists(EpgSyncStatusStore::update)
                .fold(
                    onSuccess = {
                        metadataStore.markCompleted()
                        EpgSyncStatusStore.complete()
                        Result.success()
                    },
                    onFailure = { error ->
                        EpgSyncStatusStore.fail(error.message ?: "Guide sync failed. It will retry later.")
                        Result.retry()
                    }
                )
        }
    }

    private companion object {
        val workerMutex = Mutex()
    }
}
