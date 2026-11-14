package com.farmsaathi.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import coil3.ImageLoader
import coil3.memory.MemoryCache
import com.farmsaathi.core.common.StoragePaths
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CapturedPhotoMetadata(
    val relativePath: String,
    val capturedAtEpochMillis: Long? = null,
)

/**
 * Manages app-private JPEG storage tuned for Realme 5s (4 GB RAM):
 * downsamples imported/captured images to max 1600px long edge at 80% JPEG with EXIF rotation fix.
 */
@Singleton
class LocalPhotoStorage
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    fun photosDir(): File {
        val dir = File(context.filesDir, StoragePaths.PHOTOS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createTempCaptureFile(): File {
        val dir = File(context.cacheDir, "camera_tmp")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "cap_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
    }

    suspend fun saveCompressedFromUri(sourceUri: Uri): Result<String> =
        saveCompressedWithExifFromUri(sourceUri).map { it.relativePath }

    suspend fun compressAndPersist(sourceFile: File): Result<String> =
        compressAndPersistWithExif(sourceFile).map { it.relativePath }

    suspend fun saveCompressedWithExifFromUri(sourceUri: Uri): Result<CapturedPhotoMetadata> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tempFile = createTempCaptureFile()
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: error("Cannot open image stream")

                val saved = compressAndPersistWithExif(tempFile)
                tempFile.delete()
                saved.getOrThrow()
            }
        }

    suspend fun compressAndPersistWithExif(sourceFile: File): Result<CapturedPhotoMetadata> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(sourceFile.exists() && sourceFile.length() > 0L) { "Empty source photo" }
                val capturedAt = extractExifDateTimeMillis(sourceFile.absolutePath)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
                val srcW = bounds.outWidth
                val srcH = bounds.outHeight
                require(srcW > 0 && srcH > 0) { "Invalid photo dimensions" }

                var inSampleSize = 1
                val maxDim = maxOf(srcW, srcH)
                while (maxDim / inSampleSize > MAX_LONG_EDGE_PX * 2) {
                    inSampleSize *= 2
                }

                val decodeOpts =
                    BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                        this.inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                val decoded =
                    BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOpts)
                        ?: error("Bitmap decode returned null")

                val oriented = applyExifOrientation(decoded, sourceFile.absolutePath)
                val scaled = scaleToMaxEdge(oriented, MAX_LONG_EDGE_PX)
                if (oriented !== decoded && !decoded.isRecycled) decoded.recycle()
                if (scaled !== oriented && !oriented.isRecycled) oriented.recycle()

                val target =
                    File(
                        photosDir(),
                        "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg",
                    )
                FileOutputStream(target).use { out ->
                    check(scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                        "JPEG compression failed"
                    }
                }
                if (!scaled.isRecycled) scaled.recycle()

                CapturedPhotoMetadata(
                    relativePath = toRelativePath(target),
                    capturedAtEpochMillis = capturedAt,
                )
            }
        }

    fun resolveFile(storedPath: String): File {
        val direct = File(storedPath)
        return if (direct.isAbsolute) direct else File(context.filesDir, storedPath)
    }

    fun toRelativePath(file: File): String {
        val base = context.filesDir.absolutePath.trimEnd(File.separatorChar) + File.separator
        val abs = file.absolutePath
        return if (abs.startsWith(base)) abs.removePrefix(base).replace('\\', '/') else abs
    }

    fun buildLowMemoryImageLoader(): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .build()

    private fun applyExifOrientation(
        bitmap: Bitmap,
        filePath: String,
    ): Bitmap {
        val orientation =
            runCatching {
                ExifInterface(filePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToMaxEdge(
        bitmap: Bitmap,
        maxEdge: Int,
    ): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longest.toFloat()
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    companion object {
        private const val MAX_LONG_EDGE_PX = 1600
        private const val JPEG_QUALITY = 80

        internal fun extractExifDateTimeMillis(filePath: String): Long? =
            runCatching {
                val exif = ExifInterface(filePath)
                val raw =
                    exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                        ?: return@runCatching null
                val offset =
                    exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_OFFSET_TIME)
                parseExifDateTimeString(raw, offset)
            }.getOrNull()

        internal fun parseExifDateTimeString(
            raw: String,
            offset: String? = null,
        ): Long? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val tz =
                if (!offset.isNullOrBlank()) {
                    TimeZone.getTimeZone("GMT${offset.trim()}")
                } else {
                    TimeZone.getDefault()
                }
            val parser =
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
                    isLenient = false
                    timeZone = tz
                }
            val parsed = parser.parse(trimmed, ParsePosition(0)) ?: return null
            return parsed.time.takeIf { it > 0L }
        }
    }
}
