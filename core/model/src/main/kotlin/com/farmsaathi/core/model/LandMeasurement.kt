package com.farmsaathi.core.model

/** Method used to obtain a plot-area estimate. */
enum class LandMeasurementMethod {
    GPS_BOUNDARY,
    MANUAL_RECTANGLE,
}

/** A single accepted point from a GPS boundary walk. */
data class LandBoundaryPoint(
    val sequenceNumber: Int,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Double?,
    val capturedAtEpochMillis: Long,
)

/** A saved measurement. The original boundary points are retained for review and recalculation. */
data class LandMeasurement(
    val id: Long = 0L,
    val farmId: Long,
    val plotId: Long,
    val method: LandMeasurementMethod,
    val areaMicroAcres: Long,
    val perimeterMeters: Double?,
    val boundaryPoints: List<LandBoundaryPoint>,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val appliedToPlot: Boolean = false,
    val notes: String? = null,
) {
    /** Derived display value; areaMicroAcres is the single persisted source of truth. */
    val areaSquareMeters: Double
        get() = areaMicroAcres.toDouble() / AreaConverter.MICRO_ACRES_PER_ACRE.toDouble() * SQUARE_METERS_PER_ACRE

    val averageGpsAccuracyMeters: Double?
        get() = boundaryPoints.mapNotNull { it.horizontalAccuracyMeters }.takeIf { it.isNotEmpty() }?.average()

    val maximumGpsAccuracyMeters: Double?
        get() = boundaryPoints.mapNotNull { it.horizontalAccuracyMeters }.maxOrNull()

    private companion object {
        const val SQUARE_METERS_PER_ACRE = 4_046.8564224
    }
}
