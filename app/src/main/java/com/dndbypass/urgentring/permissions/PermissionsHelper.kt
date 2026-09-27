package com.dndbypass.urgentring.permissions

import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object PermissionsHelper {

    fun isCallScreeningRoleHeld(context: Context): Boolean {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return false
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    fun requestCallScreeningRoleIntent(context: Context): Intent {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun ignoreBatteryOptimizationsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    // Android 14+ requires explicit user grant for a non-calling-role app to launch a
    // full-screen intent; before that it was allowed by default with the manifest
    // permission alone.
    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.canUseFullScreenIntent()
    }

    fun fullScreenIntentSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    // Some OEM skins (MIUI in particular) gate launching an Activity from a background
    // context — like our CallScreeningService — behind their own extra permission
    // ("Display pop-up windows while running in the background", or similar) that has
    // no standard Android API to check or request. The app's own settings page is the
    // most reliable place to send the user looking for it, since the exact menu path
    // and wording varies by OEM and OS version.
    fun appInfoIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    // Undocumented but widely-used deep link straight to MIUI's own per-app permission
    // editor (where "Display pop-up windows while running in the background" lives).
    // Not guaranteed to exist on every MIUI version/region, so callers should catch
    // ActivityNotFoundException and fall back to appInfoIntent().
    fun miuiPermissionEditorIntent(context: Context): Intent =
        Intent().apply {
            component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            putExtra("extra_pkgname", context.packageName)
        }
}
