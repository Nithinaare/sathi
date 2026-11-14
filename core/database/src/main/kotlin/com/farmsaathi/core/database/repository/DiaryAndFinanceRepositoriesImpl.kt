package com.farmsaathi.core.database.repository

import com.farmsaathi.core.common.SqlSearchUtils
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.DiaryRepository
import com.farmsaathi.core.common.repository.FinanceRepository
import com.farmsaathi.core.common.repository.SeasonFinancialTotals
import com.farmsaathi.core.database.DatabaseMaintenanceCoordinator
import com.farmsaathi.core.database.dao.CropDao
import com.farmsaathi.core.database.dao.DiaryDao
import com.farmsaathi.core.database.dao.FieldOpsDao
import com.farmsaathi.core.database.dao.FinanceDao
import com.farmsaathi.core.database.entity.CropSaleEntity
import com.farmsaathi.core.database.entity.ExpenseDetailEntity
import com.farmsaathi.core.database.entity.LedgerEntryEntity
import com.farmsaathi.core.database.mapper.toDomain
import com.farmsaathi.core.database.mapper.toEntity
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.CropProfitabilitySummary
import com.farmsaathi.core.model.CropSale
import com.farmsaathi.core.model.DiaryEntry
import com.farmsaathi.core.model.DomainError
import com.farmsaathi.core.model.ExpenseDetail
import com.farmsaathi.core.model.FinancialCalculators
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.LedgerSourceType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomDiaryRepository
@Inject
constructor(
    private val diaryDao: DiaryDao,
    private val timeProvider: TimeProvider,
    private val coordinator: DatabaseMaintenanceCoordinator,
) : DiaryRepository {
    override fun observeDiaryEntries(
        farmId: Long?,
        plotId: Long?,
        cropCycleId: Long?,
        query: String,
    ): Flow<List<DiaryEntry>> {
        val escaped = SqlSearchUtils.escapeSqlLike(query)
        return diaryDao.observeEntries(farmId, plotId, cropCycleId, escaped)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()
    }

    override suspend fun getDiaryEntry(entryId: Long): DiaryEntry? = diaryDao.getEntry(entryId)?.toDomain()

    override suspend fun saveDiaryEntry(entry: DiaryEntry): AppResult<Long> {
        if (entry.title.isBlank()) return AppResult.Error(DomainError.InvalidName)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val entity = entry.toEntity(now)
            val id =
                if (entry.id == 0L) {
                    diaryDao.insertEntry(entity)
                } else {
                    diaryDao.updateEntry(entity)
                    entry.id
                }
            AppResult.Success(id)
        }
    }

    override suspend fun archiveDiaryEntry(entryId: Long): AppResult<Unit> =
        coordinator.withExclusiveLock {
            diaryDao.archiveEntry(entryId, timeProvider.nowEpochMillis())
            AppResult.Success(Unit)
        }
}

