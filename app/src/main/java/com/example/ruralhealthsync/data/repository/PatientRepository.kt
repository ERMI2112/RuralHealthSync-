package com.example.ruralhealthsync.data.repository

import androidx.lifecycle.LiveData
import com.example.ruralhealthsync.data.local.AppDatabase
import com.example.ruralhealthsync.data.local.PatientEntity
import com.example.ruralhealthsync.data.remote.RetrofitClient

/**
 * Single source of truth for patient data.
 *
 * Coordinates reads/writes between the local Room database (offline storage)
 * and the remote PHP/MySQL API (online sync). The UI should only interact with
 * this repository, never directly with [AppDatabase] or [RetrofitClient].
 */
class PatientRepository(private val database: AppDatabase) {

    private val patientDao = database.patientDao()
    private val apiService = RetrofitClient.apiService

    /** Observable list of all patients from the local database. */
    val allPatients: LiveData<List<PatientEntity>> = patientDao.getAllPatients()

    /**
     * Inserts a new patient into the local Room database.
     * The record is marked as unsynced (isSynced = false) by default.
     */
    suspend fun insertPatient(patient: PatientEntity) {
        patientDao.insert(patient)
    }

    /**
     * Fetches all records not yet pushed to the server, sends them to the API,
     * and marks each successfully synced record in the local database.
     *
     * @return `true` if the sync completed without errors, `false` otherwise.
     */
    suspend fun syncUnsyncedPatients(): Boolean {
        val unsyncedPatients = patientDao.getUnsyncedPatients()
        if (unsyncedPatients.isEmpty()) return true

        return try {
            val response = apiService.syncPatients(unsyncedPatients)
            if (response.isSuccessful) {
                val syncedIds = response.body()?.syncedIds ?: emptyList()
                syncedIds.forEach { id -> patientDao.markAsSynced(id) }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
