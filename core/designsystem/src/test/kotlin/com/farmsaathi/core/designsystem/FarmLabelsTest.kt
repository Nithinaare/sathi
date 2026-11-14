package com.farmsaathi.core.designsystem

import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AreaConverter
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.ContactType
import com.farmsaathi.core.model.DiaryCategory
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.core.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmLabelsTest {
    @Test
    fun `area and quantity formatting supports telugu and english cleanly`() {
        val micros = AreaConverter.fromAcresAndGuntas(2, 20.0)
        assertEquals("2 ఎక. 20 గుం.", FarmLabels.formatArea(micros, AreaUnit.ACRE_GUNTA, AppLanguage.TELUGU))
        assertEquals("2 Ac 20 Gu", FarmLabels.formatArea(micros, AreaUnit.ACRE_GUNTA, AppLanguage.ENGLISH))
        assertEquals("2 Ac 50 Ct", FarmLabels.formatArea(micros, AreaUnit.ACRE_CENT, AppLanguage.ENGLISH))
        assertEquals("12.5 క్వింటాళ్ళు", FarmLabels.formatQuantity(12_500L, QuantityUnit.QUINTAL, AppLanguage.TELUGU))
    }

    @Test
    fun `all ledger and contact categories produce non-empty labels in both languages`() {
        LedgerCategory.entries.forEach { cat ->
            assertTrue(FarmLabels.ledgerCategory(cat, AppLanguage.TELUGU).isNotBlank())
            assertTrue(FarmLabels.ledgerCategory(cat, AppLanguage.ENGLISH).isNotBlank())
        }
        ContactType.entries.forEach { ct ->
            assertTrue(FarmLabels.contactType(ct, AppLanguage.TELUGU).isNotBlank())
            assertTrue(FarmLabels.contactType(ct, AppLanguage.ENGLISH).isNotBlank())
        }
    }

    @Test
    fun `stock movement reasons and search hit types localize without raw enum leaks`() {
        StockMovementReason.entries.forEach { reason ->
            val te = FarmLabels.stockMovementReasonLabel(reason, isTelugu = true)
            val en = FarmLabels.stockMovementReasonLabel(reason, isTelugu = false)
            assertTrue(te.isNotBlank())
            assertTrue(en.isNotBlank())
            assertNotEquals(reason.name, te)
            assertNotEquals(reason.name, en)
        }

        listOf("FARM", "PLOT", "CROP", "DIARY", "LEDGER", "WORKER", "CONTACT", "INVENTORY").forEach { raw ->
            val te = FarmLabels.searchHitTypeLabel(raw, isTelugu = true)
            val en = FarmLabels.searchHitTypeLabel(raw, isTelugu = false)
            assertTrue(te.isNotBlank())
            assertTrue(en.isNotBlank())
            assertNotEquals(raw, te)
            assertNotEquals(raw, en)
        }
    }

    @Test
    fun `all new webp assets are wired into FarmImages`() {
        assertEquals(R.drawable.img_reports, FarmImages.reports)
        assertEquals(R.drawable.img_seeds, FarmImages.forLedgerCategory(LedgerCategory.SEEDS))
        assertEquals(R.drawable.img_fertilizer, FarmImages.forLedgerCategory(LedgerCategory.FERTILIZERS))
        assertEquals(R.drawable.img_pesticide, FarmImages.forLedgerCategory(LedgerCategory.PESTICIDES))
        assertEquals(R.drawable.img_diesel, FarmImages.forLedgerCategory(LedgerCategory.DIESEL))

        assertEquals(R.drawable.img_fertilizer, FarmImages.forTaskCategory(TaskCategory.FERTILIZER))
        assertEquals(R.drawable.img_pesticide, FarmImages.forTaskCategory(TaskCategory.SPRAYING))

        assertEquals(R.drawable.img_fertilizer, FarmImages.forDiaryCategory(DiaryCategory.FERTILIZER_SPRAY))
        assertEquals(R.drawable.img_pesticide, FarmImages.forDiaryCategory(DiaryCategory.PEST_OBSERVATION))

        assertEquals(R.drawable.img_seeds, FarmImages.forInputCategory(InputCategory.SEED))
        assertEquals(R.drawable.img_fertilizer, FarmImages.forInputCategory(InputCategory.FERTILIZER))
        assertEquals(R.drawable.img_fertilizer, FarmImages.forInputCategory(InputCategory.MICRONUTRIENT))
        assertEquals(R.drawable.img_pesticide, FarmImages.forInputCategory(InputCategory.PESTICIDE))
        assertEquals(R.drawable.img_pesticide, FarmImages.forInputCategory(InputCategory.HERBICIDE))
        assertEquals(R.drawable.img_pesticide, FarmImages.forInputCategory(InputCategory.FUNGICIDE))
        assertEquals(R.drawable.img_inputs, FarmImages.forInputCategory(InputCategory.OTHER))

        assertEquals(R.drawable.img_seeds, FarmImages.forContactType(ContactType.SEED_SUPPLIER))
        assertEquals(R.drawable.img_fertilizer, FarmImages.forContactType(ContactType.FERTILIZER_SHOP))
        assertEquals(R.drawable.img_pesticide, FarmImages.forContactType(ContactType.PESTICIDE_SHOP))
    }
}
