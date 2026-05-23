package com.example.ruralhealthsync.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks patients deleted on-device so sync does not re-import them from the server.
 * [serverId] is sent to the API when > 0; [fullName]/[phone] block re-import for local-only records.
 */
@Entity(tableName = "pending_deletions")
data class PendingDeletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: Int = 0,
    val fullName: String = "",
    val phone: String = ""
)
