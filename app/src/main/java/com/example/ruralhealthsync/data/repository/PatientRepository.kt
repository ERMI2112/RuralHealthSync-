package com.example.ruralhealthsync.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.ruralhealthsync.data.local.AppDatabase
import com.example.ruralhealthsync.data.local.PatientEntity
import com.example.ruralhealthsync.data.local.PendingDeletionEntity
import com.example.ruralhealthsync.data.remote.RetrofitClient
import com.example.ruralhealthsync.data.remote.SyncRequest
import org.json.JSONObject

class PatientRepository(
    private val database: AppDatabase,
    private val context: Context
) {

    data class DuplicatePatientMatch(
        val patient: PatientEntity,
        val matchedByPhone: Boolean
    )

    private val patientDao = database.patientDao()
    private val pendingDeletionDao = database.pendingDeletionDao()
    private val apiService get() = RetrofitClient.getApiService(context)

    fun getPatientsForWorker(workerId: Int): LiveData<List<PatientEntity>> {
        return patientDao.getPatientsForWorker(workerId)
    }

    fun getAllPatients(): LiveData<List<PatientEntity>> {
        return patientDao.getAllPatients()
    }

    fun searchPatients(workerId: Int, query: String): LiveData<List<PatientEntity>> {
        return patientDao.searchPatients(workerId, query)
    }

    fun searchAllPatients(query: String): LiveData<List<PatientEntity>> {
        return patientDao.searchAllPatients(query)
    }

    fun getUnsyncedCount(): LiveData<Int> {
        return patientDao.getUnsyncedCount()
    }

    suspend fun insertPatient(patient: PatientEntity) {
        patientDao.insert(patient)
    }

    suspend fun checkDuplicatePatient(name: String, phone: String): PatientEntity? {
        return patientDao.checkDuplicatePatient(name, phone)
    }

    suspend fun findDuplicatePatient(
        name: String,
        age: Int,
        gender: String,
        phone: String,
        excludePatientId: Int = 0
    ): DuplicatePatientMatch? {
        val normalizedName = normalizeName(name)
        if (normalizedName.isEmpty()) return null

        val normalizedPhone = normalizePhone(phone)
        if (normalizedPhone.isNotEmpty()) {
            val exactPhoneMatch = if (excludePatientId > 0) {
                patientDao.checkDuplicatePatientExcluding(name.trim(), phone.trim(), excludePatientId)
            } else {
                patientDao.checkDuplicatePatient(name.trim(), phone.trim())
            }
            if (exactPhoneMatch != null) {
                return DuplicatePatientMatch(exactPhoneMatch, matchedByPhone = true)
            }
        }

        val normalizedGender = gender.trim().lowercase()
        val candidates = patientDao.findPatientsByCollapsedName(
            collapsedName = normalizedName.replace(" ", ""),
            excludeId = excludePatientId
        )

        val phoneMatch = candidates.firstOrNull { candidate ->
            normalizedPhone.isNotEmpty() && normalizePhone(candidate.phone) == normalizedPhone
        }
        if (phoneMatch != null) {
            return DuplicatePatientMatch(phoneMatch, matchedByPhone = true)
        }

        return candidates.firstOrNull { candidate ->
            candidate.age == age && gendersMatch(candidate.gender, normalizedGender)
        }?.let { DuplicatePatientMatch(it, matchedByPhone = false) }
    }

    private fun normalizeName(name: String): String {
        return name.trim().lowercase().replace(Regex("\\s+"), " ")
    }

    private fun normalizePhone(phone: String): String {
        return phone.filter(Char::isDigit)
    }

    private fun gendersMatch(candidateGender: String, normalizedGender: String): Boolean {
        if (normalizedGender.isBlank()) return true
        return candidateGender.trim().lowercase() == normalizedGender
    }

    suspend fun deletePatient(patientId: Int): Boolean {
        val patient = patientDao.getById(patientId) ?: return false
        recordPendingDeletion(patient)
        return patientDao.deleteById(patientId) > 0
    }

    private suspend fun recordPendingDeletion(patient: PatientEntity) {
        pendingDeletionDao.insert(
            PendingDeletionEntity(
                serverId = patient.serverId,
                fullName = patient.fullName.trim(),
                phone = patient.phone.trim()
            )
        )
    }

    suspend fun getPatientById(patientId: Int): PatientEntity? {
        return patientDao.getById(patientId)
    }

    suspend fun getPatientsWithLocation(): List<PatientEntity> {
        return patientDao.getPatientsWithLocation()
    }

    suspend fun syncUnsyncedPatients(workerId: Int): Boolean {
        val unsynced = patientDao.getUnsyncedPatients()
        if (unsynced.isEmpty()) return true

        return try {
            val deletedServerIds = pendingDeletionDao.getPendingServerIds()
            val request = SyncRequest(
                workerId = workerId,
                patients = unsynced,
                deletedServerIds = deletedServerIds
            )
            val response = apiService.syncPatientsFull(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                applySyncedRecords(body)
                if (deletedServerIds.isNotEmpty()) {
                    pendingDeletionDao.clearServerIds(deletedServerIds)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun performFullSync(workerId: Int): Pair<Boolean, String> {
        val unsynced = patientDao.getUnsyncedPatients()
        val deletedServerIds = pendingDeletionDao.getPendingServerIds()

        return try {
            val request = SyncRequest(
                workerId = workerId,
                patients = unsynced,
                deletedServerIds = deletedServerIds
            )
            val response = apiService.syncPatientsFull(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!

                applySyncedRecords(body)

                if (deletedServerIds.isNotEmpty()) {
                    pendingDeletionDao.clearServerIds(deletedServerIds)
                }

                body.patients?.let { remotePatients ->
                    mergeRemotePatients(remotePatients)
                }
                val syncedCount = body.syncedRecords?.size ?: body.syncedIds?.size ?: 0
                val deletedCount = deletedServerIds.size
                val message = buildString {
                    append("Sync successful!")
                    if (syncedCount > 0) append(" $syncedCount uploaded.")
                    if (deletedCount > 0) append(" $deletedCount removed on server.")
                }
                Pair(true, message)
            } else {
                Pair(
                    false,
                    extractSyncFailureMessage(response.errorBody()?.string())
                        ?: response.body()?.message?.takeIf { it.isNotBlank() }
                        ?: "Sync failed"
                )
            }
        } catch (e: Exception) {
            Pair(false, buildSyncErrorMessage(e))
        }
    }

    private fun buildSyncErrorMessage(error: Exception): String {
        val message = error.message.orEmpty()
        return if (
            message.contains("JsonReader.setLenient", ignoreCase = true) ||
            message.contains("Malformed JSON", ignoreCase = true)
        ) {
            "Sync failed because the server returned an invalid response."
        } else {
            "Error: ${error.message}"
        }
    }

    private fun extractSyncFailureMessage(errorBody: String?): String? {
        val payload = errorBody?.trim().orEmpty()
        if (payload.isEmpty()) return null

        return try {
            JSONObject(payload).optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            payload.take(180).takeIf { it.isNotBlank() }
        }
    }

    private suspend fun applySyncedRecords(body: com.example.ruralhealthsync.data.remote.SyncResponse) {
        val syncedRecords = body.syncedRecords
        if (!syncedRecords.isNullOrEmpty()) {
            syncedRecords.forEach { record ->
                val localId = record.localId ?: return@forEach
                val serverId = record.serverId ?: return@forEach
                if (localId > 0 && serverId > 0) {
                    patientDao.markAsSyncedWithServerId(localId, serverId)
                }
            }
        } else {
            body.syncedIds?.forEach { id ->
                if (id > 0) patientDao.markAsSynced(id)
            }
        }
    }

    private suspend fun mergeRemotePatients(remotePatients: List<com.example.ruralhealthsync.data.remote.RemotePatientDto>) {
        val tombstones = pendingDeletionDao.getAll()
        val blockedServerIds = tombstones.map { it.serverId }.filter { it > 0 }.toSet()

        remotePatients.forEach { dto ->
            val remote = dto.toEntity()
            val serverId = remote.serverId

            if (serverId > 0 && serverId in blockedServerIds) {
                return@forEach
            }

            if (isBlockedByTombstone(remote.fullName, remote.phone, tombstones)) {
                return@forEach
            }

            if (serverId > 0) {
                val existing = patientDao.getByServerId(serverId)
                if (existing != null) {
                    // Preserve local photo if remote doesn't have one
                    val photoToKeep = if (remote.photoUrl.isEmpty() && existing.photoUrl.isNotEmpty()) {
                        existing.photoUrl
                    } else {
                        remote.photoUrl
                    }
                    patientDao.update(remote.copy(
                        id = existing.id, 
                        isSynced = true,
                        photoUrl = photoToKeep
                    ))
                } else {
                    patientDao.insert(remote)
                }
            } else {
                patientDao.insert(remote)
            }
        }
    }

    private fun isBlockedByTombstone(
        fullName: String,
        phone: String,
        tombstones: List<PendingDeletionEntity>
    ): Boolean {
        val name = fullName.trim()
        val phoneNorm = phone.trim()
        return tombstones.any { tomb ->
            tomb.fullName.equals(name, ignoreCase = true) &&
                tomb.phone.equals(phoneNorm, ignoreCase = true)
        }
    }
}
