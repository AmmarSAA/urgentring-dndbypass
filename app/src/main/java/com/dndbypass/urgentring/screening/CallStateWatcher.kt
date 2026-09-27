@file:Suppress("DEPRECATION") // PhoneStateListener is the only call-state API on API 29-30 (minSdk)

package com.dndbypass.urgentring.screening

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.dndbypass.urgentring.data.DebugLogger

/**
 * Watches for the device's call state to return to idle (no active or ringing call)
 * so the ringer override can stop as soon as the call is answered or ends, instead of
 * always waiting out UrgentCallScreeningService's fixed timeout. If READ_PHONE_STATE
 * isn't granted, waitForIdle() simply does nothing and that fixed timeout remains the
 * only way the alert stops — a safety net, not a hard dependency on this class.
 *
 * Registering a call-state listener fires it immediately with whatever the CURRENT
 * state already is, not just on future changes. Screening runs before the call is
 * actually presented as ringing, so at registration time the state is still IDLE —
 * without guarding for this, the very first (stale, not-a-real-change) callback would
 * be misread as "the call just ended" and kill the alert within milliseconds of it
 * starting. We only act once we've first observed the call actually ringing.
 *
 * The alert should stop the instant the user answers (state goes OFFHOOK), not only
 * when the call fully ends (state goes IDLE) — otherwise it keeps blaring through the
 * whole conversation. So both transitions count as "stop", as long as they follow a
 * genuine RINGING state we saw ourselves (not the stale state at registration).
 */
class CallStateWatcher(private val context: Context) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var modernCallback: TelephonyCallback? = null
    private var legacyListener: PhoneStateListener? = null
    private var sawRingingState = false

    fun waitForIdle(onIdle: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            DebugLogger.log(context, "READ_PHONE_STATE not granted — relying on fixed timeout only")
            return
        }

        sawRingingState = false
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                registerModern(onIdle)
            } else {
                registerLegacy(onIdle)
            }
        }.onFailure {
            DebugLogger.log(context, "CallStateWatcher registration FAILED: ${it.message}")
        }
    }

    private fun handleStateChange(state: Int, onIdle: () -> Unit) {
        DebugLogger.log(context, "CallStateWatcher state=$state sawRingingState=$sawRingingState")
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            sawRingingState = true
            return
        }
        if (!sawRingingState) {
            // Stale state reported at registration time, before the call actually
            // started ringing — not a real answer/end-of-call event. Ignore it.
            return
        }
        // Either OFFHOOK (answered) or IDLE (ended/declined/missed) — both mean the
        // alert should stop now.
        stop()
        onIdle()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerModern(onIdle: () -> Unit) {
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) = handleStateChange(state, onIdle)
        }
        modernCallback = callback
        telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
    }

    @Suppress("DEPRECATION")
    private fun registerLegacy(onIdle: () -> Unit) {
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) = handleStateChange(state, onIdle)
        }
        legacyListener = listener
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    fun stop() {
        runCatching {
            modernCallback?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) telephonyManager.unregisterTelephonyCallback(it)
            }
            @Suppress("DEPRECATION")
            legacyListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
        }
        modernCallback = null
        legacyListener = null
    }
}
