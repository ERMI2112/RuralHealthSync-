package com.example.ruralhealthsync.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a patient record stored locally in the Room (SQLite) database.
 */
@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: Int = 0,
    val workerId: Int = 0,
    val fullName: String,
    val age: Int,
    val gender: String,
    val phone: String = "",
    val address: String = "",
    val diagnosis: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val nextVisitDate: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isSynced: Boolean = false,
    
    // New health fields
    val bloodType: String = "",
    val allergies: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val currentMedications: String = "",
    
    // Vital signs
    val bloodPressureSystolic: Int = 0,
    val bloodPressureDiastolic: Int = 0,
    val temperature: Float = 0f,
    val weight: Float = 0f,
    val height: Float = 0f,
    
    // Chronic conditions (comma-separated)
    val chronicConditions: String = ""
)
