package com.farmsaathi.feature.farm.measurement.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Configurable field-walk filter. Rejected fixes are not added to the boundary. */
data class GpsFilterConfig(
    val maximumAccuracyMeters: Double = 20.0,
    val minimumPointSpacingMeters: Double = 2.0,
    val maximumWalkingSpeedMetersPerSecond: Double = 8.0,
    val maximumFixAgeMillis: Long = 20_000L,
    val maximumFutureClockSkewMillis: Long = 5_000L,
)

enum class GpsFixRejection {
    INVALID_COORDINATE,
    MISSING_OR_POOR_ACCURACY,
    STALE_FIX,
    OUT_OF_ORDER_FIX,
    TOO_CLOSE_TO_PREVIOUS_POINT,
    UNLIKELY_MOVEMENT,
}

sealed interface GpsFixDecision {
    data object Accept : GpsFixDecision
    data class Reject(val reason: GpsFixRejection) : GpsFixDecision
}

class GpsPointAcceptancePolicy(
    private val config: GpsFilterConfig = GpsFilterConfig(),
) {
    fun evaluate(
        candidate: GpsLocationSample,
        previousAccepted: GpsLocationSample?,
        nowEpochMillis: Long,
    ): GpsFixDecision {
        if (!candidate.latitude.isFinite() || !candidate.longitude.isFinite() ||
            candidate.latitude !in -90.0..90.0 || candidate.longitude !in -180.0..180.0
        ) return GpsFixDecision.Reject(GpsFixRejection.INVALID_COORDINATE)

        val accuracyMeters = candidate.accuracyMeters
        if (accuracyMeters == null || !accuracyMeters.isFinite() || accuracyMeters <= 0.0 ||
            accuracyMeters > config.maximumAccuracyMeters
        ) return GpsFixDecision.Reject(GpsFixRejection.MISSING_OR_POOR_ACCURACY)

        val age = nowEpochMillis - candidate.capturedAtEpochMillis
        if (age > config.maximumFixAgeMillis || age < -config.maximumFutureClockSkewMillis) {
            return GpsFixDecision.Reject(GpsFixRejection.STALE_FIX)
        }

        if (previousAccepted != null) {
            val elapsedMillis = candidate.capturedAtEpochMillis - previousAccepted.capturedAtEpochMillis
            if (elapsedMillis <= 0L) return GpsFixDecision.Reject(GpsFixRejection.OUT_OF_ORDER_FIX)
            val distance = distanceMeters(previousAccepted, candidate)
            if (distance < config.minimumPointSpacingMeters) {
                return GpsFixDecision.Reject(GpsFixRejection.TOO_CLOSE_TO_PREVIOUS_POINT)
            }
            val speed = distance / (elapsedMillis / 1_000.0)
            if (!speed.isFinite() || speed > config.maximumWalkingSpeedMetersPerSecond) {
                return GpsFixDecision.Reject(GpsFixRejection.UNLIKELY_MOVEMENT)
            }
        }
        return GpsFixDecision.Accept
    }

    private fun distanceMeters(first: GpsLocationSample, second: GpsLocationSample): Double {
        val lat1 = Math.toRadians(first.latitude)
        val lat2 = Math.toRadians(second.latitude)
        val deltaLat = lat2 - lat1
        var deltaLon = Math.toRadians(second.longitude - first.longitude)
        if (deltaLon > Math.PI) deltaLon -= 2.0 * Math.PI
        if (deltaLon < -Math.PI) deltaLon += 2.0 * Math.PI
        val a = sin(deltaLat / 2.0).pow(2.0) + cos(lat1) * cos(lat2) * sin(deltaLon / 2.0).pow(2.0)
        return 2.0 * EARTH_MEAN_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    private companion object {
        const val EARTH_MEAN_RADIUS_METERS = 6_371_008.8
    }
}
