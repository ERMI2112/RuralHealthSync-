package com.example.ruralhealthsync.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single visit or checkup for a patient.
 * Uses a foreign key to link to the patient, ensuring if a patient is deleted,
 * their visit history is also deleted (CASCADE).
 */
@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patientId")]
)
data class VisitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val patientId: Int,
    val visitDate: Long = System.currentTimeMillis(),
    
    // Symptoms Checklist (stored as comma-separated string or JSON)
    val symptoms: String = "",
    
    // Doctor/CHW Notes
    val notes: String = "",
    
    // Prescriptions tracking
    val prescription: String = "",
    
    // Follow-up reminder date (0 if none)
    val followUpDate: Long = 0L,
    
    // To sync with remote server
    val isSynced: Boolean = false
)
