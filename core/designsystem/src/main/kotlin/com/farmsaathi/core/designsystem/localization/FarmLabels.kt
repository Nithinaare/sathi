package com.farmsaathi.core.designsystem.localization

import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AreaConverter
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.AttendanceStatus
import com.farmsaathi.core.model.ContactType
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.DiaryCategory
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.core.model.IrrigationMethod
import com.farmsaathi.core.model.IrrigationSource
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.MachineryBillingMode
import com.farmsaathi.core.model.MachineryUsageType
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.SeasonType
import com.farmsaathi.core.model.SoilHealthRating
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.core.model.StorageStatus
import com.farmsaathi.core.model.TaskCategory
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.core.model.WorkerPaymentType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Single centralized label and unit formatter across all feature modules (DRY).
 */
object FarmLabels {
    private fun pick(lang: AppLanguage, pair: Pair<String, String>): String =
        if (lang == AppLanguage.TELUGU) pair.first else pair.second

    private val ledgerCategoryLabels = mapOf(
        LedgerCategory.SEEDS to ("విత్తనాలు" to "Seeds"),
        LedgerCategory.FERTILIZERS to ("ఎరువులు" to "Fertilisers"),
        LedgerCategory.PESTICIDES to ("పురుగు మందులు" to "Pesticides"),
        LedgerCategory.LABOUR to ("కూలీలు" to "Labour"),
        LedgerCategory.MACHINERY to ("యంత్రం / ట్రాక్టర్" to "Machinery"),
        LedgerCategory.DIESEL to ("డీజిల్ / ఇంధనం" to "Diesel"),
        LedgerCategory.TRANSPORT to ("రవాణా / బాడుగ" to "Transport"),
        LedgerCategory.IRRIGATION to ("నీటి పారుదల" to "Irrigation"),
        LedgerCategory.REPAIRS to ("మరమ్మతులు" to "Repairs"),
        LedgerCategory.OTHER_EXPENSE to ("ఇతర ఖర్చు" to "Other Expense"),
        LedgerCategory.CROP_SALE to ("పంట అమ్మకం" to "Crop Sale"),
        LedgerCategory.MACHINERY_HIRE_INCOME to ("యంత్రం అద్దె ఆదాయం" to "Machinery Hire Income"),
        LedgerCategory.OTHER_INCOME to ("ఇతర ఆదాయం" to "Other Income"),
    )

    private val taskCategoryLabels = mapOf(
        TaskCategory.LAND_PREP to ("పొలం తయారీ" to "Land Prep"),
        TaskCategory.SOWING to ("విత్తడం / నాటు" to "Sowing"),
        TaskCategory.WEEDING to ("కలుపు తీత" to "Weeding"),
        TaskCategory.FERTILIZER to ("ఎరువు" to "Fertilizer"),
        TaskCategory.SPRAYING to ("మందు పిచికారీ" to "Spraying"),
        TaskCategory.IRRIGATION to ("నీటి తడి" to "Irrigation"),
        TaskCategory.HARVEST to ("కోత" to "Harvest"),
        TaskCategory.OTHER to ("ఇతర పని" to "Other"),
    )

    private val diaryCategoryLabels = mapOf(
        DiaryCategory.CROP_GROWTH to ("పంట పెరుగుదల" to "Crop Growth"),
        DiaryCategory.PEST_OBSERVATION to ("పురుగు / తెగులు చూపు" to "Pest Note"),
        DiaryCategory.IRRIGATION to ("నీటి తడి" to "Irrigation"),
        DiaryCategory.FERTILIZER_SPRAY to ("ఎరువు / మందు" to "Spray / Fertilizer"),
        DiaryCategory.WEATHER_DAMAGE to ("వాతావరణ నష్టం" to "Weather Damage"),
        DiaryCategory.HARVEST to ("కోత" to "Harvest"),
        DiaryCategory.OTHER to ("ఇతర గమనిక" to "Other"),
    )

    private val inputCategoryLabels = mapOf(
        InputCategory.SEED to ("విత్తనాలు" to "Seeds"),
        InputCategory.FERTILIZER to ("ఎరువులు" to "Fertilizer"),
        InputCategory.PESTICIDE to ("పురుగు మందు" to "Pesticide"),
        InputCategory.HERBICIDE to ("కలుపు మందు" to "Herbicide"),
        InputCategory.FUNGICIDE to ("తెగులు మందు" to "Fungicide"),
        InputCategory.MICRONUTRIENT to ("సూక్ష్మ పోషకాలు" to "Micronutrient"),
        InputCategory.ORGANIC to ("సేంద్రియ ఎరువు" to "Organic"),
        InputCategory.OTHER to ("ఇతర సరుకు" to "Other"),
    )

