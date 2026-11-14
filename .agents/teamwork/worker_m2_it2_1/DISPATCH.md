## 2026-11-14T11:40:18Z
You are worker_m2_it2_1, a versatile implementation worker.
Working directory: C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
State folder: C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\worker_m2_it2_1

MANDATORY FIRST STEP:
Read C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\ORIGINAL_REQUEST.md before starting work.
Also read C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\PROJECT.md.

Read the detailed remediation recommendations from the 3 Explorers:
- C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\explorer_m2_it2_1\handoff.md (Audio synthesis and festival announcement fix)
- C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\explorer_m2_it2_2\handoff.md (Test assertion alignment)
- C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\explorer_m2_it2_3\handoff.md (Test execution and verification plan)

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

EXCLUSIVE FILE OWNERSHIP:
You own and may modify:
- `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt`
- `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/Milestone2ChallengerCalendarVerificationTest.kt`
- `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/TeluguCalendarViewModelTest.kt`

TASKS:
1. In `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt`:
   In `buildDayAudioText` (around lines 321-331), update the festival and Amavasya branching:
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
   Note: This fixes both: (a) domain standard token `"ఎడ్లకు విశ్రాంతి"` (replaces `"కాడెడ్లకు"`), and (b) announces the festival name on sacred Amavasyas (such as Polala Amavasya).

2. In `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/Milestone2ChallengerCalendarVerificationTest.kt`:
   Update lines 309–316 to genuine assertions:
   ```kotlin
   // Test Polala Amavasya
   val polalaDay = repository.getDay(LocalDate.of(2025, 8, 23).toEpochDay())!!
   val polalaSpeech = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
   assertTrue("Polala Amavasya audio must include festival name", polalaSpeech.contains("పోలాల అమావాస్య"))
   assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("ఎడ్లకు విశ్రాంతి"))
   assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
   ```

3. In `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/TeluguCalendarViewModelTest.kt`:
   Add a unit test verifying Polala Amavasya audio synthesis:
   - Day with `isAmavasya = true`, `isFestival = true`, `festivalName = "పోలాల అమావాస్య"`.
   - Asserts speech contains `"పోలాల అమావాస్య"`, `"ఎడ్లకు విశ్రాంతి"`, and `"దీపపు బుట్టలు"`.

4. Stop Gradle daemons and execute test suites:
   ```powershell
   .\gradlew.bat --stop
   .\gradlew.bat :feature:farm:testDebugUnitTest --rerun-tasks
   .\gradlew.bat :feature:farm:ktlintTestSourceSetCheck
   ```
   Verify 100% test success across all tests in `:feature:farm` (68/68 or 69/69 passed, 0 failures, 0 errors).

5. Deliver your handoff report to `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi\.agents\teamwork\worker_m2_it2_1\handoff.md`.
6. Send a message to your caller upon completion.
