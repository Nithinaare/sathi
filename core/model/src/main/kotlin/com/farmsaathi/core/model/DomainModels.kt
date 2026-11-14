package com.farmsaathi.core.model

/**
 * Pure Kotlin domain models decoupled from Room entities and Compose UI state.
 */
data class Farm(
    val id: Long = 0L,
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
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L,
)

data class Plot(
    val id: Long = 0L,
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
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L,
)

data class CropTemplate(
    val id: Long = 0L,
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
)

data class EditableCropTemplate(
    val id: Long = 0L,
    val templateKey: String = "",
    val cropNameTe: String,
    val cropNameEn: String,
    val defaultSeason: SeasonType = SeasonType.KHARIF,
    val expectedDurationDays: Int = 120,
    val defaultYieldUnit: QuantityUnit = QuantityUnit.QUINTAL,
    val stages: List<CropTemplateStage> = emptyList(),
    val isStarterTemplate: Boolean = false,
    val isEditable: Boolean = true,
    val disclaimerTe: String = StarterCropTemplates.DISCLAIMER_TE,
    val disclaimerEn: String = StarterCropTemplates.DISCLAIMER_EN,
    val isArchived: Boolean = false,
) {
    fun toCropTemplate(): CropTemplate =
        CropTemplate(
            id = id,
            templateKey = templateKey,
            cropNameTe = cropNameTe,
            cropNameEn = cropNameEn,
            defaultSeason = defaultSeason,
            expectedDurationDays = expectedDurationDays,
            defaultYieldUnit = defaultYieldUnit,
            stages = stages,
            isStarterTemplate = isStarterTemplate,
            isEditable = isEditable,
            disclaimerTe = disclaimerTe,
            disclaimerEn = disclaimerEn,
            isArchived = isArchived,
        )
}

fun CropTemplate.toEditable(): EditableCropTemplate =
    EditableCropTemplate(
        id = id,
        templateKey = templateKey,
        cropNameTe = cropNameTe,
        cropNameEn = cropNameEn,
        defaultSeason = defaultSeason,
        expectedDurationDays = expectedDurationDays,
        defaultYieldUnit = defaultYieldUnit,
        stages = stages,
        isStarterTemplate = isStarterTemplate,
        isEditable = isEditable,
        disclaimerTe = disclaimerTe,
        disclaimerEn = disclaimerEn,
        isArchived = isArchived,
    )

data class CropCycle(
    val id: Long = 0L,
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
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L,
)

data class CropTask(
    val id: Long = 0L,
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
)

data class DiaryEntry(
    val id: Long = 0L,
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
)

data class LedgerEntry(
    val id: Long = 0L,
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
    val totalAmount: MoneyPaise,
    val settledAmount: MoneyPaise,
    val paymentStatus: PaymentStatus,
    val paymentMode: PaymentMode,
    val transactionDateEpochMillis: Long,
    val dueDateEpochMillis: Long? = null,
    val receiptPhotoPath: String? = null,
    val voiceNotePath: String? = null,
    val isVoided: Boolean = false,
    val voidReason: String? = null,
) {
    val balanceDue: MoneyPaise
        get() = (totalAmount - settledAmount).let { if (it.isNegative()) MoneyPaise.ZERO else it }
}

data class ExpenseDetail(
    val id: Long = 0L,
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
    val unitPrice: MoneyPaise? = null,
    val totalAmount: MoneyPaise,
    val paidAmount: MoneyPaise,
    val paymentMode: PaymentMode,
    val expenseDateEpochMillis: Long,
    val receiptPhotoPath: String? = null,
    val voiceNotePath: String? = null,
    val voiceNoteDurationMillis: Long? = null,
    val notes: String? = null,
    val isVoided: Boolean = false,
)

data class CropSale(
    val id: Long = 0L,
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
    val ratePerUnit: MoneyPaise,
    val grossAmount: MoneyPaise,
    val deductions: MoneyPaise = MoneyPaise.ZERO,
    val netTotalAmount: MoneyPaise,
    val amountReceived: MoneyPaise,
    val paymentStatus: PaymentStatus,
    val paymentMode: PaymentMode,
    val saleDateEpochMillis: Long,
    val dueDateEpochMillis: Long? = null,
    val receiptPhotoPath: String? = null,
    val notes: String? = null,
    val isVoided: Boolean = false,
) {
    val outstandingAmount: MoneyPaise
        get() = (netTotalAmount - amountReceived).let { if (it.isNegative()) MoneyPaise.ZERO else it }
}

