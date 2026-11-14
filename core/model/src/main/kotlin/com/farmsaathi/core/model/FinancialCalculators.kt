package com.farmsaathi.core.model

/**
 * Pure domain calculation services shared across use cases, repositories, and reporting.
 * Never duplicate these formulas inside ViewModels or Compose screens.
 */
object FinancialCalculators {
    fun derivePaymentStatus(
        totalAmount: MoneyPaise,
        settledAmount: MoneyPaise,
    ): PaymentStatus {
        require(!totalAmount.isNegative()) { "Total amount cannot be negative" }
        require(!settledAmount.isNegative()) { "Settled amount cannot be negative" }
        require(settledAmount <= totalAmount) { "Settled amount cannot exceed total amount" }
        return when {
            totalAmount.isZero() || settledAmount == totalAmount -> PaymentStatus.PAID
            settledAmount.isZero() -> PaymentStatus.PENDING
            else -> PaymentStatus.PARTIAL
        }
    }

    fun calculateWageAccrued(
        status: AttendanceStatus,
        baseDailyWage: MoneyPaise,
        halfDayPercent: Int = 50,
        pieceUnitRate: MoneyPaise? = null,
        pieceQuantityMilliUnits: Long? = null,
        halfDayRateOverride: MoneyPaise? = null,
    ): MoneyPaise {
        require(!baseDailyWage.isNegative()) { "Base daily wage cannot be negative" }
        return when (status) {
            AttendanceStatus.FULL_DAY -> baseDailyWage
            AttendanceStatus.HALF_DAY ->
                halfDayRateOverride?.also { require(!it.isNegative()) { "Half-day rate cannot be negative" } }
                    ?: baseDailyWage.percentage(halfDayPercent)
            AttendanceStatus.ABSENT -> MoneyPaise.ZERO
            AttendanceStatus.PIECE_RATE -> {
                val rate = pieceUnitRate ?: MoneyPaise.ZERO
                val qty = pieceQuantityMilliUnits ?: 0L
                require(!rate.isNegative() && qty >= 0L) { "Piece rate/qty cannot be negative" }
                MoneyPaise.multiplyMilliUnits(rate, qty)
            }
        }
    }

    fun computeMachineryElapsedMillis(
        startEpochMillis: Long,
        startElapsedRealtimeMillis: Long?,
        startBootCount: Int?,
        nowEpochMillis: Long,
        nowElapsedRealtimeMillis: Long,
        nowBootCount: Int,
    ): Long {
        val canUseMonotonicClock =
            startElapsedRealtimeMillis != null &&
                startBootCount != null &&
                startBootCount >= 0 &&
                nowBootCount == startBootCount &&
                nowElapsedRealtimeMillis >= startElapsedRealtimeMillis
        return if (canUseMonotonicClock) {
            nowElapsedRealtimeMillis - startElapsedRealtimeMillis!!
        } else {
            maxOf(0L, nowEpochMillis - startEpochMillis)
        }
    }

    fun computeMachineryCharge(
        billingMode: MachineryBillingMode,
        elapsedMinutes: Long = 0L,
        hourlyRate: MoneyPaise = MoneyPaise.ZERO,
        perAcreRate: MoneyPaise = MoneyPaise.ZERO,
        areaCoveredMicroAcres: Long = 0L,
        flatAmount: MoneyPaise = MoneyPaise.ZERO,
    ): MoneyPaise =
        when (billingMode) {
            MachineryBillingMode.HOURLY ->
                MoneyPaise.prorateMinutesRoundedToRupee(elapsedMinutes, hourlyRate)

            MachineryBillingMode.PER_ACRE -> {
                if (areaCoveredMicroAcres <= 0L || perAcreRate.paise <= 0L) {
                    MoneyPaise.ZERO
                } else {
                    MoneyPaise(
                        MathRounding.divideAndRoundHalfUp(
                            Math.multiplyExact(perAcreRate.paise, areaCoveredMicroAcres),
                            AreaConverter.MICRO_ACRES_PER_ACRE,
                        ),
                    )
                }
            }

            MachineryBillingMode.FIXED_FLAT -> flatAmount
        }

