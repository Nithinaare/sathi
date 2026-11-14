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
        farmRepo.updateTaskStatusResult = AppResult.Error(DomainError.ValidationFailed("task", "failed"))

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

    @Test
    fun `buildDayAudioText synthesizes festival name and cattle rest advice on Polala Amavasya`() {
        val polalaDay = TeluguCalendarDay(
            epochDay = 20323L,
            gregorianDate = "2025-08-23",
            teluguYear = "విశ్వావసు",
            teluguMonth = TeluguMonth.SHRAVANAM,
            paksham = Paksham.KRISHNA,
            tithi = TeluguTithi.AMAVASYA,
            nakshatram = "మఖ",
            karteId = 10,
            karteName = "మఖ",
            karteDayNumber = 7,
            totalKarteDays = 14,
            isAmavasya = true,
            isPournami = false,
            isFestival = true,
            festivalName = "పోలాల అమావాస్య",
        )
        val speech = TeluguCalendarViewModel.buildDayAudioText(polalaDay, tasksCount = 1)
        assertTrue("Speech must include festival name", speech.contains("పోలాల అమావాస్య"))
        assertTrue("Speech must include cattle rest advice", speech.contains("ఎడ్లకు విశ్రాంతి"))
        assertTrue("Speech must include light trap advice", speech.contains("దీపపు బుట్టలు"))
        assertTrue("Speech must include task count", speech.contains("1 వ్యవసాయ పనులు ఉన్నాయి"))
    }

    @Test
    fun `buildDayAudioText synthesizes cattle rest and light trap advice on standard Amavasya`() {
        val amavasyaDay = TeluguCalendarDay(
            epochDay = 20205L,
            gregorianDate = "2025-04-27",
            teluguYear = "విశ్వావసు",
            teluguMonth = TeluguMonth.CHAITRAM,
            paksham = Paksham.KRISHNA,
            tithi = TeluguTithi.AMAVASYA,
            nakshatram = "అశ్విని",
            karteId = 2,
            karteName = "భరణి",
            karteDayNumber = 1,
            totalKarteDays = 14,
            isAmavasya = true,
            isPournami = false,
            isFestival = false,
        )
        val speech = TeluguCalendarViewModel.buildDayAudioText(amavasyaDay, tasksCount = 0)
        assertTrue("Speech must include amavasya", speech.contains("అమావాస్య"))
        assertTrue("Speech must include cattle rest advice", speech.contains("ఎడ్లకు విశ్రాంతి"))
        assertTrue("Speech must include light trap advice", speech.contains("దీపపు బుట్టలు"))
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
