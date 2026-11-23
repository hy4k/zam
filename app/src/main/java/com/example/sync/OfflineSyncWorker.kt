package com.example.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.KinNestDatabase
import java.util.concurrent.TimeUnit

/**
 * WorkManager Offline Sync Worker:
 * Processes queued E2EE messages, Heartbeats, and Memory Capsules cleanly on reconnection.
 * Supports expedited execution for urgent Heartbeat/Safety alerts and exponential backoff for media.
 */
class OfflineSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = KinNestDatabase.getInstance(applicationContext).kinNestDao()
        val repository = KinNestSyncRepository(dao)
        val pending = dao.getPendingSyncItems()
        var anyFailed = false

        for (item in pending) {
            val pushResult = repository.pushPendingQueueItem(item)
            if (pushResult.isSuccess) {
                dao.updateSyncItemStatus(
                    id = item.id,
                    status = "SYNCED",
                    retryCount = item.retryCount,
                    lastError = null
                )
            } else {
                anyFailed = true
                dao.updateSyncItemStatus(
                    id = item.id,
                    status = "FAILED",
                    retryCount = item.retryCount + 1,
                    lastError = pushResult.exceptionOrNull()?.localizedMessage ?: "Network retry queued"
                )
            }
        }

        dao.pruneSyncedItems()
        return if (anyFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_SYNC_WORK = "kinnest_offline_sync_work"

        fun enqueueSync(context: Context, expedited: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val builder = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)

            if (expedited) {
                builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    UNIQUE_SYNC_WORK,
                    ExistingWorkPolicy.REPLACE,
                    builder.build()
                )
        }
    }
}
