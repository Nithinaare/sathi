## 2026-11-14T09:43:34Z
# Task: Implement Milestone 1 Remediation Fixes & Run Test Verification

## Context & Role
You are the **Milestone 1 Remediation Worker** (`teamwork_worker`).
Your state folder: `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\teamwork_worker_m1_it2_1`
Working directory: `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi`

Read these mandatory files first:
1. `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\ORIGINAL_REQUEST.md`
2. `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\PROJECT.md`
3. Explorer 1 Report (HomeViewModel Audio Trigger): `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\teamwork_explorer_m1_it2_1\handoff.md`
4. Explorer 2 Report (Polala Amavasya Shadowing): `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\teamwork_explorer_m1_it2_2\handoff.md`
5. Explorer 3 Report (Test Verification Plan): `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\teamwork_explorer_m1_it2_3\handoff.md`

## MANDATORY INTEGRITY WARNING
> DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Exclusive Write Ownership
You own and may edit ONLY the following files:
- `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`
- `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`
- `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`
- `core/common/src/test/kotlin/com/farmsaathi/core/common/Milestone1ChallengerStressTest.kt`

## Action Instructions

### Step 1: Fix Polala Amavasya Festival Priority in `OfflineTeluguCalendarRepositoryImpl.kt`
In `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/OfflineTeluguCalendarRepositoryImpl.kt`:
In `private fun calculateTodayAlert(epochDay: Long): LunarDayAlert`, move the `// 3. Agricultural Festival Alert` check (`if (day.isFestival && day.festivalName != null)`) to be evaluated FIRST before `if (day.isAmavasya)`.
This guarantees that Polala Amavasya ("పోలాల అమావాస్య"), Eruvaka Pournami ("ఏరువాక పౌర్ణమి"), and Kanuma ("కనుమ") emit `LunarAlertType.FESTIVAL` with their sacred agro-guidance rather than being shadowed by generic Amavasya/Pournami checks.
Refer to Explorer 2 report for the exact code replacement.

### Step 2: Fix Audio Trigger in `HomeViewModel.kt`
In `feature/home/src/main/kotlin/com/farmsaathi/feature/home/viewmodel/HomeViewModel.kt`:
1. Add property:
   ```kotlin
   private var cachedTodayAlert: LunarDayAlert? = null
   ```
2. In `init`:
   ```kotlin
   viewModelScope.launch(dispatchers.default) {
       teluguCalendarRepo.observeTodayAlert(todayEpochDay).collect { alert ->
           cachedTodayAlert = alert
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
This decouples audio triggering from `WhileSubscribed` collection delay, resolving all 3 failures in `HomeAudioAndRecompositionChallengerTest`.

### Step 3: Update Tests in `OfflineTeluguCalendarRepositoryTest.kt` & `Milestone1ChallengerStressTest.kt`
1. In `core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`:
   - Replace the permissive `||` test on Polala Amavasya with:
     - `getTodayAlert_triggersAmavasyaAlertOnAmavasya`: test pure Amavasya on `2025-04-27` (Chaitra Amavasya), assert `assertEquals(LunarAlertType.AMAVASYA, alert.alertType)` and `assertTrue(alert.title.contains("అమావాస్య: ఎడ్లకు విశ్రాంతి"))`.
     - `getTodayAlert_triggersFestivalAlertOnPolalaAmavasya`: test `2025-08-23` (`20323L`), assert `assertEquals(LunarAlertType.FESTIVAL, alert.alertType)` and `assertTrue(alert.title.contains("పోలాల అమావాస్య"))`.
     - `getTodayAlert_triggersFestivalAlertOnEruvakaPournami`: test `2025-06-11` (Jyeshtha Pournami), assert `assertEquals(LunarAlertType.FESTIVAL, alert.alertType)` and `assertTrue(alert.title.contains("ఏరువాక పౌర్ణమి"))`.
     - `getTodayAlert_triggersKarteTransitionAlertOnDay1`: test `2025-06-08` (Mrigashira Day 1), assert `assertEquals(LunarAlertType.KARTE_TRANSITION, alert.alertType)`.
2. In `core/common/src/test/kotlin/com/farmsaathi/core/common/Milestone1ChallengerStressTest.kt`:
   - In `testAlert_polalaAmavasyaBugInvestigation`, assert strictly:
     ```kotlin
     val alert = repository.getTodayAlert(polalaDay)
     assertEquals(LunarAlertType.FESTIVAL, alert.alertType)
     assertTrue(alert.title.contains("పోలాల అమావాస్య"))
     ```

### Step 4: Run Clean & Test Verification
Run the following PowerShell commands:
```powershell
Set-Location "C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi"
.\gradlew.bat --stop
.\gradlew.bat :core:common:cleanTestDebugUnitTest :feature:home:cleanTestDebugUnitTest
.\gradlew.bat :core:model:compileKotlin :core:common:compileDebugKotlin :feature:home:compileDebugKotlin
.\gradlew.bat --no-daemon :core:common:testDebugUnitTest :feature:home:testDebugUnitTest
```
Ensure all 29 tests in `:core:common` and all 15 tests in `:feature:home` pass (44/44 total).

## 2026-11-14T09:59:17Z
**Context**: Milestone 1 Remediation Execution Status Check
**Content**: Checking in on the status of test verification task (:core:common:testDebugUnitTest :feature:home:testDebugUnitTest). Please update your progress.md with the latest status or let me know if tests have completed and handoff.md is ready.
**Action**: Report current status or finalize handoff.md.

## 2026-11-14T10:03:22Z
**Context**: Authorization for Test Files in `feature:home`
**Content**: You are hereby granted write ownership for the following two test files:
1. `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeDashboardUiTest.kt`
2. `feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeAudioAndRecompositionChallengerTest.kt`

### Exact Guidance:
1. **`HomeDashboardUiTest.kt:410`**:
   Change:
   ```kotlin
   composeRule.onNodeWithText("రాములు గారు").assertIsDisplayed()
   ```
   To:
   ```kotlin
   composeRule.onNodeWithText("నమస్కారం, రాములు గారు").assertIsDisplayed()
   ```
   (Matches line 120 and the header greeting).

2. **`HomeAudioAndRecompositionChallengerTest.kt:513`**:
   In `ControllableCalendarRepo`, change lines 513–515:
   ```kotlin
   private val alertFlow = MutableStateFlow(alert ?: LunarDayAlert(
       LunarAlertType.DAY_STANDARD, "సహజ రోజు", "పనులు కొనసాగించండి", "", null, null, false
   ))
   ```
   (Defaulting `ttsSpeechText` to `""` when `alert == null`, ensuring that `alert = null` satisfies the test condition `togglePanchangAudio is a no-op when todayAlert is null or speech text is blank`).

### Action:
Apply these two edits, re-run `.\gradlew.bat --no-daemon :feature:home:testDebugUnitTest`, ensure all 15 tests pass, write `handoff.md`, and send completion message.
