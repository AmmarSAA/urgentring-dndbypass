package com.dndbypass.urgentring.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_events")
data class CallEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val normalizedNumber: String,
    val timestampMillis: Long,
    val ringOverridden: Boolean
)
