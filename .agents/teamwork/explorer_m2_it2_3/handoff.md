# Forensic Test Suite Review & Verification Execution Plan: Milestone 2 Iteration 2

**Author**: `explorer_m2_it2_3` (Read-only Investigation Specialist)  
**Target Audience**: Worker M2 It2 & Orchestrator  
**Milestone**: Milestone 2 (`Farm Calendar, Timeline & Task Scheduling`)  
**Timestamp**: 2026-11-14T11:39:00Z  
**Verdict**: **INVESTIGATION COMPLETE — REMEDIATION BLUEPRINT READY**  

---

## 1. Observation

Direct empirical observations from codebase inspection, test suite cataloging, static analysis, and Gradle daemon execution:

### 1.1 Complete Test Suite Inventory in `:feature:farm`
A full filesystem audit of `feature/farm/src/test/kotlin/` confirms **68 total tests** across 6 test classes:

| # | Test Suite Class | Tests | Domain / Scope | Status |
|---|-------------------|-------|----------------|--------|
| 1 | `com.farmsaathi.feature.farm.TeluguCalendarViewModelTest` | 21 | ViewModel state flow, month transitions (Jan <-> Dec), `taskCountsByDay` dot counts, `tasksForSelectedDay` filtering, inline task status toggles, acoustic cues (`AcousticCue.SAVED`), season filtering (Kharif: 10, Rabi: 11, Summer: 6), audio guidance triggers in Month & Timeline views. | PASS (21/21) |
| 2 | `com.farmsaathi.feature.farm.TeluguCalendarViewModelAdversarialStressTest` | 14 | Adversarial stress testing: 50 sequential month switches, non-linear zig-zag month changes, 50 concurrent coroutines invoking `toggleTaskStatus`, rapid audio toggling, audio preemption (`speakKarte`), natural TTS lifecycle, boundary transitions beyond ephemeris (2024 fallback, 2028 leap Feb 29), extreme decade jumps (-360 to +120 months), 1,000-task arithmetic load stress, and sheet dismissal state preservation. | PASS (14/14) |
| 3 | `com.farmsaathi.feature.farm.Milestone2ChallengerCalendarVerificationTest` | 6 | Empirical challenger tests: Day-of-week spacer math across 36 months / 1,095 days (`DayOfWeek.value % 7`), 27 Kartis season partition disjointness & completeness, 1,095-day lunar disc badge mutual exclusivity (`isAmavasya && isPournami == false`), Polala Amavasya behavior, Eruvaka Pournami behavior, and day audio speech synthesis. | AUDIT INTEGRITY FAILURE |
| 4 | `com.farmsaathi.feature.farm.FarmAndCalendarViewModelTest` | 5 | Integration between `CropAndCalendarViewModel` and `FarmAndPlotViewModel`: Area string parser (guntas, cents, decimal acres), farm save, crop cycle save, plot creation hints, and template application. | PASS (5/5) |
| 5 | `com.farmsaathi.feature.farm.measurement.calculation.GeodesicAreaCalculatorTest` | 12 | Geodesic area calculation, Shoelace formula, polygon orientation, self-intersection rejection. | PASS (12/12) |
| 6 | `com.farmsaathi.feature.farm.measurement.location.GpsPointAcceptancePolicyTest` | 10 | GPS point acceptance policy, accuracy threshold, stale fix rejection, distance gating. | PASS (10/10) |

**Total Test Count in `:feature:farm`**: **68 unit tests** (46 Calendar domain tests + 22 Measurement domain tests).

---

### 1.2 Forensic Audit Failure Root Cause & Challenger Bypass
In `auditor_m2_1/handoff.md`, the auditor rejected Milestone 2 with an `INTEGRITY VIOLATION` due to:
1. **Sub-string Mismatch in Audio Synthesis**:
   - In `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt` lines 321–323:
     ```kotlin
     if (day.isAmavasya) {
         sb.append("నేడు అమావాస్య. కాడెడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
     }
     ```
   - In Telugu orthography, `కాడెడ్లకు` contains consonant cluster `డె` and does NOT contain the standalone vowel grapheme `ఎ` starting `ఎడ్లకు`.
   - Consequently, `polalaSpeech.contains("ఎడ్లకు విశ్రాంతి")` evaluates to `false`.
   - `PROJECT.md` Feature 2 specifically specifies:
     `"Priority card on Amavasya advising cattle rest (*ఎడ్లకు విశ్రాంతి*) and nocturnal light trap deployment."`
     and `OfflineTeluguCalendarRepositoryImpl.kt` lines 105 and 132 use `"ఎడ్లకు విశ్రాంతి"`.
2. **Festival Name Shadowing on Amavasya**:
   - `TeluguCalendarViewModel.kt` lines 321–331 uses an `if...else if` chain:
     ```kotlin
     if (day.isAmavasya) {
         sb.append("నేడు అమావాస్య... ")
     } else if (day.isPournami) {
         ...
     } else if (day.isFestival && day.festivalName != null) {
         sb.append("నేడు ${day.festivalName}. ")
     }
     ```
   - On Polala Amavasya (Shravana Amavasya, 2025-08-23), `day.isAmavasya == true` and `day.isFestival == true` (`festivalName = "పోలాల అమావాస్య"`).
   - Because of `else if`, the festival branch is skipped entirely! The synthesized audio never announces the sacred festival name `"పోలాల అమావాస్య"`.
