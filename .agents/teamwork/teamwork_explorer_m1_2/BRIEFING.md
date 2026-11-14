# BRIEFING — 2026-11-14T08:02:00Z

## Mission
Investigate `HomeViewModel.kt`, its dependencies, `combine` stream, UI state, and audio integration for Telugu Calendar & Panchang alert banner, providing a detailed handoff recommendation for Milestone 1.

## 🔒 My Identity
- Archetype: explorer
- Roles: investigator, reporter
- Working directory: C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
- State folder: C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\teamwork_explorer_m1_2
- Original parent: 079bcced-b104-4b1b-9219-13d48cdb214a
- Milestone: M1: Core Repository & Home Screen Dynamic Alert Banner

## 🔒 Key Constraints
- Read-only investigation — do NOT implement source code
- Files for content delivery, messages for coordination
- Must provide exact code proposals, logic chains, caveats, and verification methods in handoff.md

## Current Parent
- Conversation ID: 079bcced-b104-4b1b-9219-13d48cdb214a
- Updated: 2026-11-14T07:55:00Z

## Investigation State
- **Explored paths**:
  - `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`
  - `core/database/src/main/kotlin/com/farmsaathi/core/database/di/DatabaseModule.kt`
  - `core/model/src/main/kotlin/com/farmsaathi/core/model/TeluguCalendarModels.kt`
  - `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/TeluguCalendarRepository.kt`
  - `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`
  - `core/common/src/main/kotlin/com/farmsaathi/core/common/BackupAndAudioContracts.kt`
  - `core/audio/src/main/kotlin/com/farmsaathi/core/audio/HybridAudioGuidanceEngine.kt`
  - `feature/home/src/main/kotlin/com/farmsaathi/feature/home/components/HomePanchangKarteBanner.kt`
  - `feature/home/src/main/kotlin/com/farmsaathi/feature/home/screens/HomeDashboardScreen.kt`
  - `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeDashboardUiTest.kt`
- **Key findings**:
  - `TeluguCalendarRepository` is already provided as a `@Singleton` in `DatabaseProvideModule` (`DatabaseModule.kt:88`).
  - `HomeViewModel` constructor can inject `teluguCalendarRepo: TeluguCalendarRepository` with optional default parameter `= OfflineTeluguCalendarRepositoryImpl()` to preserve backward compatibility for tests.
  - `HomeUiState` additions: `todayCalendarDay: TeluguCalendarDay? = null`, `todayKarteInfo: CurrentKarteInfo? = null`, `todayAlert: LunarDayAlert? = null`, `isPanchangAudioPlaying: Boolean = false`.
  - Main `combine` stream in `HomeViewModel` currently combines 9 flows into an array; adding the 3 calendar repository flows (`observeDay`, `observeCurrentKarte`, `observeTodayAlert`) and `isPanchangAudioPlayingFlow` extends this to 13 flows cleanly, or via a calendar sub-flow.
  - `togglePanchangAudio()` should check if currently playing (if so, stop TTS and set flag to false), otherwise retrieve `uiState.value.todayAlert`, set flag to true, and invoke `audioEngine.speakDynamicText(alert.ttsSpeechText, useTelugu = true)`. Listening to `audioEngine.isSpeaking` automatically resets `isPanchangAudioPlaying` when speech concludes.
- **Unexplored areas**: None within the scope of M1-Explorer-2.

## Key Decisions Made
- Fully analyzed `HomeViewModel.kt` structure and audio concurrency semantics.
- Formulated the exact implementation recommendation for the Worker.

## Artifact Index
- DISPATCH.md — incoming instructions log
- progress.md — liveness heartbeat
- handoff.md — final analysis and worker recommendations
