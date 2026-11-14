package com.farmsaathi.feature.operations

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.audio.VoiceNoteManager
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.BatchAttendanceItem
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.FinanceRepository
import com.farmsaathi.core.common.repository.LabourRepository
import com.farmsaathi.core.common.repository.SeasonFinancialTotals
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.media.LocalPhotoStorage
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.AttendanceStatus
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropProfitabilitySummary
import com.farmsaathi.core.model.CropSale
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CropTemplate
import com.farmsaathi.core.model.ExpenseDetail
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.core.model.Worker
import com.farmsaathi.core.model.WorkerAttendance
import com.farmsaathi.core.model.WorkerBalanceSummary
import com.farmsaathi.core.model.WorkerPayment
import com.farmsaathi.feature.operations.util.WageReceiptData
import com.farmsaathi.feature.operations.util.WageReceiptShareHelper
import com.farmsaathi.feature.operations.viewmodel.LabourHubMode
import com.farmsaathi.feature.operations.viewmodel.LabourViewModel
import com.farmsaathi.feature.operations.viewmodel.WorkerFormValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LabourReArchitectureTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testDispatchers =
        object : AppDispatchers {
            override val io = testDispatcher
            override val default = testDispatcher
            override val main = testDispatcher
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
    fun `context aware hub mode defaults to crew calculator and toggles with zero workers`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            val collectorJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

            assertEquals(LabourHubMode.CREW_CALCULATOR, vm.uiState.value.hubMode)
            assertTrue(vm.uiState.value.workers.isEmpty())

            vm.selectHubMode(LabourHubMode.WORKER_BALANCE)
            assertEquals(LabourHubMode.WORKER_BALANCE, vm.uiState.value.hubMode)

            vm.selectHubMode(LabourHubMode.CREW_CALCULATOR)
            assertEquals(LabourHubMode.CREW_CALCULATOR, vm.uiState.value.hubMode)
            collectorJob.cancel()
        }

    @Test
    fun `halfDayRatePaise overrides percentage math on half sun attendance`() =
        runTest(testDispatcher) {
            val labourRepo = FakeLabourRepo()
            val vm = createViewModel(labourRepo = labourRepo)

            val fixedHalfWorker =
                Worker(
                    id = 10L,
                    name = "రాజు",
                    defaultDailyWage = MoneyPaise.fromRupees(500),
                    defaultHalfDayPercent = 50,
                    halfDayRatePaise = 20_000L,
                )
            vm.markAttendanceQuick(fixedHalfWorker, AttendanceStatus.HALF_DAY, farmId = 1L)
            assertEquals(MoneyPaise.fromRupees(200), labourRepo.lastAttendance?.wageAccrued)

            val fallbackWorker = fixedHalfWorker.copy(id = 11L, halfDayRatePaise = null)
            vm.markAttendanceQuick(fallbackWorker, AttendanceStatus.HALF_DAY, farmId = 1L)
            assertEquals(MoneyPaise.fromRupees(250), labourRepo.lastAttendance?.wageAccrued)

            vm.markAttendanceQuick(fallbackWorker, AttendanceStatus.ABSENT, farmId = 1L)
            assertEquals(MoneyPaise.ZERO, labourRepo.lastAttendance?.wageAccrued)
        }

    @Test
    fun `saveWorker persists halfDayRatePaise and clears override when null`() =
        runTest(testDispatcher) {
            val labourRepo = FakeLabourRepo()
            val vm = createViewModel(labourRepo = labourRepo)

            vm.saveWorker(
                existing = null,
                name = "లక్ష్మి",
                phone = "",
                skillRole = "",
                dailyWage = MoneyPaise.fromRupees(400),
                notes = "",
                halfDayRatePaise = 25_000L,
            ) {}
            assertEquals(25_000L, labourRepo.lastSavedWorker?.halfDayRatePaise)

            val existing = labourRepo.lastSavedWorker!!
            vm.saveWorker(
                existing = existing,
                name = "లక్ష్మి",
                phone = "",
                skillRole = "",
                dailyWage = MoneyPaise.fromRupees(400),
                notes = "",
                halfDayRatePaise = null,
            ) {}
            assertNull(labourRepo.lastSavedWorker?.halfDayRatePaise)
        }

    @Test
    fun `batch full sun and repeat yesterday wire to LabourRepository`() =
        runTest(testDispatcher) {
            val workerA = Worker(id = 1L, farmId = 2L, name = "రాము", defaultDailyWage = MoneyPaise.fromRupees(400))
            val workerB = Worker(id = 2L, farmId = 2L, name = "సీత", defaultDailyWage = MoneyPaise.fromRupees(350))
            val labourRepo =
                FakeLabourRepo(
                    initialWorkers =
                    listOf(
                        WorkerBalanceSummary(workerA, MoneyPaise.ZERO, MoneyPaise.ZERO, MoneyPaise.ZERO),
                        WorkerBalanceSummary(workerB, MoneyPaise.ZERO, MoneyPaise.ZERO, MoneyPaise.ZERO),
                    ),
                )
            val audio = FakeAudioEngine()
            val vm = createViewModel(labourRepo = labourRepo, audio = audio)
            val collectorJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

            var batchCount = 0
            vm.markAllFullDay { batchCount = it }
            assertEquals(2, batchCount)
            assertEquals(2, labourRepo.lastBatchItems.size)
            assertTrue(labourRepo.lastBatchItems.all { it.status == AttendanceStatus.FULL_DAY })
            assertEquals(AcousticCue.SAVED, audio.cues.last())

            labourRepo.repeatResultCount = 2
            var repeated = 0
            vm.repeatYesterdayAttendance { repeated = it }
            assertEquals(2, repeated)
            assertEquals(20_650L, labourRepo.lastRepeatTargetDay)
            assertTrue(audio.spokenTexts.last().contains("2"))

            collectorJob.cancel()
        }

    @Test
    fun `worker form validation decouples nameError and wageError`() {
        val blankNameValidWage =
            WorkerFormValidator.validate(
                quickMode = false,
                name = "   ",
                wageStr = "400",
                useCustomHalfDayRate = false,
                customHalfDayRateStr = "",
                requiredMessage = "Required",
                invalidNumberMessage = "Invalid",
            )
        assertFalse(blankNameValidWage.isValid)
        assertEquals("Required", blankNameValidWage.nameError)
        assertNull(blankNameValidWage.wageError)

        val validNameInvalidWage =
            WorkerFormValidator.validate(
                quickMode = false,
                name = "మల్లయ్య",
                wageStr = "",
                useCustomHalfDayRate = false,
                customHalfDayRateStr = "",
                requiredMessage = "Required",
                invalidNumberMessage = "Invalid",
            )
        assertFalse(validNameInvalidWage.isValid)
        assertNull(validNameInvalidWage.nameError)
        assertEquals("Required", validNameInvalidWage.wageError)

        val quickModeBlankName =
            WorkerFormValidator.validate(
                quickMode = true,
                name = "",
                wageStr = "450",
                useCustomHalfDayRate = false,
                customHalfDayRateStr = "",
                requiredMessage = "Required",
                invalidNumberMessage = "Invalid",
            )
        assertTrue(quickModeBlankName.isValid)
        assertNull(quickModeBlankName.nameError)
        assertNull(quickModeBlankName.wageError)
    }

    @Test
    fun `crew payout and settlement emit visual receipt data and png helper saves in camera_tmp`() =
        runTest(testDispatcher) {
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            val worker = Worker(id = 5L, farmId = 1L, name = "వెంకన్న", defaultDailyWage = MoneyPaise.fromRupees(400))
            val summary =
                WorkerBalanceSummary(
                    worker = worker,
                    totalWageAccrued = MoneyPaise.fromRupees(800),
                    totalAdvancesPaid = MoneyPaise.ZERO,
                    totalSettlementsPaid = MoneyPaise.ZERO,
                    totalDaysWorkedTenths = 20,
                )
            val labourRepo = FakeLabourRepo(initialWorkers = listOf(summary))
            val audio = FakeAudioEngine()
            val vm = createViewModel(labourRepo = labourRepo, audio = audio)
            val collectorJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

            var crewReceipt: WageReceiptData? = null
            vm.saveGroupWageExpense(
                workerCount = 4,
                daysTenths = 15,
                dailyRate = MoneyPaise.fromRupees(400),
                advancePaid = MoneyPaise.fromRupees(200),
                mode = PaymentMode.UPI,
                notes = "",
                onReceipt = { crewReceipt = it },
                onSaved = {},
            )
            assertNotNull(crewReceipt)
            assertEquals(1, crewReceipt?.fullSunCount)
            assertEquals(1, crewReceipt?.halfSunCount)
            assertEquals("UPI", crewReceipt?.paymentModeLabel)
            assertEquals(MoneyPaise.fromRupees(2200), crewReceipt?.paidAmount)
            assertTrue(audio.spokenTexts.last().contains("4"))

            var settleReceipt: WageReceiptData? = null
            vm.settleWorkerBalance(
                summary = summary,
                amount = MoneyPaise.fromRupees(800),
                mode = PaymentMode.CASH,
                onReceipt = { settleReceipt = it },
                onSaved = {},
            )
            assertNotNull(settleReceipt)
            assertEquals(MoneyPaise.ZERO, settleReceipt?.remainingBalance)
            assertEquals(2, settleReceipt?.fullSunCount)

            val bitmap = WageReceiptShareHelper.renderReceiptBitmap(settleReceipt!!)
            val pngFile = WageReceiptShareHelper.saveReceiptPng(ctx, bitmap)
            assertTrue(pngFile.exists())
            assertTrue(pngFile.length() > 0L)
            assertEquals("camera_tmp", pngFile.parentFile?.name)
            pngFile.delete()

            collectorJob.cancel()
        }

    @Test
    fun `shareReceipt renders png on ioDispatcher and starts activity on mainDispatcher`() =
        runTest(testDispatcher) {
            val baseCtx = ApplicationProvider.getApplicationContext<Context>()
            var startedIntent: Intent? = null
            var ioDispatches = 0
            var mainDispatches = 0
            val backingDispatcher = StandardTestDispatcher(testDispatcher.scheduler)
            val ioTracker =
                object : CoroutineDispatcher() {
                    override fun dispatch(
                        context: CoroutineContext,
                        block: Runnable,
                    ) {
                        ioDispatches++
                        backingDispatcher.dispatch(context, block)
                    }
                }
            val mainTracker =
                object : CoroutineDispatcher() {
                    override fun dispatch(
                        context: CoroutineContext,
                        block: Runnable,
                    ) {
                        mainDispatches++
                        backingDispatcher.dispatch(context, block)
                    }
                }
            val trackingContext =
                object : ContextWrapper(baseCtx) {
                    override fun startActivity(intent: Intent?) {
                        startedIntent = intent
                    }
                }
            val receipt =
                WageReceiptData(
                    title = "రామయ్య",
                    subtitle = "2 రోజులు",
                    daysLabel = "2.0",
                    paidAmount = MoneyPaise.fromRupees(800),
                    remainingBalance = MoneyPaise.ZERO,
                    paymentModeLabel = "నగదు",
                    dateFormatted = "12 Nov 2026",
                    useTelugu = true,
                    spokenSummary = "రామయ్యకు 800 రూపాయలు ఇచ్చాము",
                )

            val result =
                WageReceiptShareHelper.shareReceipt(
                    context = trackingContext,
                    receipt = receipt,
                    ioDispatcher = ioTracker,
                    mainDispatcher = mainTracker,
                    uriProvider = { _, file -> Uri.fromFile(file) },
                )

            val savedFile = result.getOrThrow()
            assertTrue(savedFile.exists())
            assertTrue(ioDispatches > 0)
            assertTrue(mainDispatches > 0)
            assertEquals(Intent.ACTION_CHOOSER, startedIntent?.action)
            savedFile.delete()
        }

    private fun createViewModel(
        labourRepo: FakeLabourRepo = FakeLabourRepo(),
        audio: FakeAudioEngine = FakeAudioEngine(),
    ): LabourViewModel {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        return LabourViewModel(
            labourRepo = labourRepo,
            farmRepo = FakeFarmRepo(),
            financeRepo = FakeFinanceRepo(),
            prefsRepo = FakePrefsRepo(),
            photoStorage = LocalPhotoStorage(ctx),
            voiceManager = VoiceNoteManager(ctx),
            audioEngine = audio,
            timeProvider = FakeTimeProvider(),
            dispatchers = testDispatchers,
        )
    }

    private class FakeLabourRepo(
        initialWorkers: List<WorkerBalanceSummary> = emptyList(),
    ) : LabourRepository {
        private val workersFlow = MutableStateFlow(initialWorkers)
        var lastAttendance: WorkerAttendance? = null
        var lastSavedWorker: Worker? = null
        var lastBatchItems: List<BatchAttendanceItem> = emptyList()
        var lastRepeatTargetDay: Long? = null
        var repeatResultCount: Int = 0

        override fun observeWorkersWithBalance(): Flow<List<WorkerBalanceSummary>> = workersFlow

        override fun observeWorker(workerId: Long): Flow<Worker?> = flowOf(null)

        override fun observeWorkerAttendance(workerId: Long): Flow<List<WorkerAttendance>> = flowOf(emptyList())

        override fun observeAttendanceForDay(workDateEpochDay: Long): Flow<List<WorkerAttendance>> = flowOf(emptyList())

        override fun observeWorkerPayments(workerId: Long): Flow<List<WorkerPayment>> = flowOf(emptyList())

        override suspend fun saveWorker(worker: Worker): AppResult<Long> {
            lastSavedWorker = worker
            return AppResult.Success(1L)
        }

        override suspend fun archiveWorker(workerId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun adjustWorkerDaysTenths(
            workerId: Long,
            deltaTenths: Int,
        ): AppResult<Int> = AppResult.Success(10)

        override suspend fun setWorkerDaysTenths(
            workerId: Long,
            daysTenths: Int,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun upsertAttendance(attendance: WorkerAttendance): AppResult<Long> {
            lastAttendance = attendance
            return AppResult.Success(1L)
        }

        override suspend fun markBatchAttendance(
            farmId: Long,
            workDateEpochDay: Long,
            items: List<BatchAttendanceItem>,
        ): AppResult<Int> {
            lastBatchItems = items
            return AppResult.Success(items.size)
        }

        override suspend fun repeatYesterdayAttendance(
            targetEpochDay: Long,
            farmId: Long?,
        ): AppResult<Int> {
            lastRepeatTargetDay = targetEpochDay
            return AppResult.Success(repeatResultCount)
        }

        override suspend fun recordWorkerPayment(payment: WorkerPayment): AppResult<Long> = AppResult.Success(1L)

        override suspend fun voidWorkerPayment(
            paymentId: Long,
            reason: String,
        ): AppResult<Unit> = AppResult.Success(Unit)
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

    private class FakeFinanceRepo : FinanceRepository {
        override fun observeLedgerEntries(
            farmId: Long?,
            cropCycleId: Long?,
            entryType: LedgerEntryType?,
            query: String,
        ): Flow<List<LedgerEntry>> = flowOf(emptyList())

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
        val cues = mutableListOf<AcousticCue>()
        val spokenTexts = mutableListOf<String>()
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
        ): String = "12 Nov 2026"

        override fun formatEpochDay(
            epochDay: Long,
            useTelugu: Boolean,
        ): String = "12 Nov 2026"

        override fun formatTime(epochMillis: Long): String = "10:00 AM"
    }
}
