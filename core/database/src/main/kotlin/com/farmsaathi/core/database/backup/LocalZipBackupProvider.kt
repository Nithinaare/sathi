package com.farmsaathi.core.database.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.BackupInspection
import com.farmsaathi.core.common.BackupProvider
import com.farmsaathi.core.common.BackupResult
import com.farmsaathi.core.common.RestoreResult
import com.farmsaathi.core.common.StoragePaths
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.database.DatabaseMaintenanceCoordinator
import com.farmsaathi.core.database.FarmSaathiDatabase
import com.farmsaathi.core.datastore.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Self-contained offline .zip backup provider:
 * - Uses PRAGMA wal_checkpoint(TRUNCATE) for SQLite 3.22 (Android 10) consistency.
 * - Packages farm_saathi.db + filesDir/media/ + SHA-256 manifest.json.
 * - Exports via MediaStore.Downloads (Downloads/FarmSaathiBackups/) for 1-tap Share.
 * - Restores with integrity verification and automatic pre-restore rollback snapshot.
 */
@Singleton
class LocalZipBackupProvider
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val dbProvider: Provider<FarmSaathiDatabase>,
    private val coordinator: DatabaseMaintenanceCoordinator,
    private val prefsRepo: UserPreferencesRepository,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers,
) : BackupProvider {
    override suspend fun exportBackup(): BackupResult =
        withContext(dispatchers.io) {
            coordinator.withExclusiveLock {
                val now = timeProvider.nowEpochMillis()
                val stagingDir = File(context.cacheDir, "backup_staging_${UUID.randomUUID()}")
                stagingDir.mkdirs()

                try {
                    checkpointDatabase(dbProvider.get())
                    val dbFile = context.getDatabasePath(FarmSaathiDatabase.DATABASE_NAME)
                    val stagedDb = File(stagingDir, DB_ENTRY_PATH)
                    stagedDb.parentFile?.mkdirs()
                    dbFile.copyTo(stagedDb, overwrite = true)

                    val mediaRoot = File(context.filesDir, StoragePaths.MEDIA_DIR)
                    val mediaEntries = collectAndStageMedia(mediaRoot, stagingDir)
                    val dbSha = sha256Hex(stagedDb)
                    val manifestBytes = buildManifestJson(now, dbSha, mediaEntries).toByteArray(Charsets.UTF_8)
                    val manifestFile = File(stagingDir, MANIFEST_ENTRY_PATH)
                    manifestFile.writeBytes(manifestBytes)
                    val manifestSha = sha256Hex(manifestFile)

                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date(now))
                    val fileName = "farm_saathi_$stamp.zip"
                    val tempZip = File(context.cacheDir, fileName)
                    writeZipFromStaging(stagingDir, tempZip)
                    val sizeBytes = tempZip.length()
                    val publishedUri = writeZipToDownloads(fileName, tempZip)
                    tempZip.delete()
                    prefsRepo.setLastBackupEpochMillis(now)

                    BackupResult(
                        uri = publishedUri,
                        fileName = fileName,
                        sizeBytes = sizeBytes,
                        manifestSha256 = manifestSha,
                        createdAtEpochMillis = now,
                    )
                } finally {
                    stagingDir.deleteRecursively()
                }
            }
        }

    override suspend fun inspectBackup(sourceUri: Uri): BackupInspection =
        withContext(dispatchers.io) {
            val stagingDir = File(context.cacheDir, "inspect_${UUID.randomUUID()}")
            try {
                unzipSafely(sourceUri, stagingDir)
                val parsed = validateStagedBackup(stagingDir)
                if (parsed.error != null) {
                    BackupInspection(false, 0, 0, 0L, 0, parsed.error)
                } else {
                    BackupInspection(
                        isValid = true,
                        formatVersion = parsed.formatVersion,
                        dbSchemaVersion = parsed.dbSchemaVersion,
                        createdAtEpochMillis = parsed.createdAt,
                        mediaCount = parsed.mediaCount,
                    )
                }
            } catch (e: IOException) {
                BackupInspection(false, 0, 0, 0L, 0, e.message ?: "Unable to read zip archive")
            } catch (e: JSONException) {
                BackupInspection(false, 0, 0, 0L, 0, e.message ?: "Invalid backup manifest")
            } catch (e: IllegalArgumentException) {
                BackupInspection(false, 0, 0, 0L, 0, e.message ?: "Unsafe zip archive")
            } catch (e: SecurityException) {
                BackupInspection(false, 0, 0, 0L, 0, e.message ?: "Permission denied")
            } finally {
                stagingDir.deleteRecursively()
            }
        }

    override suspend fun restoreBackup(sourceUri: Uri): RestoreResult =
        withContext(dispatchers.io) {
            coordinator.withExclusiveLock {
                val stagingDir = File(context.cacheDir, "restore_${UUID.randomUUID()}")
                val rollbackDir = File(context.cacheDir, "rollback_${UUID.randomUUID()}")
                try {
                    unzipSafely(sourceUri, stagingDir)
                    val validation = validateStagedBackup(stagingDir)
                    if (validation.error != null) {
                        return@withExclusiveLock RestoreResult.AbortedBeforeMutation(validation.error)
                    }

                    val db = dbProvider.get()
                    checkpointDatabase(db)
                    db.close()

                    snapshotCurrentStateForRollback(rollbackDir)
                    val restoredCount = applyStagedBackup(stagingDir)
                    RestoreResult.Success(restoredCount)
                } catch (e: IOException) {
                    attemptRollback(rollbackDir, e.message)
                } catch (e: JSONException) {
                    attemptRollback(rollbackDir, e.message)
                } catch (e: IllegalStateException) {
                    attemptRollback(rollbackDir, e.message)
                } catch (e: IllegalArgumentException) {
                    attemptRollback(rollbackDir, e.message)
                } catch (e: SecurityException) {
                    attemptRollback(rollbackDir, e.message)
                } finally {
                    stagingDir.deleteRecursively()
                    rollbackDir.deleteRecursively()
                }
            }
        }

    private fun attemptRollback(
        rollbackDir: File,
        message: String?,
    ): RestoreResult {
        runCatching { restoreRollbackSnapshot(rollbackDir) }
        return RestoreResult.FailedAndRolledBack(message ?: "Restore failed and rolled back")
    }

    private fun checkpointDatabase(db: FarmSaathiDatabase) {
        val sqlite = db.openHelper.writableDatabase
        sqlite.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
            if (cursor.moveToFirst()) {
                val busy = cursor.getInt(0)
                check(busy == 0) { "Database checkpoint busy ($busy)" }
            }
        }
    }

    private fun collectAndStageMedia(
        mediaRoot: File,
        stagingDir: File,
    ): List<Pair<String, String>> {
        if (!mediaRoot.exists()) return emptyList()
        val entries = mutableListOf<Pair<String, String>>()
        mediaRoot.walkTopDown().filter { it.isFile }.forEach { file ->
            val rel = file.relativeTo(context.filesDir).invariantSeparatorsPath
            val dest = File(stagingDir, rel)
            dest.parentFile?.mkdirs()
            file.copyTo(dest, overwrite = true)
            entries += rel to sha256Hex(dest)
        }
        return entries
    }

    private fun buildManifestJson(
        now: Long,
        dbSha: String,
        media: List<Pair<String, String>>,
    ): String {
        val mediaArr = JSONArray()
        media.forEach { (path, sha) ->
            mediaArr.put(
                JSONObject().apply {
                    put("path", path)
                    put("sha256", sha)
                },
            )
        }
        return JSONObject().apply {
            put("formatVersion", FORMAT_VERSION)
            put("dbSchemaVersion", FarmSaathiDatabase.SCHEMA_VERSION)
            put("createdAtEpochMillis", now)
            put("dbPath", DB_ENTRY_PATH)
            put("dbSha256", dbSha)
            put("mediaFiles", mediaArr)
        }.toString(2)
    }

    private fun writeZipFromStaging(
        stagingDir: File,
        targetZip: File,
    ) {
        ZipOutputStream(FileOutputStream(targetZip)).use { zos ->
            stagingDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val relPath = file.relativeTo(stagingDir).invariantSeparatorsPath
                zos.putNextEntry(ZipEntry(relPath))
                FileInputStream(file).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun writeZipToDownloads(
        fileName: String,
        sourceZip: File,
    ): Uri {
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/${StoragePaths.BACKUP_SUBFOLDER}",
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        val resolver = context.contentResolver
        val uri =
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create MediaStore download entry")
        resolver.openOutputStream(uri)?.use { out ->
            FileInputStream(sourceZip).use { it.copyTo(out) }
        } ?: error("Unable to open download output stream")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun unzipSafely(
        sourceUri: Uri,
        targetDir: File,
    ) {
        targetDir.mkdirs()
        val canonicalRoot = targetDir.canonicalPath + File.separator
        val stream = context.contentResolver.openInputStream(sourceUri) ?: error("Cannot read backup file")
        stream.use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    copyEntryIfFile(zis, entry, targetDir, canonicalRoot)
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    private fun copyEntryIfFile(
        zis: ZipInputStream,
        entry: ZipEntry,
        targetDir: File,
        canonicalRoot: String,
    ) {
        if (entry.isDirectory) return
        val dest = File(targetDir, entry.name)
        require(dest.canonicalPath.startsWith(canonicalRoot)) { "Blocked unsafe zip path" }
        dest.parentFile?.mkdirs()
        FileOutputStream(dest).use { zis.copyTo(it) }
    }

    private fun validateStagedBackup(stagingDir: File): ParsedManifest {
        val manifestFile = File(stagingDir, MANIFEST_ENTRY_PATH)
        if (!manifestFile.exists()) return ParsedManifest(error = "Missing manifest.json")
        val json = JSONObject(manifestFile.readText(Charsets.UTF_8))
        val formatVer = json.optInt("formatVersion", 0)
        val dbVer = json.optInt("dbSchemaVersion", 0)
        val checksumError = verifyChecksums(stagingDir, json, formatVer, dbVer)
        if (checksumError != null) return ParsedManifest(error = checksumError)

        val mediaArr = json.optJSONArray("mediaFiles") ?: JSONArray()
        return ParsedManifest(
            formatVersion = formatVer,
            dbSchemaVersion = dbVer,
            createdAt = json.optLong("createdAtEpochMillis", 0L),
            mediaCount = mediaArr.length(),
        )
    }

    private fun verifyChecksums(
        stagingDir: File,
        json: JSONObject,
        formatVer: Int,
        dbVer: Int,
    ): String? {
        if (formatVer != FORMAT_VERSION || dbVer < 1 || dbVer > FarmSaathiDatabase.SCHEMA_VERSION) {
            return "Unsupported backup version"
        }
        val dbPath = json.optString("dbPath", DB_ENTRY_PATH)
        val dbFile = File(stagingDir, dbPath)
        if (!dbFile.exists() || sha256Hex(dbFile) != json.optString("dbSha256", "")) {
            return "Database checksum mismatch"
        }
        val mediaArr = json.optJSONArray("mediaFiles") ?: JSONArray()
        val allMediaValid =
            (0 until mediaArr.length()).all { i ->
                val obj = mediaArr.getJSONObject(i)
                val mFile = File(stagingDir, obj.getString("path"))
                mFile.exists() && sha256Hex(mFile) == obj.getString("sha256")
            }
        return if (allMediaValid) null else "Media checksum mismatch"
    }

    private fun snapshotCurrentStateForRollback(rollbackDir: File) {
        rollbackDir.mkdirs()
        val dbFile = context.getDatabasePath(FarmSaathiDatabase.DATABASE_NAME)
        if (dbFile.exists()) dbFile.copyTo(File(rollbackDir, DB_ENTRY_PATH), overwrite = true)
        val mediaRoot = File(context.filesDir, StoragePaths.MEDIA_DIR)
        if (mediaRoot.exists()) {
            mediaRoot.copyRecursively(File(rollbackDir, StoragePaths.MEDIA_DIR), overwrite = true)
        }
    }

    private fun applyStagedBackup(stagingDir: File): Int {
        val targetDb = context.getDatabasePath(FarmSaathiDatabase.DATABASE_NAME)
        File("${targetDb.absolutePath}-wal").delete()
        File("${targetDb.absolutePath}-shm").delete()
        File(stagingDir, DB_ENTRY_PATH).copyTo(targetDb, overwrite = true)

        val currentMedia = File(context.filesDir, StoragePaths.MEDIA_DIR)
        if (currentMedia.exists()) currentMedia.deleteRecursively()
        val stagedMedia = File(stagingDir, StoragePaths.MEDIA_DIR)
        var restoredCount = 0
        if (stagedMedia.exists()) {
            stagedMedia.copyRecursively(currentMedia, overwrite = true)
            restoredCount = currentMedia.walkTopDown().count { it.isFile }
        }
        return restoredCount
    }

    private fun restoreRollbackSnapshot(rollbackDir: File) {
        val prevDb = File(rollbackDir, DB_ENTRY_PATH)
        val targetDb = context.getDatabasePath(FarmSaathiDatabase.DATABASE_NAME)
        if (prevDb.exists()) prevDb.copyTo(targetDb, overwrite = true)
        val prevMedia = File(rollbackDir, StoragePaths.MEDIA_DIR)
        val targetMedia = File(context.filesDir, StoragePaths.MEDIA_DIR)
        if (targetMedia.exists()) targetMedia.deleteRecursively()
        if (prevMedia.exists()) prevMedia.copyRecursively(targetMedia, overwrite = true)
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(8192)
            var read = input.read(buf)
            while (read > 0) {
                digest.update(buf, 0, read)
                read = input.read(buf)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private data class ParsedManifest(
        val formatVersion: Int = 0,
        val dbSchemaVersion: Int = 0,
        val createdAt: Long = 0L,
        val mediaCount: Int = 0,
        val error: String? = null,
    )

    companion object {
        private const val FORMAT_VERSION = 1
        private const val DB_ENTRY_PATH = "db/farm_saathi.db"
        private const val MANIFEST_ENTRY_PATH = "manifest.json"
    }
}
