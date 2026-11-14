package com.farmsaathi.core.model

import kotlinx.serialization.Serializable

enum class AppLanguage {
    TELUGU,
    ENGLISH,
}

enum class AreaUnit {
    ACRE_GUNTA,
    ACRE_CENT,
    ACRE_DECIMAL,
}

enum class QuantityUnit {
    KG,
    QUINTAL,
    BAG,
    TON,
    LITRE,
    ML,
    PIECE,
    HOUR,
    ACRE,
}

enum class SeasonType {
    KHARIF,
    RABI,
    ZAID,
    PERENNIAL,
}

enum class CropCycleStatus {
    PLANNED,
    ACTIVE,
    HARVESTED,
    CLOSED,
    FAILED,
}

enum class TaskCategory {
    LAND_PREP,
    SOWING,
    WEEDING,
    FERTILIZER,
    SPRAYING,
    IRRIGATION,
    HARVEST,
    OTHER,
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
}

enum class TaskStatus {
    PENDING,
    COMPLETED,
    RESCHEDULED,
    MISSED,
    CANCELLED,
}

enum class DiaryCategory {
    CROP_GROWTH,
    PEST_OBSERVATION,
    IRRIGATION,
    FERTILIZER_SPRAY,
    WEATHER_DAMAGE,
    HARVEST,
    OTHER,
}

enum class LedgerEntryType {
    INCOME,
    EXPENSE,
}

enum class LedgerSourceType {
    MANUAL_EXPENSE,
    MANUAL_INCOME,
    INPUT_PURCHASE,
    CROP_SALE,
    WORKER_PAYMENT,
    MACHINERY_SESSION,
}

enum class LedgerCategory {
    SEEDS,
    FERTILIZERS,
    PESTICIDES,
    LABOUR,
    MACHINERY,
    DIESEL,
    TRANSPORT,
    IRRIGATION,
    REPAIRS,
    OTHER_EXPENSE,
    CROP_SALE,
    MACHINERY_HIRE_INCOME,
    OTHER_INCOME,
}

enum class PaymentStatus {
    PAID,
    PARTIAL,
    PENDING,
}

enum class PaymentMode {
    CASH,
    UPI,
    BANK_TRANSFER,
    CREDIT,
    CHEQUE,
    OTHER,
}

enum class AttendanceStatus {
    FULL_DAY,
    HALF_DAY,
    ABSENT,
    PIECE_RATE,
}

enum class WorkerPaymentType {
    ADVANCE,
    SETTLEMENT,
}

enum class MachineryUsageType {
    HIRED_IN,
    OWN_FARM_USE,
    HIRED_OUT,
}

enum class MachineryBillingMode {
    HOURLY,
    PER_ACRE,
    FIXED_FLAT,
}

enum class MachineryEntryMode {
    LIVE_TIMER,
    MANUAL_LOG,
}

enum class MachinerySessionStatus {
    RUNNING,
    COMPLETED,
    CANCELLED,
}

enum class InputCategory {
    SEED,
    FERTILIZER,
    PESTICIDE,
    HERBICIDE,
    FUNGICIDE,
    MICRONUTRIENT,
    ORGANIC,
    OTHER,
}

enum class StockMovementType {
    IN,
    OUT,
    ADJUSTMENT,
}

enum class StockMovementReason {
    PURCHASE,
    OPENING_STOCK,
    FIELD_APPLICATION,
    RETURN,
    WASTAGE_EXPIRED,
    STOCK_COUNT_CORRECTION,
    ;

    val isStockIn: Boolean
        get() = this == PURCHASE || this == OPENING_STOCK || this == RETURN

    val isStockOut: Boolean
        get() = this == FIELD_APPLICATION || this == WASTAGE_EXPIRED
}

enum class IrrigationSource {
    BOREWELL,
    CANAL,
    OPEN_WELL,
    RAINFED,
    DRIP,
    TANK,
    OTHER,
}

enum class IrrigationMethod {
    FLOOD,
    DRIP,
    SPRINKLER,
    FURROW,
    OTHER,
}

enum class StorageStatus {
    IN_STORAGE,
    PARTIALLY_DISPATCHED,
    CLEARED,
}

enum class SoilHealthRating {
    LOW,
    MEDIUM,
    OPTIMAL,
    HIGH,
    SALINE_ALKALINE,
}

enum class ContactType {
    TRACTOR_OWNER,
    HARVESTER_OWNER,
    LABOUR_CONTRACTOR,
    FERTILIZER_SHOP,
    PESTICIDE_SHOP,
    SEED_SUPPLIER,
    MECHANIC,
    TRANSPORT_PROVIDER,
    AGRICULTURE_OFFICER,
    BUYER_TRADER,
    OTHER,
}

@Serializable
data class CropStageTemplate(
    val stageKey: String,
    val titleTe: String,
    val titleEn: String,
    val offsetDaysFromSowing: Int,
    val category: TaskCategory,
    val notesTe: String = "",
    val notesEn: String = "",
)

typealias CropTemplateStage = CropStageTemplate
