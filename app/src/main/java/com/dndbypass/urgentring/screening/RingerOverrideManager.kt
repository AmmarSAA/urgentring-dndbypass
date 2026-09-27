package com.dndbypass.urgentring.screening

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import com.dndbypass.urgentring.data.DebugLogger

/**
 * Plays a loud alert for an urgent call WITHOUT changing the phone's ringer mode or
 * Do Not Disturb state — the Silent/DND toggle stays exactly as the user left it, the
 * same way Google's "Find My Device" ring works.
 *
 * The first version of this class called AudioManager.setRingerMode() and
 * NotificationManager.setInterruptionFilter() to force RINGER_MODE_NORMAL and lift
 * DND system-wide. That worked (confirmed on-device) but visibly took the whole phone
 * out of Silent/DND for the override window, not just this one call — not what was
 * wanted. This version instead plays on the ALARM stream via
 * AudioAttributes.USAGE_ALARM, which the OS exempts from ringer-mode muting (alarms
 * still sound in Silent mode) and, for the common DND levels, from Do Not Disturb
 * filtering too — without touching either setting.
 *
 * Known limit: Android's strictest DND level ("Total Silence" /
 * ZEN_MODE_NO_INTERRUPTIONS) mutes the alarm stream as well, by design, with no
 * exception mechanism available to any app. That one DND level can't be bypassed this
 * way — see the platform-feasibility notes from earlier in this project.
 */
class RingerOverrideManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null

    @Volatile private var savedAlarmVolume: Int? = null

    fun overrideForUrgentCall(): Boolean {
        if (mediaPlayer != null) return true // already playing for a call in flight

        return runCatching {
            savedAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getValidRingtoneUri(context)
                ?: error("No ringtone URI available")

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, soundUri)
                isLooping = true
                prepare()
                start()
            }
        }.onSuccess {
            DebugLogger.log(
                context,
                "Alarm-stream override playing (ringer mode / DND untouched): " +
                    "alarmVolume=${audioManager.getStreamVolume(AudioManager.STREAM_ALARM)}"
            )
        }.onFailure {
            DebugLogger.log(context, "Alarm-stream override FAILED (${it.javaClass.simpleName}): ${it.message}")
        }.isSuccess
    }

    fun restore() {
        runCatching {
            mediaPlayer?.apply {
                stop()
                release()
            }
            mediaPlayer = null
            savedAlarmVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) }
            savedAlarmVolume = null
        }.onSuccess {
            DebugLogger.log(context, "Alarm-stream override stopped, alarm volume restored")
        }.onFailure {
            DebugLogger.log(context, "Alarm-stream restore FAILED: ${it.message}")
        }
    }
}
