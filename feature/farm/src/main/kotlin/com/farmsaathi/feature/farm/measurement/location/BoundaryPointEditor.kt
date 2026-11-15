package com.farmsaathi.feature.farm.measurement.location

/** Explicit result for a farmer-reviewed boundary edit. */
sealed interface BoundaryPointEditResult {
    data class Updated(
        val points: List<GpsLocationSample>,
        val selectedIndex: Int,
    ) : BoundaryPointEditResult

    data class Rejected(val reason: BoundaryPointEditError) : BoundaryPointEditResult
}

enum class BoundaryPointEditError {
    INVALID_INDEX,
    INVALID_COORDINATE,
    TOO_FEW_POINTS_TO_REMOVE,
    TOO_MANY_POINTS,
}

/**
 * Pure boundary editing operations. A manually moved/inserted point has null GPS accuracy:
 * the app must not pretend that a user-placed map coordinate came from a GPS fix.
 */
object BoundaryPointEditor {
    fun move(
        points: List<GpsLocationSample>,
        index: Int,
        latitude: Double,
        longitude: Double,
        nowEpochMillis: Long,
    ): BoundaryPointEditResult {
        if (index !in points.indices) return BoundaryPointEditResult.Rejected(BoundaryPointEditError.INVALID_INDEX)
        if (!isValidCoordinate(latitude, longitude)) return BoundaryPointEditResult.Rejected(BoundaryPointEditError.INVALID_COORDINATE)
        val updated = points.toMutableList()
        updated[index] = GpsLocationSample(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = null,
            capturedAtEpochMillis = nowEpochMillis,
        )
        return BoundaryPointEditResult.Updated(updated, index)
    }

    fun insertAfter(
        points: List<GpsLocationSample>,
        index: Int,
        latitude: Double,
        longitude: Double,
        nowEpochMillis: Long,
    ): BoundaryPointEditResult {
        if (index !in points.indices) return BoundaryPointEditResult.Rejected(BoundaryPointEditError.INVALID_INDEX)
        if (!isValidCoordinate(latitude, longitude)) return BoundaryPointEditResult.Rejected(BoundaryPointEditError.INVALID_COORDINATE)
        if (points.size >= MAXIMUM_BOUNDARY_POINTS) return BoundaryPointEditResult.Rejected(BoundaryPointEditError.TOO_MANY_POINTS)
        val updated = points.toMutableList()
        val insertIndex = index + 1
        updated.add(
            insertIndex,
            GpsLocationSample(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = null,
                capturedAtEpochMillis = nowEpochMillis,
            ),
        )
        return BoundaryPointEditResult.Updated(updated, insertIndex)
    }

    fun remove(
        points: List<GpsLocationSample>,
        index: Int,
    ): BoundaryPointEditResult {
        if (index !in points.indices) return BoundaryPointEditResult.Rejected(BoundaryPointEditError.INVALID_INDEX)
        if (points.size <= MINIMUM_BOUNDARY_POINTS) {
            return BoundaryPointEditResult.Rejected(BoundaryPointEditError.TOO_FEW_POINTS_TO_REMOVE)
        }
        val updated = points.toMutableList().apply { removeAt(index) }
        return BoundaryPointEditResult.Updated(updated, (index - 1).coerceAtLeast(0))
    }

    private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

    private const val MAXIMUM_BOUNDARY_POINTS = 5_000
    private const val MINIMUM_BOUNDARY_POINTS = 3
}
