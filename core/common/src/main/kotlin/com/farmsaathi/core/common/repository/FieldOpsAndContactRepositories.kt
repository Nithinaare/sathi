package com.farmsaathi.core.common.repository

import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.Contact
import com.farmsaathi.core.model.ContactType
import com.farmsaathi.core.model.HarvestRecord
import com.farmsaathi.core.model.IrrigationLog
import com.farmsaathi.core.model.SoilTestRecord
import com.farmsaathi.core.model.StorageRecord
import kotlinx.coroutines.flow.Flow

interface FieldOpsRepository {
    fun observeIrrigationLogs(
        plotId: Long? = null,
        cropCycleId: Long? = null,
    ): Flow<List<IrrigationLog>>

    suspend fun getIrrigationLog(logId: Long): IrrigationLog?

    suspend fun saveIrrigationLog(log: IrrigationLog): AppResult<Long>

    fun observeHarvestRecords(cropCycleId: Long? = null): Flow<List<HarvestRecord>>

    suspend fun getHarvestRecord(harvestId: Long): HarvestRecord?

    suspend fun saveHarvestRecord(record: HarvestRecord): AppResult<Long>

    fun observeStorageRecords(cropCycleId: Long? = null): Flow<List<StorageRecord>>

    suspend fun getStorageRecord(storageId: Long): StorageRecord?

    suspend fun saveStorageRecord(record: StorageRecord): AppResult<Long>

    fun observeSoilTests(plotId: Long? = null): Flow<List<SoilTestRecord>>

    suspend fun getSoilTest(testId: Long): SoilTestRecord?

    suspend fun saveSoilTest(record: SoilTestRecord): AppResult<Long>
}

data class GlobalSearchHit(
    val id: Long,
    val type: SearchHitType,
    val title: String,
    val subtitle: String,
    val timestampMillis: Long,
)

enum class SearchHitType {
    FARM,
    PLOT,
    CROP,
    DIARY,
    LEDGER,
    WORKER,
    CONTACT,
    INVENTORY,
}

data class DataOverviewCounts(
    val farms: Int,
    val plots: Int,
    val cropCycles: Int,
    val diaryEntries: Int,
    val ledgerEntries: Int,
    val workers: Int,
    val contacts: Int,
)

interface ContactAndSearchRepository {
    fun observeContacts(
        typeFilter: ContactType? = null,
        query: String = "",
    ): Flow<List<Contact>>

    fun observeContact(contactId: Long): Flow<Contact?>

    suspend fun saveContact(contact: Contact): AppResult<Long>

    suspend fun archiveContact(contactId: Long): AppResult<Unit>

    suspend fun searchOffline(query: String): List<GlobalSearchHit>

    suspend fun getDataOverviewCounts(): DataOverviewCounts
}
