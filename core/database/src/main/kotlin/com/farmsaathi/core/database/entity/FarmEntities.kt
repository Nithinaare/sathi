package com.farmsaathi.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropStageTemplate
import com.farmsaathi.core.model.DiaryCategory
import com.farmsaathi.core.model.IrrigationSource
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.SeasonType
import com.farmsaathi.core.model.TaskCategory
import com.farmsaathi.core.model.TaskPriority
import com.farmsaathi.core.model.TaskStatus

@Entity(
    tableName = "farms",
    indices = [
        Index(value = ["isArchived"]),
        Index(value = ["name"]),
    ],
)
data class FarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val villageOrLocation: String? = null,
    val surveyNumber: String? = null,
    val totalAreaMicroAcres: Long = 0L,
    val preferredAreaUnit: AreaUnit = AreaUnit.ACRE_GUNTA,
    val soilType: String? = null,
    val waterSource: IrrigationSource? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPath: String? = null,
    val voiceNamePath: String? = null,
    val notes: String? = null,
    val isPrimary: Boolean = false,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "plots",
    foreignKeys = [
        ForeignKey(
            entity = FarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["farmId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["farmId", "isArchived"]),
    ],
)
data class PlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val name: String,
    val areaMicroAcres: Long,
    val preferredAreaUnit: AreaUnit = AreaUnit.ACRE_GUNTA,
    val soilType: String? = null,
    val irrigationSource: IrrigationSource? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPaths: List<String> = emptyList(),
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "crop_templates",
    indices = [
        Index(value = ["templateKey"], unique = true),
        Index(value = ["isArchived"]),
    ],
)
data class CropTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val templateKey: String,
    val cropNameTe: String,
    val cropNameEn: String,
    val defaultSeason: SeasonType,
    val expectedDurationDays: Int,
    val defaultYieldUnit: QuantityUnit,
    val stages: List<CropStageTemplate>,
    val isStarterTemplate: Boolean = false,
    val isEditable: Boolean = true,
    val disclaimerTe: String,
    val disclaimerEn: String,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "crop_cycles",
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
            entity = CropTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["plotId"]),
        Index(value = ["templateId"]),
        Index(value = ["status", "isArchived"]),
        Index(value = ["sowingDateEpochDay"]),
    ],
)
data class CropCycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val plotId: Long,
    val templateId: Long? = null,
    val cropName: String,
    val variety: String? = null,
    val season: SeasonType,
    val cropYear: Int,
    val plantedAreaMicroAcres: Long,
    val sowingDateEpochDay: Long,
    val expectedHarvestEpochDay: Long? = null,
    val actualHarvestEpochDay: Long? = null,
    val status: CropCycleStatus = CropCycleStatus.ACTIVE,
    val isIntercrop: Boolean = false,
    val voiceNamePath: String? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "crop_tasks",
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
        Index(value = ["status", "isArchived", "dueDateEpochDay"]),
    ],
)
data class CropTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val title: String,
    val description: String? = null,
    val category: TaskCategory,
    val dueDateEpochDay: Long,
    val offsetFromSowingDays: Int? = null,
    val sourceTemplateStageKey: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val completedAtEpochMillis: Long? = null,
    val reminderEnabled: Boolean = true,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "diary_entries",
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
        Index(value = ["isArchived", "entryDateEpochMillis"]),
    ],
)
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val entryDateEpochMillis: Long,
    val category: DiaryCategory,
    val title: String,
    val notes: String? = null,
    val photoPaths: List<String> = emptyList(),
    val voiceNotePath: String? = null,
    val voiceNoteDurationMillis: Long? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
