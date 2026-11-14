# Milestone 1 Challenger Report (Challenger 1)

## Explicit Verdict: REQUEST_CHANGES

---

## 1. Observation

### 1.1 Direct Code Observations & Shadowed Festival Defect
1. `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`:
   - Lines 91–101:
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
     ```
   - Lines 117–134:
     ```kotlin
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
     ```
   - Lines 387–389:
     ```kotlin
     if (lunar.month == TeluguMonth.SHRAVANAM && lunar.tithi == TeluguTithi.AMAVASYA) {
         return "పోలాల అమావాస్య"
     }
     ```
   - Polala Amavasya occurs on Shravana Amavasya (e.g. August 23, 2025, `epochDay = 20323L`).
   - Because `day.isAmavasya` is evaluated at line 92 before `day.isFestival` at line 117, on August 23, 2025 `calculateTodayAlert` immediately returns `LunarAlertType.AMAVASYA` with title `"నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి"`.
   - The specialized agricultural festival alert block at lines 127–134 (`"పోలాల అమావాస్య: పశువుల కృతజ్ఞతా దినం"`, mandated by `PROJECT.md` Feature 5) is unreachable dead code.

### 1.2 Test Disjunction & Incomplete Alert Type Coverage
1. `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`:
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
   - The assertion uses a permissive `||` disjunction that masks the fact that `FESTIVAL` is never returned.
   - Across all 8 tests in `OfflineTeluguCalendarRepositoryTest.kt`, there is:
     - **ZERO** test asserting `LunarAlertType.FESTIVAL` (neither for Eruvaka Pournami, Kanuma, Ugadi, nor Polala Amavasya).
     - **ZERO** test asserting `LunarAlertType.KARTE_TRANSITION` (for day 1 of any of the 27 Kartis).

### 1.3 Empirical Stress Harness Results
Command executed:
```powershell
Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"; .\gradlew.bat --no-daemon :core:common:testDebugUnitTest
```
Verification test file created: `core/common/src/test/kotlin/com/farmsaathi/core/common/Milestone1ChallengerStressTest.kt`.
Results report: `core/common/build/reports/tests/testDebugUnitTest/classes/com.farmsaathi.core.common.Milestone1ChallengerStressTest.html`:
- 13 tests executed, 13 passed, 0 failures.
- Verbatim standard output from `testAlert_polalaAmavasyaBugInvestigation`:
  ```
  POLALA AMAVASYA ALERT TYPE: AMAVASYA, TITLE: నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి
  ```
  This empirically confirms that on Polala Amavasya, `alert.alertType` is `AMAVASYA` and `FESTIVAL` is shadowed.
- `testLeapYear_february29_2024` and `testLeapYear_february29_2028`: PASSED without throwing `DateTimeException`.
- `testDateBoundaries_monthTransitionsAcrossSpan`: Successfully iterated all 1,095 consecutive days in 2025–2027 without nulls or calculation anomalies.
- `testOutside2025_2027_fallbackResilience`: Fallback synodic engine evaluated `1970-01-01`, `2000-01-01`, `2020-06-15`, `2030-08-15`, `2050-12-31`, and epoch `-1000L` without index-out-of-bounds or invalid enums.

### 1.4 Defensive Null Safety in UI & ViewModel
1. `feature/home/src/main/kotlin/com/farmsaathi/feature/home/screens/HomeDashboardScreen.kt`:
   - Line 185: Guard `if (state.todayCalendarDay != null && state.todayAlert != null)` safely prevents `HomePanchangKarteBanner` from rendering when calendar data is missing, avoiding runtime NPE.
2. `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`:
   - Lines 224–225:
     ```kotlin
     val alert = uiState.value.todayAlert ?: return
     if (alert.ttsSpeechText.isBlank()) return
     ```
     Safely aborts audio toggle without crashing when `todayAlert` is null or speech text is blank.

---

## 2. Logic Chain

1. **Bug in Alert Priority (`OfflineTeluguCalendarRepositoryImpl.kt:92`)**:
   - `calculateTodayAlert` checks conditions sequentially: (1) `isAmavasya`, (2) `daysSinceLastAmavasya in 3..5`, (3) `isFestival`, (4) `isPournami`, (5) `karteDayNumber == 1`, (6) `DAY_STANDARD`.
   - Polala Amavasya falls on Shravana Amavasya (`isAmavasya == true` AND `festivalName == "పోలాల అమావాస్య"`).
   - Because check (1) precedes check (3), check (1) matches and returns `LunarAlertType.AMAVASYA`.
   - Therefore, lines 127–134 in check (3) can NEVER be executed. The specialized agricultural festival alert for `పోలాల అమావాస్య` is dead code.

2. **Incomplete Test Suite in `OfflineTeluguCalendarRepositoryTest.kt`**:
   - Requirement 1 of the challenger mandate: *"Verify that OfflineTeluguCalendarRepositoryTest truly exercises all alert types (AMAVASYA, POST_AMAVASYA_SPRAY, POURNAMI, DAY_STANDARD, FESTIVAL)"*.
   - Only 4 alert types are asserted across existing tests (`AMAVASYA`, `POST_AMAVASYA_SPRAY`, `DAY_STANDARD`, `POURNAMI`).
   - `FESTIVAL` and `KARTE_TRANSITION` alert types are completely unexercised.
   - `getTodayAlert_triggersAmavasyaAlertOnAmavasya` asserted `alert.alertType == LunarAlertType.AMAVASYA || alert.alertType == LunarAlertType.FESTIVAL`. This concealed the shadowing defect.

3. **Required Actions**:
   - In `OfflineTeluguCalendarRepositoryImpl.kt`: Move the agricultural festival block (`// 3. Agricultural Festival Alert`) ABOVE `// 1. Amavasya Day Alert` so that sacred agricultural festivals occurring on Amavasya (`పోలాల అమావాస్య`, `మహాలయ అమావాస్య`, `దీపావళి`) or Pournami (`ఏరువాక పౌర్ణమి`, `కార్తీక పౌర్ణమి`) emit their specialized `LunarAlertType.FESTIVAL` alert.
   - In `OfflineTeluguCalendarRepositoryTest.kt`:
     1. Update `getTodayAlert_triggersAmavasyaAlertOnAmavasya` to use a non-festival Amavasya (e.g. `LocalDate.of(2025, 4, 27)`, Chaitra Amavasya) and assert `assertEquals(LunarAlertType.AMAVASYA, alert.alertType)`.
     2. Add explicit unit tests for `LunarAlertType.FESTIVAL` (e.g. `LocalDate.of(2025, 6, 11)` for Eruvaka Pournami and `LocalDate.of(2025, 8, 23)` for Polala Amavasya).
     3. Add an explicit unit test for `LunarAlertType.KARTE_TRANSITION` (e.g. `LocalDate.of(2025, 6, 8)` for Mrigashira Day 1).

