package com.example.ruralhealthsync

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.ruralhealthsync.data.local.AppDatabase
import com.example.ruralhealthsync.data.local.PreferenceManager

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
        AppCompatDelegate.setDefaultNightMode(PreferenceManager(this).getThemeMode())
        database = AppDatabase.getInstance(this)
    }
}
