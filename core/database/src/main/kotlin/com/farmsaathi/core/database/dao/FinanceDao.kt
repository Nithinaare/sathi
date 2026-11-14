package com.farmsaathi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.farmsaathi.core.database.entity.CropSaleEntity
import com.farmsaathi.core.database.entity.ExpenseDetailEntity
import com.farmsaathi.core.database.entity.LedgerEntryEntity
import com.farmsaathi.core.database.entity.StorageRecordEntity
import com.farmsaathi.core.model.FinancialCalculators
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.LedgerSourceType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.core.model.StorageStatus
import kotlinx.coroutines.flow.Flow

data class SeasonTotalsProjection(
    val totalExpensePaise: Long,
    val totalIncomePaise: Long,
    val totalPayableDuePaise: Long,
    val totalReceivableDuePaise: Long,
)

@Dao
@Suppress("TooManyFunctions") // Single cohesive DAO owning ledger, expense & crop sale transactions
interface FinanceDao {
    @Query(
        """
        SELECT * FROM ledger_entries
        WHERE isVoided = 0 AND isArchived = 0
          AND (:farmId IS NULL OR farmId = :farmId)
          AND (:cropCycleId IS NULL OR cropCycleId = :cropCycleId)
          AND (:entryType IS NULL OR entryType = :entryType)
          AND (:escaped = '' OR title LIKE '%' || :escaped || '%' ESCAPE '\'
                             OR notes LIKE '%' || :escaped || '%' ESCAPE '\')
        ORDER BY transactionDateEpochMillis DESC, id DESC
        """,
    )
    fun observeLedgerEntries(
        farmId: Long?,
        cropCycleId: Long?,
        entryType: LedgerEntryType?,
        escaped: String,
    ): Flow<List<LedgerEntryEntity>>

