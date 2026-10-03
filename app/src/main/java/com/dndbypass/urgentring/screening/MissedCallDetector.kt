@file:Suppress("DEPRECATION") // PhoneStateListener is the only call-state API on API 29-30 (minSdk)

package com.dndbypass.urgentring.screening

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.dndbypass.urgentring.data.DebugLogger

/**
 * Reports a call that rang and then ended without ever being answered.
 *
 * Like CallStateWatcher, it ignores the stale state delivered at registration time (screening
 * runs before the call rings) and only acts after it has seen the call actually ringing. An
 * answered call (OFFHOOK after RINGING) cancels the watch; RINGING followed by IDLE with no
 * OFFHOOK in between is a miss. A call the user declines also ends this way, because Android
 * does not expose why a call ended to a screening-role app.
 */
class MissedCallDetector(private val context: Context) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val handler = Handler(Looper.getMainLooper())
    private var modernCallback: TelephonyCallback? = null
    private var legacyListener: PhoneStateListener? = null
    private var sawRinging = false
    private var finished = false

    /** Returns false if READ_PHONE_STATE is missing or registration failed. [onMissed] fires at most once. */
    fun watch(onMissed: () -> Unit): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            DebugLogger.log(context, "MissedCallDetector: READ_PHONE_STATE not granted — cannot detect missed calls")
            return false
        }
        val registered = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) registerModern(onMissed) else registerLegacy(onMissed)
        }.onFailure {
            DebugLogger.log(context, "MissedCallDetector registration FAILED: ${it.message}")
        }.isSuccess
        if (registered) handler.postDelayed({ finish() }, WATCH_TIMEOUT_MS)
        return registered
    }

    private fun handle(state: Int, onMissed: () -> Unit) {
        if (finished) return
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> sawRinging = true
            TelephonyManager.CALL_STATE_OFFHOOK -> if (sawRinging) {
                DebugLogger.log(context, "MissedCallDetector: call answered — no location share")
                finish()
            }
            TelephonyManager.CALL_STATE_IDLE -> if (sawRinging) {
                finish()
                onMissed()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerModern(onMissed: () -> Unit) {
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) = handle(state, onMissed)
        }
        modernCallback = callback
        telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
    }

    private fun registerLegacy(onMissed: () -> Unit) {
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) = handle(state, onMissed)
        }
        legacyListener = listener
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun finish() {
        if (finished) return
        finished = true
        handler.removeCallbacksAndMessages(null)
        runCatching {
            modernCallback?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) telephonyManager.unregisterTelephonyCallback(it)
            }
            legacyListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
        }
        modernCallback = null
        legacyListener = null
    }

    private companion object {
        // Safety net so a call that never rings (or never ends) can't leak the listener.
        const val WATCH_TIMEOUT_MS = 2 * 60_000L
    }
}
