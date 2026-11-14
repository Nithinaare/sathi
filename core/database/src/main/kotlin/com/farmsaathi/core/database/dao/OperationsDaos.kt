package com.farmsaathi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.farmsaathi.core.database.entity.InputProductEntity
import com.farmsaathi.core.database.entity.InputPurchaseEntity
import com.farmsaathi.core.database.entity.LedgerEntryEntity
import com.farmsaathi.core.database.entity.MachinerySessionEntity
import com.farmsaathi.core.database.entity.StockMovementEntity
import com.farmsaathi.core.database.entity.WorkerAttendanceEntity
import com.farmsaathi.core.database.entity.WorkerEntity
import com.farmsaathi.core.database.entity.WorkerPaymentEntity
import com.farmsaathi.core.model.FinancialCalculators
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.LedgerSourceType
import com.farmsaathi.core.model.MachinerySessionStatus
import com.farmsaathi.core.model.MachineryUsageType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.core.model.StockMovementType
import com.farmsaathi.core.model.WorkerPaymentType
import kotlinx.coroutines.flow.Flow

@Dao
interface LabourDao {
    @Query("SELECT * FROM workers WHERE isArchived = 0 ORDER BY name ASC")
    fun observeActiveWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE id = :workerId")
    fun observeWorker(workerId: Long): Flow<WorkerEntity?>

    @Query("SELECT * FROM workers WHERE id = :workerId")
    suspend fun getWorker(workerId: Long): WorkerEntity?

    @Insert
    suspend fun insertWorker(worker: WorkerEntity): Long

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Query("UPDATE workers SET isArchived = 1, updatedAtEpochMillis = :nowMillis WHERE id = :workerId")
    suspend fun archiveWorker(
        workerId: Long,
        nowMillis: Long,
    )

    @Query("UPDATE workers SET daysWorkedTenths = :daysTenths, updatedAtEpochMillis = :nowMillis WHERE id = :workerId")
    suspend fun updateWorkerDaysTenths(
        workerId: Long,
        daysTenths: Int,
        nowMillis: Long,
    )

    @Query("SELECT * FROM workers WHERE isArchived = 0 AND name LIKE '%' || :escaped || '%' ESCAPE '\\' LIMIT :limit")
    suspend fun searchWorkers(
        escaped: String,
        limit: Int,
    ): List<WorkerEntity>

    @Query("SELECT COUNT(*) FROM workers WHERE isArchived = 0")
    suspend fun countActiveWorkers(): Int

    @Query("SELECT * FROM worker_attendance WHERE isVoided = 0 ORDER BY workDateEpochDay DESC")
    fun observeAllAttendance(): Flow<List<WorkerAttendanceEntity>>

    @Query("SELECT * FROM worker_attendance WHERE isVoided = 0 AND workerId = :workerId ORDER BY workDateEpochDay DESC")
    fun observeWorkerAttendance(workerId: Long): Flow<List<WorkerAttendanceEntity>>

    @Query("SELECT * FROM worker_attendance WHERE isVoided = 0 AND workDateEpochDay = :epochDay")
    fun observeAttendanceForDay(epochDay: Long): Flow<List<WorkerAttendanceEntity>>

    @Query("SELECT * FROM worker_attendance WHERE isVoided = 0 AND workDateEpochDay = :epochDay")
    suspend fun getAttendanceForDay(epochDay: Long): List<WorkerAttendanceEntity>

    @Query("SELECT * FROM worker_attendance WHERE workerId = :workerId AND workDateEpochDay = :epochDay LIMIT 1")
    suspend fun findAttendanceByWorkerAndDay(
        workerId: Long,
        epochDay: Long,
    ): WorkerAttendanceEntity?

    @Insert
    suspend fun insertAttendance(attendance: WorkerAttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: WorkerAttendanceEntity)

