# Review & Adversarial Challenge Report — Milestone 2

**Reviewer**: reviewer_m2_1  
**Target**: Milestone 2 Implementation (`TeluguCalendarViewModel.kt`, `TeluguCalendarViewModelTest.kt`, UI Screens)  
**Date**: 2026-11-14T11:21:45Z  
**Verdict**: **REQUEST_CHANGES**

---

## 1. Observation

Direct observations from codebase inspection and independent test execution:

### 1.1 Test Suite Execution Output
1. Running `.\gradlew.bat :feature:farm:testDebugUnitTest --tests com.farmsaathi.feature.farm.TeluguCalendarViewModelTest`:
   ```
   BUILD SUCCESSFUL in 1m 27s
   109 actionable tasks: 1 executed, 108 up-to-date
   ```
   All 21 unit tests in `TeluguCalendarViewModelTest` passed (0 failures, 0 errors, 0 skipped).
   `TeluguCalendarViewModelAdversarialStressTest`: 14 tests executed, 14 passed.
   `FarmAndCalendarViewModelTest`: 5 tests executed, 5 passed.

2. Running `.\gradlew.bat :feature:farm:testDebugUnitTest`:
   Result: **BUILD FAILED**
   Test Failure in `Milestone2ChallengerCalendarVerificationTest`:
   File: `feature/farm/build/test-results/testDebugUnitTest/TEST-com.farmsaathi.feature.farm.Milestone2ChallengerCalendarVerificationTest.xml`
   ```xml
   <testcase name="verifyDayAudioText_adversarialAudioSynthesis" classname="com.farmsaathi.feature.farm.Milestone2ChallengerCalendarVerificationTest" time="0.1">
     <failure message="java.lang.AssertionError: Polala Amavasya audio must include Amavasya cattle rest advice" type="java.lang.AssertionError">
       java.lang.AssertionError: Polala Amavasya audio must include Amavasya cattle rest advice
       at org.junit.Assert.fail(Assert.java:89)
       at org.junit.Assert.assertTrue(Assert.java:42)
       at com.farmsaathi.feature.farm.Milestone2ChallengerCalendarVerificationTest$verifyDayAudioText_adversarialAudioSynthesis$1.invokeSuspend(Milestone2ChallengerCalendarVerificationTest.kt:312)
     </failure>
   </testcase>
   ```

### 1.2 Exact Code Discrepancy
- In `worker_m2_1/handoff.md` line 18, the worker claimed:
  > *"Uses authentic Telugu agrarian strings in audio text generators buildDayAudioText and buildCurrentKarteAudioText ("అదను", "ఎడ్లకు విశ్రాంతి", "దీపపు బుట్టలు")."*
- In `PROJECT.md` line 25:
  > *"2 | Amavasya Cattle & Pest Alert | Priority card on Amavasya advising cattle rest (ఎడ్లకు విశ్రాంతి) and nocturnal light trap deployment."*
- In `core/common/.../OfflineTeluguCalendarRepositoryImpl.kt` line 132:
  > `title = "నేడు అమావాస్య: ఎడ్లకు విశ్రాంతి"`
- In `feature/farm/.../TeluguCalendarViewModel.kt` lines 321–322:
  ```kotlin
  if (day.isAmavasya) {
      sb.append("నేడు అమావాస్య. కాడెడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
  }
  ```
- Substring analysis: `కాడెడ్లకు విశ్రాంతి` does **NOT** contain the substring `ఎడ్లకు విశ్రాంతి` because the Telugu characters are distinct (`కా` + `డె` + `డ్ల` vs `ఎ` + `డ్ల`). As a result, the test asserting `polalaSpeech.contains("ఎడ్లకు విశ్రాంతి")` failed.

---

## 2. Logic Chain

1. **Specification & Contract Alignment**:
   `PROJECT.md` Feature 2 specifically designates the Amavasya cattle rest alert token as `ఎడ్లకు విశ్రాంతి`. Upstream repository implementations (`OfflineTeluguCalendarRepositoryImpl.kt:132`) and tests (`OfflineTeluguCalendarRepositoryTest.kt:69`) standardize on `"అమావాస్య: ఎడ్లకు విశ్రాంతి"`.
