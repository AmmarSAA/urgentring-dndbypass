package com.dndbypass.urgentring.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CallEvent::class, CallerRule::class, DebugLogEntry::class, LocationShareContact::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callEventDao(): CallEventDao
    abstract fun callerRuleDao(): CallerRuleDao
    abstract fun debugLogDao(): DebugLogDao
    abstract fun locationShareContactDao(): LocationShareContactDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // Adds the location-sharing caller list without wiping the user's existing rules.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `location_share_contacts` (" +
                        "`normalizedNumber` TEXT NOT NULL, `displayName` TEXT, " +
                        "`lastSentMillis` INTEGER NOT NULL, PRIMARY KEY(`normalizedNumber`))"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "urgent_ring.db"
                ).addMigrations(MIGRATION_2_3).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
