package com.farmsaathi.core.common

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.common.navigation.FarmRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class CoreCommonContractsTest {
    @Test
    fun `formatTime uses non-breaking space before AM PM`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = AndroidTimeProvider(context)
        val formatted = provider.formatTime(1_794_326_000_000L)
        assertTrue("Expected non-breaking space before AM/PM in '$formatted'", formatted.contains('\u00A0'))
    }

    @Test
    fun `formatEpochDay and formatDate cache formatters consistently`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = AndroidTimeProvider(context)
        val epochDay = LocalDate.of(2026, 11, 12).toEpochDay()

        assertEquals("12 Nov 2026", provider.formatEpochDay(epochDay, useTelugu = false))
        assertEquals("12 నవంబర్ 2026", provider.formatEpochDay(epochDay, useTelugu = true))

        val millis = provider.epochDayToStartMillis(epochDay)
        assertEquals("12 Nov 2026", provider.formatDate(millis, useTelugu = false))
        assertEquals(provider.formatTime(millis), provider.formatTime(millis))
    }

    @Test
    fun `bootCount caches initial Settings Global read per AndroidTimeProvider instance`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, 7)

        val provider = AndroidTimeProvider(context)
        assertEquals(7, provider.bootCount())

        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, 99)
        assertEquals(7, provider.bootCount())

        val freshProvider = AndroidTimeProvider(context)
        assertEquals(99, freshProvider.bootCount())
    }

    @Test
    fun `audio cue catalog contains all expected screen and form cue keys`() {
        val requiredKeys =
            listOf(
                AudioCueCatalog.PLOTS_HELP,
                AudioCueCatalog.LABOUR_HELP,
                AudioCueCatalog.IRRIGATION_HELP,
                AudioCueCatalog.EXPENSE_FORM_HELP,
                AudioCueCatalog.SALE_FORM_HELP,
                AudioCueCatalog.WATERING_FORM_HELP,
                AudioCueCatalog.CROP_FORM_HELP,
                AudioCueCatalog.HARVEST_FORM_HELP,
                AudioCueCatalog.SOIL_FORM_HELP,
                AudioCueCatalog.DIARY_FORM_HELP,
                AudioCueCatalog.SPRAY_FORM_HELP,
                AudioCueCatalog.TRACTOR_FORM_HELP,
                AudioCueCatalog.WORKER_FORM_HELP,
                AudioCueCatalog.CONTACT_FORM_HELP,
                AudioCueCatalog.LEDGER_DETAIL_HELP,
            )
        for (key in requiredKeys) {
            val script = AudioCueCatalog.find(key)
            assertNotNull("Missing cue script for $key", script)
            assertTrue("Empty Telugu script for $key", script!!.scriptTe.isNotBlank())
            assertTrue("Empty English script for $key", script.scriptEn.isNotBlank())
        }
    }

    @Test
    fun `navigation route optional defaults preserve compatibility`() {
        val cropDirect = FarmRoute.CropForm(farmId = 7L)
        assertNull(cropDirect.plotId)
        assertEquals(7L, cropDirect.farmId)

        val sprayFromSnap =
            FarmRoute.InventoryUsageForm(
                plotId = 3L,
                cropCycleId = 9L,
                initialPhotoPath = "media/photos/spray.jpg",
                initialDateMillis = 1234L,
            )
        assertEquals(0L, sprayFromSnap.productId)
        assertEquals("media/photos/spray.jpg", sprayFromSnap.initialPhotoPath)

        val harvest = FarmRoute.HarvestForm(cropCycleId = 5L, initialPhotoPath = "slip.jpg", initialDateMillis = 999L)
        assertEquals("slip.jpg", harvest.initialPhotoPath)

        val soil = FarmRoute.SoilTestForm(plotId = 4L, initialPhotoPath = "soil.jpg", initialDateMillis = 888L)
        assertEquals("soil.jpg", soil.initialPhotoPath)
    }
}
