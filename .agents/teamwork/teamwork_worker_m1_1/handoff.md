# Milestone 1: Core Repository & Home Screen Dynamic Alert Banner — Implementation Handoff Report

## 1. Observation

### Exact File Paths & Lines Modified
1. `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`
   - Line 90: Updated fixture date in `getTodayAlert_standardDayProducesCleanInformativeAlert` from `LocalDate.of(2025, 7, 10)` to `LocalDate.of(2025, 7, 8)` (Ashadha Shukla Trayodashi).
   - Lines 98–109: Added test `getTodayAlert_triggersPournamiAlertOnPournami` using `LocalDate.of(2025, 7, 10)` asserting `LunarAlertType.POURNAMI` alert generation.
2. `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`
   - Lines 18, 20: Added imports for `OfflineTeluguCalendarRepositoryImpl` and `TeluguCalendarRepository`.
   - Lines 27, 30, 35: Added imports for `CurrentKarteInfo`, `LunarDayAlert`, and `TeluguCalendarDay`.
   - Lines 70–73: Added `todayCalendarDay: TeluguCalendarDay? = null`, `todayKarteInfo: CurrentKarteInfo? = null`, `todayAlert: LunarDayAlert? = null`, `isPanchangAudioPlaying: Boolean = false` to `HomeUiState`.
   - Line 90: Injected `private val teluguCalendarRepo: TeluguCalendarRepository = OfflineTeluguCalendarRepositoryImpl()` into `@Inject constructor`.
   - Lines 103, 107–113: Added `isPanchangAudioPlayingFlow = MutableStateFlow(false)` and collector in `init` listening to `audioEngine.isSpeaking` to reset playback state on completion.
   - Lines 116–140: Added `todayEpochDay = timeProvider.todayEpochDay()` and observed `teluguCalendarRepo.observeDay(todayEpochDay)`, `teluguCalendarRepo.observeCurrentKarte(todayEpochDay)`, `teluguCalendarRepo.observeTodayAlert(todayEpochDay)`, and `isPanchangAudioPlayingFlow` inside `combine(...)`.
   - Lines 174–178: Mapped `calendarDay`, `karteInfo`, `todayAlert`, and `isPanchangAudioPlaying` into `HomeUiState`, and isolated header audio playing via `isAudioPlaying = speaking && !isPanchangAudioPlaying`.
   - Lines 231–248: Implemented `fun togglePanchangAudio()` to speak `alert.ttsSpeechText` via `audioEngine.speakDynamicText(..., useTelugu = true)` or stop playback if active.
   - Lines 246, 317–321: Added cancellation of panchang audio in `toggleHomeAudio()` and `onCleared()`.
3. `feature/home/src/main/kotlin/com/farmsaathi/feature/home/screens/HomeDashboardScreen.kt`
   - Line 98: Imported `com.farmsaathi.feature.home.components.HomePanchangKarteBanner`.
   - Line 120: Added parameter `onOpenTeluguCalendar: () -> Unit = {}` to `HomeDashboardScreen`.
   - Lines 185–196: Inserted `HomePanchangKarteBanner` directly above `item(key = "season_summary_card")` in `LazyColumn`, guarded by `state.todayCalendarDay != null && state.todayAlert != null`, wired with `onPlayAudio = viewModel::togglePanchangAudio` and `onClick = onOpenTeluguCalendar`.
4. `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeDashboardUiTest.kt`
   - Lines 10–12, 19, 36, 40–41, 44, 48–51: Added imports for `TeluguCalendarRepository`, `CurrentKarteInfo`, `LunarAlertType`, `LunarDayAlert`, `Paksham`, `TeluguCalendarDay`, `TeluguKarte`, `TeluguKarteluCatalog`, `TeluguMonth`, `TeluguTithi`, `onAllNodesWithText`, `onLast`.
   - Lines 98, 147, 190, 244: Passed `teluguCalendarRepo = StubTeluguCalendarRepo()` to all 4 existing tests.
   - Lines 275–371: Added two unit tests:
     - `home dashboard displays panchang karte banner with telugu details and handles click`: Verifies month, paksham, tithi, karte, day progress rendering, and navigation click callback invocation.
     - `home dashboard panchang banner audio button triggers tts speech`: Verifies 1-tap "వినండి" audio button click triggers `audioEngine.speakDynamicText()` with exact telugu speech script.
   - Lines 644–716: Implemented `StubTeluguCalendarRepo` providing sample calendar day, karte info, and lunar day alert.

### Verbatim Build & Test Execution
Command executed:
```powershell
Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"; .\gradlew.bat :core:common:testDebugUnitTest :feature:home:testDebugUnitTest :feature:home:compileDebugKotlin
```
Verbatim Output:
```
BUILD SUCCESSFUL in 1m 52s
143 actionable tasks: 1 executed, 142 up-to-date
```
Test Results:
- `:core:common`: 13 tests passed, 0 failures, 0 skipped (`OfflineTeluguCalendarRepositoryTest`: 8 passed; `CoreCommonContractsTest`: 5 passed).
- `:feature:home`: 6 tests passed, 0 failures, 0 skipped (`HomeDashboardUiTest`: 6 passed).
- `:feature:home:compileDebugKotlin`: SUCCESS.

