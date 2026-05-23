package com.example.ruralhealthsync.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.lifecycle.LiveData

@Dao
@JvmSuppressWildcards
interface VisitDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity): Long
    
    @Query("SELECT * FROM visits WHERE patientId = :patientId ORDER BY visitDate DESC")
    fun getVisitsForPatient(patientId: Int): LiveData<List<VisitEntity>>
    
    @Query("SELECT * FROM visits WHERE isSynced = 0")
    suspend fun getUnsyncedVisits(): List<VisitEntity>
    
    @Query("UPDATE visits SET isSynced = 1 WHERE id = :visitId")
    suspend fun markAsSynced(visitId: Int): Int
    
    @Query("DELETE FROM visits WHERE id = :visitId")
    suspend fun deleteVisit(visitId: Int): Int
}
