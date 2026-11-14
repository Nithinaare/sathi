package com.farmsaathi.core.database

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.RestoreResult
import com.farmsaathi.core.common.StoragePaths
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.database.backup.LocalZipBackupProvider
import com.farmsaathi.core.database.entity.FarmEntity
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AreaUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LocalZipBackupTest {
    private lateinit var context: Context
    private lateinit var db: FarmSaathiDatabase
    private lateinit var backupProvider: LocalZipBackupProvider
    private lateinit var mediaStoreDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(FarmSaathiDatabase.DATABASE_NAME)
        File(context.filesDir, StoragePaths.MEDIA_DIR).deleteRecursively()
        mediaStoreDir =
            File(context.cacheDir, "fake_downloads").apply {
                deleteRecursively()
                mkdirs()
            }
        val providerInfo = ProviderInfo().apply { authority = MediaStore.AUTHORITY }
        ShadowContentResolver.registerProviderInternal(
            MediaStore.AUTHORITY,
            FakeMediaStoreProvider(mediaStoreDir).apply { attachInfo(context, providerInfo) },
        )

        db = openDb()
        backupProvider =
            LocalZipBackupProvider(
                context = context,
                dbProvider = Provider { db },
                coordinator = DatabaseMaintenanceCoordinator(),
                prefsRepo = FakePrefsRepo(),
                timeProvider = SimpleTimeProvider(),
                dispatchers =
                object : AppDispatchers {
                    override val io: CoroutineDispatcher = Dispatchers.Unconfined
                    override val default: CoroutineDispatcher = Dispatchers.Unconfined
                    override val main: CoroutineDispatcher = Dispatchers.Unconfined
                },
            )
    }

    @After
    fun teardown() {
        if (db.isOpen) db.close()
        context.deleteDatabase(FarmSaathiDatabase.DATABASE_NAME)
        File(context.filesDir, StoragePaths.MEDIA_DIR).deleteRecursively()
        mediaStoreDir.deleteRecursively()
    }

    @Test
    fun `corrupt zip backup aborts safely without mutating live database`() =
        runTest {
            db.farmDao().insertFarm(
                FarmEntity(
                    name = "Live Farm",
                    createdAtEpochMillis = 1000L,
                    updatedAtEpochMillis = 1000L,
                ),
            )

            val badZip = File(context.cacheDir, "corrupt.zip")
            ZipOutputStream(FileOutputStream(badZip)).use { zos ->
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write("""{"formatVersion":1,"dbSchemaVersion":1,"dbSha256":"bad"}""".toByteArray())
                zos.closeEntry()
            }

            val inspection = backupProvider.inspectBackup(Uri.fromFile(badZip))
            assertFalse(inspection.isValid)

            val res = backupProvider.restoreBackup(Uri.fromFile(badZip))
            assertTrue(res is RestoreResult.AbortedBeforeMutation)
            assertEquals(1, db.farmDao().countActiveFarms())
            badZip.delete()
        }

    @Test
    fun `full zip export inspect and restore preserves database and media files`() =
        runTest {
            db.farmDao().insertFarm(
                FarmEntity(
                    name = "Raju Garam Khet",
                    villageOrLocation = "Nalgonda",
                    createdAtEpochMillis = 1000L,
                    updatedAtEpochMillis = 1000L,
                ),
            )
            val photoFile = File(context.filesDir, "${StoragePaths.MEDIA_DIR}/photos/plot_1.jpg")
            photoFile.parentFile?.mkdirs()
            photoFile.writeText("fake-jpeg-bytes-v1")

            val exportResult = backupProvider.exportBackup()
            assertTrue(exportResult.sizeBytes > 0L)
            assertEquals(64, exportResult.manifestSha256.length)

            val inspection = backupProvider.inspectBackup(exportResult.uri)
            assertTrue("Inspection failed: ${inspection.errorReason}", inspection.isValid)
            assertEquals(1, inspection.formatVersion)
            assertEquals(1, inspection.mediaCount)

            // Mutate live database and delete photo before restoring backup
            db.farmDao().insertFarm(
                FarmEntity(
                    name = "Temporary Farm To Wipe",
                    createdAtEpochMillis = 2000L,
                    updatedAtEpochMillis = 2000L,
                ),
            )
            assertEquals(2, db.farmDao().countActiveFarms())
            photoFile.delete()
            assertFalse(photoFile.exists())

            val restoreResult = backupProvider.restoreBackup(exportResult.uri)
            assertTrue("Restore failed: $restoreResult", restoreResult is RestoreResult.Success)
            assertEquals(1, (restoreResult as RestoreResult.Success).restoredMediaCount)

            db = openDb()
            val restoredFarms = db.farmDao().observeFarms(false).first()
            assertEquals(1, restoredFarms.size)
            assertEquals("Raju Garam Khet", restoredFarms.first().name)
            assertEquals("fake-jpeg-bytes-v1", photoFile.readText())
        }

    private fun openDb(): FarmSaathiDatabase =
        Room.databaseBuilder(context, FarmSaathiDatabase::class.java, FarmSaathiDatabase.DATABASE_NAME)
            .allowMainThreadQueries()
            .build()

    private class FakeMediaStoreProvider(private val rootDir: File) : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri {
            val name = values?.getAsString(MediaStore.Downloads.DISPLAY_NAME) ?: "backup.zip"
            File(rootDir, name).apply {
                parentFile?.mkdirs()
                createNewFile()
            }
            return Uri.parse("content://${MediaStore.AUTHORITY}/downloads/$name")
        }

        override fun openFile(
            uri: Uri,
            mode: String,
        ): ParcelFileDescriptor {
            val file = File(rootDir, uri.lastPathSegment ?: "backup.zip")
            val pfdMode =
                when {
                    mode.contains("w") ->
                        ParcelFileDescriptor.MODE_CREATE or
                            ParcelFileDescriptor.MODE_TRUNCATE or
                            ParcelFileDescriptor.MODE_WRITE_ONLY
                    else -> ParcelFileDescriptor.MODE_READ_ONLY
                }
            return ParcelFileDescriptor.open(file, pfdMode)
        }

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 1

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun getType(uri: Uri): String = "application/zip"
    }

    private class FakePrefsRepo : UserPreferencesRepository {
        private val state = MutableStateFlow(UserPreferences())
        override val userPreferences: Flow<UserPreferences> = state

        override suspend fun setOnboardingCompleted(completed: Boolean) {}

        override suspend fun updateProfile(
            farmerName: String,
            villageName: String,
        ) {}

        override suspend fun setLanguage(language: AppLanguage) {}

        override suspend fun setAudioGuidanceEnabled(enabled: Boolean) {}

        override suspend fun setPreferredAreaUnit(unit: AreaUnit) {}

        override suspend fun setActiveFarmId(farmId: Long?) {}

        override suspend fun setDailyReminder(
            enabled: Boolean,
            hour: Int,
            minute: Int,
        ) {}

        override suspend fun setLastBackupEpochMillis(epochMillis: Long) {
            state.value = state.value.copy(lastBackupEpochMillis = epochMillis)
        }
    }

    private class SimpleTimeProvider : TimeProvider {
        override fun nowEpochMillis(): Long = 1_794_326_000_000L

        override fun todayEpochDay(): Long = 20_650L

        override fun elapsedRealtimeMillis(): Long = 10_000L

        override fun bootCount(): Int = 1

        override fun epochMillisToEpochDay(epochMillis: Long): Long = 20_650L

        override fun epochDayToStartMillis(epochDay: Long): Long = epochDay * 86_400_000L

        override fun formatDate(
            epochMillis: Long,
            useTelugu: Boolean,
        ): String = "10 Nov 2026"

        override fun formatEpochDay(
            epochDay: Long,
            useTelugu: Boolean,
        ): String = "10 Nov 2026"

        override fun formatTime(epochMillis: Long): String = "10:00 AM"
    }
}