    @Query(
        """
        SELECT * FROM ledger_entries
        WHERE isVoided = 0 AND isArchived = 0
          AND (:farmId IS NULL OR farmId = :farmId)
        """,
    )
    fun observeActiveLedgerForTotals(farmId: Long?): Flow<List<LedgerEntryEntity>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN entryType = 'EXPENSE' THEN totalAmountPaise ELSE 0 END), 0) AS totalExpensePaise,
            COALESCE(SUM(CASE WHEN entryType = 'INCOME' THEN totalAmountPaise ELSE 0 END), 0) AS totalIncomePaise,
            COALESCE(
                SUM(
                    CASE
                        WHEN entryType = 'EXPENSE' THEN MAX(0, totalAmountPaise - settledAmountPaise)
                        ELSE 0
                    END
                ),
                0
            ) AS totalPayableDuePaise,
            COALESCE(
                SUM(
                    CASE
                        WHEN entryType = 'INCOME' THEN MAX(0, totalAmountPaise - settledAmountPaise)
                        ELSE 0
                    END
                ),
                0
            ) AS totalReceivableDuePaise
        FROM ledger_entries
        WHERE isVoided = 0 AND isArchived = 0
          AND (:farmId IS NULL OR farmId = :farmId)
        """,
    )
    fun observeSeasonTotals(farmId: Long?): Flow<SeasonTotalsProjection>

    @Query("SELECT * FROM ledger_entries WHERE id = :id")
    fun observeLedgerEntry(id: Long): Flow<LedgerEntryEntity?>

    @Query("SELECT * FROM ledger_entries WHERE id = :id")
    suspend fun getLedgerEntry(id: Long): LedgerEntryEntity?

    @Query("SELECT * FROM ledger_entries WHERE sourceType = :sourceType AND sourceId = :sourceId LIMIT 1")
    suspend fun findBySource(
        sourceType: LedgerSourceType,
        sourceId: Long,
    ): LedgerEntryEntity?

    @Insert
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long

    @Update
    suspend fun updateLedgerEntry(entry: LedgerEntryEntity)

    @Query("SELECT * FROM expense_details WHERE id = :expenseId")
    suspend fun getExpenseDetail(expenseId: Long): ExpenseDetailEntity?

    @Insert
    suspend fun insertExpenseDetail(detail: ExpenseDetailEntity): Long

    @Update
    suspend fun updateExpenseDetail(detail: ExpenseDetailEntity)

    @Query("UPDATE expense_details SET isVoided = 1, updatedAtEpochMillis = :nowMillis WHERE id = :id")
    suspend fun voidExpenseDetail(
        id: Long,
        nowMillis: Long,
    )

    @Query(
        """
        SELECT * FROM crop_sales
        WHERE isVoided = 0
          AND (:cropCycleId IS NULL OR cropCycleId = :cropCycleId)
        ORDER BY saleDateEpochMillis DESC, id DESC
        """,
    )
    fun observeCropSales(cropCycleId: Long?): Flow<List<CropSaleEntity>>

    @Query("SELECT * FROM crop_sales WHERE id = :saleId")
    fun observeCropSale(saleId: Long): Flow<CropSaleEntity?>

    @Query("SELECT * FROM crop_sales WHERE id = :saleId")
    suspend fun getCropSale(saleId: Long): CropSaleEntity?

    @Insert
    suspend fun insertCropSale(sale: CropSaleEntity): Long

    @Update
    suspend fun updateCropSale(sale: CropSaleEntity)

    @Query("UPDATE crop_sales SET isVoided = 1, updatedAtEpochMillis = :nowMillis WHERE id = :id")
    suspend fun voidCropSale(
        id: Long,
        nowMillis: Long,
    )

    @Query("SELECT * FROM storage_records WHERE id = :storageId")
    suspend fun getStorageForSale(storageId: Long): StorageRecordEntity?

    @Update
    suspend fun updateStorageRecord(storage: StorageRecordEntity)

    @Query("UPDATE input_purchases SET paidAmountPaise = :paid, paymentStatus = :status, updatedAtEpochMillis = :nowMillis WHERE id = :id")
    suspend fun syncInputPurchaseSettlement(
        id: Long,
        paid: Long,
        status: PaymentStatus,
        nowMillis: Long,
    )

    @Query("UPDATE input_purchases SET isVoided = 1, updatedAtEpochMillis = :nowMillis WHERE id = :id")
    suspend fun voidInputPurchase(
        id: Long,
        nowMillis: Long,
    )

    @Query(
        "UPDATE machinery_sessions SET paidAmountPaise = :paid, paymentStatus = :status, updatedAtEpochMillis = :nowMillis WHERE id = :id",
    )
    suspend fun syncMachinerySettlement(
        id: Long,
        paid: Long,
        status: PaymentStatus,
        nowMillis: Long,
    )

    @Query("UPDATE machinery_sessions SET isVoided = 1, updatedAtEpochMillis = :nowMillis WHERE id = :id")
    suspend fun voidMachinerySession(
        id: Long,
        nowMillis: Long,
    )

    @Query("UPDATE worker_payments SET isVoided = 1, updatedAtEpochMillis = :nowMillis WHERE id = :id")
    suspend fun voidWorkerPayment(
        id: Long,
        nowMillis: Long,
    )

    @Query(
        """
        SELECT * FROM ledger_entries
        WHERE isVoided = 0 AND isArchived = 0
          AND (title LIKE '%' || :escaped || '%' ESCAPE '\'
               OR notes LIKE '%' || :escaped || '%' ESCAPE '\')
        LIMIT :limit
        """,
    )
    suspend fun searchLedger(
        escaped: String,
        limit: Int,
    ): List<LedgerEntryEntity>

    @Query("SELECT COUNT(*) FROM ledger_entries WHERE isVoided = 0 AND isArchived = 0")
    suspend fun countActiveLedgerEntries(): Int

    @Transaction
    suspend fun saveManualExpenseAtomic(
        detail: ExpenseDetailEntity,
        nowMillis: Long,
    ): Long {
        val status =
            FinancialCalculators.derivePaymentStatus(
                MoneyPaise(detail.totalAmountPaise),
                MoneyPaise(detail.paidAmountPaise),
            )
        val expenseId =
            if (detail.id == 0L) {
                insertExpenseDetail(detail.copy(createdAtEpochMillis = nowMillis, updatedAtEpochMillis = nowMillis))
            } else {
                updateExpenseDetail(detail.copy(updatedAtEpochMillis = nowMillis))
                detail.id
            }

        val existingLedger = findBySource(LedgerSourceType.MANUAL_EXPENSE, expenseId)
        val ledger =
            LedgerEntryEntity(
                id = existingLedger?.id ?: 0L,
                entryType = LedgerEntryType.EXPENSE,
                sourceType = LedgerSourceType.MANUAL_EXPENSE,
                sourceId = expenseId,
                farmId = detail.farmId,
                plotId = detail.plotId,
                cropCycleId = detail.cropCycleId,
                contactId = detail.contactId,
                category = detail.category,
                title = detail.description,
                notes = detail.notes,
                totalAmountPaise = detail.totalAmountPaise,
                settledAmountPaise = detail.paidAmountPaise,
                paymentStatus = status,
                paymentMode = detail.paymentMode,
                transactionDateEpochMillis = detail.expenseDateEpochMillis,
                receiptPhotoPath = detail.receiptPhotoPath,
                voiceNotePath = detail.voiceNotePath,
                createdAtEpochMillis = existingLedger?.createdAtEpochMillis ?: nowMillis,
                updatedAtEpochMillis = nowMillis,
            )
        if (existingLedger == null) insertLedgerEntry(ledger) else updateLedgerEntry(ledger)
        return expenseId
    }

    @Transaction
    suspend fun saveCropSaleAtomic(
        sale: CropSaleEntity,
        nowMillis: Long,
    ): Long {
        val status =
            FinancialCalculators.derivePaymentStatus(
                MoneyPaise(sale.netTotalAmountPaise),
                MoneyPaise(sale.amountReceivedPaise),
            )
        val isNew = sale.id == 0L
        val persisted =
            sale.copy(
                paymentStatus = status,
                createdAtEpochMillis = if (isNew) nowMillis else sale.createdAtEpochMillis,
                updatedAtEpochMillis = nowMillis,
            )
        val saleId =
            if (isNew) {
                insertCropSale(persisted)
            } else {
                updateCropSale(persisted)
                sale.id
            }

        if (isNew && sale.storageRecordId != null) {
            val storage = getStorageForSale(sale.storageRecordId)
            if (storage != null) {
                val newRemaining = maxOf(0L, storage.remainingQuantityMilliUnits - sale.quantityMilliUnits)
                val newStatus =
                    when {
                        newRemaining == 0L -> StorageStatus.CLEARED
                        newRemaining < storage.quantityMilliUnits -> StorageStatus.PARTIALLY_DISPATCHED
                        else -> StorageStatus.IN_STORAGE
                    }
                updateStorageRecord(
                    storage.copy(
                        remainingQuantityMilliUnits = newRemaining,
                        status = newStatus,
                        updatedAtEpochMillis = nowMillis,
                    ),
                )
            }
        }

        val existingLedger = findBySource(LedgerSourceType.CROP_SALE, saleId)
        val ledger =
            LedgerEntryEntity(
                id = existingLedger?.id ?: 0L,
                entryType = LedgerEntryType.INCOME,
                sourceType = LedgerSourceType.CROP_SALE,
                sourceId = saleId,
                farmId = sale.farmId,
                plotId = sale.plotId,
                cropCycleId = sale.cropCycleId,
                contactId = sale.buyerContactId,
                category = LedgerCategory.CROP_SALE,
                title = sale.buyerNameSnapshot,
                notes = sale.notes,
                totalAmountPaise = sale.netTotalAmountPaise,
                settledAmountPaise = sale.amountReceivedPaise,
                paymentStatus = status,
                paymentMode = sale.paymentMode,
                transactionDateEpochMillis = sale.saleDateEpochMillis,
                dueDateEpochMillis = sale.dueDateEpochMillis,
                receiptPhotoPath = sale.receiptPhotoPath,
                createdAtEpochMillis = existingLedger?.createdAtEpochMillis ?: nowMillis,
                updatedAtEpochMillis = nowMillis,
            )
        if (existingLedger == null) insertLedgerEntry(ledger) else updateLedgerEntry(ledger)
        return saleId
    }

    @Transaction
    suspend fun recordSettlementAtomic(
        ledgerEntryId: Long,
        additionalPaise: Long,
        paymentMode: PaymentMode,
        nowMillis: Long,
    ): Boolean {
        val entry = getLedgerEntry(ledgerEntryId) ?: return false
        if (entry.isVoided || additionalPaise <= 0L) return false
        val newSettled = entry.settledAmountPaise + additionalPaise
        if (newSettled > entry.totalAmountPaise) return false

        val newStatus =
            FinancialCalculators.derivePaymentStatus(
                MoneyPaise(entry.totalAmountPaise),
                MoneyPaise(newSettled),
            )
        updateLedgerEntry(
            entry.copy(
                settledAmountPaise = newSettled,
                paymentStatus = newStatus,
                paymentMode = paymentMode,
                updatedAtEpochMillis = nowMillis,
            ),
        )
        val srcId = entry.sourceId
        if (srcId != null) {
            when (entry.sourceType) {
                LedgerSourceType.CROP_SALE -> {
                    getCropSale(srcId)?.let { sale ->
                        updateCropSale(
                            sale.copy(
                                amountReceivedPaise = newSettled,
                                paymentStatus = newStatus,
                                paymentMode = paymentMode,
                                updatedAtEpochMillis = nowMillis,
                            ),
                        )
                    }
                }
                LedgerSourceType.MANUAL_EXPENSE -> {
                    getExpenseDetail(srcId)?.let { exp ->
                        updateExpenseDetail(
                            exp.copy(
                                paidAmountPaise = newSettled,
                                paymentMode = paymentMode,
                                updatedAtEpochMillis = nowMillis,
                            ),
                        )
                    }
                }
                LedgerSourceType.INPUT_PURCHASE ->
                    syncInputPurchaseSettlement(srcId, newSettled, newStatus, nowMillis)
                LedgerSourceType.MACHINERY_SESSION ->
                    syncMachinerySettlement(srcId, newSettled, newStatus, nowMillis)
                LedgerSourceType.WORKER_PAYMENT, LedgerSourceType.MANUAL_INCOME -> Unit
            }
        }
        return true
    }

    @Transaction
    suspend fun voidLedgerEntryAtomic(
        ledgerEntryId: Long,
        reason: String,
        nowMillis: Long,
    ): Boolean {
        val entry = getLedgerEntry(ledgerEntryId) ?: return false
        updateLedgerEntry(
            entry.copy(
                isVoided = true,
                voidReason = reason,
                updatedAtEpochMillis = nowMillis,
            ),
        )
        val srcId = entry.sourceId
        if (srcId != null) {
            when (entry.sourceType) {
                LedgerSourceType.MANUAL_EXPENSE -> voidExpenseDetail(srcId, nowMillis)
                LedgerSourceType.CROP_SALE -> voidCropSale(srcId, nowMillis)
                LedgerSourceType.INPUT_PURCHASE -> voidInputPurchase(srcId, nowMillis)
                LedgerSourceType.MACHINERY_SESSION -> voidMachinerySession(srcId, nowMillis)
                LedgerSourceType.WORKER_PAYMENT -> voidWorkerPayment(srcId, nowMillis)
                LedgerSourceType.MANUAL_INCOME -> Unit
            }
        }
        return true
    }
}
