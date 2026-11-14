package com.farmsaathi.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.farmsaathi.core.database.entity.LandBoundaryPointEntity
import com.farmsaathi.core.database.entity.LandMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LandMeasurementDao {
    @Transaction
    @Query("SELECT * FROM land_measurements WHERE plotId = :plotId ORDER BY completedAtEpochMillis DESC, id DESC")
    fun observeForPlot(plotId: Long): Flow<List<LandMeasurementWithPoints>>

    @Insert
    suspend fun insertMeasurement(entity: LandMeasurementEntity): Long

    @Insert
    suspend fun insertBoundaryPoints(points: List<LandBoundaryPointEntity>)

    @Query("SELECT farmId FROM plots WHERE id = :plotId LIMIT 1")
    suspend fun findFarmIdForPlot(plotId: Long): Long?

    @Query("UPDATE plots SET areaMicroAcres = :areaMicroAcres, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :plotId AND farmId = :farmId")
    suspend fun updatePlotArea(
        plotId: Long,
        areaMicroAcres: Long,
        updatedAtEpochMillis: Long,
        farmId: Long,
    ): Int

    @Transaction
    suspend fun insertMeasurementAndMaybeApplyArea(
        measurement: LandMeasurementEntity,
        points: List<LandBoundaryPointEntity>,
        applyAreaToPlot: Boolean,
    ): Long {
        val plotFarmId = findFarmIdForPlot(measurement.plotId)
        check(plotFarmId != null && plotFarmId == measurement.farmId) {
            "The selected plot does not belong to the selected farm"
        }
        val measurementId = insertMeasurement(measurement.copy(appliedToPlot = applyAreaToPlot))
        if (points.isNotEmpty()) {
            insertBoundaryPoints(points.map { it.copy(measurementId = measurementId) })
        }
        if (applyAreaToPlot) {
            val updated = updatePlotArea(
                plotId = measurement.plotId,
                areaMicroAcres = measurement.areaMicroAcres,
                updatedAtEpochMillis = measurement.completedAtEpochMillis,
                farmId = measurement.farmId,
            )
            check(updated == 1) { "The selected plot no longer exists; measurement was not committed" }
        }
        return measurementId
    }
}

data class LandMeasurementWithPoints(
    @Embedded val measurement: LandMeasurementEntity,
    @Relation(parentColumn = "id", entityColumn = "measurementId")
    val points: List<LandBoundaryPointEntity>,
)
