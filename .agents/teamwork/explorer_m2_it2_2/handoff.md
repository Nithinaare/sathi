# Remediation Investigation Report: Polala Amavasya Audio Synthesis & Test Alignment

**Target Milestone**: Milestone 2 (Telugu Agricultural Calendar & Tasks)  
**Author**: `explorer_m2_it2_2`  
**State Folder**: `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\explorer_m2_it2_2`  
**Working Directory**: `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi`  
**Date**: 2026-11-14T11:37:00Z  

---

## 1. Observation

### 1.1 Direct Observation of Test Challenger Assertion in `Milestone2ChallengerCalendarVerificationTest.kt`
In `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/Milestone2ChallengerCalendarVerificationTest.kt` (lines 305–317):
```kotlin
        // Test Eruvaka Pournami
        val eruvakaDay = repository.getDay(LocalDate.of(2025, 6, 11).toEpochDay())!!
        val eruvakaSpeech = TeluguCalendarViewModel.buildDayAudioText(eruvakaDay, tasksCount = 0)
        assertTrue("Eruvaka Pournami audio must include festival name", eruvakaSpeech.contains("ఏరువాక పౌర్ణమి"))

        // Test Polala Amavasya
        val polalaDay = repository.getDay(LocalDate.of(2025, 8, 23).toEpochDay())!!
        val polalaSpeech = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
        assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("కాడెడ్లకు విశ్రాంతి"))
        assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
        // Note: Polala Amavasya festival name is shadowed in day audio because isAmavasya branch consumes execution
        assertFalse("Polala Amavasya festival name is shadowed by isAmavasya in buildDayAudioText", polalaSpeech.contains("పోలాల అమావాస్య"))
    }
```
Direct observations:
1. Line 312 asserts `polalaSpeech.contains("కాడెడ్లకు విశ్రాంతి")`.
2. Line 315 asserts `assertFalse("Polala Amavasya festival name is shadowed by isAmavasya in buildDayAudioText", polalaSpeech.contains("పోలాల అమావాస్య"))`.

