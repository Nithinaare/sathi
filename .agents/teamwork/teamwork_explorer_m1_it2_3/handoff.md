# Remediation Test Verification Report (Milestone 1 Iteration 2)

**Author**: Explorer 3 (`teamwork_explorer_m1_it2_3`)  
**Objective**: Investigate test suite execution and verification requirements across `core:common` and `feature:home`, formulate exact commands, assertions, and verification criteria for Worker to guarantee 100% test pass and zero integrity violations.

---

## 1. Observation

### 1.1 `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeAudioAndRecompositionChallengerTest.kt`
- **Location**: Lines 108–163, 166–199, 201–234.
- **Direct Code**:
  - Line 129: `vm.togglePanchangAudio()`
  - Line 131: `assertTrue("Audio engine should be speaking after 1st toggle", audioEngine.isSpeaking.value)`
  - Line 158: `assertEquals("Cycle $i mismatch for isSpeaking", expectedPlaying, audioEngine.isSpeaking.value)`
  - Line 184: `assertTrue(audioEngine.isSpeaking.value)`
  - Line 220: `assertTrue(audioEngine.isSpeaking.value)`
- **Failure Cause Observed**:
  In `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`:
  ```kotlin
  fun togglePanchangAudio() {
      if (isPanchangAudioPlayingFlow.value) {
          audioEngine.stop()
          isPanchangAudioPlayingFlow.value = false
          return
      }
      val alert = uiState.value.todayAlert ?: return
      if (alert.ttsSpeechText.isBlank()) return
      isPanchangAudioPlayingFlow.value = true
      audioEngine.speakDynamicText(alert.ttsSpeechText, useTelugu = true)
  }
  ```
  `uiState` is created via `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())`.
  If `togglePanchangAudio()` is called headlessly or before Compose UI attaches a subscriber, `uiState.value.todayAlert` is `null`. The method returns early without calling `audioEngine.speakDynamicText(...)`, causing assertion failures at lines 104/131, 158, and 184 in `HomeAudioAndRecompositionChallengerTest`.

### 1.2 `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`
- **Location**: Lines 63–74:
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
- **Direct Code in Implementation** (`core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt` lines 91–134):
  - Line 92: `if (day.isAmavasya)` evaluates before line 117 `if (day.isFestival && day.festivalName != null)`.
  - On Polala Amavasya (Shravana Amavasya, Aug 23, 2025, `epochDay = 20323L`), `day.isAmavasya == true` and `day.isFestival == true`.
  - Because `isAmavasya` is evaluated first, `calculateTodayAlert` immediately returns `LunarAlertType.AMAVASYA` ("నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి").
  - The sacred agro-festival branch for `పోలాల అమావాస్య` (`LunarAlertType.FESTIVAL`, mandated in `PROJECT.md` Feature 5) is unreachable dead code.
  - The test in `OfflineTeluguCalendarRepositoryTest.kt` line 60 masked this bug with an `||` disjunction.
  - `OfflineTeluguCalendarRepositoryTest.kt` has zero tests asserting `LunarAlertType.FESTIVAL` and zero tests asserting `LunarAlertType.KARTE_TRANSITION`.

### 1.3 `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeDashboardUiTest.kt`
- **Location**: 7 UI tests using Robolectric:
  1. `home dashboard displays telugu greeting quick tiles and handles expense click` (lines 84–131)
  2. `home dashboard renders english copy when locale switched` (lines 133–175)
  3. `home dashboard shows backup safety banner when records exceed 10 and no backup exists` (lines 177–221)
  4. `home dashboard shows contact role subtitle and speaks name plus role` (lines 223–274)
  5. `home dashboard displays panchang karte banner with telugu details and handles click` (lines 276–320)
  6. `home dashboard panchang banner audio button triggers tts speech` (lines 322–371)
  7. `home dashboard gracefully handles null calendar day without crashing` (lines 373–417)
- In `HomeDashboardUiTest`, `HomeDashboardScreen` actively collects `uiState`. Therefore `uiState` has a subscriber, so `uiState.value.todayAlert` is non-null when clicking "వినండి". All 7 UI tests pass.

---

## 2. Logic Chain