    @Query("SELECT * FROM worker_payments WHERE isVoided = 0 ORDER BY paymentDateEpochMillis DESC")
    fun observeAllPayments(): Flow<List<WorkerPaymentEntity>>

    @Query("SELECT * FROM worker_payments WHERE isVoided = 0 AND workerId = :workerId ORDER BY paymentDateEpochMillis DESC")
    fun observeWorkerPayments(workerId: Long): Flow<List<WorkerPaymentEntity>>

    @Query("SELECT * FROM worker_payments WHERE id = :paymentId")
    suspend fun getPayment(paymentId: Long): WorkerPaymentEntity?

    @Insert
    suspend fun insertPayment(payment: WorkerPaymentEntity): Long

    @Query("UPDATE worker_payments SET isVoided = 1, updatedAtEpochMillis = :nowMillis WHERE id = :paymentId")
    suspend fun markPaymentVoided(
        paymentId: Long,
        nowMillis: Long,
    )

    @Insert
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long

    @Query(
        """
        UPDATE ledger_entries
        SET isVoided = 1, voidReason = :reason, updatedAtEpochMillis = :nowMillis
        WHERE sourceType = 'WORKER_PAYMENT' AND sourceId = :paymentId
        """,
    )
    suspend fun voidWorkerPaymentLedger(
        paymentId: Long,
        reason: String,
        nowMillis: Long,
    )

    @Transaction
    suspend fun upsertAttendanceAtomic(
        entity: WorkerAttendanceEntity,
        nowMillis: Long,
    ): Long {
        val existing = findAttendanceByWorkerAndDay(entity.workerId, entity.workDateEpochDay)
        return if (existing == null) {
            insertAttendance(entity.copy(createdAtEpochMillis = nowMillis, updatedAtEpochMillis = nowMillis))
        } else {
            updateAttendance(
                entity.copy(
                    id = existing.id,
                    isVoided = false,
                    createdAtEpochMillis = existing.createdAtEpochMillis,
                    updatedAtEpochMillis = nowMillis,
                ),
            )
            existing.id
        }
    }

    @Transaction
    suspend fun upsertBatchAttendanceAtomic(
        entities: List<WorkerAttendanceEntity>,
        nowMillis: Long,
    ): Int {
        for (entity in entities) {
            upsertAttendanceAtomic(entity, nowMillis)
        }
        return entities.size
    }

    @Transaction
    suspend fun recordWorkerPaymentAtomic(
        payment: WorkerPaymentEntity,
        workerName: String,
        nowMillis: Long,
    ): Long {
        val paymentId =
            insertPayment(
                payment.copy(createdAtEpochMillis = nowMillis, updatedAtEpochMillis = nowMillis),
            )
        val label =
            if (payment.paymentType == WorkerPaymentType.ADVANCE) {
                "$workerName (Advance)"
            } else {
                workerName
            }
        insertLedgerEntry(
            LedgerEntryEntity(
                entryType = LedgerEntryType.EXPENSE,
                sourceType = LedgerSourceType.WORKER_PAYMENT,
                sourceId = paymentId,
                farmId = payment.farmId,
                cropCycleId = payment.cropCycleId,
                category = LedgerCategory.LABOUR,
                title = label,
                notes = payment.notes,
                totalAmountPaise = payment.amountPaise,
                settledAmountPaise = payment.amountPaise,
                paymentStatus = PaymentStatus.PAID,
                paymentMode = payment.paymentMode,
                transactionDateEpochMillis = payment.paymentDateEpochMillis,
                createdAtEpochMillis = nowMillis,
                updatedAtEpochMillis = nowMillis,
            ),
        )
        return paymentId
    }

    @Transaction
    suspend fun voidWorkerPaymentAtomic(
        paymentId: Long,
        reason: String,
        nowMillis: Long,
    ) {
        markPaymentVoided(paymentId, nowMillis)
        voidWorkerPaymentLedger(paymentId, reason, nowMillis)
    }
}

