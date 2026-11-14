package com.farmsaathi.core.database.repository

import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.BatchAttendanceItem
import com.farmsaathi.core.common.repository.InventoryRepository
import com.farmsaathi.core.common.repository.LabourRepository
import com.farmsaathi.core.common.repository.MachineryRepository
import com.farmsaathi.core.common.repository.StartMachinerySessionRequest
import com.farmsaathi.core.database.DatabaseMaintenanceCoordinator
import com.farmsaathi.core.database.dao.InventoryDao
import com.farmsaathi.core.database.dao.LabourDao
import com.farmsaathi.core.database.dao.MachineryDao
import com.farmsaathi.core.database.entity.InputProductEntity
import com.farmsaathi.core.database.entity.InputPurchaseEntity
import com.farmsaathi.core.database.entity.MachinerySessionEntity
import com.farmsaathi.core.database.entity.WorkerAttendanceEntity
import com.farmsaathi.core.database.entity.WorkerEntity
import com.farmsaathi.core.database.entity.WorkerPaymentEntity
import com.farmsaathi.core.database.mapper.toDomain
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.DomainError
import com.farmsaathi.core.model.FinancialCalculators
import com.farmsaathi.core.model.InputProduct
import com.farmsaathi.core.model.InputPurchase
import com.farmsaathi.core.model.MachineryEntryMode
import com.farmsaathi.core.model.MachinerySession
import com.farmsaathi.core.model.MachinerySessionStatus
import com.farmsaathi.core.model.MathRounding
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.StockMovement
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.core.model.Worker
import com.farmsaathi.core.model.WorkerAttendance
import com.farmsaathi.core.model.WorkerBalanceSummary
import com.farmsaathi.core.model.WorkerPayment
import com.farmsaathi.core.model.WorkerPaymentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomLabourRepository
@Inject
constructor(
    private val labourDao: LabourDao,
    private val timeProvider: TimeProvider,
    private val coordinator: DatabaseMaintenanceCoordinator,
) : LabourRepository {
    override fun observeWorkersWithBalance(): Flow<List<WorkerBalanceSummary>> =
        combine(
            labourDao.observeActiveWorkers(),
            labourDao.observeAllAttendance(),
            labourDao.observeAllPayments(),
        ) { workers, attendanceList, paymentList ->
            val attByWorker = attendanceList.groupBy { it.workerId }
            val payByWorker = paymentList.groupBy { it.workerId }
            workers.map { workerEntity ->
                val worker = workerEntity.toDomain()
                val attendanceItems = attByWorker[worker.id].orEmpty()
                val attendanceWage =
                    attendanceItems
                        .fold(MoneyPaise.ZERO) { acc, item -> acc + MoneyPaise(item.wageAccruedPaise) }
                val attendanceDaysTenths: Int =
                    attendanceItems.sumOf { item ->
                        val daysTenths: Int =
                            when (item.status) {
                                com.farmsaathi.core.model.AttendanceStatus.FULL_DAY -> 10
                                com.farmsaathi.core.model.AttendanceStatus.HALF_DAY -> 5
                                else -> 0
                            }
                        daysTenths
                    }
                val manualWage =
                    FinancialCalculators.calculateManualDaysWage(
                        daysWorkedTenths = worker.daysWorkedTenths,
                        dailyRate = worker.defaultDailyWage,
                    )
                val totalWage = attendanceWage + manualWage
                val workerPayments = payByWorker[worker.id].orEmpty()
                val advances =
                    workerPayments.filter { it.paymentType == WorkerPaymentType.ADVANCE }
                        .fold(MoneyPaise.ZERO) { acc, p -> acc + MoneyPaise(p.amountPaise) }
                val settlements =
                    workerPayments.filter { it.paymentType == WorkerPaymentType.SETTLEMENT }
                        .fold(MoneyPaise.ZERO) { acc, p -> acc + MoneyPaise(p.amountPaise) }
                WorkerBalanceSummary(
                    worker = worker,
                    totalWageAccrued = totalWage,
                    totalAdvancesPaid = advances,
                    totalSettlementsPaid = settlements,
                    totalDaysWorkedTenths = worker.daysWorkedTenths + attendanceDaysTenths,
                )
            }
        }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override fun observeWorker(workerId: Long): Flow<Worker?> =
        labourDao.observeWorker(workerId)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override fun observeWorkerAttendance(workerId: Long): Flow<List<WorkerAttendance>> =
        labourDao.observeWorkerAttendance(workerId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override fun observeAttendanceForDay(workDateEpochDay: Long): Flow<List<WorkerAttendance>> =
        labourDao.observeAttendanceForDay(workDateEpochDay)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override fun observeWorkerPayments(workerId: Long): Flow<List<WorkerPayment>> =
        labourDao.observeWorkerPayments(workerId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override suspend fun saveWorker(worker: Worker): AppResult<Long> {
        if (worker.name.isBlank()) return AppResult.Error(DomainError.InvalidName)
        if (worker.defaultDailyWage.isNegative()) return AppResult.Error(DomainError.InvalidAmount)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val existing = if (worker.id != 0L) labourDao.getWorker(worker.id) else null
            val entity =
                WorkerEntity(
                    id = worker.id,
                    farmId = worker.farmId,
                    name = worker.name.trim(),
                    phone = worker.phone?.trim(),
                    skillRole = worker.skillRole?.trim(),
                    photoPath = worker.photoPath ?: existing?.photoPath,
                    voiceNamePath = worker.voiceNamePath ?: existing?.voiceNamePath,
                    daysWorkedTenths = existing?.daysWorkedTenths ?: worker.daysWorkedTenths.coerceAtLeast(0),
                    defaultDailyWagePaise = worker.defaultDailyWage.paise,
                    defaultHalfDayPercent = worker.defaultHalfDayPercent.coerceIn(1, 99),
                    halfDayRatePaise = worker.halfDayRatePaise,
                    notes = worker.notes?.trim(),
                    isArchived = worker.isArchived,
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                )
            val id =
                if (worker.id == 0L) {
                    labourDao.insertWorker(entity)
                } else {
                    labourDao.updateWorker(entity)
                    worker.id
                }
            AppResult.Success(id)
        }
    }

    override suspend fun adjustWorkerDaysTenths(
        workerId: Long,
        deltaTenths: Int,
    ): AppResult<Int> =
        coordinator.withExclusiveLock {
            val worker =
                labourDao.getWorker(workerId)
                    ?: return@withExclusiveLock AppResult.Error(DomainError.ValidationFailed("worker", "missing"))
            val updated = maxOf(0, worker.daysWorkedTenths + deltaTenths)
            labourDao.updateWorkerDaysTenths(workerId, updated, timeProvider.nowEpochMillis())
            AppResult.Success(updated)
        }

    override suspend fun setWorkerDaysTenths(
        workerId: Long,
        daysTenths: Int,
    ): AppResult<Unit> =
        coordinator.withExclusiveLock {
            labourDao.getWorker(workerId)
                ?: return@withExclusiveLock AppResult.Error(DomainError.ValidationFailed("worker", "missing"))
            labourDao.updateWorkerDaysTenths(workerId, daysTenths.coerceAtLeast(0), timeProvider.nowEpochMillis())
            AppResult.Success(Unit)
        }

    override suspend fun archiveWorker(workerId: Long): AppResult<Unit> =
        coordinator.withExclusiveLock {
            labourDao.archiveWorker(workerId, timeProvider.nowEpochMillis())
            AppResult.Success(Unit)
        }

    override suspend fun upsertAttendance(attendance: WorkerAttendance): AppResult<Long> =
        coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val worker = labourDao.getWorker(attendance.workerId)
            val accrued =
                FinancialCalculators.calculateWageAccrued(
                    status = attendance.status,
                    baseDailyWage = attendance.baseDailyWage,
                    halfDayPercent = attendance.halfDayPercent,
                    pieceUnitRate = attendance.pieceUnitRate,
                    pieceQuantityMilliUnits = attendance.pieceQuantityMilliUnits,
                    halfDayRateOverride = worker?.halfDayRatePaise?.let(::MoneyPaise),
                )
            val id =
                labourDao.upsertAttendanceAtomic(
                    WorkerAttendanceEntity(
                        id = attendance.id,
                        workerId = attendance.workerId,
                        farmId = attendance.farmId,
                        plotId = attendance.plotId,
                        cropCycleId = attendance.cropCycleId,
                        workDateEpochDay = attendance.workDateEpochDay,
                        status = attendance.status,
                        baseDailyWagePaise = attendance.baseDailyWage.paise,
                        halfDayPercent = attendance.halfDayPercent.coerceIn(1, 99),
                        pieceRateUnit = attendance.pieceRateUnit,
                        pieceQuantityMilliUnits = attendance.pieceQuantityMilliUnits,
                        pieceUnitRatePaise = attendance.pieceUnitRate?.paise,
                        wageAccruedPaise = accrued.paise,
                        notes = attendance.notes?.trim(),
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                    now,
                )
            AppResult.Success(id)
        }

    override suspend fun markBatchAttendance(
        farmId: Long,
        workDateEpochDay: Long,
        items: List<BatchAttendanceItem>,
    ): AppResult<Int> {
        if (items.isEmpty()) return AppResult.Success(0)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val entities =
                items.mapNotNull { item ->
                    val worker = labourDao.getWorker(item.workerId)
                    val resolvedFarmId = if (farmId > 0L) farmId else worker?.farmId
                    if (worker == null || worker.isArchived || resolvedFarmId == null || resolvedFarmId <= 0L) {
                        null
                    } else {
                        val baseWage = MoneyPaise(worker.defaultDailyWagePaise)
                        val accrued =
                            FinancialCalculators.calculateWageAccrued(
                                status = item.status,
                                baseDailyWage = baseWage,
                                halfDayPercent = worker.defaultHalfDayPercent,
                                halfDayRateOverride = worker.halfDayRatePaise?.let(::MoneyPaise),
                            )
                        WorkerAttendanceEntity(
                            workerId = worker.id,
                            farmId = resolvedFarmId,
                            plotId = item.plotId,
                            cropCycleId = item.cropCycleId,
                            workDateEpochDay = workDateEpochDay,
                            status = item.status,
                            baseDailyWagePaise = baseWage.paise,
                            halfDayPercent = worker.defaultHalfDayPercent.coerceIn(1, 99),
                            wageAccruedPaise = accrued.paise,
                            notes = item.notes?.trim(),
                            createdAtEpochMillis = now,
                            updatedAtEpochMillis = now,
                        )
                    }
                }
            val count = if (entities.isEmpty()) 0 else labourDao.upsertBatchAttendanceAtomic(entities, now)
            AppResult.Success(count)
        }
    }

    override suspend fun repeatYesterdayAttendance(
        targetEpochDay: Long,
        farmId: Long?,
    ): AppResult<Int> =
        coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val yesterdayRows =
                labourDao.getAttendanceForDay(targetEpochDay - 1L)
                    .filter { farmId == null || it.farmId == farmId }
            val alreadyMarkedIds =
                labourDao.getAttendanceForDay(targetEpochDay)
                    .map { it.workerId }
                    .toSet()
            val toInsert =
                yesterdayRows.mapNotNull { prev ->
                    if (prev.workerId in alreadyMarkedIds) {
                        null
                    } else {
                        val worker = labourDao.getWorker(prev.workerId)
                        if (worker == null || worker.isArchived) {
                            null
                        } else {
                            val baseWage = MoneyPaise(worker.defaultDailyWagePaise)
                            val accrued =
                                FinancialCalculators.calculateWageAccrued(
                                    status = prev.status,
                                    baseDailyWage = baseWage,
                                    halfDayPercent = worker.defaultHalfDayPercent,
                                    pieceUnitRate = prev.pieceUnitRatePaise?.let(::MoneyPaise),
                                    pieceQuantityMilliUnits = prev.pieceQuantityMilliUnits,
                                    halfDayRateOverride = worker.halfDayRatePaise?.let(::MoneyPaise),
                                )
                            WorkerAttendanceEntity(
                                workerId = worker.id,
                                farmId = prev.farmId,
                                plotId = prev.plotId,
                                cropCycleId = prev.cropCycleId,
                                workDateEpochDay = targetEpochDay,
                                status = prev.status,
                                baseDailyWagePaise = baseWage.paise,
                                halfDayPercent = worker.defaultHalfDayPercent.coerceIn(1, 99),
                                pieceRateUnit = prev.pieceRateUnit,
                                pieceQuantityMilliUnits = prev.pieceQuantityMilliUnits,
                                pieceUnitRatePaise = prev.pieceUnitRatePaise,
                                wageAccruedPaise = accrued.paise,
                                notes = prev.notes,
                                createdAtEpochMillis = now,
                                updatedAtEpochMillis = now,
                            )
                        }
                    }
                }
            val count = if (toInsert.isEmpty()) 0 else labourDao.upsertBatchAttendanceAtomic(toInsert, now)
            AppResult.Success(count)
        }

    override suspend fun recordWorkerPayment(payment: WorkerPayment): AppResult<Long> {
        if (!payment.amount.isPositive()) return AppResult.Error(DomainError.InvalidAmount)
        return coordinator.withExclusiveLock {
            val worker =
                labourDao.getWorker(payment.workerId)
                    ?: return@withExclusiveLock AppResult.Error(DomainError.ValidationFailed("worker", "missing"))
            val now = timeProvider.nowEpochMillis()
            val id =
                labourDao.recordWorkerPaymentAtomic(
                    payment =
                    WorkerPaymentEntity(
                        workerId = payment.workerId,
                        farmId = payment.farmId,
                        cropCycleId = payment.cropCycleId,
                        paymentType = payment.paymentType,
                        amountPaise = payment.amount.paise,
                        paymentDateEpochMillis = payment.paymentDateEpochMillis,
                        paymentMode = payment.paymentMode,
                        notes = payment.notes?.trim(),
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                    workerName = worker.name,
                    nowMillis = now,
                )
            AppResult.Success(id)
        }
    }

    override suspend fun voidWorkerPayment(
        paymentId: Long,
        reason: String,
    ): AppResult<Unit> =
        coordinator.withExclusiveLock {
            labourDao.voidWorkerPaymentAtomic(paymentId, reason.trim(), timeProvider.nowEpochMillis())
            AppResult.Success(Unit)
        }
}