    fun calculateManualDaysWage(
        daysWorkedTenths: Int,
        dailyRate: MoneyPaise,
    ): MoneyPaise =
        calculateQuickGroupWage(
            workerCount = 1,
            daysWorkedTenths = daysWorkedTenths,
            dailyRate = dailyRate,
            advancePaid = MoneyPaise.ZERO,
        ).grossWage

    fun calculateQuickGroupWage(
        workerCount: Int,
        daysWorkedTenths: Int,
        dailyRate: MoneyPaise,
        advancePaid: MoneyPaise = MoneyPaise.ZERO,
    ): QuickWageCalculation {
        require(workerCount >= 0) { "Worker count cannot be negative" }
        require(daysWorkedTenths >= 0) { "Days worked cannot be negative" }
        require(!dailyRate.isNegative()) { "Daily rate cannot be negative" }
        require(!advancePaid.isNegative()) { "Advance cannot be negative" }
        val workerDaysTenths = Math.multiplyExact(workerCount.toLong(), daysWorkedTenths.toLong())
        val numerator = Math.multiplyExact(workerDaysTenths, dailyRate.paise)
        val gross = MoneyPaise(MathRounding.divideAndRoundHalfUp(numerator, 10L))
        val net = MoneyPaise(maxOf(0L, gross.paise - advancePaid.paise))
        return QuickWageCalculation(
            grossWage = gross,
            advancePaid = advancePaid,
            netPayable = net,
        )
    }
}

data class QuickWageCalculation(
    val grossWage: MoneyPaise,
    val advancePaid: MoneyPaise,
    val netPayable: MoneyPaise,
)

data class CropProfitabilitySummary(
    val cropCycleId: Long,
    val cropName: String,
    val recordedExpense: MoneyPaise,
    val recordedIncome: MoneyPaise,
    val pendingExpensePayable: MoneyPaise,
    val pendingIncomeReceivable: MoneyPaise,
    val hasActiveCycle: Boolean,
    val hasHarvestRecord: Boolean,
    val isCalculationPartial: Boolean,
) {
    val knownBalance: MoneyPaise
        get() = recordedIncome - recordedExpense

    companion object {
        fun compute(
            cropCycle: CropCycle,
            entries: List<LedgerEntry>,
            hasHarvestRecord: Boolean,
        ): CropProfitabilitySummary {
            val activeEntries = entries.filter { !it.isVoided && it.cropCycleId == cropCycle.id }
            var expenseTotal = MoneyPaise.ZERO
            var incomeTotal = MoneyPaise.ZERO
            var payable = MoneyPaise.ZERO
            var receivable = MoneyPaise.ZERO

            for (entry in activeEntries) {
                when (entry.entryType) {
                    LedgerEntryType.EXPENSE -> {
                        expenseTotal += entry.totalAmount
                        payable += entry.balanceDue
                    }
                    LedgerEntryType.INCOME -> {
                        incomeTotal += entry.totalAmount
                        receivable += entry.balanceDue
                    }
                }
            }

            val isActive = cropCycle.status == CropCycleStatus.ACTIVE || cropCycle.status == CropCycleStatus.PLANNED
            val partial =
                isActive ||
                    incomeTotal.isZero() ||
                    expenseTotal.isZero() ||
                    !hasHarvestRecord ||
                    !payable.isZero() ||
                    !receivable.isZero()

            return CropProfitabilitySummary(
                cropCycleId = cropCycle.id,
                cropName = cropCycle.cropName,
                recordedExpense = expenseTotal,
                recordedIncome = incomeTotal,
                pendingExpensePayable = payable,
                pendingIncomeReceivable = receivable,
                hasActiveCycle = isActive,
                hasHarvestRecord = hasHarvestRecord,
                isCalculationPartial = partial,
            )
        }
    }
}
