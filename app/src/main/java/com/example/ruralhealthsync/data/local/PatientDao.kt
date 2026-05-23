package com.example.ruralhealthsync.data.local

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
@JvmSuppressWildcards
interface PatientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: PatientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(patients: List<PatientEntity>): List<Long>

    @Update
    suspend fun update(patient: PatientEntity): Int

    @Delete
    suspend fun delete(patient: PatientEntity): Int

    @Query("SELECT * FROM patients WHERE workerId = :workerId ORDER BY createdAt DESC")
    fun getPatientsForWorker(workerId: Int): LiveData<List<PatientEntity>>

    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    fun getAllPatients(): LiveData<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE workerId = :workerId AND (fullName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchPatients(workerId: Int, query: String): LiveData<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE (fullName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchAllPatients(query: String): LiveData<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE isSynced = 0")
    suspend fun getUnsyncedPatients(): List<PatientEntity>

    @Query("SELECT COUNT(*) FROM patients WHERE isSynced = 0")
    fun getUnsyncedCount(): LiveData<Int>

    @Query("UPDATE patients SET isSynced = 1 WHERE id = :patientId")
    suspend fun markAsSynced(patientId: Int): Int

    @Query("UPDATE patients SET isSynced = 1, serverId = :serverId WHERE id = :patientId")
    suspend fun markAsSyncedWithServerId(patientId: Int, serverId: Int): Int

    @Query("SELECT * FROM patients WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): PatientEntity?

    @Query("UPDATE patients SET photoUrl = :url WHERE id = :patientId")
    suspend fun updatePhotoUrl(patientId: Int, url: String): Int

    @Query("SELECT * FROM patients WHERE fullName = :name AND phone = :phone LIMIT 1")
    suspend fun checkDuplicatePatient(name: String, phone: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE fullName = :name AND phone = :phone AND id != :excludeId LIMIT 1")
    suspend fun checkDuplicatePatientExcluding(name: String, phone: String, excludeId: Int): PatientEntity?

    @Query(
        """
        SELECT * FROM patients
        WHERE id != :excludeId
        AND REPLACE(LOWER(TRIM(fullName)), ' ', '') = :collapsedName
        ORDER BY createdAt DESC
        """
    )
    suspend fun findPatientsByCollapsedName(collapsedName: String, excludeId: Int): List<PatientEntity>

    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    suspend fun getById(patientId: Int): PatientEntity?

    @Query("DELETE FROM patients WHERE id = :patientId")
    suspend fun deleteById(patientId: Int): Int

    @Query("SELECT * FROM patients WHERE latitude != 0.0 AND longitude != 0.0")
    suspend fun getPatientsWithLocation(): List<PatientEntity>
}
