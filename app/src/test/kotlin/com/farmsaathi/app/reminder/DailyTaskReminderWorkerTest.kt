package com.farmsaathi.app.reminder

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CropTemplate
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class DailyTaskReminderWorkerTest {
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun `disabled reminder skips posting notification`() =
        runTest {
            val worker =
                buildWorker(
                    prefs = UserPreferences(dailyReminderEnabled = false, language = AppLanguage.TELUGU),
                    dueCount = 3,
                )
            val result = worker.doWork()
            assertEquals(ListenableWorker.Result.success(), result)
            assertTrue(Shadows.shadowOf(notificationManager).allNotifications.isEmpty())
        }

    @Test
    fun `zero due tasks skips posting notification`() =
        runTest {
            val worker =
                buildWorker(
                    prefs = UserPreferences(dailyReminderEnabled = true, language = AppLanguage.TELUGU),
                    dueCount = 0,
                )
            val result = worker.doWork()
            assertEquals(ListenableWorker.Result.success(), result)
            assertTrue(Shadows.shadowOf(notificationManager).allNotifications.isEmpty())
        }

    @Test
    fun `due tasks posts telugu daily reminder notification`() =
        runTest {
            val worker =
                buildWorker(
                    prefs = UserPreferences(dailyReminderEnabled = true, language = AppLanguage.TELUGU),
                    dueCount = 3,
                )
            val result = worker.doWork()
            assertEquals(ListenableWorker.Result.success(), result)

            val notifications = Shadows.shadowOf(notificationManager).allNotifications
            assertEquals(1, notifications.size)
            val extras = notifications.first().extras
            assertEquals("ఈరోజు పొలం పనులు", extras.getString(Notification.EXTRA_TITLE))
            assertTrue(extras.getString(Notification.EXTRA_TEXT).orEmpty().contains("3"))
        }

    private fun buildWorker(
        prefs: UserPreferences,
        dueCount: Int,
    ): DailyTaskReminderWorker {
        val farmRepo = StubFarmRepo(dueCount)
        val prefsRepo = StubPrefsRepo(prefs)
        val timeProvider = StubTimeProvider()
        return TestListenableWorkerBuilder<DailyTaskReminderWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker =
                        DailyTaskReminderWorker(
                            context = appContext,
                            params = workerParameters,
                            farmRepository = farmRepo,
                            prefsRepository = prefsRepo,
                            timeProvider = timeProvider,
                        )
                },
            )
            .build()
    }

    private class StubPrefsRepo(prefs: UserPreferences) : UserPreferencesRepository {
        override val userPreferences: Flow<UserPreferences> = MutableStateFlow(prefs)

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

        override suspend fun setLastBackupEpochMillis(epochMillis: Long) {}
    }

    private class StubFarmRepo(private val dueCount: Int) : FarmRepository {
        override suspend fun getDueAndOverdueCount(todayEpochDay: Long): Int = dueCount

        override fun observeFarms(includeArchived: Boolean): Flow<List<Farm>> = flowOf(emptyList())

        override fun observeFarm(farmId: Long): Flow<Farm?> = flowOf(null)

        override suspend fun saveFarm(farm: Farm): AppResult<Long> = AppResult.Success(1L)

        override suspend fun ensureDefaultFarmAndPlot(): Pair<Long, Long> = 1L to 1L

        override suspend fun setPrimaryFarm(farmId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun archiveFarm(farmId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override fun observePlots(
            farmId: Long?,
            includeArchived: Boolean,
        ): Flow<List<Plot>> = flowOf(emptyList())

        override fun observePlot(plotId: Long): Flow<Plot?> = flowOf(null)

        override suspend fun savePlot(plot: Plot): AppResult<Long> = AppResult.Success(1L)

        override suspend fun archivePlot(plotId: Long): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeCropTemplates(): Flow<List<CropTemplate>> = flowOf(emptyList())

        override suspend fun getCropTemplate(templateId: Long): CropTemplate? = null

        override suspend fun saveCropTemplate(template: CropTemplate): AppResult<Long> = AppResult.Success(1L)

        override suspend fun ensureStarterTemplatesSeeded() {}

        override fun observeCropCycles(
            plotId: Long?,
            farmId: Long?,
        ): Flow<List<CropCycle>> = flowOf(emptyList())

        override fun observeCropCycle(cropCycleId: Long): Flow<CropCycle?> = flowOf(null)

        override suspend fun createCropCycleWithTemplate(
            cycle: CropCycle,
            generateTasksFromTemplate: Boolean,
            useTeluguTitles: Boolean,
        ): AppResult<Long> = AppResult.Success(1L)

        override suspend fun updateCropCycle(
            cycle: CropCycle,
            recalculateUncompletedTemplateTasks: Boolean,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun updateCropCycleStatus(
            cropCycleId: Long,
            status: CropCycleStatus,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeTasks(
            cropCycleId: Long?,
            plotId: Long?,
            farmId: Long?,
        ): Flow<List<CropTask>> = flowOf(emptyList())

        override fun observeDueAndOverdueTasks(todayEpochDay: Long): Flow<List<CropTask>> = flowOf(emptyList())

        override suspend fun getTask(taskId: Long): CropTask? = null

        override suspend fun saveTask(task: CropTask): AppResult<Long> = AppResult.Success(1L)

        override suspend fun updateTaskStatus(
            taskId: Long,
            status: TaskStatus,
            newDueEpochDay: Long?,
        ): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class StubTimeProvider : TimeProvider {
        override fun nowEpochMillis(): Long = 1_794_326_000_000L

        override fun todayEpochDay(): Long = 20_650L

        override fun elapsedRealtimeMillis(): Long = 1000L

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
