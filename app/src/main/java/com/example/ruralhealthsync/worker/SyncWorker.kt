package com.example.ruralhealthsync.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ruralhealthsync.data.local.AppDatabase
import com.example.ruralhealthsync.data.repository.PatientRepository

/**
 * A [CoroutineWorker] that runs in the background to push unsynced patient
 * records from the local Room database to the remote PHP/MySQL API.
 *
 * This worker is scheduled by [com.example.ruralhealthsync.ui.MainActivity]
 * via WorkManager to run periodically (every 15 minutes) whenever the device
 * has a network connection.
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(applicationContext)
        val repository = PatientRepository(database)

        val success = repository.syncUnsyncedPatients()
        return if (success) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "com.example.ruralhealthsync.SyncWorker"
    }
}
