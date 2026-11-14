package com.farmsaathi.core.database.repository

import com.farmsaathi.core.common.SqlSearchUtils
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.DataOverviewCounts
import com.farmsaathi.core.common.repository.FieldOpsRepository
import com.farmsaathi.core.common.repository.GlobalSearchHit
import com.farmsaathi.core.common.repository.SearchHitType
import com.farmsaathi.core.database.DatabaseMaintenanceCoordinator
import com.farmsaathi.core.database.dao.ContactDao
import com.farmsaathi.core.database.dao.CropDao
import com.farmsaathi.core.database.dao.DiaryDao
import com.farmsaathi.core.database.dao.FarmDao
import com.farmsaathi.core.database.dao.FieldOpsDao
import com.farmsaathi.core.database.dao.FinanceDao
import com.farmsaathi.core.database.dao.InventoryDao
import com.farmsaathi.core.database.dao.LabourDao
import com.farmsaathi.core.database.dao.PlotDao
import com.farmsaathi.core.database.entity.ContactEntity
import com.farmsaathi.core.database.entity.CropTaskEntity
import com.farmsaathi.core.database.entity.HarvestRecordEntity
import com.farmsaathi.core.database.entity.IrrigationLogEntity
import com.farmsaathi.core.database.entity.SoilTestRecordEntity
import com.farmsaathi.core.database.entity.StorageRecordEntity
import com.farmsaathi.core.database.mapper.toDomain
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.Contact
import com.farmsaathi.core.model.ContactType
import com.farmsaathi.core.model.DomainError
import com.farmsaathi.core.model.HarvestRecord
import com.farmsaathi.core.model.IrrigationLog
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.SoilTestRecord
import com.farmsaathi.core.model.StorageRecord
import com.farmsaathi.core.model.TaskCategory
import com.farmsaathi.core.model.TaskPriority
import com.farmsaathi.core.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomFieldOpsRepository
@Inject
constructor(
    private val fieldOpsDao: FieldOpsDao,
    private val timeProvider: TimeProvider,
    private val coordinator: DatabaseMaintenanceCoordinator,
    private val cropDao: CropDao,
) : FieldOpsRepository {
    override fun observeIrrigationLogs(
        plotId: Long?,
        cropCycleId: Long?,
    ): Flow<List<IrrigationLog>> = fieldOpsDao.observeIrrigationLogs(plotId, cropCycleId).map { list -> list.map { it.toDomain() } }

    override suspend fun getIrrigationLog(logId: Long): IrrigationLog? = fieldOpsDao.getIrrigationLog(logId)?.toDomain()

    override suspend fun saveIrrigationLog(log: IrrigationLog): AppResult<Long> =
        coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val existing = if (log.id != 0L) fieldOpsDao.getIrrigationLog(log.id) else null
            val entity =
                IrrigationLogEntity(
                    id = log.id,
                    farmId = log.farmId,
                    plotId = log.plotId,
                    cropCycleId = log.cropCycleId,
                    irrigationDateEpochMillis = log.irrigationDateEpochMillis,
                    durationMinutes = log.durationMinutes,
                    source = log.source,
                    method = log.method,
                    nextWateringEpochDay = log.nextWateringEpochDay,
                    motorHoursOrFuelNote = log.motorHoursOrFuelNote?.trim(),
                    photoPath = log.photoPath ?: existing?.photoPath,
                    notes = log.notes?.trim(),
                    isArchived = log.isArchived,
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                )
            val id =
                if (log.id == 0L) {
                    fieldOpsDao.insertIrrigationLog(entity)
                } else {
                    fieldOpsDao.updateIrrigationLog(entity)
                    log.id
                }
            upsertNextWateringReminder(log = log, logId = id, nowMillis = now)
            AppResult.Success(id)
        }

    private suspend fun upsertNextWateringReminder(
        log: IrrigationLog,
        logId: Long,
        nowMillis: Long,
    ) {
        val nextDay = log.nextWateringEpochDay ?: return
        val stageKey = "auto_irrigation_$logId"
        val existingTask = cropDao.getTaskByStageKeyGlobal(stageKey)
        if (existingTask == null) {
            cropDao.insertTask(
                CropTaskEntity(
                    farmId = log.farmId,
                    plotId = log.plotId,
                    cropCycleId = log.cropCycleId,
                    title = "తదుపరి నీటి తడి",
                    category = TaskCategory.IRRIGATION,
                    dueDateEpochDay = nextDay,
                    sourceTemplateStageKey = stageKey,
                    priority = TaskPriority.MEDIUM,
                    reminderEnabled = true,
                    createdAtEpochMillis = nowMillis,
                    updatedAtEpochMillis = nowMillis,
                ),
            )
        } else if (existingTask.status == TaskStatus.PENDING || existingTask.status == TaskStatus.RESCHEDULED) {
            cropDao.updateTask(
                existingTask.copy(
                    plotId = log.plotId,
                    cropCycleId = log.cropCycleId,
                    dueDateEpochDay = nextDay,
                    updatedAtEpochMillis = nowMillis,
                ),
            )
        }
    }

    override fun observeHarvestRecords(cropCycleId: Long?): Flow<List<HarvestRecord>> =
        fieldOpsDao.observeHarvestRecords(cropCycleId).map { list -> list.map { it.toDomain() } }

    override suspend fun getHarvestRecord(harvestId: Long): HarvestRecord? = fieldOpsDao.getHarvestRecord(harvestId)?.toDomain()

    override suspend fun saveHarvestRecord(record: HarvestRecord): AppResult<Long> {
        if (record.quantityMilliUnits <= 0L) return AppResult.Error(DomainError.InvalidQuantity)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val existing = if (record.id != 0L) fieldOpsDao.getHarvestRecord(record.id) else null
            val entity =
                HarvestRecordEntity(
                    id = record.id,
                    farmId = record.farmId,
                    plotId = record.plotId,
                    cropCycleId = record.cropCycleId,
                    harvestDateEpochDay = record.harvestDateEpochDay,
                    quantityMilliUnits = record.quantityMilliUnits,
                    quantityUnit = record.quantityUnit,
                    bagCount = record.bagCount,
                    moisturePercentTenths = record.moisturePercentTenths,
                    qualityGrade = record.qualityGrade?.trim(),
                    notes = record.notes?.trim(),
                    photoPath = record.photoPath ?: existing?.photoPath,
                    isArchived = record.isArchived,
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                )
            val id =
                if (record.id == 0L) {
                    fieldOpsDao.insertHarvestRecord(entity)
                } else {
                    fieldOpsDao.updateHarvestRecord(entity)
                    record.id
                }
            AppResult.Success(id)
        }
    }

    override fun observeStorageRecords(cropCycleId: Long?): Flow<List<StorageRecord>> =
        fieldOpsDao.observeStorageRecords(cropCycleId).map { list -> list.map { it.toDomain() } }

    override suspend fun getStorageRecord(storageId: Long): StorageRecord? = fieldOpsDao.getStorageRecord(storageId)?.toDomain()

    override suspend fun saveStorageRecord(record: StorageRecord): AppResult<Long> {
        if (record.storageLocation.isBlank()) return AppResult.Error(DomainError.InvalidName)
        if (record.quantityMilliUnits <= 0L) return AppResult.Error(DomainError.InvalidQuantity)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val entity =
                StorageRecordEntity(
                    id = record.id,
                    farmId = record.farmId,
                    cropCycleId = record.cropCycleId,
                    harvestRecordId = record.harvestRecordId,
                    storageLocation = record.storageLocation.trim(),
                    quantityMilliUnits = record.quantityMilliUnits,
                    remainingQuantityMilliUnits = record.remainingQuantityMilliUnits.coerceIn(0L, record.quantityMilliUnits),
                    quantityUnit = record.quantityUnit,
                    storedDateEpochDay = record.storedDateEpochDay,
                    bagCount = record.bagCount,
                    status = record.status,
                    notes = record.notes?.trim(),
                    isArchived = record.isArchived,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
            val id =
                if (record.id == 0L) {
                    fieldOpsDao.insertStorageRecord(entity)
                } else {
                    fieldOpsDao.updateStorageRecord(entity)
                    record.id
                }
            AppResult.Success(id)
        }
    }

    override fun observeSoilTests(plotId: Long?): Flow<List<SoilTestRecord>> =
        fieldOpsDao.observeSoilTests(plotId).map { list -> list.map { it.toDomain() } }

    override suspend fun getSoilTest(testId: Long): SoilTestRecord? = fieldOpsDao.getSoilTest(testId)?.toDomain()

    override suspend fun saveSoilTest(record: SoilTestRecord): AppResult<Long> =
        coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val entity =
                SoilTestRecordEntity(
                    id = record.id,
                    farmId = record.farmId,
                    plotId = record.plotId,
                    testDateEpochDay = record.testDateEpochDay,
                    labName = record.labName?.trim(),
                    sampleNumber = record.sampleNumber?.trim(),
                    phTenths = record.phTenths,
                    ecMilliSiemensPerCmHundredths = record.ecMilliSiemensPerCmHundredths,
                    organicCarbonPercentHundredths = record.organicCarbonPercentHundredths,
                    nitrogenKgPerHaTenths = record.nitrogenKgPerHaTenths,
                    phosphorusKgPerHaTenths = record.phosphorusKgPerHaTenths,
                    potassiumKgPerHaTenths = record.potassiumKgPerHaTenths,
                    overallRating = record.overallRating,
                    reportPhotoPath = record.reportPhotoPath,
                    recommendationsNote = record.recommendationsNote?.trim(),
                    isArchived = record.isArchived,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
            val id =
                if (record.id == 0L) {
                    fieldOpsDao.insertSoilTest(entity)
                } else {
                    fieldOpsDao.updateSoilTest(entity)
                    record.id
                }
            AppResult.Success(id)
        }
}

