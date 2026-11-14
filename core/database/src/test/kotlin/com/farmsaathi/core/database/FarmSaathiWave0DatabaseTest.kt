package com.farmsaathi.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.BatchAttendanceItem
import com.farmsaathi.core.database.repository.RoomFarmRepository
import com.farmsaathi.core.database.repository.RoomFieldOpsRepository
import com.farmsaathi.core.database.repository.RoomInventoryRepository
import com.farmsaathi.core.database.repository.RoomLabourRepository
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AttendanceStatus
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropStageTemplate
import com.farmsaathi.core.model.HarvestRecord
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.core.model.InputProduct
import com.farmsaathi.core.model.IrrigationLog
import com.farmsaathi.core.model.IrrigationMethod
import com.farmsaathi.core.model.IrrigationSource
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.SeasonType
import com.farmsaathi.core.model.StarterCropTemplates
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.core.model.TaskCategory
import com.farmsaathi.core.model.Worker
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
class FarmSaathiWave0DatabaseTest {
    private lateinit var db: FarmSaathiDatabase
    private lateinit var fakeTime: FakeTimeProvider
    private lateinit var coordinator: DatabaseMaintenanceCoordinator
    private lateinit var farmRepo: RoomFarmRepository
    private lateinit var labourRepo: RoomLabourRepository
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
        labourRepo = RoomLabourRepository(db.labourDao(), fakeTime, coordinator)
        inventoryRepo = RoomInventoryRepository(db.inventoryDao(), fakeTime, coordinator)
        fieldOpsRepo = RoomFieldOpsRepository(db.fieldOpsDao(), fakeTime, coordinator, db.cropDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `migration 2 to 3 adds halfDayRatePaise photoPath columns and preserves data`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("mig_v2_v3_test.db")
        dbFile.delete()
        createV2SeededDatabase(dbFile)

        val upgraded =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name("mig_v2_v3_test.db")
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(3) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                error("Expected onUpgrade(2 -> 3), not onCreate")
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) {
                                FarmSaathiDatabase.MIGRATION_2_3.migrate(db)
                            }
                        },
                    )
                    .build(),
            ).writableDatabase

        upgraded.query("SELECT name, halfDayRatePaise FROM workers").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Sita", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }
        assertTrue("photoPath" in tableColumns(upgraded, "harvest_records"))
        assertTrue("photoPath" in tableColumns(upgraded, "stock_movements"))
        upgraded.query("SELECT name FROM sqlite_master WHERE type='table' AND name='crop_templates'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        upgraded.close()
        dbFile.delete()
    }

    @Test
    fun `crop templates auto seed upsert reset defaults and apply to crop cycle`() =
        runTest {
            val seeded = farmRepo.observeCropTemplates().first()
            assertEquals(StarterCropTemplates.all().size, seeded.size)

            val paddy = seeded.first { it.templateKey == "starter_paddy" }
            val edited =
                paddy.copy(
                    cropNameTe = "వరి (సవరించినది)",
                    stages =
                    paddy.stages +
                        CropStageTemplate(
                            stageKey = "paddy_extra",
                            titleTe = "అదనపు పరీక్ష",
                            titleEn = "Extra Check",
                            offsetDaysFromSowing = 50,
                            category = TaskCategory.OTHER,
                        ),
                )
            farmRepo.upsertCropTemplate(edited)
            val afterEdit = farmRepo.getCropTemplate(paddy.id)!!
            assertEquals("వరి (సవరించినది)", afterEdit.cropNameTe)
            assertEquals(paddy.stages.size + 1, afterEdit.stages.size)

            val (farmId, plotId) = farmRepo.ensureDefaultFarmAndPlot()
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
                            sowingDateEpochDay = 20_600L,
                        ),
                        generateTasksFromTemplate = false,
                        useTeluguTitles = true,
                    ) as AppResult.Success
                    ).data

            val appliedCount = (farmRepo.applyTemplateToCrop(cycleId, paddy.id, useTeluguTitles = true) as AppResult.Success).data
            assertEquals(afterEdit.stages.size, appliedCount)
            assertEquals(paddy.id, farmRepo.observeCropCycle(cycleId).first()!!.templateId)

            farmRepo.resetDefaultTemplates()
            val resetPaddy = farmRepo.getCropTemplate(paddy.id)!!
            assertEquals("వరి", resetPaddy.cropNameTe)
            assertEquals(paddy.stages.size, resetPaddy.stages.size)
        }

    @Test
    fun `labour batch attendance halfDayRatePaise and repeatYesterdayAttendance work cleanly`() =
        runTest {
            val (farmId, _) = farmRepo.ensureDefaultFarmAndPlot()
            val w1 =
                (
                    labourRepo.saveWorker(
                        Worker(
                            farmId = farmId,
                            name = "లక్ష్మి",
                            defaultDailyWage = MoneyPaise.fromRupees(500),
                            halfDayRatePaise = 30_000L,
                        ),
                    ) as AppResult.Success
                    ).data
            val w2 =
                (
                    labourRepo.saveWorker(
                        Worker(
                            farmId = farmId,
                            name = "కృష్ణ",
                            defaultDailyWage = MoneyPaise.fromRupees(400),
                        ),
                    ) as AppResult.Success
                    ).data

            assertEquals(30_000L, labourRepo.observeWorker(w1).first()!!.halfDayRatePaise)

            val day1 = 20_650L
            val batchCount =
                (
                    labourRepo.markBatchAttendance(
                        farmId = farmId,
                        workDateEpochDay = day1,
                        items =
                        listOf(
                            BatchAttendanceItem(w1, AttendanceStatus.HALF_DAY),
                            BatchAttendanceItem(w2, AttendanceStatus.FULL_DAY),
                        ),
                    ) as AppResult.Success
                    ).data
            assertEquals(2, batchCount)

            val day1Att = labourRepo.observeAttendanceForDay(day1).first()
            assertEquals(MoneyPaise.fromRupees(300), day1Att.first { it.workerId == w1 }.wageAccrued)
            assertEquals(MoneyPaise.fromRupees(400), day1Att.first { it.workerId == w2 }.wageAccrued)

            val day2 = 20_651L
            val repeatedCount = (labourRepo.repeatYesterdayAttendance(day2, farmId) as AppResult.Success).data
            assertEquals(2, repeatedCount)
            val day2Att = labourRepo.observeAttendanceForDay(day2).first()
            assertEquals(2, day2Att.size)
            assertEquals(MoneyPaise.fromRupees(300), day2Att.first { it.workerId == w1 }.wageAccrued)
        }

    @Test
    fun `harvest photo stock movement photo and irrigation nextWatering reminder persist`() =
        runTest {
            val (farmId, plotId) = farmRepo.ensureDefaultFarmAndPlot()
            val cycleId =
                (
                    farmRepo.createCropCycleWithTemplate(
                        CropCycle(
                            farmId = farmId,
                            plotId = plotId,
                            cropName = "మిర్చి",
                            season = SeasonType.KHARIF,
                            cropYear = 2026,
                            plantedAreaMicroAcres = 1_000_000L,
                            sowingDateEpochDay = 20_600L,
                        ),
                        generateTasksFromTemplate = false,
                        useTeluguTitles = true,
                    ) as AppResult.Success
                    ).data

            val harvestId =
                (
                    fieldOpsRepo.saveHarvestRecord(
                        HarvestRecord(
                            farmId = farmId,
                            plotId = plotId,
                            cropCycleId = cycleId,
                            harvestDateEpochDay = 20_700L,
                            quantityMilliUnits = 5_000L,
                            quantityUnit = QuantityUnit.QUINTAL,
                            photoPath = "media/photos/weigh_slip_1.jpg",
                        ),
                    ) as AppResult.Success
                    ).data
            assertEquals("media/photos/weigh_slip_1.jpg", fieldOpsRepo.getHarvestRecord(harvestId)!!.photoPath)

            val productId =
                (
                    inventoryRepo.saveProduct(
                        InputProduct(
                            name = "Neem Oil",
                            category = InputCategory.PESTICIDE,
                            unit = QuantityUnit.LITRE,
                            currentStockMilliUnits = 5_000L,
                        ),
                    ) as AppResult.Success
                    ).data
            inventoryRepo.recordStockAdjustmentOrUsage(
                productId = productId,
                farmId = farmId,
                plotId = plotId,
                cropCycleId = cycleId,
                signedDeltaMilliUnits = -1_000L,
                reason = StockMovementReason.FIELD_APPLICATION,
                notes = "Spray",
                photoPath = "media/photos/spray_bottle.jpg",
            )
            val movements = inventoryRepo.observeMovements(productId).first()
            assertEquals("media/photos/spray_bottle.jpg", movements.single().photoPath)

            fieldOpsRepo.saveIrrigationLog(
                IrrigationLog(
                    farmId = farmId,
                    plotId = plotId,
                    cropCycleId = cycleId,
                    irrigationDateEpochMillis = fakeTime.epochMillis,
                    source = IrrigationSource.BOREWELL,
                    method = IrrigationMethod.DRIP,
                    nextWateringEpochDay = 20_655L,
                ),
            )
            val tasks = farmRepo.observeTasks(cropCycleId = cycleId).first()
            assertTrue(tasks.any { it.category == TaskCategory.IRRIGATION && it.dueDateEpochDay == 20_655L })
        }

    private fun createV2SeededDatabase(dbFile: java.io.File) {
        val sqlite = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqlite.execSQL(
            """
            CREATE TABLE workers (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                farmId INTEGER,
                name TEXT NOT NULL,
                phone TEXT,
                skillRole TEXT,
                photoPath TEXT,
                voiceNamePath TEXT,
                daysWorkedTenths INTEGER NOT NULL,
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
            CREATE TABLE harvest_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                farmId INTEGER NOT NULL,
                plotId INTEGER NOT NULL,
                cropCycleId INTEGER NOT NULL,
                harvestDateEpochDay INTEGER NOT NULL,
                quantityMilliUnits INTEGER NOT NULL,
                quantityUnit TEXT NOT NULL,
                bagCount INTEGER,
                moisturePercentTenths INTEGER,
                qualityGrade TEXT,
                notes TEXT,
                isArchived INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            CREATE TABLE stock_movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productId INTEGER NOT NULL,
                farmId INTEGER NOT NULL,
                plotId INTEGER,
                cropCycleId INTEGER,
                purchaseId INTEGER,
                movementType TEXT NOT NULL,
                reason TEXT NOT NULL,
                signedDeltaMilliUnits INTEGER NOT NULL,
                balanceAfterMilliUnits INTEGER NOT NULL,
                unitCostPaiseSnapshot INTEGER,
                movementDateEpochMillis INTEGER NOT NULL,
                notes TEXT,
                createdAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            INSERT INTO workers (
                name, daysWorkedTenths, defaultDailyWagePaise, defaultHalfDayPercent,
                isArchived, createdAtEpochMillis, updatedAtEpochMillis
            ) VALUES ('Sita', 20, 45000, 50, 0, 1000, 1000)
            """.trimIndent(),
        )
        sqlite.execSQL("PRAGMA user_version = 2")
        sqlite.close()
    }

    private fun tableColumns(
        db: SupportSQLiteDatabase,
        table: String,
    ): List<String> =
        db.query("PRAGMA table_info($table)").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) cols += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            cols
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

        override fun formatTime(epochMillis: Long): String = "09:30\u00A0AM"
    }
}
