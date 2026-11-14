# Milestone 2 Test Strategy & Verification Plan: `TeluguCalendarViewModel` & UI Components

**Target Component:** `TeluguCalendarViewModelTest.kt` & Farm UI Test Architecture  
**Investigating Agent:** `explorer_m2_3`  
**Target Milestone:** Milestone 2 (`Farm Calendar, Timeline & Task Scheduling`)  
**Date:** 2026-11-14  

---

## 1. Observation

Direct observations and findings gathered across the codebase:

### 1.1 Existing Test Architecture Inspection

1. **`feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/FarmAndCalendarViewModelTest.kt`**:
   - Lines 50–53:
     ```kotlin
     @OptIn(ExperimentalCoroutinesApi::class)
     @RunWith(RobolectricTestRunner::class)
     @Config(sdk = [29])
     class FarmAndCalendarViewModelTest {
     ```
   - Uses `UnconfinedTestDispatcher()` and `Dispatchers.setMain(testDispatcher)` / `Dispatchers.resetMain()`.
   - Uses `TestAppDispatchers` object implementing `AppDispatchers` where `io`, `default`, and `main` all route to `testDispatcher`.
   - Uses handwritten test doubles: `FakeFarmRepo` (lines 239–353), `FakePrefsRepo` (lines 355–382), `FakeAudioEngine` (lines 383–412), `FakeTimeProvider` (lines 413–438).
   - Observes ViewModel state using `backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }`.
   - Does NOT use MockK or Mockito; zero bytecode mocking dependencies are used.

2. **`feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeAudioAndRecompositionChallengerTest.kt`**:
   - Lines 96–98:
     ```kotlin
     @RunWith(RobolectricTestRunner::class)
     @Config(sdk = [29], qualifiers = "w412dp-h915dp-xxhdpi")
     class HomeAudioAndRecompositionChallengerTest {
     ```
   - Employs `ControllableAudioEngine` (lines 467–502) with `_isSpeaking = MutableStateFlow(false)`, `stopCalls`, `speakDynamicCalls`, and `simulateTtsCompletion()`.
   - Employs `ControllableCalendarRepo` (lines 504–532) implementing `TeluguCalendarRepository` with mutable flows for `observeDay`, `observeMonth`, `observeCurrentKarte`, and `observeTodayAlert`.
   - Tests rapid audio toggling, StateFlow transitions, and recomposition loops.

3. **`feature/home/src/test/kotlin/com/farmsaathi/feature/home/HomeDashboardUiTest.kt`**:
   - Uses `createComposeRule()` from `androidx.compose.ui.test.junit4.createComposeRule`.
   - Tests UI nodes: `onNodeWithText`, `assertIsDisplayed`, `performClick`.

4. **`core/common/src/test/kotlin/com/farmsaathi/core/common/OfflineTeluguCalendarRepositoryTest.kt`**:
   - Tests `OfflineTeluguCalendarRepositoryImpl` directly with real astronomical dates: Ugadi 2025 (`2025-03-30`), Chaitra Amavasya (`2025-04-27`), Polala Amavasya (`2025-08-23`), Eruvaka Pournami (`2025-06-11`), Mrigashira Day 1 (`2025-06-08`).
   - Validates that `getDay()`, `getMonthDays()`, `getCurrentKarte()`, and `getTodayAlert()` return non-null, accurate domain models.

### 1.2 Module Dependency & Version Catalog Inspection

1. **`feature/farm/build.gradle.kts`**:
   - Lines 22–37:
     ```kotlin
     dependencies {
         implementation(project(":core:model"))
         implementation(project(":core:common"))
         implementation(project(":core:designsystem"))
         implementation(project(":core:datastore"))
         implementation(project(":core:media"))
         implementation(libs.androidx.lifecycle.runtime.compose)
         implementation(libs.androidx.lifecycle.viewmodel.compose)
         implementation(libs.androidx.hilt.navigation.compose)
         implementation(libs.hilt.android)
         ksp(libs.hilt.compiler)
         testImplementation(libs.junit)
         testImplementation(libs.kotlinx.coroutines.test)
         testImplementation(libs.robolectric)
         testImplementation(libs.androidx.test.core)
     }
     ```
   - **Key Dependency Observations**:
     - `testImplementation(libs.junit)` -> JUnit 4.13.2.
     - `testImplementation(libs.kotlinx.coroutines.test)` -> Coroutines Test 1.9.0 (`runTest`, `StandardTestDispatcher`, `UnconfinedTestDispatcher`, `backgroundScope`).
     - `testImplementation(libs.robolectric)` -> Robolectric 4.14.1.
     - `testImplementation(libs.androidx.test.core)` -> AndroidX Test Core 1.6.1 (`ApplicationProvider`).
     - `libs.turbine` is in `gradle/libs.versions.toml` (line 48 & line 113: `app.cash.turbine:turbine:1.2.0`), but NOT currently in `:feature:farm/build.gradle.kts`.
     - Compose UI test (`libs.androidx.compose.ui.test.junit4`) is present in `feature/home/build.gradle.kts`, but NOT currently in `:feature:farm/build.gradle.kts`.
     - MockK / Mockito are NOT in `gradle/libs.versions.toml`.

