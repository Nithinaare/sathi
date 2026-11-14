# Milestone 1 Remediation Investigation Report (Explorer 2)

## Executive Summary
**Issue Investigated**: Issue 2 — Polala Amavasya Agricultural Festival Alert Shadowed by Generic Amavasya Alert in `OfflineTeluguCalendarRepositoryImpl.kt`.  
**Root Cause**: In `calculateTodayAlert(epochDay)`, `if (day.isAmavasya)` was evaluated at line 92 prior to `if (day.isFestival && day.festivalName != null)` at line 117. Because Polala Amavasya ("పోలాల అమావాస్య") falls on Shravana Amavasya, `day.isAmavasya` evaluated to `true`, returning the generic Amavasya cattle rest alert and rendering the dedicated sacred agro-festival guidance completely unreachable dead code.  
**Resolution**: Evaluate `if (day.isFestival && day.festivalName != null)` at Step 1 before `if (day.isAmavasya)` at Step 2. Strengthen unit tests in `OfflineTeluguCalendarRepositoryTest.kt` by separating pure Amavasya assertions from Polala Amavasya festival assertions.

---

## 1. Observation

### 1.1 Direct Source Code Observations
1. `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`:
   - Lines 87–151: Alert precedence in `calculateTodayAlert(epochDay: Long)`:
     ```kotlin
     // 1. Amavasya Day Alert
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

     // 2. Post-Amavasya Spray Window (3rd to 5th day after Amavasya)
     val daysSinceLastAmavasya = findDaysSinceLastAmavasya(epochDay)
     if (daysSinceLastAmavasya in 3..5) {
         return LunarDayAlert(...)
     }

     // 3. Agricultural Festival Alert
     if (day.isFestival && day.festivalName != null) {
         return when (day.festivalName) {
             "ఏరువాక పౌర్ణమి" -> ...
             "పోలాల అమావాస్య" -> LunarDayAlert(
                 alertType = LunarAlertType.FESTIVAL,
                 title = "నేడు పోలాల అమావాస్య: పశువుల కృతజ్ఞతా దినం",
                 subtitle = "ఎడ్లకు బరువు దింపి విశ్రాంతినిచ్చే సాంప్రదాయం • పొలంలో దీపపు బుట్టలు వెలిగించండి",
                 ttsSpeechText = "నేడు పోలాల అమావాస్య. పొలంలో కష్టపడే పశువులకు విశ్రాంతి ఇచ్చి పూజించండి.",
                 actionText = "డైరీలో రాయండి",
                 isPriority = true,
             )
             "కనుమ" -> ...
     ```
   - Lines 387–389 in `findFestival(date: LocalDate, lunar: LunarResult)`:
     ```kotlin
     if (lunar.month == TeluguMonth.SHRAVANAM && lunar.tithi == TeluguTithi.AMAVASYA) {
         return "పోలాల అమావాస్య"
     }
     ```
   - Lines 212–215 in `calculateDay(epochDay: Long)`:
     ```kotlin
     isAmavasya = lunarCalculation.tithi == TeluguTithi.AMAVASYA,
     isPournami = lunarCalculation.tithi == TeluguTithi.POURNAMI,
     isFestival = festival != null,
     festivalName = festival,
     ```
   - On Polala Amavasya (e.g., August 23, 2025, `epochDay = 20323L`), `calculateDay` yields:
     - `isAmavasya = true`
     - `isFestival = true`
     - `festivalName = "పోలాల అమావాస్య"`
   - Because `if (day.isAmavasya)` executes first at line 92, it immediately returns `LunarAlertType.AMAVASYA` with title `"నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి"`.
   - The dedicated branch `"పోలాల అమావాస్య"` in `// 3. Agricultural Festival Alert` (lines 127–134) is shadowed and can never be reached under any circumstances.

2. `PROJECT.md` Feature Mandates:
   - Line 28 (Feature 5): *"Sacred Agro-Festival Alerts: Specific alerts for ఏరువాక పౌర్ణమి (Kharif kickoff/navadhanya), పోలాల అమావాస్య (cattle thanksgiving), and కనుమ."*
   - The unreachable branch directly violates Feature 5 of `PROJECT.md`.

