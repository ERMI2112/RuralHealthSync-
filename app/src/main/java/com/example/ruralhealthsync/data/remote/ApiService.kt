package com.example.ruralhealthsync.data.remote

import com.example.ruralhealthsync.data.local.PatientEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface defining the remote API endpoints for the PHP backend.
 */
interface ApiService {

    /**
     * Sends a list of unsynced patient records to the remote server.
     *
     * @param patients The list of [PatientEntity] objects to synchronize.
     * @return A [Response] wrapping a [SyncResponse] that reports success/failure.
     */
    @POST("sync_patients.php")
    suspend fun syncPatients(@Body patients: List<PatientEntity>): Response<SyncResponse>
}

/**
 * Data class representing the JSON response from the sync endpoint.
 */
data class SyncResponse(
    val success: Boolean,
    val message: String,
    val syncedIds: List<Int>
)