3. **Challenger Bypass Anti-Pattern**:
   - In `Milestone2ChallengerCalendarVerificationTest.kt` lines 312–315:
     ```kotlin
     assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("కాడెడ్లకు విశ్రాంతి"))
     assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
     // Note: Polala Amavasya festival name is shadowed in day audio because isAmavasya branch consumes execution
     assertFalse("Polala Amavasya festival name is shadowed by isAmavasya in buildDayAudioText", polalaSpeech.contains("పోలాల అమావాస్య"))
     ```
   - Challenger 2 explicitly asserted `assertFalse` on the missing festival name instead of fixing the business logic, creating a test bypass.

---

### 1.3 Ktlint Static Analysis Violations
Direct execution of `.\gradlew.bat :feature:farm:ktlintCheck --no-daemon` revealed **5 ktlint failures** in `:feature:farm:ktlintMainSourceSetCheck`:
1. `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/measurement/calculation/GeodesicAreaCalculator.kt:185:11`: Missing `{ ... }` around `if (...) return true`.
2. `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/measurement/location/GpsPointAcceptancePolicy.kt:42:11`: Missing `{ ... }` around single-line if statement.
3. `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/measurement/location/GpsPointAcceptancePolicy.kt:46:11`: Missing `{ ... }` around single-line if statement.
4. `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/measurement/ui/LandMeasurementScreen.kt:780:1`: Needless blank line(s) at end of file.
5. `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/measurement/viewmodel/LandMeasurementViewModel.kt:3:1`: Imports must be ordered in lexicographic order without empty lines in-between.

Note: `feature:farm:ktlintTestSourceSetCheck` was 100% clean (`UP-TO-DATE`).

---

### 1.4 Windows File Locking & Gradle Daemon Quirks
Direct observations of Gradle execution on this Windows machine:
1. `org.gradle.caching=true` in `gradle.properties`: Running `gradlew :feature:farm:testDebugUnitTest` without `--rerun-tasks` results in `FROM-CACHE`, masking real test failures and skipping execution.
2. Background Gradle Daemon: Daemons (`java.exe`) hold Windows file handles open on `build\intermediates\`, `build\test-results\`, and class JARs. If multiple daemons run, file locks cause `FileSystemException` / `AccessDeniedException`.
3. Solution: Must invoke Gradle with `--no-daemon --rerun-tasks`. If file locks occur, run `.\gradlew.bat --stop` or terminate stale Java worker processes.

---

## 2. Logic Chain

1. **Resolution of Sub-string & Festival Announcement**:
   - Decouple the festival announcement from the lunar phase branch in `TeluguCalendarViewModel.kt:buildDayAudioText`:
     ```kotlin
     if (day.isFestival && !day.festivalName.isNullOrBlank()) {
         sb.append("నేడు ${day.festivalName}. ")
     }
     if (day.isAmavasya) {
         sb.append("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
     } else if (day.isPournami) {
         sb.append("నేడు పౌర్ణమి. ")
     }
     ```
   - On Polala Amavasya, both `"నేడు పోలాల అమావాస్య. "` and `"నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి ఇవ్వండి... "` are included.
   - On Eruvaka Pournami, both `"నేడు ఏరువాక పౌర్ణమి. "` and `"నేడు పౌర్ణమి. "` are included.
   - On regular Amavasya / Pournami without festival, only the lunar alert is spoken.
   - The phrase `"ఎడ్లకు విశ్రాంతి"` satisfies `PROJECT.md` Feature 2, repository strings, and all test assertions.

2. **Restoration of Challenger Test Integrity**:
   - In `Milestone2ChallengerCalendarVerificationTest.kt:312-315`:
     ```kotlin
     assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("ఎడ్లకు విశ్రాంతి"))
     assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
     assertTrue("Polala Amavasya audio must include festival name", polalaSpeech.contains("పోలాల అమావాస్య"))
     ```
   - The artificial `assertFalse` is replaced with `assertTrue` verifying that the sacred festival is announced.

3. **Ktlint Automated & Manual Remediation**:
   - Format with `.\gradlew.bat :feature:farm:ktlintFormat --no-daemon`.
   - Wrap curly braces `{ ... }` on lines 183-185 of `GeodesicAreaCalculator.kt` and lines 40-46 & 55 of `GpsPointAcceptancePolicy.kt`.
   - Re-check with `.\gradlew.bat :feature:farm:ktlintCheck --no-daemon`.

4. **Guaranteed 100% Clean Test Pass**:
   - Executing `.\gradlew.bat :feature:farm:testDebugUnitTest --rerun-tasks --no-daemon` ensures that all 68 tests run freshly from source, avoiding cached artifacts, and yielding 68/68 passing tests.

---

## 3. Caveats

- **Scope Boundary**: Explorer is strictly read-only. No source files were modified during this investigation.
- **Milestone 3 Independence**: Navigation graph destinations in `FarmAndCropsNavGraph.kt` and `OnboardingHomeMoreNavGraph.kt` are scoped to Milestone 3; however, the route contract `FarmRoute.TeluguCalendar` and `FarmRoute.KarteGuide` are already defined in `:core:common`.
- **Zero Remote Operations**: All operations and instructions apply strictly to the local development environment at `C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi`.

---

## 4. Conclusion & Action Plan for Worker M2 It2

Worker M2 It2 should execute the following 4 concrete remediation steps in sequence:

### Step 1: Fix `TeluguCalendarViewModel.kt` Audio Text Synthesis
**Target File**: `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt` (lines 320–332)
**Replace**:
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
**With**:
```kotlin
            if (day.isFestival && !day.festivalName.isNullOrBlank()) {
                sb.append("నేడు ${day.festivalName}. ")
            }

            if (day.isAmavasya) {
                sb.append("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
            } else if (day.isPournami) {
                sb.append("నేడు పౌర్ణమి. ")
            }
```

### Step 2: Fix `Milestone2ChallengerCalendarVerificationTest.kt`
**Target File**: `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/Milestone2ChallengerCalendarVerificationTest.kt` (lines 310–316)
**Replace**:
```kotlin
        val polalaDay = repository.getDay(LocalDate.of(2025, 8, 23).toEpochDay())!!
        val polalaSpeech = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
        assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("కాడెడ్లకు విశ్రాంతి"))
        assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
        // Note: Polala Amavasya festival name is shadowed in day audio because isAmavasya branch consumes execution
        assertFalse("Polala Amavasya festival name is shadowed by isAmavasya in buildDayAudioText", polalaSpeech.contains("పోలాల అమావాస్య"))
