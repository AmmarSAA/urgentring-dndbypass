package com.dndbypass.urgentring.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persists a rolling trace of screening decisions so they can be inspected from the
 * Debug logs screen. This exists because the phones this app runs on often can't be
 * reached with adb (no USB debugging, MIUI, etc.), so logcat isn't an option for the
 * person testing it — an in-app, copy-to-clipboard log is.
 */
object DebugLogger {
    private const val TAG = "UrgentRingDebug"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun log(context: Context, message: String) {
        Log.i(TAG, message)
        scope.launch {
            val dao = AppDatabase.get(context).debugLogDao()
            dao.insert(DebugLogEntry(timestampMillis = System.currentTimeMillis(), message = message))
            dao.trimToRecent()
        }
    }

    fun format(entry: DebugLogEntry): String = "${formatter.format(Date(entry.timestampMillis))}  ${entry.message}"
}
