package com.dndbypass.urgentring.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A caller who gets the user's location after a missed call (see LocationShareManager). */
@Entity(tableName = "location_share_contacts")
data class LocationShareContact(
    @PrimaryKey val normalizedNumber: String,
    val displayName: String?,
    val lastSentMillis: Long = 0
)
