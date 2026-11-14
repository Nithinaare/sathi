package com.farmsaathi.feature.farm.measurement.location

import kotlinx.coroutines.flow.Flow

/** Location fix passed to the measurement domain. */
data class GpsLocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val capturedAtEpochMillis: Long,
)

interface GpsLocationDataSource {
    fun locationUpdates(): Flow<GpsLocationSample>
}

sealed class GpsLocationException(message: String) : Exception(message) {
    class PermissionMissing : GpsLocationException("Precise location permission is required")
    class ProviderDisabled : GpsLocationException("GPS is turned off")
    class ProviderUnavailable : GpsLocationException("GPS provider is not available")
}
