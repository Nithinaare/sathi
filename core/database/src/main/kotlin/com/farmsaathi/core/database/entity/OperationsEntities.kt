package com.farmsaathi.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.farmsaathi.core.model.AttendanceStatus
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.core.model.MachineryBillingMode
import com.farmsaathi.core.model.MachineryEntryMode
import com.farmsaathi.core.model.MachinerySessionStatus
import com.farmsaathi.core.model.MachineryUsageType
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.core.model.StockMovementType
import com.farmsaathi.core.model.WorkerPaymentType

@Entity(
    tableName = "workers",
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
        Index(value = ["isArchived", "name"]),
    ],
)
data class WorkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long? = null,
    val name: String,
    val phone: String? = null,
    val skillRole: String? = null,
    val photoPath: String? = null,
    val voiceNamePath: String? = null,
    val daysWorkedTenths: Int = 0,
    val defaultDailyWagePaise: Long,
    val defaultHalfDayPercent: Int = 50,
    val halfDayRatePaise: Long? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "worker_attendance",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
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
        Index(value = ["workerId", "workDateEpochDay"], unique = true),
        Index(value = ["farmId"]),
        Index(value = ["plotId"]),
        Index(value = ["cropCycleId"]),
        Index(value = ["workDateEpochDay"]),
    ],
)
data class WorkerAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workerId: Long,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val workDateEpochDay: Long,
    val status: AttendanceStatus,
    val baseDailyWagePaise: Long,
    val halfDayPercent: Int = 50,
    val pieceRateUnit: QuantityUnit? = null,
    val pieceQuantityMilliUnits: Long? = null,
    val pieceUnitRatePaise: Long? = null,
    val wageAccruedPaise: Long,
    val notes: String? = null,
    val isVoided: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "worker_payments",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
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
    ],
    indices = [
        Index(value = ["workerId"]),
        Index(value = ["farmId"]),
        Index(value = ["cropCycleId"]),
        Index(value = ["paymentDateEpochMillis"]),
    ],
)
data class WorkerPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workerId: Long,
    val farmId: Long,
    val cropCycleId: Long? = null,
    val paymentType: WorkerPaymentType,
    val amountPaise: Long,
    val paymentDateEpochMillis: Long,
    val paymentMode: PaymentMode,
    val notes: String? = null,
    val isVoided: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "machinery_sessions",
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
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerContactId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["activeLockKey"], unique = true),
        Index(value = ["farmId"]),
        Index(value = ["plotId"]),
        Index(value = ["cropCycleId"]),
        Index(value = ["providerContactId"]),
        Index(value = ["status", "startEpochMillis"]),
    ],
)
data class MachinerySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val providerContactId: Long? = null,
    val machineName: String,
    val operatorName: String? = null,
    val operationType: String,
    val equipmentKey: String,
    val activeLockKey: String? = null,
    val usageType: MachineryUsageType = MachineryUsageType.HIRED_IN,
    val billingMode: MachineryBillingMode = MachineryBillingMode.HOURLY,
    val entryMode: MachineryEntryMode = MachineryEntryMode.LIVE_TIMER,
    val status: MachinerySessionStatus = MachinerySessionStatus.RUNNING,
    val startEpochMillis: Long,
    val startElapsedRealtimeMillis: Long? = null,
    val startBootCount: Int? = null,
    val endEpochMillis: Long? = null,
    val endElapsedRealtimeMillis: Long? = null,
    val elapsedMinutes: Long? = null,
    val hourlyRatePaise: Long? = null,
    val perAcreRatePaise: Long? = null,
    val areaCoveredMicroAcres: Long? = null,
    val computedAmountPaise: Long = 0L,
    val paidAmountPaise: Long = 0L,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val notes: String? = null,
    val isVoided: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "input_products",
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
        Index(value = ["isArchived", "category", "name"]),
    ],
)
data class InputProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long? = null,
    val name: String,
    val category: InputCategory,
    val brand: String? = null,
    val unit: QuantityUnit,
    val bagWeightMilliKg: Long? = null,
    val currentStockMilliUnits: Long = 0L,
    val lowStockThresholdMilliUnits: Long = 0L,
    val defaultUnitPricePaise: Long? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "input_purchases",
    foreignKeys = [
        ForeignKey(
            entity = InputProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT,
        ),
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
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["vendorContactId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["farmId"]),
        Index(value = ["cropCycleId"]),
        Index(value = ["vendorContactId"]),
        Index(value = ["purchaseDateEpochMillis"]),
    ],
)
data class InputPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val farmId: Long,
    val cropCycleId: Long? = null,
    val vendorContactId: Long? = null,
    val vendorNameSnapshot: String? = null,
    val billNumber: String? = null,
    val quantityMilliUnits: Long,
    val unit: QuantityUnit,
    val unitPricePaise: Long,
    val totalAmountPaise: Long,
    val paidAmountPaise: Long,
    val paymentStatus: PaymentStatus,
    val paymentMode: PaymentMode,
    val purchaseDateEpochMillis: Long,
    val expiryDateEpochDay: Long? = null,
    val dueDateEpochMillis: Long? = null,
    val receiptPhotoPath: String? = null,
    val notes: String? = null,
    val isVoided: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = InputProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT,
        ),
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
        ForeignKey(
            entity = InputPurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["purchaseId"], unique = true),
        Index(value = ["productId", "movementDateEpochMillis"]),
        Index(value = ["farmId"]),
        Index(value = ["plotId"]),
        Index(value = ["cropCycleId"]),
    ],
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val purchaseId: Long? = null,
    val movementType: StockMovementType,
    val reason: StockMovementReason,
    val signedDeltaMilliUnits: Long,
    val balanceAfterMilliUnits: Long,
    val unitCostPaiseSnapshot: Long? = null,
    val movementDateEpochMillis: Long,
    val notes: String? = null,
    val photoPath: String? = null,
    val createdAtEpochMillis: Long,
)