1. **Defect 1: Headless Audio Trigger Failure in `HomeViewModel.togglePanchangAudio()`**:
   - `togglePanchangAudio()` reads `uiState.value.todayAlert`.
   - `uiState` is produced via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())`.
   - Before subscriber attachment or when invoked in headless ViewModel unit tests, `uiState.value` equals `HomeUiState()` where `todayAlert == null`.
   - Caching `todayAlert` during `init` (e.g. `viewModelScope.launch(dispatchers.main) { teluguCalendarRepo.observeTodayAlert(todayEpochDay).collect { cachedAlert = it } }`) and reading `cachedAlert ?: uiState.value.todayAlert ?: return` ensures reliable audio triggering under all collection states.

2. **Defect 2: Polala Amavasya Shadowing in `OfflineTeluguCalendarRepositoryImpl.kt`**:
   - `calculateTodayAlert` currently checks: (1) `isAmavasya`, (2) `postAmavasya in 3..5`, (3) `isFestival`, (4) `isPournami`, (5) `karteDayNumber == 1`, (6) `DAY_STANDARD`.
   - Polala Amavasya is both an Amavasya and a Festival. Because check (1) precedes check (3), check (1) absorbs the event, rendering the `పోలాల అమావాస్య` festival branch unreachable dead code.
   - Moving `if (day.isFestival && day.festivalName != null)` to evaluate before `isAmavasya` and `isPournami` guarantees that sacred agricultural festivals (`పోలాల అమావాస్య`, `ఏరువాక పౌర్ణమి`, `కనుమ`) emit `LunarAlertType.FESTIVAL`.

3. **Defect 3: Incomplete & Masking Assertions in `OfflineTeluguCalendarRepositoryTest.kt`**:
   - `assertTrue(alert.alertType == LunarAlertType.AMAVASYA || alert.alertType == LunarAlertType.FESTIVAL)` masked Defect 2.
   - The test must be split:
     - Verify standard Amavasya on `2025-04-27` (Chaitra Amavasya) with `assertEquals(LunarAlertType.AMAVASYA, alert.alertType)`.
     - Verify Polala Amavasya on `2025-08-23` with `assertEquals(LunarAlertType.FESTIVAL, alert.alertType)`.
     - Verify Eruvaka Pournami on `2025-06-11` with `assertEquals(LunarAlertType.FESTIVAL, alert.alertType)`.
     - Verify Karte Transition on `2025-06-08` (Mrigashira Day 1) with `assertEquals(LunarAlertType.KARTE_TRANSITION, alert.alertType)`.

---

## 3. Caveats

- **Robolectric & Windows File Locking**: Running Gradle with `--rerun-tasks` while daemons hold file locks on Windows can fail with zip packaging errors on `model.jar`. Always run `.\gradlew.bat --stop` or use `--no-daemon` and `cleanTestDebugUnitTest`.
- **Integrity Mode Constraint**: As Explorer, this investigation is strictly read-only. The code modifications and test additions must be applied by Worker.

---

## 4. Conclusion

Remediating Milestone 1 requires three concrete actions from the Worker:
1. In `OfflineTeluguCalendarRepositoryImpl.kt`: Move the agricultural festival alert block above the Amavasya alert block.
2. In `HomeViewModel.kt`: Cache `todayAlert` during `init` to decouple `togglePanchangAudio()` from `WhileSubscribed` lifecycle collection.
3. In `OfflineTeluguCalendarRepositoryTest.kt`: Fix the permissive `||` assertion and add dedicated tests for `FESTIVAL` (Polala Amavasya, Eruvaka Pournami) and `KARTE_TRANSITION`.

---

## 5. Verification Method (Step-by-Step Instructions for Worker)

### Step 5.1: Worker Implementation Steps

#### A. In `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`
Reorder `calculateTodayAlert` alert checks so `day.isFestival` takes precedence over generic Amavasya and Pournami:
```kotlin
// 1. Agricultural Festival Alert (Check FIRST so sacred agro-festivals like Polala Amavasya or Eruvaka Pournami are not shadowed)
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
            subtitle = "రైతులకు శుభదినం • పనులను సంతోషంగా నిర్వహించండి",
            ttsSpeechText = "నేడు ${day.festivalName} పండుగ. రైతు కుటుంబాలందరికీ శుభాకాంక్షలు.",
            actionText = "వివరాలు",
            isPriority = true,
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
...
```

#### B. In `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`
Cache `todayAlert` to eliminate `WhileSubscribed` lifecycle race condition in `togglePanchangAudio()`:
1. Add property:
   ```kotlin
   private var cachedTodayAlert: LunarDayAlert? = null
   ```
2. In `init`:
   ```kotlin
   init {
       refreshBackupCheckCounts()
       viewModelScope.launch(dispatchers.main) {
           teluguCalendarRepo.observeTodayAlert(todayEpochDay).collect { alert ->
               cachedTodayAlert = alert
           }
       }
       viewModelScope.launch {
           audioEngine.isSpeaking.collect { speaking ->
               if (!speaking) {
                   isPanchangAudioPlayingFlow.value = false
               }
           }
       }
   }
   ```
3. In `togglePanchangAudio()`:
   ```kotlin
   fun togglePanchangAudio() {
       if (isPanchangAudioPlayingFlow.value) {
           audioEngine.stop()
           isPanchangAudioPlayingFlow.value = false
           return
       }
       val alert = cachedTodayAlert ?: uiState.value.todayAlert ?: return
       if (alert.ttsSpeechText.isBlank()) return
       isPanchangAudioPlayingFlow.value = true
       audioEngine.speakDynamicText(alert.ttsSpeechText, useTelugu = true)
   }
   ```

#### C. In `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`
Update existing test and add new tests:
```kotlin
    @Test
    fun getTodayAlert_triggersAmavasyaAlertOnStandardAmavasya() = runTest {
        // Chaitra Amavasya 2025: April 27, 2025 (epochDay = 20205)
        val amavasyaEpoch = 20205L
        val alert = repository.getTodayAlert(amavasyaEpoch)

        assertEquals(LunarAlertType.AMAVASYA, alert.alertType)
        assertTrue(alert.title.contains("అమావాస్య"))
        assertTrue(alert.isPriority)
    }

    @Test
    fun getTodayAlert_triggersPolalaAmavasyaFestivalAlert() = runTest {
        // Shravana Amavasya 2025 (Polala Amavasya): Aug 23, 2025 (epochDay = 20323)
        val polalaEpoch = 20323L
        val alert = repository.getTodayAlert(polalaEpoch)

        assertEquals(LunarAlertType.FESTIVAL, alert.alertType)
        assertTrue(alert.title.contains("పోలాల అమావాస్య"))
        assertTrue(alert.subtitle.contains("పశువుల"))
        assertTrue(alert.isPriority)
    }

    @Test
    fun getTodayAlert_triggersEruvakaPournamiFestivalAlert() = runTest {
        // Jyeshtha Pournami (Eruvaka Pournami): June 11, 2025 (epochDay = 20249)
        val eruvakaEpoch = 20249L
        val alert = repository.getTodayAlert(eruvakaEpoch)

        assertEquals(LunarAlertType.FESTIVAL, alert.alertType)
        assertTrue(alert.title.contains("ఏరువాక పౌర్ణమి"))
        assertTrue(alert.isPriority)
    }

    @Test
    fun getTodayAlert_triggersKarteTransitionAlertOnFirstDay() = runTest {
        // Mrigashira Day 1: June 8, 2025 (epochDay = 20246)
        val mrigashiraDay1Epoch = 20246L
        val alert = repository.getTodayAlert(mrigashiraDay1Epoch)

        assertEquals(LunarAlertType.KARTE_TRANSITION, alert.alertType)
        assertTrue(alert.title.contains("కార్తె ప్రారంభం"))
        assertTrue(alert.isPriority)
    }
