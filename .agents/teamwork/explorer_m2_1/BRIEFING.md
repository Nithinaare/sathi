# BRIEFING — 2026-11-14T10:18:45Z

## Mission
Investigate requirements and design the exact architecture of TeluguCalendarViewModel & Domain State Integration for Milestone 2.

## 🔒 My Identity
- Archetype: explorer
- Roles: Read-only investigation, architectural analysis, synthesis
- Working directory: C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
- State folder: C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\explorer_m2_1
- Original parent: e5e53cba-1bad-4cfe-bd14-70ccb2e8277a
- Milestone: Milestone 2 — TeluguCalendarViewModel & Domain State Integration

## 🔒 Key Constraints
- Read-only investigation — do NOT modify project source code directly
- Handoff report in state folder with 5-component protocol
- Accurate file paths, line numbers, and symbol verification

## Current Parent
- Conversation ID: e5e53cba-1bad-4cfe-bd14-70ccb2e8277a
- Updated: 2026-11-14T10:18:45Z

## Investigation State
- **Explored paths**:
  - `core/model/src/main/kotlin/com/farmsaathi/core/model/TeluguCalendarModels.kt`
  - `core/model/src/main/kotlin/com/farmsaathi/core/model/DomainModels.kt` (`CropTask`)
  - `core/model/src/main/kotlin/com/farmsaathi/core/model/FarmEnums.kt` (`TaskStatus`, `AppLanguage`)
  - `core/model/src/main/kotlin/com/farmsaathi/core/model/DomainError.kt` (`AppResult`)
  - `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/TeluguCalendarRepository.kt`
  - `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`
  - `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/FarmRepository.kt`
  - `core/common/src/main/kotlin/com/farmsaathi/core/common/AppDispatchers.kt`
  - `core/common/src/main/kotlin/com/farmsaathi/core/common/TimeAndUtils.kt` (`TimeProvider`)
  - `core/common/src/main/kotlin/com/farmsaathi/core/common/BackupAndAudioContracts.kt` (`AudioGuidanceEngine`)
  - `core/common/src/main/kotlin/com/farmsaathi/core/common/navigation/FarmRoute.kt` (`TeluguCalendar`, `KarteGuide`, `ActivityForm`)
  - `core/database/src/main/kotlin/com/farmsaathi/core/database/di/DatabaseModule.kt`
  - `core/datastore/src/main/kotlin/com/farmsaathi/core/datastore/UserPreferencesRepository.kt`
  - `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`
  - `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/CropAndCalendarViewModel.kt`
  - `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/CropsAndCalendarScreens.kt` (`ActivityFormScreen`)
  - `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/FarmAndCalendarViewModelTest.kt`
  - Reference specs in `brain/10b008e7-4e30-42dd-aafb-3cafee65ec3d/`
- **Key findings**:
  - `TeluguCalendarModels.kt` provides all domain entities: `TeluguMonth`, `Paksham`, `TeluguTithi`, `TeluguKarte`, `TeluguCalendarDay`, `CurrentKarteInfo`, `LunarDayAlert`, `TeluguKarteluCatalog`.
  - `OfflineTeluguCalendarRepositoryImpl` provides pre-computed 2025–2027 Chandramana ephemeris dataset and 27 Kartis boundaries with zero crashes.
  - `FarmRepository.observeTasks()` returns `Flow<List<CropTask>>` where tasks have `dueDateEpochDay: Long`.
  - Navigation route `FarmRoute.ActivityForm` has `initialDueDateEpochDay: Long? = null` ready for 1-tap pre-filled navigation.
  - `AudioGuidanceEngine` dynamic speech synthesis in Telugu (`speakDynamicText(text, useTelugu = true)`) is ready and supports unhurried rural cadence.
  - Clean separation: Keep `TeluguCalendarDay` pure domain; maintain task counts in `TeluguCalendarUiState.taskCountsByDay: Map<Long, Int>`.
- **Unexplored areas**: None. Architectural blueprint and symbol verification are complete.

## Key Decisions Made
- Architecture for `TeluguCalendarViewModel` designed with Hilt dependency injection, nested typed reactive state slices for rock-solid concurrency, exact task merging by `dueDateEpochDay`, and authentic Telugu TTS synthesis text generation.

## Artifact Index
- DISPATCH.md — Incoming parent instructions
- BRIEFING.md — Persistent context & identity
- progress.md — Heartbeat and step progress
- handoff.md — Comprehensive analysis and design report
