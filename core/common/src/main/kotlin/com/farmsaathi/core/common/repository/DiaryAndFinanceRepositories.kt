package com.farmsaathi.core.common.repository

import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.CropProfitabilitySummary
import com.farmsaathi.core.model.CropSale
import com.farmsaathi.core.model.DiaryEntry
import com.farmsaathi.core.model.ExpenseDetail
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import kotlinx.coroutines.flow.Flow

interface DiaryRepository {
    fun observeDiaryEntries(
        farmId: Long? = null,
        plotId: Long? = null,
        cropCycleId: Long? = null,
        query: String = "",
    ): Flow<List<DiaryEntry>>

    suspend fun getDiaryEntry(entryId: Long): DiaryEntry?

    suspend fun saveDiaryEntry(entry: DiaryEntry): AppResult<Long>

    suspend fun archiveDiaryEntry(entryId: Long): AppResult<Unit>
}

data class SeasonFinancialTotals(
    val totalExpense: MoneyPaise,
    val totalIncome: MoneyPaise,
    val pendingPayable: MoneyPaise,
    val pendingReceivable: MoneyPaise,
) {
    val netBalance: MoneyPaise get() = totalIncome - totalExpense
}

interface FinanceRepository {
    fun observeLedgerEntries(
        farmId: Long? = null,
        cropCycleId: Long? = null,
        entryType: LedgerEntryType? = null,
        query: String = "",
    ): Flow<List<LedgerEntry>>

    fun observeSeasonTotals(farmId: Long? = null): Flow<SeasonFinancialTotals>

    fun observeLedgerEntry(ledgerEntryId: Long): Flow<LedgerEntry?>

    suspend fun getExpenseDetail(expenseId: Long): ExpenseDetail?

    suspend fun saveManualExpense(
        detail: ExpenseDetail,
        paidAmount: MoneyPaise,
    ): AppResult<Long>

    suspend fun saveManualIncome(
        farmId: Long,
        plotId: Long?,
        cropCycleId: Long?,
        title: String,
        amount: MoneyPaise,
        settledAmount: MoneyPaise,
        paymentMode: PaymentMode,
        dateEpochMillis: Long,
        notes: String?,
    ): AppResult<Long>

    fun observeCropSales(cropCycleId: Long? = null): Flow<List<CropSale>>

    fun observeCropSale(saleId: Long): Flow<CropSale?>

    suspend fun saveCropSale(sale: CropSale): AppResult<Long>

    suspend fun recordLedgerSettlement(
        ledgerEntryId: Long,
        additionalPaidOrReceived: MoneyPaise,
        paymentMode: PaymentMode,
    ): AppResult<Unit>

    suspend fun voidLedgerEntry(
        ledgerEntryId: Long,
        reason: String,
    ): AppResult<Unit>

    fun observeCropProfitabilitySummaries(farmId: Long? = null): Flow<List<CropProfitabilitySummary>>
}
