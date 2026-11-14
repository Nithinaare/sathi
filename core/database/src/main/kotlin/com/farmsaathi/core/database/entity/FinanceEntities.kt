package com.farmsaathi.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.LedgerSourceType
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.core.model.QuantityUnit

@Entity(
    tableName = "ledger_entries",
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
            childColumns = ["contactId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["sourceType", "sourceId"], unique = true),
        Index(value = ["farmId"]),
        Index(value = ["plotId"]),
        Index(value = ["cropCycleId"]),
        Index(value = ["contactId"]),
        Index(value = ["isVoided", "isArchived", "entryType", "transactionDateEpochMillis"]),
        Index(value = ["paymentStatus", "isVoided", "isArchived"]),
    ],
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entryType: LedgerEntryType,
    val sourceType: LedgerSourceType,
    val sourceId: Long? = null,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val contactId: Long? = null,
    val category: LedgerCategory,
    val title: String,
    val notes: String? = null,
    val totalAmountPaise: Long,
    val settledAmountPaise: Long,
    val paymentStatus: PaymentStatus,
    val paymentMode: PaymentMode,
    val transactionDateEpochMillis: Long,
    val dueDateEpochMillis: Long? = null,
    val receiptPhotoPath: String? = null,
    val voiceNotePath: String? = null,
    val isVoided: Boolean = false,
    val voidReason: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "expense_details",
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
            childColumns = ["contactId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["plotId"]),
        Index(value = ["cropCycleId"]),
        Index(value = ["contactId"]),
        Index(value = ["expenseDateEpochMillis"]),
    ],
)
data class ExpenseDetailEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val contactId: Long? = null,
    val category: LedgerCategory,
    val description: String,
    val vendorNameSnapshot: String? = null,
    val billNumber: String? = null,
    val quantityMilliUnits: Long? = null,
    val quantityUnit: QuantityUnit? = null,
    val unitPricePaise: Long? = null,
    val totalAmountPaise: Long,
    val paidAmountPaise: Long,
    val paymentMode: PaymentMode,
    val expenseDateEpochMillis: Long,
    val receiptPhotoPath: String? = null,
    val voiceNotePath: String? = null,
    val voiceNoteDurationMillis: Long? = null,
    val notes: String? = null,
    val isVoided: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "crop_sales",
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
            entity = HarvestRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["harvestRecordId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = StorageRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["storageRecordId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["buyerContactId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["plotId"]),
        Index(value = ["cropCycleId"]),
        Index(value = ["harvestRecordId"]),
        Index(value = ["storageRecordId"]),
        Index(value = ["buyerContactId"]),
        Index(value = ["saleDateEpochMillis"]),
    ],
)
data class CropSaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long,
    val harvestRecordId: Long? = null,
    val storageRecordId: Long? = null,
    val buyerContactId: Long? = null,
    val buyerNameSnapshot: String,
    val mandiOrLocation: String? = null,
    val quantityMilliUnits: Long,
    val quantityUnit: QuantityUnit,
    val ratePerUnitPaise: Long,
    val grossAmountPaise: Long,
    val deductionsPaise: Long = 0L,
    val netTotalAmountPaise: Long,
    val amountReceivedPaise: Long,
    val paymentStatus: PaymentStatus,
    val paymentMode: PaymentMode,
    val saleDateEpochMillis: Long,
    val dueDateEpochMillis: Long? = null,
    val receiptPhotoPath: String? = null,
    val notes: String? = null,
    val isVoided: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
