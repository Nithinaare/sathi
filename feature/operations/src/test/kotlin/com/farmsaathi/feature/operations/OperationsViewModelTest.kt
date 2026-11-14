package com.farmsaathi.feature.operations

import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.audio.VoiceNoteManager
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.DataOverviewCounts
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.FieldOpsRepository
import com.farmsaathi.core.common.repository.FinanceRepository
import com.farmsaathi.core.common.repository.GlobalSearchHit
import com.farmsaathi.core.common.repository.InventoryRepository
import com.farmsaathi.core.common.repository.LabourRepository
import com.farmsaathi.core.common.repository.MachineryRepository
import com.farmsaathi.core.common.repository.SeasonFinancialTotals
import com.farmsaathi.core.common.repository.StartMachinerySessionRequest
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.media.LocalPhotoStorage
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.AttendanceStatus
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
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.core.model.InputProduct
import com.farmsaathi.core.model.InputPurchase
import com.farmsaathi.core.model.IrrigationLog
import com.farmsaathi.core.model.IrrigationMethod
import com.farmsaathi.core.model.IrrigationSource
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.MachineryBillingMode
import com.farmsaathi.core.model.MachinerySession
import com.farmsaathi.core.model.MachineryUsageType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.SeasonType
import com.farmsaathi.core.model.SoilHealthRating
import com.farmsaathi.core.model.SoilTestRecord
import com.farmsaathi.core.model.StockMovement
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.core.model.StorageRecord
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.core.model.Worker
import com.farmsaathi.core.model.WorkerAttendance
import com.farmsaathi.core.model.WorkerBalanceSummary
import com.farmsaathi.core.model.WorkerPayment
import com.farmsaathi.feature.operations.viewmodel.FieldOpsAndContactsViewModel
import com.farmsaathi.feature.operations.viewmodel.InventoryViewModel
import com.farmsaathi.feature.operations.viewmodel.LabourViewModel
import com.farmsaathi.feature.operations.viewmodel.MachineryViewModel
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

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class OperationsViewModelTest {
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
    fun `labour quick half day attendance accrues fifty percent wage`() =
        runTest {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val labourRepo = FakeLabourRepo()
            val financeRepo = FakeFinanceRepo()
            val audio = FakeAudioEngine()
            val vm =
                LabourViewModel(
                    labourRepo = labourRepo,
                    farmRepo = FakeFarmRepo(),
                    financeRepo = financeRepo,
                    prefsRepo = FakePrefsRepo(),
                    photoStorage = LocalPhotoStorage(ctx),
                    voiceManager = VoiceNoteManager(ctx),
                    audioEngine = audio,
                    timeProvider = FakeTimeProvider(),
                    dispatchers = testDispatchers,
                )
            val worker =
                Worker(
                    id = 4L,
                    name = "Mallaiah",
                    defaultDailyWage = MoneyPaise.fromRupees(500),
                    defaultHalfDayPercent = 50,
                )

            vm.markAttendanceQuick(worker, AttendanceStatus.HALF_DAY, farmId = 1L)

            assertEquals(MoneyPaise.fromRupees(250), labourRepo.lastAttendance?.wageAccrued)
            assertEquals(AcousticCue.SAVED, audio.cues.last())
        }

    @Test
    fun `labour photo quick create auto names worker and wage calculator saves expense with voice confirmation`() =
        runTest {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val labourRepo = FakeLabourRepo()
            val financeRepo = FakeFinanceRepo()
            val audio = FakeAudioEngine()
            val vm =
                LabourViewModel(
                    labourRepo = labourRepo,
                    farmRepo = FakeFarmRepo(),
                    financeRepo = financeRepo,
                    prefsRepo = FakePrefsRepo(),
                    photoStorage = LocalPhotoStorage(ctx),
                    voiceManager = VoiceNoteManager(ctx),
                    audioEngine = audio,
                    timeProvider = FakeTimeProvider(),
                    dispatchers = testDispatchers,
                )

            var savedWorkerId = 0L
            vm.saveWorker(
                existing = null,
                name = "",
                phone = "",
                skillRole = "",
                dailyWage = MoneyPaise.fromRupees(400),
                halfDayPercent = 50,
                notes = "",
                photoPath = "media/photos/w_photo.jpg",
                voiceNamePath = "media/audio/w_voice.m4a",
            ) { savedWorkerId = it }

            assertEquals(1L, savedWorkerId)
            assertEquals("కూలీ #1", labourRepo.lastSavedWorker?.name)
            assertEquals("media/photos/w_photo.jpg", labourRepo.lastSavedWorker?.photoPath)
            assertEquals("media/audio/w_voice.m4a", labourRepo.lastSavedWorker?.voiceNamePath)

            var groupSaved = false
            vm.saveGroupWageExpense(
                workerCount = 3,
                // 2.5 days
                daysTenths = 25,
                dailyRate = MoneyPaise.fromRupees(400),
                advancePaid = MoneyPaise.fromRupees(500),
                mode = PaymentMode.CASH,
                notes = "",
            ) { groupSaved = true }

            assertTrue(groupSaved)
            // 3 * 2.5 * 400 - 500 = 2500
            assertEquals(MoneyPaise.fromRupees(2500), financeRepo.lastSavedExpense?.totalAmount)
            assertTrue(audio.spokenTexts.last().contains("2500"))
        }

    @Test
    fun `machinery timer start and stop play timer cues`() =
        runTest {
            val machineryRepo = FakeMachineryRepo()
            val audio = FakeAudioEngine()
            val vm =
                MachineryViewModel(
                    machineryRepo,
                    FakeFarmRepo(),
                    FakeContactRepo(),
                    FakePrefsRepo(),
                    audio,
                    FakeTimeProvider(),
                    testDispatchers,
                )

            var startedId = 0L
            vm.startTimerSession(
                StartMachinerySessionRequest(
                    farmId = 1L,
                    plotId = null,
                    cropCycleId = null,
                    providerContactId = null,
                    machineName = "John Deere Tractor",
                    operatorName = "Ramesh",
                    operationType = "Ploughing",
                    usageType = MachineryUsageType.HIRED_IN,
                    billingMode = MachineryBillingMode.HOURLY,
                    hourlyRate = MoneyPaise.fromRupees(1_000),
                    perAcreRate = null,
                    notes = null,
                ),
                onStarted = { startedId = it },
            )
            assertEquals(12L, startedId)
            assertEquals(AcousticCue.TIMER_START, audio.cues.last())

            var stopped = false
            vm.stopTimerSession(
                sessionId = 12L,
                overrideElapsedMinutes = 60L,
                areaCoveredMicroAcres = null,
                flatAmount = null,
                paidAmount = MoneyPaise.fromRupees(1_000),
                paymentMode = PaymentMode.CASH,
                notes = null,
                onStopped = { stopped = true },
            )
            assertTrue(stopped)
            assertEquals(AcousticCue.TIMER_STOP, audio.cues.last())
        }

    @Test
    fun `inventory insufficient stock error sets insufficientStockPrompt flag`() =
        runTest(testDispatcher) {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val inventoryRepo = FakeInventoryRepo()
            val audio = FakeAudioEngine()
            val vm =
                InventoryViewModel(
                    inventoryRepo,
                    FakeFarmRepo(),
                    FakePrefsRepo(),
                    LocalPhotoStorage(ctx),
                    audio,
                    FakeTimeProvider(),
                    testDispatchers,
                )
            val collectorJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

            vm.recordUsageOrAdjustment(
                productId = 3L,
                farmId = 1L,
                plotId = null,
                cropCycleId = null,
                signedDeltaMilli = -5_000L,
                reason = StockMovementReason.FIELD_APPLICATION,
                notes = "",
                onSaved = {},
            )

            assertTrue(vm.uiState.value.insufficientStockPrompt)
            assertEquals("error_insufficient_inventory", vm.uiState.value.errorMessage)
            assertEquals(AcousticCue.ERROR, audio.cues.last())
            collectorJob.cancel()
        }

    @Test
    fun `inventory spray usage forwards plot crop and photo and purchase forwards receipt photo`() =
        runTest(testDispatcher) {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val inventoryRepo = FakeInventoryRepo().apply { returnSuccess = true }
            val audio = FakeAudioEngine()
            val vm =
                InventoryViewModel(
                    inventoryRepo,
                    FakeFarmRepo(),
                    FakePrefsRepo(),
                    LocalPhotoStorage(ctx),
                    audio,
                    FakeTimeProvider(),
                    testDispatchers,
                )
            val product =
                InputProduct(
                    id = 8L,
                    name = "Neem Oil",
                    category = InputCategory.PESTICIDE,
                    unit = QuantityUnit.ML,
                    currentStockMilliUnits = 1_000_000L,
                )

            var purchaseSaved = false
            vm.recordPurchase(
                product = product,
                farmId = 1L,
                cropCycleId = 14L,
                vendor = "Sri Sai Agro",
                qtyMilli = 500_000L,
                unitPrice = MoneyPaise(25_000L),
                totalAmount = MoneyPaise(25_000L),
                paidAmount = MoneyPaise(25_000L),
                mode = PaymentMode.CASH,
                dateMillis = 1_794_326_000_000L,
                notes = "Bill #12",
                receiptPhotoPath = "photos/bill_12.jpg",
                onSaved = { purchaseSaved = true },
            )

            assertTrue(purchaseSaved)
            assertEquals("photos/bill_12.jpg", inventoryRepo.lastPurchase?.receiptPhotoPath)

            var spraySaved = false
            vm.recordUsageOrAdjustment(
                productId = 8L,
                farmId = 1L,
                plotId = 4L,
                cropCycleId = 14L,
                signedDeltaMilli = -250_000L,
                reason = StockMovementReason.FIELD_APPLICATION,
                notes = "Bollworm spray",
                photoPath = "photos/spray_8.jpg",
                onSaved = { spraySaved = true },
            )

            assertTrue(spraySaved)
            assertEquals(8L, inventoryRepo.lastUsageProductId)
            assertEquals(4L, inventoryRepo.lastUsagePlotId)
            assertEquals(14L, inventoryRepo.lastUsageCropCycleId)
            assertEquals("photos/spray_8.jpg", inventoryRepo.lastUsagePhotoPath)
            assertEquals(AcousticCue.SAVED, audio.cues.last())
        }

    @Test
    fun `fieldOps watering save supports edit nextDay photo and archiveIrrigationLog`() =
        runTest(testDispatcher) {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val fieldOpsRepo = FakeFieldOpsRepo()
            val audio = FakeAudioEngine()
            val vm =
                FieldOpsAndContactsViewModel(
                    fieldOpsRepo = fieldOpsRepo,
                    contactRepo = FakeContactRepo(),
                    farmRepo = FakeFarmRepo(),
                    prefsRepo = FakePrefsRepo(),
                    photoStorage = LocalPhotoStorage(ctx),
                    voiceManager = VoiceNoteManager(ctx),
                    audioEngine = audio,
                    timeProvider = FakeTimeProvider(),
                    dispatchers = testDispatchers,
                )
            val plot = Plot(id = 3L, farmId = 2L, name = "South Block", areaMicroAcres = 1_000_000L)

            var saved = false
            vm.saveIrrigationQuick(
                selectedPlot = plot,
                cropCycleId = null,
                dateMillis = 1_794_326_000_000L,
                durationMins = 90,
                source = IrrigationSource.BOREWELL,
                method = IrrigationMethod.DRIP,
                nextDay = 20_655L,
                photoPath = "photos/drip_3.jpg",
                notes = "Evening drip",
                existingId = 41L,
                onSaved = { saved = true },
            )

            assertTrue(saved)
            assertEquals(41L, fieldOpsRepo.lastSavedIrrigation?.id)
            assertEquals(IrrigationMethod.DRIP, fieldOpsRepo.lastSavedIrrigation?.method)
            assertEquals(20_655L, fieldOpsRepo.lastSavedIrrigation?.nextWateringEpochDay)
            assertEquals("photos/drip_3.jpg", fieldOpsRepo.lastSavedIrrigation?.photoPath)

            val savedLog = requireNotNull(fieldOpsRepo.lastSavedIrrigation)
            var archived = false
            vm.archiveIrrigationLog(savedLog) { archived = true }
            assertTrue(archived)
            assertTrue(fieldOpsRepo.lastSavedIrrigation?.isArchived == true)
        }

    @Test
    fun `fieldOps harvest soil and contact helpers forward edit ids photos and cues`() =
        runTest(testDispatcher) {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val fieldOpsRepo = FakeFieldOpsRepo()
            val audio = FakeAudioEngine()
            val vm =
                FieldOpsAndContactsViewModel(
                    fieldOpsRepo = fieldOpsRepo,
                    contactRepo = FakeContactRepo(),
                    farmRepo = FakeFarmRepo(),
                    prefsRepo = FakePrefsRepo(),
                    photoStorage = LocalPhotoStorage(ctx),
                    voiceManager = VoiceNoteManager(ctx),
                    audioEngine = audio,
                    timeProvider = FakeTimeProvider(),
                    dispatchers = testDispatchers,
                )
            val plot = Plot(id = 5L, farmId = 2L, name = "East Block", areaMicroAcres = 1_500_000L)
            val cycle =
                CropCycle(
                    id = 19L,
                    farmId = 2L,
                    plotId = 5L,
                    cropName = "Paddy",
                    season = SeasonType.KHARIF,
                    cropYear = 2026,
                    plantedAreaMicroAcres = 1_500_000L,
                    sowingDateEpochDay = 20_550L,
                )

            vm.saveHarvestAndOptionalStorage(
                cycle = cycle,
                harvestDay = 20_650L,
                qtyMilli = 15_000L,
                unit = QuantityUnit.QUINTAL,
                bagCount = 20,
                qualityGrade = "A",
                storageLocation = "Home Room",
                notes = "Weighed at field",
                existingId = 77L,
                photoPath = "photos/slip_77.jpg",
                onSaved = {},
            )

            assertEquals(77L, fieldOpsRepo.lastSavedHarvest?.id)
            assertEquals("photos/slip_77.jpg", fieldOpsRepo.lastSavedHarvest?.photoPath)
            assertEquals(null, fieldOpsRepo.lastSavedStorage)

            vm.saveSoilTest(
                plot = plot,
                testDay = 20_650L,
                labName = "District Lab",
                phTenths = 72,
                nitrogenTenths = null,
                phosphorusTenths = null,
                potassiumTenths = null,
                rating = SoilHealthRating.OPTIMAL,
                photoPath = "photos/soil_88.jpg",
                notes = "Healthy soil",
                existingId = 88L,
                onSaved = {},
            )

            assertEquals(88L, fieldOpsRepo.lastSavedSoilTest?.id)
            assertEquals("photos/soil_88.jpg", fieldOpsRepo.lastSavedSoilTest?.reportPhotoPath)

            vm.speakContactDetails("Ramesh", "Driver", "9876543210")
            assertTrue(audio.spokenTexts.last().contains("Ramesh"))
            vm.speakBlankContactVoiceHint()
            assertTrue(audio.spokenTexts.last().isNotBlank())
            vm.playErrorCue()
            assertEquals(AcousticCue.ERROR, audio.cues.last())
        }

    private class FakeLabourRepo : LabourRepository {
        var lastAttendance: WorkerAttendance? = null
        var lastSavedWorker: Worker? = null

        override fun observeWorkersWithBalance(): Flow<List<WorkerBalanceSummary>> = flowOf(emptyList())

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

        override suspend fun recordWorkerPayment(payment: WorkerPayment): AppResult<Long> = AppResult.Success(1L)

        override suspend fun voidWorkerPayment(
            paymentId: Long,
            reason: String,
        ): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeMachineryRepo : MachineryRepository {
        override fun observeRunningSessions(): Flow<List<MachinerySession>> = flowOf(emptyList())

        override fun observeSessions(cropCycleId: Long?): Flow<List<MachinerySession>> = flowOf(emptyList())

        override fun observeSession(sessionId: Long): Flow<MachinerySession?> = flowOf(null)

        override suspend fun startLiveSession(request: StartMachinerySessionRequest): AppResult<Long> = AppResult.Success(12L)

        override suspend fun stopLiveSession(
            sessionId: Long,
            overrideElapsedMinutes: Long?,
            areaCoveredMicroAcres: Long?,
            flatAmount: MoneyPaise?,
            paidAmount: MoneyPaise,
            paymentMode: PaymentMode,
            notes: String?,
        ): AppResult<MachinerySession> =
            AppResult.Success(
                MachinerySession(
                    id = sessionId,
                    farmId = 1L,
                    machineName = "Tractor",
                    operationType = "Ploughing",
                    equipmentKey = "tractor",
                    startEpochMillis = 1000L,
                ),
            )

        override suspend fun cancelLiveSession(sessionId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun saveManualSession(
            session: MachinerySession,
            paymentMode: PaymentMode,
        ): AppResult<Long> = AppResult.Success(1L)
    }

    private class FakeInventoryRepo : InventoryRepository {
        var returnSuccess = false
        var lastPurchase: InputPurchase? = null
        var lastUsageProductId: Long? = null
        var lastUsagePlotId: Long? = null
        var lastUsageCropCycleId: Long? = null
        var lastUsagePhotoPath: String? = null

        override fun observeProducts(includeArchived: Boolean): Flow<List<InputProduct>> = flowOf(emptyList())

        override fun observeProduct(productId: Long): Flow<InputProduct?> = flowOf(null)

        override fun observePurchases(productId: Long?): Flow<List<InputPurchase>> = flowOf(emptyList())

        override fun observeMovements(productId: Long): Flow<List<StockMovement>> = flowOf(emptyList())

        override fun observeAllMovements(): Flow<List<StockMovement>> = flowOf(emptyList())

        override suspend fun saveProduct(product: InputProduct): AppResult<Long> = AppResult.Success(1L)

        override suspend fun archiveProduct(productId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun recordPurchase(purchase: InputPurchase): AppResult<Long> {
            lastPurchase = purchase
            return AppResult.Success(1L)
        }

        override suspend fun recordStockAdjustmentOrUsage(
            productId: Long,
            farmId: Long,
            plotId: Long?,
            cropCycleId: Long?,
            signedDeltaMilliUnits: Long,
            reason: StockMovementReason,
            notes: String?,
        ): AppResult<Long> =
            recordStockAdjustmentOrUsage(
                productId = productId,
                farmId = farmId,
                plotId = plotId,
                cropCycleId = cropCycleId,
                signedDeltaMilliUnits = signedDeltaMilliUnits,
                reason = reason,
                notes = notes,
                photoPath = null,
            )

        override suspend fun recordStockAdjustmentOrUsage(
            productId: Long,
            farmId: Long,
            plotId: Long?,
            cropCycleId: Long?,
            signedDeltaMilliUnits: Long,
            reason: StockMovementReason,
            notes: String?,
            photoPath: String?,
        ): AppResult<Long> {
            lastUsageProductId = productId
            lastUsagePlotId = plotId
            lastUsageCropCycleId = cropCycleId
            lastUsagePhotoPath = photoPath
            return if (returnSuccess) AppResult.Success(9L) else AppResult.Error(DomainError.InsufficientInventory(2_000L))
        }
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
        ): String = "10 Nov 2026"

        override fun formatEpochDay(
            epochDay: Long,
            useTelugu: Boolean,
        ): String = "10 Nov 2026"

        override fun formatTime(epochMillis: Long): String = "10:00 AM"
    }

    private class FakeFinanceRepo : FinanceRepository {
        var lastSavedExpense: ExpenseDetail? = null

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
        ): AppResult<Long> {
            lastSavedExpense = detail
            return AppResult.Success(1L)
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

    private class FakeFieldOpsRepo : FieldOpsRepository {
        var lastSavedIrrigation: IrrigationLog? = null
        var lastSavedHarvest: HarvestRecord? = null
        var lastSavedStorage: StorageRecord? = null
        var lastSavedSoilTest: SoilTestRecord? = null

        override fun observeIrrigationLogs(
            plotId: Long?,
            cropCycleId: Long?,
        ): Flow<List<IrrigationLog>> = flowOf(emptyList())

        override suspend fun getIrrigationLog(logId: Long): IrrigationLog? = null

        override suspend fun saveIrrigationLog(log: IrrigationLog): AppResult<Long> {
            lastSavedIrrigation = log
            return AppResult.Success(1L)
        }

        override fun observeHarvestRecords(cropCycleId: Long?): Flow<List<HarvestRecord>> = flowOf(emptyList())

        override suspend fun getHarvestRecord(harvestId: Long): HarvestRecord? = null

        override suspend fun saveHarvestRecord(record: HarvestRecord): AppResult<Long> {
            lastSavedHarvest = record
            return AppResult.Success(1L)
        }

        override fun observeStorageRecords(cropCycleId: Long?): Flow<List<StorageRecord>> = flowOf(emptyList())

        override suspend fun getStorageRecord(storageId: Long): StorageRecord? = null

        override suspend fun saveStorageRecord(record: StorageRecord): AppResult<Long> {
            lastSavedStorage = record
            return AppResult.Success(1L)
        }

        override fun observeSoilTests(plotId: Long?): Flow<List<SoilTestRecord>> = flowOf(emptyList())

        override suspend fun getSoilTest(testId: Long): SoilTestRecord? = null

        override suspend fun saveSoilTest(record: SoilTestRecord): AppResult<Long> {
            lastSavedSoilTest = record
            return AppResult.Success(1L)
        }
    }
}
