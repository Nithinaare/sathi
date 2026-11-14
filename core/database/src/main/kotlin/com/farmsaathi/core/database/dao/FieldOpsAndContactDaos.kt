package com.farmsaathi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.farmsaathi.core.database.entity.ContactEntity
import com.farmsaathi.core.database.entity.HarvestRecordEntity
import com.farmsaathi.core.database.entity.IrrigationLogEntity
import com.farmsaathi.core.database.entity.SoilTestRecordEntity
import com.farmsaathi.core.database.entity.StorageRecordEntity
import com.farmsaathi.core.model.ContactType
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldOpsDao {
    @Query(
        """
        SELECT * FROM irrigation_logs
        WHERE isArchived = 0
          AND (:plotId IS NULL OR plotId = :plotId)
          AND (:cropCycleId IS NULL OR cropCycleId = :cropCycleId)
        ORDER BY irrigationDateEpochMillis DESC, id DESC
        """,
    )
    fun observeIrrigationLogs(
        plotId: Long?,
        cropCycleId: Long?,
    ): Flow<List<IrrigationLogEntity>>

    @Query("SELECT * FROM irrigation_logs WHERE id = :id")
    suspend fun getIrrigationLog(id: Long): IrrigationLogEntity?

    @Insert
    suspend fun insertIrrigationLog(log: IrrigationLogEntity): Long

    @Update
    suspend fun updateIrrigationLog(log: IrrigationLogEntity)

    @Query(
        """
        SELECT * FROM harvest_records
        WHERE isArchived = 0
          AND (:cropCycleId IS NULL OR cropCycleId = :cropCycleId)
        ORDER BY harvestDateEpochDay DESC, id DESC
        """,
    )
    fun observeHarvestRecords(cropCycleId: Long?): Flow<List<HarvestRecordEntity>>

    @Query("SELECT * FROM harvest_records WHERE isArchived = 0")
    fun observeAllActiveHarvestRecords(): Flow<List<HarvestRecordEntity>>

    @Query("SELECT * FROM harvest_records WHERE id = :id")
    suspend fun getHarvestRecord(id: Long): HarvestRecordEntity?

    @Insert
    suspend fun insertHarvestRecord(record: HarvestRecordEntity): Long

    @Update
    suspend fun updateHarvestRecord(record: HarvestRecordEntity)

    @Query(
        """
        SELECT * FROM storage_records
        WHERE isArchived = 0
          AND (:cropCycleId IS NULL OR cropCycleId = :cropCycleId)
        ORDER BY storedDateEpochDay DESC, id DESC
        """,
    )
    fun observeStorageRecords(cropCycleId: Long?): Flow<List<StorageRecordEntity>>

    @Query("SELECT * FROM storage_records WHERE id = :id")
    suspend fun getStorageRecord(id: Long): StorageRecordEntity?

    @Insert
    suspend fun insertStorageRecord(record: StorageRecordEntity): Long

    @Update
    suspend fun updateStorageRecord(record: StorageRecordEntity)

    @Query(
        "SELECT * FROM soil_test_records WHERE isArchived = 0 AND (:plotId IS NULL OR plotId = :plotId) ORDER BY testDateEpochDay DESC, id DESC",
    )
    fun observeSoilTests(plotId: Long?): Flow<List<SoilTestRecordEntity>>

    @Query("SELECT * FROM soil_test_records WHERE id = :id")
    suspend fun getSoilTest(id: Long): SoilTestRecordEntity?

    @Insert
    suspend fun insertSoilTest(record: SoilTestRecordEntity): Long

    @Update
    suspend fun updateSoilTest(record: SoilTestRecordEntity)
}

@Dao
interface ContactDao {
    @Query(
        """
        SELECT * FROM contacts
        WHERE isArchived = 0
          AND (:typeFilter IS NULL OR contactType = :typeFilter)
          AND (:escaped = '' OR name LIKE '%' || :escaped || '%' ESCAPE '\'
                             OR phone LIKE '%' || :escaped || '%' ESCAPE '\'
                             OR villageOrMandi LIKE '%' || :escaped || '%' ESCAPE '\')
        ORDER BY name ASC
        """,
    )
    fun observeContacts(
        typeFilter: ContactType?,
        escaped: String,
    ): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    fun observeContact(contactId: Long): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContact(contactId: Long): ContactEntity?

    @Insert
    suspend fun insertContact(contact: ContactEntity): Long

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET isArchived = 1, updatedAtEpochMillis = :nowMillis WHERE id = :contactId")
    suspend fun archiveContact(
        contactId: Long,
        nowMillis: Long,
    )

    @Query(
        """
        SELECT * FROM contacts
        WHERE isArchived = 0
          AND (name LIKE '%' || :escaped || '%' ESCAPE '\'
               OR phone LIKE '%' || :escaped || '%' ESCAPE '\')
        LIMIT :limit
        """,
    )
    suspend fun searchContacts(
        escaped: String,
        limit: Int,
    ): List<ContactEntity>

    @Query("SELECT COUNT(*) FROM contacts WHERE isArchived = 0")
    suspend fun countActiveContacts(): Int
}