3. `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`:
   - Lines 63–74:
     ```kotlin
     @Test
     fun getTodayAlert_triggersAmavasyaAlertOnAmavasya() = runTest {
         // Shravana Amavasya 2025: Aug 23, 2025 (epochDay = 20323)
         val amavasyaEpoch = 20323L
         val alert = repository.getTodayAlert(amavasyaEpoch)

         assertTrue(
             alert.alertType == LunarAlertType.AMAVASYA || alert.alertType == LunarAlertType.FESTIVAL,
         )
         assertTrue(alert.title.contains("అమావాస్య") || alert.title.contains("పోలాల"))
         assertTrue(alert.isPriority)
     }
     ```
   - The test used August 23, 2025 (Polala Amavasya) but tested it with a relaxed `||` (OR) disjunction, allowing the method to pass when returning either `AMAVASYA` or `FESTIVAL`. This masked the shadowing defect.
   - Furthermore, `OfflineTeluguCalendarRepositoryTest.kt` had no tests explicitly asserting `LunarAlertType.FESTIVAL` or `LunarAlertType.KARTE_TRANSITION`.

4. Empirical Output from Challenger 1 (`Milestone1ChallengerStressTest.kt`):
   ```
   POLALA AMAVASYA ALERT TYPE: AMAVASYA, TITLE: నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి
   ```
   This confirmed that on Polala Amavasya, `alert.alertType` was returned as `AMAVASYA`, not `FESTIVAL`.

---

## 2. Logic Chain

1. **Precedence Hierarchy in Panchang Advisories**:
   - In traditional Telugu agricultural practices and under the Farm Saathi specification, specific sacred agro-festivals (such as Polala Amavasya, Eruvaka Pournami, Kanuma, Ugadi, Sankranti) provide high-value, festival-specific cultural and agricultural guidance (e.g., removing weight from bullocks, cattle worship, sowing navadhanya).
   - Generic lunar advisories (e.g., Amavasya cattle rest, Pournami foliar spraying) serve as general baseline alerts when no sacred agricultural festival is being observed.
   - Therefore, specific agricultural festival guidance must take precedence over generic lunar alerts.

2. **Root Cause Mechanism**:
   - `calculateTodayAlert` was structured as:
     1. Amavasya (`isAmavasya`)
     2. Post-Amavasya Spray Window (`daysSinceLastAmavasya in 3..5`)
     3. Agricultural Festivals (`isFestival && festivalName != null`)
     4. Pournami (`isPournami`)
     5. Karte Entry Day (`karteDayNumber == 1`)
     6. Standard Day Panchang
   - Polala Amavasya occurs on Shravana Amavasya. By definition, its tithi is `TeluguTithi.AMAVASYA`.
   - Because `day.isAmavasya` is evaluated in step 1, any festival falling on Amavasya (`పోలాల అమావాస్య`, `మహాలయ అమావాస్య`, `దీపావళి`) gets intercepted at step 1 and returns the generic Amavasya alert.
   - The dedicated festival alert in step 3 is unreachable dead code.

3. **Remediation Strategy**:
   - Reorder the evaluation sequence in `calculateTodayAlert` so that:
     1. `// 1. Agricultural Festival Alert`: `if (day.isFestival && day.festivalName != null)` -> returns `LunarAlertType.FESTIVAL` (handles Polala Amavasya, Eruvaka Pournami, Kanuma, etc.)
     2. `// 2. Amavasya Day Alert`: `if (day.isAmavasya)` -> returns `LunarAlertType.AMAVASYA` (handles pure non-festival Amavasyas like Chaitra Amavasya, Vaishakha Amavasya, Jyeshtha Amavasya, Ashadha Amavasya)
     3. `// 3. Post-Amavasya Spray Window`: `if (daysSinceLastAmavasya in 3..5)` -> returns `LunarAlertType.POST_AMAVASYA_SPRAY`
     4. `// 4. Pournami Alert`: `if (day.isPournami)` -> returns `LunarAlertType.POURNAMI` (handles regular Pournamis not tied to festivals)
     5. `// 5. Karte Entry Day Alert`: `if (day.karteDayNumber == 1)` -> returns `LunarAlertType.KARTE_TRANSITION`
     6. `// 6. Clean Standard Day Alert`: returns `LunarAlertType.DAY_STANDARD`
   - In `OfflineTeluguCalendarRepositoryTest.kt`:
     - Disambiguate `getTodayAlert_triggersAmavasyaAlertOnAmavasya` by using a pure Amavasya date: Chaitra Amavasya 2025 (April 27, 2025, `epochDay = 20205L`). Assert strictly `assertEquals(LunarAlertType.AMAVASYA, alert.alertType)`.
     - Add `getTodayAlert_triggersFestivalAlertOnPolalaAmavasya` for August 23, 2025 (`epochDay = 20323L`). Assert strictly `assertEquals(LunarAlertType.FESTIVAL, alert.alertType)` and `assertTrue(alert.title.contains("పోలాల"))`.
     - Add `getTodayAlert_triggersFestivalAlertOnEruvakaPournami` for June 11, 2025 (`epochDay = 20250L`). Assert `assertEquals(LunarAlertType.FESTIVAL, alert.alertType)` and `assertTrue(alert.title.contains("ఏరువాక"))`.
     - Add `getTodayAlert_triggersKarteTransitionAlertOnDay1` for June 8, 2025 (`epochDay = 20247L`). Assert `assertEquals(LunarAlertType.KARTE_TRANSITION, alert.alertType)`.

