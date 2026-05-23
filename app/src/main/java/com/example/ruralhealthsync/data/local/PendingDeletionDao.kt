package com.example.ruralhealthsync.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
@JvmSuppressWildcards
interface PendingDeletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deletion: PendingDeletionEntity): Long

    @Query("SELECT serverId FROM pending_deletions WHERE serverId > 0")
    suspend fun getPendingServerIds(): List<Int>

    @Query("SELECT * FROM pending_deletions")
    suspend fun getAll(): List<PendingDeletionEntity>

    @Query("DELETE FROM pending_deletions WHERE serverId IN (:serverIds)")
    suspend fun clearServerIds(serverIds: List<Int>): Int

    @Query("DELETE FROM pending_deletions WHERE fullName = :name AND phone = :phone")
    suspend fun clearByNamePhone(name: String, phone: String): Int

    @Query("DELETE FROM pending_deletions")
    suspend fun clearAll(): Int
}
