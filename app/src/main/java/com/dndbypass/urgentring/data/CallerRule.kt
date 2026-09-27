package com.dndbypass.urgentring.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Sentinel primary key for the "all callers" fallback rule. */
const val DEFAULT_RULE_KEY = "__default__"

@Entity(tableName = "caller_rules")
data class CallerRule(
    @PrimaryKey val numberOrDefault: String,
    val displayName: String?,
    val thresholdCalls: Int,
    val windowMinutes: Int,
    val enabled: Boolean,
    val neverOverride: Boolean = false
)