@Singleton
class RoomContactAndSearchRepository
@Inject
constructor(
    private val farmDao: FarmDao,
    private val plotDao: PlotDao,
    private val cropDao: CropDao,
    private val diaryDao: DiaryDao,
    private val financeDao: FinanceDao,
    private val labourDao: LabourDao,
    private val inventoryDao: InventoryDao,
    private val contactDao: ContactDao,
    private val timeProvider: TimeProvider,
    private val coordinator: DatabaseMaintenanceCoordinator,
) : ContactAndSearchRepository {
    override fun observeContacts(
        typeFilter: ContactType?,
        query: String,
    ): Flow<List<Contact>> =
        contactDao.observeContacts(typeFilter, SqlSearchUtils.escapeSqlLike(query))
            .map { list -> list.map { it.toDomain() } }

    override fun observeContact(contactId: Long): Flow<Contact?> = contactDao.observeContact(contactId).map { it?.toDomain() }

    override suspend fun saveContact(contact: Contact): AppResult<Long> {
        if (contact.name.isBlank()) return AppResult.Error(DomainError.InvalidName)
        return coordinator.withExclusiveLock {
            val now = timeProvider.nowEpochMillis()
            val entity =
                ContactEntity(
                    id = contact.id,
                    name = contact.name.trim(),
                    phone = contact.phone?.trim(),
                    contactType = contact.contactType,
                    villageOrMandi = contact.villageOrMandi?.trim(),
                    upiId = contact.upiId?.trim(),
                    voiceNamePath = contact.voiceNamePath,
                    notes = contact.notes?.trim(),
                    isArchived = contact.isArchived,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
            val id =
                if (contact.id == 0L) {
                    contactDao.insertContact(entity)
                } else {
                    contactDao.updateContact(entity)
                    contact.id
                }
            AppResult.Success(id)
        }
    }

    override suspend fun archiveContact(contactId: Long): AppResult<Unit> =
        coordinator.withExclusiveLock {
            contactDao.archiveContact(contactId, timeProvider.nowEpochMillis())
            AppResult.Success(Unit)
        }

    override suspend fun searchOffline(query: String): List<GlobalSearchHit> {
        val escaped = SqlSearchUtils.escapeSqlLike(query)
        if (escaped.isEmpty()) return emptyList()
        val hits = mutableListOf<GlobalSearchHit>()
        val perTableLimit = 10
        farmDao.searchFarms(escaped, perTableLimit).forEach {
            hits += GlobalSearchHit(it.id, SearchHitType.FARM, it.name, it.villageOrLocation.orEmpty(), it.updatedAtEpochMillis)
        }
        plotDao.searchPlots(escaped, perTableLimit).forEach {
            hits += GlobalSearchHit(it.id, SearchHitType.PLOT, it.name, it.soilType.orEmpty(), it.updatedAtEpochMillis)
        }
        cropDao.searchCropCycles(escaped, perTableLimit).forEach {
            hits += GlobalSearchHit(it.id, SearchHitType.CROP, it.cropName, it.variety.orEmpty(), it.updatedAtEpochMillis)
        }
        diaryDao.searchDiary(escaped, perTableLimit).forEach {
            hits += GlobalSearchHit(it.id, SearchHitType.DIARY, it.title, it.notes.orEmpty(), it.entryDateEpochMillis)
        }
        financeDao.searchLedger(escaped, perTableLimit).forEach {
            val amountStr = MoneyPaise(it.totalAmountPaise).formatInr()
            hits += GlobalSearchHit(it.id, SearchHitType.LEDGER, it.title, amountStr, it.transactionDateEpochMillis)
        }
        labourDao.searchWorkers(escaped, perTableLimit).forEach {
            hits += GlobalSearchHit(it.id, SearchHitType.WORKER, it.name, it.phone.orEmpty(), it.updatedAtEpochMillis)
        }
        contactDao.searchContacts(escaped, perTableLimit).forEach {
            hits += GlobalSearchHit(it.id, SearchHitType.CONTACT, it.name, it.phone.orEmpty(), it.updatedAtEpochMillis)
        }
        inventoryDao.searchProducts(escaped, perTableLimit).forEach {
            hits += GlobalSearchHit(it.id, SearchHitType.INVENTORY, it.name, it.category.name, it.updatedAtEpochMillis)
        }
        return hits.sortedByDescending { it.timestampMillis }
    }

    override suspend fun getDataOverviewCounts(): DataOverviewCounts =
        DataOverviewCounts(
            farms = farmDao.countActiveFarms(),
            plots = plotDao.countActivePlots(),
            cropCycles = cropDao.countActiveCropCycles(),
            diaryEntries = diaryDao.countActiveDiaryEntries(),
            ledgerEntries = financeDao.countActiveLedgerEntries(),
            workers = labourDao.countActiveWorkers(),
            contacts = contactDao.countActiveContacts(),
        )
}
