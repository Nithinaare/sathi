## 2026-11-14T08:04:43Z
You are the Worker for Milestone 1 (M1: Core Repository & Home Screen Dynamic Alert Banner).
Working directory: C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
State folder: C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\teamwork_worker_m1_1

Mandatory reading:
1. ORIGINAL_REQUEST.md at C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\ORIGINAL_REQUEST.md
2. PROJECT.md at C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\PROJECT.md
3. Explorer Handoff Reports:
   - C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\teamwork_explorer_m1_1\handoff.md
   - C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\teamwork_explorer_m1_2\handoff.md
   - C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\teamwork_explorer_m1_3\handoff.md

Exclusive Write Ownership:
- core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt
- feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt
- feature/home/src/main/kotlin/com/farmsaathi/feature/home/screens/HomeDashboardScreen.kt
- feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeDashboardUiTest.kt

Implementation Tasks:
1. Fix test date in `OfflineTeluguCalendarRepositoryTest.kt` line 90 from `2025-07-10` to `2025-07-08` (Ashadha Shukla Trayodashi) so it tests a true standard day instead of Pournami.
2. In `HomeViewModel.kt`:
   - Inject `private val teluguCalendarRepo: TeluguCalendarRepository` in `@Inject constructor`.
   - Add to `HomeUiState`:
     `val todayCalendarDay: TeluguCalendarDay? = null,`
     `val todayKarteInfo: CurrentKarteInfo? = null,`
     `val todayAlert: LunarDayAlert? = null,`
     `val isPanchangAudioPlaying: Boolean = false,`
   - In `init` or stream combination, observe `teluguCalendarRepo.observeDay(todayEpochDay)`, `teluguCalendarRepo.observeCurrentKarte(todayEpochDay)`, and `teluguCalendarRepo.observeTodayAlert(todayEpochDay)` and merge them into `HomeUiState`. Also map `audioEngine.isSpeaking` or keep audio tracking.
   - Implement `fun togglePanchangAudio()`: If audio is currently speaking panchang alert, call `audioEngine.stop()`. Otherwise, read `uiState.value.todayAlert?.ttsSpeechText` and if not null or blank, call `audioEngine.speakDynamicText(ttsSpeechText, useTelugu = true)`.
3. In `HomeDashboardScreen.kt`:
   - Add parameter `onOpenTeluguCalendar: () -> Unit = {}` to `HomeDashboardScreen`.
   - In `LazyColumn`, directly above `item(key = "season_summary_card")`, insert `HomePanchangKarteBanner` when `state.todayCalendarDay != null && state.todayAlert != null`, wiring `onPlayAudio = viewModel::togglePanchangAudio` and `onClick = onOpenTeluguCalendar`.
4. In `HomeDashboardUiTest.kt`:
   - Provide `StubTeluguCalendarRepo` so existing tests continue to pass with the new ViewModel constructor.
   - Add tests verifying the banner renders with the panchang data and that audio playback can be triggered.
5. Run build and tests:
   Run `.\gradlew.bat :core:common:testDebugUnitTest :feature:home:testDebugUnitTest :feature:home:compileDebugKotlin` and verify everything builds and passes.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Deliverable:
Write your implementation report to:
C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\teamwork_worker_m1_1\handoff.md
Send a completion message to the orchestrator when finished.


## 2026-11-14T08:34:42Z
**Context**: Milestone 1 Worker
**Content**: Status check on Milestone 1 implementation and test results.
**Action**: Please write handoff.md and send your completion report.
