package com.dndbypass.urgentring.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationShareContactDao {

    @Query("SELECT * FROM location_share_contacts ORDER BY displayName, normalizedNumber")
    fun observeAll(): Flow<List<LocationShareContact>>

    @Query("SELECT * FROM location_share_contacts WHERE normalizedNumber = :number LIMIT 1")
    suspend fun find(number: String): LocationShareContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: LocationShareContact)

    @Query("UPDATE location_share_contacts SET lastSentMillis = :millis WHERE normalizedNumber = :number")
    suspend fun markSent(number: String, millis: Long)

    @Delete
    suspend fun delete(contact: LocationShareContact)
}
