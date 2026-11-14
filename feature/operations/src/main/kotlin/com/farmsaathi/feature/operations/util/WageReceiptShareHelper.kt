package com.farmsaathi.feature.operations.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.farmsaathi.core.model.MoneyPaise
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class WageReceiptData(
    val title: String,
    val subtitle: String,
    val workerPhotoPath: String? = null,
    val fullSunCount: Int = 0,
    val halfSunCount: Int = 0,
    val daysLabel: String,
    val paidAmount: MoneyPaise,
    val remainingBalance: MoneyPaise,
    val paymentModeLabel: String,
    val dateFormatted: String,
    val useTelugu: Boolean,
    val spokenSummary: String,
)

/**
 * Generates an offline visual wage receipt ("లెక్క చీటీ") PNG via Android Canvas
 * and shares it through the app's configured FileProvider authority.
 */
object WageReceiptShareHelper {
    private const val WIDTH_PX = 840
    private const val HEIGHT_PX = 1020
    private const val RECEIPT_PREFIX = "receipt_"
    private const val CAMERA_TMP_DIR = "camera_tmp"

    suspend fun shareReceipt(
        context: Context,
        receipt: WageReceiptData,
        workerPhotoFile: File? = null,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
        uriProvider: (Context, File) -> Uri = { ctx, file ->
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        },
    ): Result<File> =
        runCatching {
            val (file, chooser) =
                withContext(ioDispatcher) {
                    val bitmap = renderReceiptBitmap(receipt, workerPhotoFile)
                    val savedFile = saveReceiptPng(context, bitmap)
                    val uri = uriProvider(context, savedFile)
                    val sendIntent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, receipt.spokenSummary)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    val chooserTitle = if (receipt.useTelugu) "లెక్క చీటీ పంపండి" else "Share Wage Receipt"
                    val chooserIntent =
                        Intent.createChooser(sendIntent, chooserTitle).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    savedFile to chooserIntent
                }
            withContext(mainDispatcher) {
                context.startActivity(chooser)
            }
            file
        }.onFailure { throwable ->
            if (throwable is CancellationException) throw throwable
        }

    internal fun saveReceiptPng(
        context: Context,
        bitmap: Bitmap,
    ): File {
        val dir = File(context.cacheDir, CAMERA_TMP_DIR)
        if (!dir.exists()) dir.mkdirs()
        dir.listFiles { f -> f.name.startsWith(RECEIPT_PREFIX) && f.name.endsWith(".png") }
            ?.forEach { it.delete() }
        val file = File(dir, "$RECEIPT_PREFIX${System.currentTimeMillis()}.png")
        try {
            FileOutputStream(file).use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    "Failed to compress receipt PNG"
                }
            }
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        return file
    }

    internal fun renderReceiptBitmap(
        receipt: WageReceiptData,
        workerPhotoFile: File? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH_PX, HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Warm field cream background
        canvas.drawColor(Color.parseColor("#F7F8F2"))

        // Outer white receipt card
        val cardRect = RectF(36f, 36f, WIDTH_PX - 36f, HEIGHT_PX - 36f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val borderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#C3CEC2")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
        canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)
        canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

        drawHeaderBanner(canvas, receipt)
        drawIdentityRow(canvas, receipt, workerPhotoFile)
        drawDayTallySection(canvas, receipt)
        drawAmountBox(canvas, receipt)
        drawBalanceAndFooter(canvas, receipt)

        return bitmap
    }

    private fun drawHeaderBanner(
        canvas: Canvas,
        receipt: WageReceiptData,
    ) {
        val headerRect = RectF(36f, 36f, WIDTH_PX - 36f, 180f)
        val headerPath =
            Path().apply {
                addRoundRect(
                    headerRect,
                    floatArrayOf(28f, 28f, 28f, 28f, 0f, 0f, 0f, 0f),
                    Path.Direction.CW,
                )
            }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#245A3A") }
        canvas.drawPath(headerPath, headerPaint)

        val brandPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2F0E5")
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        val brandText = if (receipt.useTelugu) "ఫామ్ సాథీ • లెక్క చీటీ" else "Farm Saathi • Wage Receipt"
        canvas.drawText(brandText, 76f, 98f, brandPaint)

        val titlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 40f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        canvas.drawText(receipt.title, 76f, 150f, titlePaint)

        val datePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2F0E5")
                textSize = 26f
                textAlign = Paint.Align.RIGHT
            }
        canvas.drawText(receipt.dateFormatted, WIDTH_PX - 76f, 102f, datePaint)
    }

    private fun drawIdentityRow(
        canvas: Canvas,
        receipt: WageReceiptData,
        workerPhotoFile: File?,
    ) {
        val cx = 136f
        val cy = 276f
        val radius = 56f
        val decoded = decodeAvatarBitmap(workerPhotoFile)
        if (decoded != null) {
            canvas.save()
            val clipPath = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
            canvas.clipPath(clipPath)
            val src = Rect(0, 0, decoded.width, decoded.height)
            val dst = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawBitmap(decoded, src, dst, null)
            canvas.restore()
            decoded.recycle()
        } else {
            val circleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E2F0E5") }
            canvas.drawCircle(cx, cy, radius, circleBg)
            val badgeText =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#245A3A")
                    textSize = 38f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            val initials = receipt.subtitle.trim().take(2).ifBlank { "FS" }
            canvas.drawText(initials, cx, cy + 13f, badgeText)
        }
        val ringPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#245A3A")
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
        canvas.drawCircle(cx, cy, radius, ringPaint)

        val namePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E2B22")
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        canvas.drawText(receipt.subtitle.take(26), 218f, 270f, namePaint)

        val modePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#47574B")
                textSize = 26f
            }
        val modePrefix = if (receipt.useTelugu) "చెల్లింపు: " else "Mode: "
        canvas.drawText("$modePrefix${receipt.paymentModeLabel}", 218f, 314f, modePaint)
    }

    private fun drawDayTallySection(
        canvas: Canvas,
        receipt: WageReceiptData,
    ) {
        val boxRect = RectF(72f, 366f, WIDTH_PX - 72f, 514f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F7F8F2") }
        canvas.drawRoundRect(boxRect, 20f, 20f, bgPaint)

        val labelPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#47574B")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        val sectionTitle = if (receipt.useTelugu) "పని చేసిన రోజులు" else "Work Days"
        canvas.drawText(sectionTitle, 102f, 408f, labelPaint)

        val daysValuePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E2B22")
                textSize = 30f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        canvas.drawText(receipt.daysLabel, WIDTH_PX - 102f, 410f, daysValuePaint)

        // Draw Sun / Half-Sun tally icons
        var cursorX = 122f
        val iconY = 466f
        val maxIcons = 8
        val fullToDraw = receipt.fullSunCount.coerceAtMost(maxIcons)
        repeat(fullToDraw) {
            drawSunIcon(canvas, cursorX, iconY, full = true)
            cursorX += 58f
        }
        if (receipt.halfSunCount > 0 && fullToDraw < maxIcons) {
            drawSunIcon(canvas, cursorX, iconY, full = false)
            cursorX += 58f
        }
        if (receipt.fullSunCount > maxIcons) {
            val morePaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#9A6200")
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            canvas.drawText("+${receipt.fullSunCount - maxIcons}", cursorX - 10f, iconY + 9f, morePaint)
        }
    }

    private fun drawSunIcon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        full: Boolean,
    ) {
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B7791F") }
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E6D7B9") }
        if (full) {
            canvas.drawCircle(cx, cy, 18f, sunPaint)
        } else {
            canvas.drawCircle(cx, cy, 18f, trackPaint)
            val arcRect = RectF(cx - 18f, cy - 18f, cx + 18f, cy + 18f)
            canvas.drawArc(arcRect, 90f, 180f, true, sunPaint)
        }
        val ringPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#9A6200")
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }
        canvas.drawCircle(cx, cy, 18f, ringPaint)
    }

    private fun drawAmountBox(
        canvas: Canvas,
        receipt: WageReceiptData,
    ) {
        val amountRect = RectF(72f, 542f, WIDTH_PX - 72f, 738f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E2F0E5") }
        val borderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#245A3A")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
        canvas.drawRoundRect(amountRect, 24f, 24f, bgPaint)
        canvas.drawRoundRect(amountRect, 24f, 24f, borderPaint)

        val paidLabelPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#163C27")
                textSize = 28f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        val paidLabel = if (receipt.useTelugu) "చెల్లించిన మొత్తం" else "Amount Paid"
        canvas.drawText(paidLabel, WIDTH_PX / 2f, 600f, paidLabelPaint)

        val amountPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#245A3A")
                textSize = 72f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        canvas.drawText(receipt.paidAmount.formatInr(), WIDTH_PX / 2f, 692f, amountPaint)
    }

    private fun drawBalanceAndFooter(
        canvas: Canvas,
        receipt: WageReceiptData,
    ) {
        val hasDue = receipt.remainingBalance.isPositive()
        val dueRect = RectF(72f, 766f, WIDTH_PX - 72f, 868f)
        val dueBg =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(if (hasDue) "#FDF0D5" else "#E1F2E6")
            }
        canvas.drawRoundRect(dueRect, 20f, 20f, dueBg)

        val dueTextPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(if (hasDue) "#9A6200" else "#2F6F44")
                textSize = 32f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        val dueText =
            if (hasDue) {
                if (receipt.useTelugu) {
                    "మిగిలిన బాకీ: ${receipt.remainingBalance.formatInr()}"
                } else {
                    "Remaining Due: ${receipt.remainingBalance.formatInr()}"
                }
            } else {
                if (receipt.useTelugu) "బాకీ సున్నా ✓" else "Zero Balance ✓"
            }
        canvas.drawText(dueText, WIDTH_PX / 2f, 828f, dueTextPaint)

        val footerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#47574B")
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
        val footerText =
            if (receipt.useTelugu) {
                "ఫామ్ సాథీ • ఫోన్‌లోనే ఆఫ్‌లైన్ లెక్క"
            } else {
                "Farm Saathi • Offline Wage Record"
            }
        canvas.drawText(footerText, WIDTH_PX / 2f, 940f, footerPaint)
    }

    private fun decodeAvatarBitmap(file: File?): Bitmap? {
        if (file == null || !file.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (maxDim / sample > 256) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()
    }
}
