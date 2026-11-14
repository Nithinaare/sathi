package com.farmsaathi.feature.finance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.audio.VoiceNoteManager
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.DataOverviewCounts
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.FieldOpsRepository
import com.farmsaathi.core.common.repository.FinanceRepository
import com.farmsaathi.core.common.repository.GlobalSearchHit
import com.farmsaathi.core.common.repository.SeasonFinancialTotals
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
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
import com.farmsaathi.core.model.DomainError
import com.farmsaathi.core.model.ExpenseDetail
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.HarvestRecord
import com.farmsaathi.core.model.IrrigationLog
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.SeasonType
import com.farmsaathi.core.model.SoilTestRecord
import com.farmsaathi.core.model.StorageRecord
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.feature.finance.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class FinanceViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testDispatchers =
        object : AppDispatchers {
            override val io = testDispatcher
            override val default = testDispatcher
            override val main = testDispatcher
        }
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveCropSale computes net amount with deductions and clamps received amount`() =
        runTest {
            val financeRepo = FakeFinanceRepo()
            val audio = FakeAudioEngine()
            val vm = buildViewModel(financeRepo, audio)
            val cycle =
                CropCycle(
                    id = 5L,
                    farmId = 1L,
                    plotId = 2L,
                    cropName = "Paddy",
                    season = SeasonType.KHARIF,
                    cropYear = 2026,
                    plantedAreaMicroAcres = 1_000_000L,
                    sowingDateEpochDay = 20_500L,
                )

            var done = false
            vm.saveCropSale(
                cropCycle = cycle,
                storageId = null,
                buyerName = "Sri Sai Traders",
                mandiLocation = "Miryalaguda",
                // 10 quintals
                qtyMilliUnits = 10_000L,
                unit = QuantityUnit.QUINTAL,
                ratePerUnit = MoneyPaise.fromRupees(2_300),
                deductions = MoneyPaise.fromRupees(500),
                amountReceived = MoneyPaise.fromRupees(15_000),
                paymentMode = PaymentMode.UPI,
                dateMillis = 1_794_326_000_000L,
                notes = "",
                onSaved = { done = true },
            )

            assertTrue(done)
            val saved = financeRepo.lastSavedSale!!
            assertEquals(MoneyPaise.fromRupees(23_000), saved.grossAmount)
            assertEquals(MoneyPaise.fromRupees(22_500), saved.netTotalAmount)
            assertEquals(MoneyPaise.fromRupees(15_000), saved.amountReceived)
            assertEquals(PaymentStatus.PARTIAL, saved.paymentStatus)
            assertEquals(AcousticCue.SAVED, audio.cues.last())
        }

    @Test
    fun `saveExpense and voidEntry forward validated data to repository`() =
        runTest {
            val financeRepo = FakeFinanceRepo()
            val audio = FakeAudioEngine()
            val vm = buildViewModel(financeRepo, audio)

            var saved = false
            vm.saveExpense(
                farmId = 1L,
                plotId = 2L,
                cropCycleId = 5L,
                category = LedgerCategory.FERTILIZERS,
                description = "DAP 4 Bags",
                vendor = "Kisan Kendra",
                totalAmount = MoneyPaise.fromRupees(5_400),
                paidAmount = MoneyPaise.fromRupees(5_400),
                paymentMode = PaymentMode.CASH,
                dateMillis = 1_794_326_000_000L,
                photoPath = null,
                notes = "",
                onSaved = { saved = true },
            )

            assertTrue(saved)
            assertEquals("DAP 4 Bags", financeRepo.lastSavedExpense?.description)
            assertEquals(MoneyPaise.fromRupees(5_400), financeRepo.lastPaidForExpense)
            assertTrue(audio.spokenTexts.last().contains("5400"))

            var voided = false
            vm.voidEntry(99L, "", onDone = { voided = true })
            assertTrue(voided)
            assertEquals("Cancelled by user", financeRepo.lastVoidReason)
        }

    @Test
    fun `toggleAudio delegates form and detail cue keys with active language`() =
        runTest(testDispatcher) {
            val financeRepo = FakeFinanceRepo()
            val audio = FakeAudioEngine()
            val prefsRepo = FakePrefsRepo(UserPreferences(language = AppLanguage.ENGLISH))
            val vm = buildViewModel(financeRepo, audio, prefsRepo)
            val collectJob = backgroundScope.launch { vm.uiState.collect {} }

            vm.toggleAudio(AudioCueCatalog.EXPENSE_FORM_HELP)
            vm.toggleAudio(AudioCueCatalog.SALE_FORM_HELP)
            vm.toggleAudio(AudioCueCatalog.LEDGER_DETAIL_HELP)
            vm.toggleAudio(FinanceViewModel.ACCOUNTS_CUE)

            assertEquals(
                listOf(
                    AudioCueCatalog.EXPENSE_FORM_HELP to false,
                    AudioCueCatalog.SALE_FORM_HELP to false,
                    AudioCueCatalog.LEDGER_DETAIL_HELP to false,
                    AudioCueCatalog.ACCOUNTS_SUMMARY to false,
                ),
                audio.spokenCueKeys,
            )
            collectJob.cancel()
        }

    @Test
    fun `recordSettlement forwards payment mode and handles repository errors`() =
        runTest(testDispatcher) {
            val financeRepo = FakeFinanceRepo()
            val audio = FakeAudioEngine()
            val vm = buildViewModel(financeRepo, audio)
            val collectJob = backgroundScope.launch { vm.uiState.collect {} }

            var settled = false
            vm.recordSettlement(
                ledgerEntryId = 42L,
                additional = MoneyPaise.fromRupees(1_200),
                mode = PaymentMode.UPI,
                onDone = { settled = true },
            )

            assertTrue(settled)
            assertEquals(
                Triple(42L, MoneyPaise.fromRupees(1_200), PaymentMode.UPI),
                financeRepo.lastSettlement,
            )
            assertEquals(AcousticCue.SAVED, audio.cues.last())

            financeRepo.nextSettlementResult = AppResult.Error(DomainError.PaymentExceedsTotal)
            var failedDone = false
            vm.recordSettlement(
                ledgerEntryId = 42L,
                additional = MoneyPaise.fromRupees(500),
                mode = PaymentMode.CASH,
                onDone = { failedDone = true },
            )

            assertTrue(!failedDone)
            assertEquals(DomainError.PaymentExceedsTotal.messageKey, vm.uiState.value.errorMessage)
            assertEquals(AcousticCue.ERROR, audio.cues.last())
            collectJob.cancel()
        }

    @Test
    fun `saveDirectFarmSale clamps received amount and saves manual income`() =
        runTest {
            val financeRepo = FakeFinanceRepo()
            val audio = FakeAudioEngine()
            val vm = buildViewModel(financeRepo, audio)

            var saved = false
            vm.saveDirectFarmSale(
                farmId = 0L,
                buyerName = "",
                amount = MoneyPaise.fromRupees(8_000),
                receivedAmount = MoneyPaise.fromRupees(9_500),
                paymentMode = PaymentMode.CASH,
                dateMillis = 1_794_326_000_000L,
                notes = "Mandi lot",
                onSaved = { saved = true },
            )

            assertTrue(saved)
            assertEquals("Crop Sale", financeRepo.lastManualIncomeTitle)
            assertEquals(MoneyPaise.fromRupees(8_000), financeRepo.lastManualIncomeAmount)
            assertEquals(MoneyPaise.fromRupees(8_000), financeRepo.lastManualIncomeSettled)
            assertEquals(AcousticCue.SAVED, audio.cues.last())
            assertTrue(audio.spokenTexts.last().contains("8000"))
        }

    @Test
    fun `saveExpense sets error message and plays error cue when repository fails`() =
        runTest(testDispatcher) {
            val financeRepo =
                FakeFinanceRepo().apply {
                    nextExpenseResult = AppResult.Error(DomainError.InvalidAmount)
                }
            val audio = FakeAudioEngine()
            val vm = buildViewModel(financeRepo, audio)
            val collectJob = backgroundScope.launch { vm.uiState.collect {} }

            var saved = false
            vm.saveExpense(
                farmId = 1L,
                plotId = null,
                cropCycleId = null,
                category = LedgerCategory.SEEDS,
                description = "",
                vendor = "",
                totalAmount = MoneyPaise.fromRupees(500),
                paidAmount = MoneyPaise.fromRupees(500),
                paymentMode = PaymentMode.CASH,
                dateMillis = 1_794_326_000_000L,
                photoPath = null,
                notes = "",
                onSaved = { saved = true },
            )

            assertTrue(!saved)
            assertEquals("Farm Expense", financeRepo.lastSavedExpense?.description)
            assertEquals(DomainError.InvalidAmount.messageKey, vm.uiState.value.errorMessage)
            assertEquals(AcousticCue.ERROR, audio.cues.last())
            collectJob.cancel()
        }

    private fun buildViewModel(
        financeRepo: FakeFinanceRepo,
        audio: FakeAudioEngine,
        prefsRepo: UserPreferencesRepository = FakePrefsRepo(),
    ) = FinanceViewModel(
        financeRepo = financeRepo,
        farmRepo = FakeFarmRepo(),
        fieldOpsRepo = FakeFieldOpsRepo(),
        contactRepo = FakeContactRepo(),
        prefsRepo = prefsRepo,
        photoStorage = LocalPhotoStorage(context),
        voiceManager = VoiceNoteManager(context),
        audioEngine = audio,
        timeProvider = FakeTimeProvider(),
        dispatchers = testDispatchers,
    )

    private class FakeFinanceRepo : FinanceRepository {
        var lastSavedSale: CropSale? = null
        var lastSavedExpense: ExpenseDetail? = null
        var lastPaidForExpense: MoneyPaise? = null
        var lastVoidReason: String? = null
        var lastSettlement: Triple<Long, MoneyPaise, PaymentMode>? = null
        var lastManualIncomeTitle: String? = null
        var lastManualIncomeAmount: MoneyPaise? = null
        var lastManualIncomeSettled: MoneyPaise? = null
        var nextExpenseResult: AppResult<Long>? = null
        var nextSettlementResult: AppResult<Unit>? = null

        override fun observeLedgerEntries(
            farmId: Long?,
            cropCycleId: Long?,
            entryType: LedgerEntryType?,
            query: String,
        ): Flow<List<LedgerEntry>> = flowOf(emptyList())

        override fun observeSeasonTotals(farmId: Long?): Flow<SeasonFinancialTotals> =
            flowOf(
                SeasonFinancialTotals(MoneyPaise.ZERO, MoneyPaise.ZERO, MoneyPaise.ZERO, MoneyPaise.ZERO),
            )

        override fun observeLedgerEntry(ledgerEntryId: Long): Flow<LedgerEntry?> = flowOf(null)

        override suspend fun getExpenseDetail(expenseId: Long): ExpenseDetail? = null

        override suspend fun saveManualExpense(
            detail: ExpenseDetail,
            paidAmount: MoneyPaise,
        ): AppResult<Long> {
            lastSavedExpense = detail
            lastPaidForExpense = paidAmount
            return nextExpenseResult ?: AppResult.Success(1L)
        }

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
        ): AppResult<Long> {
            lastManualIncomeTitle = title
            lastManualIncomeAmount = amount
            lastManualIncomeSettled = settledAmount
            return AppResult.Success(1L)
        }

        override fun observeCropSales(cropCycleId: Long?): Flow<List<CropSale>> = flowOf(emptyList())

        override fun observeCropSale(saleId: Long): Flow<CropSale?> = flowOf(null)

        override suspend fun saveCropSale(sale: CropSale): AppResult<Long> {
            lastSavedSale = sale
            return AppResult.Success(10L)
        }

        override suspend fun recordLedgerSettlement(
            ledgerEntryId: Long,
            additionalPaidOrReceived: MoneyPaise,
            paymentMode: PaymentMode,
        ): AppResult<Unit> {
            lastSettlement = Triple(ledgerEntryId, additionalPaidOrReceived, paymentMode)
            return nextSettlementResult ?: AppResult.Success(Unit)
        }

        override suspend fun voidLedgerEntry(
            ledgerEntryId: Long,
            reason: String,
        ): AppResult<Unit> {
            lastVoidReason = reason
            return AppResult.Success(Unit)
        }

        override fun observeCropProfitabilitySummaries(farmId: Long?): Flow<List<CropProfitabilitySummary>> = flowOf(emptyList())
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

    private class FakeFieldOpsRepo : FieldOpsRepository {
        override fun observeIrrigationLogs(
            plotId: Long?,
            cropCycleId: Long?,
        ): Flow<List<IrrigationLog>> = flowOf(emptyList())

        override suspend fun getIrrigationLog(logId: Long): IrrigationLog? = null

        override suspend fun saveIrrigationLog(log: IrrigationLog): AppResult<Long> = AppResult.Success(1L)

        override fun observeHarvestRecords(cropCycleId: Long?): Flow<List<HarvestRecord>> = flowOf(emptyList())

        override suspend fun getHarvestRecord(harvestId: Long): HarvestRecord? = null

        override suspend fun saveHarvestRecord(record: HarvestRecord): AppResult<Long> = AppResult.Success(1L)

        override fun observeStorageRecords(cropCycleId: Long?): Flow<List<StorageRecord>> = flowOf(emptyList())

        override suspend fun getStorageRecord(storageId: Long): StorageRecord? = null

        override suspend fun saveStorageRecord(record: StorageRecord): AppResult<Long> = AppResult.Success(1L)

        override fun observeSoilTests(plotId: Long?): Flow<List<SoilTestRecord>> = flowOf(emptyList())

        override suspend fun getSoilTest(testId: Long): SoilTestRecord? = null

        override suspend fun saveSoilTest(record: SoilTestRecord): AppResult<Long> = AppResult.Success(1L)
    }

    private class FakeContactRepo : ContactAndSearchRepository {
        override fun observeContacts(
            typeFilter: ContactType?,
            query: String,
        ): Flow<List<Contact>> = flowOf(emptyList())

        override fun observeContact(contactId: Long): Flow<Contact?> = flowOf(null)

        override suspend fun saveContact(contact: Contact): AppResult<Long> = AppResult.Success(1L)

        override suspend fun archiveContact(contactId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun searchOffline(query: String): List<GlobalSearchHit> = emptyList()

        override suspend fun getDataOverviewCounts(): DataOverviewCounts = DataOverviewCounts(0, 0, 0, 0, 0, 0, 0)
    }

    private class FakePrefsRepo(
        initialPrefs: UserPreferences = UserPreferences(),
    ) : UserPreferencesRepository {
        override val userPreferences: Flow<UserPreferences> = flowOf(initialPrefs)

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
        val cues = mutableListOf<AcousticCue>()
        val spokenTexts = mutableListOf<String>()
        val spokenCueKeys = mutableListOf<Pair<String, Boolean>>()
        override val activeCueKey: StateFlow<String?> = MutableStateFlow(null)
        override val isSpeaking: StateFlow<Boolean> = MutableStateFlow(false)

        override fun speakCue(
            cueKey: String,
            useTelugu: Boolean,
        ) {
            spokenCueKeys += cueKey to useTelugu
        }

        override fun speakDynamicText(
            text: String,
            useTelugu: Boolean,
        ) {
            spokenTexts += text
        }

        override fun playAcousticFeedback(cue: AcousticCue) {
            cues += cue
        }

        override fun stop() {}

        override fun isTeluguVoiceReady(): Boolean = true

        override fun hasCustomPrompt(cueKey: String): Boolean = false

        override fun customPromptFilePath(cueKey: String): String = ""

        override fun clearCustomPrompt(cueKey: String) {}
    }

    private class FakeTimeProvider : TimeProvider {
        override fun nowEpochMillis(): Long = 1_794_326_000_000L

        override fun todayEpochDay(): Long = 20_650L

        override fun elapsedRealtimeMillis(): Long = 1000L

        override fun bootCount(): Int = 1

        override fun epochMillisToEpochDay(epochMillis: Long): Long = 20_650L

        override fun epochDayToStartMillis(epochDay: Long): Long = epochDay * 86_400_000L

        override fun formatDate(
            epochMillis: Long,
            useTelugu: Boolean,
        ): String = "10 Nov 2026"

        override fun formatEpochDay(
            epochDay: Long,
            useTelugu: Boolean,
        ): String = "10 Nov 2026"

        override fun formatTime(epochMillis: Long): String = "10:00 AM"
    }
}
