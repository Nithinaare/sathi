package com.farmsaathi.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialCalculatorsTest {
    @Test
    fun `derivePaymentStatus reflects paid partial and pending accurately`() {
        val total = MoneyPaise.fromRupees(1000)
        assertEquals(PaymentStatus.PENDING, FinancialCalculators.derivePaymentStatus(total, MoneyPaise.ZERO))
        assertEquals(PaymentStatus.PARTIAL, FinancialCalculators.derivePaymentStatus(total, MoneyPaise.fromRupees(400)))
        assertEquals(PaymentStatus.PAID, FinancialCalculators.derivePaymentStatus(total, total))
    }

    @Test
    fun `labour wage calculator computes full half absent and piece rate`() {
        val daily = MoneyPaise.fromRupees(450)
        assertEquals(daily, FinancialCalculators.calculateWageAccrued(AttendanceStatus.FULL_DAY, daily))
        assertEquals(
            MoneyPaise.fromRupees(225),
            FinancialCalculators.calculateWageAccrued(AttendanceStatus.HALF_DAY, daily, halfDayPercent = 50),
        )
        assertEquals(
            MoneyPaise.ZERO,
            FinancialCalculators.calculateWageAccrued(AttendanceStatus.ABSENT, daily),
        )
        // 3.5 bags @ ₹120/bag = ₹420
        assertEquals(
            MoneyPaise.fromRupees(420),
            FinancialCalculators.calculateWageAccrued(
                status = AttendanceStatus.PIECE_RATE,
                baseDailyWage = daily,
                pieceUnitRate = MoneyPaise.fromRupees(120),
                pieceQuantityMilliUnits = 3_500L,
            ),
        )
    }

    @Test
    fun `machinery elapsed falls back to epoch time across device reboot`() {
        // Same boot uses monotonic elapsedRealtime even if wall clock jumped
        val sameBoot =
            FinancialCalculators.computeMachineryElapsedMillis(
                startEpochMillis = 1_000_000L,
                startElapsedRealtimeMillis = 50_000L,
                startBootCount = 3,
                nowEpochMillis = 9_999_999L,
                nowElapsedRealtimeMillis = 50_000L + 3_600_000L,
                nowBootCount = 3,
            )
        assertEquals(3_600_000L, sameBoot)

        // Rebooted device (bootCount changed and elapsedRealtime reset) uses wall-clock epoch delta
        val afterReboot =
            FinancialCalculators.computeMachineryElapsedMillis(
                startEpochMillis = 1_000_000L,
                startElapsedRealtimeMillis = 50_000L,
                startBootCount = 3,
                nowEpochMillis = 4_600_000L,
                nowElapsedRealtimeMillis = 12_000L,
                nowBootCount = 4,
            )
        assertEquals(3_600_000L, afterReboot)
    }

    @Test
    fun `machinery per acre charge computes exact gunta billing`() {
        // 1 acre 20 guntas = 1.5 acres @ ₹2000/acre = ₹3000
        val area = AreaConverter.fromAcresAndGuntas(1, 20.0)
        val charge =
            FinancialCalculators.computeMachineryCharge(
                billingMode = MachineryBillingMode.PER_ACRE,
                perAcreRate = MoneyPaise.fromRupees(2000),
                areaCoveredMicroAcres = area,
            )
        assertEquals(MoneyPaise.fromRupees(3000), charge)
    }

    @Test
    fun `crop profitability marks partial until closed and settled`() {
        val activeCycle =
            CropCycle(
                id = 1L,
                farmId = 1L,
                plotId = 1L,
                cropName = "వరి",
                season = SeasonType.KHARIF,
                cropYear = 2026,
                plantedAreaMicroAcres = 1_000_000L,
                sowingDateEpochDay = 20_600L,
                status = CropCycleStatus.ACTIVE,
            )
        val entries =
            listOf(
                LedgerEntry(
                    id = 1L,
                    entryType = LedgerEntryType.EXPENSE,
                    sourceType = LedgerSourceType.MANUAL_EXPENSE,
                    sourceId = 1L,
                    farmId = 1L,
                    cropCycleId = 1L,
                    category = LedgerCategory.SEEDS,
                    title = "Seeds",
                    totalAmount = MoneyPaise.fromRupees(2000),
                    settledAmount = MoneyPaise.fromRupees(2000),
                    paymentStatus = PaymentStatus.PAID,
                    paymentMode = PaymentMode.CASH,
                    transactionDateEpochMillis = 1000L,
                ),
                LedgerEntry(
                    id = 2L,
                    entryType = LedgerEntryType.INCOME,
                    sourceType = LedgerSourceType.CROP_SALE,
                    sourceId = 1L,
                    farmId = 1L,
                    cropCycleId = 1L,
                    category = LedgerCategory.CROP_SALE,
                    title = "Paddy Sale",
                    totalAmount = MoneyPaise.fromRupees(10_000),
                    settledAmount = MoneyPaise.fromRupees(10_000),
                    paymentStatus = PaymentStatus.PAID,
                    paymentMode = PaymentMode.UPI,
                    transactionDateEpochMillis = 2000L,
                ),
            )
        val activeSummary = CropProfitabilitySummary.compute(activeCycle, entries, hasHarvestRecord = true)
        assertEquals(MoneyPaise.fromRupees(8000), activeSummary.knownBalance)
        assertTrue(activeSummary.isCalculationPartial)

        val closedSummary =
            CropProfitabilitySummary.compute(
                activeCycle.copy(status = CropCycleStatus.HARVESTED),
                entries,
                hasHarvestRecord = true,
            )
        assertFalse(closedSummary.isCalculationPartial)
    }

    @Test
    fun `quick group wage calculator and manual days counter compute half day and net pay`() {
        // 1.5 days (15 tenths) @ ₹400 = ₹600
        assertEquals(
            MoneyPaise.fromRupees(600),
            FinancialCalculators.calculateManualDaysWage(
                daysWorkedTenths = 15,
                dailyRate = MoneyPaise.fromRupees(400),
            ),
        )
        // 4 workers × 2.5 days (25 tenths) @ ₹400 − ₹500 advance = ₹4000 gross, ₹3500 net
        val calc =
            FinancialCalculators.calculateQuickGroupWage(
                workerCount = 4,
                daysWorkedTenths = 25,
                dailyRate = MoneyPaise.fromRupees(400),
                advancePaid = MoneyPaise.fromRupees(500),
            )
        assertEquals(MoneyPaise.fromRupees(4000), calc.grossWage)
        assertEquals(MoneyPaise.fromRupees(3500), calc.netPayable)

        // Advance exceeding gross floors net at zero
        val capped =
            FinancialCalculators.calculateQuickGroupWage(
                workerCount = 1,
                daysWorkedTenths = 10,
                dailyRate = MoneyPaise.fromRupees(300),
                advancePaid = MoneyPaise.fromRupees(500),
            )
        assertEquals(MoneyPaise.ZERO, capped.netPayable)
    }

    @Test
    fun `worker halfDayRatePaise overrides percentage when set and editable crop template converts`() {
        val customWorker =
            Worker(
                name = "Ravi",
                defaultDailyWage = MoneyPaise.fromRupees(500),
                defaultHalfDayPercent = 50,
                halfDayRatePaise = 30_000L,
            )
        assertEquals(MoneyPaise.fromRupees(300), customWorker.effectiveHalfDayWage)
        assertEquals(
            MoneyPaise.fromRupees(300),
            FinancialCalculators.calculateWageAccrued(
                status = AttendanceStatus.HALF_DAY,
                baseDailyWage = customWorker.defaultDailyWage,
                halfDayPercent = customWorker.defaultHalfDayPercent,
                halfDayRateOverride = MoneyPaise(30_000L),
            ),
        )

        val editable =
            EditableCropTemplate(
                cropNameTe = "టమాటా",
                cropNameEn = "Tomato",
                stages =
                listOf(
                    CropTemplateStage(
                        stageKey = "t_sow",
                        titleTe = "నాటు",
                        titleEn = "Transplant",
                        offsetDaysFromSowing = 0,
                        category = TaskCategory.SOWING,
                    ),
                ),
            )
        val domain = editable.toCropTemplate()
        assertEquals("Tomato", domain.cropNameEn)
        assertEquals(editable, domain.toEditable())
        assertTrue(StockMovementReason.PURCHASE.isStockIn)
        assertTrue(StockMovementReason.FIELD_APPLICATION.isStockOut)
    }
}
