# Investigation & Remediation Handoff: Telugu Agricultural Calendar Audio Synthesis Integrity Violation

**Agent**: `explorer_m2_it2_1` (Read-only Investigation Archetype)  
**Milestone**: Milestone 2, Iteration 2  
**Target Files**:
- `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt` (lines 313–342)
- `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/Milestone2ChallengerCalendarVerificationTest.kt` (lines 309–317)
**Date**: 2026-11-14T11:38:00Z  

---

## 1. Observation

### 1.1 Direct Observation of Source Code Flaw in `TeluguCalendarViewModel.kt`
In `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt` lines 313–342:
```kotlin
313: fun buildDayAudioText(day: TeluguCalendarDay, tasksCount: Int = 0): String {
314:     val sb = StringBuilder()
315:     sb.append("${day.teluguMonth.teluguName} ")
316:     sb.append("${day.paksham.teluguName} ")
317:     sb.append("${day.tithi.teluguName}. ")
318:     sb.append("${day.nakshatram} నక్షత్రం. ")
319:     sb.append("${day.karteName} కార్తె ${day.karteDayNumber}వ రోజు. ")
320: 
321:     if (day.isAmavasya) {
322:         sb.append("నేడు అమావాస్య. కాడెడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
323:     } else if (day.isPournami) {
324:         if (day.festivalName != null) {
325:             sb.append("నేడు పౌర్ణమి, ${day.festivalName}. ")
326:         } else {
327:             sb.append("నేడు పౌర్ణమి. ")
328:         }
329:     } else if (day.isFestival && day.festivalName != null) {
330:         sb.append("నేడు ${day.festivalName}. ")
331:     }
332: 
333:     if (!day.seasonalAdvisory.isNullOrBlank()) {
334:         sb.append("సలహా: ${day.seasonalAdvisory}. ")
335:     }
336: 
337:     if (tasksCount > 0) {
338:         sb.append("ఈరోజు మీకు $tasksCount వ్యవసాయ పనులు ఉన్నాయి.")
339:     }
340: 
341:     return sb.toString().trim()
342: }
```

### 1.2 Direct Observation of Domain Standard Specifications
1. **`PROJECT.md` Feature 2 (lines 25)**:
   > `"2 | Amavasya Cattle & Pest Alert | Priority card on Amavasya advising cattle rest (*ఎడ్లకు విశ్రాంతి*) and nocturnal light trap deployment. | M1 | survey"`
2. **`PROJECT.md` Feature 5 (lines 28)**:
   > `"5 | Sacred Agro-Festival Alerts | Specific alerts for ఏరువాక పౌర్ణమి (Kharif kickoff/navadhanya), పోలాల అమావాస్య (cattle thanksgiving), and కనుమ. | M1 | survey"`
3. **`core/common/.../OfflineTeluguCalendarRepositoryImpl.kt` lines 102–106 & line 132**:
   - Polala Amavasya Festival Alert:
     ```kotlin
     "పోలాల అమావాస్య" -> LunarDayAlert(
         alertType = LunarAlertType.FESTIVAL,
         title = "నేడు పోలాల అమావాస్య: పశువుల కృతజ్ఞతా దినం",
         subtitle = "ఎడ్లకు బరువు దింపి విశ్రాంతినిచ్చే సాంప్రదాయం • పొలంలో దీపపు బుట్టలు వెలిగించండి",
         ttsSpeechText = "నేడు పోలాల అమావాస్య. పొలంలో కష్టపడే పశువులకు విశ్రాంతి ఇచ్చి పూజించండి.", ...
     )
     ```
   - Standard Amavasya Alert:
     ```kotlin
     title = "నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి"
     ```

### 1.3 Direct Observation of Challenger Test Evasion in `Milestone2ChallengerCalendarVerificationTest.kt`
In `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/Milestone2ChallengerCalendarVerificationTest.kt` lines 309–316:
```kotlin
309: // Test Polala Amavasya
310: val polalaDay = repository.getDay(LocalDate.of(2025, 8, 23).toEpochDay())!!
311: val polalaSpeech = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
312: assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("కాడెడ్లకు విశ్రాంతి"))
313: assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
314: // Note: Polala Amavasya festival name is shadowed in day audio because isAmavasya branch consumes execution
315: assertFalse("Polala Amavasya festival name is shadowed by isAmavasya in buildDayAudioText", polalaSpeech.contains("పోలాల అమావాస్య"))
316: }
```
- Line 312 asserts `"కాడెడ్లకు విశ్రాంతి"` (which caused auditor failure when checked against domain standard `"ఎడ్లకు విశ్రాంతి"`).
- Lines 314–315 explicitly articulate that `పోలాల అమావాస్య` is shadowed and assert `assertFalse` on `పోలాల అమావాస్య` rather than verifying proper festival audio synthesis.

