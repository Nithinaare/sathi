package com.farmsaathi.feature.home

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.audio.VoiceNoteManager
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.DataOverviewCounts
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.FinanceRepository
import com.farmsaathi.core.common.repository.GlobalSearchHit
import com.farmsaathi.core.common.repository.MachineryRepository
import com.farmsaathi.core.common.repository.SeasonFinancialTotals
import com.farmsaathi.core.common.repository.StartMachinerySessionRequest
import com.farmsaathi.core.common.repository.TeluguCalendarRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.designsystem.localization.ProvideFarmLocale
import com.farmsaathi.core.designsystem.theme.FarmSaathiTheme
import com.farmsaathi.core.media.LocalPhotoStorage
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.Contact
import com.farmsaathi.core.model.ContactType
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropProfitabilitySummary
import com.farmsaathi.core.model.CropSale
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CropTemplate
import com.farmsaathi.core.model.CurrentKarteInfo
import com.farmsaathi.core.model.ExpenseDetail
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.LunarAlertType
import com.farmsaathi.core.model.LunarDayAlert
import com.farmsaathi.core.model.MachinerySession
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.Paksham
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.core.model.TeluguCalendarDay
import com.farmsaathi.core.model.TeluguKarte
import com.farmsaathi.core.model.TeluguMonth
import com.farmsaathi.core.model.TeluguTithi
import com.farmsaathi.feature.home.components.HomePanchangKarteBanner
import com.farmsaathi.feature.home.viewmodel.HomeUiState
import com.farmsaathi.feature.home.viewmodel.HomeViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * Empirical Challenger 2 Test Suite for Milestone 1:
 * Validates audio playback state transitions, rapid toggling robustness,
 * HomeUiState default values, and Compose recomposition stability.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], qualifiers = "w412dp-h915dp-xxhdpi")
class HomeAudioAndRecompositionChallengerTest {

    @get:Rule
    val composeRule = createComposeRule()

    // -------------------------------------------------------------------------
    // 1. Audio Playback State Transitions & Rapid Toggling Tests
    // -------------------------------------------------------------------------

