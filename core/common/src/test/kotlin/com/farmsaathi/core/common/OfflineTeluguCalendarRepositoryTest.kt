package com.farmsaathi.core.common

import com.farmsaathi.core.common.repository.OfflineTeluguCalendarRepositoryImpl
import com.farmsaathi.core.model.LunarAlertType
import com.farmsaathi.core.model.Paksham
import com.farmsaathi.core.model.TeluguMonth
import com.farmsaathi.core.model.TeluguTithi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class OfflineTeluguCalendarRepositoryTest {

    private lateinit var repository: OfflineTeluguCalendarRepositoryImpl

    @Before
    fun setUp() {
        repository = OfflineTeluguCalendarRepositoryImpl()
    }

    @Test
    fun getDay_returnsAccurateTeluguDateAndPanchang() = runTest {
        // Ugadi 2025: March 30, 2025 (epochDay = 20177)
        val epochDay = LocalDate.of(2025, 3, 30).toEpochDay()
        val day = repository.getDay(epochDay)

        assertNotNull(day)
        assertEquals("2025-03-30", day?.gregorianDate)
        assertEquals("విశ్వావసు", day?.teluguYear)
        assertEquals(TeluguMonth.CHAITRAM, day?.teluguMonth)
        assertEquals(Paksham.SHUKLA, day?.paksham)
        assertEquals(TeluguTithi.PADYAMI, day?.tithi)
        assertTrue(day?.isFestival == true)
        assertEquals("ఉగాది", day?.festivalName)
    }

    @Test
    fun getMonthDays_returnsCompleteDaysForMonth() = runTest {
        val days = repository.getMonthDays(2026, 6)
        assertEquals(30, days.size)
        assertEquals("2026-06-01", days.first().gregorianDate)
        assertEquals("2026-06-30", days.last().gregorianDate)
    }

    @Test
    fun getCurrentKarte_identifiesCorrectKarteAndProgress() = runTest {
        // June 10, 2026 is inside Mrigashira Karte (starts June 8, day 3)
        val epochDay = LocalDate.of(2026, 6, 10).toEpochDay()
        val karteInfo = repository.getCurrentKarte(epochDay)

        assertEquals("మృగశిర", karteInfo.teluguName)
        assertEquals(3, karteInfo.dayNumber)
        assertTrue(karteInfo.daysRemaining > 0)
        assertTrue(karteInfo.recommendedCrops.contains("పత్తి"))
    }

    @Test
    fun getTodayAlert_triggersAmavasyaAlertOnAmavasya() = runTest {
        // Chaitra Amavasya 2025 (pure Amavasya, non-festival): April 27, 2025 (epochDay = 20205)
        val amavasyaEpoch = LocalDate.of(2025, 4, 27).toEpochDay()
        val alert = repository.getTodayAlert(amavasyaEpoch)

        assertEquals(LunarAlertType.AMAVASYA, alert.alertType)
        assertTrue(alert.title.contains("అమావాస్య: ఎడ్లకు విశ్రాంతి"))
        assertTrue(alert.isPriority)
    }

    @Test
    fun getTodayAlert_triggersFestivalAlertOnPolalaAmavasya() = runTest {
        // Polala Amavasya: Shravana Amavasya 2025 - August 23, 2025 (epochDay = 20323)
        val polalaEpoch = 20323L
        val alert = repository.getTodayAlert(polalaEpoch)

        assertEquals(LunarAlertType.FESTIVAL, alert.alertType)
        assertTrue(alert.title.contains("పోలాల అమావాస్య"))
        assertTrue(alert.title.contains("పశువుల కృతజ్ఞతా దినం"))
        assertTrue(alert.isPriority)
    }

    @Test
    fun getTodayAlert_triggersFestivalAlertOnEruvakaPournami() = runTest {
        // Eruvaka Pournami 2025: Jyeshtha Pournami - June 11, 2025 (epochDay = 20250)
        val eruvakaEpoch = LocalDate.of(2025, 6, 11).toEpochDay()
        val alert = repository.getTodayAlert(eruvakaEpoch)

        assertEquals(LunarAlertType.FESTIVAL, alert.alertType)
        assertTrue(alert.title.contains("ఏరువాక పౌర్ణమి"))
        assertTrue(alert.title.contains("వానాకాలం ప్రారంభం"))
        assertTrue(alert.isPriority)
    }

    @Test
    fun getTodayAlert_triggersKarteTransitionAlertOnDay1() = runTest {
        // Mrigashira starts June 8 every year: June 8, 2025 (day 1 of karte)
        val karteDay1Epoch = LocalDate.of(2025, 6, 8).toEpochDay()
        val alert = repository.getTodayAlert(karteDay1Epoch)

        assertEquals(LunarAlertType.KARTE_TRANSITION, alert.alertType)
        assertTrue(alert.title.contains("కార్తె ప్రారంభం"))
        assertTrue(alert.isPriority)
    }

    @Test
    fun getTodayAlert_triggersPostAmavasyaSprayWindow() = runTest {
        // 4 days after Aug 23, 2025 is Aug 27, 2025 (epochDay = 20327)
        val postAmavasyaEpoch = 20327L
        val alert = repository.getTodayAlert(postAmavasyaEpoch)

        assertEquals(LunarAlertType.POST_AMAVASYA_SPRAY, alert.alertType)
        assertTrue(alert.title.contains("పురుగుల నివారణ"))
        assertTrue(alert.subtitle.contains("వేప కషాయం"))
        assertTrue(alert.isPriority)
    }

    @Test
    fun getTodayAlert_standardDayProducesCleanInformativeAlert() = runTest {
        // A regular day in Ashadha (e.g., July 8, 2025 - Ashadha Shukla Trayodashi)
        val epochDay = LocalDate.of(2025, 7, 8).toEpochDay()
        val alert = repository.getTodayAlert(epochDay)

        assertEquals(LunarAlertType.DAY_STANDARD, alert.alertType)
        assertFalse(alert.isPriority)
        assertTrue(alert.ttsSpeechText.isNotEmpty())
    }

    @Test
    fun getTodayAlert_triggersPournamiAlertOnPournami() = runTest {
        // Ashadha Pournami (Guru Pournami): July 10, 2025 (epochDay = 20279)
        val epochDay = LocalDate.of(2025, 7, 10).toEpochDay()
        val alert = repository.getTodayAlert(epochDay)

        assertEquals(LunarAlertType.POURNAMI, alert.alertType)
        assertFalse(alert.isPriority)
        assertTrue(alert.title.contains("పౌర్ణమి"))
        assertTrue(alert.ttsSpeechText.isNotEmpty())
    }

    @Test
    fun observeAllKartis_returnsAll27Kartis() = runTest {
        val kartis = repository.observeAllKartis()
        // Collect first emission
        var count = 0
        kartis.collect { list ->
            count = list.size
            assertEquals(27, list.size)
            assertEquals("అశ్విని", list.first().teluguName)
            assertEquals("రేవతి", list.last().teluguName)
        }
        assertEquals(27, count)
    }
}
