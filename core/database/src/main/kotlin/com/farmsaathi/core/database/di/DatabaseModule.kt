package com.farmsaathi.core.database.di

import android.content.Context
import com.farmsaathi.core.common.AndroidTimeProvider
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.BackupProvider
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.DiaryRepository
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.FieldOpsRepository
import com.farmsaathi.core.common.repository.FinanceRepository
import com.farmsaathi.core.common.repository.InventoryRepository
import com.farmsaathi.core.common.repository.LabourRepository
import com.farmsaathi.core.common.repository.MachineryRepository
import com.farmsaathi.core.database.FarmSaathiDatabase
import com.farmsaathi.core.database.backup.LocalZipBackupProvider
import com.farmsaathi.core.database.dao.ContactDao
import com.farmsaathi.core.database.dao.CropDao
import com.farmsaathi.core.database.dao.DiaryDao
import com.farmsaathi.core.database.dao.FarmDao
import com.farmsaathi.core.database.dao.FieldOpsDao
import com.farmsaathi.core.database.dao.FinanceDao
import com.farmsaathi.core.database.dao.InventoryDao
import com.farmsaathi.core.database.dao.LabourDao
import com.farmsaathi.core.database.dao.MachineryDao
import com.farmsaathi.core.database.dao.PlotDao
import com.farmsaathi.core.database.repository.RoomContactAndSearchRepository
import com.farmsaathi.core.database.repository.RoomDiaryRepository
import com.farmsaathi.core.database.repository.RoomFarmRepository
import com.farmsaathi.core.database.repository.RoomFieldOpsRepository
import com.farmsaathi.core.database.repository.RoomFinanceRepository
import com.farmsaathi.core.database.repository.RoomInventoryRepository
import com.farmsaathi.core.database.repository.RoomLabourRepository
import com.farmsaathi.core.database.repository.RoomMachineryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseProvideModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): FarmSaathiDatabase = FarmSaathiDatabase.create(context)

    @Provides fun provideFarmDao(db: FarmSaathiDatabase): FarmDao = db.farmDao()

    @Provides fun providePlotDao(db: FarmSaathiDatabase): PlotDao = db.plotDao()

    @Provides fun provideCropDao(db: FarmSaathiDatabase): CropDao = db.cropDao()

    @Provides fun provideDiaryDao(db: FarmSaathiDatabase): DiaryDao = db.diaryDao()

    @Provides fun provideFinanceDao(db: FarmSaathiDatabase): FinanceDao = db.financeDao()

    @Provides fun provideLabourDao(db: FarmSaathiDatabase): LabourDao = db.labourDao()

    @Provides fun provideMachineryDao(db: FarmSaathiDatabase): MachineryDao = db.machineryDao()

    @Provides fun provideInventoryDao(db: FarmSaathiDatabase): InventoryDao = db.inventoryDao()

    @Provides fun provideFieldOpsDao(db: FarmSaathiDatabase): FieldOpsDao = db.fieldOpsDao()

    @Provides fun provideContactDao(db: FarmSaathiDatabase): ContactDao = db.contactDao()

    @Provides
    @Singleton
    fun provideTimeProvider(
        @ApplicationContext context: Context,
    ): TimeProvider = AndroidTimeProvider(context)

    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = DefaultAppDispatchers
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindModule {
    @Binds @Singleton
    abstract fun bindFarmRepo(impl: RoomFarmRepository): FarmRepository

    @Binds @Singleton
    abstract fun bindDiaryRepo(impl: RoomDiaryRepository): DiaryRepository

    @Binds @Singleton
    abstract fun bindFinanceRepo(impl: RoomFinanceRepository): FinanceRepository

    @Binds @Singleton
    abstract fun bindLabourRepo(impl: RoomLabourRepository): LabourRepository

    @Binds @Singleton
    abstract fun bindMachineryRepo(impl: RoomMachineryRepository): MachineryRepository

    @Binds @Singleton
    abstract fun bindInventoryRepo(impl: RoomInventoryRepository): InventoryRepository

    @Binds @Singleton
    abstract fun bindFieldOpsRepo(impl: RoomFieldOpsRepository): FieldOpsRepository

    @Binds @Singleton
    abstract fun bindContactRepo(impl: RoomContactAndSearchRepository): ContactAndSearchRepository

    @Binds @Singleton
    abstract fun bindBackupProvider(impl: LocalZipBackupProvider): BackupProvider
}
