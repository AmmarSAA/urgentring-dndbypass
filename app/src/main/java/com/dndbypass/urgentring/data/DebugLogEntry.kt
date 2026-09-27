package com.dndbypass.urgentring.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debug_log")
data class DebugLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val message: String
)
