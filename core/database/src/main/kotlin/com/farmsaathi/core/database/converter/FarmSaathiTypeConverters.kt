package com.farmsaathi.core.database.converter

import androidx.room.TypeConverter
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.AttendanceStatus
import com.farmsaathi.core.model.ContactType
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropStageTemplate
import com.farmsaathi.core.model.DiaryCategory
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.core.model.IrrigationMethod
import com.farmsaathi.core.model.IrrigationSource
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.LedgerSourceType
import com.farmsaathi.core.model.MachineryBillingMode
import com.farmsaathi.core.model.MachineryEntryMode
import com.farmsaathi.core.model.MachinerySessionStatus
import com.farmsaathi.core.model.MachineryUsageType
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.SeasonType
import com.farmsaathi.core.model.SoilHealthRating
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.core.model.StockMovementType
import com.farmsaathi.core.model.StorageStatus
import com.farmsaathi.core.model.TaskCategory
import com.farmsaathi.core.model.TaskPriority
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.core.model.WorkerPaymentType
import org.json.JSONArray
import org.json.JSONObject

// Room requires one from/to @TypeConverter pair per domain enum & value type.
@Suppress("TooManyFunctions")
class FarmSaathiTypeConverters {
    @TypeConverter fun fromAreaUnit(v: AreaUnit?): String? = v?.name

    @TypeConverter fun toAreaUnit(raw: String?): AreaUnit? = raw?.let(AreaUnit::valueOf)

    @TypeConverter fun fromQuantityUnit(v: QuantityUnit?): String? = v?.name

    @TypeConverter fun toQuantityUnit(raw: String?): QuantityUnit? = raw?.let(QuantityUnit::valueOf)

    @TypeConverter fun fromSeasonType(v: SeasonType?): String? = v?.name

    @TypeConverter fun toSeasonType(raw: String?): SeasonType? = raw?.let(SeasonType::valueOf)

    @TypeConverter fun fromCropCycleStatus(v: CropCycleStatus?): String? = v?.name

    @TypeConverter fun toCropCycleStatus(raw: String?): CropCycleStatus? = raw?.let(CropCycleStatus::valueOf)

    @TypeConverter fun fromTaskCategory(v: TaskCategory?): String? = v?.name

    @TypeConverter fun toTaskCategory(raw: String?): TaskCategory? = raw?.let(TaskCategory::valueOf)

    @TypeConverter fun fromTaskPriority(v: TaskPriority?): String? = v?.name

    @TypeConverter fun toTaskPriority(raw: String?): TaskPriority? = raw?.let(TaskPriority::valueOf)

    @TypeConverter fun fromTaskStatus(v: TaskStatus?): String? = v?.name

    @TypeConverter fun toTaskStatus(raw: String?): TaskStatus? = raw?.let(TaskStatus::valueOf)

    @TypeConverter fun fromDiaryCategory(v: DiaryCategory?): String? = v?.name

    @TypeConverter fun toDiaryCategory(raw: String?): DiaryCategory? = raw?.let(DiaryCategory::valueOf)

    @TypeConverter fun fromLedgerEntryType(v: LedgerEntryType?): String? = v?.name

    @TypeConverter fun toLedgerEntryType(raw: String?): LedgerEntryType? = raw?.let(LedgerEntryType::valueOf)

    @TypeConverter fun fromLedgerSourceType(v: LedgerSourceType?): String? = v?.name

    @TypeConverter fun toLedgerSourceType(raw: String?): LedgerSourceType? = raw?.let(LedgerSourceType::valueOf)

    @TypeConverter fun fromLedgerCategory(v: LedgerCategory?): String? = v?.name

    @TypeConverter fun toLedgerCategory(raw: String?): LedgerCategory? = raw?.let(LedgerCategory::valueOf)

    @TypeConverter fun fromPaymentStatus(v: PaymentStatus?): String? = v?.name

    @TypeConverter fun toPaymentStatus(raw: String?): PaymentStatus? = raw?.let(PaymentStatus::valueOf)

    @TypeConverter fun fromPaymentMode(v: PaymentMode?): String? = v?.name

    @TypeConverter fun toPaymentMode(raw: String?): PaymentMode? = raw?.let(PaymentMode::valueOf)

    @TypeConverter fun fromAttendanceStatus(v: AttendanceStatus?): String? = v?.name

    @TypeConverter fun toAttendanceStatus(raw: String?): AttendanceStatus? = raw?.let(AttendanceStatus::valueOf)

    @TypeConverter fun fromWorkerPaymentType(v: WorkerPaymentType?): String? = v?.name

