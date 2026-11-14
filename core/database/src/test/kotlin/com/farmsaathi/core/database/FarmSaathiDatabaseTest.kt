package com.farmsaathi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.StartMachinerySessionRequest
import com.farmsaathi.core.database.repository.RoomFarmRepository
import com.farmsaathi.core.database.repository.RoomFieldOpsRepository
import com.farmsaathi.core.database.repository.RoomFinanceRepository
import com.farmsaathi.core.database.repository.RoomInventoryRepository
import com.farmsaathi.core.database.repository.RoomLabourRepository
import com.farmsaathi.core.database.repository.RoomMachineryRepository
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AreaConverter
import com.farmsaathi.core.model.AttendanceStatus
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropSale
import com.farmsaathi.core.model.DomainError
import com.farmsaathi.core.model.ExpenseDetail
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.core.model.InputProduct
import com.farmsaathi.core.model.InputPurchase
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.MachineryBillingMode
import com.farmsaathi.core.model.MachinerySessionStatus
import com.farmsaathi.core.model.MachineryUsageType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.SeasonType
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.core.model.StorageRecord
import com.farmsaathi.core.model.Worker
import com.farmsaathi.core.model.WorkerAttendance
import com.farmsaathi.core.model.WorkerPayment
import com.farmsaathi.core.model.WorkerPaymentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FarmSaathiDatabaseTest {
    private lateinit var db: FarmSaathiDatabase
    private lateinit var fakeTime: FakeTimeProvider
    private lateinit var coordinator: DatabaseMaintenanceCoordinator
    private lateinit var farmRepo: RoomFarmRepository
    private lateinit var financeRepo: RoomFinanceRepository
    private lateinit var labourRepo: RoomLabourRepository
    private lateinit var machineryRepo: RoomMachineryRepository
    private lateinit var inventoryRepo: RoomInventoryRepository
    private lateinit var fieldOpsRepo: RoomFieldOpsRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, FarmSaathiDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        fakeTime = FakeTimeProvider()
        coordinator = DatabaseMaintenanceCoordinator()
        farmRepo = RoomFarmRepository(db.farmDao(), db.plotDao(), db.cropDao(), fakeTime, coordinator)
        financeRepo = RoomFinanceRepository(db.financeDao(), db.cropDao(), db.fieldOpsDao(), fakeTime, coordinator)
        labourRepo = RoomLabourRepository(db.labourDao(), fakeTime, coordinator)
        machineryRepo = RoomMachineryRepository(db.machineryDao(), fakeTime, coordinator)
        inventoryRepo = RoomInventoryRepository(db.inventoryDao(), fakeTime, coordinator)
        fieldOpsRepo = RoomFieldOpsRepository(db.fieldOpsDao(), fakeTime, coordinator, db.cropDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `crop cycle generates template tasks and shifts uncompleted tasks on sowing change`() =
        runTest {
            farmRepo.ensureStarterTemplatesSeeded()
            val templates = farmRepo.observeCropTemplates().first()
            assertEquals(6, templates.size)
            val paddy = templates.first { it.templateKey == "starter_paddy" }

            val farmId = (farmRepo.saveFarm(Farm(name = "Main Farm")) as AppResult.Success).data
            val plotId =
                (
                    farmRepo.savePlot(
                        Plot(farmId = farmId, name = "East Plot", areaMicroAcres = AreaConverter.fromAcresAndGuntas(2, 0.0)),
                    ) as AppResult.Success
                    ).data

            val cycleId =
                (
                    farmRepo.createCropCycleWithTemplate(
                        cycle =
                        CropCycle(
                            farmId = farmId,
                            plotId = plotId,
                            templateId = paddy.id,
                            cropName = "వరి",
                            season = SeasonType.KHARIF,
                            cropYear = 2026,
                            plantedAreaMicroAcres = 2_000_000L,
                            sowingDateEpochDay = 20_600L,
                        ),
                        generateTasksFromTemplate = true,
                        useTeluguTitles = true,
                    ) as AppResult.Success
                    ).data

            val tasks = farmRepo.observeTasks(cropCycleId = cycleId).first()
            assertEquals(paddy.stages.size, tasks.size)
            assertEquals(20_621L, tasks[1].dueDateEpochDay)

            val savedCycle = farmRepo.observeCropCycle(cycleId).first()!!
            farmRepo.updateCropCycle(savedCycle.copy(sowingDateEpochDay = 20_610L), recalculateUncompletedTemplateTasks = true)
            val shifted = farmRepo.observeTasks(cropCycleId = cycleId).first()
            assertEquals(20_631L, shifted[1].dueDateEpochDay)
        }

    @Test
    fun `labour attendance accrues wages without ledger entry while payment posts once`() =
        runTest {
            val farmId = (farmRepo.saveFarm(Farm(name = "Main Farm")) as AppResult.Success).data
            val workerId =
                (
                    labourRepo.saveWorker(
                        Worker(name = "రమేష్", defaultDailyWage = MoneyPaise.fromRupees(500)),
                    ) as AppResult.Success
                    ).data

            // Full day + half day = ₹750 accrued
            labourRepo.upsertAttendance(
                WorkerAttendance(
                    workerId = workerId,
                    farmId = farmId,
                    workDateEpochDay = 20_600L,
                    status = AttendanceStatus.FULL_DAY,
                    baseDailyWage = MoneyPaise.fromRupees(500),
                    wageAccrued = MoneyPaise.fromRupees(500),
                ),
            )
            labourRepo.upsertAttendance(
                WorkerAttendance(
                    workerId = workerId,
                    farmId = farmId,
                    workDateEpochDay = 20_601L,
                    status = AttendanceStatus.HALF_DAY,
                    baseDailyWage = MoneyPaise.fromRupees(500),
                    wageAccrued = MoneyPaise.fromRupees(250),
                ),
            )
            // Attendance alone must NOT post cash ledger expense
            assertEquals(0, financeRepo.observeLedgerEntries().first().size)

            // Advance ₹300 -> ledger expense ₹300, pending balance ₹450
            labourRepo.recordWorkerPayment(
                WorkerPayment(
                    workerId = workerId,
                    farmId = farmId,
                    paymentType = WorkerPaymentType.ADVANCE,
                    amount = MoneyPaise.fromRupees(300),
                    paymentDateEpochMillis = 1_000_000L,
                    paymentMode = PaymentMode.CASH,
                ),
            )
            val summary = labourRepo.observeWorkersWithBalance().first().single()
            assertEquals(MoneyPaise.fromRupees(750), summary.totalWageAccrued)
            assertEquals(MoneyPaise.fromRupees(300), summary.totalAdvancesPaid)
            assertEquals(MoneyPaise.fromRupees(450), summary.pendingBalance)
            assertEquals(MoneyPaise.fromRupees(300), financeRepo.observeSeasonTotals().first().totalExpense)
        }

    @Test
    fun `machinery timer enforces active lock survives reboot and posts one expense on stop`() =
        runTest {
            val farmId = (farmRepo.saveFarm(Farm(name = "Main Farm")) as AppResult.Success).data
            val sessionId =
                (
                    machineryRepo.startLiveSession(
                        StartMachinerySessionRequest(
                            farmId = farmId,
                            plotId = null,
                            cropCycleId = null,
                            providerContactId = null,
                            machineName = "John Deere Tractor",
                            operatorName = "Srinivas",
                            operationType = "Ploughing",
                            usageType = MachineryUsageType.HIRED_IN,
                            billingMode = MachineryBillingMode.HOURLY,
                            hourlyRate = MoneyPaise.fromRupees(900),
                            perAcreRate = null,
                            notes = null,
                        ),
                    ) as AppResult.Success
                    ).data

            // Duplicate start blocked
            val dup =
                machineryRepo.startLiveSession(
                    StartMachinerySessionRequest(
                        farmId = farmId,
                        plotId = null,
                        cropCycleId = null,
                        providerContactId = null,
                        machineName = "john deere tractor",
                        operatorName = null,
                        operationType = "Cultivator",
                        usageType = MachineryUsageType.HIRED_IN,
                        billingMode = MachineryBillingMode.HOURLY,
                        hourlyRate = MoneyPaise.fromRupees(900),
                        perAcreRate = null,
                        notes = null,
                    ),
                )
            assertTrue(dup is AppResult.Error && dup.error == DomainError.ActiveMachinerySessionExists)

            // Simulate 90 mins + device reboot (bootCount changed!)
            fakeTime.boot += 1
            fakeTime.epochMillis += 90L * 60_000L
            val stopped =
                (
                    machineryRepo.stopLiveSession(
                        sessionId = sessionId,
                        overrideElapsedMinutes = null,
                        areaCoveredMicroAcres = null,
                        flatAmount = null,
                        paidAmount = MoneyPaise.fromRupees(500),
                        paymentMode = PaymentMode.CASH,
                        notes = null,
                    ) as AppResult.Success
                    ).data

            assertEquals(MachinerySessionStatus.COMPLETED, stopped.status)
            assertEquals(90L, stopped.elapsedMinutes)
            assertEquals(MoneyPaise.fromRupees(1350), stopped.computedAmount)
            assertEquals(PaymentStatus.PARTIAL, stopped.paymentStatus)

            // Idempotent second stop produces zero duplicate ledger rows
            machineryRepo.stopLiveSession(sessionId, null, null, null, MoneyPaise.ZERO, PaymentMode.CASH, null)
            val ledger = financeRepo.observeLedgerEntries().first()
            assertEquals(1, ledger.size)
            assertEquals(MoneyPaise.fromRupees(1350), ledger.single().totalAmount)
        }

    @Test
    fun `inventory purchase updates stock and ledger atomically and blocks negative stock`() =
        runTest {
            val farmId = (farmRepo.saveFarm(Farm(name = "Main Farm")) as AppResult.Success).data
            val productId =
                (
                    inventoryRepo.saveProduct(
                        InputProduct(name = "యూరియా", category = InputCategory.FERTILIZER, unit = QuantityUnit.BAG),
                    ) as AppResult.Success
                    ).data

            inventoryRepo.recordPurchase(
                InputPurchase(
                    productId = productId,
                    farmId = farmId,
                    // 10 bags
                    quantityMilliUnits = 10_000L,
                    unit = QuantityUnit.BAG,
                    unitPrice = MoneyPaise.fromRupees(270),
                    totalAmount = MoneyPaise.fromRupees(2700),
                    paidAmount = MoneyPaise.fromRupees(2700),
                    paymentStatus = PaymentStatus.PAID,
                    paymentMode = PaymentMode.UPI,
                    purchaseDateEpochMillis = fakeTime.epochMillis,
                ),
            )
            assertEquals(10_000L, inventoryRepo.observeProduct(productId).first()!!.currentStockMilliUnits)
            assertEquals(1, financeRepo.observeLedgerEntries().first().size)

            // Negative stock blocked (trying to use 12 bags when only 10 in stock)
            val failUse =
                inventoryRepo.recordStockAdjustmentOrUsage(
                    productId = productId,
                    farmId = farmId,
                    plotId = null,
                    cropCycleId = null,
                    signedDeltaMilliUnits = -12_000L,
                    reason = StockMovementReason.FIELD_APPLICATION,
                    notes = null,
                )
            assertTrue(failUse is AppResult.Error && failUse.error is DomainError.InsufficientInventory)
            assertEquals(10_000L, inventoryRepo.observeProduct(productId).first()!!.currentStockMilliUnits)

            // Valid usage of 4 bags leaves 6 bags and adds zero duplicate ledger rows
            val okUse =
                inventoryRepo.recordStockAdjustmentOrUsage(
                    productId = productId,
                    farmId = farmId,
                    plotId = null,
                    cropCycleId = null,
                    signedDeltaMilliUnits = -4_000L,
                    reason = StockMovementReason.FIELD_APPLICATION,
                    notes = null,
                )
            assertTrue(okUse is AppResult.Success)
            assertEquals(6_000L, inventoryRepo.observeProduct(productId).first()!!.currentStockMilliUnits)
            assertEquals(1, financeRepo.observeLedgerEntries().first().size)
        }

    @Test
    fun `crop sale updates storage syncs settlement and void excludes from totals`() =
        runTest {
            val farmId = (farmRepo.saveFarm(Farm(name = "Main Farm")) as AppResult.Success).data
            val plotId =
                (
                    farmRepo.savePlot(
                        Plot(farmId = farmId, name = "Plot A", areaMicroAcres = 1_000_000L),
                    ) as AppResult.Success
                    ).data
            val cycleId =
                (
                    farmRepo.createCropCycleWithTemplate(
                        CropCycle(
                            farmId = farmId,
                            plotId = plotId,
                            cropName = "వరి",
                            season = SeasonType.KHARIF,
                            cropYear = 2026,
                            plantedAreaMicroAcres = 1_000_000L,
                            sowingDateEpochDay = 20_500L,
                        ),
                        generateTasksFromTemplate = false,
                        useTeluguTitles = true,
                    ) as AppResult.Success
                    ).data

            val storageId =
                (
                    fieldOpsRepo.saveStorageRecord(
                        StorageRecord(
                            farmId = farmId,
                            cropCycleId = cycleId,
                            storageLocation = "Home Shed",
                            quantityMilliUnits = 20_000L,
                            remainingQuantityMilliUnits = 20_000L,
                            quantityUnit = QuantityUnit.QUINTAL,
                            storedDateEpochDay = 20_620L,
                        ),
                    ) as AppResult.Success
                    ).data

            val saleId =
                (
                    financeRepo.saveCropSale(
                        CropSale(
                            farmId = farmId,
                            plotId = plotId,
                            cropCycleId = cycleId,
                            storageRecordId = storageId,
                            buyerNameSnapshot = "లక్ష్మీ ట్రేడర్స్",
                            quantityMilliUnits = 15_000L,
                            quantityUnit = QuantityUnit.QUINTAL,
                            ratePerUnit = MoneyPaise.fromRupees(2200),
                            grossAmount = MoneyPaise.fromRupees(33_000),
                            deductions = MoneyPaise.fromRupees(1000),
                            netTotalAmount = MoneyPaise.fromRupees(32_000),
                            amountReceived = MoneyPaise.fromRupees(20_000),
                            paymentStatus = PaymentStatus.PARTIAL,
                            paymentMode = PaymentMode.BANK_TRANSFER,
                            saleDateEpochMillis = fakeTime.epochMillis,
                        ),
                    ) as AppResult.Success
                    ).data

            assertEquals(5_000L, fieldOpsRepo.getStorageRecord(storageId)!!.remainingQuantityMilliUnits)
            val ledgerEntry = financeRepo.observeLedgerEntries().first().single()
            assertEquals(MoneyPaise.fromRupees(12_000), ledgerEntry.balanceDue)

            // Settle remaining ₹12,000
            financeRepo.recordLedgerSettlement(ledgerEntry.id, MoneyPaise.fromRupees(12_000), PaymentMode.UPI)
            assertEquals(PaymentStatus.PAID, financeRepo.observeCropSale(saleId).first()!!.paymentStatus)

            // Expense + void exclusion check
            financeRepo.saveManualExpense(
                ExpenseDetail(
                    farmId = farmId,
                    cropCycleId = cycleId,
                    category = LedgerCategory.DIESEL,
                    description = "Diesel",
                    totalAmount = MoneyPaise.fromRupees(2000),
                    paidAmount = MoneyPaise.fromRupees(2000),
                    paymentMode = PaymentMode.CASH,
                    expenseDateEpochMillis = fakeTime.epochMillis,
                ),
                paidAmount = MoneyPaise.fromRupees(2000),
            )
            val expLedgerId = financeRepo.observeLedgerEntries().first().first { it.title == "Diesel" }.id
            assertEquals(MoneyPaise.fromRupees(2000), financeRepo.observeSeasonTotals().first().totalExpense)
            financeRepo.voidLedgerEntry(expLedgerId, "Entered by mistake")
            assertEquals(MoneyPaise.ZERO, financeRepo.observeSeasonTotals().first().totalExpense)
        }

    @Test
    fun `ensureDefaultFarmAndPlot auto creates and reuses primary farm and plot`() =
        runTest {
            val first = farmRepo.ensureDefaultFarmAndPlot()
            val second = farmRepo.ensureDefaultFarmAndPlot()
            assertEquals(first, second)
            assertEquals(1, farmRepo.observeFarms().first().size)
            assertEquals(1, farmRepo.observePlots().first().size)
        }

    @Test
    fun `worker days counter preserves profile fields and floors at zero`() =
        runTest {
            val workerId =
                (
                    labourRepo.saveWorker(
                        Worker(
                            name = "రామయ్య",
                            photoPath = "media/photos/w1.jpg",
                            voiceNamePath = "media/audio/w1.m4a",
                            defaultDailyWage = MoneyPaise.fromRupees(400),
                        ),
                    ) as AppResult.Success
                    ).data

            labourRepo.adjustWorkerDaysTenths(workerId, 10)
            labourRepo.adjustWorkerDaysTenths(workerId, 5)
            val summary = labourRepo.observeWorkersWithBalance().first().single()
            assertEquals(15, summary.totalDaysWorkedTenths)
            assertEquals(MoneyPaise.fromRupees(600), summary.totalWageAccrued)
            assertEquals("media/photos/w1.jpg", summary.worker.photoPath)
            assertEquals("media/audio/w1.m4a", summary.worker.voiceNamePath)

            // Updating profile name keeps daysWorkedTenths intact
            labourRepo.saveWorker(summary.worker.copy(name = "రామయ్య గారు"))
            assertEquals(15, labourRepo.observeWorker(workerId).first()!!.daysWorkedTenths)

            // Decrement floors at zero
            labourRepo.adjustWorkerDaysTenths(workerId, -50)
            assertEquals(0, labourRepo.observeWorker(workerId).first()!!.daysWorkedTenths)
        }

    @Test
    fun `migration 1 to 2 adds new columns cleanly to v1 schema`() {
        val dbFile = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("mig_v1_v2_test.db")
        dbFile.delete()
        val sqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqlite.execSQL(
            """
            CREATE TABLE workers (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                farmId INTEGER,
                name TEXT NOT NULL,
                phone TEXT,
                skillRole TEXT,
                photoPath TEXT,
                defaultDailyWagePaise INTEGER NOT NULL,
                defaultHalfDayPercent INTEGER NOT NULL,
                notes TEXT,
                isArchived INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            CREATE TABLE farms (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                villageOrLocation TEXT,
                surveyNumber TEXT,
                totalAreaMicroAcres INTEGER NOT NULL,
                preferredAreaUnit TEXT NOT NULL,
                soilType TEXT,
                waterSource TEXT,
                latitude REAL,
                longitude REAL,
                photoPath TEXT,
                notes TEXT,
                isPrimary INTEGER NOT NULL,
                isArchived INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            CREATE TABLE crop_cycles (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                farmId INTEGER NOT NULL,
                plotId INTEGER NOT NULL,
                templateId INTEGER,
                cropName TEXT NOT NULL,
                variety TEXT,
                season TEXT NOT NULL,
                cropYear INTEGER NOT NULL,
                plantedAreaMicroAcres INTEGER NOT NULL,
                sowingDateEpochDay INTEGER NOT NULL,
                expectedHarvestEpochDay INTEGER,
                actualHarvestEpochDay INTEGER,
                status TEXT NOT NULL,
                isIntercrop INTEGER NOT NULL,
                notes TEXT,
                isArchived INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            CREATE TABLE contacts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                phone TEXT,
                contactType TEXT NOT NULL,
                villageOrMandi TEXT,
                upiId TEXT,
                notes TEXT,
                isArchived INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        seedWorkerAndVerifyV2Migration(sqlite, dbFile)
    }

    private fun seedWorkerAndVerifyV2Migration(
        sqlite: android.database.sqlite.SQLiteDatabase,
        dbFile: java.io.File,
    ) {
        sqlite.execSQL(
            """
            CREATE TABLE irrigation_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                farmId INTEGER NOT NULL,
                plotId INTEGER NOT NULL,
                cropCycleId INTEGER,
                irrigationDateEpochMillis INTEGER NOT NULL,
                durationMinutes INTEGER,
                source TEXT NOT NULL,
                method TEXT NOT NULL,
                nextWateringEpochDay INTEGER,
                motorHoursOrFuelNote TEXT,
                notes TEXT,
                isArchived INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            INSERT INTO workers (
                name, defaultDailyWagePaise, defaultHalfDayPercent,
                isArchived, createdAtEpochMillis, updatedAtEpochMillis
            ) VALUES ('Raju', 40000, 50, 0, 1000, 1000)
            """.trimIndent(),
        )
        sqlite.execSQL("PRAGMA user_version = 1")
        sqlite.close()

        val upgraded =
            androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
                    .name("mig_v1_v2_test.db")
                    .callback(
                        object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                                error("Expected onUpgrade(1 -> 2), not onCreate")
                            }

                            override fun onUpgrade(
                                db: androidx.sqlite.db.SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) {
                                FarmSaathiDatabase.MIGRATION_1_2.migrate(db)
                            }
                        },
                    )
                    .build(),
            ).writableDatabase
        upgraded.query("SELECT name, daysWorkedTenths, voiceNamePath FROM workers").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Raju", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertTrue(cursor.isNull(2))
        }
        upgraded.close()
        dbFile.delete()
    }

    private class FakeTimeProvider : TimeProvider {
        var epochMillis: Long = 1_794_326_000_000L
        var elapsedMillis: Long = 100_000L
        var boot: Int = 1

        override fun nowEpochMillis(): Long = epochMillis

        override fun todayEpochDay(): Long = 20_650L

        override fun elapsedRealtimeMillis(): Long = elapsedMillis

        override fun bootCount(): Int = boot

        override fun epochMillisToEpochDay(epochMillis: Long): Long = 20_650L

        override fun epochDayToStartMillis(epochDay: Long): Long = epochMillis

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
}