### 1.2 Direct Observation in Source Code (`TeluguCalendarViewModel.kt`)
In `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt` (lines 313–332):
```kotlin
        fun buildDayAudioText(day: TeluguCalendarDay, tasksCount: Int = 0): String {
            val sb = StringBuilder()
            sb.append("${day.teluguMonth.teluguName} ")
            sb.append("${day.paksham.teluguName} ")
            sb.append("${day.tithi.teluguName}. ")
            sb.append("${day.nakshatram} నక్షత్రం. ")
            sb.append("${day.karteName} కార్తె ${day.karteDayNumber}వ రోజు. ")

            if (day.isAmavasya) {
                sb.append("నేడు అమావాస్య. కాడెడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
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
Direct observations:
1. Line 322 appends: `"నేడు అమావాస్య. కాడెడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. "`.
2. When `day.isAmavasya == true`, the `if (day.isAmavasya)` block executes unconditionally. The `else if (day.isFestival && day.festivalName != null)` block (lines 329–331) is unreachable, completely suppressing the festival name when a festival coincides with Amavasya (e.g., Polala Amavasya on Shravana Amavasya, 2025-08-23).

### 1.3 Direct Observation in Project Specification (`PROJECT.md`)
In `PROJECT.md`:
- Line 25: `Feature 2: Priority card on Amavasya advising cattle rest (*ఎడ్లకు విశ్రాంతి*) and nocturnal light trap deployment.`
- Line 28: `Feature 5: Sacred Agro-Festival Alerts: Specific alerts for ఏరువాక పౌర్ణమి (Kharif kickoff/navadhanya), పోలాల అమావాస్య (cattle thanksgiving), and కనుమ.`

### 1.4 Direct Observation of Forensic Audit Report (`auditor_m2_1/handoff.md`)
In `.agents/teamwork/auditor_m2_1/handoff.md`:
- Check 5: Domain Spec Conformity & Festival Audio Synthesis: **FAIL**
  `"TeluguCalendarViewModel.buildDayAudioText() uses non-standard phrase కాడెడ్లకు విశ్రాంతి instead of domain standard ఎడ్లకు విశ్రాంతి (PROJECT.md Feature 2 & OfflineTeluguCalendarRepositoryImpl.kt), and fails to announce festival name when Amavasya coincides with a sacred festival (such as Polala Amavasya)."`
- Remediation Guidance:
  `"1. In TeluguCalendarViewModel.kt (buildDayAudioText): Replace కాడెడ్లకు విశ్రాంతి with ఎడ్లకు విశ్రాంతి ... Allow festival announcements on Amavasya when day.festivalName != null"`

### 1.5 Direct Observation of Audio Assertions in `TeluguCalendarViewModelTest.kt`
In `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/TeluguCalendarViewModelTest.kt`:
Lines 498–530:
```kotlin
    @Test
    fun `togglePanchangAudio speaks authentic Telugu day details when day details bottom sheet is visible`() = runTest(testDispatcher) {
        val vm = createViewModel()
        ...
        vm.selectDay(baseEpochDay)
        ...
        vm.togglePanchangAudio()
        ...
        val spoken = audioEngine.lastSpokenText ?: ""
        assertTrue("Speech must contain Telugu month or paksham", spoken.contains("ఆషాఢం") || spoken.contains("పక్షం"))
        assertTrue(audioEngine.isSpeaking.value)
```
Lines 533–552:
```kotlin
    @Test
    fun `togglePanchangAudio in timeline mode speaks current karte wisdom`() = runTest(testDispatcher) {
        ...
        val spoken = audioEngine.lastSpokenText ?: ""
        assertTrue("Speech must contain karte name or proverb", spoken.contains("కార్తె") || spoken.contains("సామెత"))
```
Lines 555–571:
```kotlin
    @Test
    fun `speakKarte speaks individual karte wisdom from catalogue`() = runTest(testDispatcher) {
        ...
        assertTrue(audioEngine.lastSpokenText!!.contains("మృగశిర"))
        assertTrue(audioEngine.lastSpokenText!!.contains(mrigashira.proverb))
```
Lines 574–593:
```kotlin
    @Test
    fun `audio state automatically resets when engine finishes speaking naturally`() = runTest(testDispatcher) {
        ...
        assertFalse(vm.uiState.value.isAudioSpeaking)
```
Direct observations:
1. `TeluguCalendarViewModelTest.kt` has 4 audio tests.
2. The only day-audio test (`togglePanchangAudio speaks authentic Telugu day details...`) evaluates `baseEpochDay` (July 15, 2026, which is Ashadha Pournami) and only asserts `spoken.contains("ఆషాఢం") || spoken.contains("పక్షం")`.
3. None of the tests in `TeluguCalendarViewModelTest.kt` assert on Amavasya phrasing, cattle rest phrasing (`ఎడ్లకు విశ్రాంతి` / `కాడెడ్లకు విశ్రాంతి`), or festival names on Amavasya (`పోలాల అమావాస్య`).

---

## 2. Logic Chain

1. **Orthographic Analysis of Telugu Script**:
   - `కాడెడ్లకు` is formed by compounding `కాడి` (yoke) + `ఎడ్లు` (bullocks) + `కు` (dative suffix).
   - In Telugu orthography, the consonant cluster `డె` (Da + E vowel sign / matra `ె`) is a single akshara glyph; it does NOT contain the independent vowel letter `ఎ` (U+0C0E).
   - Therefore, `polalaSpeech.contains("ఎడ్లకు విశ్రాంతి")` returns `false` when `polalaSpeech` contains `కాడెడ్లకు విశ్రాంతి`.
   - `PROJECT.md` Feature 2 specifically establishes the domain requirement as `ఎడ్లకు విశ్రాంతి`.

2. **Branch Shadowing Defect in Audio Synthesis**:
   - Polala Amavasya occurs on Shravana Amavasya (e.g. 2025-08-23). On this day, `day.isAmavasya == true`, `day.isFestival == true`, and `day.festivalName == "పోలాల అమావాస్య"`.
   - In `TeluguCalendarViewModel.kt:321-331`, an `if ... else if` structure is employed:
     ```kotlin
     if (day.isAmavasya) { ... }
     else if (day.isPournami) { ... }
     else if (day.isFestival && day.festivalName != null) { ... }
     ```
   - Because `day.isAmavasya` is the first branch, execution enters line 321 and skips lines 329–331.
   - Consequently, the festival name is omitted from TTS audio on Polala Amavasya.

3. **Challenger Assertion Evolution & Correction**:
   - In `Milestone2ChallengerCalendarVerificationTest.kt`, line 312 was written to assert `polalaSpeech.contains("కాడెడ్లకు విశ్రాంతి")`, and line 315 was added with `assertFalse(..., polalaSpeech.contains("పోలాల అమావాస్య"))` to reflect the observed shadowing behavior.
   - When `buildDayAudioText` is remediated:
     1. Festival names must be announced on Amavasya days when `day.festivalName != null` (just as Pournami festivals like Eruvaka Pournami are announced).
     2. Standard domain phrase `ఎడ్లకు విశ్రాంతి` must replace `కాడెడ్లకు విశ్రాంతి`.
     3. Light trap advice `దీపపు బుట్టలు` must be preserved.
   - Therefore, `Milestone2ChallengerCalendarVerificationTest.kt` lines 312–315 must be updated to:
     ```kotlin
     assertTrue("Polala Amavasya audio must include festival name", polalaSpeech.contains("పోలాల అమావాస్య"))
     assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("ఎడ్లకు విశ్రాంతి"))
     assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
     ```

4. **Impact Analysis on `TeluguCalendarViewModelTest.kt`**:
   - Detailed inspection of all 861 lines and 21 unit tests in `TeluguCalendarViewModelTest.kt` confirms:
     - No test invokes `buildDayAudioText` directly.
     - `togglePanchangAudio` invokes `buildDayAudioText` on `baseEpochDay` (Pournami), asserting only `spoken.contains("ఆషాఢం") || spoken.contains("పక్షం")`.
     - No test asserts on Amavasya text or Polala Amavasya audio text.
   - Conclusion: **Zero assertions in `TeluguCalendarViewModelTest.kt` are broken or require modification.**

---

## 3. Caveats

1. **UI Layer Consistency in `TeluguCalendarScreen.kt`**:
   - In `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/TeluguCalendarScreen.kt:659`:
     ```kotlin
     text = "నేడు అమావాస్య: కాడెడ్లకు విశ్రాంతి ఇవ్వండి • రాత్రి పొలంలో దీపపు బుట్టలు (Light Traps) అమర్చండి."
     ```
   - In `TeluguCalendarScreen.kt:668`:
     ```kotlin
     if (day.isPournami && day.festivalName != null) { ... }
     ```
   - The Day Details bottom sheet UI currently has `కాడెడ్లకు` in the visual warning card, and only renders a festival card for Pournami festivals. While no tests currently assert against this UI string, the implementer may optionally align line 659 to `ఎడ్లకు విశ్రాంతి` and allow festival card rendering when `day.festivalName != null` regardless of lunar phase.
2. **Offline Repository Alert Text**:
   - `OfflineTeluguCalendarRepositoryImpl.kt` lines 98 and 134 use `కాడెడ్లకు` in TTS alert text, while line 105 subtitle uses `ఎడ్లకు బరువు దింపి విశ్రాంతినిచ్చే సాంప్రదాయం` and line 132 title uses `నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి`.
   - The repository alert tests in `core:common` pass and test `alert.title.contains("పోలాల అమావాస్య")` (lines 80, 148). They do not conflict with the ViewModel's `buildDayAudioText`.

---

## 4. Conclusion

### Actionable Remediation Plan for Implementer Agent (`worker_m2_it2_1`)

#### Change 1: `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt`
In `buildDayAudioText(day: TeluguCalendarDay, tasksCount: Int = 0)` (lines 321–331):

**Target Code to Replace**:
```kotlin
            if (day.isAmavasya) {
                sb.append("నేడు అమావాస్య. కాడెడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
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

**Replacement Code**:
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

#### Change 2: `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/Milestone2ChallengerCalendarVerificationTest.kt`
In `verifyDayAudioText_adversarialAudioSynthesis` (lines 309–316):

**Target Code to Replace**:
```kotlin
        // Test Polala Amavasya
        val polalaDay = repository.getDay(LocalDate.of(2025, 8, 23).toEpochDay())!!
        val polalaSpeech = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
        assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("కాడెడ్లకు విశ్రాంతి"))
        assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
        // Note: Polala Amavasya festival name is shadowed in day audio because isAmavasya branch consumes execution
        assertFalse("Polala Amavasya festival name is shadowed by isAmavasya in buildDayAudioText", polalaSpeech.contains("పోలాల అమావాస్య"))
```

**Replacement Code**:
```kotlin
        // Test Polala Amavasya
        val polalaDay = repository.getDay(LocalDate.of(2025, 8, 23).toEpochDay())!!
        val polalaSpeech = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
        assertTrue("Polala Amavasya audio must include festival name", polalaSpeech.contains("పోలాల అమావాస్య"))
        assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("ఎడ్లకు విశ్రాంతి"))
        assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
```

#### Change 3: `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/TeluguCalendarViewModelTest.kt`
- **Existing Assertions**: No existing assertions require alteration.
- **Recommended Addition**: Implementer can optionally add a direct unit test to `TeluguCalendarViewModelTest.kt` to reinforce test suite coverage:
```kotlin
    @Test
    fun `buildDayAudioText synthesizes festival name and cattle rest advice on Polala Amavasya`() {
        val polalaDay = TeluguCalendarDay(
            epochDay = 20323L,
            gregorianDate = "2025-08-23",
            teluguYear = "విశ్వావసు",
            teluguMonth = TeluguMonth.SHRAVANAM,
            paksham = Paksham.KRISHNA,
            tithi = TeluguTithi.AMAVASYA,
            nakshatram = "మఖ",
            karteId = 10,
            karteName = "మఖ",
            karteDayNumber = 7,
            totalKarteDays = 14,
            isAmavasya = true,
            isPournami = false,
            isFestival = true,
            festivalName = "పోలాల అమావాస్య",
        )
        val speech = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
        assertTrue("Must include festival name", speech.contains("పోలాల అమావాస్య"))
        assertTrue("Must include cattle rest advice", speech.contains("ఎడ్లకు విశ్రాంతి"))
        assertTrue("Must include light trap advice", speech.contains("దీపపు బుట్టలు"))
        assertTrue("Must include task count", speech.contains("1 వ్యవసాయ పనులు ఉన్నాయి"))
    }
```

---

## 5. Verification Method

To independently verify this remediation:
1. Apply the changes above to `TeluguCalendarViewModel.kt` and `Milestone2ChallengerCalendarVerificationTest.kt`.
2. Run the Gradle unit test suite for `:feature:farm`:
   ```powershell
   Set-Location C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
   .\gradlew.bat :feature:farm:testDebugUnitTest
   ```
3. Verify test outcomes:
   - Total tests executed: 68
   - Total failures: 0
   - `Milestone2ChallengerCalendarVerificationTest > verifyDayAudioText_adversarialAudioSynthesis` passes.
   - All tests in `TeluguCalendarViewModelTest` pass.
4. Invalidation condition:
   - Any test failure in `verifyDayAudioText_adversarialAudioSynthesis` indicating missing festival name (`పోలాల అమావాస్య`) or missing advisory (`ఎడ్లకు విశ్రాంతి`).
