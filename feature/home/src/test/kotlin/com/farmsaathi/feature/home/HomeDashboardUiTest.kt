package com.farmsaathi.feature.home

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.audio.VoiceNoteManager
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AudioGuidanceEngine
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
import com.farmsaathi.core.model.TeluguKarteluCatalog
import com.farmsaathi.core.model.TeluguMonth
import com.farmsaathi.core.model.TeluguTithi
import com.farmsaathi.feature.home.screens.HomeDashboardScreen
import com.farmsaathi.feature.home.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], qualifiers = "w412dp-h915dp-xxhdpi")
class HomeDashboardUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `home dashboard displays telugu greeting quick tiles and handles expense click`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val vm =
            HomeViewModel(
                prefsRepo = StubPrefsRepo(),
                farmRepo = StubFarmRepo(),
                financeRepo = StubFinanceRepo(),
                machineryRepo = StubMachineryRepo(),
                contactRepo = StubContactRepo(),
                photoStorage = LocalPhotoStorage(ctx),
                voiceManager = VoiceNoteManager(ctx),
                audioEngine = StubAudioEngine(),
                timeProvider = StubTimeProvider(),
                teluguCalendarRepo = StubTeluguCalendarRepo(),
            )
        var expenseTapped = false

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(AppLanguage.TELUGU) {
                    HomeDashboardScreen(
                        viewModel = vm,
                        onAddExpense = { expenseTapped = true },
                        onAddSale = {},
                        onOpenActivities = {},
                        onOpenAttendance = {},
                        onOpenMachinery = {},
                        onOpenAccounts = {},
                        onOpenTask = {},
                        onOpenMachinerySession = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("నమస్కారం, రాములు గారు").assertIsDisplayed()
        composeRule.onNodeWithText("₹7,000").assertIsDisplayed()
        composeRule.onNodeWithText("ఫోటో తీసి రాయండి").assertIsDisplayed()
        val expenseTileMatcher =
            hasText("ఖర్చు") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(expenseTileMatcher)
        composeRule.onNode(expenseTileMatcher)
            .assertIsDisplayed()
            .performClick()
        assertTrue(expenseTapped)
    }

    @Test
    fun `home dashboard renders english copy when locale switched`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val vm =
            HomeViewModel(
                prefsRepo = StubPrefsRepo(AppLanguage.ENGLISH),
                farmRepo = StubFarmRepo(),
                financeRepo = StubFinanceRepo(),
                machineryRepo = StubMachineryRepo(),
                contactRepo = StubContactRepo(),
                photoStorage = LocalPhotoStorage(ctx),
                voiceManager = VoiceNoteManager(ctx),
                audioEngine = StubAudioEngine(),
                timeProvider = StubTimeProvider(),
                teluguCalendarRepo = StubTeluguCalendarRepo(),
            )

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(AppLanguage.ENGLISH) {
                    HomeDashboardScreen(
                        viewModel = vm,
                        onAddExpense = {},
                        onAddSale = {},
                        onOpenActivities = {},
                        onOpenAttendance = {},
                        onOpenMachinery = {},
                        onOpenAccounts = {},
                        onOpenTask = {},
                        onOpenMachinerySession = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Namaskaram, రాములు").assertIsDisplayed()
        composeRule.onNodeWithText("Snap Photo to Log").assertIsDisplayed()
        val expenseTileMatcher =
            hasText("Expense") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(expenseTileMatcher)
        composeRule.onNode(expenseTileMatcher).assertIsDisplayed()
    }

    @Test
    fun `home dashboard shows backup safety banner when records exceed 10 and no backup exists`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val vm =
            HomeViewModel(
                prefsRepo = StubPrefsRepo(),
                farmRepo = StubFarmRepo(),
                financeRepo = StubFinanceRepo(),
                machineryRepo = StubMachineryRepo(),
                contactRepo = StubContactRepo(counts = DataOverviewCounts(1, 2, 1, 3, 5, 2, 1)),
                photoStorage = LocalPhotoStorage(ctx),
                voiceManager = VoiceNoteManager(ctx),
                audioEngine = StubAudioEngine(),
                timeProvider = StubTimeProvider(),
                teluguCalendarRepo = StubTeluguCalendarRepo(),
            )
        var backupTapped = false

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(AppLanguage.TELUGU) {
                    HomeDashboardScreen(
                        viewModel = vm,
                        onAddExpense = {},
                        onAddSale = {},
                        onOpenActivities = {},
                        onOpenAttendance = {},
                        onOpenMachinery = {},
                        onOpenAccounts = {},
                        onOpenTask = {},
                        onOpenMachinerySession = {},
                        onOpenBackup = { backupTapped = true },
                    )
                }
            }
        }

        val bannerMatcher = hasText("డేటా భద్రత: బ్యాకప్ తీసుకోండి")
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(bannerMatcher)
        composeRule.onNode(bannerMatcher)
            .assertIsDisplayed()
            .performClick()
        assertTrue(backupTapped)
    }

    @Test
    fun `home dashboard shows contact role subtitle and speaks name plus role`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val audioEngine = StubAudioEngine()
        val tractorContact =
            Contact(
                id = 7L,
                name = "రమేష్",
                phone = "9876543210",
                contactType = ContactType.TRACTOR_OWNER,
            )
        val vm =
            HomeViewModel(
                prefsRepo = StubPrefsRepo(),
                farmRepo = StubFarmRepo(),
                financeRepo = StubFinanceRepo(),
                machineryRepo = StubMachineryRepo(),
                contactRepo = StubContactRepo(contacts = listOf(tractorContact)),
                photoStorage = LocalPhotoStorage(ctx),
                voiceManager = VoiceNoteManager(ctx),
                audioEngine = audioEngine,
                timeProvider = StubTimeProvider(),
                teluguCalendarRepo = StubTeluguCalendarRepo(),
            )

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(AppLanguage.TELUGU) {
                    HomeDashboardScreen(
                        viewModel = vm,
                        onAddExpense = {},
                        onAddSale = {},
                        onOpenActivities = {},
                        onOpenAttendance = {},
                        onOpenMachinery = {},
                        onOpenAccounts = {},
                        onOpenTask = {},
                        onOpenMachinerySession = {},
                    )
                }
            }
        }

        val roleMatcher = hasText("ట్రాక్టర్ యజమాని")
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(roleMatcher)
        composeRule.onNode(roleMatcher).assertIsDisplayed()

        vm.playContactBadge(tractorContact)
        assertEquals("రమేష్, ట్రాక్టర్ యజమాని", audioEngine.lastDynamicText)
    }

    @Test
    fun `home dashboard displays panchang karte banner with telugu details and handles click`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val vm =
            HomeViewModel(
                prefsRepo = StubPrefsRepo(),
                farmRepo = StubFarmRepo(),
                financeRepo = StubFinanceRepo(),
                machineryRepo = StubMachineryRepo(),
                contactRepo = StubContactRepo(),
                photoStorage = LocalPhotoStorage(ctx),
                voiceManager = VoiceNoteManager(ctx),
                audioEngine = StubAudioEngine(),
                timeProvider = StubTimeProvider(),
                teluguCalendarRepo = StubTeluguCalendarRepo(),
            )
        var calendarTapped = false

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(AppLanguage.TELUGU) {
                    HomeDashboardScreen(
                        viewModel = vm,
                        onAddExpense = {},
                        onAddSale = {},
                        onOpenActivities = {},
                        onOpenAttendance = {},
                        onOpenMachinery = {},
                        onOpenAccounts = {},
                        onOpenTask = {},
                        onOpenMachinerySession = {},
                        onOpenTeluguCalendar = { calendarTapped = true },
                    )
                }
            }
        }

        composeRule.onNodeWithText("కార్తీకం • శుక్ల పక్షం పాడ్యమి").assertIsDisplayed()
        composeRule.onNodeWithText("విశాఖ కార్తె").assertIsDisplayed()
        composeRule.onNodeWithText("విశాఖ కార్తె 5వ రోజు").assertIsDisplayed()
        composeRule.onNodeWithText("పంచాంగం & పనులు చూడండి")
            .assertIsDisplayed()
            .performClick()
        assertTrue(calendarTapped)
    }

    @Test
    fun `home dashboard panchang banner audio button triggers tts speech`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val audioEngine = StubAudioEngine()
        val vm =
            HomeViewModel(
                prefsRepo = StubPrefsRepo(),
                farmRepo = StubFarmRepo(),
                financeRepo = StubFinanceRepo(),
                machineryRepo = StubMachineryRepo(),
                contactRepo = StubContactRepo(),
                photoStorage = LocalPhotoStorage(ctx),
                voiceManager = VoiceNoteManager(ctx),
                audioEngine = audioEngine,
                timeProvider = StubTimeProvider(),
                teluguCalendarRepo = StubTeluguCalendarRepo(),
            )

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(AppLanguage.TELUGU) {
                    HomeDashboardScreen(
                        viewModel = vm,
                        onAddExpense = {},
                        onAddSale = {},
                        onOpenActivities = {},
                        onOpenAttendance = {},
                        onOpenMachinery = {},
                        onOpenAccounts = {},
                        onOpenTask = {},
                        onOpenMachinerySession = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("విశాఖ కార్తె 5వ రోజు").assertIsDisplayed()

        // Disambiguate from header audio button (which has stateDescription "Stopped")
        val bannerAudioMatcher =
            hasText("వినండి") and SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription)
        composeRule.onNode(bannerAudioMatcher)
            .assertIsDisplayed()
            .performClick()

        assertEquals(
            "నేడు కార్తీక మాసం శుక్ల పక్షం పాడ్యమి. విశాఖ కార్తె 5వ రోజు.",
            audioEngine.lastDynamicText,
        )
    }

    @Test
    fun `home dashboard gracefully handles null calendar day without crashing`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val audioEngine = StubAudioEngine()
        val vm =
            HomeViewModel(
                prefsRepo = StubPrefsRepo(),
                farmRepo = StubFarmRepo(),
                financeRepo = StubFinanceRepo(),
                machineryRepo = StubMachineryRepo(),
                contactRepo = StubContactRepo(),
                photoStorage = LocalPhotoStorage(ctx),
                voiceManager = VoiceNoteManager(ctx),
                audioEngine = audioEngine,
                timeProvider = StubTimeProvider(),
                teluguCalendarRepo = StubTeluguCalendarRepo(day = null),
            )

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(AppLanguage.TELUGU) {
                    HomeDashboardScreen(
                        viewModel = vm,
                        onAddExpense = {},
                        onAddSale = {},
                        onOpenActivities = {},
                        onOpenAttendance = {},
                        onOpenMachinery = {},
                        onOpenAccounts = {},
                        onOpenTask = {},
                        onOpenMachinerySession = {},
                        onOpenTeluguCalendar = {},
                    )
                }
            }
        }

        // Verify that dashboard renders summary card even if calendar day is null
        composeRule.onNodeWithText("నమస్కారం, రాములు గారు").assertIsDisplayed()
        org.junit.Assert.assertNull(vm.uiState.value.todayCalendarDay)

        // Audio toggle should safely no-op if day is null / alert tts is blank
        vm.togglePanchangAudio()
        org.junit.Assert.assertFalse(vm.uiState.value.isPanchangAudioPlaying)
    }

    private class StubPrefsRepo(
        initialLang: AppLanguage = AppLanguage.TELUGU,
    ) : UserPreferencesRepository {
        private val flow =
            MutableStateFlow(
                UserPreferences(onboardingCompleted = true, farmerName = "రాములు", language = initialLang),
            )
        override val userPreferences: Flow<UserPreferences> = flow

        override suspend fun setOnboardingCompleted(completed: Boolean) {}

        override suspend fun updateProfile(
            farmerName: String,
            villageName: String,
        ) {}

        override suspend fun setLanguage(language: AppLanguage) {}

        override suspend fun setAudioGuidanceEnabled(enabled: Boolean) {}

        override suspend fun setPreferredAreaUnit(unit: AreaUnit) {}

        override suspend fun setActiveFarmId(farmId: Long?) {}

        override suspend fun setDailyReminder(
            enabled: Boolean,
            hour: Int,
            minute: Int,
        ) {}

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

        override fun observePlots(
            farmId: Long?,
            includeArchived: Boolean,
        ): Flow<List<Plot>> = flowOf(emptyList())

        override fun observePlot(plotId: Long): Flow<Plot?> = flowOf(null)

        override suspend fun savePlot(plot: Plot): AppResult<Long> = AppResult.Success(1L)

        override suspend fun archivePlot(plotId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeCropTemplates(): Flow<List<CropTemplate>> = flowOf(emptyList())

        override suspend fun getCropTemplate(templateId: Long): CropTemplate? = null

        override suspend fun saveCropTemplate(template: CropTemplate): AppResult<Long> = AppResult.Success(1L)

        override suspend fun ensureStarterTemplatesSeeded() {}

        override fun observeCropCycles(
            plotId: Long?,
            farmId: Long?,
        ): Flow<List<CropCycle>> = flowOf(emptyList())

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

        override suspend fun updateCropCycleStatus(
            cropCycleId: Long,
            status: CropCycleStatus,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeTasks(
            cropCycleId: Long?,
            plotId: Long?,
            farmId: Long?,
        ): Flow<List<CropTask>> = flowOf(emptyList())

        override fun observeDueAndOverdueTasks(todayEpochDay: Long): Flow<List<CropTask>> = flowOf(emptyList())

        override suspend fun getDueAndOverdueCount(todayEpochDay: Long): Int = 0

        override suspend fun getTask(taskId: Long): CropTask? = null

        override suspend fun saveTask(task: CropTask): AppResult<Long> = AppResult.Success(1L)

        override suspend fun updateTaskStatus(
            taskId: Long,
            status: TaskStatus,
            newDueEpochDay: Long?,
        ) = AppResult.Success(Unit)
    }

    private class StubFinanceRepo : FinanceRepository {
        override fun observeLedgerEntries(
            farmId: Long?,
            cropCycleId: Long?,
            entryType: LedgerEntryType?,
            query: String,
        ): Flow<List<LedgerEntry>> = flowOf(emptyList())

        override fun observeSeasonTotals(farmId: Long?): Flow<SeasonFinancialTotals> =
            flowOf(
                SeasonFinancialTotals(
                    totalExpense = MoneyPaise.fromRupees(5000),
                    totalIncome = MoneyPaise.fromRupees(12_000),
                    pendingPayable = MoneyPaise.ZERO,
                    pendingReceivable = MoneyPaise.ZERO,
                ),
            )

        override fun observeLedgerEntry(ledgerEntryId: Long): Flow<LedgerEntry?> = flowOf(null)

        override suspend fun getExpenseDetail(expenseId: Long): ExpenseDetail? = null

        override suspend fun saveManualExpense(
            detail: ExpenseDetail,
            paidAmount: MoneyPaise,
        ) = AppResult.Success(1L)

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

        override suspend fun voidLedgerEntry(
            ledgerEntryId: Long,
            reason: String,
        ) = AppResult.Success(Unit)

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

        override suspend fun saveManualSession(
            session: MachinerySession,
            paymentMode: PaymentMode,
        ) = AppResult.Success(1L)
    }

    private class StubAudioEngine : AudioGuidanceEngine {
        var lastDynamicText: String? = null
        override val activeCueKey: StateFlow<String?> = MutableStateFlow(null)
        override val isSpeaking: StateFlow<Boolean> = MutableStateFlow(false)

        override fun speakCue(
            cueKey: String,
            useTelugu: Boolean,
        ) {}

        override fun speakDynamicText(
            text: String,
            useTelugu: Boolean,
        ) {
            lastDynamicText = text
        }

        override fun playAcousticFeedback(cue: AcousticCue) {}

        override fun stop() {}

        override fun isTeluguVoiceReady(): Boolean = true

        override fun hasCustomPrompt(cueKey: String): Boolean = false

        override fun customPromptFilePath(cueKey: String): String = ""

        override fun clearCustomPrompt(cueKey: String) {}
    }

    private class StubTimeProvider : TimeProvider {
        override fun nowEpochMillis(): Long = 1_794_326_000_000L

        override fun todayEpochDay(): Long = 20_650L

        override fun elapsedRealtimeMillis(): Long = 1000L

        override fun bootCount(): Int = 1

        override fun epochMillisToEpochDay(epochMillis: Long): Long = 20_650L

        override fun epochDayToStartMillis(epochDay: Long): Long = epochDay * 86_400_000L

        override fun formatDate(
            epochMillis: Long,
            useTelugu: Boolean,
        ): String = "10 నవంబర్ 2026"

        override fun formatEpochDay(
            epochDay: Long,
            useTelugu: Boolean,
        ): String = "10 నవంబర్ 2026"

        override fun formatTime(epochMillis: Long): String = "09:30 AM"
    }

    private class StubContactRepo(
        private val contacts: List<Contact> = emptyList(),
        private val counts: DataOverviewCounts = DataOverviewCounts(0, 0, 0, 0, 0, 0, 0),
    ) : ContactAndSearchRepository {
        override fun observeContacts(
            typeFilter: ContactType?,
            query: String,
        ): Flow<List<Contact>> = flowOf(contacts)

        override fun observeContact(contactId: Long): Flow<Contact?> = flowOf(contacts.firstOrNull { it.id == contactId })

        override suspend fun saveContact(contact: Contact): AppResult<Long> = AppResult.Success(1L)

        override suspend fun archiveContact(contactId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun searchOffline(query: String): List<GlobalSearchHit> = emptyList()

        override suspend fun getDataOverviewCounts(): DataOverviewCounts = counts
    }

    private class StubTeluguCalendarRepo(
        private val day: TeluguCalendarDay? = createSampleDay(),
        private val karte: CurrentKarteInfo = createSampleKarte(),
        private val alert: LunarDayAlert = createSampleAlert(),
    ) : TeluguCalendarRepository {
        override fun observeDay(epochDay: Long): Flow<TeluguCalendarDay?> = flowOf(day)

        override suspend fun getDay(epochDay: Long): TeluguCalendarDay? = day

        override fun observeMonth(
            year: Int,
            month: Int,
        ): Flow<List<TeluguCalendarDay>> = flowOf(day?.let { listOf(it) } ?: emptyList())

        override suspend fun getMonthDays(
            year: Int,
            month: Int,
        ): List<TeluguCalendarDay> = day?.let { listOf(it) } ?: emptyList()

        override fun observeCurrentKarte(epochDay: Long): Flow<CurrentKarteInfo> = flowOf(karte)

        override suspend fun getCurrentKarte(epochDay: Long): CurrentKarteInfo = karte

        override fun observeTodayAlert(epochDay: Long): Flow<LunarDayAlert> = flowOf(alert)

        override suspend fun getTodayAlert(epochDay: Long): LunarDayAlert = alert

        override fun observeAllKartis(): Flow<List<TeluguKarte>> = flowOf(TeluguKarteluCatalog.ALL)

        companion object {
            fun createSampleDay() =
                TeluguCalendarDay(
                    epochDay = 20_650L,
                    gregorianDate = "2026-11-10",
                    teluguYear = "పరాభవ",
                    teluguMonth = TeluguMonth.KARTHIKAM,
                    paksham = Paksham.SHUKLA,
                    tithi = TeluguTithi.PADYAMI,
                    nakshatram = "అనూరాధ",
                    karteId = 16,
                    karteName = "విశాఖ",
                    karteDayNumber = 5,
                    totalKarteDays = 14,
                    isAmavasya = false,
                    isPournami = false,
                    isFestival = false,
                )

            fun createSampleKarte() =
                CurrentKarteInfo(
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

            fun createSampleAlert() =
                LunarDayAlert(
                    alertType = LunarAlertType.DAY_STANDARD,
                    title = "విశాఖ కార్తె 5వ రోజు",
                    subtitle = "వరి కోతలు ముమ్మరంగా సాగుతాయి. గింజ ఆరబెట్టండి.",
                    ttsSpeechText = "నేడు కార్తీక మాసం శుక్ల పక్షం పాడ్యమి. విశాఖ కార్తె 5వ రోజు.",
                    isPriority = false,
                )
        }
    }
}
