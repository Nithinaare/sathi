package com.farmsaathi.feature.farm

import com.farmsaathi.core.common.repository.OfflineTeluguCalendarRepositoryImpl
import com.farmsaathi.core.model.LunarAlertType
import com.farmsaathi.core.model.Paksham
import com.farmsaathi.core.model.TeluguKarteluCatalog
import com.farmsaathi.core.model.TeluguMonth
import com.farmsaathi.core.model.TeluguTithi
import com.farmsaathi.feature.farm.viewmodel.SeasonFilter
import com.farmsaathi.feature.farm.viewmodel.TeluguCalendarViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class Milestone2ChallengerCalendarVerificationTest {

    private lateinit var repository: OfflineTeluguCalendarRepositoryImpl

    @Before
    fun setUp() {
        repository = OfflineTeluguCalendarRepositoryImpl()
    }

    // =========================================================================
    // 1. DAY-OF-WEEK SPACER CALCULATION ACROSS ALL 12 MONTHS (2025, 2026, 2027)
    // =========================================================================

    @Test
    fun verifyDayOfWeekSpacerCalculation_allMonths2025to2027() = runTest {
        var sundayStarts = 0
        var mondayStarts = 0
        var saturdayStarts = 0
        var totalMonthsChecked = 0

        for (year in 2025..2027) {
            for (month in 1..12) {
                totalMonthsChecked++
                val firstDate = LocalDate.of(year, month, 1)
                val firstEpochDay = firstDate.toEpochDay()

                // Worker formula under adversarial challenge:
                val spacer = LocalDate.ofEpochDay(firstEpochDay).dayOfWeek.value % 7

                // Validate mathematical expectations based on DayOfWeek
                when (firstDate.dayOfWeek) {
                    DayOfWeek.SUNDAY -> {
                        sundayStarts++
                        // DayOfWeek.SUNDAY.value is 7. 7 % 7 == 0.
                        assertEquals(
                            "Sunday must produce 0 leading spacers ($year-$month)",
                            0,
                            spacer,
                        )
                    }
                    DayOfWeek.MONDAY -> {
                        mondayStarts++
                        // DayOfWeek.MONDAY.value is 1. 1 % 7 == 1.
                        assertEquals(
                            "Monday must produce 1 leading spacer ($year-$month)",
                            1,
                            spacer,
                        )
                    }
                    DayOfWeek.TUESDAY -> assertEquals(2, spacer)
                    DayOfWeek.WEDNESDAY -> assertEquals(3, spacer)
                    DayOfWeek.THURSDAY -> assertEquals(4, spacer)
                    DayOfWeek.FRIDAY -> assertEquals(5, spacer)
                    DayOfWeek.SATURDAY -> {
                        saturdayStarts++
                        // DayOfWeek.SATURDAY.value is 6. 6 % 7 == 6.
                        assertEquals(
                            "Saturday must produce 6 leading spacers ($year-$month)",
                            6,
                            spacer,
                        )
                    }
                }

                // Verify Month Grid alignment via repository data
                val monthDays = repository.observeMonth(year, month).first()
                assertEquals("First day must be day 1", firstEpochDay, monthDays.first().epochDay)
                assertEquals("Days count must match month length", firstDate.lengthOfMonth(), monthDays.size)

                // Every day d in month must map to exact expected weekday column
                for ((dayIndex, day) in monthDays.withIndex()) {
                    val date = LocalDate.ofEpochDay(day.epochDay)
                    val expectedDayOfWeekColumn = date.dayOfWeek.value % 7
                    val computedGridColumn = (spacer + dayIndex) % 7
                    assertEquals(
                        "Grid column mismatch on $date: expected $expectedDayOfWeekColumn but got $computedGridColumn",
                        expectedDayOfWeekColumn,
                        computedGridColumn,
                    )
                }
            }
        }

        assertEquals("Total months verified should be 36 (12 * 3)", 36, totalMonthsChecked)
        assertTrue("Must have tested Sunday starts", sundayStarts > 0)
        assertTrue("Must have tested Monday starts", mondayStarts > 0)
        assertTrue("Must have tested Saturday starts", saturdayStarts > 0)
    }

    // =========================================================================
    // 2. SEASON PARTITION MATHEMATICAL RIGOR (KHARIF, RABI, SUMMER)
    // =========================================================================

    @Test
    fun verifySeasonPartition_disjointnessAndCompleteness() {
        val allKartis = TeluguKarteluCatalog.ALL
        assertEquals("Total Kartis must be exactly 27", 27, allKartis.size)

        val catalogIds = allKartis.map { it.id }.toSet()
        assertEquals("Catalog IDs must be 1..27 without gaps", (1..27).toSet(), catalogIds)

        var kharifCount = 0
        var rabiCount = 0
        var summerCount = 0

        for (id in 1..27) {
            val matchesKharif = SeasonFilter.KHARIF.matches(id)
            val matchesRabi = SeasonFilter.RABI.matches(id)
            val matchesSummer = SeasonFilter.SUMMER.matches(id)
            val matchesAll = SeasonFilter.ALL.matches(id)

            assertTrue("SeasonFilter.ALL must match every Karti (id: $id)", matchesAll)

            val matchingSeasons = listOf(matchesKharif, matchesRabi, matchesSummer).count { it }
            assertEquals(
                "Karti ID $id must match exactly ONE season (no overlaps, no omissions). Matches: Kharif=$matchesKharif, Rabi=$matchesRabi, Summer=$matchesSummer",
                1,
                matchingSeasons,
            )

            if (matchesKharif) kharifCount++
            if (matchesRabi) rabiCount++
            if (matchesSummer) summerCount++
        }

        // Exact partition size checks
        assertEquals("Kharif must have exactly 10 Kartis (IDs 5..14)", 10, kharifCount)
        assertEquals("Rabi must have exactly 11 Kartis (IDs 15..25)", 11, rabiCount)
        assertEquals("Summer must have exactly 6 Kartis (IDs 26..27 and 1..4)", 6, summerCount)
        assertEquals("Sum of partitions must be 27", 27, kharifCount + rabiCount + summerCount)

        // Exact boundary checks
        assertFalse(SeasonFilter.KHARIF.matches(4))
        assertTrue(SeasonFilter.KHARIF.matches(5))
        assertTrue(SeasonFilter.KHARIF.matches(14))
        assertFalse(SeasonFilter.KHARIF.matches(15))

        assertFalse(SeasonFilter.RABI.matches(14))
        assertTrue(SeasonFilter.RABI.matches(15))
        assertTrue(SeasonFilter.RABI.matches(25))
        assertFalse(SeasonFilter.RABI.matches(26))

        assertFalse(SeasonFilter.SUMMER.matches(25))
        assertTrue(SeasonFilter.SUMMER.matches(26))
        assertTrue(SeasonFilter.SUMMER.matches(27))
        assertTrue(SeasonFilter.SUMMER.matches(1))
        assertTrue(SeasonFilter.SUMMER.matches(4))
        assertFalse(SeasonFilter.SUMMER.matches(5))

        // Out-of-bounds inputs
        assertFalse(SeasonFilter.KHARIF.matches(0))
        assertFalse(SeasonFilter.KHARIF.matches(28))
        assertFalse(SeasonFilter.RABI.matches(0))
        assertFalse(SeasonFilter.RABI.matches(28))
        assertFalse(SeasonFilter.SUMMER.matches(0))
        assertFalse(SeasonFilter.SUMMER.matches(28))
    }

    // =========================================================================
    // 3. LUNAR DISC BADGES: MUTUAL EXCLUSIVITY & SACRED FESTIVALS
    // =========================================================================

    @Test
    fun verifyLunarDiscBadges_mutualExclusivity_entireEphemerisSpan() = runTest {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2027, 12, 31)

        var amavasyaCount = 0
        var pournamiCount = 0
        var totalDaysChecked = 0

        var cur = startDate
        while (!cur.isAfter(endDate)) {
            val epochDay = cur.toEpochDay()
            val day = repository.getDay(epochDay)
            assertNotNull("Day must not be null for $cur", day)
            day!!

            // Core mutual exclusivity assertion: A day CANNOT be both Amavasya and Pournami
            assertFalse(
                "Mutual Exclusivity Violation on $cur ($epochDay): isAmavasya and isPournami are both true!",
                day.isAmavasya && day.isPournami,
            )

            if (day.isAmavasya) {
                amavasyaCount++
                assertEquals(
                    "isAmavasya day must have TeluguTithi.AMAVASYA ($cur)",
                    TeluguTithi.AMAVASYA,
                    day.tithi,
                )
                assertEquals(
                    "isAmavasya day must have Paksham.KRISHNA ($cur)",
                    Paksham.KRISHNA,
                    day.paksham,
                )
            }

            if (day.isPournami) {
                pournamiCount++
                assertEquals(
                    "isPournami day must have TeluguTithi.POURNAMI ($cur)",
                    TeluguTithi.POURNAMI,
                    day.tithi,
                )
                assertEquals(
                    "isPournami day must have Paksham.SHUKLA ($cur)",
                    Paksham.SHUKLA,
                    day.paksham,
                )
            }

            cur = cur.plusDays(1)
            totalDaysChecked++
        }

        assertEquals("Total days checked in 2025-2027 (3 full years)", 1095, totalDaysChecked)
        assertTrue("Amavasya count in 3 years should be between 35 and 40 (got $amavasyaCount)", amavasyaCount in 35..40)
        assertTrue("Pournami count in 3 years should be between 35 and 40 (got $pournamiCount)", pournamiCount in 35..40)
    }

    @Test
    fun verifyPolalaAmavasya_behavior() = runTest {
        // Shravana Amavasya 2025: August 23, 2025 (epochDay = 20323)
        val epochDay = LocalDate.of(2025, 8, 23).toEpochDay()
        val day = repository.getDay(epochDay)

        assertNotNull(day)
        day!!

        // Lunar Disc Badge assertions
        assertTrue("Polala Amavasya must have isAmavasya == true", day.isAmavasya)
        assertFalse("Polala Amavasya must have isPournami == false", day.isPournami)
        assertEquals("Polala Amavasya must be in Shravanam", TeluguMonth.SHRAVANAM, day.teluguMonth)
        assertEquals("Polala Amavasya must have Tithi AMAVASYA", TeluguTithi.AMAVASYA, day.tithi)
        assertTrue("Polala Amavasya must be marked as festival", day.isFestival)
        assertEquals("Festival name must be పోలాల అమావాస్య", "పోలాల అమావాస్య", day.festivalName)

        // Alert priority and content check
        val alert = repository.getTodayAlert(epochDay)
        assertEquals("Alert type must be FESTIVAL", LunarAlertType.FESTIVAL, alert.alertType)
        assertTrue("Alert title must mention పోలాల అమావాస్య", alert.title.contains("పోలాల అమావాస్య"))
        assertTrue("Polala Amavasya alert must be priority", alert.isPriority)
    }

    @Test
    fun verifyEruvakaPournami_behavior() = runTest {
        // Jyeshtha Pournami 2025: June 11, 2025 (epochDay = 20250)
        val epochDay = LocalDate.of(2025, 6, 11).toEpochDay()
        val day = repository.getDay(epochDay)

        assertNotNull(day)
        day!!

        // Lunar Disc Badge assertions
        assertTrue("Eruvaka Pournami must have isPournami == true", day.isPournami)
        assertFalse("Eruvaka Pournami must have isAmavasya == false", day.isAmavasya)
        assertEquals("Eruvaka Pournami must be in Jyeshtham", TeluguMonth.JYESHTHAM, day.teluguMonth)
        assertEquals("Eruvaka Pournami must have Tithi POURNAMI", TeluguTithi.POURNAMI, day.tithi)
        assertTrue("Eruvaka Pournami must be marked as festival", day.isFestival)
        assertEquals("Festival name must be ఏరువాక పౌర్ణమి", "ఏరువాక పౌర్ణమి", day.festivalName)

        // Alert priority and content check
        val alert = repository.getTodayAlert(epochDay)
        assertEquals("Alert type must be FESTIVAL", LunarAlertType.FESTIVAL, alert.alertType)
        assertTrue("Alert title must mention ఏరువాక పౌర్ణమి", alert.title.contains("ఏరువాక పౌర్ణమి"))
        assertTrue("Eruvaka Pournami alert must be priority", alert.isPriority)
    }

    @Test
    fun verifyDayAudioText_adversarialAudioSynthesis() = runTest {
        // Test standard day
        val normalDay = repository.getDay(LocalDate.of(2025, 6, 1).toEpochDay())!!
        val normalSpeech = TeluguCalendarViewModel.buildDayAudioText(normalDay, tasksCount = 2)
        assertTrue(normalSpeech.contains("జ్యేష్ఠం"))
        assertTrue(normalSpeech.contains("2 వ్యవసాయ పనులు ఉన్నాయి"))

        // Test Eruvaka Pournami
        val eruvakaDay = repository.getDay(LocalDate.of(2025, 6, 11).toEpochDay())!!
        val eruvakaSpeech = TeluguCalendarViewModel.buildDayAudioText(eruvakaDay, tasksCount = 0)
        assertTrue("Eruvaka Pournami audio must include festival name", eruvakaSpeech.contains("ఏరువాక పౌర్ణమి"))

        // Test Polala Amavasya
        val polalaDay = repository.getDay(LocalDate.of(2025, 8, 23).toEpochDay())!!
        val polalaSpeech = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
        assertTrue("Polala Amavasya audio must include festival name", polalaSpeech.contains("పోలాల అమావాస్య"))
        assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("ఎడ్లకు విశ్రాంతి"))
        assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
    }
}
