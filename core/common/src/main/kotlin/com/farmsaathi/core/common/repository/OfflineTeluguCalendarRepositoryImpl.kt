package com.farmsaathi.core.common.repository

import com.farmsaathi.core.model.CurrentKarteInfo
import com.farmsaathi.core.model.LunarAlertType
import com.farmsaathi.core.model.LunarDayAlert
import com.farmsaathi.core.model.Paksham
import com.farmsaathi.core.model.TeluguCalendarDay
import com.farmsaathi.core.model.TeluguKarte
import com.farmsaathi.core.model.TeluguKarteluCatalog
import com.farmsaathi.core.model.TeluguMonth
import com.farmsaathi.core.model.TeluguTithi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.YearMonth

class OfflineTeluguCalendarRepositoryImpl : TeluguCalendarRepository {

    override fun observeDay(epochDay: Long): Flow<TeluguCalendarDay?> =
        flow { emit(calculateDay(epochDay)) }

    override suspend fun getDay(epochDay: Long): TeluguCalendarDay? =
        calculateDay(epochDay)

    override fun observeMonth(
        year: Int,
        month: Int,
    ): Flow<List<TeluguCalendarDay>> =
        flow { emit(calculateMonthDays(year, month)) }

    override suspend fun getMonthDays(
        year: Int,
        month: Int,
    ): List<TeluguCalendarDay> =
        calculateMonthDays(year, month)

    override fun observeCurrentKarte(epochDay: Long): Flow<CurrentKarteInfo> =
        flow { emit(calculateCurrentKarte(epochDay)) }

    override suspend fun getCurrentKarte(epochDay: Long): CurrentKarteInfo =
        calculateCurrentKarte(epochDay)

    override fun observeTodayAlert(epochDay: Long): Flow<LunarDayAlert> =
        flow { emit(calculateTodayAlert(epochDay)) }

    override suspend fun getTodayAlert(epochDay: Long): LunarDayAlert =
        calculateTodayAlert(epochDay)

    private fun calculateMonthDays(
        year: Int,
        month: Int,
    ): List<TeluguCalendarDay> {
        val ym = YearMonth.of(year, month)
        val daysInMonth = ym.lengthOfMonth()
        val result = ArrayList<TeluguCalendarDay>(daysInMonth)
        for (day in 1..daysInMonth) {
            val date = LocalDate.of(year, month, day)
            result.add(calculateDay(date.toEpochDay()))
        }
        return result
    }