@Singleton
class RoomFinanceRepository
@Inject
constructor(
    private val financeDao: FinanceDao,
    private val cropDao: CropDao,
    private val fieldOpsDao: FieldOpsDao,
    private val timeProvider: TimeProvider,
    private val coordinator: DatabaseMaintenanceCoordinator,
) : FinanceRepository {
    override fun observeLedgerEntries(
        farmId: Long?,
        cropCycleId: Long?,
        entryType: LedgerEntryType?,
        query: String,
    ): Flow<List<LedgerEntry>> =
        financeDao.observeLedgerEntries(
            farmId = farmId,
            cropCycleId = cropCycleId,
            entryType = entryType,
            escaped = SqlSearchUtils.escapeSqlLike(query),
        ).map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override fun observeSeasonTotals(farmId: Long?): Flow<SeasonFinancialTotals> =
        financeDao.observeSeasonTotals(farmId)
            .map { totals ->
                SeasonFinancialTotals(
                    totalExpense = MoneyPaise(totals.totalExpensePaise),
                    totalIncome = MoneyPaise(totals.totalIncomePaise),
                    pendingPayable = MoneyPaise(totals.totalPayableDuePaise),
                    pendingReceivable = MoneyPaise(totals.totalReceivableDuePaise),
                )
            }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override fun observeLedgerEntry(ledgerEntryId: Long): Flow<LedgerEntry?> =
        financeDao.observeLedgerEntry(ledgerEntryId)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override suspend fun getExpenseDetail(expenseId: Long): ExpenseDetail? = financeDao.getExpenseDetail(expenseId)?.toDomain()

    override suspend fun saveManualExpense(
        detail: ExpenseDetail,
        paidAmount: MoneyPaise,
    ): AppResult<Long> {
        if (detail.description.isBlank()) return AppResult.Error(DomainError.InvalidName)
        if (detail.totalAmount.isNegative() || paidAmount.isNegative() || paidAmount > detail.totalAmount) {
            return AppResult.Error(DomainError.InvalidAmount)
        }
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val entity =
                ExpenseDetailEntity(
                    id = detail.id,
                    farmId = detail.farmId,
                    plotId = detail.plotId,
                    cropCycleId = detail.cropCycleId,
                    contactId = detail.contactId,
                    category = detail.category,
                    description = detail.description.trim(),
                    vendorNameSnapshot = detail.vendorNameSnapshot?.trim(),
                    billNumber = detail.billNumber?.trim(),
                    quantityMilliUnits = detail.quantityMilliUnits,
                    quantityUnit = detail.quantityUnit,
                    unitPricePaise = detail.unitPrice?.paise,
                    totalAmountPaise = detail.totalAmount.paise,
                    paidAmountPaise = paidAmount.paise,
                    paymentMode = detail.paymentMode,
                    expenseDateEpochMillis = detail.expenseDateEpochMillis,
                    receiptPhotoPath = detail.receiptPhotoPath,
                    voiceNotePath = detail.voiceNotePath,
                    voiceNoteDurationMillis = detail.voiceNoteDurationMillis,
                    notes = detail.notes?.trim(),
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
            AppResult.Success(financeDao.saveManualExpenseAtomic(entity, now))
        }
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
        if (title.isBlank()) return AppResult.Error(DomainError.InvalidName)
        if (amount.isNegative() || settledAmount.isNegative() || settledAmount > amount) {
            return AppResult.Error(DomainError.InvalidAmount)
        }
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val id =
                financeDao.insertLedgerEntry(
                    LedgerEntryEntity(
                        entryType = LedgerEntryType.INCOME,
                        sourceType = LedgerSourceType.MANUAL_INCOME,
                        sourceId = null,
                        farmId = farmId,
                        plotId = plotId,
                        cropCycleId = cropCycleId,
                        category = LedgerCategory.OTHER_INCOME,
                        title = title.trim(),
                        notes = notes?.trim(),
                        totalAmountPaise = amount.paise,
                        settledAmountPaise = settledAmount.paise,
                        paymentStatus = FinancialCalculators.derivePaymentStatus(amount, settledAmount),
                        paymentMode = paymentMode,
                        transactionDateEpochMillis = dateEpochMillis,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )
            AppResult.Success(id)
        }
    }

    override fun observeCropSales(cropCycleId: Long?): Flow<List<CropSale>> =
        financeDao.observeCropSales(cropCycleId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override fun observeCropSale(saleId: Long): Flow<CropSale?> =
        financeDao.observeCropSale(saleId)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override suspend fun saveCropSale(sale: CropSale): AppResult<Long> {
        if (sale.buyerNameSnapshot.isBlank()) return AppResult.Error(DomainError.InvalidName)
        if (sale.quantityMilliUnits <= 0L) return AppResult.Error(DomainError.InvalidQuantity)
        if (sale.netTotalAmount.isNegative() || sale.amountReceived.isNegative() ||
            sale.amountReceived > sale.netTotalAmount
        ) {
            return AppResult.Error(DomainError.InvalidAmount)
        }
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val entity =
                CropSaleEntity(
                    id = sale.id,
                    farmId = sale.farmId,
                    plotId = sale.plotId,
                    cropCycleId = sale.cropCycleId,
                    harvestRecordId = sale.harvestRecordId,
                    storageRecordId = sale.storageRecordId,
                    buyerContactId = sale.buyerContactId,
                    buyerNameSnapshot = sale.buyerNameSnapshot.trim(),
                    mandiOrLocation = sale.mandiOrLocation?.trim(),
                    quantityMilliUnits = sale.quantityMilliUnits,
                    quantityUnit = sale.quantityUnit,
                    ratePerUnitPaise = sale.ratePerUnit.paise,
                    grossAmountPaise = sale.grossAmount.paise,
                    deductionsPaise = sale.deductions.paise,
                    netTotalAmountPaise = sale.netTotalAmount.paise,
                    amountReceivedPaise = sale.amountReceived.paise,
                    paymentStatus = sale.paymentStatus,
                    paymentMode = sale.paymentMode,
                    saleDateEpochMillis = sale.saleDateEpochMillis,
                    dueDateEpochMillis = sale.dueDateEpochMillis,
                    receiptPhotoPath = sale.receiptPhotoPath,
                    notes = sale.notes?.trim(),
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
            AppResult.Success(financeDao.saveCropSaleAtomic(entity, now))
        }
    }

    override suspend fun recordLedgerSettlement(
        ledgerEntryId: Long,
        additionalPaidOrReceived: MoneyPaise,
        paymentMode: PaymentMode,
    ): AppResult<Unit> {
        if (!additionalPaidOrReceived.isPositive()) return AppResult.Error(DomainError.InvalidAmount)
        return coordinator.withExclusiveLock {
            val ok =
                financeDao.recordSettlementAtomic(
                    ledgerEntryId = ledgerEntryId,
                    additionalPaise = additionalPaidOrReceived.paise,
                    paymentMode = paymentMode,
                    nowMillis = timeProvider.nowEpochMillis(),
                )
            if (ok) AppResult.Success(Unit) else AppResult.Error(DomainError.PaymentExceedsTotal)
        }
    }

    override suspend fun voidLedgerEntry(
        ledgerEntryId: Long,
        reason: String,
    ): AppResult<Unit> =
        coordinator.withExclusiveLock {
            financeDao.voidLedgerEntryAtomic(ledgerEntryId, reason.trim(), timeProvider.nowEpochMillis())
            AppResult.Success(Unit)
        }

    override fun observeCropProfitabilitySummaries(farmId: Long?): Flow<List<CropProfitabilitySummary>> =
        combine(
            cropDao.observeCropCycles(plotId = null, farmId = farmId),
            financeDao.observeActiveLedgerForTotals(farmId),
            fieldOpsDao.observeAllActiveHarvestRecords(),
        ) { cycles, ledgerEntities, harvests ->
            val ledger = ledgerEntities.map { it.toDomain() }
            val harvestedCycleIds = harvests.map { it.cropCycleId }.toSet()
            cycles.map { cycleEntity ->
                val cycle = cycleEntity.toDomain()
                CropProfitabilitySummary.compute(
                    cropCycle = cycle,
                    entries = ledger,
                    hasHarvestRecord = cycle.id in harvestedCycleIds,
                )
            }
        }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()
}
