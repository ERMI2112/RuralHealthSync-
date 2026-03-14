package com.example.ruralhealthsync.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ruralhealthsync.databinding.ActivityMainBinding
import com.example.ruralhealthsync.worker.SyncWorker
import java.util.concurrent.TimeUnit

/**
 * The main entry point of the RuralHealthSync application.
 *
 * On startup it schedules a periodic [SyncWorker] via WorkManager that runs
 * every 15 minutes **only when the device has an active network connection**.
 * This ensures unsynced patient records are automatically pushed to the
 * PHP/MySQL backend whenever connectivity is available.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scheduleSyncWork()
    }

    /**
     * Registers a periodic sync task with WorkManager.
     * The task is constrained to run only when a network connection is
     * available, preventing unnecessary failures in offline scenarios.
     */
    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
