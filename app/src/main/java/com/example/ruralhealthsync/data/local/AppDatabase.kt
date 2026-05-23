package com.example.ruralhealthsync.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database for the RuralHealthSync application.
 * This is the main access point for the underlying SQLite database.
 */
@Database(
    entities = [PatientEntity::class, PendingDeletionEntity::class, VisitEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao

    abstract fun pendingDeletionDao(): PendingDeletionDao
    
    abstract fun visitDao(): VisitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of [AppDatabase].
         * Creates it if it does not exist yet using double-checked locking.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rural_health_sync.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