    private fun calculateCurrentKarte(epochDay: Long): CurrentKarteInfo {
        val day = calculateDay(epochDay)
        val karte = TeluguKarteluCatalog.getById(day.karteId)
        val daysRemaining = (day.totalKarteDays - day.karteDayNumber).coerceAtLeast(0)
        val progress = if (day.totalKarteDays > 0) {
            (day.karteDayNumber.toFloat() / day.totalKarteDays.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        return CurrentKarteInfo(
            karteId = karte.id,
            teluguName = karte.teluguName,
            englishName = karte.englishName,
            dayNumber = day.karteDayNumber,
            totalDays = day.totalKarteDays,
            daysRemaining = daysRemaining,
            progressPercentage = progress,
            proverb = karte.proverb,
            agriculturalSignificance = karte.agriculturalSignificance,
            recommendedCrops = karte.recommendedCrops,
        )
    }

    private fun calculateTodayAlert(epochDay: Long): LunarDayAlert {
        val day = calculateDay(epochDay)
        val karteInfo = calculateCurrentKarte(epochDay)

        // 1. Agricultural Festival Alert (Takes precedence over generic lunar alerts)
        if (day.isFestival && day.festivalName != null) {
            return when (day.festivalName) {
                "ఏరువాక పౌర్ణమి" -> LunarDayAlert(
                    alertType = LunarAlertType.FESTIVAL,
                    title = "నేడు ఏరువాక పౌర్ణమి: వానాకాలం ప్రారంభం",
                    subtitle = "ఎడ్లను అలంకరించి తొలి దుక్కి దున్నే పర్వదినం • నవధాన్యాల విత్తనం",
                    ttsSpeechText = "నేడు ఏరువాక పౌర్ణమి. వానాకాలం సాగుకు తొలి అడుగు. కాడెడ్లకు పూజ చేసి తొలి దుక్కి వేయండి.",
                    actionText = "తొలి దుక్కి",
                    isPriority = true,
                )
                "పోలాల అమావాస్య" -> LunarDayAlert(
                    alertType = LunarAlertType.FESTIVAL,
                    title = "నేడు పోలాల అమావాస్య: పశువుల కృతజ్ఞతా దినం",
                    subtitle = "ఎడ్లకు బరువు దింపి విశ్రాంతినిచ్చే సాంప్రదాయం • పొలంలో దీపపు బుట్టలు వెలిగించండి",
                    ttsSpeechText = "నేడు పోలాల అమావాస్య. పొలంలో కష్టపడే పశువులకు విశ్రాంతి ఇచ్చి పూజించండి.",
                    actionText = "డైరీలో రాయండి",
                    isPriority = true,
                )
                "కనుమ" -> LunarDayAlert(
                    alertType = LunarAlertType.FESTIVAL,
                    title = "నేడు కనుమ: పశువుల పండుగ",
                    subtitle = "వ్యవసాయ పశువులను పూజించి గౌరవించే దినం",
                    ttsSpeechText = "రైతు మిత్రులారా, నేడు కనుమ పండుగ. మనకు చేదోడువాదోడుగా ఉండే పశువుల పండుగ.",
                    actionText = "పశువుల సంరక్షణ",
                    isPriority = true,
                )
                else -> LunarDayAlert(
                    alertType = LunarAlertType.FESTIVAL,
                    title = "నేడు ${day.festivalName}",
                    subtitle = "${day.teluguMonth.teluguName} ${day.tithi.teluguName} శుభాకాంక్షలు",
                    ttsSpeechText = "రైతు మిత్రులారా, మీకు ${day.festivalName} శుభాకాంక్షలు.",
                    isPriority = false,
                )
            }
        }

        // 2. Amavasya Day Alert
        if (day.isAmavasya) {
            return LunarDayAlert(
                alertType = LunarAlertType.AMAVASYA,
                title = "నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి",
                subtitle = "చీకటి రాత్రికి దీపపు బుట్టలు అమర్చండి • రాత్రి పురుగుల సంపర్కం నివారించండి",
                ttsSpeechText = "రైతు మిత్రులారా, నేడు అమావాస్య. కాడెడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా చేలో దీపపు బుట్టలు వెలిగించండి.",
                actionText = "రక్షణ చర్యలు",
                isPriority = true,
            )
        }

        // 3. Post-Amavasya Spray Window (3rd to 5th day after Amavasya)
        val daysSinceLastAmavasya = findDaysSinceLastAmavasya(epochDay)
        if (daysSinceLastAmavasya in 3..5) {
            return LunarDayAlert(
                alertType = LunarAlertType.POST_AMAVASYA_SPRAY,
                title = "అమావాస్య అదను: పురుగుల నివారణ సమయం",
                subtitle = "అమావాస్య తర్వాత ${daysSinceLastAmavasya}వ రోజు. గుడ్ల నుంచి తొలి దశ పురుగులు బయటకు వస్తాయి • వేప కషాయం (NSKE 5%) పిచికారీకి సరైన సమయం",
                ttsSpeechText = "అమావాస్య తర్వాత చిన్న పురుగులు బయటకు వచ్చే సమయం ఇది. లేత పురుగుల నివారణకు వేప గింజల కషాయం పిచికారీ చేయండి.",
                actionText = "పిచికారీ సూచనలు",
                isPriority = true,
            )
        }

        // 4. Pournami Alert
        if (day.isPournami) {
            return LunarDayAlert(
                alertType = LunarAlertType.POURNAMI,
                title = "నేడు పౌర్ణమి: ${day.teluguMonth.teluguName} మాసం",
                subtitle = "పైపాటు పెరుగుదల మరియు విత్తనాలు విత్తుటకు అనుకూలమైన శుక్ల పక్ష పౌర్ణమి",
                ttsSpeechText = "నేడు ${day.teluguMonth.teluguName} పౌర్ణమి. పైరు ఎదుగుదలకు అనుకూలమైన సమయం.",
                isPriority = false,
            )
        }

        // 5. Karte Entry Day Alert (Day 1 of new Karte)
        if (day.karteDayNumber == 1) {
            return LunarDayAlert(
                alertType = LunarAlertType.KARTE_TRANSITION,
                title = "నేడు ${day.karteName} కార్తె ప్రారంభం",
                subtitle = "అదను: ${karteInfo.agriculturalSignificance}",
                ttsSpeechText = "నేడు ${day.karteName} కార్తె ప్రారంభమైంది. ${karteInfo.agriculturalSignificance}",
                actionText = "కార్తె అదను వివరాలు",
                isPriority = true,
            )
        }

        // 6. Clean Standard Day Alert
        return LunarDayAlert(
            alertType = LunarAlertType.DAY_STANDARD,
            title = "${day.teluguMonth.teluguName} • ${day.paksham.teluguName} ${day.tithi.teluguName}",
            subtitle = "${day.karteName} కార్తె (${day.karteDayNumber}/${day.totalKarteDays}వ రోజు) • ${karteInfo.proverb}",
            ttsSpeechText = "ఈరోజు ${day.teluguMonth.teluguName} మాసం, ${day.paksham.teluguName}, ${day.tithi.teluguName}. ${day.karteName} కార్తె ${day.karteDayNumber}వ రోజు నడుస్తోంది. సామెత: ${karteInfo.proverb}.",
            isPriority = false,
        )
    }

    override fun observeAllKartis(): Flow<List<TeluguKarte>> =
        flowOf(TeluguKarteluCatalog.ALL)

    // --- Internal Calculation Engines ---

    private fun calculateDay(epochDay: Long): TeluguCalendarDay {
        val date = LocalDate.ofEpochDay(epochDay)
        val gregorianDate = date.toString()

        val karteCalculation = calculateKarte(date)
        val lunarCalculation = calculateLunarPanchang(epochDay, date)

        val festival = findFestival(date, lunarCalculation)

        return TeluguCalendarDay(
            epochDay = epochDay,
            gregorianDate = gregorianDate,
            teluguYear = lunarCalculation.yearName,
            teluguMonth = lunarCalculation.month,
            paksham = lunarCalculation.paksham,
            tithi = lunarCalculation.tithi,
            nakshatram = calculateNakshatram(epochDay),
            karteId = karteCalculation.karteId,
            karteName = karteCalculation.karteName,
            karteDayNumber = karteCalculation.dayNumber,
            totalKarteDays = karteCalculation.totalDays,
            isAmavasya = lunarCalculation.tithi == TeluguTithi.AMAVASYA,
            isPournami = lunarCalculation.tithi == TeluguTithi.POURNAMI,
            isFestival = festival != null,
            festivalName = festival,
            seasonalAdvisory = karteCalculation.advisory,
        )
    }

    private data class KarteResult(
        val karteId: Int,
        val karteName: String,
        val dayNumber: Int,
        val totalDays: Int,
        val advisory: String,
    )

    private fun calculateKarte(date: LocalDate): KarteResult {
        val month = date.monthValue
        val day = date.dayOfMonth
        val year = date.year

        // Find the matching karte from fixed solar entry dates
        for (i in KARTE_BOUNDARIES.indices) {
            val curr = KARTE_BOUNDARIES[i]
            val next = if (i + 1 < KARTE_BOUNDARIES.size) {
                KARTE_BOUNDARIES[i + 1]
            } else {
                KARTE_BOUNDARIES[0]
            }

            val startDate = if (curr.month > month || (curr.month == month && curr.day > day)) {
                // If boundary is in Dec and date is in Jan, or similar wrap-around
                LocalDate.of(year - (if (curr.month >= 11 && month <= 4) 1 else 0), curr.month, curr.day)
            } else {
                LocalDate.of(year, curr.month, curr.day)
            }

            val endDateExclusive = if (next.month < curr.month) {
                LocalDate.of(startDate.year + 1, next.month, next.day)
            } else {
                LocalDate.of(startDate.year, next.month, next.day)
            }

            if (!date.isBefore(startDate) && date.isBefore(endDateExclusive)) {
                val dayNum = (date.toEpochDay() - startDate.toEpochDay()).toInt() + 1
                val totalDays = (endDateExclusive.toEpochDay() - startDate.toEpochDay()).toInt()
                val karte = TeluguKarteluCatalog.getById(curr.karteId)
                return KarteResult(
                    karteId = curr.karteId,
                    karteName = karte.teluguName,
                    dayNumber = dayNum,
                    totalDays = totalDays,
                    advisory = karte.agriculturalSignificance,
                )
            }
        }

        // Fallback to Ashwini
        val fallbackKarte = TeluguKarteluCatalog.getById(1)
        return KarteResult(
            karteId = 1,
            karteName = fallbackKarte.teluguName,
            dayNumber = 1,
            totalDays = 13,
            advisory = fallbackKarte.agriculturalSignificance,
        )
    }

    private data class LunarResult(
        val yearName: String,
        val month: TeluguMonth,
        val paksham: Paksham,
        val tithi: TeluguTithi,
    )

    private fun calculateLunarPanchang(epochDay: Long, date: LocalDate): LunarResult {
        // Find matching Lunar Month Span for 2025-2027
        for (span in LUNAR_MONTH_SPANS) {
            if (epochDay >= span.startEpochDay && epochDay <= span.endEpochDay) {
                val isShukla = epochDay <= span.pournamiEpochDay
                val paksham = if (isShukla) Paksham.SHUKLA else Paksham.KRISHNA

                val tithi = when {
                    epochDay == span.endEpochDay -> TeluguTithi.AMAVASYA
                    epochDay == span.pournamiEpochDay -> TeluguTithi.POURNAMI
                    isShukla -> {
                        val offset = (epochDay - span.startEpochDay).toInt() + 1
                        TeluguTithi.fromId(offset.coerceIn(1, 14))
                    }
                    else -> {
                        val offset = (epochDay - span.pournamiEpochDay).toInt()
                        TeluguTithi.fromId(offset.coerceIn(1, 14))
                    }
                }

                return LunarResult(
                    yearName = span.yearName,
                    month = span.month,
                    paksham = paksham,
                    tithi = tithi,
                )
            }
        }

        // Algorithmic fallback using synodic month (mean 29.530588 days)
        val refEpochDay = 20177L // 2025-03-30 (Ugadi, Chaitra Shukla Padyami, Vishvavasu)
        val daysSinceRef = epochDay - refEpochDay
        val synodicDays = 29.530588
        val cycle = ((daysSinceRef % synodicDays) + synodicDays) % synodicDays
        val tithiNumber = (cycle / (synodicDays / 30.0)).toInt() + 1

        val (paksham, tithi) = if (tithiNumber <= 15) {
            if (tithiNumber == 15) {
                Pair(Paksham.SHUKLA, TeluguTithi.POURNAMI)
            } else {
                Pair(Paksham.SHUKLA, TeluguTithi.fromId(tithiNumber.coerceIn(1, 14)))
            }
        } else {
            if (tithiNumber >= 30) {
                Pair(Paksham.KRISHNA, TeluguTithi.AMAVASYA)
            } else {
                Pair(Paksham.KRISHNA, TeluguTithi.fromId((tithiNumber - 15).coerceIn(1, 14)))
            }
        }

        val monthIndex = (((daysSinceRef / synodicDays).toInt() % 12) + 12) % 12 + 1
        val yearName = if (date.year >= 2026) "పరాభవ" else "విశ్వావసు"

        return LunarResult(
            yearName = yearName,
            month = TeluguMonth.fromId(monthIndex),
            paksham = paksham,
            tithi = tithi,
        )
    }

    private fun findDaysSinceLastAmavasya(epochDay: Long): Int {
        var lastAmavasyaEpoch: Long? = null
        for (span in LUNAR_MONTH_SPANS) {
            if (span.endEpochDay < epochDay) {
                lastAmavasyaEpoch = span.endEpochDay
            } else {
                break
            }
        }
        return if (lastAmavasyaEpoch != null) {
            (epochDay - lastAmavasyaEpoch).toInt()
        } else {
            // Algorithmic approximation
            val synodic = 29.530588
            val refAmavasya = 20176L // 2025-03-29
            val diff = epochDay - refAmavasya
            val rem = ((diff % synodic) + synodic) % synodic
            rem.toInt()
        }
    }

    private fun calculateNakshatram(epochDay: Long): String {
        // Sidereal Nakshatras rotate in 27.32 days
        val index = (((epochDay % 27) + 27) % 27).toInt()
        return NAKSHATRAS[index]
    }

    private fun findFestival(date: LocalDate, lunar: LunarResult): String? {
        // Solar fixed agricultural festivals
        if (date.monthValue == 1 && date.dayOfMonth in 14..15) return "సంక్రాంతి"
        if (date.monthValue == 1 && date.dayOfMonth == 16) return "కనుమ"

        // Lunar agricultural festivals
        if (lunar.month == TeluguMonth.CHAITRAM && lunar.paksham == Paksham.SHUKLA && lunar.tithi == TeluguTithi.PADYAMI) {
            return "ఉగాది"
        }
        if (lunar.month == TeluguMonth.JYESHTHAM && lunar.tithi == TeluguTithi.POURNAMI) {
            return "ఏరువాక పౌర్ణమి"
        }
        if (lunar.month == TeluguMonth.SHRAVANAM && lunar.tithi == TeluguTithi.AMAVASYA) {
            return "పోలాల అమావాస్య"
        }
        if (lunar.month == TeluguMonth.BHADRAPADAM && lunar.tithi == TeluguTithi.AMAVASYA) {
            return "మహాలయ అమావాస్య"
        }
        if (lunar.month == TeluguMonth.ASHWAYUJAM && lunar.tithi == TeluguTithi.AMAVASYA) {
            return "దీపావళి"
        }
        if (lunar.month == TeluguMonth.KARTHIKAM && lunar.tithi == TeluguTithi.POURNAMI) {
            return "కార్తీక పౌర్ణమి"
        }
        return null
    }

    companion object {
        private data class KarteBoundary(
            val karteId: Int,
            val month: Int,
            val day: Int,
        )

        private val KARTE_BOUNDARIES = listOf(
            KarteBoundary(1, 4, 14), // Ashwini: Apr 14
            KarteBoundary(2, 4, 27), // Bharani: Apr 27
            KarteBoundary(3, 5, 11), // Krittika: May 11
            KarteBoundary(4, 5, 25), // Rohini: May 25
            KarteBoundary(5, 6, 8), // Mrigashira: Jun 8
            KarteBoundary(6, 6, 22), // Arudra: Jun 22
            KarteBoundary(7, 7, 6), // Punarvasu: Jul 6
            KarteBoundary(8, 7, 20), // Pushyami: Jul 20
            KarteBoundary(9, 8, 3), // Ashlesha: Aug 3
            KarteBoundary(10, 8, 17), // Makha: Aug 17
            KarteBoundary(11, 8, 31), // Pubba: Aug 31
            KarteBoundary(12, 9, 13), // Uttara: Sep 13
            KarteBoundary(13, 9, 27), // Hasta: Sep 27
            KarteBoundary(14, 10, 11), // Chitta: Oct 11
            KarteBoundary(15, 10, 24), // Swati: Oct 24
            KarteBoundary(16, 11, 6), // Vishakha: Nov 6
            KarteBoundary(17, 11, 20), // Anuradha: Nov 20
            KarteBoundary(18, 12, 3), // Jyeshtha: Dec 3
            KarteBoundary(19, 12, 16), // Moola: Dec 16
            KarteBoundary(20, 12, 29), // Purvashadha: Dec 29
            KarteBoundary(21, 1, 11), // Uttarashadha: Jan 11
            KarteBoundary(22, 1, 24), // Shravana: Jan 24
            KarteBoundary(23, 2, 6), // Dhanishtha: Feb 6
            KarteBoundary(24, 2, 19), // Shatabhisha: Feb 19
            KarteBoundary(25, 3, 4), // Purvabhadra: Mar 4
            KarteBoundary(26, 3, 18), // Uttarabhadra: Mar 18
            KarteBoundary(27, 3, 31), // Revati: Mar 31
        )

        private data class LunarSpan(
            val yearName: String,
            val month: TeluguMonth,
            val startEpochDay: Long,
            val pournamiEpochDay: Long,
            val endEpochDay: Long,
        )

        // Verified Chandramana lunar spans covering 2025, 2026, and 2027
        private val LUNAR_MONTH_SPANS = listOf(
            // 2025 (Krodhi / Vishvavasu)
            LunarSpan("క్రోధి", TeluguMonth.PUSHYAM, 20088L, 20101L, 20116L), // 2024-12-31 to 2025-01-28
            LunarSpan("క్రోధి", TeluguMonth.MAGHAM, 20117L, 20131L, 20146L), // 2025-01-29 to 2025-02-27
            LunarSpan("క్రోధి", TeluguMonth.PHALGUNAM, 20147L, 20161L, 20176L), // 2025-02-28 to 2025-03-29
            // Ugadi 2025 -> Vishvavasu starts
            LunarSpan("విశ్వావసు", TeluguMonth.CHAITRAM, 20177L, 20191L, 20205L), // 2025-03-30 to 2025-04-27
            LunarSpan("విశ్వావసు", TeluguMonth.VAISHAKHAM, 20206L, 20220L, 20235L), // 2025-04-28 to 2025-05-27
            LunarSpan("విశ్వావసు", TeluguMonth.JYESHTHAM, 20236L, 20250L, 20264L), // 2025-05-28 to 2025-06-25
            LunarSpan("విశ్వావసు", TeluguMonth.ASHADHAM, 20265L, 20279L, 20293L), // 2025-06-26 to 2025-07-24
            LunarSpan("విశ్వావసు", TeluguMonth.SHRAVANAM, 20294L, 20309L, 20323L), // 2025-07-25 to 2025-08-23
            LunarSpan("విశ్వావసు", TeluguMonth.BHADRAPADAM, 20324L, 20338L, 20352L), // 2025-08-24 to 2025-09-21
            LunarSpan("విశ్వావసు", TeluguMonth.ASHWAYUJAM, 20353L, 20367L, 20382L), // 2025-09-22 to 2025-10-21
            LunarSpan("విశ్వావసు", TeluguMonth.KARTHIKAM, 20383L, 20397L, 20412L), // 2025-10-22 to 2025-11-20
            LunarSpan("విశ్వావసు", TeluguMonth.MARGASHIRAM, 20413L, 20427L, 20442L), // 2025-11-21 to 2025-12-20
            LunarSpan("విశ్వావసు", TeluguMonth.PUSHYAM, 20443L, 20456L, 20471L), // 2025-12-21 to 2026-01-18
            // 2026 (Vishvavasu / Parabhava)
            LunarSpan("విశ్వావసు", TeluguMonth.MAGHAM, 20472L, 20486L, 20501L), // 2026-01-19 to 2026-02-17
            LunarSpan("విశ్వావసు", TeluguMonth.PHALGUNAM, 20502L, 20515L, 20530L), // 2026-02-18 to 2026-03-18
            // Ugadi 2026 -> Parabhava starts
            LunarSpan("పరాభవ", TeluguMonth.CHAITRAM, 20531L, 20545L, 20560L), // 2026-03-19 to 2026-04-17
            LunarSpan("పరాభవ", TeluguMonth.VAISHAKHAM, 20561L, 20575L, 20589L), // 2026-04-18 to 2026-05-16
            LunarSpan("పరాభవ", TeluguMonth.JYESHTHAM, 20590L, 20604L, 20618L), // 2026-05-17 to 2026-06-14
            LunarSpan("పరాభవ", TeluguMonth.ASHADHAM, 20619L, 20633L, 20648L), // 2026-06-15 to 2026-07-14
            LunarSpan("పరాభవ", TeluguMonth.SHRAVANAM, 20649L, 20663L, 20677L), // 2026-07-15 to 2026-08-12
            LunarSpan("పరాభవ", TeluguMonth.BHADRAPADAM, 20678L, 20692L, 20707L), // 2026-08-13 to 2026-09-11
            LunarSpan("పరాభవ", TeluguMonth.ASHWAYUJAM, 20708L, 20722L, 20736L), // 2026-09-12 to 2026-10-10
            LunarSpan("పరాభవ", TeluguMonth.KARTHIKAM, 20737L, 20751L, 20766L), // 2026-10-11 to 2026-11-09
            LunarSpan("పరాభవ", TeluguMonth.MARGASHIRAM, 20767L, 20781L, 20796L), // 2026-11-10 to 2026-12-09
            LunarSpan("పరాభవ", TeluguMonth.PUSHYAM, 20797L, 20810L, 20825L), // 2026-12-10 to 2027-01-07
            // 2027
            LunarSpan("పరాభవ", TeluguMonth.MAGHAM, 20826L, 20840L, 20855L), // 2027-01-08 to 2027-02-06
            LunarSpan("పరాభవ", TeluguMonth.PHALGUNAM, 20856L, 20870L, 20885L), // 2027-02-07 to 2027-03-08
            LunarSpan("ప్లవంగ", TeluguMonth.CHAITRAM, 20886L, 20900L, 20914L), // 2027-03-09 to 2027-04-06
        )

        private val NAKSHATRAS = listOf(
            "అశ్విని", "భరణి", "కృత్తిక", "రోహిణి", "మృగశిర", "ఆరుద్ర",
            "పునర్వసు", "పుష్యమి", "ఆశ్లేష", "మఖ", "పుబ్బ", "ఉత్తర",
            "హస్త", "చిత్త", "స్వాతి", "విశాఖ", "అనూరాధ", "జ్యేష్ఠ",
            "మూల", "పూర్వాషాఢ", "ఉత్తరాషాఢ", "శ్రవణం", "ధనిష్ఠ", "శతభిషం",
            "పూర్వాభాద్ర", "ఉత్తరాభాద్ర", "రేవతి",
        )
    }
}
