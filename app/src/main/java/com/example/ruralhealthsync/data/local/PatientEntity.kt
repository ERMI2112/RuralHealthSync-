package com.example.ruralhealthsync.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a patient record stored locally in the Room (SQLite) database.
 * The [isSynced] flag indicates whether this record has been successfully
 * pushed to the remote MySQL server via the PHP API.
 */
@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fullName: String,
    val age: Int,
    val gender: String,
    val diagnosis: String,
    val isSynced: Boolean = false
)
