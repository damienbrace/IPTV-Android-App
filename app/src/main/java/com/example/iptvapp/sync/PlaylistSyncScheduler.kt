package com.example.iptvapp.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object PlaylistSyncScheduler {
    private const val WORK_NAME = "playlist_background_sync"
    private const val IMMEDIATE_WORK_NAME = "playlist_immediate_sync"
    internal const val FORCE_SYNC_INPUT = "force_sync"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<PlaylistSyncWorker>(
            24,
            TimeUnit.HOURS,
            2,
            TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        syncIfDue(context)
    }

    fun syncNow(context: Context) {
        enqueueImmediate(context, force = true)
    }

    private fun syncIfDue(context: Context) {
        enqueueImmediate(context, force = false)
    }

    private fun enqueueImmediate(context: Context, force: Boolean) {
        val request = OneTimeWorkRequestBuilder<PlaylistSyncWorker>()
            .setInputData(workDataOf(FORCE_SYNC_INPUT to force))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }
}
