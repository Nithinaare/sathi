# Milestone 1 Challenger 2 Report: TTS Audio Playback, Banner UI States & Reactive Pipeline

## Explicit Verdict: REQUEST_CHANGES

---

## 1. Observation

### 1.1 Direct Code Observations in `HomeViewModel.kt`
File: `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`
1. Lines 218–228:
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
   - Notice: `togglePanchangAudio()` reads `uiState.value.todayAlert`.
2. Lines 201–203:
   ```kotlin
   }.flowOn(dispatchers.default)
       .distinctUntilChanged()
       .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
   ```
   - `uiState` is produced using `SharingStarted.WhileSubscribed(5_000)` with initial value `HomeUiState()`.
   - In `HomeUiState()`, `todayAlert` defaults to `null`.
   - If `togglePanchangAudio()` is invoked when `uiState` has no active lifecycle collectors (e.g. before screen transitions, in decoupled event handlers, or in unit tests that test actions directly), `uiState.value.todayAlert` is always `null`, causing `togglePanchangAudio()` to silently abort without playing audio.
3. Lines 108–114:
   ```kotlin
   viewModelScope.launch {
       audioEngine.isSpeaking.collect { speaking ->
           if (!speaking) {
               isPanchangAudioPlayingFlow.value = false
           }
       }
   }
   ```
   - The audio state reset cleanly handles external TTS completions (utterance progress listener `onDone`/`onError`).
4. Line 231:
   ```kotlin
   fun toggleHomeAudio() {
       isPanchangAudioPlayingFlow.value = false
       val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
       audioEngine.speakCue(AudioCueCatalog.HOME_SUMMARY, useTe)
   }
   ```
   - Calling `toggleHomeAudio()` explicitly sets `isPanchangAudioPlayingFlow.value = false`, avoiding dual-audio collision.

### 1.2 Direct Code Observations in `HomePanchangKarteBanner.kt` & `HomeDashboardScreen.kt`
1. File: `feature/home/src/main/kotlin/com/farmsaathi/feature/home/screens/HomeDashboardScreen.kt`
   - Line 185:
     ```kotlin
     if (state.todayCalendarDay != null && state.todayAlert != null) {
         item(key = "panchang_karte_banner", contentType = "panchang_karte_banner") {
             HomePanchangKarteBanner(
                 calendarDay = state.todayCalendarDay,
                 karteInfo = state.todayKarteInfo,
                 alert = state.todayAlert,
                 isPlayingAudio = state.isPanchangAudioPlaying,
                 onPlayAudio = viewModel::togglePanchangAudio,
                 onClick = onOpenTeluguCalendar,
             )
         }
     }
     ```
   - Guarded against null `todayCalendarDay` and null `todayAlert`. Does not render placeholder or throw NPE when uninitialized.
2. File: `feature/home/src/main/kotlin/com/farmsaathi/feature/home/components/HomePanchangKarteBanner.kt`
   - Lines 49, 134:
     ```kotlin
     if (calendarDay == null || alert == null) return
     ...
     if (karteInfo != null) {
     ```
   - Gracefully handles absent `karteInfo` without runtime exceptions.
   - Lines 106–130:
     Toggles between volume icon ("వినండి") and stop icon ("ఆపండి") based on `isPlayingAudio`.

### 1.3 Shadowed Festival Alert Defect in `OfflineTeluguCalendarRepositoryImpl.kt`
File: `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`
Lines 91–101:
```kotlin
// 1. Amavasya Day Alert
if (day.isAmavasya) {
    return LunarDayAlert(
        alertType = LunarAlertType.AMAVASYA,
        title = "నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి",
...
```
Lines 117–134:
```kotlin
// 3. Agricultural Festival Alert
if (day.isFestival && day.festivalName != null) {
    return when (day.festivalName) {
        "పోలాల అమావాస్య" -> LunarDayAlert(
            alertType = LunarAlertType.FESTIVAL,
            title = "నేడు పోలాల అమావాస్య: పశువుల కృతజ్ఞతా దినం",
            subtitle = "ఎడ్లకు బరువు దింపి విశ్రాంతినిచ్చే సాంప్రదాయం • పొలంలో దీపపు బుట్టలు వెలిగించండి",
            ttsSpeechText = "నేడు పోలాల అమావాస్య. పొలంలో కష్టపడే పశువులకు విశ్రాంతి ఇచ్చి పూజించండి.",
...
```
- Polala Amavasya occurs on Shravana Amavasya (e.g. August 23, 2025, epochDay `20323L`).
- Because `if (day.isAmavasya)` is evaluated *before* `if (day.isFestival)`, on August 23, 2025 `calculateTodayAlert` immediately returns `LunarAlertType.AMAVASYA`.
- The specialized agro-festival alert for Polala Amavasya (`LunarAlertType.FESTIVAL`, mandated in `PROJECT.md` Feature 5) is unreachable dead code.
- This was confirmed empirically in `Milestone1ChallengerStressTest.kt` with output:
  `POLALA AMAVASYA ALERT TYPE: AMAVASYA, TITLE: నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి`.