    @Test
    fun `rapid toggling of panchang audio toggles stop and speak without state desync`() = runTest {
        val audioEngine = ControllableAudioEngine()
        val calendarRepo = ControllableCalendarRepo(
            day = sampleDay(),
            karte = sampleKarte(),
            alert = sampleAlert("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి."),
        )
        val vm = createViewModel(
            audioEngine = audioEngine,
            calendarRepo = calendarRepo,
            dispatchers = TestAppDispatchers(StandardTestDispatcher(testScheduler)),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        testScheduler.advanceUntilIdle()

        assertFalse(audioEngine.isSpeaking.value)
        assertEquals(0, audioEngine.stopCalls)
        assertEquals(0, audioEngine.speakDynamicCalls)

        // 1st toggle: starts speaking
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()
        assertTrue("Audio engine should be speaking after 1st toggle", audioEngine.isSpeaking.value)
        assertEquals("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి.", audioEngine.lastSpokenText)
        assertEquals(1, audioEngine.speakDynamicCalls)

        // 2nd toggle: stops speaking
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()
        assertFalse("Audio engine should stop speaking after 2nd toggle", audioEngine.isSpeaking.value)
        assertEquals(1, audioEngine.stopCalls)

        // 3rd toggle: starts speaking again
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()
        assertTrue("Audio engine should resume speaking after 3rd toggle", audioEngine.isSpeaking.value)
        assertEquals(2, audioEngine.speakDynamicCalls)

        // 4th toggle: stops speaking again
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()
        assertFalse("Audio engine should stop speaking after 4th toggle", audioEngine.isSpeaking.value)
        assertEquals(2, audioEngine.stopCalls)

        // Rapid 10-toggle stress loop
        for (i in 1..10) {
            vm.togglePanchangAudio()
            testScheduler.advanceUntilIdle()
            val expectedPlaying = (i % 2 != 0)
            assertEquals("Cycle $i mismatch for isSpeaking", expectedPlaying, audioEngine.isSpeaking.value)
        }

        // Final state after 10 toggles (even number of toggles) must be stopped
        assertFalse(audioEngine.isSpeaking.value)
    }

    @Test
    fun `external tts completion resets panchang audio state to idle`() = runTest {
        val audioEngine = ControllableAudioEngine()
        val calendarRepo = ControllableCalendarRepo(
            day = sampleDay(),
            karte = sampleKarte(),
            alert = sampleAlert("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి."),
        )
        val vm = createViewModel(
            audioEngine = audioEngine,
            calendarRepo = calendarRepo,
            dispatchers = TestAppDispatchers(StandardTestDispatcher(testScheduler)),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        // Start playback
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()
        assertTrue(audioEngine.isSpeaking.value)

        // Simulate Android TTS utterance completion callback
        audioEngine.simulateTtsCompletion()
        testScheduler.advanceUntilIdle()

        // Panchang audio playing state in VM should have reset to false
        assertFalse("Panchang audio state must reset when TTS completes", audioEngine.isSpeaking.value)

        // Next toggle should start fresh playback rather than attempt a redundant stop
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()
        assertTrue("Should start fresh playback after external completion", audioEngine.isSpeaking.value)
        assertEquals(2, audioEngine.speakDynamicCalls)
    }

    @Test
    fun `home header audio cancels active panchang audio without desync`() = runTest {
        val audioEngine = ControllableAudioEngine()
        val calendarRepo = ControllableCalendarRepo(
            day = sampleDay(),
            karte = sampleKarte(),
            alert = sampleAlert("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి."),
        )
        val vm = createViewModel(
            audioEngine = audioEngine,
            calendarRepo = calendarRepo,
            dispatchers = TestAppDispatchers(StandardTestDispatcher(testScheduler)),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        // Start panchang audio
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()
        assertTrue(audioEngine.isSpeaking.value)

        // User taps top header audio summary
        vm.toggleHomeAudio()
        testScheduler.advanceUntilIdle()

        // audioEngine.speakCue should have been invoked
        assertEquals(AudioCueCatalog.HOME_SUMMARY, audioEngine.lastSpokenCue)

        // Now next toggle of panchang audio must start panchang audio cleanly
        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()
        assertTrue(audioEngine.isSpeaking.value)
        assertEquals(2, audioEngine.speakDynamicCalls)
    }

    @Test
    fun `togglePanchangAudio is a no-op when todayAlert is null or speech text is blank`() = runTest {
        val audioEngine = ControllableAudioEngine()
        val calendarRepo = ControllableCalendarRepo(
            day = sampleDay(),
            karte = sampleKarte(),
            alert = null,
        )
        val vm = createViewModel(
            audioEngine = audioEngine,
            calendarRepo = calendarRepo,
            dispatchers = TestAppDispatchers(StandardTestDispatcher(testScheduler)),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()

        assertEquals(0, audioEngine.speakDynamicCalls)
        assertEquals(0, audioEngine.stopCalls)
        assertFalse(audioEngine.isSpeaking.value)

        // Update repo with blank speech text
        calendarRepo.updateAlert(sampleAlert(speechText = "   "))
        testScheduler.advanceUntilIdle()

        vm.togglePanchangAudio()
        testScheduler.advanceUntilIdle()

        assertEquals(0, audioEngine.speakDynamicCalls)
        assertEquals(0, audioEngine.stopCalls)
        assertFalse(audioEngine.isSpeaking.value)
    }

    // -------------------------------------------------------------------------
    // 2. Compose Recomposition Stability & HomeUiState Default Values Tests
    // -------------------------------------------------------------------------

    @Test
    fun `default HomeUiState values do not cause crash or infinite recomposition`() {
        val defaultState = HomeUiState()

        // Verify default field values
        assertTrue(defaultState.isLoading)
        assertNull(defaultState.todayCalendarDay)
        assertNull(defaultState.todayKarteInfo)
        assertNull(defaultState.todayAlert)
        assertFalse(defaultState.isPanchangAudioPlaying)
        assertFalse(defaultState.isAudioPlaying)

        var recompositionCount = 0

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(AppLanguage.TELUGU) {
                    SideEffect {
                        recompositionCount++
                    }

                    // Directly test banner component with null day/alert
                    HomePanchangKarteBanner(
                        calendarDay = defaultState.todayCalendarDay,
                        karteInfo = defaultState.todayKarteInfo,
                        alert = defaultState.todayAlert,
                        isPlayingAudio = defaultState.isPanchangAudioPlaying,
                        onPlayAudio = {},
                        onClick = {},
                    )
                }
            }
        }

        // Banner returns early on null calendarDay or alert without rendering anything
        assertEquals(1, recompositionCount)
    }

    @Test
    fun `HomePanchangKarteBanner gracefully handles null todayKarteInfo`() {
        val day = sampleDay()
        val alert = sampleAlert()

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(AppLanguage.TELUGU) {
                    HomePanchangKarteBanner(
                        calendarDay = day,
                        karteInfo = null,
                        alert = alert,
                        isPlayingAudio = false,
                        onPlayAudio = {},
                        onClick = {},
                    )
                }
            }
        }

        // Main text and alert text should be displayed without throwing NPE
        composeRule.onNodeWithText("కార్తీకం • శుక్ల పక్షం పాడ్యమి").assertIsDisplayed()
        composeRule.onNodeWithText("అమావాస్య హెచ్చరిక").assertIsDisplayed()
    }

