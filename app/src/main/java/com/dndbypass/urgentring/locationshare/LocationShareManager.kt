package com.dndbypass.urgentring.locationshare

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dndbypass.urgentring.R
import com.dndbypass.urgentring.data.AppDatabase
import com.dndbypass.urgentring.data.DebugLogger
import com.dndbypass.urgentring.data.LocationShareContact
import com.dndbypass.urgentring.data.SettingsRepository
import com.dndbypass.urgentring.screening.MissedCallDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Optional feature: when the user misses a call from someone on their location-sharing list,
 * send that person the user's current position as a Google Maps link.
 *
 * SMS is sent automatically. WhatsApp has no API for sending on the user's behalf, so for it we
 * post a notification whose tap opens WhatsApp with the message pre-filled.
 *
 * Everything is gated on the master switch in the Location sharing screen (off by default) and
 * on the caller being explicitly selected there. A per-caller cooldown stops repeated missed
 * calls from producing a stream of messages.
 */
class LocationShareManager(private val context: Context) {
    private val database = AppDatabase.get(context)
    private val settings = SettingsRepository(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        notificationManager.createNotificationChannel(
            NotificationChannel(STATUS_CHANNEL, "Location sharing status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Confirms when your location was sent after a missed call"
            }
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(PROMPT_CHANNEL, "Send location on WhatsApp", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tap to send your location on WhatsApp after a missed call"
            }
        )
    }

    /** Called when a call starts screening. Starts watching it only if this feature applies to the caller. */
    suspend fun watchIfEligible(normalizedNumber: String?) {
        if (normalizedNumber == null) return
        if (!settings.locationShareEnabled.first()) return

        val contact = database.locationShareContactDao().find(normalizedNumber) ?: return
        val sinceLast = System.currentTimeMillis() - contact.lastSentMillis
        if (sinceLast < COOLDOWN_MS) {
            DebugLogger.log(context, "LocationShare: $normalizedNumber on cooldown (${sinceLast / 1000}s since last send)")
            return
        }
        val sms = settings.locationShareSms.first()
        val whatsApp = settings.locationShareWhatsApp.first()
        if (!sms && !whatsApp) return

        val watching = MissedCallDetector(context).watch { scope.launch { share(contact, sms, whatsApp) } }
        DebugLogger.log(context, "LocationShare: watching call from $normalizedNumber for a miss, watching=$watching")
    }

    private suspend fun share(contact: LocationShareContact, sms: Boolean, whatsApp: Boolean) {
        try {
            // The user may have switched this off while the phone was ringing.
            if (!settings.locationShareEnabled.first()) return
            DebugLogger.log(context, "LocationShare: missed call from ${contact.normalizedNumber}, fetching location")

            val location = LocationFetcher.currentLocation(context)
            if (location == null) {
                DebugLogger.log(context, "LocationShare: no location available (permission or no fix)")
                notifyStatus(contact, "Couldn't get your location to send to ${label(contact)}")
                return
            }

            val message = buildMessage(location)
            var smsSent = false
            if (sms) smsSent = sendSms(contact.normalizedNumber, message)
            if (whatsApp) postWhatsAppPrompt(contact, message)

            if (smsSent || whatsApp) {
                database.locationShareContactDao().markSent(contact.normalizedNumber, System.currentTimeMillis())
            }
            when {
                smsSent && whatsApp -> notifyStatus(contact, "Location texted to ${label(contact)}; WhatsApp prompt ready")
                smsSent -> notifyStatus(contact, "Location texted to ${label(contact)}")
                !whatsApp -> notifyStatus(contact, "Couldn't text your location to ${label(contact)} (SMS permission?)")
            }
        } catch (e: Exception) {
            DebugLogger.log(context, "LocationShare ERROR ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun sendSms(number: String, message: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            DebugLogger.log(context, "LocationShare: SEND_SMS not granted — SMS skipped")
            return false
        }
        return runCatching {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendMultipartTextMessage(number, null, smsManager.divideMessage(message), null, null)
        }.onSuccess {
            DebugLogger.log(context, "LocationShare: SMS handed to the system for $number")
        }.onFailure {
            DebugLogger.log(context, "LocationShare: SMS FAILED ${it.javaClass.simpleName}: ${it.message}")
        }.isSuccess
    }

    private fun postWhatsAppPrompt(contact: LocationShareContact, message: String) {
        val digits = contact.normalizedNumber.filter { it.isDigit() }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId(contact),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, PROMPT_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("You missed ${label(contact)}")
            .setContentText("Tap to send your location on WhatsApp")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Send on WhatsApp", pendingIntent)
            .build()
        notificationManager.notify(notificationId(contact), notification)
        DebugLogger.log(context, "LocationShare: WhatsApp prompt posted for ${contact.normalizedNumber}")
    }

    private fun notifyStatus(contact: LocationShareContact, text: String) {
        val notification = NotificationCompat.Builder(context, STATUS_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Urgent Ring")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId(contact) + 1, notification)
    }

    private fun buildMessage(location: Location): String {
        val link = String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", location.latitude, location.longitude)
        val accuracy = if (location.hasAccuracy()) " (accuracy about ${location.accuracy.toInt()} m)" else ""
        val ageMinutes = (System.currentTimeMillis() - location.time) / 60_000
        val age = if (ageMinutes >= 2) " as of $ageMinutes min ago" else ""
        return "Sorry I missed your call. My location$age: $link$accuracy. Sent automatically by Urgent Ring."
    }

    private fun label(contact: LocationShareContact) = contact.displayName ?: contact.normalizedNumber

    private fun notificationId(contact: LocationShareContact) = 2000 + (contact.normalizedNumber.hashCode() and 0x3FFF) * 2

    private companion object {
        const val COOLDOWN_MS = 15 * 60_000L
        const val STATUS_CHANNEL = "location_share_status"
        const val PROMPT_CHANNEL = "location_share_prompt"
    }
}