---

## 3. Caveats

- **Leap Years & Ephemeris Fallback**: Robustness was empirically validated for leap years (2024, 2028), full 1,095 days of 2025–2027, and fallback dates outside the span. No defects were found in date arithmetic or boundary conditions.
- **Null Safety**: `HomeDashboardScreen` and `HomeViewModel` exhibit clean defensive behavior when calendar data is null.
- **Review-Only Constraint**: In compliance with the Challenger role constraint (*"Review-only — do NOT modify implementation code"*), the implementation fix in `OfflineTeluguCalendarRepositoryImpl.kt` must be applied by the worker.

---

## 4. Conclusion

Milestone 1 cannot be approved in its current state due to:
1. **Critical Defect**: In `OfflineTeluguCalendarRepositoryImpl.kt`, `పోలాల అమావాస్య` festival alert is unreachable dead code due to premature `isAmavasya` evaluation.
2. **Missing Test Coverage**: `OfflineTeluguCalendarRepositoryTest.kt` does not exercise `LunarAlertType.FESTIVAL` and `LunarAlertType.KARTE_TRANSITION`.

**Verdict: REQUEST_CHANGES**.

---

## 5. Verification Method

To independently verify the defect and validate the upcoming fix:

1. **Inspect Current Shadowing**:
   Examine lines 91–134 of `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`. Observe that `if (day.isAmavasya)` precedes `if (day.isFestival)`.

2. **Run Empirical Challenger Stress Test**:
   ```powershell
   Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
   .\gradlew.bat --no-daemon :core:common:testDebugUnitTest --tests "com.farmsaathi.core.common.Milestone1ChallengerStressTest"
   ```
   Check `testAlert_polalaAmavasyaBugInvestigation` output in the generated test report:
   `POLALA AMAVASYA ALERT TYPE: AMAVASYA, TITLE: నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి`.

3. **Post-Fix Verification**:
   After the worker moves `// 3. Agricultural Festival Alert` above `// 1. Amavasya Day Alert` and adds the missing tests to `OfflineTeluguCalendarRepositoryTest.kt`:
   ```powershell
   .\gradlew.bat :core:common:testDebugUnitTest :feature:home:testDebugUnitTest
   ```
   All tests in `:core:common` and `:feature:home` must pass with 100% success rate.
