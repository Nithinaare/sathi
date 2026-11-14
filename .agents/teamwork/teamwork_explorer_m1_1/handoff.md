# Handoff Report: M1 Core Repository & Unit Test Investigation

## 1. Observation

### File & Line References
- **Test File**: `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`
  - Lines 88–96:
    ```kotlin
    @Test
    fun getTodayAlert_standardDayProducesCleanInformativeAlert() = runTest {
        // A regular day in Ashadha (e.g., July 10, 2025)
        val epochDay = LocalDate.of(2025, 7, 10).toEpochDay()
        val alert = repository.getTodayAlert(epochDay)

        assertEquals(LunarAlertType.DAY_STANDARD, alert.alertType)
        assertFalse(alert.isPriority)
        assertTrue(alert.ttsSpeechText.isNotEmpty())
    }
    ```
- **Repository Implementation**: `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`
  - Lines 457 (`LUNAR_MONTH_SPANS`):
    ```kotlin
    LunarSpan("విశ్వావసు", TeluguMonth.ASHADHAM, 20265L, 20279L, 20293L), // 2025-06-26 to 2025-07-24
    ```
  - Lines 294–296 (`calculateLunarPanchang`):
    ```kotlin
    val tithi = when {
        epochDay == span.endEpochDay -> TeluguTithi.AMAVASYA
        epochDay == span.pournamiEpochDay -> TeluguTithi.POURNAMI
    ```
  - Line 213 (`calculateDay`):
    ```kotlin
    isPournami = lunarCalculation.tithi == TeluguTithi.POURNAMI,
    ```
  - Lines 154–162 (`calculateTodayAlert`):
    ```kotlin
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
    ```

### Command Execution & Verbatim Errors
1. Running `.\gradlew.bat :core:common:testDebugUnitTest`:
   - Command: `Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"; .\gradlew.bat :core:common:testDebugUnitTest`
   - Test results XML: `core/common/build/test-results/testDebugUnitTest/TEST-com.farmsaathi.core.common.OfflineTeluguCalendarRepositoryTest.xml`
   - Verbatim failure:
     ```
     OfflineTeluguCalendarRepositoryTest > getTodayAlert_standardDayProducesCleanInformativeAlert FAILED
         java.lang.AssertionError: expected:<DAY_STANDARD> but was:<POURNAMI>
         at org.junit.Assert.fail(Assert.java:89)
         at org.junit.Assert.failNotEquals(Assert.java:835)
         at org.junit.Assert.assertEquals(Assert.java:120)
         at org.junit.Assert.assertEquals(Assert.java:146)
         at com.farmsaathi.core.common.OfflineTeluguCalendarRepositoryTest$getTodayAlert_standardDayProducesCleanInformativeAlert$1.invokeSuspend(OfflineTeluguCalendarRepositoryTest.kt:93)
     ```
   - Summary: 12 tests completed across `:core:common` (`CoreCommonContractsTest`: 5 passed; `OfflineTeluguCalendarRepositoryTest`: 6 passed, 1 failed).
2. Running `.\gradlew.bat :core:model:test`:
   - Command: `Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"; .\gradlew.bat :core:model:test`
   - Result: `BUILD SUCCESSFUL in 55s. 4 actionable tasks: 2 executed, 2 up-to-date.`
   - All tests in `FinancialCalculatorsTest.kt` and `MoneyAndUnitsTest.kt` passed with 0 failures.

---

## 2. Logic Chain

1. **Epoch Day Computation**:
   - `LocalDate.of(2025, 7, 10).toEpochDay()` yields exactly `20279L`.
2. **Span Matching**:
   - In `OfflineTeluguCalendarRepositoryImpl.kt:457`, the Ashadha 2025 span is defined as:
     `LunarSpan("విశ్వావసు", TeluguMonth.ASHADHAM, 20265L, 20279L, 20293L)`.
   - Here, `startEpochDay = 20265L` (2025-06-26), `pournamiEpochDay = 20279L` (2025-07-10), and `endEpochDay = 20293L` (2025-07-24).
3. **Panchang Tithi Evaluation**:
   - In `calculateLunarPanchang`, when `epochDay == span.pournamiEpochDay` (`20279L == 20279L`), the tithi evaluates to `TeluguTithi.POURNAMI`.
   - In `calculateDay`, `isPournami` evaluates to `true`.
