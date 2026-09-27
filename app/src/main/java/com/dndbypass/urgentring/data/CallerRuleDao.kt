package com.dndbypass.urgentring.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallerRuleDao {

    @Query("SELECT * FROM caller_rules WHERE numberOrDefault = :key LIMIT 1")
    suspend fun findByKey(key: String): CallerRule?

    @Query("SELECT * FROM caller_rules ORDER BY (numberOrDefault = '$DEFAULT_RULE_KEY') DESC, displayName")
    fun observeAll(): Flow<List<CallerRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CallerRule)

    @Delete
    suspend fun delete(rule: CallerRule)
}
