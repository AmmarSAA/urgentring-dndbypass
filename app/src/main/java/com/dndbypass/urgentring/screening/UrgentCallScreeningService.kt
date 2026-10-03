package com.dndbypass.urgentring.screening

import android.app.NotificationManager
import android.telecom.Call
import android.telecom.CallScreeningService
import com.dndbypass.urgentring.data.AppDatabase
import com.dndbypass.urgentring.data.ContactLookup
import com.dndbypass.urgentring.data.DEFAULT_RULE_KEY
import com.dndbypass.urgentring.data.DebugLogger
import com.dndbypass.urgentring.data.PhoneNumberNormalizer
import com.dndbypass.urgentring.data.SettingsRepository
import com.dndbypass.urgentring.locationshare.LocationShareManager
import com.dndbypass.urgentring.permissions.PermissionsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Bound by the system Telecom framework for every incoming call once the user has
 * granted this app the ROLE_CALL_SCREENING role. onScreenCall() runs before the call
 * rings and must respond quickly via respondToCall().
 *
 * We never disallow or reject a call here — calls under the threshold stay silent
 * only because the device's own ringer/DND state is already silent; they still show
 * up as a normal (if quiet) missed call. Calls at/above the threshold get a loud
 * alert played over the top via RingerOverrideManager, without changing the device's
 * ringer mode or Do Not Disturb state.
 */
class UrgentCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var callCounter: CallCounter
    private lateinit var ringerOverride: RingerOverrideManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var fullScreenCallNotifier: FullScreenCallNotifier
    private lateinit var callStateWatcher: CallStateWatcher
    private lateinit var locationShareManager: LocationShareManager
    private var restoreJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(applicationContext)
        callCounter = CallCounter(db.callerRuleDao(), db.callEventDao())
        ringerOverride = RingerOverrideManager(applicationContext)
        settingsRepository = SettingsRepository(applicationContext)
        fullScreenCallNotifier = FullScreenCallNotifier(applicationContext)
        callStateWatcher = CallStateWatcher(applicationContext)
        locationShareManager = LocationShareManager(applicationContext)
        DebugLogger.log(applicationContext, "Service created (fresh process or first bind)")
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart
        val normalized = PhoneNumberNormalizer.normalize(rawNumber)
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        DebugLogger.log(
            applicationContext,
            "onScreenCall number=$rawNumber normalized=$normalized " +
                "callScreeningRole=${PermissionsHelper.isCallScreeningRoleHeld(applicationContext)} " +
                "batteryExempt=${PermissionsHelper.isIgnoringBatteryOptimizations(applicationContext)} " +
                "interruptionFilter=${notificationManager.currentInterruptionFilter} (informational only, not touched)"
        )

        serviceScope.launch {
            try {
                // Independent of the urgent-ring toggle below, and isolated so a failure here
                // can never affect the call or the urgent alert.
                runCatching { locationShareManager.watchIfEligible(normalized) }.onFailure {
                    DebugLogger.log(applicationContext, "LocationShare watch ERROR ${it.javaClass.simpleName}: ${it.message}")
                }

                val featureOn = settingsRepository.featureEnabled.first()
                DebugLogger.log(applicationContext, "featureEnabled=$featureOn")
                if (featureOn) {
                    val decision = callCounter.recordAndEvaluate(normalized, System.currentTimeMillis())
                    DebugLogger.log(
                        applicationContext,
                        "decision shouldRingAudibly=${decision.shouldRingAudibly} rule=${decision.rule}"
                    )
                    if (decision.shouldRingAudibly) {
                        val overrideSucceeded = ringerOverride.overrideForUrgentCall()
                        DebugLogger.log(applicationContext, "overrideSucceeded=$overrideSucceeded")
                        if (overrideSucceeded && decision.eventId != null) {
                            callCounter.markRingOverridden(decision.eventId)
                        }

                        // The default "all callers" rule's own label ("All callers")
                        // isn't the caller's identity — only show a saved name for a
                        // per-number rule; otherwise fall back to a contacts lookup,
                        // then finally just the number.
                        val callerDisplayName = decision.rule
                            ?.takeIf { it.numberOrDefault != DEFAULT_RULE_KEY }
                            ?.displayName
                            ?: ContactLookup.displayNameFor(applicationContext, rawNumber)
                        val fullScreenShown = fullScreenCallNotifier.show(callerDisplayName, rawNumber)
                        DebugLogger.log(applicationContext, "fullScreenCallShown=$fullScreenShown")

                        scheduleRestore()
                        // Whichever fires first — the call actually going idle, or the
                        // fixed timeout above as a safety net — stops the alert.
                        callStateWatcher.waitForIdle {
                            DebugLogger.log(applicationContext, "Call went idle — stopping alert early")
                            restoreJob?.cancel()
                            stopOverride()
                        }
                    }
                }
            } catch (e: Exception) {
                // Without this, an exception here would skip respondToCall() entirely
                // (see finally below) — logged so a real-device failure is visible.
                DebugLogger.log(applicationContext, "onScreenCall ERROR ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                // Always respond, even if something above threw — an unanswered
                // screening request can leave the call in limbo.
                respondToCall(callDetails, ALLOW_RESPONSE)
            }
        }
    }

    // Safety-net ceiling in case CallStateWatcher can't run (e.g. READ_PHONE_STATE not
    // granted) — normally callStateWatcher.waitForIdle() above stops things sooner, as
    // soon as the call is actually answered or ends.
    private fun scheduleRestore() {
        restoreJob = serviceScope.launch {
            delay(RING_OVERRIDE_DURATION_MS)
            DebugLogger.log(applicationContext, "Fixed timeout reached — stopping alert")
            stopOverride()
        }
    }

    private fun stopOverride() {
        ringerOverride.restore()
        fullScreenCallNotifier.dismiss()
        callStateWatcher.stop()
    }

    companion object {
        private const val RING_OVERRIDE_DURATION_MS = 45_000L

        private val ALLOW_RESPONSE = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
    }
}