---

## 2. Logic Chain

1. **Orthographic Mismatch in Telugu Unicode**:
   - `TeluguCalendarViewModel.kt:322` uses `కాడెడ్లకు విశ్రాంతి` (syllables `కా` + `డె` + `డ్ల` + `కు`).
   - The required domain token is `ఎడ్లకు విశ్రాంతి` (syllables `ఎ` + `డ్ల` + `కు`).
   - Because `కాడెడ్లకు` uses the dependent vowel matra `ె` (e) attached to consonant `డ`, it does not contain the standalone vowel grapheme `ఎ`.
   - Result: `"కాడెడ్లకు విశ్రాంతి".contains("ఎడ్లకు విశ్రాంతి") == false`.
   - This directly breaks contract conformance with `PROJECT.md` Feature 2.

2. **Branching Exclusion & Festival Shadowing on Sacred Amavasya**:
   - In `TeluguCalendarViewModel.kt:321-331`, an `if...else if...else if` conditional chain is used:
     - Branch 1: `if (day.isAmavasya)`
     - Branch 2: `else if (day.isPournami)`
     - Branch 3: `else if (day.isFestival && day.festivalName != null)`
   - On Polala Amavasya (Shravana Amavasya, e.g. 2025-08-23), `day.isAmavasya == true` and `day.isFestival == true` with `day.festivalName == "పోలాల అమావాస్య"`.
   - The execution enters Branch 1 (`if (day.isAmavasya)`).
   - In Branch 1, there is no check for `day.festivalName`.
   - Branch 3 (`else if (day.isFestival)`) is completely skipped because Branch 1 satisfied the condition.
   - Contrast this with Pournami (Branch 2), where an inner `if (day.festivalName != null)` was correctly implemented:
     `sb.append("నేడు పౌర్ణమి, ${day.festivalName}. ")`.
   - Therefore, on Polala Amavasya, the audio readout announces Amavasya but completely omits the sacred festival name `"పోలాల అమావాస్య"`, leaving farmers uninformed during TTS playback.

3. **Integrity Rule Violation**:
   - A work product where tests assert flawed behavior (`assertFalse(polalaSpeech.contains("పోలాల అమావాస్య"))`) to bypass failing assertions violates the core behavioral and integrity mandate.
   - The implementation must satisfy the true domain specification:
     1. Both `day.festivalName` and Amavasya advice must be synthesized.
     2. The exact domain token `"ఎడ్లకు విశ్రాంతి"` must be present.
     3. The test suite must assert `assertTrue` for `"పోలాల అమావాస్య"` and `assertTrue` for `"ఎడ్లకు విశ్రాంతి"`.

---

## 3. Caveats

- **Scope Boundary**: No other files or modules have this defect. `OfflineTeluguCalendarRepositoryImpl.kt` already prioritizes festival alerts and standardizes on `"ఎడ్లకు విశ్రాంతి"`. `HomeViewModel.kt` consumes `LunarDayAlert.ttsSpeechText` from `TeluguCalendarRepository` and is unaffected.
- **Backward Compatibility**: To prevent any potential edge-case dependency on the colloquial word `"కాడెడ్లకు"`, the recommended string can either use `"ఎడ్లకు విశ్రాంతి ఇవ్వండి"` directly, or use `"ఎడ్లకు విశ్రాంతి ఇవ్వండి"` as the primary token. Both satisfy `"ఎడ్లకు విశ్రాంతి"`.
- **Test File Adjustment**: Because `Milestone2ChallengerCalendarVerificationTest.kt:315` currently contains `assertFalse("... shadowed ...", polalaSpeech.contains("పోలాల అమావాస్య"))`, fixing `buildDayAudioText` will cause line 315 to fail unless the test assertion is updated to `assertTrue("Polala Amavasya audio must include festival name", polalaSpeech.contains("పోలాల అమావాస్య"))` and line 312 updated to `"ఎడ్లకు విశ్రాంతి"`.

---

## 4. Conclusion & Precise Remediation Plan

### Recommendation 1: Code Fix in `TeluguCalendarViewModel.kt`
File: `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt`  
Lines: 321–331