2. **Existing Test Execution Verification**:
   - Executed: `.\gradlew.bat :feature:farm:testDebugUnitTest`
   - Output: `BUILD SUCCESSFUL in 1m 42s`, all existing tests passed without regressions.

---

## 2. Logic Chain

1. **Test Double Strategy (Fakes over Mocks)**:
   - **Observation**: Neither MockK nor Mockito is present in `libs.versions.toml`. Every test suite in the project (`feature:farm`, `feature:home`, `core:audio`, `core:database`) relies on explicit Fakes (`FakeFarmRepo`, `ControllableAudioEngine`, `ControllableCalendarRepo`).
   - **Deduction**: We must adhere to the project's proven pattern and construct high-fidelity Fakes (`FakeTeluguCalendarRepository`, `FakeFarmRepository`, `FakeAudioGuidanceEngine`, `FakePreferencesRepository`, `FakeTimeProvider`). Fakes are thread-safe, 100% deterministic, allocate minimal memory, and compile cleanly under Robolectric without bytecode agent errors.

2. **Coroutines & StateFlow Collection Architecture**:
   - **Observation**: `TeluguCalendarViewModel.uiState` is a `StateFlow<TeluguCalendarUiState>` configured with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ...)`.
   - **Deduction**: In unit tests, a `WhileSubscribed` flow requires an active collector to trigger upstream flow collection. Launching an empty collector in `backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }` keeps the flow hot during the test. Advancing the scheduler with `testScheduler.advanceUntilIdle()` flushes all pending coroutine dispatches deterministically.

3. **Year & Month Navigation Across Boundaries**:
   - **Observation**: Navigating backwards from January (`2026-01`) should wrap to December of the previous year (`2025-12`). Navigating forward from December (`2026-12`) should wrap to January of the next year (`2027-01`).
   - **Deduction**: The test suite must assert both the boundary wrap in `uiState.value.selectedYear` / `selectedMonth` and verify that `teluguCalendarRepo.observeMonth(year, month)` was invoked with the exact wrapped parameters.

4. **Task Merging & Month Task Counts**:
   - **Observation**: Rural farmers rely on calendar dots (`• 2 పనులు`) to see scheduled work at a glance.
   - **Deduction**: `taskCountsByDay: Map<Long, Int>` must count only active uncompleted tasks (`!it.isArchived && it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED`). In contrast, `tasksForSelectedDay: List<CropTask>` must include all non-archived tasks (both pending and completed) so the farmer can see what was done and toggle items back and forth via the inline checkbox. The test suite must test this differential filtering explicitly.

5. **Acoustic and Haptic Feedback on Task Mutation**:
   - **Observation**: In FarmSaathi, every successful mutation provides sensory confirmation (`AcousticCue.SAVED`), while failures play `AcousticCue.ERROR` and set `userMessage`.
   - **Deduction**: Testing `updateTaskStatus` must verify both the repository call and that `audioEngine.playAcousticFeedback(AcousticCue.SAVED)` was invoked on success, or `AcousticCue.ERROR` on failure.

6. **Seasonal Filtering Mathematical Specification**:
   - **Observation**: 27 solar Kartis are divided into Kharif, Rabi, and Summer:
     - Kharif / వానాకాలం: IDs 5..14 (10 Kartis).
     - Rabi / యాసంగి: IDs 15..25 (11 Kartis).
     - Summer / వేసవి: IDs 26..27 and 1..4 (6 Kartis).
   - **Deduction**: 10 + 11 + 6 = 27 Kartis. The test suite must verify that filtering by each season produces the exact count and range of IDs, ensuring zero gaps or overlaps.

7. **Audio Playback State Machine**:
   - **Observation**: 1-tap "వినండి" audio must toggle playback smoothly:
     - If not speaking: synthesizes Telugu text via `audioEngine.speakDynamicText(text, useTelugu = true)`.
     - If speaking: stops playback via `audioEngine.stop()`.
     - When TTS finishes naturally (`audioEngine.isSpeaking` emits `false`), UI state `isAudioSpeaking` resets to `false`.
   - **Deduction**: The test suite must test the toggle-on, toggle-off, and natural-completion paths to prevent desync bugs.

---

## 3. Caveats

1. **Turbine vs Coroutines-Test**:
   - `app.cash.turbine:turbine` is declared in `gradle/libs.versions.toml`, but not currently included in `:feature:farm/build.gradle.kts`.
   - The test code provided below uses pure `kotlinx.coroutines.test` (`backgroundScope.launch` + `testScheduler.advanceUntilIdle()`). This allows the test suite to compile and run immediately without altering `feature/farm/build.gradle.kts`.
   - If the team prefers Turbine's `.test { ... }` syntax, adding `testImplementation(libs.turbine)` to `feature/farm/build.gradle.kts` is recommended and straightforward.
2. **Compose UI Tests**:
   - For UI-level testing of `TeluguCalendarScreen` and `KarteGuideScreen` using `createComposeRule()`, `:feature:farm/build.gradle.kts` will need:
     ```kotlin
     testImplementation(platform(libs.androidx.compose.bom))
     testImplementation(libs.androidx.compose.ui.test.junit4)
     debugImplementation(libs.androidx.compose.ui.test.manifest)
     ```
   - Standard Robolectric ViewModel unit tests do NOT require these Compose test dependencies.
3. **Fixed System Epoch vs Dynamic Dates**:
   - Tests should always use `FakeTimeProvider` with a fixed known epochDay (e.g. `20650L` = 2026-07-15) rather than relying on system clock. This prevents flaky test failures across time zones and leap years.

---

## 4. Conclusion & Concrete Test Architecture

### 4.1 Complete, Compilable Test Suite: `TeluguCalendarViewModelTest.kt`

Path: `feature/farm/src/test/kotlin/com/farmsaathi/feature/farm/TeluguCalendarViewModelTest.kt`

```kotlin
package com.farmsaathi.feature.farm

