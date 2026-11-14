package com.farmsaathi.core.common

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

interface TimeProvider {
    fun nowEpochMillis(): Long

    fun todayEpochDay(): Long

    fun elapsedRealtimeMillis(): Long

    fun bootCount(): Int

    fun epochMillisToEpochDay(epochMillis: Long): Long

    fun epochDayToStartMillis(epochDay: Long): Long

    fun formatDate(
        epochMillis: Long,
        useTelugu: Boolean,
    ): String

    fun formatEpochDay(
        epochDay: Long,
        useTelugu: Boolean,
    ): String

    fun formatTime(epochMillis: Long): String
}

class AndroidTimeProvider(private val context: Context) : TimeProvider {
    private val zoneId: ZoneId get() = ZoneId.systemDefault()
    private val cachedBootCount: Int by lazy { readBootCount() }

    override fun nowEpochMillis(): Long = System.currentTimeMillis()

    override fun todayEpochDay(): Long = LocalDate.now(zoneId).toEpochDay()

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun bootCount(): Int = cachedBootCount

    private fun readBootCount(): Int =
        try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        } catch (_: SecurityException) {
            -1
        } catch (_: IllegalArgumentException) {
            -1
        }

    override fun epochMillisToEpochDay(epochMillis: Long): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().toEpochDay()

    override fun epochDayToStartMillis(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId).toInstant().toEpochMilli()

    override fun formatDate(
        epochMillis: Long,
        useTelugu: Boolean,
    ): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return formatLocalDate(date, useTelugu)
    }

    override fun formatEpochDay(
        epochDay: Long,
        useTelugu: Boolean,
    ): String = formatLocalDate(LocalDate.ofEpochDay(epochDay), useTelugu)

    override fun formatTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(TIME_FORMATTER)

    private fun formatLocalDate(
        date: LocalDate,
        useTelugu: Boolean,
    ): String {
        if (!useTelugu) {
            return date.format(DATE_FORMATTER)
        }
        val monthTe = TELUGU_MONTHS[date.monthValue - 1]
        return "${date.dayOfMonth} $monthTe ${date.year}"
    }

    companion object {
        private val TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("hh:mm\u00A0a", Locale.ENGLISH)
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
        private val TELUGU_MONTHS =
            listOf(
                "జనవరి", "ఫిబ్రవరి", "మార్చి", "ఏప్రిల్", "మే", "జూన్",
                "జూలై", "ఆగస్టు", "సెప్టెంబర్", "అక్టోబర్", "నవంబర్", "డిసెంబర్",
            )
    }
}

object SqlSearchUtils {
    fun escapeSqlLike(raw: String): String =
        raw.trim()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
}

object StoragePaths {
    const val MEDIA_DIR = "media"
    const val PHOTOS_DIR = "media/photos"
    const val AUDIO_NOTES_DIR = "media/audio"
    const val CUSTOM_PROMPTS_DIR = "audio_prompts"
    const val BACKUP_SUBFOLDER = "FarmSaathiBackups"
}
