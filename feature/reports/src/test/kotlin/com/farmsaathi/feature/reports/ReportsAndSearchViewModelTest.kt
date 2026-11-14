package com.farmsaathi.feature.reports

import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.DataOverviewCounts
import com.farmsaathi.core.common.repository.DiaryRepository
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.FinanceRepository
import com.farmsaathi.core.common.repository.GlobalSearchHit
import com.farmsaathi.core.common.repository.InventoryRepository
import com.farmsaathi.core.common.repository.LabourRepository
import com.farmsaathi.core.common.repository.SearchHitType
import com.farmsaathi.core.common.repository.SeasonFinancialTotals
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
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
import com.farmsaathi.core.model.DiaryEntry
import com.farmsaathi.core.model.ExpenseDetail
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.InputProduct
import com.farmsaathi.core.model.InputPurchase
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.LedgerSourceType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.StockMovement
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.core.model.Worker
import com.farmsaathi.core.model.WorkerAttendance
import com.farmsaathi.core.model.WorkerBalanceSummary
import com.farmsaathi.core.model.WorkerPayment
import com.farmsaathi.feature.reports.viewmodel.ReportsAndSearchViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsAndSearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers =
        object : AppDispatchers {
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
        }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ledger observer is shared once across catalog and uiState`() =
        runTest(testDispatcher) {
            val financeRepo = FakeFinanceRepo()
            financeRepo.ledgerFlow.value =
                listOf(
                    LedgerEntry(
                        id = 11L,
                        entryType = LedgerEntryType.EXPENSE,
                        sourceType = LedgerSourceType.MANUAL_EXPENSE,
                        farmId = 1L,
                        category = LedgerCategory.SEEDS,
                        title = "Paddy Seeds",
                        totalAmount = MoneyPaise.fromRupees(500),
                        settledAmount = MoneyPaise.fromRupees(500),
                        paymentStatus = PaymentStatus.PAID,
                        paymentMode = PaymentMode.CASH,
                        transactionDateEpochMillis = 1_794_326_000_000L,
                    ),
                )
            val vm = buildViewModel(financeRepo = financeRepo)
            val collector = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }
            runCurrent()

            assertEquals(1, financeRepo.observeLedgerCallCount)
            assertEquals(1, financeRepo.activeLedgerCollectors)
            assertFalse(vm.uiState.value.isLoading)
            assertEquals(listOf(LedgerCategory.SEEDS to MoneyPaise.fromRupees(500)), vm.uiState.value.categoryTotals)
            assertTrue(vm.uiState.value.searchHits.any { it.id == 11L && it.title == "Paddy Seeds" })
            collector.cancel()
        }

    @Test
    fun `updateSearch debounces 250ms and cancels stale in-flight query`() =
        runTest(testDispatcher) {
            val contactRepo = FakeContactAndSearchRepo()
            val vm = buildViewModel(contactRepo = contactRepo)
            val collector = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }
            runCurrent()

            vm.updateSearch("p")
            advanceTimeBy(100L)
            runCurrent()
            vm.updateSearch("pa")
            advanceTimeBy(100L)
            runCurrent()
            vm.updateSearch("pad")
            advanceTimeBy(200L)
            runCurrent()
            assertTrue(contactRepo.searchQueries.isEmpty())

            advanceTimeBy(50L)
            runCurrent()
            assertEquals(listOf("pad"), contactRepo.searchQueries)
            assertEquals(listOf("Hit: pad"), vm.uiState.value.searchHits.map { it.title })

            contactRepo.searchDelayMs = 500L
            vm.updateSearch("slow")
            advanceTimeBy(250L)
            runCurrent()
            assertEquals(listOf("pad", "slow"), contactRepo.searchQueries)

            contactRepo.searchDelayMs = 0L
            vm.updateSearch("fast")
            advanceTimeBy(250L)
            runCurrent()
            advanceTimeBy(500L)
            runCurrent()

            assertEquals(listOf("Hit: fast"), vm.uiState.value.searchHits.map { it.title })
            collector.cancel()
        }

    private fun buildViewModel(
        financeRepo: FakeFinanceRepo = FakeFinanceRepo(),
        contactRepo: FakeContactAndSearchRepo = FakeContactAndSearchRepo(),
    ): ReportsAndSearchViewModel =
        ReportsAndSearchViewModel(
            financeRepo = financeRepo,
            contactAndSearchRepo = contactRepo,
            farmRepo = FakeFarmRepo(),
            diaryRepo = FakeDiaryRepo(),
            labourRepo = FakeLabourRepo(),
            inventoryRepo = FakeInventoryRepo(),
            prefsRepo = FakePrefsRepo(),
            audioEngine = FakeAudioEngine(),
            dispatchers = testDispatchers,
        )

    private class FakeFinanceRepo : FinanceRepository {
        val ledgerFlow = MutableStateFlow<List<LedgerEntry>>(emptyList())
        var observeLedgerCallCount = 0
        var activeLedgerCollectors = 0

        override fun observeLedgerEntries(
            farmId: Long?,
            cropCycleId: Long?,
            entryType: LedgerEntryType?,
            query: String,
        ): Flow<List<LedgerEntry>> {
            observeLedgerCallCount++
            return flow {
                activeLedgerCollectors++
                try {
                    ledgerFlow.collect { emit(it) }
                } finally {
                    activeLedgerCollectors--
                }
            }
        }

        override fun observeSeasonTotals(farmId: Long?): Flow<SeasonFinancialTotals> =
            flowOf(SeasonFinancialTotals(MoneyPaise.ZERO, MoneyPaise.ZERO, MoneyPaise.ZERO, MoneyPaise.ZERO))

        override fun observeLedgerEntry(ledgerEntryId: Long): Flow<LedgerEntry?> = flowOf(null)

        override suspend fun getExpenseDetail(expenseId: Long): ExpenseDetail? = null

        override suspend fun saveManualExpense(
            detail: ExpenseDetail,
            paidAmount: MoneyPaise,
        ): AppResult<Long> = AppResult.Success(1L)

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
        ): AppResult<Long> = AppResult.Success(1L)

        override fun observeCropSales(cropCycleId: Long?): Flow<List<CropSale>> = flowOf(emptyList())

        override fun observeCropSale(saleId: Long): Flow<CropSale?> = flowOf(null)

        override suspend fun saveCropSale(sale: CropSale): AppResult<Long> = AppResult.Success(1L)

        override suspend fun recordLedgerSettlement(
            ledgerEntryId: Long,
            additionalPaidOrReceived: MoneyPaise,
            paymentMode: PaymentMode,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun voidLedgerEntry(
            ledgerEntryId: Long,
            reason: String,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeCropProfitabilitySummaries(farmId: Long?): Flow<List<CropProfitabilitySummary>> = flowOf(emptyList())
    }

    private class FakeContactAndSearchRepo : ContactAndSearchRepository {
        val searchQueries = mutableListOf<String>()
        var searchDelayMs = 0L

        override fun observeContacts(
            typeFilter: ContactType?,
            query: String,
        ): Flow<List<Contact>> = flowOf(emptyList())

        override fun observeContact(contactId: Long): Flow<Contact?> = flowOf(null)

        override suspend fun saveContact(contact: Contact): AppResult<Long> = AppResult.Success(1L)

        override suspend fun archiveContact(contactId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun searchOffline(query: String): List<GlobalSearchHit> {
            searchQueries += query
            if (searchDelayMs > 0L) {
                delay(searchDelayMs)
            }
            return listOf(
                GlobalSearchHit(
                    id = 1L,
                    type = SearchHitType.CROP,
                    title = "Hit: $query",
                    subtitle = "Kharif",
                    timestampMillis = 1_794_326_000_000L,
                ),
            )
        }

        override suspend fun getDataOverviewCounts(): DataOverviewCounts = DataOverviewCounts(0, 0, 0, 0, 0, 0, 0)
    }

    private class FakeFarmRepo : FarmRepository {
        override fun observeFarms(includeArchived: Boolean): Flow<List<Farm>> = flowOf(emptyList())

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
        ): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeDiaryRepo : DiaryRepository {
        override fun observeDiaryEntries(
            farmId: Long?,
            plotId: Long?,
            cropCycleId: Long?,
            query: String,
        ): Flow<List<DiaryEntry>> = flowOf(emptyList())

        override suspend fun getDiaryEntry(entryId: Long): DiaryEntry? = null

        override suspend fun saveDiaryEntry(entry: DiaryEntry): AppResult<Long> = AppResult.Success(1L)

        override suspend fun archiveDiaryEntry(entryId: Long): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeLabourRepo : LabourRepository {
        override fun observeWorkersWithBalance(): Flow<List<WorkerBalanceSummary>> = flowOf(emptyList())

        override fun observeWorker(workerId: Long): Flow<Worker?> = flowOf(null)

        override fun observeWorkerAttendance(workerId: Long): Flow<List<WorkerAttendance>> = flowOf(emptyList())

        override fun observeAttendanceForDay(workDateEpochDay: Long): Flow<List<WorkerAttendance>> = flowOf(emptyList())

        override fun observeWorkerPayments(workerId: Long): Flow<List<WorkerPayment>> = flowOf(emptyList())

        override suspend fun saveWorker(worker: Worker): AppResult<Long> = AppResult.Success(1L)

        override suspend fun archiveWorker(workerId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun adjustWorkerDaysTenths(
            workerId: Long,
            deltaTenths: Int,
        ): AppResult<Int> = AppResult.Success(0)

        override suspend fun setWorkerDaysTenths(
            workerId: Long,
            daysTenths: Int,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun upsertAttendance(attendance: WorkerAttendance): AppResult<Long> = AppResult.Success(1L)

        override suspend fun recordWorkerPayment(payment: WorkerPayment): AppResult<Long> = AppResult.Success(1L)

        override suspend fun voidWorkerPayment(
            paymentId: Long,
            reason: String,
        ): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeInventoryRepo : InventoryRepository {
        override fun observeProducts(includeArchived: Boolean): Flow<List<InputProduct>> = flowOf(emptyList())

        override fun observeProduct(productId: Long): Flow<InputProduct?> = flowOf(null)

        override fun observePurchases(productId: Long?): Flow<List<InputPurchase>> = flowOf(emptyList())

        override fun observeMovements(productId: Long): Flow<List<StockMovement>> = flowOf(emptyList())

        override fun observeAllMovements(): Flow<List<StockMovement>> = flowOf(emptyList())

        override suspend fun saveProduct(product: InputProduct): AppResult<Long> = AppResult.Success(1L)

        override suspend fun archiveProduct(productId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun recordPurchase(purchase: InputPurchase): AppResult<Long> = AppResult.Success(1L)

        override suspend fun recordStockAdjustmentOrUsage(
            productId: Long,
            farmId: Long,
            plotId: Long?,
            cropCycleId: Long?,
            signedDeltaMilliUnits: Long,
            reason: StockMovementReason,
            notes: String?,
        ): AppResult<Long> = AppResult.Success(1L)
    }

    private class FakePrefsRepo : UserPreferencesRepository {
        override val userPreferences: Flow<UserPreferences> = flowOf(UserPreferences())

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

    private class FakeAudioEngine : AudioGuidanceEngine {
        override val activeCueKey: StateFlow<String?> = MutableStateFlow(null)
        override val isSpeaking: StateFlow<Boolean> = MutableStateFlow(false)

        override fun speakCue(
            cueKey: String,
            useTelugu: Boolean,
        ) {}

        override fun speakDynamicText(
            text: String,
            useTelugu: Boolean,
        ) {}

        override fun playAcousticFeedback(cue: AcousticCue) {}

        override fun stop() {}

        override fun isTeluguVoiceReady(): Boolean = true

        override fun hasCustomPrompt(cueKey: String): Boolean = false

        override fun customPromptFilePath(cueKey: String): String = ""

        override fun clearCustomPrompt(cueKey: String) {}
    }
}