```

### Step 5.2: Verification Commands for Worker

1. **Clean Test Cache**:
   ```powershell
   Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
   .\gradlew.bat --stop
   .\gradlew.bat :core:common:cleanTestDebugUnitTest :feature:home:cleanTestDebugUnitTest
   ```

2. **Kotlin Compilation**:
   ```powershell
   .\gradlew.bat :core:model:compileKotlin :core:common:compileDebugKotlin :feature:home:compileDebugKotlin
   ```
   **Pass Criteria**: `BUILD SUCCESSFUL` with 0 warnings/errors.

3. **Run `:core:common` Unit Tests**:
   ```powershell
   .\gradlew.bat --no-daemon :core:common:testDebugUnitTest
   ```
   **Pass Criteria**:
   - `OfflineTeluguCalendarRepositoryTest`: 11 passed, 0 failed.
   - `Milestone1ChallengerStressTest`: 13 passed, 0 failed.
   - `CoreCommonContractsTest`: 5 passed, 0 failed.
   - Total `:core:common`: 29 passed, 0 failed.

4. **Run `:feature:home` Unit & UI Tests**:
   ```powershell
   .\gradlew.bat --no-daemon :feature:home:testDebugUnitTest
   ```
   **Pass Criteria**:
   - `HomeDashboardUiTest`: 7 passed, 0 failed.
   - `HomeAudioAndRecompositionChallengerTest`: 8 passed, 0 failed.
   - Total `:feature:home`: 15 passed, 0 failed.

5. **Full Multi-Module Test Suite Verification**:
   ```powershell
   .\gradlew.bat --no-daemon :core:common:testDebugUnitTest :feature:home:testDebugUnitTest
   ```
   **Pass Criteria**: Total 44/44 tests pass with `BUILD SUCCESSFUL` and 0 failures.