data class Worker(
    val id: Long = 0L,
    val farmId: Long? = null,
    val name: String,
    val phone: String? = null,
    val skillRole: String? = null,
    val photoPath: String? = null,
    val voiceNamePath: String? = null,
    val daysWorkedTenths: Int = 0,
    val defaultDailyWage: MoneyPaise,
    val defaultHalfDayPercent: Int = 50,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val halfDayRatePaise: Long? = null,
) {
    val effectiveHalfDayWage: MoneyPaise
        get() = halfDayRatePaise?.let(::MoneyPaise) ?: defaultDailyWage.percentage(defaultHalfDayPercent)
}

data class WorkerAttendance(
    val id: Long = 0L,
    val workerId: Long,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val workDateEpochDay: Long,
    val status: AttendanceStatus,
    val baseDailyWage: MoneyPaise,
    val halfDayPercent: Int = 50,
    val pieceRateUnit: QuantityUnit? = null,
    val pieceQuantityMilliUnits: Long? = null,
    val pieceUnitRate: MoneyPaise? = null,
    val wageAccrued: MoneyPaise,
    val notes: String? = null,
    val isVoided: Boolean = false,
)

data class WorkerPayment(
    val id: Long = 0L,
    val workerId: Long,
    val farmId: Long,
    val cropCycleId: Long? = null,
    val paymentType: WorkerPaymentType,
    val amount: MoneyPaise,
    val paymentDateEpochMillis: Long,
    val paymentMode: PaymentMode,
    val notes: String? = null,
    val isVoided: Boolean = false,
)

data class WorkerBalanceSummary(
    val worker: Worker,
    val totalWageAccrued: MoneyPaise,
    val totalAdvancesPaid: MoneyPaise,
    val totalSettlementsPaid: MoneyPaise,
    val totalDaysWorkedTenths: Int = worker.daysWorkedTenths,
) {
    val pendingBalance: MoneyPaise
        get() = totalWageAccrued - (totalAdvancesPaid + totalSettlementsPaid)
}

data class MachinerySession(
    val id: Long = 0L,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val providerContactId: Long? = null,
    val machineName: String,
    val operatorName: String? = null,
    val operationType: String,
    val equipmentKey: String,
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
    val hourlyRate: MoneyPaise? = null,
    val perAcreRate: MoneyPaise? = null,
    val areaCoveredMicroAcres: Long? = null,
    val computedAmount: MoneyPaise = MoneyPaise.ZERO,
    val paidAmount: MoneyPaise = MoneyPaise.ZERO,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val notes: String? = null,
    val isVoided: Boolean = false,
)

data class InputProduct(
    val id: Long = 0L,
    val farmId: Long? = null,
    val name: String,
    val category: InputCategory,
    val brand: String? = null,
    val unit: QuantityUnit,
    val bagWeightMilliKg: Long? = null,
    val currentStockMilliUnits: Long = 0L,
    val lowStockThresholdMilliUnits: Long = 0L,
    val defaultUnitPrice: MoneyPaise? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
)

data class InputPurchase(
    val id: Long = 0L,
    val productId: Long,
    val farmId: Long,
    val cropCycleId: Long? = null,
    val vendorContactId: Long? = null,
    val vendorNameSnapshot: String? = null,
    val billNumber: String? = null,
    val quantityMilliUnits: Long,
    val unit: QuantityUnit,
    val unitPrice: MoneyPaise,
    val totalAmount: MoneyPaise,
    val paidAmount: MoneyPaise,
    val paymentStatus: PaymentStatus,
    val paymentMode: PaymentMode,
    val purchaseDateEpochMillis: Long,
    val expiryDateEpochDay: Long? = null,
    val dueDateEpochMillis: Long? = null,
    val receiptPhotoPath: String? = null,
    val notes: String? = null,
    val isVoided: Boolean = false,
)

data class StockMovement(
    val id: Long = 0L,
    val productId: Long,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val purchaseId: Long? = null,
    val movementType: StockMovementType,
    val reason: StockMovementReason,
    val signedDeltaMilliUnits: Long,
    val balanceAfterMilliUnits: Long,
    val unitCostSnapshot: MoneyPaise? = null,
    val movementDateEpochMillis: Long,
    val notes: String? = null,
    val photoPath: String? = null,
)

data class IrrigationLog(
    val id: Long = 0L,
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
)

data class HarvestRecord(
    val id: Long = 0L,
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
    val isArchived: Boolean = false,
    val photoPath: String? = null,
)

data class StorageRecord(
    val id: Long = 0L,
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
)

data class SoilTestRecord(
    val id: Long = 0L,
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
)

data class Contact(
    val id: Long = 0L,
    val name: String,
    val phone: String? = null,
    val contactType: ContactType,
    val villageOrMandi: String? = null,
    val upiId: String? = null,
    val voiceNamePath: String? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
)
