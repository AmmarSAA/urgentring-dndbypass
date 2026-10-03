@file:Suppress("DEPRECATION") // requestSingleUpdate is the only one-shot API on API 29 (minSdk)

package com.dndbypass.urgentring.locationshare

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/** One-shot location lookup using only platform APIs (no Google Play services dependency). */
object LocationFetcher {
    private const val PER_PROVIDER_TIMEOUT_MS = 8_000L
    private const val MAX_LAST_KNOWN_AGE_MS = 10 * 60_000L

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Returns the first fresh fix from any enabled provider, else the freshest recent last-known
     * fix. Returns null if there is no permission or no usable fix.
     */
    suspend fun currentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        // Ask every provider at once and take whichever answers first, so a provider that never
        // responds (no network, no GPS lock) doesn't hold up one that already has a fix.
        val freshFix = coroutineScope {
            val first = CompletableDeferred<Location?>()
            val workers = providers.map { provider ->
                launch {
                    val fix = runCatching {
                        withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) { requestFix(manager, provider) }
                    }.getOrNull()
                    if (fix != null) first.complete(fix)
                }
            }
            launch {
                workers.joinAll()
                first.complete(null)
            }
            first.await().also { coroutineContext.cancelChildren() }
        }
        if (freshFix != null) return freshFix

        return providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time <= MAX_LAST_KNOWN_AGE_MS }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFix(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(provider, signal, Executor { it.run() }) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (continuation.isActive) continuation.resume(location)
                    }

                    // Abstract on API 29, so they must be implemented even though they are no-ops.
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
        }
}
