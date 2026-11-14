package com.farmsaathi.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AreaUnit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "farm_saathi_prefs",
)

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val farmerName: String = "",
    val villageName: String = "",
    val language: AppLanguage = AppLanguage.TELUGU,
    val audioGuidanceEnabled: Boolean = true,
    val preferredAreaUnit: AreaUnit = AreaUnit.ACRE_GUNTA,
    val activeFarmId: Long? = null,
    val dailyReminderEnabled: Boolean = true,
    val reminderHour: Int = 7,
    val reminderMinute: Int = 0,
    val lastBackupEpochMillis: Long? = null,
)

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun updateProfile(
        farmerName: String,
        villageName: String,
    )

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setAudioGuidanceEnabled(enabled: Boolean)

    suspend fun setPreferredAreaUnit(unit: AreaUnit)

    suspend fun setActiveFarmId(farmId: Long?)

    suspend fun setDailyReminder(
        enabled: Boolean,
        hour: Int,
        minute: Int,
    )

    suspend fun setLastBackupEpochMillis(epochMillis: Long)
}

@Singleton
class DataStoreUserPreferencesRepository
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {
    override val userPreferences: Flow<UserPreferences> =
        dataStore.data.map { prefs ->
            UserPreferences(
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                farmerName = prefs[Keys.FARMER_NAME].orEmpty(),
                villageName = prefs[Keys.VILLAGE_NAME].orEmpty(),
                language =
                prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                    ?: AppLanguage.TELUGU,
                audioGuidanceEnabled = prefs[Keys.AUDIO_GUIDANCE_ENABLED] ?: true,
                preferredAreaUnit =
                prefs[Keys.PREFERRED_AREA_UNIT]
                    ?.let { runCatching { AreaUnit.valueOf(it) }.getOrNull() }
                    ?: AreaUnit.ACRE_GUNTA,
                activeFarmId = prefs[Keys.ACTIVE_FARM_ID],
                dailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: true,
                reminderHour = prefs[Keys.REMINDER_HOUR] ?: 7,
                reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
                lastBackupEpochMillis = prefs[Keys.LAST_BACKUP_EPOCH_MILLIS],
            )
        }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun updateProfile(
        farmerName: String,
        villageName: String,
    ) {
        dataStore.edit {
            it[Keys.FARMER_NAME] = farmerName.trim()
            it[Keys.VILLAGE_NAME] = villageName.trim()
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    override suspend fun setAudioGuidanceEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUDIO_GUIDANCE_ENABLED] = enabled }
    }

    override suspend fun setPreferredAreaUnit(unit: AreaUnit) {
        dataStore.edit { it[Keys.PREFERRED_AREA_UNIT] = unit.name }
    }

    override suspend fun setActiveFarmId(farmId: Long?) {
        dataStore.edit { prefs ->
            if (farmId == null) prefs.remove(Keys.ACTIVE_FARM_ID) else prefs[Keys.ACTIVE_FARM_ID] = farmId
        }
    }

    override suspend fun setDailyReminder(
        enabled: Boolean,
        hour: Int,
        minute: Int,
    ) {
        dataStore.edit {
            it[Keys.DAILY_REMINDER_ENABLED] = enabled
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    override suspend fun setLastBackupEpochMillis(epochMillis: Long) {
        dataStore.edit { it[Keys.LAST_BACKUP_EPOCH_MILLIS] = epochMillis }
    }

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FARMER_NAME = stringPreferencesKey("farmer_name")
        val VILLAGE_NAME = stringPreferencesKey("village_name")
        val LANGUAGE = stringPreferencesKey("language")
        val AUDIO_GUIDANCE_ENABLED = booleanPreferencesKey("audio_guidance_enabled")
        val PREFERRED_AREA_UNIT = stringPreferencesKey("preferred_area_unit")
        val ACTIVE_FARM_ID = longPreferencesKey("active_farm_id")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val LAST_BACKUP_EPOCH_MILLIS = longPreferencesKey("last_backup_epoch_millis")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.userPreferencesDataStore

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository = impl
}
