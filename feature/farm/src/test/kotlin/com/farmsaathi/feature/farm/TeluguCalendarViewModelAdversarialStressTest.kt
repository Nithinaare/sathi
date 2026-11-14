package com.farmsaathi.feature.farm

import androidx.lifecycle.ViewModel
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.OfflineTeluguCalendarRepositoryImpl
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
import com.farmsaathi.feature.farm.viewmodel.TeluguCalendarViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

/**
 * Adversarial stress and boundary testing harness for [TeluguCalendarViewModel].
 *
 * Verifies:
 * 1. Rapid state mutation (rapid month changes, concurrent task toggles, rapid audio toggling).
 * 2. Boundary year transitions and mathematical fallbacks (2025-01 -> 2024-12, 2027-12 -> 2028-01, leap years).
 * 3. Task count integrity under edge cases (archived tasks, canceled tasks, zero tasks, massive task loads).
 * 4. Audio guidance state machine desynchronization resistance and lifecycle cleanup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class TeluguCalendarViewModelAdversarialStressTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = TestAppDispatchers(testDispatcher)

    private lateinit var fakeCalendarRepo: FakeTeluguCalendarRepo
    private lateinit var realCalendarRepo: TeluguCalendarRepository
    private lateinit var farmRepo: FakeFarmRepo
    private lateinit var prefsRepo: FakePrefsRepo
    private lateinit var audioEngine: FakeAudioEngine
    private lateinit var timeProvider: FakeTimeProvider

    // Base epoch: 2026-07-15 (epochDay = 20650)
    private val baseEpochDay = 20650L

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeCalendarRepo = FakeTeluguCalendarRepo()
        realCalendarRepo = OfflineTeluguCalendarRepositoryImpl()
        farmRepo = FakeFarmRepo()
        prefsRepo = FakePrefsRepo()
        audioEngine = FakeAudioEngine()
        timeProvider = FakeTimeProvider(todayEpoch = baseEpochDay)

        // Seed default calendar data for 2026-07 in fake repo
        fakeCalendarRepo.setMonthDays(2026, 7, generateMonthDays(2026, 7))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(useRealRepo: Boolean = false): TeluguCalendarViewModel {
        return TeluguCalendarViewModel(
            teluguCalendarRepo = if (useRealRepo) realCalendarRepo else fakeCalendarRepo,
            farmRepo = farmRepo,
            prefsRepo = prefsRepo,
            audioEngine = audioEngine,
            timeProvider = timeProvider,
            dispatchers = testDispatchers,
        )
    }

    // =========================================================================
    // 1. Rapid State Mutation & Concurrency Stress Tests
    // =========================================================================

    @Test
    fun `rapid month switching sequential 50 times computes exact target year and month`() = runTest(testDispatcher) {
        val vm = createViewModel(useRealRepo = true)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Starting at 2026-07, advance 50 months:
        // 50 months = 4 years (48 months) + 2 months -> 2030-09
        repeat(50) {
            vm.changeMonth(1)
        }
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2030, state.selectedYear)
        assertEquals(9, state.selectedMonth)
        assertEquals(30, state.monthDays.size) // September has 30 days
        assertTrue(state.monthDays.all { it.epochDay != 0L })

        // Now retreat 50 months back
        repeat(50) {
            vm.changeMonth(-1)
        }
        testScheduler.advanceUntilIdle()

        val restoredState = vm.uiState.value
        assertEquals(2026, restoredState.selectedYear)
        assertEquals(7, restoredState.selectedMonth)
        assertEquals(31, restoredState.monthDays.size)

        collector.cancel()
    }

    @Test
    fun `rapid zig-zag month changes maintain state consistency and correct days count`() = runTest(testDispatcher) {
        val vm = createViewModel(useRealRepo = true)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val offsets = listOf(1, -1, 3, -2, 12, -24, 6, -1, -5, 10)
        var expectedDate = LocalDate.of(2026, 7, 1)
        for (offset in offsets) {
            vm.changeMonth(offset)
            expectedDate = expectedDate.plusMonths(offset.toLong())
        }
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(expectedDate.year, state.selectedYear)
        assertEquals(expectedDate.monthValue, state.selectedMonth)
        assertEquals(expectedDate.lengthOfMonth(), state.monthDays.size)

        collector.cancel()
    }

    @Test
    fun `concurrent task toggling across 50 coroutines dispatches every update accurately`() = runTest(testDispatcher) {
        val tasks = (1..50).map { id ->
            sampleCropTask(id = id.toLong(), dueEpochDay = baseEpochDay, status = TaskStatus.PENDING)
        }
        farmRepo.tasksFlow.value = tasks

        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        assertEquals(50, vm.uiState.value.taskCountsByDay[baseEpochDay])

        // Launch 50 concurrent toggles
        val jobs = tasks.map { task ->
            backgroundScope.async {
                vm.toggleTaskStatus(task)
            }
        }
        jobs.awaitAll()
        testScheduler.advanceUntilIdle()

        // Verify that all 50 tasks were updated to COMPLETED
        assertEquals(50, farmRepo.updateTaskStatusCalls.size)
        assertTrue(farmRepo.updateTaskStatusCalls.all { it.second == TaskStatus.COMPLETED })
        assertEquals(50, audioEngine.cuesPlayed.size)
        assertTrue(audioEngine.cuesPlayed.all { it == AcousticCue.SAVED })

        // Task count on baseEpochDay should now be null/0 active pending tasks
        val finalState = vm.uiState.value
        assertNull(finalState.taskCountsByDay[baseEpochDay])
        // But all 50 completed tasks still appear in tasksForSelectedDay
        assertEquals(50, finalState.tasksForSelectedDay.size)
        assertTrue(finalState.tasksForSelectedDay.all { it.status == TaskStatus.COMPLETED })

        collector.cancel()
    }

    @Test
    fun `rapid audio toggles do not desynchronize engine state machine`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.selectDay(baseEpochDay)
        testScheduler.advanceUntilIdle()

        // Rapid 10-toggle burst
        repeat(10) { i ->
            vm.togglePanchangAudio()
            testScheduler.advanceUntilIdle()
            // Even toggles turn ON (speaking = true), odd toggles turn OFF (speaking = false)
            val expectedSpeaking = (i % 2 == 0)
            assertEquals("Iteration $i speaking state mismatch", expectedSpeaking, vm.uiState.value.isAudioSpeaking)
        }

        assertEquals(5, audioEngine.speakDynamicCalls)
        assertEquals(5, audioEngine.stopCalls)

        collector.cancel()
    }

    @Test
    fun `speakKarte stops existing playback and starts new speech without crash`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.selectDay(baseEpochDay)
        vm.togglePanchangAudio() // starts speaking day details
        testScheduler.advanceUntilIdle()
        assertTrue(audioEngine.isSpeaking.value)

        val pushyami = TeluguKarteluCatalog.getById(8)
        // Calling speakKarte while already speaking should stop speech
        vm.speakKarte(pushyami)
        testScheduler.advanceUntilIdle()

        assertEquals(1, audioEngine.stopCalls)
        assertFalse(audioEngine.isSpeaking.value)

        // Calling again when stopped should now start speaking the Karte
        vm.speakKarte(pushyami)
        testScheduler.advanceUntilIdle()

        assertTrue(audioEngine.isSpeaking.value)
        assertTrue(audioEngine.lastSpokenText!!.contains("పుష్యమి"))

        collector.cancel()
    }

    @Test
    fun `natural TTS finish allows immediate restart without desync`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.selectDay(baseEpochDay)
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.isAudioSpeaking)

        // Engine finishes speaking naturally
        audioEngine.simulateTtsFinished()
        testScheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isAudioSpeaking)

        // Next toggle immediately starts speech afresh (does NOT attempt to stop)
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isAudioSpeaking)
        assertEquals(2, audioEngine.speakDynamicCalls)

        collector.cancel()
    }

    @Test
    fun `onCleared terminates active audio playback`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.selectDay(baseEpochDay)
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()
        assertTrue(audioEngine.isSpeaking.value)

        // Trigger onCleared via reflection
        val onClearedMethod = ViewModel::class.java.getDeclaredMethod("onCleared")
        onClearedMethod.isAccessible = true
        onClearedMethod.invoke(vm)
        testScheduler.advanceUntilIdle()

        assertEquals(1, audioEngine.stopCalls)
        assertFalse(audioEngine.isSpeaking.value)

        collector.cancel()
    }

    // =========================================================================
    // 2. Boundary Year Transitions & Real Offline Engine Fallbacks
    // =========================================================================

    @Test
    fun `boundary year transition from 2025-01 backwards to 2024-12 fallback calculates valid 31 days`() = runTest(testDispatcher) {
        // Test with real repository engine
        val vm = createViewModel(useRealRepo = true)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // 2026-07 -> 2025-01 is -18 months
        vm.changeMonth(-18)
        testScheduler.advanceUntilIdle()
        assertEquals(2025, vm.uiState.value.selectedYear)
        assertEquals(1, vm.uiState.value.selectedMonth)

        // Step backwards 1 more month into 2024-12 (outside precomputed 2025-2027 tables)
        vm.changeMonth(-1)
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2024, state.selectedYear)
        assertEquals(12, state.selectedMonth)
        assertEquals(31, state.monthDays.size)

        // Verify every single day has valid, non-empty, non-null properties
        state.monthDays.forEachIndexed { index, day ->
            val dayNum = index + 1
            val expectedEpoch = LocalDate.of(2024, 12, dayNum).toEpochDay()
            assertEquals("Day $dayNum epochDay mismatch", expectedEpoch, day.epochDay)
            assertNotNull("Day $dayNum teluguYear must not be null", day.teluguYear)
            assertTrue("Day $dayNum teluguYear must not be blank", day.teluguYear.isNotBlank())
            assertNotNull("Day $dayNum teluguMonth must not be null", day.teluguMonth)
            assertNotNull("Day $dayNum paksham must not be null", day.paksham)
            assertNotNull("Day $dayNum tithi must not be null", day.tithi)
            assertNotNull("Day $dayNum nakshatram must not be null", day.nakshatram)
            assertTrue("Day $dayNum karteId must be in 1..27", day.karteId in 1..27)
        }

        collector.cancel()
    }

    @Test
    fun `boundary year transition from 2027-12 forward to 2028-01 fallback calculates valid days and leap year Feb`() = runTest(testDispatcher) {
        val vm = createViewModel(useRealRepo = true)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // 2026-07 -> 2027-12 is +17 months
        vm.changeMonth(17)
        testScheduler.advanceUntilIdle()
        assertEquals(2027, vm.uiState.value.selectedYear)
        assertEquals(12, vm.uiState.value.selectedMonth)

        // Step forward 1 month into 2028-01 (outside precomputed tables)
        vm.changeMonth(1)
        testScheduler.advanceUntilIdle()

        val stateJan = vm.uiState.value
        assertEquals(2028, stateJan.selectedYear)
        assertEquals(1, stateJan.selectedMonth)
        assertEquals(31, stateJan.monthDays.size)
        assertTrue(stateJan.monthDays.all { it.gregorianDate.isNotBlank() && it.teluguYear.isNotBlank() })

        // Step forward 1 more month into 2028-02 (LEAP YEAR!)
        vm.changeMonth(1)
        testScheduler.advanceUntilIdle()

        val stateFeb = vm.uiState.value
        assertEquals(2028, stateFeb.selectedYear)
        assertEquals(2, stateFeb.selectedMonth)
        // 2028 is a leap year -> February must have exactly 29 days!
        assertEquals(29, stateFeb.monthDays.size)
        assertEquals(LocalDate.of(2028, 2, 29).toEpochDay(), stateFeb.monthDays.last().epochDay)

        collector.cancel()
    }

    @Test
    fun `extreme year boundary jumps 10 years future and 20 years past execute without exception`() = runTest(testDispatcher) {
        val vm = createViewModel(useRealRepo = true)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Jump +120 months (10 years to 2036-07)
        vm.changeMonth(120)
        testScheduler.advanceUntilIdle()
        assertEquals(2036, vm.uiState.value.selectedYear)
        assertEquals(7, vm.uiState.value.selectedMonth)
        assertEquals(31, vm.uiState.value.monthDays.size)

        // Jump -360 months (30 years back to 2006-07)
        vm.changeMonth(-360)
        testScheduler.advanceUntilIdle()
        assertEquals(2006, vm.uiState.value.selectedYear)
        assertEquals(7, vm.uiState.value.selectedMonth)
        assertEquals(31, vm.uiState.value.monthDays.size)

        collector.cancel()
    }

    // =========================================================================
    // 3. Task Count Integrity under Edge Cases
    // =========================================================================

    @Test
    fun `archived tasks are excluded from both taskCountsByDay and tasksForSelectedDay`() = runTest(testDispatcher) {
        val taskActive = sampleCropTask(id = 1L, dueEpochDay = baseEpochDay, isArchived = false)
        val taskArchived = sampleCropTask(id = 2L, dueEpochDay = baseEpochDay, isArchived = true)

        farmRepo.tasksFlow.value = listOf(taskActive, taskArchived)

        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.taskCountsByDay[baseEpochDay])
        assertEquals(1, state.tasksForSelectedDay.size)
        assertEquals(1L, state.tasksForSelectedDay.first().id)
        assertFalse(state.tasksForSelectedDay.any { it.isArchived })

        collector.cancel()
    }

    @Test
    fun `cancelled tasks are excluded from taskCountsByDay dot count`() = runTest(testDispatcher) {
        val taskPending = sampleCropTask(id = 101L, dueEpochDay = baseEpochDay, status = TaskStatus.PENDING)
        val taskCancelled = sampleCropTask(id = 102L, dueEpochDay = baseEpochDay, status = TaskStatus.CANCELLED)
        val taskCompleted = sampleCropTask(id = 103L, dueEpochDay = baseEpochDay, status = TaskStatus.COMPLETED)
        val taskRescheduled = sampleCropTask(id = 104L, dueEpochDay = baseEpochDay, status = TaskStatus.RESCHEDULED)
        val taskMissed = sampleCropTask(id = 105L, dueEpochDay = baseEpochDay, status = TaskStatus.MISSED)

        farmRepo.tasksFlow.value = listOf(taskPending, taskCancelled, taskCompleted, taskRescheduled, taskMissed)

        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        // Active pending tasks that count towards the cell dot badge are:
        // PENDING (101), RESCHEDULED (104), MISSED (105) -> Total = 3
        // COMPLETED (103) and CANCELLED (102) are excluded from the pending count
        assertEquals(3, state.taskCountsByDay[baseEpochDay])

        // But in tasksForSelectedDay, all non-archived tasks on that day are shown (5 tasks)
        assertEquals(5, state.tasksForSelectedDay.size)

        collector.cancel()
    }

    @Test
    fun `massive task load of 1000 tasks verifies arithmetic integrity and performance`() = runTest(testDispatcher) {
        val tasks = ArrayList<CropTask>(1000)
        // 400 PENDING on baseEpochDay
        for (i in 1..400) {
            tasks.add(sampleCropTask(id = i.toLong(), dueEpochDay = baseEpochDay, status = TaskStatus.PENDING))
        }
        // 300 COMPLETED on baseEpochDay
        for (i in 401..700) {
            tasks.add(sampleCropTask(id = i.toLong(), dueEpochDay = baseEpochDay, status = TaskStatus.COMPLETED))
        }
        // 100 CANCELLED on baseEpochDay
        for (i in 701..800) {
            tasks.add(sampleCropTask(id = i.toLong(), dueEpochDay = baseEpochDay, status = TaskStatus.CANCELLED))
        }
        // 100 ARCHIVED on baseEpochDay
        for (i in 801..900) {
            tasks.add(sampleCropTask(id = i.toLong(), dueEpochDay = baseEpochDay, isArchived = true))
        }
        // 100 PENDING on tomorrow (baseEpochDay + 1)
        for (i in 901..1000) {
            tasks.add(sampleCropTask(id = i.toLong(), dueEpochDay = baseEpochDay + 1, status = TaskStatus.PENDING))
        }

        farmRepo.tasksFlow.value = tasks

        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        // baseEpochDay active count must be EXACTLY 400
        assertEquals(400, state.taskCountsByDay[baseEpochDay])
        // tomorrow active count must be EXACTLY 100
        assertEquals(100, state.taskCountsByDay[baseEpochDay + 1])

        // tasksForSelectedDay on baseEpochDay should have 800 tasks (400 pending + 300 completed + 100 cancelled, excluding 100 archived)
        assertEquals(800, state.tasksForSelectedDay.size)

        collector.cancel()
    }

    @Test
    fun `dismissDayDetails preserves selected day but hides details sheet`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        vm.selectDay(baseEpochDay + 3)
        testScheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isDayDetailsVisible)
        assertEquals(baseEpochDay + 3, vm.uiState.value.selectedDay?.epochDay)

        vm.dismissDayDetails()
        testScheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isDayDetailsVisible)
        // selectedDay remains remembered
        assertEquals(baseEpochDay + 3, vm.uiState.value.selectedDay?.epochDay)

        collector.cancel()
    }

    // =========================================================================
    // Helpers & Doubles
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
                teluguYear = "పరాభవ",
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

    private class TestAppDispatchers(private val dispatcher: CoroutineDispatcher) : AppDispatchers {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private class FakeTeluguCalendarRepo : TeluguCalendarRepository {
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
            return monthDaysMap.getOrPut(Pair(year, month)) { MutableStateFlow(emptyList()) }
        }

        override suspend fun getMonthDays(year: Int, month: Int): List<TeluguCalendarDay> {
            return monthDaysMap[Pair(year, month)]?.value ?: emptyList()
        }

        override fun observeDay(epochDay: Long): Flow<TeluguCalendarDay?> = flowOf(dayMap[epochDay])
        override suspend fun getDay(epochDay: Long): TeluguCalendarDay? = dayMap[epochDay]

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
                tasksFlow.value = tasksFlow.value.map {
                    if (it.id == taskId) it.copy(status = status) else it
                }
            }
            return updateTaskStatusResult
        }

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
