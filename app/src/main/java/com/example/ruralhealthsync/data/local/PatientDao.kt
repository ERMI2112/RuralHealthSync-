package com.example.ruralhealthsync.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for [PatientEntity].
 * Provides methods for inserting and querying patient records in the local database.
 */
@Dao
interface PatientDao {

    /**
     * Inserts a new patient record. If a conflict occurs (same primary key),
     * the existing record is replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: PatientEntity)

    /**
     * Returns all patient records as a [LiveData] list so the UI can
     * observe changes reactively.
     */
    @Query("SELECT * FROM patients ORDER BY fullName ASC")
    fun getAllPatients(): LiveData<List<PatientEntity>>

    /**
     * Returns all patient records that have not yet been pushed to the
     * remote server (isSynced = 0).
     */
    @Query("SELECT * FROM patients WHERE isSynced = 0")
    suspend fun getUnsyncedPatients(): List<PatientEntity>

    /**
     * Marks a patient record as synced after successful upload to the API.
     */
    @Query("UPDATE patients SET isSynced = 1 WHERE id = :patientId")
    suspend fun markAsSynced(patientId: Int)
}
