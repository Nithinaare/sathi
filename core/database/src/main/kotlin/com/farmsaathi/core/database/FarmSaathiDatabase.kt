package com.farmsaathi.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.farmsaathi.core.database.converter.FarmSaathiTypeConverters
import com.farmsaathi.core.database.dao.ContactDao
import com.farmsaathi.core.database.dao.CropDao
import com.farmsaathi.core.database.dao.DiaryDao
import com.farmsaathi.core.database.dao.FarmDao
import com.farmsaathi.core.database.dao.FieldOpsDao
import com.farmsaathi.core.database.dao.FinanceDao
import com.farmsaathi.core.database.dao.InventoryDao
import com.farmsaathi.core.database.dao.LabourDao
import com.farmsaathi.core.database.dao.LandMeasurementDao
import com.farmsaathi.core.database.dao.MachineryDao
import com.farmsaathi.core.database.dao.PlotDao
import com.farmsaathi.core.database.entity.ContactEntity
import com.farmsaathi.core.database.entity.CropCycleEntity
import com.farmsaathi.core.database.entity.CropSaleEntity
import com.farmsaathi.core.database.entity.CropTaskEntity
import com.farmsaathi.core.database.entity.CropTemplateEntity
import com.farmsaathi.core.database.entity.DiaryEntryEntity
import com.farmsaathi.core.database.entity.ExpenseDetailEntity
import com.farmsaathi.core.database.entity.FarmEntity
import com.farmsaathi.core.database.entity.HarvestRecordEntity
import com.farmsaathi.core.database.entity.InputProductEntity
import com.farmsaathi.core.database.entity.InputPurchaseEntity
import com.farmsaathi.core.database.entity.IrrigationLogEntity
import com.farmsaathi.core.database.entity.LandBoundaryPointEntity
import com.farmsaathi.core.database.entity.LandMeasurementEntity
import com.farmsaathi.core.database.entity.LedgerEntryEntity
import com.farmsaathi.core.database.entity.MachinerySessionEntity
import com.farmsaathi.core.database.entity.PlotEntity
import com.farmsaathi.core.database.entity.SoilTestRecordEntity
import com.farmsaathi.core.database.entity.StockMovementEntity
import com.farmsaathi.core.database.entity.StorageRecordEntity
import com.farmsaathi.core.database.entity.WorkerAttendanceEntity
import com.farmsaathi.core.database.entity.WorkerEntity
import com.farmsaathi.core.database.entity.WorkerPaymentEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [
        FarmEntity::class,
        PlotEntity::class,
        CropTemplateEntity::class,
        CropCycleEntity::class,
        CropTaskEntity::class,
        DiaryEntryEntity::class,
        LedgerEntryEntity::class,
        ExpenseDetailEntity::class,
        CropSaleEntity::class,
        WorkerEntity::class,
        WorkerAttendanceEntity::class,
        WorkerPaymentEntity::class,
        MachinerySessionEntity::class,
        InputProductEntity::class,
        InputPurchaseEntity::class,
        StockMovementEntity::class,
        IrrigationLogEntity::class,
        HarvestRecordEntity::class,
        StorageRecordEntity::class,
        SoilTestRecordEntity::class,
        ContactEntity::class,
        LandMeasurementEntity::class,
        LandBoundaryPointEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(FarmSaathiTypeConverters::class)
abstract class FarmSaathiDatabase : RoomDatabase() {
    abstract fun farmDao(): FarmDao

    abstract fun plotDao(): PlotDao

    abstract fun cropDao(): CropDao

    abstract fun diaryDao(): DiaryDao

    abstract fun financeDao(): FinanceDao

    abstract fun labourDao(): LabourDao

    abstract fun machineryDao(): MachineryDao

    abstract fun inventoryDao(): InventoryDao

    abstract fun fieldOpsDao(): FieldOpsDao

    abstract fun contactDao(): ContactDao

    abstract fun landMeasurementDao(): LandMeasurementDao

    companion object {
        const val DATABASE_NAME = "farm_saathi.db"
        const val SCHEMA_VERSION = 4

        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE workers ADD COLUMN voiceNamePath TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE workers ADD COLUMN daysWorkedTenths INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE farms ADD COLUMN voiceNamePath TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE crop_cycles ADD COLUMN voiceNamePath TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE contacts ADD COLUMN voiceNamePath TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE irrigation_logs ADD COLUMN photoPath TEXT DEFAULT NULL")
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `crop_templates` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `templateKey` TEXT NOT NULL,
                            `cropNameTe` TEXT NOT NULL,
                            `cropNameEn` TEXT NOT NULL,
                            `defaultSeason` TEXT NOT NULL,
                            `expectedDurationDays` INTEGER NOT NULL,
                            `defaultYieldUnit` TEXT NOT NULL,
                            `stages` TEXT NOT NULL,
                            `isStarterTemplate` INTEGER NOT NULL,
                            `isEditable` INTEGER NOT NULL,
                            `disclaimerTe` TEXT NOT NULL,
                            `disclaimerEn` TEXT NOT NULL,
                            `isArchived` INTEGER NOT NULL,
                            `createdAtEpochMillis` INTEGER NOT NULL,
                            `updatedAtEpochMillis` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_crop_templates_templateKey` " +
                            "ON `crop_templates` (`templateKey`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_crop_templates_isArchived` " +
                            "ON `crop_templates` (`isArchived`)",
                    )
                    db.execSQL("ALTER TABLE workers ADD COLUMN halfDayRatePaise INTEGER DEFAULT NULL")
                    db.execSQL("ALTER TABLE harvest_records ADD COLUMN photoPath TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE stock_movements ADD COLUMN photoPath TEXT DEFAULT NULL")
                }
            }

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `land_measurements` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `farmId` INTEGER NOT NULL,
                            `plotId` INTEGER NOT NULL,
                            `method` TEXT NOT NULL,
                            `areaMicroAcres` INTEGER NOT NULL,
                            `perimeterMeters` REAL,
                            `startedAtEpochMillis` INTEGER NOT NULL,
                            `completedAtEpochMillis` INTEGER NOT NULL,
                            `appliedToPlot` INTEGER NOT NULL,
                            `notes` TEXT,
                            FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                            FOREIGN KEY(`plotId`) REFERENCES `plots`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_land_measurements_farmId` ON `land_measurements` (`farmId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_land_measurements_plotId` ON `land_measurements` (`plotId`)")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_land_measurements_completedAtEpochMillis` " +
                            "ON `land_measurements` (`completedAtEpochMillis`)",
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `land_boundary_points` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `measurementId` INTEGER NOT NULL,
                            `sequenceNumber` INTEGER NOT NULL,
                            `latitude` REAL NOT NULL,
                            `longitude` REAL NOT NULL,
                            `horizontalAccuracyMeters` REAL,
                            `capturedAtEpochMillis` INTEGER NOT NULL,
                            FOREIGN KEY(`measurementId`) REFERENCES `land_measurements`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_land_boundary_points_measurementId_sequenceNumber` " +
                            "ON `land_boundary_points` (`measurementId`, `sequenceNumber`)",
                    )
                }
            }

        fun create(context: Context): FarmSaathiDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                FarmSaathiDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}

/**
 * Coordinates exclusive DB access during WAL checkpoint, ZIP backup export, and rollback-safe restore.
 */
@Singleton
class DatabaseMaintenanceCoordinator
@Inject
constructor() {
    private val mutex = Mutex()

    suspend fun <T> withExclusiveLock(block: suspend () -> T): T = mutex.withLock { block() }
}
