package com.farmsaathi.core.common.repository

import com.farmsaathi.core.model.LandMeasurement
import kotlinx.coroutines.flow.Flow

/** Persists completed plot measurements and, only when requested, applies the area to the plot. */
interface LandMeasurementRepository {
    fun observeMeasurements(plotId: Long): Flow<List<LandMeasurement>>

    /**
     * Saves the measurement and its boundary points in one transaction.
     * When [applyAreaToPlot] is true, the plot area is updated atomically with the history record.
     */
    suspend fun saveMeasurement(
        measurement: LandMeasurement,
        applyAreaToPlot: Boolean,
    ): Long
}