    private val contactTypeLabels = mapOf(
        ContactType.TRACTOR_OWNER to ("ట్రాక్టర్ యజమాని" to "Tractor Owner"),
        ContactType.HARVESTER_OWNER to ("హార్వెస్టర్ యజమాని" to "Harvester Owner"),
        ContactType.LABOUR_CONTRACTOR to ("కూలీ మేస్త్రి" to "Labour Contractor"),
        ContactType.FERTILIZER_SHOP to ("ఎరువుల షాప్" to "Fertiliser Shop"),
        ContactType.PESTICIDE_SHOP to ("మందుల షాప్" to "Pesticide Shop"),
        ContactType.SEED_SUPPLIER to ("విత్తనాల షాప్" to "Seed Supplier"),
        ContactType.MECHANIC to ("మెకానిక్" to "Mechanic"),
        ContactType.TRANSPORT_PROVIDER to ("రవాణా / ఆటో" to "Transport"),
        ContactType.AGRICULTURE_OFFICER to ("వ్యవసాయ అధికారి" to "Agri Officer"),
        ContactType.BUYER_TRADER to ("పంట వ్యాపారి" to "Crop Buyer"),
        ContactType.OTHER to ("ఇతర" to "Other"),
    )

    fun formatArea(
        microAcres: Long,
        unit: AreaUnit,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (unit) {
            AreaUnit.ACRE_GUNTA -> {
                val (ac, gu) = AreaConverter.toAcresAndGuntas(microAcres)
                if (te) "$ac ఎక. $gu గుం." else "$ac Ac $gu Gu"
            }
            AreaUnit.ACRE_CENT -> {
                val (ac, ct) = AreaConverter.toAcresAndCents(microAcres)
                if (te) "$ac ఎక. $ct సెం." else "$ac Ac $ct Ct"
            }
            AreaUnit.ACRE_DECIMAL -> {
                val dec = AreaConverter.toDecimalAcres(microAcres).toPlainString()
                if (te) "$dec ఎకరాలు" else "$dec Acres"
            }
        }
    }

