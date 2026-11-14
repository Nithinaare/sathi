package com.farmsaathi.core.common

import com.farmsaathi.core.common.repository.OfflineTeluguCalendarRepositoryImpl
import com.farmsaathi.core.model.LunarAlertType
import com.farmsaathi.core.model.TeluguMonth
import com.farmsaathi.core.model.TeluguTithi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class Milestone1ChallengerStressTest {

    private lateinit var repository: OfflineTeluguCalendarRepositoryImpl

    @Before
    fun setUp() {
        repository = OfflineTeluguCalendarRepositoryImpl()
    }

    // =========================================================================
    // 1. ALERT TYPE VERIFICATION & DEFECT INVESTIGATION
    // =========================================================================

    @Test
    fun testAlert_pureAmavasya() = runTest {
        // 2025-04-27 is Chaitra Amavasya (not an agro festival)
        val epochDay = LocalDate.of(2025, 4, 27).toEpochDay()
        val day = repository.getDay(epochDay)
        assertNotNull(day)
        assertTrue(day!!.isAmavasya)
        assertFalse(day.isFestival)

        val alert = repository.getTodayAlert(epochDay)
        assertEquals(LunarAlertType.AMAVASYA, alert.alertType)
        assertTrue(alert.title.contains("అమావాస్య"))
        assertTrue(alert.isPriority)
    }

    @Test
    fun testAlert_postAmavasyaSprayWindow_allThreeDays() = runTest {
        // Chaitra Amavasya is 2025-04-27 (epochDay 20205)
        // Day +3: 2025-04-30 (epochDay 20208)
        // Day +4: 2025-05-01 (epochDay 20209)
        // Day +5: 2025-05-02 (epochDay 20210)
        for (offset in 3..5) {
            val epochDay = 20205L + offset
            val alert = repository.getTodayAlert(epochDay)
            assertEquals("Day +$offset should be POST_AMAVASYA_SPRAY", LunarAlertType.POST_AMAVASYA_SPRAY, alert.alertType)
            assertTrue(alert.isPriority)
            assertTrue(alert.subtitle.contains("వేప కషాయం"))
        }

        // Day +2 (2025-04-29) should NOT be spray window
        val day2Alert = repository.getTodayAlert(20205L + 2)
        assertFalse(day2Alert.alertType == LunarAlertType.POST_AMAVASYA_SPRAY)

        // Day +6 (2025-05-03) should NOT be spray window
        val day6Alert = repository.getTodayAlert(20205L + 6)
        assertFalse(day6Alert.alertType == LunarAlertType.POST_AMAVASYA_SPRAY)
    }

    @Test
    fun testAlert_pournamiAlert() = runTest {
        // Ashadha Pournami: July 10, 2025
        val epochDay = LocalDate.of(2025, 7, 10).toEpochDay()
        val day = repository.getDay(epochDay)
        assertNotNull(day)
        assertTrue(day!!.isPournami)

        val alert = repository.getTodayAlert(epochDay)
        assertEquals(LunarAlertType.POURNAMI, alert.alertType)
        assertTrue(alert.title.contains("పౌర్ణమి"))
    }

    @Test
    fun testAlert_standardDayAlert() = runTest {
        // Regular day: 2025-07-08
        val epochDay = LocalDate.of(2025, 7, 8).toEpochDay()
        val alert = repository.getTodayAlert(epochDay)
        assertEquals(LunarAlertType.DAY_STANDARD, alert.alertType)
        assertFalse(alert.isPriority)
        assertTrue(alert.ttsSpeechText.isNotEmpty())
    }

    @Test
    fun testAlert_karteTransitionDay() = runTest {
        // Mrigashira starts June 8 every year: 2025-06-08 (day 1 of karte)
        val epochDay = LocalDate.of(2025, 6, 8).toEpochDay()
        val karteInfo = repository.getCurrentKarte(epochDay)
        assertEquals(1, karteInfo.dayNumber)

        val alert = repository.getTodayAlert(epochDay)
        assertEquals(LunarAlertType.KARTE_TRANSITION, alert.alertType)
        assertTrue(alert.title.contains("కార్తె ప్రారంభం"))
        assertTrue(alert.isPriority)
    }

    @Test
    fun testAlert_agriculturalFestivals() = runTest {
        // Eruvaka Pournami: Jyeshtha Pournami (2025-06-11)
        val eruvakaDay = LocalDate.of(2025, 6, 11).toEpochDay()
        val eruvaka = repository.getDay(eruvakaDay)
        assertNotNull(eruvaka)
        assertEquals("ఏరువాక పౌర్ణమి", eruvaka!!.festivalName)
        val eruvakaAlert = repository.getTodayAlert(eruvakaDay)
        assertEquals(LunarAlertType.FESTIVAL, eruvakaAlert.alertType)
        assertTrue(eruvakaAlert.title.contains("ఏరువాక పౌర్ణమి"))

        // Kanuma: Jan 16, 2026
        val kanumaDay = LocalDate.of(2026, 1, 16).toEpochDay()
        val kanuma = repository.getDay(kanumaDay)
        assertNotNull(kanuma)
        assertEquals("కనుమ", kanuma!!.festivalName)
        val kanumaAlert = repository.getTodayAlert(kanumaDay)
        assertEquals(LunarAlertType.FESTIVAL, kanumaAlert.alertType)
        assertTrue(kanumaAlert.title.contains("కనుమ"))

        // Ugadi: March 30, 2025
        val ugadiDay = LocalDate.of(2025, 3, 30).toEpochDay()
        val ugadi = repository.getDay(ugadiDay)
        assertNotNull(ugadi)
        assertEquals("ఉగాది", ugadi!!.festivalName)
        val ugadiAlert = repository.getTodayAlert(ugadiDay)
        assertEquals(LunarAlertType.FESTIVAL, ugadiAlert.alertType)
        assertTrue(ugadiAlert.title.contains("ఉగాది"))
    }

    @Test
    fun testAlert_polalaAmavasyaBugInvestigation() = runTest {
        // Polala Amavasya: Shravana Amavasya 2025 (August 23, 2025, epochDay = 20323)
        val polalaDay = LocalDate.of(2025, 8, 23).toEpochDay()
        val day = repository.getDay(polalaDay)
        assertNotNull(day)
        assertEquals(TeluguMonth.SHRAVANAM, day!!.teluguMonth)
        assertEquals(TeluguTithi.AMAVASYA, day.tithi)
        assertEquals("పోలాల అమావాస్య", day.festivalName)
        assertTrue(day.isFestival)

        val alert = repository.getTodayAlert(polalaDay)
        assertEquals(LunarAlertType.FESTIVAL, alert.alertType)
        assertTrue(alert.title.contains("పోలాల అమావాస్య"))
    }

    // =========================================================================
    // 2. STRESS TEST DATE BOUNDARIES & LEAP YEARS
    // =========================================================================

    @Test
    fun testLeapYear_february29_2024() = runTest {
        val leapDate = LocalDate.of(2024, 2, 29)
        val epochDay = leapDate.toEpochDay()
        val day = repository.getDay(epochDay)
        assertNotNull(day)
        assertEquals("2024-02-29", day!!.gregorianDate)
        assertNotNull(day.teluguMonth)
        assertNotNull(day.paksham)
        assertNotNull(day.tithi)

        val alert = repository.getTodayAlert(epochDay)
        assertNotNull(alert)
        assertNotNull(alert.alertType)
        assertNotNull(alert.ttsSpeechText)
    }

    @Test
    fun testLeapYear_february29_2028() = runTest {
        val leapDate = LocalDate.of(2028, 2, 29)
        val epochDay = leapDate.toEpochDay()
        val day = repository.getDay(epochDay)
        assertNotNull(day)
        assertEquals("2028-02-29", day!!.gregorianDate)

        val alert = repository.getTodayAlert(epochDay)
        assertNotNull(alert)
    }

    @Test
    fun testDateBoundaries_monthTransitionsAcrossSpan() = runTest {
        // Iterate through all 1095 days across 2025 to 2027
        var current = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2027, 12, 31)

        var totalDaysChecked = 0
        while (!current.isAfter(end)) {
            val epochDay = current.toEpochDay()
            val day = repository.getDay(epochDay)
            assertNotNull("Day should not be null for $current", day)
            assertEquals(current.toString(), day!!.gregorianDate)
            assertTrue("Karte day should be >= 1 for $current", day.karteDayNumber >= 1)
            assertTrue("Total karte days should be > 0 for $current", day.totalKarteDays > 0)
            assertTrue("Karte day should be <= total for $current", day.karteDayNumber <= day.totalKarteDays)

            val alert = repository.getTodayAlert(epochDay)
            assertNotNull("Alert should not be null for $current", alert)
            assertTrue("TTS text should not be empty for $current", alert.ttsSpeechText.isNotEmpty())

            val karte = repository.getCurrentKarte(epochDay)
            assertTrue("Days remaining should be >= 0 for $current", karte.daysRemaining >= 0)
            assertTrue("Progress should be in [0, 1] for $current", karte.progressPercentage in 0f..1f)

            current = current.plusDays(1)
            totalDaysChecked++
        }
        assertEquals(1095, totalDaysChecked)
    }

    @Test
    fun testOutside2025_2027_fallbackResilience() = runTest {
        val testDates = listOf(
            LocalDate.of(1970, 1, 1),
            LocalDate.of(2000, 1, 1),
            LocalDate.of(2020, 6, 15),
            LocalDate.of(2030, 8, 15),
            LocalDate.of(2050, 12, 31),
        )

        for (date in testDates) {
            val epoch = date.toEpochDay()
            val day = repository.getDay(epoch)
            assertNotNull("Fallback day should not be null for $date", day)
            assertNotNull(day!!.teluguYear)
            assertNotNull(day.teluguMonth)
            assertNotNull(day.tithi)
            assertNotNull(day.paksham)

            val alert = repository.getTodayAlert(epoch)
            assertNotNull("Fallback alert should not be null for $date", alert)
            assertTrue(alert.ttsSpeechText.isNotEmpty())

            val karte = repository.getCurrentKarte(epoch)
            assertNotNull("Fallback karte should not be null for $date", karte)
            assertTrue(karte.dayNumber >= 1)
        }
    }

    @Test
    fun testNegativeEpochDay_fallbackResilience() = runTest {
        val pastEpoch = -1000L // around 1967
        val day = repository.getDay(pastEpoch)
        assertNotNull(day)

        val alert = repository.getTodayAlert(pastEpoch)
        assertNotNull(alert)

        val karte = repository.getCurrentKarte(pastEpoch)
        assertNotNull(karte)
    }

    @Test
    fun testObserveDay_flowEmitsExpectedDay() = runTest {
        val epochDay = LocalDate.of(2025, 5, 1).toEpochDay()
        val day = repository.observeDay(epochDay).first()
        assertNotNull(day)
        assertEquals("2025-05-01", day?.gregorianDate)
    }
}
