package com.dndbypass.urgentring.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallEventDao {

    @Insert
    suspend fun insert(event: CallEvent): Long

    @Query(
        "SELECT * FROM call_events WHERE normalizedNumber = :number AND timestampMillis >= :sinceMillis " +
            "ORDER BY timestampMillis DESC"
    )
    suspend fun eventsInWindow(number: String, sinceMillis: Long): List<CallEvent>

    @Query("DELETE FROM call_events WHERE timestampMillis < :beforeMillis")
    suspend fun pruneOlderThan(beforeMillis: Long)

    @Query("UPDATE call_events SET ringOverridden = 1 WHERE id = :id")
    suspend fun markRingOverridden(id: Long)

    @Query("SELECT * FROM call_events ORDER BY timestampMillis DESC LIMIT 200")
    fun recentEvents(): Flow<List<CallEvent>>
}
