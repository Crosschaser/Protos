package com.example.protosApp.manager

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.protosApp.worker.NotificationPollingWorker
import java.util.concurrent.TimeUnit

class NotificationPollingManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun startPeriodicPolling() {
        Log.d(TAG, "Starting periodic notification polling")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<NotificationPollingWorker>(
            5, TimeUnit.MINUTES // Poll every 5 minutes
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15000L, // 15 seconds minimum backoff
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            NotificationPollingWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            periodicWorkRequest
        )
        
        Log.d(TAG, "Periodic notification polling scheduled")
    }

    fun stopPeriodicPolling() {
        Log.d(TAG, "Stopping periodic notification polling")
        workManager.cancelUniqueWork(NotificationPollingWorker.WORK_NAME)
    }

    fun startImmediatePolling() {
        Log.d(TAG, "Starting immediate notification polling")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateWorkRequest = OneTimeWorkRequestBuilder<NotificationPollingWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(immediateWorkRequest)
    }

    fun isPollingActive(): Boolean {
        // Check if periodic work is running
        val workInfos = workManager.getWorkInfosForUniqueWork(NotificationPollingWorker.WORK_NAME)
        return try {
            val workInfoList = workInfos.get()
            workInfoList.any { workInfo ->
                workInfo.state == WorkInfo.State.RUNNING || workInfo.state == WorkInfo.State.ENQUEUED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking polling status", e)
            false
        }
    }

    companion object {
        private const val TAG = "NotificationPollingManager"
    }
}