    fun formatQuantity(
        milliUnits: Long,
        unit: QuantityUnit,
        lang: AppLanguage,
    ): String {
        val dec =
            BigDecimal(milliUnits)
                .divide(BigDecimal(1_000), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        return "$dec ${quantityUnit(unit, lang)}"
    }

    fun quantityUnit(
        unit: QuantityUnit,
        lang: AppLanguage,
    ): String =
        when (unit) {
            QuantityUnit.KG -> if (lang == AppLanguage.TELUGU) "కిలోలు" else "Kg"
            QuantityUnit.QUINTAL -> if (lang == AppLanguage.TELUGU) "క్వింటాళ్ళు" else "Quintal"
            QuantityUnit.BAG -> if (lang == AppLanguage.TELUGU) "బస్తాలు" else "Bags"
            QuantityUnit.TON -> if (lang == AppLanguage.TELUGU) "టన్నులు" else "Tonnes"
            QuantityUnit.LITRE -> if (lang == AppLanguage.TELUGU) "లీటర్లు" else "Litre"
            QuantityUnit.ML -> if (lang == AppLanguage.TELUGU) "మి.లీ." else "ml"
            QuantityUnit.PIECE -> if (lang == AppLanguage.TELUGU) "నగలు" else "Pieces"
            QuantityUnit.HOUR -> if (lang == AppLanguage.TELUGU) "గంటలు" else "Hours"
            QuantityUnit.ACRE -> if (lang == AppLanguage.TELUGU) "ఎకరాలు" else "Acres"
        }

    fun areaUnit(
        unit: AreaUnit,
        lang: AppLanguage,
    ): String =
        when (unit) {
            AreaUnit.ACRE_GUNTA -> if (lang == AppLanguage.TELUGU) "ఎకరాలు + గుంటలు (40)" else "Acres + Guntas (40)"
            AreaUnit.ACRE_CENT -> if (lang == AppLanguage.TELUGU) "ఎకరాలు + సెంట్లు (100)" else "Acres + Cents (100)"
            AreaUnit.ACRE_DECIMAL -> if (lang == AppLanguage.TELUGU) "ఎకరాలు (దశాంశం)" else "Acres (Decimal)"
        }

    fun ledgerCategory(
        cat: LedgerCategory,
        lang: AppLanguage,
    ): String = pick(lang, ledgerCategoryLabels.getValue(cat))

    fun paymentStatus(
        status: PaymentStatus,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (status) {
            PaymentStatus.PAID -> if (te) "పూర్తి" else "Paid"
            PaymentStatus.PARTIAL -> if (te) "సగం అందింది" else "Partial"
            PaymentStatus.PENDING -> if (te) "బాకీ ఉంది" else "Pending"
        }
    }

    fun paymentMode(
        mode: PaymentMode,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (mode) {
            PaymentMode.CASH -> if (te) "నగదు" else "Cash"
            PaymentMode.UPI -> if (te) "యూపీఐ / ఫోన్‌పే" else "UPI"
            PaymentMode.BANK_TRANSFER -> if (te) "బ్యాంకు ఖాతా" else "Bank"
            PaymentMode.CREDIT -> if (te) "అప్పు / ఖాతా" else "Credit"
            PaymentMode.CHEQUE -> if (te) "చెక్కు" else "Cheque"
            PaymentMode.OTHER -> if (te) "ఇతర" else "Other"
        }
    }

    fun season(
        season: SeasonType,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (season) {
            SeasonType.KHARIF -> if (te) "వానాకాలం (ఖరీఫ్)" else "Kharif"
            SeasonType.RABI -> if (te) "యాసంగి (రబీ)" else "Rabi"
            SeasonType.ZAID -> if (te) "ఎండాకాలం" else "Summer / Zaid"
            SeasonType.PERENNIAL -> if (te) "బహువార్షిక పంట" else "Perennial"
        }
    }

    fun cropCycleStatus(
        status: CropCycleStatus,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (status) {
            CropCycleStatus.PLANNED -> if (te) "ప్రణాళిక" else "Planned"
            CropCycleStatus.ACTIVE -> if (te) "పెరుగుతోంది" else "Active"
            CropCycleStatus.HARVESTED -> if (te) "కోత అయింది" else "Harvested"
            CropCycleStatus.CLOSED -> if (te) "ముగిసింది" else "Closed"
            CropCycleStatus.FAILED -> if (te) "దెబ్బతిన్నది" else "Failed"
        }
    }

    fun taskCategory(
        category: TaskCategory,
        lang: AppLanguage,
    ): String = pick(lang, taskCategoryLabels.getValue(category))

    fun taskStatus(
        status: TaskStatus,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (status) {
            TaskStatus.PENDING -> if (te) "చేయాలి" else "Pending"
            TaskStatus.COMPLETED -> if (te) "పూర్తయింది" else "Done"
            TaskStatus.RESCHEDULED -> if (te) "తేదీ మారింది" else "Rescheduled"
            TaskStatus.MISSED -> if (te) "తప్పింది" else "Missed"
            TaskStatus.CANCELLED -> if (te) "రద్దు" else "Cancelled"
        }
    }

    fun diaryCategory(
        cat: DiaryCategory,
        lang: AppLanguage,
    ): String = pick(lang, diaryCategoryLabels.getValue(cat))

    fun attendanceStatus(
        status: AttendanceStatus,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (status) {
            AttendanceStatus.FULL_DAY -> if (te) "పూర్తి రోజు" else "Full Day"
            AttendanceStatus.HALF_DAY -> if (te) "సగం రోజు" else "Half Day"
            AttendanceStatus.ABSENT -> if (te) "రాలేదు" else "Absent"
            AttendanceStatus.PIECE_RATE -> if (te) "గుత్త పని" else "Piece Rate"
        }
    }

    fun workerPaymentType(
        type: WorkerPaymentType,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (type) {
            WorkerPaymentType.ADVANCE -> if (te) "అడ్వాన్స్" else "Advance"
            WorkerPaymentType.SETTLEMENT -> if (te) "కూలీ చెల్లింపు" else "Wage Payout"
        }
    }

    fun machineryUsageType(
        type: MachineryUsageType,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (type) {
            MachineryUsageType.HIRED_IN -> if (te) "అద్దెకు తీసుకున్నాము" else "Hired In"
            MachineryUsageType.OWN_FARM_USE -> if (te) "సొంత యంత్రం" else "Own Use"
            MachineryUsageType.HIRED_OUT -> if (te) "బయటకు అద్దెకు ఇచ్చాము" else "Hired Out"
        }
    }

    fun machineryBillingMode(
        mode: MachineryBillingMode,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (mode) {
            MachineryBillingMode.HOURLY -> if (te) "గంట చొప్పున" else "Hourly"
            MachineryBillingMode.PER_ACRE -> if (te) "ఎకరా చొప్పున" else "Per Acre"
            MachineryBillingMode.FIXED_FLAT -> if (te) "మొత్తం గుత్తగా" else "Fixed Flat"
        }
    }

    fun inputCategory(
        cat: InputCategory,
        lang: AppLanguage,
    ): String = pick(lang, inputCategoryLabels.getValue(cat))

    fun irrigationSource(
        src: IrrigationSource,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (src) {
            IrrigationSource.BOREWELL -> if (te) "బోరు బావి" else "Borewell"
            IrrigationSource.CANAL -> if (te) "కాలువ" else "Canal"
            IrrigationSource.OPEN_WELL -> if (te) "బావి" else "Open Well"
            IrrigationSource.RAINFED -> if (te) "వర్షాధారం" else "Rainfed"
            IrrigationSource.DRIP -> if (te) "డ్రిప్" else "Drip"
            IrrigationSource.TANK -> if (te) "చెరువు" else "Tank"
            IrrigationSource.OTHER -> if (te) "ఇతర" else "Other"
        }
    }

    fun irrigationMethod(
        method: IrrigationMethod,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (method) {
            IrrigationMethod.FLOOD -> if (te) "కట్టునీరు" else "Flood"
            IrrigationMethod.DRIP -> if (te) "డ్రిప్" else "Drip"
            IrrigationMethod.SPRINKLER -> if (te) "స్ప్రింక్లర్" else "Sprinkler"
            IrrigationMethod.FURROW -> if (te) "బోదెలు" else "Furrow"
            IrrigationMethod.OTHER -> if (te) "ఇతర" else "Other"
        }
    }

    fun storageStatus(
        status: StorageStatus,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (status) {
            StorageStatus.IN_STORAGE -> if (te) "నిల్వలో ఉంది" else "In Storage"
            StorageStatus.PARTIALLY_DISPATCHED -> if (te) "కొంత అమ్మాము" else "Partial Sold"
            StorageStatus.CLEARED -> if (te) "అంతా అయిపోయింది" else "Cleared"
        }
    }

    fun soilHealthRating(
        rating: SoilHealthRating,
        lang: AppLanguage,
    ): String {
        val te = lang == AppLanguage.TELUGU
        return when (rating) {
            SoilHealthRating.LOW -> if (te) "తక్కువ" else "Low"
            SoilHealthRating.MEDIUM -> if (te) "మధ్యస్థం" else "Medium"
            SoilHealthRating.OPTIMAL -> if (te) "సరిపడా / మంచిది" else "Optimal"
            SoilHealthRating.HIGH -> if (te) "ఎక్కువ" else "High"
            SoilHealthRating.SALINE_ALKALINE -> if (te) "చౌడు / ఉప్పు నేల" else "Saline / Alkaline"
        }
    }

    fun contactType(
        type: ContactType,
        lang: AppLanguage,
    ): String = pick(lang, contactTypeLabels.getValue(type))

    fun stockMovementReasonLabel(
        reason: StockMovementReason,
        isTelugu: Boolean,
    ): String =
        when (reason) {
            StockMovementReason.PURCHASE -> if (isTelugu) "కొనుగోలు" else "Purchase"
            StockMovementReason.OPENING_STOCK -> if (isTelugu) "ప్రారంభ నిల్వ" else "Opening Stock"
            StockMovementReason.FIELD_APPLICATION -> if (isTelugu) "పొలంలో వాడకం" else "Field Application"
            StockMovementReason.RETURN -> if (isTelugu) "వాపసు" else "Return"
            StockMovementReason.WASTAGE_EXPIRED -> if (isTelugu) "వృధా / గడువు ముగిసింది" else "Wastage / Expired"
            StockMovementReason.STOCK_COUNT_CORRECTION -> if (isTelugu) "నిల్వ సవరణ" else "Stock Correction"
        }

    fun searchHitTypeLabel(
        rawType: String,
        isTelugu: Boolean,
    ): String =
        when (rawType.trim().uppercase()) {
            "FARM" -> if (isTelugu) "పొలం" else "Farm"
            "PLOT" -> if (isTelugu) "మడి" else "Plot"
            "CROP" -> if (isTelugu) "పంట" else "Crop"
            "DIARY" -> if (isTelugu) "డైరీ" else "Diary"
            "LEDGER" -> if (isTelugu) "లెక్క" else "Ledger"
            "WORKER" -> if (isTelugu) "కూలీ" else "Worker"
            "CONTACT" -> if (isTelugu) "పరిచయం" else "Contact"
            "INVENTORY" -> if (isTelugu) "సరుకు నిల్వ" else "Inventory"
            else -> rawType
        }
}
