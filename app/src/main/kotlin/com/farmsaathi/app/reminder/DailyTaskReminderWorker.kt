package com.farmsaathi.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.farmsaathi.app.MainActivity
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.model.AppLanguage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyTaskReminderWorker
@AssistedInject
constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val farmRepository: FarmRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = prefsRepository.userPreferences.first()
        if (!prefs.dailyReminderEnabled) return Result.success()

        val dueCount = farmRepository.getDueAndOverdueCount(timeProvider.todayEpochDay())
        if (dueCount <= 0) return Result.success()

        ensureChannel(context)
        val useTelugu = prefs.language == AppLanguage.TELUGU
        val title = if (useTelugu) "ఈరోజు పొలం పనులు" else "Today's Farm Tasks"
        val body =
            if (useTelugu) {
                "మీకు $dueCount పొలం పనులు చేయవలసి ఉన్నాయి. చూడటానికి నొక్కండి."
            } else {
                "You have $dueCount farm tasks due or overdue. Tap to view."
            }

        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pending =
            PendingIntent.getActivity(
                context,
                101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "farm_daily_tasks"
        const val WORK_NAME = "farm_saathi_daily_task_reminder"
        private const val NOTIFICATION_ID = 2001

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Farm Task Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            manager.createNotificationChannel(channel)
        }

        fun schedule(
            context: Context,
            enabled: Boolean,
            hour: Int,
            minute: Int,
        ) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val now = LocalDateTime.now()
            var next = now.withHour(hour.coerceIn(0, 23)).withMinute(minute.coerceIn(0, 59)).withSecond(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val initialDelay = Duration.between(now, next).toMinutes().coerceAtLeast(1L)

            val request =
                PeriodicWorkRequestBuilder<DailyTaskReminderWorker>(24L, TimeUnit.HOURS)
                    .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                    .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}

class BootRescheduleReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun prefsRepository(): UserPreferencesRepository
    }

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }
        val pendingResult = goAsync()
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java,
            )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = entryPoint.prefsRepository().userPreferences.first()
                DailyTaskReminderWorker.schedule(
                    context = context.applicationContext,
                    enabled = prefs.dailyReminderEnabled,
                    hour = prefs.reminderHour,
                    minute = prefs.reminderMinute,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