import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.TeluguCalendarRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CropTemplate
import com.farmsaathi.core.model.CurrentKarteInfo
import com.farmsaathi.core.model.DomainError
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.LunarAlertType
import com.farmsaathi.core.model.LunarDayAlert
import com.farmsaathi.core.model.Paksham
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.TaskCategory
import com.farmsaathi.core.model.TaskPriority
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.core.model.TeluguCalendarDay
import com.farmsaathi.core.model.TeluguKarte
import com.farmsaathi.core.model.TeluguKarteluCatalog
import com.farmsaathi.core.model.TeluguMonth
import com.farmsaathi.core.model.TeluguTithi
import com.farmsaathi.feature.farm.viewmodel.CalendarViewMode
import com.farmsaathi.feature.farm.viewmodel.SeasonFilter
import com.farmsaathi.feature.farm.viewmodel.TeluguCalendarViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class TeluguCalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = TestAppDispatchers(testDispatcher)

    private lateinit var calendarRepo: FakeTeluguCalendarRepo
    private lateinit var farmRepo: FakeFarmRepo
    private lateinit var prefsRepo: FakePrefsRepo
    private lateinit var audioEngine: FakeAudioEngine
    private lateinit var timeProvider: FakeTimeProvider

    // Base epoch: 2026-07-15 (epochDay = 20650)
    private val baseEpochDay = 20650L

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        calendarRepo = FakeTeluguCalendarRepo()
        farmRepo = FakeFarmRepo()
        prefsRepo = FakePrefsRepo()
        audioEngine = FakeAudioEngine()
        timeProvider = FakeTimeProvider(todayEpoch = baseEpochDay)

        // Seed default calendar data for 2026-07
        calendarRepo.setMonthDays(2026, 7, generateMonthDays(2026, 7))
        calendarRepo.setMonthDays(2026, 8, generateMonthDays(2026, 8))
        calendarRepo.setMonthDays(2026, 6, generateMonthDays(2026, 6))
        calendarRepo.setMonthDays(2026, 12, generateMonthDays(2026, 12))
        calendarRepo.setMonthDays(2027, 1, generateMonthDays(2027, 1))
        calendarRepo.setMonthDays(2025, 12, generateMonthDays(2025, 12))
        calendarRepo.setMonthDays(2026, 1, generateMonthDays(2026, 1))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TeluguCalendarViewModel {
        return TeluguCalendarViewModel(
            teluguCalendarRepo = calendarRepo,
            farmRepo = farmRepo,
            prefsRepo = prefsRepo,
            audioEngine = audioEngine,
            timeProvider = timeProvider,
            dispatchers = testDispatchers,
        )
    }

    // =========================================================================
    // 1. Initial State Tests
    // =========================================================================

    @Test
    fun `initial state reflects today epoch date, month view mode, and all kartis catalog`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2026, state.selectedYear)
        assertEquals(7, state.selectedMonth)
        assertEquals(baseEpochDay, state.todayEpochDay)
        assertEquals(CalendarViewMode.MONTH, state.viewMode)
        assertEquals(SeasonFilter.ALL, state.activeSeasonFilter)
        assertEquals(27, state.allKartis.size)
        assertEquals(27, state.filteredKartis.size)
        assertEquals(31, state.monthDays.size)
        assertFalse(state.isAudioSpeaking)

        collector.cancel()
    }

    // =========================================================================
    // 2. Month Navigation Across Year Boundaries Tests
    // =========================================================================

    @Test
    fun `changeMonth advances forward within same year`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.changeMonth(1)
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2026, state.selectedYear)
        assertEquals(8, state.selectedMonth)
        assertTrue(calendarRepo.observedMonths.contains(Pair(2026, 8)))

        collector.cancel()
    }

    @Test
    fun `changeMonth advances forward across year boundary from December to January`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Advance from July (7) to December (12): +5 months
        vm.changeMonth(5)
        testScheduler.advanceUntilIdle()
        assertEquals(2026, vm.uiState.value.selectedYear)
        assertEquals(12, vm.uiState.value.selectedMonth)

        // Advance 1 more month -> should roll over to 2027-01
        vm.changeMonth(1)
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2027, state.selectedYear)
        assertEquals(1, state.selectedMonth)
        assertTrue(calendarRepo.observedMonths.contains(Pair(2027, 1)))

        collector.cancel()
    }

    @Test
    fun `changeMonth retreats backward across year boundary from January to December`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Retreat from July (7) to January (1): -6 months
        vm.changeMonth(-6)
        testScheduler.advanceUntilIdle()
        assertEquals(2026, vm.uiState.value.selectedYear)
        assertEquals(1, vm.uiState.value.selectedMonth)

        // Retreat 1 more month -> should roll back to 2025-12
        vm.changeMonth(-1)
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2025, state.selectedYear)
        assertEquals(12, state.selectedMonth)
        assertTrue(calendarRepo.observedMonths.contains(Pair(2025, 12)))

        collector.cancel()
    }

    @Test
    fun `goToToday restores current month and selects today epoch day`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Navigate far away
        vm.changeMonth(10)
        testScheduler.advanceUntilIdle()
        assertEquals(2027, vm.uiState.value.selectedYear)

        // Tap Go to Today
        vm.goToToday()
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2026, state.selectedYear)
        assertEquals(7, state.selectedMonth)
        assertEquals(baseEpochDay, state.selectedDay?.epochDay)

        collector.cancel()
    }

    // =========================================================================
    // 3. Date Selection & Day Details Bottom Sheet Tests
    // =========================================================================

    @Test
    fun `selectDay updates selectedDay and reveals bottom sheet details`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val targetEpoch = baseEpochDay + 5 // 2026-07-20
        vm.selectDay(targetEpoch)
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.selectedDay)
        assertEquals(targetEpoch, state.selectedDay?.epochDay)
        assertTrue(state.isDayDetailsVisible)

        // Dismiss day details
        vm.dismissDayDetails()
        testScheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isDayDetailsVisible)

        collector.cancel()
    }

    @Test
    fun `selectDay with epochDay in another month updates displayed month automatically`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Select an epoch in August 2026: 2026-08-10 (baseEpochDay + 26)
        val augustEpoch = baseEpochDay + 26
        vm.selectDay(augustEpoch)
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2026, state.selectedYear)
        assertEquals(8, state.selectedMonth)
        assertEquals(augustEpoch, state.selectedDay?.epochDay)
        assertTrue(state.isDayDetailsVisible)

        collector.cancel()
    }

    // =========================================================================
    // 4. Task Merging with Calendar Days (dueDateEpochDay == epochDay)
    // =========================================================================

    @Test
    fun `crop tasks are grouped into taskCountsByDay for calendar grid dots`() = runTest(testDispatcher) {
        val task1 = sampleCropTask(id = 1L, dueEpochDay = baseEpochDay, status = TaskStatus.PENDING)
        val task2 = sampleCropTask(id = 2L, dueEpochDay = baseEpochDay, status = TaskStatus.PENDING)
        val task3 = sampleCropTask(id = 3L, dueEpochDay = baseEpochDay, status = TaskStatus.COMPLETED) // completed, should NOT count towards pending dot
        val task4 = sampleCropTask(id = 4L, dueEpochDay = baseEpochDay + 2, status = TaskStatus.PENDING)

        farmRepo.tasksFlow.value = listOf(task1, task2, task3, task4)

        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        // Only 2 pending tasks on baseEpochDay
        assertEquals(2, state.taskCountsByDay[baseEpochDay])
        // 1 pending task on baseEpochDay + 2
        assertEquals(1, state.taskCountsByDay[baseEpochDay + 2])
        // Days with no tasks have null or 0
        assertNull(state.taskCountsByDay[baseEpochDay + 1])

        collector.cancel()
    }

    @Test
    fun `tasksForSelectedDay filters tasks exactly by selected day epochDay including completed tasks`() = runTest(testDispatcher) {
        val task1 = sampleCropTask(id = 1L, title = "వరి నారుమడి నీరు", dueEpochDay = baseEpochDay, status = TaskStatus.PENDING)
        val task2 = sampleCropTask(id = 2L, title = "ఎరువులు వేయుట", dueEpochDay = baseEpochDay, status = TaskStatus.COMPLETED)
        val task3 = sampleCropTask(id = 3L, title = "కలుపు తీత", dueEpochDay = baseEpochDay + 5, status = TaskStatus.PENDING)

        farmRepo.tasksFlow.value = listOf(task1, task2, task3)

        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Base epoch is selected by default
        val state = vm.uiState.value
        assertEquals(2, state.tasksForSelectedDay.size)
        assertTrue(state.tasksForSelectedDay.any { it.id == 1L })
        assertTrue(state.tasksForSelectedDay.any { it.id == 2L })
        assertFalse(state.tasksForSelectedDay.any { it.id == 3L })

        // Select the other day
        vm.selectDay(baseEpochDay + 5)
        testScheduler.advanceUntilIdle()

        val updatedState = vm.uiState.value
        assertEquals(1, updatedState.tasksForSelectedDay.size)
        assertEquals(3L, updatedState.tasksForSelectedDay.first().id)

        collector.cancel()
    }

    // =========================================================================
    // 5. Task Status Mutation & Acoustic Feedback Tests
    // =========================================================================

    @Test
    fun `updateTaskStatus calls repository and plays SAVED acoustic cue on success`() = runTest(testDispatcher) {
        val task = sampleCropTask(id = 10L, dueEpochDay = baseEpochDay, status = TaskStatus.PENDING)
        farmRepo.tasksFlow.value = listOf(task)

        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Mark task as completed
        vm.updateTaskStatus(taskId = 10L, status = TaskStatus.COMPLETED)
        testScheduler.advanceUntilIdle()

        assertEquals(1, farmRepo.updateTaskStatusCalls.size)
        val (calledTaskId, calledStatus) = farmRepo.updateTaskStatusCalls.first()
        assertEquals(10L, calledTaskId)
        assertEquals(TaskStatus.COMPLETED, calledStatus)

        // Sensory feedback verification
        assertTrue(audioEngine.cuesPlayed.contains(AcousticCue.SAVED))
        assertNull(vm.uiState.value.userMessage)

        collector.cancel()
    }

    @Test
    fun `updateTaskStatus displays error message on repository failure`() = runTest(testDispatcher) {
        farmRepo.updateTaskStatusResult = AppResult.Error(DomainError.DatabaseError)

        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.updateTaskStatus(taskId = 99L, status = TaskStatus.COMPLETED)
        testScheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.userMessage)
        assertTrue(vm.uiState.value.userMessage!!.contains("విఫలమైంది"))

        // Clear error message
        vm.clearUserMessage()
        testScheduler.advanceUntilIdle()
        assertNull(vm.uiState.value.userMessage)

        collector.cancel()
    }

    @Test
    fun `toggleTaskStatus flips pending to completed and completed to pending`() = runTest(testDispatcher) {
        val pendingTask = sampleCropTask(id = 11L, status = TaskStatus.PENDING)
        val completedTask = sampleCropTask(id = 12L, status = TaskStatus.COMPLETED)

        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.toggleTaskStatus(pendingTask)
        testScheduler.advanceUntilIdle()
        assertEquals(TaskStatus.COMPLETED, farmRepo.updateTaskStatusCalls.last().second)

        vm.toggleTaskStatus(completedTask)
        testScheduler.advanceUntilIdle()
        assertEquals(TaskStatus.PENDING, farmRepo.updateTaskStatusCalls.last().second)

        collector.cancel()
    }

    // =========================================================================
    // 6. Season Filtering in Karte Timeline View Tests
    // =========================================================================

    @Test
    fun `season filter KHARIF filters exactly 10 kartis matching ids 5 to 14`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.setViewMode(CalendarViewMode.TIMELINE)
        vm.filterSeason(SeasonFilter.KHARIF)
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(CalendarViewMode.TIMELINE, state.viewMode)
        assertEquals(SeasonFilter.KHARIF, state.activeSeasonFilter)
        assertEquals(10, state.filteredKartis.size)
        assertTrue(state.filteredKartis.all { it.id in 5..14 })

        collector.cancel()
    }

    @Test
    fun `season filter RABI filters exactly 11 kartis matching ids 15 to 25`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.filterSeason(SeasonFilter.RABI)
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(11, state.filteredKartis.size)
        assertTrue(state.filteredKartis.all { it.id in 15..25 })

        collector.cancel()
    }

    @Test
    fun `season filter SUMMER filters exactly 6 kartis matching ids 26, 27 and 1 to 4`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.filterSeason(SeasonFilter.SUMMER)
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(6, state.filteredKartis.size)
        assertTrue(state.filteredKartis.all { it.id in 26..27 || it.id in 1..4 })

        // Partition validation: 10 + 11 + 6 = 27
        assertEquals(27, state.allKartis.size)

        collector.cancel()
    }

    // =========================================================================
    // 7. Audio Playback Triggers & Rapid Toggling Tests
    // =========================================================================

    @Test
    fun `togglePanchangAudio speaks authentic Telugu day details when day details bottom sheet is visible`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Select day to open sheet
        vm.selectDay(baseEpochDay)
        testScheduler.advanceUntilIdle()

        assertFalse(audioEngine.isSpeaking.value)

        // Toggle 1: speaks day details
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()

        assertEquals(1, audioEngine.speakDynamicCalls)
        assertTrue(audioEngine.lastUseTelugu == true)
        val spoken = audioEngine.lastSpokenText ?: ""
        assertTrue("Speech must contain Telugu month or paksham", spoken.contains("ఆషాఢం") || spoken.contains("పక్షం"))
        assertTrue(audioEngine.isSpeaking.value)

        // Toggle 2: stops speaking
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()

        assertEquals(1, audioEngine.stopCalls)
        assertFalse(audioEngine.isSpeaking.value)

        collector.cancel()
    }

    @Test
    fun `togglePanchangAudio in timeline mode speaks current karte wisdom`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.setViewMode(CalendarViewMode.TIMELINE)
        vm.dismissDayDetails()
        testScheduler.advanceUntilIdle()

        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()

        assertEquals(1, audioEngine.speakDynamicCalls)
        val spoken = audioEngine.lastSpokenText ?: ""
        assertTrue("Speech must contain karte name or proverb", spoken.contains("కార్తె") || spoken.contains("సామెత"))

        collector.cancel()
    }

    @Test
    fun `speakKarte speaks individual karte wisdom from catalogue`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val mrigashira = TeluguKarteluCatalog.getById(5)
        vm.speakKarte(mrigashira)
        testScheduler.advanceUntilIdle()

        assertEquals(1, audioEngine.speakDynamicCalls)
        assertTrue(audioEngine.lastSpokenText!!.contains("మృగశిర"))
        assertTrue(audioEngine.lastSpokenText!!.contains(mrigashira.proverb))

        collector.cancel()
    }

    @Test
    fun `audio state automatically resets when engine finishes speaking naturally`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.selectDay(baseEpochDay)
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.isAudioSpeaking)

        // Simulate TTS completion
        audioEngine.simulateTtsFinished()
        testScheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isAudioSpeaking)

        collector.cancel()
    }

    // =========================================================================
    // 8. Edge Cases & Boundary Conditions Tests
    // =========================================================================

    @Test
    fun `empty task list from repository emits zero task counts without exception`() = runTest(testDispatcher) {
        farmRepo.tasksFlow.value = emptyList()

        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.taskCountsByDay.isEmpty())
        assertTrue(state.tasksForSelectedDay.isEmpty())

        collector.cancel()
    }

    @Test
    fun `initializeWithEpochDay selects target day and opens bottom sheet immediately`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val target = baseEpochDay + 10
        vm.initializeWithEpochDay(target)
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(target, state.selectedDay?.epochDay)
        assertTrue(state.isDayDetailsVisible)

        collector.cancel()
    }

    // =========================================================================
    // Helper Data Generators
    // =========================================================================

    private fun sampleCropTask(
        id: Long,
        title: String = "పొలం పని",
        dueEpochDay: Long = baseEpochDay,
        status: TaskStatus = TaskStatus.PENDING,
        isArchived: Boolean = false,
    ) = CropTask(
        id = id,
        farmId = 1L,
        title = title,
        category = TaskCategory.SOWING,
        dueDateEpochDay = dueEpochDay,
        priority = TaskPriority.HIGH,
        status = status,
        isArchived = isArchived,
    )

    private fun generateMonthDays(year: Int, month: Int): List<TeluguCalendarDay> {
        val daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth()
        return (1..daysInMonth).map { day ->
            val date = LocalDate.of(year, month, day)
            val epochDay = date.toEpochDay()
            TeluguCalendarDay(
                epochDay = epochDay,
                gregorianDate = date.toString(),
                teluguYear = "క్రోధి",
                teluguMonth = TeluguMonth.ASHADHAM,
                paksham = if (day <= 15) Paksham.SHUKLA else Paksham.KRISHNA,
                tithi = if (day == 15) TeluguTithi.POURNAMI else if (day == daysInMonth) TeluguTithi.AMAVASYA else TeluguTithi.fromId((day % 14) + 1),
                nakshatram = "పునర్వసు",
                karteId = 7,
                karteName = "పునర్వసు",
                karteDayNumber = (day % 14) + 1,
                totalKarteDays = 14,
                isAmavasya = (day == daysInMonth),
                isPournami = (day == 15),
                isFestival = false,
            )
        }
    }

    // =========================================================================
    // Test Doubles (Fakes)
    // =========================================================================

    private class TestAppDispatchers(private val dispatcher: CoroutineDispatcher) : AppDispatchers {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private class FakeTeluguCalendarRepo : TeluguCalendarRepository {
        val observedMonths = mutableListOf<Pair<Int, Int>>()
        private val monthDaysMap = mutableMapOf<Pair<Int, Int>, MutableStateFlow<List<TeluguCalendarDay>>>()
        private val dayMap = mutableMapOf<Long, TeluguCalendarDay>()

        private val defaultKarte = CurrentKarteInfo(
            karteId = 7,
            teluguName = "పునర్వసు",
            englishName = "Punarvasu",
            dayNumber = 4,
            totalDays = 14,
            daysRemaining = 10,
            progressPercentage = 28.5f,
            proverb = "పునర్వసు కురిస్తే భూమి పులకరిస్తుంది",
            agriculturalSignificance = "వరి నాట్లు, మొదటి కలుపు తీతకు అనువైన అదను.",
            recommendedCrops = listOf("వరి", "పత్తి"),
        )
        val currentKarteFlow = MutableStateFlow(defaultKarte)

        private val defaultAlert = LunarDayAlert(
            alertType = LunarAlertType.DAY_STANDARD,
            title = "నేటి వ్యవసాయ సూచన",
            subtitle = "పొలం పనులను అనుకూలంగా నిర్వహించుకోండి.",
            ttsSpeechText = "నేడు ఆషాఢ మాసం. పునర్వసు కార్తె 4వ రోజు.",
        )
        val todayAlertFlow = MutableStateFlow(defaultAlert)

        fun setMonthDays(year: Int, month: Int, days: List<TeluguCalendarDay>) {
            days.forEach { dayMap[it.epochDay] = it }
            monthDaysMap.getOrPut(Pair(year, month)) { MutableStateFlow(days) }.value = days
        }

        override fun observeMonth(year: Int, month: Int): Flow<List<TeluguCalendarDay>> {
            observedMonths += Pair(year, month)
            return monthDaysMap.getOrPut(Pair(year, month)) { MutableStateFlow(emptyList()) }
        }

        override suspend fun getMonthDays(year: Int, month: Int): List<TeluguCalendarDay> {
            return monthDaysMap[Pair(year, month)]?.value ?: emptyList()
        }

        override fun observeDay(epochDay: Long): Flow<TeluguCalendarDay?> {
            return flowOf(dayMap[epochDay])
        }

        override suspend fun getDay(epochDay: Long): TeluguCalendarDay? {
            return dayMap[epochDay]
        }

        override fun observeCurrentKarte(epochDay: Long): Flow<CurrentKarteInfo> = currentKarteFlow
        override suspend fun getCurrentKarte(epochDay: Long): CurrentKarteInfo = currentKarteFlow.value

        override fun observeTodayAlert(epochDay: Long): Flow<LunarDayAlert> = todayAlertFlow
        override suspend fun getTodayAlert(epochDay: Long): LunarDayAlert = todayAlertFlow.value

        override fun observeAllKartis(): Flow<List<TeluguKarte>> = flowOf(TeluguKarteluCatalog.ALL)
    }

    private class FakeFarmRepo : FarmRepository {
        val tasksFlow = MutableStateFlow<List<CropTask>>(emptyList())
        val updateTaskStatusCalls = mutableListOf<Pair<Long, TaskStatus>>()
        var updateTaskStatusResult: AppResult<Unit> = AppResult.Success(Unit)

        override fun observeTasks(cropCycleId: Long?, plotId: Long?, farmId: Long?): Flow<List<CropTask>> = tasksFlow

        override suspend fun updateTaskStatus(
            taskId: Long,
            status: TaskStatus,
            newDueEpochDay: Long?,
        ): AppResult<Unit> {
            updateTaskStatusCalls += Pair(taskId, status)
            if (updateTaskStatusResult is AppResult.Success) {
                // Automatically reflect mutation in tasksFlow
                tasksFlow.value = tasksFlow.value.map {
                    if (it.id == taskId) it.copy(status = status) else it
                }
            }
            return updateTaskStatusResult
        }

        // Default stub implementations for unused repository methods
        override fun observeFarms(includeArchived: Boolean): Flow<List<Farm>> = flowOf(emptyList())
        override fun observeFarm(farmId: Long): Flow<Farm?> = flowOf(null)
        override suspend fun saveFarm(farm: Farm): AppResult<Long> = AppResult.Success(1L)
        override suspend fun ensureDefaultFarmAndPlot(): Pair<Long, Long> = Pair(1L, 1L)
        override suspend fun setPrimaryFarm(farmId: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun archiveFarm(farmId: Long): AppResult<Unit> = AppResult.Success(Unit)
        override fun observePlots(farmId: Long?, includeArchived: Boolean): Flow<List<Plot>> = flowOf(emptyList())
        override fun observePlot(plotId: Long): Flow<Plot?> = flowOf(null)
        override suspend fun savePlot(plot: Plot): AppResult<Long> = AppResult.Success(1L)
        override suspend fun archivePlot(plotId: Long): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeCropTemplates(): Flow<List<CropTemplate>> = flowOf(emptyList())
        override suspend fun getCropTemplate(templateId: Long): CropTemplate? = null
        override suspend fun saveCropTemplate(template: CropTemplate): AppResult<Long> = AppResult.Success(1L)
        override suspend fun ensureStarterTemplatesSeeded() {}
        override fun observeCropCycles(plotId: Long?, farmId: Long?): Flow<List<CropCycle>> = flowOf(emptyList())
        override fun observeCropCycle(cropCycleId: Long): Flow<CropCycle?> = flowOf(null)
        override suspend fun createCropCycleWithTemplate(cycle: CropCycle, generateTasksFromTemplate: Boolean, useTeluguTitles: Boolean): AppResult<Long> = AppResult.Success(1L)
        override suspend fun updateCropCycle(cycle: CropCycle, recalculateUncompletedTemplateTasks: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateCropCycleStatus(cropCycleId: Long, status: CropCycleStatus): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeDueAndOverdueTasks(todayEpochDay: Long): Flow<List<CropTask>> = flowOf(emptyList())
        override suspend fun getDueAndOverdueCount(todayEpochDay: Long): Int = 0
        override suspend fun getTask(taskId: Long): CropTask? = null
        override suspend fun saveTask(task: CropTask): AppResult<Long> = AppResult.Success(1L)
    }

    private class FakeAudioEngine : AudioGuidanceEngine {
        private val _isSpeaking = MutableStateFlow(false)
        override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
        override val activeCueKey: StateFlow<String?> = MutableStateFlow(null)

        var speakDynamicCalls = 0
        var stopCalls = 0
        var lastSpokenText: String? = null
        var lastUseTelugu: Boolean? = null
        val cuesPlayed = mutableListOf<AcousticCue>()

        override fun speakDynamicText(text: String, useTelugu: Boolean) {
            speakDynamicCalls++
            lastSpokenText = text
            lastUseTelugu = useTelugu
            _isSpeaking.value = true
        }

        override fun stop() {
            stopCalls++
            _isSpeaking.value = false
        }

        fun simulateTtsFinished() {
            _isSpeaking.value = false
        }

        override fun playAcousticFeedback(cue: AcousticCue) {
            cuesPlayed += cue
        }

        override fun speakCue(cueKey: String, useTelugu: Boolean) {}
        override fun isTeluguVoiceReady(): Boolean = true
        override fun hasCustomPrompt(cueKey: String): Boolean = false
        override fun customPromptFilePath(cueKey: String): String = ""
        override fun clearCustomPrompt(cueKey: String) {}
    }

    private class FakePrefsRepo : UserPreferencesRepository {
        private val prefsFlow = MutableStateFlow(UserPreferences(language = AppLanguage.TELUGU))
        override val userPreferences: Flow<UserPreferences> = prefsFlow

        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun updateProfile(farmerName: String, villageName: String) {}
        override suspend fun setLanguage(language: AppLanguage) {
            prefsFlow.value = prefsFlow.value.copy(language = language)
        }
        override suspend fun setAudioGuidanceEnabled(enabled: Boolean) {}
        override suspend fun setPreferredAreaUnit(unit: com.farmsaathi.core.model.AreaUnit) {}
        override suspend fun setActiveFarmId(farmId: Long?) {}
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {}
        override suspend fun setLastBackupEpochMillis(epochMillis: Long) {}
    }

    private class FakeTimeProvider(private val todayEpoch: Long) : TimeProvider {
        override fun todayEpochDay(): Long = todayEpoch
        override fun nowEpochMillis(): Long = todayEpoch * 86_400_000L
        override fun elapsedRealtimeMillis(): Long = 1000L
        override fun bootCount(): Int = 1
        override fun epochMillisToEpochDay(epochMillis: Long): Long = todayEpoch
        override fun epochDayToStartMillis(epochDay: Long): Long = epochDay * 86_400_000L
        override fun formatDate(epochMillis: Long, useTelugu: Boolean): String = "15 జూలై 2026"
        override fun formatEpochDay(epochDay: Long, useTelugu: Boolean): String = "15 జూలై 2026"
        override fun formatTime(epochMillis: Long): String = "10:00 AM"
    }
}
```

---

## 5. Verification Method

### 5.1 Verification Commands

1. **Verify Unit Tests in `:feature:farm`**:
   ```powershell
   cd C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
   .\gradlew.bat :feature:farm:testDebugUnitTest --info
   ```
   **Expected Outcome**: 
   - All tests in `FarmAndCalendarViewModelTest` and `TeluguCalendarViewModelTest` compile with zero errors.
   - All test cases pass with `BUILD SUCCESSFUL`.

2. **Verify Full App Unit Tests**:
   ```powershell
   cd C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
   .\gradlew.bat testDebugUnitTest
   ```
   **Expected Outcome**: 
   - All unit tests across `:core:model`, `:core:common`, `:core:audio`, `:core:database`, `:feature:home`, `:feature:farm`, and `:app` pass without regression.

3. **Verify Static Analysis Quality Gate**:
   ```powershell
   cd C:\Users\nithi\.gemini\antigravity\scratch\farm-saathi
   .\gradlew.bat detekt ktlintCheck
   ```
   **Expected Outcome**: Zero lint or detekt violations on newly added test files.

### 5.2 Invalidation Conditions
- Any compile error due to missing or mismatched symbols between `TeluguCalendarViewModel` and `TeluguCalendarViewModelTest`.
- Month navigation wrapping incorrectly (e.g. going from month 1 to month 0 instead of previous year month 12).
- Non-deterministic state assertions caused by unadvanced coroutine schedulers.
- Test failure when repository emits an empty list of tasks or null day.