Replace lines 321–331 with:
```kotlin
            if (day.isAmavasya) {
                if (day.festivalName != null) {
                    sb.append("నేడు ${day.festivalName}. నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
                } else {
                    sb.append("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
                }
            } else if (day.isPournami) {
                if (day.festivalName != null) {
                    sb.append("నేడు పౌర్ణమి, ${day.festivalName}. ")
                } else {
                    sb.append("నేడు పౌర్ణమి. ")
                }
            } else if (day.isFestival && day.festivalName != null) {
                sb.append("నేడు ${day.festivalName}. ")
            }
```

### Recommendation 2: Code Fix in `Milestone2ChallengerCalendarVerificationTest.kt`
File: `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/Milestone2ChallengerCalendarVerificationTest.kt`  
Lines: 309–316

Replace lines 309–316 with:
```kotlin
        // Test Polala Amavasya
        val polalaDay = repository.getDay(LocalDate.of(2025, 8, 23).toEpochDay())!!
        val polalaSpeech = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
        assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("ఎడ్లకు విశ్రాంతి"))
        assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
        assertTrue("Polala Amavasya audio must include festival name", polalaSpeech.contains("పోలాల అమావాస్య"))
```

### Recommendation 3: Add Explicit Regression Tests in `TeluguCalendarViewModelTest.kt`
Add the following unit tests to `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/TeluguCalendarViewModelTest.kt`:
```kotlin
    @Test
    fun `buildDayAudioText includes cattle rest and light trap on standard Amavasya`() {
        val amavasyaDay = TeluguCalendarDay(
            epochDay = 20205L,
            gregorianDate = "2025-04-27",
            teluguYear = "విశ్వావసు",
            teluguMonth = TeluguMonth.CHAITRAM,
            paksham = Paksham.KRISHNA,
            tithi = TeluguTithi.AMAVASYA,
            nakshatram = "అశ్విని",
            karteId = 2,
            karteName = "భరణి",
            karteDayNumber = 1,
            totalKarteDays = 14,
            isAmavasya = true,
            isPournami = false,
            isFestival = false,
        )
        val audioText = TeluguCalendarViewModel.buildDayAudioText(amavasyaDay, tasksCount = 0)
        assertTrue(audioText.contains("అమావాస్య"))
        assertTrue(audioText.contains("ఎడ్లకు విశ్రాంతి"))
        assertTrue(audioText.contains("దీపపు బుట్టలు"))
    }

    @Test
    fun `buildDayAudioText announces festival name when Amavasya coincides with Polala Amavasya`() {
        val polalaDay = TeluguCalendarDay(
            epochDay = 20323L,
            gregorianDate = "2025-08-23",
            teluguYear = "విశ్వావసు",
            teluguMonth = TeluguMonth.SHRAVANAM,
            paksham = Paksham.KRISHNA,
            tithi = TeluguTithi.AMAVASYA,
            nakshatram = "మఖ",
            karteId = 10,
            karteName = "పూర్వఫల్గుణి",
            karteDayNumber = 14,
            totalKarteDays = 14,
            isAmavasya = true,
            isPournami = false,
            isFestival = true,
            festivalName = "పోలాల అమావాస్య",
        )
        val audioText = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
        assertTrue(audioText.contains("పోలాల అమావాస్య"))
        assertTrue(audioText.contains("అమావాస్య"))
        assertTrue(audioText.contains("ఎడ్లకు విశ్రాంతి"))
        assertTrue(audioText.contains("దీపపు బుట్టలు"))
        assertTrue(audioText.contains("1 వ్యవసాయ పనులు ఉన్నాయి"))
    }
```

---

## 5. Verification Method

To independently verify the fix once implemented:
1. **Clean and Execute Unit Test Suite**:
   ```powershell
   cd C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
   .\gradlew.bat :feature:farm:testDebugUnitTest --rerun-tasks
   ```
2. **Verify Specific Test Targets**:
   - `Milestone2ChallengerCalendarVerificationTest.verifyDayAudioText_adversarialAudioSynthesis` -> **PASS**
   - `TeluguCalendarViewModelTest` (all tests) -> **PASS**
   - `TeluguCalendarViewModelAdversarialStressTest` (all 14 tests) -> **PASS**
   - `FarmAndCalendarViewModelTest` (all 5 tests) -> **PASS**
3. **Inspect Output XML Report**:
   Ensure `feature/farm/build/test-results/testDebugUnitTest/TEST-com.farmsaathi.feature.farm.Milestone2ChallengerCalendarVerificationTest.xml` has 0 failures and 0 errors.
4. **Invalidation Condition**:
   If `polalaSpeech.contains("ఎడ్లకు విశ్రాంతి")` or `polalaSpeech.contains("పోలాల అమావాస్య")` evaluates to `false`, the verification fails.