@Singleton
class RoomMachineryRepository
@Inject
constructor(
    private val machineryDao: MachineryDao,
    private val timeProvider: TimeProvider,
    private val coordinator: DatabaseMaintenanceCoordinator,
) : MachineryRepository {
    override fun observeRunningSessions(): Flow<List<MachinerySession>> =
        machineryDao.observeRunningSessions()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override fun observeSessions(cropCycleId: Long?): Flow<List<MachinerySession>> =
        machineryDao.observeSessions(cropCycleId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override fun observeSession(sessionId: Long): Flow<MachinerySession?> =
        machineryDao.observeSession(sessionId)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override suspend fun startLiveSession(request: StartMachinerySessionRequest): AppResult<Long> {
        if (request.machineName.isBlank() || request.operationType.isBlank()) {
            return AppResult.Error(DomainError.InvalidName)
        }
        val equipmentKey = request.machineName.trim().lowercase()
        val lockKey = "${request.farmId}:$equipmentKey"
        return coordinator.withExclusiveLock {
            if (machineryDao.findRunningByLockKey(lockKey) != null) {
                return@withExclusiveLock AppResult.Error(DomainError.ActiveMachinerySessionExists)
            }
            val now = timeProvider.nowEpochMillis()
            val id =
                machineryDao.insertSession(
                    MachinerySessionEntity(
                        farmId = request.farmId,
                        plotId = request.plotId,
                        cropCycleId = request.cropCycleId,
                        providerContactId = request.providerContactId,
                        machineName = request.machineName.trim(),
                        operatorName = request.operatorName?.trim(),
                        operationType = request.operationType.trim(),
                        equipmentKey = equipmentKey,
                        activeLockKey = lockKey,
                        usageType = request.usageType,
                        billingMode = request.billingMode,
                        entryMode = MachineryEntryMode.LIVE_TIMER,
                        status = MachinerySessionStatus.RUNNING,
                        startEpochMillis = now,
                        startElapsedRealtimeMillis = timeProvider.elapsedRealtimeMillis(),
                        startBootCount = timeProvider.bootCount(),
                        hourlyRatePaise = request.hourlyRate?.paise,
                        perAcreRatePaise = request.perAcreRate?.paise,
                        notes = request.notes?.trim(),
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )
            AppResult.Success(id)
        }
    }

    override suspend fun stopLiveSession(
        sessionId: Long,
        overrideElapsedMinutes: Long?,
        areaCoveredMicroAcres: Long?,
        flatAmount: MoneyPaise?,
        paidAmount: MoneyPaise,
        paymentMode: PaymentMode,
        notes: String?,
    ): AppResult<MachinerySession> =
        coordinator.withExclusiveLock {
            val current =
                machineryDao.getSession(sessionId)
                    ?: return@withExclusiveLock AppResult.Error(DomainError.MachinerySessionNotFound)
            if (current.status != MachinerySessionStatus.RUNNING) {
                return@withExclusiveLock AppResult.Success(current.toDomain())
            }

            val now = timeProvider.nowEpochMillis()
            val nowElapsed = timeProvider.elapsedRealtimeMillis()
            val nowBoot = timeProvider.bootCount()
            val elapsedMs =
                FinancialCalculators.computeMachineryElapsedMillis(
                    startEpochMillis = current.startEpochMillis,
                    startElapsedRealtimeMillis = current.startElapsedRealtimeMillis,
                    startBootCount = current.startBootCount,
                    nowEpochMillis = now,
                    nowElapsedRealtimeMillis = nowElapsed,
                    nowBootCount = nowBoot,
                )
            val computedMinutes = maxOf(0L, MathRounding.divideAndRoundHalfUp(elapsedMs, 60_000L))
            val finalMinutes = overrideElapsedMinutes?.coerceAtLeast(0L) ?: computedMinutes

            val charge =
                FinancialCalculators.computeMachineryCharge(
                    billingMode = current.billingMode,
                    elapsedMinutes = finalMinutes,
                    hourlyRate = current.hourlyRatePaise?.let(::MoneyPaise) ?: MoneyPaise.ZERO,
                    perAcreRate = current.perAcreRatePaise?.let(::MoneyPaise) ?: MoneyPaise.ZERO,
                    areaCoveredMicroAcres = areaCoveredMicroAcres ?: 0L,
                    flatAmount = flatAmount ?: MoneyPaise.ZERO,
                )
            val safePaid = paidAmount.coerceIn(MoneyPaise.ZERO, charge)
            val payStatus = FinancialCalculators.derivePaymentStatus(charge, safePaid)

            val stopped =
                current.copy(
                    activeLockKey = null,
                    status = MachinerySessionStatus.COMPLETED,
                    endEpochMillis = now,
                    endElapsedRealtimeMillis = nowElapsed,
                    elapsedMinutes = finalMinutes,
                    areaCoveredMicroAcres = areaCoveredMicroAcres,
                    computedAmountPaise = charge.paise,
                    paidAmountPaise = safePaid.paise,
                    paymentStatus = payStatus,
                    notes = notes?.trim() ?: current.notes,
                    updatedAtEpochMillis = now,
                )
            val saved = machineryDao.stopSessionAndPostLedgerAtomic(stopped, paymentMode, now)
            AppResult.Success(saved.toDomain())
        }

    override suspend fun cancelLiveSession(sessionId: Long): AppResult<Unit> =
        coordinator.withExclusiveLock {
            val current =
                machineryDao.getSession(sessionId)
                    ?: return@withExclusiveLock AppResult.Error(DomainError.MachinerySessionNotFound)
            val now = timeProvider.nowEpochMillis()
            machineryDao.updateSession(
                current.copy(
                    activeLockKey = null,
                    status = MachinerySessionStatus.CANCELLED,
                    endEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            AppResult.Success(Unit)
        }

    override suspend fun saveManualSession(
        session: MachinerySession,
        paymentMode: PaymentMode,
    ): AppResult<Long> {
        if (session.machineName.isBlank()) return AppResult.Error(DomainError.InvalidName)
        if (session.computedAmount.isNegative() || session.paidAmount.isNegative() ||
            session.paidAmount > session.computedAmount
        ) {
            return AppResult.Error(DomainError.InvalidAmount)
        }
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val payStatus = FinancialCalculators.derivePaymentStatus(session.computedAmount, session.paidAmount)
            val entity =
                MachinerySessionEntity(
                    farmId = session.farmId,
                    plotId = session.plotId,
                    cropCycleId = session.cropCycleId,
                    providerContactId = session.providerContactId,
                    machineName = session.machineName.trim(),
                    operatorName = session.operatorName?.trim(),
                    operationType = session.operationType.trim().ifEmpty { "Field Work" },
                    equipmentKey = session.machineName.trim().lowercase(),
                    activeLockKey = null,
                    usageType = session.usageType,
                    billingMode = session.billingMode,
                    entryMode = MachineryEntryMode.MANUAL_LOG,
                    status = MachinerySessionStatus.COMPLETED,
                    startEpochMillis = session.startEpochMillis,
                    endEpochMillis = session.endEpochMillis ?: session.startEpochMillis,
                    elapsedMinutes = session.elapsedMinutes,
                    hourlyRatePaise = session.hourlyRate?.paise,
                    perAcreRatePaise = session.perAcreRate?.paise,
                    areaCoveredMicroAcres = session.areaCoveredMicroAcres,
                    computedAmountPaise = session.computedAmount.paise,
                    paidAmountPaise = session.paidAmount.paise,
                    paymentStatus = payStatus,
                    notes = session.notes?.trim(),
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
            AppResult.Success(machineryDao.saveManualSessionAndLedgerAtomic(entity, paymentMode, now))
        }
    }
}

@Singleton
class RoomInventoryRepository
@Inject
constructor(
    private val inventoryDao: InventoryDao,
    private val timeProvider: TimeProvider,
    private val coordinator: DatabaseMaintenanceCoordinator,
) : InventoryRepository {
    override fun observeProducts(includeArchived: Boolean): Flow<List<InputProduct>> =
        inventoryDao.observeProducts(includeArchived)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override fun observeProduct(productId: Long): Flow<InputProduct?> =
        inventoryDao.observeProduct(productId)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override fun observePurchases(productId: Long?): Flow<List<InputPurchase>> =
        inventoryDao.observePurchases(productId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override fun observeMovements(productId: Long): Flow<List<StockMovement>> =
        inventoryDao.observeMovements(productId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override fun observeAllMovements(): Flow<List<StockMovement>> =
        inventoryDao.observeAllMovements()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()

    override suspend fun saveProduct(product: InputProduct): AppResult<Long> {
        if (product.name.isBlank()) return AppResult.Error(DomainError.InvalidName)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val entity =
                InputProductEntity(
                    id = product.id,
                    farmId = product.farmId,
                    name = product.name.trim(),
                    category = product.category,
                    brand = product.brand?.trim(),
                    unit = product.unit,
                    bagWeightMilliKg = product.bagWeightMilliKg,
                    currentStockMilliUnits = maxOf(0L, product.currentStockMilliUnits),
                    lowStockThresholdMilliUnits = maxOf(0L, product.lowStockThresholdMilliUnits),
                    defaultUnitPricePaise = product.defaultUnitPrice?.paise,
                    notes = product.notes?.trim(),
                    isArchived = product.isArchived,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
            val id =
                if (product.id == 0L) {
                    inventoryDao.insertProduct(entity)
                } else {
                    inventoryDao.updateProduct(entity)
                    product.id
                }
            AppResult.Success(id)
        }
    }

    override suspend fun archiveProduct(productId: Long): AppResult<Unit> =
        coordinator.withExclusiveLock {
            inventoryDao.archiveProduct(productId, timeProvider.nowEpochMillis())
            AppResult.Success(Unit)
        }

    override suspend fun recordPurchase(purchase: InputPurchase): AppResult<Long> {
        if (purchase.quantityMilliUnits <= 0L) return AppResult.Error(DomainError.InvalidQuantity)
        if (purchase.totalAmount.isNegative() || purchase.paidAmount.isNegative() ||
            purchase.paidAmount > purchase.totalAmount
        ) {
            return AppResult.Error(DomainError.InvalidAmount)
        }
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val status = FinancialCalculators.derivePaymentStatus(purchase.totalAmount, purchase.paidAmount)
            val entity =
                InputPurchaseEntity(
                    productId = purchase.productId,
                    farmId = purchase.farmId,
                    cropCycleId = purchase.cropCycleId,
                    vendorContactId = purchase.vendorContactId,
                    vendorNameSnapshot = purchase.vendorNameSnapshot?.trim(),
                    billNumber = purchase.billNumber?.trim(),
                    quantityMilliUnits = purchase.quantityMilliUnits,
                    unit = purchase.unit,
                    unitPricePaise = purchase.unitPrice.paise,
                    totalAmountPaise = purchase.totalAmount.paise,
                    paidAmountPaise = purchase.paidAmount.paise,
                    paymentStatus = status,
                    paymentMode = purchase.paymentMode,
                    purchaseDateEpochMillis = purchase.purchaseDateEpochMillis,
                    expiryDateEpochDay = purchase.expiryDateEpochDay,
                    dueDateEpochMillis = purchase.dueDateEpochMillis,
                    receiptPhotoPath = purchase.receiptPhotoPath,
                    notes = purchase.notes?.trim(),
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
            AppResult.Success(inventoryDao.recordPurchaseAtomic(entity, now))
        }
    }

    override suspend fun recordStockAdjustmentOrUsage(
        productId: Long,
        farmId: Long,
        plotId: Long?,
        cropCycleId: Long?,
        signedDeltaMilliUnits: Long,
        reason: StockMovementReason,
        notes: String?,
    ): AppResult<Long> =
        recordStockAdjustmentOrUsage(
            productId = productId,
            farmId = farmId,
            plotId = plotId,
            cropCycleId = cropCycleId,
            signedDeltaMilliUnits = signedDeltaMilliUnits,
            reason = reason,
            notes = notes,
            photoPath = null,
        )

    override suspend fun recordStockAdjustmentOrUsage(
        productId: Long,
        farmId: Long,
        plotId: Long?,
        cropCycleId: Long?,
        signedDeltaMilliUnits: Long,
        reason: StockMovementReason,
        notes: String?,
        photoPath: String?,
    ): AppResult<Long> {
        if (signedDeltaMilliUnits == 0L) return AppResult.Error(DomainError.InvalidQuantity)
        return coordinator.withExclusiveLock {
            val currentProduct =
                inventoryDao.getProduct(productId)
                    ?: return@withExclusiveLock AppResult.Error(DomainError.ValidationFailed("product", "missing"))
            val movementId =
                inventoryDao.recordMovementAtomic(
                    productId = productId,
                    farmId = farmId,
                    plotId = plotId,
                    cropCycleId = cropCycleId,
                    signedDeltaMilliUnits = signedDeltaMilliUnits,
                    reason = reason,
                    notes = notes?.trim(),
                    nowMillis = timeProvider.nowEpochMillis(),
                    photoPath = photoPath,
                )
            if (movementId == null) {
                AppResult.Error(DomainError.InsufficientInventory(currentProduct.currentStockMilliUnits))
            } else {
                AppResult.Success(movementId)
            }
        }
    }
}