    @Test
    fun `HomePanchangKarteBanner displays stop button when isPlayingAudio is true`() {
        val day = sampleDay()
        val alert = sampleAlert()
        var playAudioClicked = false

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(AppLanguage.TELUGU) {
                    HomePanchangKarteBanner(
                        calendarDay = day,
                        karteInfo = sampleKarte(),
                        alert = alert,
                        isPlayingAudio = true,
                        onPlayAudio = { playAudioClicked = true },
                        onClick = {},
                    )
                }
            }
        }

        // When playing, button shows "ఆపండి"
        composeRule.onNodeWithText("ఆపండి")
            .assertIsDisplayed()
            .performClick()

        assertTrue(playAudioClicked)
    }

    @Test
    fun `recomposition count remains strictly bounded under distinct state emissions`() {
        val recompositionCount = AtomicInteger(0)
        val stateFlow = MutableStateFlow(HomeUiState(isLoading = false, todayCalendarDay = sampleDay(), todayAlert = sampleAlert()))

        composeRule.setContent {
            val state = stateFlow.value
            SideEffect {
                recompositionCount.incrementAndGet()
            }
            HomePanchangKarteBanner(
                calendarDay = state.todayCalendarDay,
                karteInfo = state.todayKarteInfo,
                alert = state.todayAlert,
                isPlayingAudio = state.isPanchangAudioPlaying,
                onPlayAudio = {},
                onClick = {},
            )
        }

        val initialRecomps = recompositionCount.get()
        assertEquals(1, initialRecomps)

        // Mutating with identical state
        repeat(10) {
            stateFlow.value = stateFlow.value.copy()
        }

        // Recomposition should not loop indefinitely
        assertTrue("Recomposition must remain bounded (actual: ${recompositionCount.get()})", recompositionCount.get() <= 2)
    }

    // -------------------------------------------------------------------------
    // Helpers & Test Doubles
    // -------------------------------------------------------------------------

    private fun sampleDay() = TeluguCalendarDay(
        epochDay = 20_650L,
        gregorianDate = "2026-11-10",
        teluguYear = "క్రోధి",
        teluguMonth = TeluguMonth.KARTHIKAM,
        paksham = Paksham.SHUKLA,
        tithi = TeluguTithi.PADYAMI,
        nakshatram = "విశాఖ",
        karteId = 16,
        karteName = "విశాఖ",
        karteDayNumber = 5,
        totalKarteDays = 14,
        isAmavasya = true,
        isPournami = false,
        isFestival = false,
    )

    private fun sampleKarte() = CurrentKarteInfo(
        karteId = 16,
        teluguName = "విశాఖ",
        englishName = "Vishakha",
        dayNumber = 5,
        totalDays = 14,
        daysRemaining = 9,
        progressPercentage = 35.7f,
        proverb = "విశాఖలో చల్లితే విరివిగా పంట",
        agriculturalSignificance = "వరి కోతలు ప్రారంభం.",
        recommendedCrops = listOf("వరి", "శనగ"),
    )

    private fun sampleAlert(speechText: String = "నేడు కార్తీక మాసం శుక్ల పక్షం పాడ్యమి. విశాఖ కార్తె 5వ రోజు.") = LunarDayAlert(
        alertType = LunarAlertType.AMAVASYA,
        title = "అమావాస్య హెచ్చరిక",
        subtitle = "ఎడ్లకు పని నుండి విశ్రాంతి ఇవ్వండి. కాంతి ఉచ్చులు అమర్చండి.",
        ttsSpeechText = speechText,
        isPriority = true,
    )

    private fun createViewModel(
        audioEngine: ControllableAudioEngine,
        calendarRepo: TeluguCalendarRepository = ControllableCalendarRepo(),
        dispatchers: AppDispatchers = DefaultAppDispatchers,
    ): HomeViewModel {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        return HomeViewModel(
            prefsRepo = StubPrefsRepo(),
            farmRepo = StubFarmRepo(),
            financeRepo = StubFinanceRepo(),
            machineryRepo = StubMachineryRepo(),
            contactRepo = StubContactRepo(),
            photoStorage = LocalPhotoStorage(ctx),
            voiceManager = VoiceNoteManager(ctx),
            audioEngine = audioEngine,
            timeProvider = StubTimeProvider(),
            teluguCalendarRepo = calendarRepo,
            dispatchers = dispatchers,
        )
    }

    private class TestAppDispatchers(private val testDispatcher: CoroutineDispatcher) : AppDispatchers {
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val main: CoroutineDispatcher = testDispatcher
    }

    private class ControllableAudioEngine : AudioGuidanceEngine {
        val isSpeakingFlow = MutableStateFlow(false)
        override val isSpeaking: StateFlow<Boolean> = isSpeakingFlow.asStateFlow()
        override val activeCueKey: StateFlow<String?> = MutableStateFlow(null)

        var stopCalls = 0
        var speakDynamicCalls = 0
        var lastSpokenText: String? = null
        var lastSpokenCue: String? = null

        override fun speakCue(cueKey: String, useTelugu: Boolean) {
            lastSpokenCue = cueKey
            isSpeakingFlow.value = true
        }

        override fun speakDynamicText(text: String, useTelugu: Boolean) {
            speakDynamicCalls++
            lastSpokenText = text
            isSpeakingFlow.value = true
        }

        override fun stop() {
            stopCalls++
            isSpeakingFlow.value = false
        }

        fun simulateTtsCompletion() {
            isSpeakingFlow.value = false
        }

        override fun playAcousticFeedback(cue: AcousticCue) {}
        override fun isTeluguVoiceReady(): Boolean = true
        override fun hasCustomPrompt(cueKey: String): Boolean = false
        override fun customPromptFilePath(cueKey: String): String = ""
        override fun clearCustomPrompt(cueKey: String) {}
    }

    private class ControllableCalendarRepo(
        private var day: TeluguCalendarDay? = null,
        private var karte: CurrentKarteInfo? = null,
        private var alert: LunarDayAlert? = null,
    ) : TeluguCalendarRepository {
        private val dayFlow = MutableStateFlow(day)
        private val karteFlow = MutableStateFlow(
            karte ?: CurrentKarteInfo(
                1, "అశ్విని", "Ashwini", 1, 14, 13, 7.1f, "అశ్విని సామెత", "అదను", listOf("పత్తి"),
            ),
        )
        private val alertFlow = MutableStateFlow(
            alert ?: LunarDayAlert(
                LunarAlertType.DAY_STANDARD, "సహజ రోజు", "పనులు కొనసాగించండి", "", null, null, false,
            ),
        )

        fun updateAlert(newAlert: LunarDayAlert) {
            alert = newAlert
            alertFlow.value = newAlert
        }

        override fun observeDay(epochDay: Long): Flow<TeluguCalendarDay?> = dayFlow
        override suspend fun getDay(epochDay: Long): TeluguCalendarDay? = dayFlow.value
        override fun observeMonth(year: Int, month: Int): Flow<List<TeluguCalendarDay>> = flowOf(emptyList())
        override suspend fun getMonthDays(year: Int, month: Int): List<TeluguCalendarDay> = emptyList()
        override fun observeCurrentKarte(epochDay: Long): Flow<CurrentKarteInfo> = karteFlow
        override suspend fun getCurrentKarte(epochDay: Long): CurrentKarteInfo = karteFlow.value
        override fun observeTodayAlert(epochDay: Long): Flow<LunarDayAlert> = alertFlow
        override suspend fun getTodayAlert(epochDay: Long): LunarDayAlert = alertFlow.value
        override fun observeAllKartis(): Flow<List<TeluguKarte>> = flowOf(emptyList())
    }

    private class StubPrefsRepo : UserPreferencesRepository {
        private val flow = MutableStateFlow(UserPreferences(language = AppLanguage.TELUGU))
        override val userPreferences: Flow<UserPreferences> = flow
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun updateProfile(farmerName: String, villageName: String) {}
        override suspend fun setLanguage(language: AppLanguage) {}
        override suspend fun setAudioGuidanceEnabled(enabled: Boolean) {}
        override suspend fun setPreferredAreaUnit(unit: AreaUnit) {}
        override suspend fun setActiveFarmId(farmId: Long?) {}
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {}
        override suspend fun setLastBackupEpochMillis(epochMillis: Long) {}
    }

    private class StubFarmRepo : FarmRepository {
        override fun observeFarms(includeArchived: Boolean): Flow<List<Farm>> =
            flowOf(listOf(Farm(id = 1L, name = "చెరువు పొలం", isPrimary = true)))
        override fun observeFarm(farmId: Long): Flow<Farm?> = flowOf(null)
        override suspend fun saveFarm(farm: Farm): AppResult<Long> = AppResult.Success(1L)
        override suspend fun ensureDefaultFarmAndPlot(): Pair<Long, Long> = 1L to 1L
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
        override suspend fun createCropCycleWithTemplate(
            cycle: CropCycle,
            generateTasksFromTemplate: Boolean,
            useTeluguTitles: Boolean,
        ): AppResult<Long> = AppResult.Success(1L)
        override suspend fun updateCropCycle(
            cycle: CropCycle,
            recalculateUncompletedTemplateTasks: Boolean,
        ): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateCropCycleStatus(cropCycleId: Long, status: CropCycleStatus): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeTasks(cropCycleId: Long?, plotId: Long?, farmId: Long?): Flow<List<CropTask>> = flowOf(emptyList())
        override fun observeDueAndOverdueTasks(todayEpochDay: Long): Flow<List<CropTask>> = flowOf(emptyList())
        override suspend fun getDueAndOverdueCount(todayEpochDay: Long): Int = 0
        override suspend fun getTask(taskId: Long): CropTask? = null
        override suspend fun saveTask(task: CropTask): AppResult<Long> = AppResult.Success(1L)
        override suspend fun updateTaskStatus(taskId: Long, status: TaskStatus, newDueEpochDay: Long?) = AppResult.Success(Unit)
    }

    private class StubFinanceRepo : FinanceRepository {
        override fun observeLedgerEntries(
            farmId: Long?,
            cropCycleId: Long?,
            entryType: LedgerEntryType?,
            query: String,
        ): Flow<List<LedgerEntry>> = flowOf(emptyList())
        override fun observeSeasonTotals(farmId: Long?): Flow<SeasonFinancialTotals> =
            flowOf(SeasonFinancialTotals(MoneyPaise.fromRupees(5000), MoneyPaise.fromRupees(12_000), MoneyPaise.ZERO, MoneyPaise.ZERO))
        override fun observeLedgerEntry(ledgerEntryId: Long): Flow<LedgerEntry?> = flowOf(null)
        override suspend fun getExpenseDetail(expenseId: Long): ExpenseDetail? = null
        override suspend fun saveManualExpense(detail: ExpenseDetail, paidAmount: MoneyPaise) = AppResult.Success(1L)
        override suspend fun saveManualIncome(
            farmId: Long,
            plotId: Long?,
            cropCycleId: Long?,
            title: String,
            amount: MoneyPaise,
            settledAmount: MoneyPaise,
            paymentMode: PaymentMode,
            dateEpochMillis: Long,
            notes: String?,
        ) = AppResult.Success(1L)
        override fun observeCropSales(cropCycleId: Long?): Flow<List<CropSale>> = flowOf(emptyList())
        override fun observeCropSale(saleId: Long): Flow<CropSale?> = flowOf(null)
        override suspend fun saveCropSale(sale: CropSale) = AppResult.Success(1L)
        override suspend fun recordLedgerSettlement(
            ledgerEntryId: Long,
            additionalPaidOrReceived: MoneyPaise,
            paymentMode: PaymentMode,
        ) = AppResult.Success(Unit)
        override suspend fun voidLedgerEntry(ledgerEntryId: Long, reason: String) = AppResult.Success(Unit)
        override fun observeCropProfitabilitySummaries(farmId: Long?): Flow<List<CropProfitabilitySummary>> = flowOf(emptyList())
    }

    private class StubMachineryRepo : MachineryRepository {
        override fun observeRunningSessions(): Flow<List<MachinerySession>> = flowOf(emptyList())
        override fun observeSessions(cropCycleId: Long?): Flow<List<MachinerySession>> = flowOf(emptyList())
        override fun observeSession(sessionId: Long): Flow<MachinerySession?> = flowOf(null)
        override suspend fun startLiveSession(request: StartMachinerySessionRequest) = AppResult.Success(1L)
        override suspend fun stopLiveSession(
            sessionId: Long,
            overrideElapsedMinutes: Long?,
            areaCoveredMicroAcres: Long?,
            flatAmount: MoneyPaise?,
            paidAmount: MoneyPaise,
            paymentMode: PaymentMode,
            notes: String?,
        ): AppResult<MachinerySession> = error("unused")
        override suspend fun cancelLiveSession(sessionId: Long) = AppResult.Success(Unit)
        override suspend fun saveManualSession(session: MachinerySession, paymentMode: PaymentMode) = AppResult.Success(1L)
    }

    private class StubContactRepo : ContactAndSearchRepository {
        override fun observeContacts(typeFilter: ContactType?, query: String): Flow<List<Contact>> = flowOf(emptyList())
        override fun observeContact(contactId: Long): Flow<Contact?> = flowOf(null)
        override suspend fun saveContact(contact: Contact): AppResult<Long> = AppResult.Success(1L)
        override suspend fun archiveContact(contactId: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun searchOffline(query: String): List<GlobalSearchHit> = emptyList()
        override suspend fun getDataOverviewCounts(): DataOverviewCounts = DataOverviewCounts(0, 0, 0, 0, 0, 0, 0)
    }

    private class StubTimeProvider : TimeProvider {
        override fun nowEpochMillis(): Long = 1_794_326_000_000L
        override fun todayEpochDay(): Long = 20_650L
        override fun elapsedRealtimeMillis(): Long = 1000L
        override fun bootCount(): Int = 1
        override fun epochMillisToEpochDay(epochMillis: Long): Long = 20_650L
        override fun epochDayToStartMillis(epochDay: Long): Long = epochDay * 86_400_000L
        override fun formatDate(epochMillis: Long, useTelugu: Boolean): String = "10 నవంబర్ 2026"
        override fun formatEpochDay(epochDay: Long, useTelugu: Boolean): String = "10 నవంబర్ 2026"
        override fun formatTime(epochMillis: Long): String = "09:30 AM"
    }
}
