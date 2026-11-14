package com.farmsaathi.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.farmsaathi.app.reminder.DailyTaskReminderWorker
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.media.LocalPhotoStorage
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FarmSaathiApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var localPhotoStorage: LocalPhotoStorage

    @Inject lateinit var farmRepository: FarmRepository

    @Inject lateinit var prefsRepository: UserPreferencesRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun newImageLoader(context: PlatformContext): ImageLoader = localPhotoStorage.buildLowMemoryImageLoader()

    override fun onCreate() {
        super.onCreate()
        DailyTaskReminderWorker.ensureChannel(this)
        appScope.launch {
            farmRepository.ensureStarterTemplatesSeeded()
            val prefs = prefsRepository.userPreferences.first()
            DailyTaskReminderWorker.schedule(
                context = this@FarmSaathiApplication,
                enabled = prefs.dailyReminderEnabled,
                hour = prefs.reminderHour,
                minute = prefs.reminderMinute,
            )
        }
    }
}