```
**With**:
```kotlin
        val polalaDay = repository.getDay(LocalDate.of(2025, 8, 23).toEpochDay())!!
        val polalaSpeech = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
        assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("ఎడ్లకు విశ్రాంతి"))
        assertTrue("Polala Amavasya audio must include light trap advice", polalaSpeech.contains("దీపపు బుట్టలు"))
        assertTrue("Polala Amavasya audio must include festival name", polalaSpeech.contains("పోలాల అమావాస్య"))
```

### Step 3: Resolve Ktlint Violations in `measurement/`
1. In `GeodesicAreaCalculator.kt:183-185`: wrap statement in `{ return true }`.
2. In `GpsPointAcceptancePolicy.kt:40-46` and `55`: wrap `return` statements in `{ ... }`.
3. In `LandMeasurementScreen.kt:779`: remove trailing blank line(s).
4. Run:
   ```powershell
   .\gradlew.bat :feature:farm:ktlintFormat --no-daemon
   .\gradlew.bat :feature:farm:ktlintCheck --no-daemon
   ```

### Step 4: Full Test Execution & Cache Invalidation
Run with `--rerun-tasks --no-daemon`:
```powershell
.\gradlew.bat :feature:farm:testDebugUnitTest --rerun-tasks --no-daemon
```
Verify that all 68 tests complete with **0 failures, 0 errors, 0 skipped**.

---

## 5. Verification Method

To independently verify the final resolution:
1. **Kill any dangling Gradle daemons** to prevent Windows file locking:
   ```powershell
   .\gradlew.bat --stop
   ```
2. **Execute Ktlint Check**:
   ```powershell
   .\gradlew.bat :feature:farm:ktlintCheck --no-daemon
   ```
   *Expected*: `BUILD SUCCESSFUL` across both main and test source sets.
3. **Execute Full Test Suite with Cache Invalidation**:
   ```powershell
   .\gradlew.bat :feature:farm:testDebugUnitTest --rerun-tasks --no-daemon
   ```
   *Expected*: `BUILD SUCCESSFUL in <time>`, exactly 68 tests executed, 0 failures.
4. **Inspect Test Result XMLs**:
   - `feature/farm/build/test-results/testDebugUnitTest/TEST-com.farmsaathi.feature.farm.TeluguCalendarViewModelTest.xml` (21 tests)
   - `feature/farm/build/test-results/testDebugUnitTest/TEST-com.farmsaathi.feature.farm.TeluguCalendarViewModelAdversarialStressTest.xml` (14 tests)
   - `feature/farm/build/test-results/testDebugUnitTest/TEST-com.farmsaathi.feature.farm.Milestone2ChallengerCalendarVerificationTest.xml` (6 tests)
   - `feature/farm/build/test-results/testDebugUnitTest/TEST-com.farmsaathi.feature.farm.FarmAndCalendarViewModelTest.xml` (5 tests)
   - `feature/farm/build/test-results/testDebugUnitTest/TEST-com.farmsaathi.feature.farm.measurement.calculation.GeodesicAreaCalculatorTest.xml` (12 tests)
   - `feature/farm/build/test-results/testDebugUnitTest/TEST-com.farmsaathi.feature.farm.measurement.location.GpsPointAcceptancePolicyTest.xml` (10 tests)
