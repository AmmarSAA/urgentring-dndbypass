package com.dndbypass.urgentring.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DebugLogDao {

    @Insert
    suspend fun insert(entry: DebugLogEntry)

    @Query("SELECT * FROM debug_log ORDER BY timestampMillis DESC LIMIT 500")
    fun observeRecent(): Flow<List<DebugLogEntry>>

    @Query("DELETE FROM debug_log")
    suspend fun clear()

    // Keeps the table from growing unbounded across many test calls.
    @Query("DELETE FROM debug_log WHERE id NOT IN (SELECT id FROM debug_log ORDER BY timestampMillis DESC LIMIT 500)")
    suspend fun trimToRecent()
}
