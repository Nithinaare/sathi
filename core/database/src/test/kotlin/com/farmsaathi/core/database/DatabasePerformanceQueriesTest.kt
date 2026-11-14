package com.farmsaathi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.database.entity.LedgerEntryEntity
import com.farmsaathi.core.database.repository.RoomFarmRepository
import com.farmsaathi.core.database.repository.RoomFinanceRepository
import com.farmsaathi.core.database.repository.RoomInventoryRepository
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.core.model.InputProduct
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.LedgerSourceType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.StockMovementReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabasePerformanceQueriesTest {
    private lateinit var db: FarmSaathiDatabase
    private lateinit var fakeTime: FakeTimeProvider
    private lateinit var farmRepo: RoomFarmRepository
    private lateinit var financeRepo: RoomFinanceRepository
    private lateinit var inventoryRepo: RoomInventoryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, FarmSaathiDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        fakeTime = FakeTimeProvider()
        val coordinator = DatabaseMaintenanceCoordinator()
        farmRepo = RoomFarmRepository(db.farmDao(), db.plotDao(), db.cropDao(), fakeTime, coordinator)
        financeRepo = RoomFinanceRepository(db.financeDao(), db.cropDao(), db.fieldOpsDao(), fakeTime, coordinator)
        inventoryRepo = RoomInventoryRepository(db.inventoryDao(), fakeTime, coordinator)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `observeSeasonTotals SQL projection sums expenses income and non-negative dues excluding voided and archived`() =
        runTest {
            val farm1Id = (farmRepo.saveFarm(Farm(name = "Farm One")) as AppResult.Success).data
            val farm2Id = (farmRepo.saveFarm(Farm(name = "Farm Two")) as AppResult.Success).data

            val emptyTotals = financeRepo.observeSeasonTotals(farm1Id).first()
            assertEquals(MoneyPaise.ZERO, emptyTotals.totalExpense)
            assertEquals(MoneyPaise.ZERO, emptyTotals.totalIncome)
            assertEquals(MoneyPaise.ZERO, emptyTotals.pendingPayable)
            assertEquals(MoneyPaise.ZERO, emptyTotals.pendingReceivable)

            val financeDao = db.financeDao()
            // Farm 1 Expense: 10000 total, 3500 settled -> 6500 payable due
            financeDao.insertLedgerEntry(
                testLedgerEntity(
                    farmId = farm1Id,
                    entryType = LedgerEntryType.EXPENSE,
                    totalPaise = 10_000L,
                    settledPaise = 3_500L,
                ),
            )
            // Farm 1 Expense (over-settled): 2000 total, 2500 settled -> due clamped to 0
            financeDao.insertLedgerEntry(
                testLedgerEntity(
                    farmId = farm1Id,
                    entryType = LedgerEntryType.EXPENSE,
                    totalPaise = 2_000L,
                    settledPaise = 2_500L,
                ),
            )
            // Farm 1 Income: 50000 total, 20000 settled -> 30000 receivable due
            financeDao.insertLedgerEntry(
                testLedgerEntity(
                    farmId = farm1Id,
                    entryType = LedgerEntryType.INCOME,
                    totalPaise = 50_000L,
                    settledPaise = 20_000L,
                ),
            )
            // Farm 1 Voided Expense (must be excluded)
            financeDao.insertLedgerEntry(
                testLedgerEntity(
                    farmId = farm1Id,
                    entryType = LedgerEntryType.EXPENSE,
                    totalPaise = 99_999L,
                    settledPaise = 0L,
                    isVoided = true,
                ),
            )
            // Farm 1 Archived Income (must be excluded)
            financeDao.insertLedgerEntry(
                testLedgerEntity(
                    farmId = farm1Id,
                    entryType = LedgerEntryType.INCOME,
                    totalPaise = 88_888L,
                    settledPaise = 0L,
                    isArchived = true,
                ),
            )
            // Farm 2 Income: 15000 total, 5000 settled -> 10000 receivable due
            financeDao.insertLedgerEntry(
                testLedgerEntity(
                    farmId = farm2Id,
                    entryType = LedgerEntryType.INCOME,
                    totalPaise = 15_000L,
                    settledPaise = 5_000L,
                ),
            )

            val farm1Totals = financeRepo.observeSeasonTotals(farm1Id).first()
            assertEquals(MoneyPaise(12_000L), farm1Totals.totalExpense)
            assertEquals(MoneyPaise(50_000L), farm1Totals.totalIncome)
            assertEquals(MoneyPaise(6_500L), farm1Totals.pendingPayable)
            assertEquals(MoneyPaise(30_000L), farm1Totals.pendingReceivable)

            val allFarmsTotals = financeRepo.observeSeasonTotals(farmId = null).first()
            assertEquals(MoneyPaise(12_000L), allFarmsTotals.totalExpense)
            assertEquals(MoneyPaise(65_000L), allFarmsTotals.totalIncome)
            assertEquals(MoneyPaise(6_500L), allFarmsTotals.pendingPayable)
            assertEquals(MoneyPaise(40_000L), allFarmsTotals.pendingReceivable)
        }

    @Test
    fun `observeAllMovements returns stock movements across products ordered newest first`() =
        runTest {
            val farmId = (farmRepo.saveFarm(Farm(name = "Stock Farm")) as AppResult.Success).data
            val ureaId =
                (
                    inventoryRepo.saveProduct(
                        InputProduct(
                            farmId = farmId,
                            name = "Urea",
                            category = InputCategory.FERTILIZER,
                            unit = QuantityUnit.KG,
                            currentStockMilliUnits = 50_000L,
                        ),
                    ) as AppResult.Success
                    ).data
            val seedId =
                (
                    inventoryRepo.saveProduct(
                        InputProduct(
                            farmId = farmId,
                            name = "Paddy Seed",
                            category = InputCategory.SEED,
                            unit = QuantityUnit.KG,
                            currentStockMilliUnits = 30_000L,
                        ),
                    ) as AppResult.Success
                    ).data

            fakeTime.epochMillis = 1_000_000L
            val firstMoveId =
                (
                    inventoryRepo.recordStockAdjustmentOrUsage(
                        productId = ureaId,
                        farmId = farmId,
                        plotId = null,
                        cropCycleId = null,
                        signedDeltaMilliUnits = -5_000L,
                        reason = StockMovementReason.FIELD_APPLICATION,
                        notes = "First usage",
                    ) as AppResult.Success
                    ).data

            fakeTime.epochMillis = 2_000_000L
            val secondMoveId =
                (
                    inventoryRepo.recordStockAdjustmentOrUsage(
                        productId = seedId,
                        farmId = farmId,
                        plotId = null,
                        cropCycleId = null,
                        signedDeltaMilliUnits = -10_000L,
                        reason = StockMovementReason.FIELD_APPLICATION,
                        notes = "Second usage",
                    ) as AppResult.Success
                    ).data

            fakeTime.epochMillis = 2_000_000L
            val thirdMoveId =
                (
                    inventoryRepo.recordStockAdjustmentOrUsage(
                        productId = ureaId,
                        farmId = farmId,
                        plotId = null,
                        cropCycleId = null,
                        signedDeltaMilliUnits = 2_000L,
                        reason = StockMovementReason.STOCK_COUNT_CORRECTION,
                        notes = "Third tiebreak usage",
                    ) as AppResult.Success
                    ).data

            val allMovements = inventoryRepo.observeAllMovements().first()
            assertEquals(listOf(thirdMoveId, secondMoveId, firstMoveId), allMovements.map { it.id })
        }

    private fun testLedgerEntity(
        farmId: Long,
        entryType: LedgerEntryType,
        totalPaise: Long,
        settledPaise: Long,
        isVoided: Boolean = false,
        isArchived: Boolean = false,
    ): LedgerEntryEntity =
        LedgerEntryEntity(
            entryType = entryType,
            sourceType =
            if (entryType == LedgerEntryType.EXPENSE) {
                LedgerSourceType.MANUAL_EXPENSE
            } else {
                LedgerSourceType.MANUAL_INCOME
            },
            sourceId = null,
            farmId = farmId,
            category =
            if (entryType == LedgerEntryType.EXPENSE) {
                LedgerCategory.OTHER_EXPENSE
            } else {
                LedgerCategory.OTHER_INCOME
            },
            title = "Test Entry",
            totalAmountPaise = totalPaise,
            settledAmountPaise = settledPaise,
            paymentStatus = PaymentStatus.PARTIAL,
            paymentMode = PaymentMode.CASH,
            transactionDateEpochMillis = fakeTime.epochMillis,
            isVoided = isVoided,
            isArchived = isArchived,
            createdAtEpochMillis = fakeTime.epochMillis,
            updatedAtEpochMillis = fakeTime.epochMillis,
        )

    private class FakeTimeProvider : TimeProvider {
        var epochMillis: Long = 1_794_326_000_000L

        override fun nowEpochMillis(): Long = epochMillis

        override fun todayEpochDay(): Long = 20_650L

        override fun elapsedRealtimeMillis(): Long = 100_000L

        override fun bootCount(): Int = 1

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
