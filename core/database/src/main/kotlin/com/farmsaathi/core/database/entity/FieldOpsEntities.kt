package com.farmsaathi.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.farmsaathi.core.model.ContactType
import com.farmsaathi.core.model.IrrigationMethod
import com.farmsaathi.core.model.IrrigationSource
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.SoilHealthRating
import com.farmsaathi.core.model.StorageStatus

@Entity(
    tableName = "irrigation_logs",
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
        ForeignKey(
            entity = CropCycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cropCycleId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["plotId"]),
        Index(value = ["cropCycleId"]),
        Index(value = ["irrigationDateEpochMillis"]),
    ],
)
data class IrrigationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val plotId: Long,
    val cropCycleId: Long? = null,
    val irrigationDateEpochMillis: Long,
    val durationMinutes: Int? = null,
    val source: IrrigationSource,
    val method: IrrigationMethod = IrrigationMethod.FLOOD,
    val nextWateringEpochDay: Long? = null,
    val motorHoursOrFuelNote: String? = null,
    val photoPath: String? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "harvest_records",
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
        ForeignKey(
            entity = CropCycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cropCycleId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["plotId"]),
        Index(value = ["cropCycleId"]),
        Index(value = ["harvestDateEpochDay"]),
    ],
)
data class HarvestRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val plotId: Long,
    val cropCycleId: Long,
    val harvestDateEpochDay: Long,
    val quantityMilliUnits: Long,
    val quantityUnit: QuantityUnit,
    val bagCount: Int? = null,
    val moisturePercentTenths: Int? = null,
    val qualityGrade: String? = null,
    val notes: String? = null,
    val photoPath: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "storage_records",
    foreignKeys = [
        ForeignKey(
            entity = FarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["farmId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CropCycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cropCycleId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = HarvestRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["harvestRecordId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["cropCycleId"]),
        Index(value = ["harvestRecordId"]),
        Index(value = ["status", "isArchived"]),
    ],
)
data class StorageRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val cropCycleId: Long,
    val harvestRecordId: Long? = null,
    val storageLocation: String,
    val quantityMilliUnits: Long,
    val remainingQuantityMilliUnits: Long,
    val quantityUnit: QuantityUnit,
    val storedDateEpochDay: Long,
    val bagCount: Int? = null,
    val status: StorageStatus = StorageStatus.IN_STORAGE,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "soil_test_records",
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
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["plotId"]),
        Index(value = ["testDateEpochDay"]),
    ],
)
data class SoilTestRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val plotId: Long,
    val testDateEpochDay: Long,
    val labName: String? = null,
    val sampleNumber: String? = null,
    val phTenths: Int? = null,
    val ecMilliSiemensPerCmHundredths: Int? = null,
    val organicCarbonPercentHundredths: Int? = null,
    val nitrogenKgPerHaTenths: Int? = null,
    val phosphorusKgPerHaTenths: Int? = null,
    val potassiumKgPerHaTenths: Int? = null,
    val overallRating: SoilHealthRating? = null,
    val reportPhotoPath: String? = null,
    val recommendationsNote: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["isArchived", "contactType", "name"]),
        Index(value = ["phone"]),
    ],
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String? = null,
    val contactType: ContactType,
    val villageOrMandi: String? = null,
    val upiId: String? = null,
    val voiceNamePath: String? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
