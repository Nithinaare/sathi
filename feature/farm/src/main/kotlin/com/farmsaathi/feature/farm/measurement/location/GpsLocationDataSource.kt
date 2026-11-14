package com.farmsaathi.feature.farm.measurement.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline GPS-only source. It deliberately does not use NETWORK_PROVIDER and does not request
 * background-location permission. Collection is stopped when the screen is paused or closed.
 */
@Singleton
class AndroidGpsLocationDataSource
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : GpsLocationDataSource {
    @SuppressLint("MissingPermission")
    override fun locationUpdates(): Flow<GpsLocationSample> = callbackFlow {
        val fineGranted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted) {
            close(GpsLocationException.PermissionMissing())
            return@callbackFlow
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        if (!gpsEnabled) {
            close(GpsLocationException.ProviderDisabled())
            return@callbackFlow
        }
        if (!locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            close(GpsLocationException.ProviderUnavailable())
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!location.hasAccuracy() || !location.accuracy.isFinite()) return
                trySend(
                    GpsLocationSample(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = location.accuracy.toDouble(),
                        capturedAtEpochMillis = location.time,
                    ),
                )
            }

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) close(GpsLocationException.ProviderDisabled())
            }

            @Deprecated("Deprecated by Android; retained for API 29 compatibility")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MILLIS,
                LOCATION_MIN_DISTANCE_METERS,
                listener,
                Looper.getMainLooper(),
            )
        } catch (security: SecurityException) {
            close(GpsLocationException.PermissionMissing())
            return@callbackFlow
        } catch (exception: IllegalArgumentException) {
            close(GpsLocationException.ProviderUnavailable())
            return@callbackFlow
        }

        awaitClose { runCatching { locationManager.removeUpdates(listener) } }
    }

    private companion object {
        const val LOCATION_UPDATE_INTERVAL_MILLIS = 1_000L
        const val LOCATION_MIN_DISTANCE_METERS = 1f
    }
}
