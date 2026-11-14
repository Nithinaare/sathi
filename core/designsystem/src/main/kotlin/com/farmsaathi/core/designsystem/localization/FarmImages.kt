package com.farmsaathi.core.designsystem.localization

import androidx.annotation.DrawableRes
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.model.ContactType
import com.farmsaathi.core.model.DiaryCategory
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.core.model.IrrigationSource
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.TaskCategory

/**
 * Centralized registry of bundled real agricultural photo thumbnails (DRY).
 */
object FarmImages {
    @get:DrawableRes
    val farmField: Int
        get() = R.drawable.img_farm_field

    @get:DrawableRes
    val sunriseWelcome: Int
        get() = R.drawable.img_sunrise_welcome

    @get:DrawableRes
    val expense: Int
        get() = R.drawable.img_expense

    @get:DrawableRes
    val income: Int
        get() = R.drawable.img_income

    @get:DrawableRes
    val farmWork: Int
        get() = R.drawable.img_farm_work

    @get:DrawableRes
    val diary: Int
        get() = R.drawable.img_diary

    @get:DrawableRes
    val labour: Int
        get() = R.drawable.img_labour

    @get:DrawableRes
    val tractor: Int
        get() = R.drawable.img_tractor

    @get:DrawableRes
    val inputs: Int
        get() = R.drawable.img_inputs

    @get:DrawableRes
    val irrigation: Int
        get() = R.drawable.img_irrigation

    @get:DrawableRes
    val harvest: Int
        get() = R.drawable.img_harvest

    @get:DrawableRes
    val soilTest: Int
        get() = R.drawable.img_soil_test

    @get:DrawableRes
    val contacts: Int
        get() = R.drawable.img_contacts

    @get:DrawableRes
    val snapCamera: Int
        get() = R.drawable.img_snap_camera

    @get:DrawableRes
    val wageCalc: Int
        get() = R.drawable.img_wage_calc

    @get:DrawableRes
    val extrasShed: Int
        get() = R.drawable.img_extras_shed

    @get:DrawableRes
    val reports: Int
        get() = R.drawable.img_reports

    @get:DrawableRes
    val seeds: Int
        get() = R.drawable.img_seeds

    @get:DrawableRes
    val fertilizer: Int
        get() = R.drawable.img_fertilizer

    @get:DrawableRes
    val pesticide: Int
        get() = R.drawable.img_pesticide

    @get:DrawableRes
    val diesel: Int
        get() = R.drawable.img_diesel

    @get:DrawableRes
    val cropPaddy: Int
        get() = R.drawable.img_crop_paddy

    @get:DrawableRes
    val cropGeneric: Int
        get() = R.drawable.img_crop_generic

    private val cropKeywordDrawables = listOf(
        listOf("paddy", "rice", "వరి") to R.drawable.img_crop_paddy,
        listOf("cotton", "పత్తి") to R.drawable.img_crop_cotton,
        listOf("chilli", "chili", "మిర్చి") to R.drawable.img_crop_chilli,
        listOf("maize", "corn", "మక్క", "మొక్కజొన్న") to R.drawable.img_crop_maize,
        listOf("groundnut", "peanut", "వేరుశ") to R.drawable.img_crop_groundnut,
    )

    @DrawableRes
    fun forCropName(name: String): Int {
        val lower = name.lowercase()
        return cropKeywordDrawables
            .firstOrNull { (keywords, _) -> keywords.any { lower.contains(it) } }
            ?.second
            ?: R.drawable.img_crop_generic
    }

    @DrawableRes
    fun forLedgerCategory(category: LedgerCategory): Int =
        when (category) {
            LedgerCategory.SEEDS -> R.drawable.img_seeds
            LedgerCategory.FERTILIZERS -> R.drawable.img_fertilizer
            LedgerCategory.PESTICIDES -> R.drawable.img_pesticide
            LedgerCategory.DIESEL -> R.drawable.img_diesel
            LedgerCategory.LABOUR -> R.drawable.img_labour
            LedgerCategory.MACHINERY,
            LedgerCategory.MACHINERY_HIRE_INCOME,
            -> R.drawable.img_tractor
            LedgerCategory.IRRIGATION -> R.drawable.img_irrigation
            LedgerCategory.TRANSPORT,
            LedgerCategory.REPAIRS,
            LedgerCategory.OTHER_EXPENSE,
            -> R.drawable.img_expense
            LedgerCategory.CROP_SALE,
            LedgerCategory.OTHER_INCOME,
            -> R.drawable.img_income
        }

    @DrawableRes
    fun forTaskCategory(category: TaskCategory): Int =
        when (category) {
            TaskCategory.LAND_PREP -> R.drawable.img_tractor
            TaskCategory.SOWING,
            TaskCategory.WEEDING,
            TaskCategory.OTHER,
            -> R.drawable.img_farm_work
            TaskCategory.FERTILIZER -> R.drawable.img_fertilizer
            TaskCategory.SPRAYING -> R.drawable.img_pesticide
            TaskCategory.IRRIGATION -> R.drawable.img_irrigation
            TaskCategory.HARVEST -> R.drawable.img_harvest
        }

    @DrawableRes
    fun forDiaryCategory(category: DiaryCategory): Int =
        when (category) {
            DiaryCategory.CROP_GROWTH -> R.drawable.img_farm_work
            DiaryCategory.PEST_OBSERVATION -> R.drawable.img_pesticide
            DiaryCategory.FERTILIZER_SPRAY -> R.drawable.img_fertilizer
            DiaryCategory.IRRIGATION -> R.drawable.img_irrigation
            DiaryCategory.HARVEST -> R.drawable.img_harvest
            DiaryCategory.WEATHER_DAMAGE,
            DiaryCategory.OTHER,
            -> R.drawable.img_diary
        }

    @DrawableRes
    fun forInputCategory(category: InputCategory): Int =
        when (category) {
            InputCategory.ORGANIC -> R.drawable.img_soil_test
            InputCategory.SEED -> R.drawable.img_seeds
            InputCategory.FERTILIZER,
            InputCategory.MICRONUTRIENT,
            -> R.drawable.img_fertilizer
            InputCategory.PESTICIDE,
            InputCategory.HERBICIDE,
            InputCategory.FUNGICIDE,
            -> R.drawable.img_pesticide
            InputCategory.OTHER -> R.drawable.img_inputs
        }

    @DrawableRes
    fun forIrrigationSource(source: IrrigationSource): Int =
        when (source) {
            IrrigationSource.BOREWELL,
            IrrigationSource.OPEN_WELL,
            IrrigationSource.DRIP,
            IrrigationSource.TANK,
            -> R.drawable.img_irrigation
            IrrigationSource.CANAL,
            IrrigationSource.RAINFED,
            IrrigationSource.OTHER,
            -> R.drawable.img_farm_field
        }

    @DrawableRes
    fun forContactType(type: ContactType): Int =
        when (type) {
            ContactType.TRACTOR_OWNER,
            ContactType.HARVESTER_OWNER,
            ContactType.TRANSPORT_PROVIDER,
            ContactType.MECHANIC,
            -> R.drawable.img_tractor
            ContactType.LABOUR_CONTRACTOR -> R.drawable.img_labour
            ContactType.SEED_SUPPLIER -> R.drawable.img_seeds
            ContactType.FERTILIZER_SHOP -> R.drawable.img_fertilizer
            ContactType.PESTICIDE_SHOP -> R.drawable.img_pesticide
            ContactType.BUYER_TRADER -> R.drawable.img_income
            ContactType.AGRICULTURE_OFFICER,
            ContactType.OTHER,
            -> R.drawable.img_contacts
        }
}