@Dao
interface MachineryDao {
    @Query("SELECT * FROM machinery_sessions WHERE isVoided = 0 AND status = 'RUNNING' ORDER BY startEpochMillis DESC")
    fun observeRunningSessions(): Flow<List<MachinerySessionEntity>>

    @Query(
        """
        SELECT * FROM machinery_sessions
        WHERE isVoided = 0
          AND (:cropCycleId IS NULL OR cropCycleId = :cropCycleId)
        ORDER BY startEpochMillis DESC, id DESC
        """,
    )
    fun observeSessions(cropCycleId: Long?): Flow<List<MachinerySessionEntity>>

    @Query("SELECT * FROM machinery_sessions WHERE id = :sessionId")
    fun observeSession(sessionId: Long): Flow<MachinerySessionEntity?>

    @Query("SELECT * FROM machinery_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): MachinerySessionEntity?

    @Query("SELECT * FROM machinery_sessions WHERE activeLockKey = :lockKey LIMIT 1")
    suspend fun findRunningByLockKey(lockKey: String): MachinerySessionEntity?

    @Insert
    suspend fun insertSession(session: MachinerySessionEntity): Long

    @Update
    suspend fun updateSession(session: MachinerySessionEntity)

    @Insert
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long

    @Transaction
    suspend fun stopSessionAndPostLedgerAtomic(
        stoppedSession: MachinerySessionEntity,
        paymentMode: PaymentMode,
        nowMillis: Long,
    ): MachinerySessionEntity {
        val current = getSession(stoppedSession.id) ?: return stoppedSession
        if (current.status != MachinerySessionStatus.RUNNING) return current

        updateSession(stoppedSession)

        if (stoppedSession.usageType != MachineryUsageType.OWN_FARM_USE) {
            val entryType =
                if (stoppedSession.usageType == MachineryUsageType.HIRED_OUT) {
                    LedgerEntryType.INCOME
                } else {
                    LedgerEntryType.EXPENSE
                }
            val category =
                if (entryType == LedgerEntryType.INCOME) {
                    LedgerCategory.MACHINERY_HIRE_INCOME
                } else {
                    LedgerCategory.MACHINERY
                }
            val payStatus =
                FinancialCalculators.derivePaymentStatus(
                    MoneyPaise(stoppedSession.computedAmountPaise),
                    MoneyPaise(stoppedSession.paidAmountPaise),
                )
            insertLedgerEntry(
                LedgerEntryEntity(
                    entryType = entryType,
                    sourceType = LedgerSourceType.MACHINERY_SESSION,
                    sourceId = stoppedSession.id,
                    farmId = stoppedSession.farmId,
                    plotId = stoppedSession.plotId,
                    cropCycleId = stoppedSession.cropCycleId,
                    contactId = stoppedSession.providerContactId,
                    category = category,
                    title = "${stoppedSession.machineName} (${stoppedSession.operationType})",
                    notes = stoppedSession.notes,
                    totalAmountPaise = stoppedSession.computedAmountPaise,
                    settledAmountPaise = stoppedSession.paidAmountPaise,
                    paymentStatus = payStatus,
                    paymentMode = paymentMode,
                    transactionDateEpochMillis = stoppedSession.endEpochMillis ?: nowMillis,
                    createdAtEpochMillis = nowMillis,
                    updatedAtEpochMillis = nowMillis,
                ),
            )
        }
        return stoppedSession
    }

    @Transaction
    suspend fun saveManualSessionAndLedgerAtomic(
        session: MachinerySessionEntity,
        paymentMode: PaymentMode,
        nowMillis: Long,
    ): Long {
        val sessionId = insertSession(session)
        if (session.usageType != MachineryUsageType.OWN_FARM_USE) {
            val entryType =
                if (session.usageType == MachineryUsageType.HIRED_OUT) {
                    LedgerEntryType.INCOME
                } else {
                    LedgerEntryType.EXPENSE
                }
            val category =
                if (entryType == LedgerEntryType.INCOME) {
                    LedgerCategory.MACHINERY_HIRE_INCOME
                } else {
                    LedgerCategory.MACHINERY
                }
            insertLedgerEntry(
                LedgerEntryEntity(
                    entryType = entryType,
                    sourceType = LedgerSourceType.MACHINERY_SESSION,
                    sourceId = sessionId,
                    farmId = session.farmId,
                    plotId = session.plotId,
                    cropCycleId = session.cropCycleId,
                    contactId = session.providerContactId,
                    category = category,
                    title = "${session.machineName} (${session.operationType})",
                    notes = session.notes,
                    totalAmountPaise = session.computedAmountPaise,
                    settledAmountPaise = session.paidAmountPaise,
                    paymentStatus = session.paymentStatus,
                    paymentMode = paymentMode,
                    transactionDateEpochMillis = session.endEpochMillis ?: session.startEpochMillis,
                    createdAtEpochMillis = nowMillis,
                    updatedAtEpochMillis = nowMillis,
                ),
            )
        }
        return sessionId
    }
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM input_products WHERE (:includeArchived = 1 OR isArchived = 0) ORDER BY name ASC")
    fun observeProducts(includeArchived: Boolean): Flow<List<InputProductEntity>>

    @Query("SELECT * FROM input_products WHERE id = :productId")
    fun observeProduct(productId: Long): Flow<InputProductEntity?>

    @Query("SELECT * FROM input_products WHERE id = :productId")
    suspend fun getProduct(productId: Long): InputProductEntity?

    @Insert
    suspend fun insertProduct(product: InputProductEntity): Long

    @Update
    suspend fun updateProduct(product: InputProductEntity)

    @Query("UPDATE input_products SET isArchived = 1, updatedAtEpochMillis = :nowMillis WHERE id = :productId")
    suspend fun archiveProduct(
        productId: Long,
        nowMillis: Long,
    )

    @Query("SELECT * FROM input_products WHERE isArchived = 0 AND name LIKE '%' || :escaped || '%' ESCAPE '\\' LIMIT :limit")
    suspend fun searchProducts(
        escaped: String,
        limit: Int,
    ): List<InputProductEntity>

    @Query(
        """
        SELECT * FROM input_purchases
        WHERE isVoided = 0
          AND (:productId IS NULL OR productId = :productId)
        ORDER BY purchaseDateEpochMillis DESC, id DESC
        """,
    )
    fun observePurchases(productId: Long?): Flow<List<InputPurchaseEntity>>

    @Insert
    suspend fun insertPurchase(purchase: InputPurchaseEntity): Long

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY movementDateEpochMillis DESC, id DESC")
    fun observeMovements(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY movementDateEpochMillis DESC, id DESC")
    fun observeAllMovements(): Flow<List<StockMovementEntity>>

    @Insert
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Insert
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long

    /**
     * Atomically creates purchase + stock-in movement + expense ledger entry (and opening product update),
     * ensuring inventory and financial ledger stay synchronized with zero duplicate financial entries.
     */
    @Transaction
    suspend fun recordPurchaseAtomic(
        purchase: InputPurchaseEntity,
        nowMillis: Long,
    ): Long {
        val product = getProduct(purchase.productId) ?: error("Product not found")
        val purchaseId =
            insertPurchase(
                purchase.copy(createdAtEpochMillis = nowMillis, updatedAtEpochMillis = nowMillis),
            )
        val newBalance = product.currentStockMilliUnits + purchase.quantityMilliUnits
        updateProduct(
            product.copy(
                currentStockMilliUnits = newBalance,
                defaultUnitPricePaise = purchase.unitPricePaise,
                updatedAtEpochMillis = nowMillis,
            ),
        )
        insertMovement(
            StockMovementEntity(
                productId = product.id,
                farmId = purchase.farmId,
                cropCycleId = purchase.cropCycleId,
                purchaseId = purchaseId,
                movementType = StockMovementType.IN,
                reason = StockMovementReason.PURCHASE,
                signedDeltaMilliUnits = purchase.quantityMilliUnits,
                balanceAfterMilliUnits = newBalance,
                unitCostPaiseSnapshot = purchase.unitPricePaise,
                movementDateEpochMillis = purchase.purchaseDateEpochMillis,
                notes = purchase.notes,
                createdAtEpochMillis = nowMillis,
            ),
        )
        val ledgerCategory =
            when (product.category) {
                com.farmsaathi.core.model.InputCategory.SEED -> LedgerCategory.SEEDS
                com.farmsaathi.core.model.InputCategory.FERTILIZER,
                com.farmsaathi.core.model.InputCategory.MICRONUTRIENT,
                com.farmsaathi.core.model.InputCategory.ORGANIC,
                -> LedgerCategory.FERTILIZERS
                com.farmsaathi.core.model.InputCategory.PESTICIDE,
                com.farmsaathi.core.model.InputCategory.HERBICIDE,
                com.farmsaathi.core.model.InputCategory.FUNGICIDE,
                -> LedgerCategory.PESTICIDES
                com.farmsaathi.core.model.InputCategory.OTHER -> LedgerCategory.OTHER_EXPENSE
            }
        insertLedgerEntry(
            LedgerEntryEntity(
                entryType = LedgerEntryType.EXPENSE,
                sourceType = LedgerSourceType.INPUT_PURCHASE,
                sourceId = purchaseId,
                farmId = purchase.farmId,
                cropCycleId = purchase.cropCycleId,
                contactId = purchase.vendorContactId,
                category = ledgerCategory,
                title = product.name,
                notes = purchase.notes,
                totalAmountPaise = purchase.totalAmountPaise,
                settledAmountPaise = purchase.paidAmountPaise,
                paymentStatus = purchase.paymentStatus,
                paymentMode = purchase.paymentMode,
                transactionDateEpochMillis = purchase.purchaseDateEpochMillis,
                dueDateEpochMillis = purchase.dueDateEpochMillis,
                receiptPhotoPath = purchase.receiptPhotoPath,
                createdAtEpochMillis = nowMillis,
                updatedAtEpochMillis = nowMillis,
            ),
        )
        return purchaseId
    }

    /**
     * Atomically records stock deduction/adjustment, blocking negative stock per approved domain rule.
     */
    @Transaction
    suspend fun recordMovementAtomic(
        productId: Long,
        farmId: Long,
        plotId: Long?,
        cropCycleId: Long?,
        signedDeltaMilliUnits: Long,
        reason: StockMovementReason,
        notes: String?,
        nowMillis: Long,
        photoPath: String? = null,
    ): Long? {
        val product = getProduct(productId) ?: return null
        val newBalance = product.currentStockMilliUnits + signedDeltaMilliUnits
        if (newBalance < 0L) return null

        val movementType =
            when {
                reason == StockMovementReason.STOCK_COUNT_CORRECTION -> StockMovementType.ADJUSTMENT
                signedDeltaMilliUnits > 0L -> StockMovementType.IN
                else -> StockMovementType.OUT
            }
        updateProduct(product.copy(currentStockMilliUnits = newBalance, updatedAtEpochMillis = nowMillis))
        return insertMovement(
            StockMovementEntity(
                productId = productId,
                farmId = farmId,
                plotId = plotId,
                cropCycleId = cropCycleId,
                movementType = movementType,
                reason = reason,
                signedDeltaMilliUnits = signedDeltaMilliUnits,
                balanceAfterMilliUnits = newBalance,
                unitCostPaiseSnapshot = product.defaultUnitPricePaise,
                movementDateEpochMillis = nowMillis,
                notes = notes,
                photoPath = photoPath,
                createdAtEpochMillis = nowMillis,
            ),
        )
    }
}
