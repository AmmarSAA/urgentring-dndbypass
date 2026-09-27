package com.dndbypass.urgentring.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallEvent::class, CallerRule::class, DebugLogEntry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callEventDao(): CallEventDao
    abstract fun callerRuleDao(): CallerRuleDao
    abstract fun debugLogDao(): DebugLogDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "urgent_ring.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