### 1.4 Test Suite Execution Results
Command executed:
```powershell
Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
.\gradlew.bat --no-daemon :core:common:testDebugUnitTest :feature:home:testDebugUnitTest
```
Verbatim Result:
```
BUILD SUCCESSFUL in 2m 22s
142 actionable tasks: 1 from cache, 141 up-to-date
```
- `:core:common`: 26 tests passed (`Milestone1ChallengerStressTest`: 13 passed, `OfflineTeluguCalendarRepositoryTest`: 8 passed, `CoreCommonContractsTest`: 5 passed).
- `:feature:home`: 6 tests passed in `HomeDashboardUiTest` (and 8 tests verified in `HomeAudioAndRecompositionChallengerTest`).

---

## 2. Logic Chain

1. **Audio State Synchronization (Rapid Toggling)**:
   - When `uiState` is subscribed, `HomeViewModel.togglePanchangAudio()` accurately dispatches `audioEngine.speakDynamicText()` on the 1st click and `audioEngine.stop()` on the 2nd click.
   - Rapid sequential toggling (tested through 10 consecutive alternating cycles in `HomeAudioAndRecompositionChallengerTest`) showed zero desync, off-by-one errors, or state deadlocks.
   - Asynchronous TTS completion via `audioEngine.isSpeaking` collector correctly resets `isPanchangAudioPlayingFlow.value = false`, allowing subsequent taps on "వినండి" to restart audio without requiring a double-tap.
   - Header audio preemption (`toggleHomeAudio()`) cleanly resets `isPanchangAudioPlayingFlow` to avoid simultaneous speech playback.

2. **Compose Recomposition Stability**:
   - `HomeUiState` default values produce `todayCalendarDay = null` and `todayAlert = null`.
   - `HomeDashboardScreen` checks `state.todayCalendarDay != null && state.todayAlert != null` before adding the banner to the `LazyColumn`.
   - Initial emission with nulls does not emit the banner or cause spurious recompositions (recomposition count strictly = 1).
   - `.distinctUntilChanged()` on `uiState` prevents duplicate emissions when input flows emit unchanged values.

3. **Critical Defect: Polala Amavasya Festival Dead Code**:
   - Feature 5 in `PROJECT.md` mandates: *"Sacred Agro-Festival Alerts: Specific alerts for ఏరువాక పౌర్ణమి (Kharif kickoff/navadhanya), పోలాల అమావాస్య (cattle thanksgiving), and కనుమ."*
   - In `OfflineTeluguCalendarRepositoryImpl.kt`, `if (day.isAmavasya)` is placed ahead of `if (day.isFestival && day.festivalName != null)`.
   - Since Polala Amavasya is by definition an Amavasya (`day.isAmavasya == true`), the code path returning `LunarAlertType.FESTIVAL` with title `"నేడు పోలాల అమావాస్య: పశువుల కృతజ్ఞతా దినం"` is never reached.
   - The test in `OfflineTeluguCalendarRepositoryTest.kt` line 60 used `assertTrue(alert.alertType == LunarAlertType.AMAVASYA || alert.alertType == LunarAlertType.FESTIVAL)`, which masked this bug.
   - On Polala Amavasya, the farmer receives a generic Amavasya alert instead of the required cultural thanksgiving alert and guidance.

4. **Architectural Fragility: `WhileSubscribed` Coupling in `togglePanchangAudio()`**:
   - `togglePanchangAudio()` relies on `val alert = uiState.value.todayAlert ?: return`.
   - Because `uiState` is backed by `SharingStarted.WhileSubscribed(5_000)`, if no UI component is actively observing `uiState`, `uiState.value` holds the initial empty `HomeUiState`.
   - While `HomeDashboardScreen` collects `uiState` when rendered, any event or caller that triggers `togglePanchangAudio()` outside an active subscriber window silently fails without an error or retry mechanism.
   - Retaining `todayAlert` in a private flow or observing it directly would make audio playback resilient regardless of `WhileSubscribed` subscriber state.

---

## 3. Caveats

- Hardware TTS audio output ducking during incoming phone calls was not tested because hardware telephony services are mocked in Robolectric.
- No other caveats.

---

## 4. Conclusion

While audio playback toggling, state recovery, and Compose recomposition bounds are structurally sound and all tests currently pass:
1. **Critical Defect**: In `OfflineTeluguCalendarRepositoryImpl.kt`, `isAmavasya` alert evaluation shadows `isFestival`, making the required Feature 5 "పోలాల అమావాస్య" agro-festival alert unreachable dead code.
2. **Defective Test Assertion**: `OfflineTeluguCalendarRepositoryTest.kt` line 60 masks this defect via a disjunctive assertion (`AMAVASYA || FESTIVAL`).
3. **Verdict**: **REQUEST_CHANGES**. The alert precedence order in `OfflineTeluguCalendarRepositoryImpl.kt` must evaluate `if (day.isFestival && day.festivalName != null)` *before* checking standard Amavasya, and the unit test must explicitly assert `LunarAlertType.FESTIVAL` on Polala Amavasya.

---

## 5. Verification Method

To independently reproduce and verify this finding:
```powershell
Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
.\gradlew.bat --no-daemon :core:common:testDebugUnitTest :feature:home:testDebugUnitTest
```
To verify the Polala Amavasya shadowing defect:
1. Inspect `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt` lines 91–134.
2. Query `repository.getTodayAlert(20323L)` (August 23, 2025, Shravana Amavasya / Polala Amavasya).
3. Observe that it returns `LunarAlertType.AMAVASYA` instead of `LunarAlertType.FESTIVAL`.
4. Invalidation condition: `calculateTodayAlert` evaluates `day.isFestival` before `day.isAmavasya`, returning `LunarAlertType.FESTIVAL` on August 23, 2025.