4. **Alert Hierarchy Interception**:
   - In `calculateTodayAlert`, alerts are evaluated in strict priority order:
     - 1. Amavasya Alert (`day.isAmavasya`) -> false.
     - 2. Post-Amavasya Spray Window (`daysSinceLastAmavasya in 3..5`) -> false (`20279 - 20264 = 15`).
     - 3. Agricultural Festival (`day.isFestival && day.festivalName != null`) -> false.
     - 4. Pournami Alert (`day.isPournami`) -> **TRUE**.
   - Step 4 returns a `LunarDayAlert` with `alertType = LunarAlertType.POURNAMI`.
   - Execution returns immediately at line 155 without reaching Step 6 (`LunarAlertType.DAY_STANDARD` at line 177).
5. **Why July 10, 2025 was Used in the Test**:
   - The test comment states: `// A regular day in Ashadha (e.g., July 10, 2025)`.
   - The test author mistakenly assumed July 10, 2025 was an ordinary, standard agricultural day in Ashadha, whereas in reality July 10, 2025 is Ashadha Pournami (Guru Purnima).
6. **Selection of July 8, 2025 (`2025-07-08`) as the Fix**:
   - `LocalDate.of(2025, 7, 8).toEpochDay()` is `20277L`.
   - Within Ashadha (`20265L..20293L`), `20277L` corresponds to Shukla Trayodashi (13th tithi, 2 days before Pournami).
   - `day.isAmavasya` is `false`.
   - `daysSinceLastAmavasya` is `13` (not in `3..5`, so no spray alert).
   - `day.isFestival` is `false`.
   - `day.isPournami` is `false` (`20277L != 20279L`).
   - `day.karteDayNumber`: Punarvasu Karte starts on July 6 (`KARTE_BOUNDARIES[6]`). July 8 is Day 3 (`3 != 1`), so no Karte entry alert.
   - All preceding conditions evaluate to `false`. Execution reaches Step 6 and produces `LunarAlertType.DAY_STANDARD`.
   - `alert.isPriority` is `false`, and `alert.ttsSpeechText` is non-empty.

---

## 3. Caveats

- **No Repository Logic Bug**: `OfflineTeluguCalendarRepositoryImpl` is operating 100% correctly according to the Chandramana ephemeris and lunar alert business logic. The bug is entirely in the unit test assertion/fixture date.
- **Festival Coverage**: Ashadha Pournami is not configured in `findFestival` (only Eruvaka Pournami in Jyeshtha and Kartika Pournami in Karthika are special agricultural festivals). Hence July 10 triggers `LunarAlertType.POURNAMI`, not `LunarAlertType.FESTIVAL`. This is expected domain design.

---

## 4. Conclusion & Actionable Recommendation for Worker

The Worker should modify `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`:

### Diff Patch / Code Replacement

**Location**: `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt` (lines 88–96)

**Before**:
```kotlin
    @Test
    fun getTodayAlert_standardDayProducesCleanInformativeAlert() = runTest {
        // A regular day in Ashadha (e.g., July 10, 2025)
        val epochDay = LocalDate.of(2025, 7, 10).toEpochDay()
        val alert = repository.getTodayAlert(epochDay)

        assertEquals(LunarAlertType.DAY_STANDARD, alert.alertType)
        assertFalse(alert.isPriority)
        assertTrue(alert.ttsSpeechText.isNotEmpty())
    }
```

**After**:
```kotlin
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
```

### Steps for Worker:
1. Open `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`.
2. Update `getTodayAlert_standardDayProducesCleanInformativeAlert` to use `LocalDate.of(2025, 7, 8)`.
3. Add `getTodayAlert_triggersPournamiAlertOnPournami` using `LocalDate.of(2025, 7, 10)` to ensure regression coverage for Pournami alert generation.
4. Run verification commands below.

---

## 5. Verification Method

To independently verify the investigation and the fix:

1. **Verify `:core:common` unit tests**:
   ```powershell
   Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
   .\gradlew.bat :core:common:testDebugUnitTest
   ```
   *Expected Result*: All 13 unit tests pass (`BUILD SUCCESSFUL`).
2. **Verify `:core:model` unit tests**:
   ```powershell
   Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
   .\gradlew.bat :core:model:test
   ```
   *Expected Result*: All model tests pass (`BUILD SUCCESSFUL`).
3. **Invalidation Conditions**:
   - If `LocalDate.of(2025, 7, 8)` returns anything other than `LunarAlertType.DAY_STANDARD`, check if Punarvasu karte entry date or Ashadha Shukla span definition was altered in `OfflineTeluguCalendarRepositoryImpl.kt`.