2. **Worker Claim vs Source Reality**:
   The worker handoff report claimed that `buildDayAudioText` incorporated `"ఎడ్లకు విశ్రాంతి"`. In reality, the implementation emitted `"కాడెడ్లకు విశ్రాంతి"`.
3. **Automated Verification Failure**:
   The automated test suite in `:feature:farm:testDebugUnitTest` runs `Milestone2ChallengerCalendarVerificationTest`. Line 312 asserts:
   `assertTrue("Polala Amavasya audio must include Amavasya cattle rest advice", polalaSpeech.contains("ఎడ్లకు విశ్రాంతి"))`
   Because `TeluguCalendarViewModel.buildDayAudioText` outputs `కాడెడ్లకు విశ్రాంతి`, this test throws an `AssertionError`, failing `:feature:farm:testDebugUnitTest`.
4. **Conclusion**:
   Per the review integrity constraints, work products with failing automated tests cannot be approved. Changes must be requested to align `buildDayAudioText` with the standardized agrarian string token `"ఎడ్లకు విశ్రాంతి"`.

---

## 3. Review Findings

### [Critical] Finding 1: Test Failure in `Milestone2ChallengerCalendarVerificationTest` due to Audio Text Substring Discrepancy
- **What**: Automated test `:feature:farm:testDebugUnitTest` fails on `verifyDayAudioText_adversarialAudioSynthesis`.
- **Where**: `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt:322`
- **Why**: Worker handoff claimed `"ఎడ్లకు విశ్రాంతి"` was used in `buildDayAudioText`. The actual code outputs `"కాడెడ్లకు విశ్రాంతి ఇవ్వండి"`. Because `కాడెడ్లకు` does not contain `ఎడ్లకు`, the contract check fails and breaks the unit test suite.
- **Suggestion**: Update line 322 of `TeluguCalendarViewModel.kt` to include `"ఎడ్లకు విశ్రాంతి"`:
  ```kotlin
  if (day.isAmavasya) {
      sb.append("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి ఇవ్వండి. కాడెడ్లకు విశ్రాంతినిస్తూ చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
  }
  ```
  Or:
  ```kotlin
  if (day.isAmavasya) {
      sb.append("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
  }
  ```
  Both `"ఎడ్లకు విశ్రాంతి"` and `"దీపపు బుట్టలు"` will be present, satisfying both `Milestone2ChallengerCalendarVerificationTest` and real-world agrarian vernacular.

---

## 4. Adversarial Challenge & Stress-Test Assessment

**Overall Risk Assessment**: MEDIUM-HIGH (High test break; Core architecture is robust).

### 4.1 Stress Test Results Matrix

| Stress Test Scenario | Expected Behavior | Actual Behavior | Result |
|---|---|---|---|
| Concurrent task toggling (50 coroutines) | Thread-safe dispatch, all 50 updates dispatched | All 50 dispatched to repository without race conditions | **PASS** |
| Rapid sequential month switching (50 switches) | State consistency, year roll-over precision | Target year/month exactly calculated, zero desync | **PASS** |
| Extreme year boundary jumps (+10y, -20y) | Clean fallback, zero out-of-bounds crashes | Valid days rendered, no exception thrown | **PASS** |
| Massive task load (1,000 tasks) | Arithmetic grouping integrity and low latency | Active task counts grouped in <110ms | **PASS** |
| Archived task exclusion | Excluded from `taskCountsByDay` & `tasksForSelectedDay` | Correctly excluded from both | **PASS** |
| Cancelled task exclusion | Excluded from `taskCountsByDay` | Correctly excluded | **PASS** |
| Natural TTS finish & rapid audio toggles | State machine resets, restart without desync | `isSpeaking` flips accurately, no deadlock | **PASS** |
| Day-of-week spacer calculation (2025–2027) | `LocalDate.dayOfWeek.value % 7` matches Sunday..Saturday | Verified across all 36 months without off-by-one | **PASS** |
| Season partitioning completeness | 10 Kharif + 11 Rabi + 6 Summer = 27 Kartis | Disjoint and complete partition verified | **PASS** |
| Amavasya audio advice synthesis | Speech contains `"ఎడ్లకు విశ్రాంతి"` | Contains `"కాడెడ్లకు విశ్రాంతి"` -> Assertion failed | **FAIL** |

