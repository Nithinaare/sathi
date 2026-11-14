package com.farmsaathi.core.database.repository

import com.farmsaathi.core.common.repository.LandMeasurementRepository
import com.farmsaathi.core.database.DatabaseMaintenanceCoordinator
import com.farmsaathi.core.database.dao.LandMeasurementDao
import com.farmsaathi.core.database.dao.LandMeasurementWithPoints
import com.farmsaathi.core.database.entity.LandBoundaryPointEntity
import com.farmsaathi.core.database.entity.LandMeasurementEntity
import com.farmsaathi.core.model.LandBoundaryPoint
import com.farmsaathi.core.model.LandMeasurement
import com.farmsaathi.core.model.LandMeasurementMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomLandMeasurementRepository
@Inject
constructor(
    private val dao: LandMeasurementDao,
    private val maintenanceCoordinator: DatabaseMaintenanceCoordinator,
) : LandMeasurementRepository {
    override fun observeMeasurements(plotId: Long): Flow<List<LandMeasurement>> =
        dao.observeForPlot(plotId).map { rows -> rows.map { row -> row.toDomain() } }

    override suspend fun saveMeasurement(
        measurement: LandMeasurement,
        applyAreaToPlot: Boolean,
    ): Long = maintenanceCoordinator.withExclusiveLock {
        require(measurement.farmId > 0L) { "A measurement must belong to a saved farm" }
        require(measurement.plotId > 0L) { "A measurement must belong to a saved plot" }
        require(measurement.areaSquareMeters.isFinite() && measurement.areaSquareMeters > 0.0) {
            "The calculated area must be finite and greater than zero"
        }
        require(measurement.areaMicroAcres > 0L) { "The converted area must be greater than zero" }
        require(measurement.perimeterMeters?.let { it.isFinite() && it > 0.0 } == true) {
            "The calculated perimeter must be finite and greater than zero"
        }
        require(measurement.completedAtEpochMillis >= measurement.startedAtEpochMillis) {
            "Completion time cannot be earlier than the start time"
        }
        require(measurement.boundaryPoints.size <= MAX_BOUNDARY_POINTS) { "Too many boundary points" }
        if (measurement.method == LandMeasurementMethod.GPS_BOUNDARY) {
            require(measurement.boundaryPoints.size >= MINIMUM_GPS_POINTS) { "GPS measurements require at least three boundary points" }
            require(
                measurement.boundaryPoints.all { point ->
                    val accuracy = point.horizontalAccuracyMeters
                    point.latitude.isFinite() && point.latitude in -90.0..90.0 &&
                        point.longitude.isFinite() && point.longitude in -180.0..180.0 &&
                        accuracy != null && accuracy.isFinite() && accuracy > 0.0
                },
            ) { "The boundary contains an invalid point" }
            require(measurement.boundaryPoints.all { it.sequenceNumber >= 0 }) {
                "Boundary point sequence numbers cannot be negative"
            }
            require(
                measurement.boundaryPoints.zipWithNext().all { (first, second) ->
                    second.sequenceNumber > first.sequenceNumber
                },
            ) { "Boundary point sequence numbers must be strictly increasing" }
            require(
                measurement.boundaryPoints.zipWithNext().all { (first, second) ->
                    second.capturedAtEpochMillis > first.capturedAtEpochMillis
                },
            ) { "Boundary point timestamps must be strictly increasing" }
        } else {
            require(measurement.boundaryPoints.isEmpty()) { "Manual measurements must not contain GPS boundary points" }
        }

        val entity =
            LandMeasurementEntity(
                farmId = measurement.farmId,
                plotId = measurement.plotId,
                method = measurement.method.name,
                areaMicroAcres = measurement.areaMicroAcres,
                perimeterMeters = measurement.perimeterMeters,
                startedAtEpochMillis = measurement.startedAtEpochMillis,
                completedAtEpochMillis = measurement.completedAtEpochMillis,
                appliedToPlot = applyAreaToPlot,
                notes = measurement.notes,
            )
        val points =
            measurement.boundaryPoints.map { point ->
                LandBoundaryPointEntity(
                    measurementId = 0L,
                    sequenceNumber = point.sequenceNumber,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    horizontalAccuracyMeters = point.horizontalAccuracyMeters,
                    capturedAtEpochMillis = point.capturedAtEpochMillis,
                )
            }
        dao.insertMeasurementAndMaybeApplyArea(entity, points, applyAreaToPlot)
    }

    private fun LandMeasurementWithPoints.toDomain(): LandMeasurement =
        LandMeasurement(
            id = measurement.id,
            farmId = measurement.farmId,
            plotId = measurement.plotId,
            method = LandMeasurementMethod.valueOf(measurement.method),
            areaMicroAcres = measurement.areaMicroAcres,
            perimeterMeters = measurement.perimeterMeters,
            boundaryPoints = points.sortedBy { it.sequenceNumber }.map { point ->
                LandBoundaryPoint(
                    sequenceNumber = point.sequenceNumber,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    horizontalAccuracyMeters = point.horizontalAccuracyMeters,
                    capturedAtEpochMillis = point.capturedAtEpochMillis,
                )
            },
            startedAtEpochMillis = measurement.startedAtEpochMillis,
            completedAtEpochMillis = measurement.completedAtEpochMillis,
            appliedToPlot = measurement.appliedToPlot,
            notes = measurement.notes,
        )

    private companion object {
        const val MINIMUM_GPS_POINTS = 3
        const val MAX_BOUNDARY_POINTS = 5_000
    }
}