---

## 3. Caveats

- **Leap Years and Ephemeris Fallback**: Verified by Challenger 1 stress harness; synodic fallback and boundary conditions across leap years (2024, 2028) and all 1,095 days of 2025–2027 function flawlessly.
- **Other Amavasya Festivals**: Festivals such as Mahalaya Amavasya and Deepavali will also correctly emit `LunarAlertType.FESTIVAL` instead of generic `AMAVASYA`. This is the intended behavior since cultural/agro festivals take precedence.
- **Read-Only Explorer Constraint**: In accordance with the Explorer archetype constraint, no direct edits have been made to source code. The exact instructions are documented below for Worker execution.

---

## 4. Conclusion & Step-by-Step Remediation Plan for Worker

### Step 1: Update `OfflineTeluguCalendarRepositoryImpl.kt`
**File Target**: `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`  
**Method Target**: `private fun calculateTodayAlert(epochDay: Long): LunarDayAlert` (lines 87–184)  
**Action**: Move `// 3. Agricultural Festival Alert` block above `// 1. Amavasya Day Alert`.

#### Exact Code Replacement:
```kotlin
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

        // 2. Amavasya Day Alert (Evaluated for non-festival Amavasyas)
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

        // 4. Pournami Alert (Evaluated for non-festival Pournamis)
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
```

---

### Step 2: Update `OfflineTeluguCalendarRepositoryTest.kt`
**File Target**: `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`  
**Lines Target**: Replace lines 62–74 with pure Amavasya and Polala Amavasya tests; add Eruvaka Pournami and Karte Transition tests.

#### Exact Code Replacement for Lines 62–74:
```kotlin
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
        assertTrue(alert.subtitle.contains("ఎడ్లకు బరువు దింపి విశ్రాంతినిచ్చే సాంప్రదాయం"))
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
```

---

### Step 3: Update `Milestone1ChallengerStressTest.kt` (Optional / Recommended)
In `core/common/src/test/kotlin/com/farmsaathi/core/common/Milestone1ChallengerStressTest.kt`:
Update `testAlert_polalaAmavasyaBugInvestigation` lines 146–152 to assert strictly:
```kotlin
        val alert = repository.getTodayAlert(polalaDay)
        assertEquals(LunarAlertType.FESTIVAL, alert.alertType)
        assertTrue(alert.title.contains("పోలాల అమావాస్య"))
        assertTrue(alert.title.contains("పశువుల కృతజ్ఞతా దినం"))
        assertTrue(alert.isPriority)
```

---

## 5. Verification Method

To verify this remediation independently:

1. **Working Directory**:
   ```powershell
   Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
   ```

2. **Execute Unit Tests**:
   ```powershell
   .\gradlew.bat :core:common:testDebugUnitTest
   ```

3. **Pass Criteria**:
   - `BUILD SUCCESSFUL`
   - All tests in `:core:common` pass (11+ unit tests in `OfflineTeluguCalendarRepositoryTest` + 13 tests in `Milestone1ChallengerStressTest`).
   - Zero test failures or regressions.
   - Polala Amavasya returns `LunarAlertType.FESTIVAL` with title `"నేడు పోలాల అమావాస్య: పశువుల కృతజ్ఞతా దినం"`.
   - Pure Amavasya (April 27, 2025) returns `LunarAlertType.AMAVASYA` with title `"నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి"`.
