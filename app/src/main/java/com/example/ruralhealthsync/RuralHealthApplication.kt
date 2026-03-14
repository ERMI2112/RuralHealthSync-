package com.example.ruralhealthsync

import android.app.Application
import com.example.ruralhealthsync.data.local.AppDatabase

/**
 * Custom [Application] class for RuralHealthSync.
 *
 * Initializes the Room database singleton at application startup so it is
 * ready before any Activity or Worker requests it.
 */
class RuralHealthApplication : Application() {

    /** Room database instance, initialized when the application is created. */
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
    }
}
