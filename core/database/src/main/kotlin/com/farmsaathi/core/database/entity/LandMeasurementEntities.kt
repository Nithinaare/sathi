package com.farmsaathi.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "land_measurements",
    foreignKeys = [
        ForeignKey(
            entity = FarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["farmId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = PlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["plotId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("farmId"), Index("plotId"), Index("completedAtEpochMillis")],
)
data class LandMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val plotId: Long,
    val method: String,
    val areaMicroAcres: Long,
    val perimeterMeters: Double?,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val appliedToPlot: Boolean,
    val notes: String?,
)

@Entity(
    tableName = "land_boundary_points",
    foreignKeys = [
        ForeignKey(
            entity = LandMeasurementEntity::class,
            parentColumns = ["id"],
            childColumns = ["measurementId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["measurementId", "sequenceNumber"], unique = true)],
)
data class LandBoundaryPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val measurementId: Long,
    val sequenceNumber: Int,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Double?,
    val capturedAtEpochMillis: Long,
)
