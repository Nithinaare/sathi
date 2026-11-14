package com.farmsaathi.core.common.repository

import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AttendanceStatus
import com.farmsaathi.core.model.InputProduct
import com.farmsaathi.core.model.InputPurchase
import com.farmsaathi.core.model.MachineryBillingMode
import com.farmsaathi.core.model.MachinerySession
import com.farmsaathi.core.model.MachineryUsageType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.StockMovement
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.core.model.Worker
import com.farmsaathi.core.model.WorkerAttendance
import com.farmsaathi.core.model.WorkerBalanceSummary
import com.farmsaathi.core.model.WorkerPayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class BatchAttendanceItem(
    val workerId: Long,
    val status: AttendanceStatus,
    val cropCycleId: Long? = null,
    val plotId: Long? = null,
    val notes: String? = null,
)

interface LabourRepository {
    fun observeWorkersWithBalance(): Flow<List<WorkerBalanceSummary>>

    fun observeWorker(workerId: Long): Flow<Worker?>

    fun observeWorkerAttendance(workerId: Long): Flow<List<WorkerAttendance>>

    fun observeAttendanceForDay(workDateEpochDay: Long): Flow<List<WorkerAttendance>>

    fun observeWorkerPayments(workerId: Long): Flow<List<WorkerPayment>>

    suspend fun saveWorker(worker: Worker): AppResult<Long>

    suspend fun archiveWorker(workerId: Long): AppResult<Unit>

    suspend fun adjustWorkerDaysTenths(
        workerId: Long,
        deltaTenths: Int,
    ): AppResult<Int>

    suspend fun setWorkerDaysTenths(
        workerId: Long,
        daysTenths: Int,
    ): AppResult<Unit>

    suspend fun upsertAttendance(attendance: WorkerAttendance): AppResult<Long>

    suspend fun markBatchAttendance(
        farmId: Long,
        workDateEpochDay: Long,
        items: List<BatchAttendanceItem>,
    ): AppResult<Int> = AppResult.Success(0)

    suspend fun repeatYesterdayAttendance(
        targetEpochDay: Long,
        farmId: Long? = null,
    ): AppResult<Int> = AppResult.Success(0)

    suspend fun recordWorkerPayment(payment: WorkerPayment): AppResult<Long>

    suspend fun voidWorkerPayment(
        paymentId: Long,
        reason: String,
    ): AppResult<Unit>
}

data class StartMachinerySessionRequest(
    val farmId: Long,
    val plotId: Long?,
    val cropCycleId: Long?,
    val providerContactId: Long?,
    val machineName: String,
    val operatorName: String?,
    val operationType: String,
    val usageType: MachineryUsageType,
    val billingMode: MachineryBillingMode,
    val hourlyRate: MoneyPaise?,
    val perAcreRate: MoneyPaise?,
    val notes: String?,
)

interface MachineryRepository {
    fun observeRunningSessions(): Flow<List<MachinerySession>>

    fun observeSessions(cropCycleId: Long? = null): Flow<List<MachinerySession>>

    fun observeSession(sessionId: Long): Flow<MachinerySession?>

    suspend fun startLiveSession(request: StartMachinerySessionRequest): AppResult<Long>

    suspend fun stopLiveSession(
        sessionId: Long,
        overrideElapsedMinutes: Long?,
        areaCoveredMicroAcres: Long?,
        flatAmount: MoneyPaise?,
        paidAmount: MoneyPaise,
        paymentMode: PaymentMode,
        notes: String?,
    ): AppResult<MachinerySession>

    suspend fun cancelLiveSession(sessionId: Long): AppResult<Unit>

    suspend fun saveManualSession(
        session: MachinerySession,
        paymentMode: PaymentMode,
    ): AppResult<Long>
}

interface InventoryRepository {
    fun observeProducts(includeArchived: Boolean = false): Flow<List<InputProduct>>

    fun observeProduct(productId: Long): Flow<InputProduct?>

    fun observePurchases(productId: Long? = null): Flow<List<InputPurchase>>

    fun observeMovements(productId: Long): Flow<List<StockMovement>>

    fun observeAllMovements(): Flow<List<StockMovement>> = emptyFlow()

    suspend fun saveProduct(product: InputProduct): AppResult<Long>

    suspend fun archiveProduct(productId: Long): AppResult<Unit>

    suspend fun recordPurchase(purchase: InputPurchase): AppResult<Long>

    suspend fun recordStockAdjustmentOrUsage(
        productId: Long,
        farmId: Long,
        plotId: Long?,
        cropCycleId: Long?,
        signedDeltaMilliUnits: Long,
        reason: StockMovementReason,
        notes: String?,
    ): AppResult<Long>

    suspend fun recordStockAdjustmentOrUsage(
        productId: Long,
        farmId: Long,
        plotId: Long?,
        cropCycleId: Long?,
        signedDeltaMilliUnits: Long,
        reason: StockMovementReason,
        notes: String?,
        photoPath: String?,
    ): AppResult<Long> =
        recordStockAdjustmentOrUsage(
            productId = productId,
            farmId = farmId,
            plotId = plotId,
            cropCycleId = cropCycleId,
            signedDeltaMilliUnits = signedDeltaMilliUnits,
            reason = reason,
            notes = notes,
        )
}