    @TypeConverter fun toWorkerPaymentType(raw: String?): WorkerPaymentType? = raw?.let(WorkerPaymentType::valueOf)

    @TypeConverter fun fromMachineryUsageType(v: MachineryUsageType?): String? = v?.name

    @TypeConverter fun toMachineryUsageType(raw: String?): MachineryUsageType? = raw?.let(MachineryUsageType::valueOf)

    @TypeConverter fun fromMachineryBillingMode(v: MachineryBillingMode?): String? = v?.name

    @TypeConverter fun toMachineryBillingMode(raw: String?): MachineryBillingMode? = raw?.let(MachineryBillingMode::valueOf)

    @TypeConverter fun fromMachineryEntryMode(v: MachineryEntryMode?): String? = v?.name

    @TypeConverter fun toMachineryEntryMode(raw: String?): MachineryEntryMode? = raw?.let(MachineryEntryMode::valueOf)

    @TypeConverter fun fromMachinerySessionStatus(v: MachinerySessionStatus?): String? = v?.name

    @TypeConverter fun toMachinerySessionStatus(raw: String?): MachinerySessionStatus? = raw?.let(MachinerySessionStatus::valueOf)

    @TypeConverter fun fromInputCategory(v: InputCategory?): String? = v?.name

    @TypeConverter fun toInputCategory(raw: String?): InputCategory? = raw?.let(InputCategory::valueOf)

    @TypeConverter fun fromStockMovementType(v: StockMovementType?): String? = v?.name

    @TypeConverter fun toStockMovementType(raw: String?): StockMovementType? = raw?.let(StockMovementType::valueOf)

    @TypeConverter fun fromStockMovementReason(v: StockMovementReason?): String? = v?.name

    @TypeConverter fun toStockMovementReason(raw: String?): StockMovementReason? = raw?.let(StockMovementReason::valueOf)

    @TypeConverter fun fromIrrigationSource(v: IrrigationSource?): String? = v?.name

    @TypeConverter fun toIrrigationSource(raw: String?): IrrigationSource? = raw?.let(IrrigationSource::valueOf)

    @TypeConverter fun fromIrrigationMethod(v: IrrigationMethod?): String? = v?.name

    @TypeConverter fun toIrrigationMethod(raw: String?): IrrigationMethod? = raw?.let(IrrigationMethod::valueOf)

    @TypeConverter fun fromStorageStatus(v: StorageStatus?): String? = v?.name

    @TypeConverter fun toStorageStatus(raw: String?): StorageStatus? = raw?.let(StorageStatus::valueOf)

    @TypeConverter fun fromSoilHealthRating(v: SoilHealthRating?): String? = v?.name

    @TypeConverter fun toSoilHealthRating(raw: String?): SoilHealthRating? = raw?.let(SoilHealthRating::valueOf)

    @TypeConverter fun fromContactType(v: ContactType?): String? = v?.name

    @TypeConverter fun toContactType(raw: String?): ContactType? = raw?.let(ContactType::valueOf)

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        val arr = JSONArray()
        list?.forEach { arr.put(it) }
        return arr.toString()
    }

    @TypeConverter
    fun toStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = JSONArray(raw)
        return List(arr.length()) { idx -> arr.getString(idx) }
    }

    @TypeConverter
    fun fromStageList(stages: List<CropStageTemplate>?): String {
        val arr = JSONArray()
        stages?.forEach { stage ->
            arr.put(
                JSONObject().apply {
                    put("stageKey", stage.stageKey)
                    put("titleTe", stage.titleTe)
                    put("titleEn", stage.titleEn)
                    put("offsetDaysFromSowing", stage.offsetDaysFromSowing)
                    put("category", stage.category.name)
                    put("notesTe", stage.notesTe)
                    put("notesEn", stage.notesEn)
                },
            )
        }
        return arr.toString()
    }

    @TypeConverter
    fun toStageList(raw: String?): List<CropStageTemplate> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = JSONArray(raw)
        return List(arr.length()) { idx ->
            val obj = arr.getJSONObject(idx)
            CropStageTemplate(
                stageKey = obj.getString("stageKey"),
                titleTe = obj.getString("titleTe"),
                titleEn = obj.getString("titleEn"),
                offsetDaysFromSowing = obj.getInt("offsetDaysFromSowing"),
                category = TaskCategory.valueOf(obj.getString("category")),
                notesTe = obj.optString("notesTe", ""),
                notesEn = obj.optString("notesEn", ""),
            )
        }
    }
}