### 4.2 Code Architecture Positives
- **Type-Safe Combine Slices**: Nested combine using typed slices (`StateSlice1`, `StateSlice2`, `StateSlice3`) avoids untyped array casting issues.
- **Task Merging UX**: Intentionally maintains completed tasks in `tasksForSelectedDay` while filtering them out of `taskCountsByDay`. This allows farmers to toggle tasks done/undone in the bottom sheet without the UI jumping or disappearing.
- **Lifecycle Cleanliness**: `onCleared()` immediately stops any active TTS playback, preventing background audio and memory leaks.
- **Offline Ephemeris Fallback**: Math calculations gracefully handle years outside the 2025–2027 precomputed range.

---

## 5. Verified Claims vs Unverified Claims

### Verified Claims
- `taskCountsByDay` filters out completed, cancelled, and archived tasks: **VERIFIED (PASS)**
- `tasksForSelectedDay` retains both completed and pending tasks for selected epoch: **VERIFIED (PASS)**
- Month navigation handles December-to-January and January-to-December rollover: **VERIFIED (PASS)**
- 1-tap task creation pre-fills `initialDueDateEpochDay` in `ActivityFormScreen`: **VERIFIED (PASS)**
- `SeasonFilter` partitions 27 Kartis into 10 Kharif, 11 Rabi, 6 Summer: **VERIFIED (PASS)**
- `onCleared` stops `AudioGuidanceEngine`: **VERIFIED (PASS)**

### Unverified / Inconsistent Claims
- Claim: Worker stated `buildDayAudioText` uses `"ఎడ్లకు విశ్రాంతి"`: **DISPROVED (Code uses `"కాడెడ్లకు విశ్రాంతి"` and causes test failure)**

---

## 6. Caveats
- Per review constraints, the reviewer does not directly edit implementation source code. A worker agent or fix agent must apply the one-line fix to `TeluguCalendarViewModel.kt:322` and rerun `:feature:farm:testDebugUnitTest`.

---

## 7. Conclusion

**Verdict**: **REQUEST_CHANGES**

Milestone 2 implementation is ~98% complete with exceptionally solid reactive architecture, sound coroutine usage, robust date arithmetic, and thorough unit tests. However, due to a single string discrepancy in `buildDayAudioText` (`"కాడెడ్లకు విశ్రాంతి"` vs standardized `"ఎడ్లకు విశ్రాంతి"`), automated test `Milestone2ChallengerCalendarVerificationTest.verifyDayAudioText_adversarialAudioSynthesis` fails, causing the `:feature:farm:testDebugUnitTest` suite to fail.

Once `TeluguCalendarViewModel.kt` line 322 is updated to contain `"ఎడ్లకు విశ్రాంతి"` and `:feature:farm:testDebugUnitTest` runs green, Milestone 2 is ready for full approval.

---

## 8. Verification Method

To verify the required fix:
1. Update `TeluguCalendarViewModel.kt:322`:
   Replace `"కాడెడ్లకు విశ్రాంతి ఇవ్వండి"` with `"ఎడ్లకు విశ్రాంతి ఇవ్వండి"` (or `"ఎడ్లకు విశ్రాంతి ఇవ్వండి. కాడెడ్లకు విశ్రాంతినిస్తూ..."`).
2. Run testDebugUnitTest:
   ```powershell
   cd C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
   .\gradlew.bat :feature:farm:testDebugUnitTest
   ```
   **Expected Result**: `BUILD SUCCESSFUL`, all test classes (`TeluguCalendarViewModelTest`, `Milestone2ChallengerCalendarVerificationTest`, `TeluguCalendarViewModelAdversarialStressTest`, `FarmAndCalendarViewModelTest`) pass with 0 failures.