---

## 2. Logic Chain

1. **Standard Day vs. Pournami in `OfflineTeluguCalendarRepositoryTest.kt`**:
   - `LocalDate.of(2025, 7, 10)` corresponds to epoch day `20279L`, which matches `LunarSpan("విశ్వావసు", TeluguMonth.ASHADHAM, 20265L, 20279L, 20293L).pournamiEpochDay`.
   - The repository correctly returned `LunarAlertType.POURNAMI`.
   - Changing the date to `LocalDate.of(2025, 7, 8)` (`20277L`, Ashadha Shukla Trayodashi) avoids Amavasya, Pournami, agricultural festivals, and Karte entry, cleanly reaching `LunarAlertType.DAY_STANDARD`.
   - Adding a dedicated Pournami test preserves regression coverage for Pournami alert generation.

2. **Repository Injection in `HomeViewModel`**:
   - `TeluguCalendarRepository` is provided as a `@Singleton` in `DatabaseModule.kt`.
   - Providing a default parameter `= OfflineTeluguCalendarRepositoryImpl()` preserves constructor usability for tests while allowing Hilt to inject the repository at runtime.
   - In `HomeUiState`, adding default values (`null` / `false`) preserves compatibility with existing callers.

3. **Reactive Flow Observation**:
   - `teluguCalendarRepo.observeDay(todayEpochDay)`, `observeCurrentKarte(todayEpochDay)`, and `observeTodayAlert(todayEpochDay)` return cold flows that emit immediately upon collection.
   - Adding these to `combine(...)` along with `isPanchangAudioPlayingFlow` combines all relevant domain states.
   - Collecting `audioEngine.isSpeaking` automatically resets `isPanchangAudioPlayingFlow.value = false` when TTS finishes speaking, restoring the button state from "ఆపండి" to "వినండి".

4. **Dashboard Layout Placement**:
   - Placing `HomePanchangKarteBanner` directly above `season_summary_card` places critical daily agricultural guidance immediately below urgent machinery and backup safety notices.
   - Adding `onOpenTeluguCalendar: () -> Unit = {}` ensures backwards compatibility for existing call sites in `OnboardingHomeMoreNavGraph.kt` while allowing Milestone 3 to wire navigation to `FarmRoute.TeluguCalendar()`.

5. **Test Determinism & Disambiguation**:
   - `HomeDashboardUiTest` supplies `StubTeluguCalendarRepo` with sample Telugu calendar day (`కార్తీకం • శుక్ల పక్షం పాడ్యమి`), Karte (`విశాఖ కార్తె 5వ రోజు`), and alert.
   - To avoid ambiguity between `HomeHeaderBar`'s audio button (which sets `StateDescription = "Stopped"`) and `HomePanchangKarteBanner`'s audio button, the test uses `hasText("వినండి") and SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription)` after waiting for the banner to be displayed.
   - All tests pass with 100% determinism.

---

## 3. Caveats

- **TimeProvider across Midnight**: `todayEpochDay` is evaluated when `HomeViewModel` initializes. If the user keeps the app open across midnight without backgrounding or recreating the ViewModel, it retains the epoch day from initial launch. This matches the existing behavior of `observeDueAndOverdueTasks`.
- **Navigation Wiring**: `onOpenTeluguCalendar` is currently defaulted to `{}` in `HomeDashboardScreen` and `OnboardingHomeMoreNavGraph.kt`. Milestone 3 will connect it to `navController.navigate(FarmRoute.TeluguCalendar())`.

---

## 4. Conclusion

Milestone 1 is completely implemented, verified, and passing all tests without regressions:
- Test date issue in `OfflineTeluguCalendarRepositoryTest.kt` is resolved, and Pournami coverage is added.
- `HomeViewModel` is connected to `TeluguCalendarRepository`, exposing lunar day, karte info, dynamic alert, and TTS audio playback toggle.
- `HomeDashboardScreen` displays `HomePanchangKarteBanner` above the season summary card and handles navigation and 1-tap audio guidance.
- `HomeDashboardUiTest` verifies the banner rendering, click navigation, and audio playback triggering.
- All unit tests and Kotlin compilation across `:core:common` and `:feature:home` pass cleanly.

---

## 5. Verification Method

To independently verify all changes:
```powershell
Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
.\gradlew.bat :core:common:testDebugUnitTest :feature:home:testDebugUnitTest :feature:home:compileDebugKotlin
```
Expected result: `BUILD SUCCESSFUL`, 13 tests passed in `:core:common`, 6 tests passed in `:feature:home`.
