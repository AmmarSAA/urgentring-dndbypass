package com.dndbypass.urgentring.screening

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dndbypass.urgentring.R
import com.dndbypass.urgentring.data.DebugLogger
import com.dndbypass.urgentring.permissions.PermissionsHelper

/**
 * Shows a full-screen, incoming-call-style alert over the lock screen for a
 * threshold-crossing call, via Notification.setFullScreenIntent() launching
 * IncomingUrgentCallActivity — the same mechanism real calling apps use.
 *
 * Some OEM skins (MIUI confirmed) silently swallow third-party full-screen notification
 * intents at the SystemUI level regardless of channel importance or permission grants —
 * dumpsys/logcat show the notification posted successfully but no activity launch is
 * ever attempted by the system. As a fallback, we also try starting the activity
 * directly from this (bound, telecom-triggered) context; on stock Android this may be
 * blocked by background-activity-launch restrictions, which is fine since the
 * notification path already covers that case there.
 *
 * This is deliberately independent of RingerOverrideManager's alarm-stream sound: if
 * neither path can show the call screen (permission not granted, OEM restriction, etc.)
 * the audible alert still fires on its own, so the visual UI here is a bonus, not a
 * single point of failure for the feature actually working.
 */
class FullScreenCallNotifier(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val channel = NotificationChannel(CHANNEL_ID, "Urgent incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Full-screen alert shown when a repeat caller crosses your threshold"
        }
        notificationManager.createNotificationChannel(channel)
    }

    /** Returns whether the full-screen alert was shown via either the direct-launch or notification path. */
    fun show(callerDisplayName: String?, callerNumber: String?): Boolean {
        val activityIntent = Intent(context, IncomingUrgentCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(IncomingUrgentCallActivity.EXTRA_NAME, callerDisplayName)
            putExtra(IncomingUrgentCallActivity.EXTRA_NUMBER, callerNumber)
        }

        val directLaunchWorked = runCatching {
            context.startActivity(activityIntent)
        }.onFailure {
            DebugLogger.log(context, "Direct startActivity for call UI FAILED (${it.javaClass.simpleName}): ${it.message}")
        }.isSuccess
        DebugLogger.log(context, "Direct startActivity for call UI worked=$directLaunchWorked")

        if (!PermissionsHelper.canUseFullScreenIntent(context)) {
            DebugLogger.log(context, "Full-screen intent not permitted — skipping notification path")
            return directLaunchWorked
        }

        val notifyWorked = runCatching {
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Urgent call")
                .setContentText(callerDisplayName ?: callerNumber ?: "Unknown caller")
                .setSmallIcon(R.drawable.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setOngoing(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }.onSuccess {
            DebugLogger.log(context, "Full-screen incoming-call notification posted")
        }.onFailure {
            DebugLogger.log(context, "Full-screen incoming-call notification FAILED: ${it.message}")
        }.isSuccess

        return directLaunchWorked || notifyWorked
    }

    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        private const val CHANNEL_ID = "urgent_incoming_call"
        private const val NOTIFICATION_ID = 1001
    }
}
