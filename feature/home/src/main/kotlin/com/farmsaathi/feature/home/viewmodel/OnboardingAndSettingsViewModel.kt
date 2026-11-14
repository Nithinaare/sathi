package com.farmsaathi.feature.home.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmsaathi.core.audio.VoiceNoteManager
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.BackupInspection
import com.farmsaathi.core.common.BackupProvider
import com.farmsaathi.core.common.BackupResult
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.RestoreResult
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.DataOverviewCounts
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.designsystem.components.VoiceRecordingState
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AreaConverter
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.Farm
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject

data class OnboardingUiState(
    val prefs: UserPreferences = UserPreferences(),
    val farmerName: String = "",
    val villageName: String = "",
    val firstFarmName: String = "",
    val acresInput: String = "",
    val subUnitInput: String = "",
    val isSpeaking: Boolean = false,
    val teluguTtsReady: Boolean = false,
    val micTestState: VoiceRecordingState = VoiceRecordingState.Idle,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
    private val prefsRepo: UserPreferencesRepository,
    private val farmRepo: FarmRepository,
    private val audioEngine: AudioGuidanceEngine,
    private val voiceManager: VoiceNoteManager,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val formState = MutableStateFlow(OnboardingForm())
    private val micTestState = MutableStateFlow<VoiceRecordingState>(VoiceRecordingState.Idle)
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<OnboardingUiState> =
        combine(
            prefsRepo.userPreferences,
            formState,
            audioEngine.isSpeaking,
            audioEngine.isTeluguReady,
            micTestState,
            saving,
            error,
        ) { arr ->
            val prefs = arr[0] as UserPreferences
            val form = arr[1] as OnboardingForm
            val speaking = arr[2] as Boolean
            val teluguReady = arr[3] as Boolean
            val mic = arr[4] as VoiceRecordingState
            val isSaving = arr[5] as Boolean
            val err = arr[6] as String?
            OnboardingUiState(
                prefs = prefs,
                farmerName = form.farmerName,
                villageName = form.villageName,
                firstFarmName = form.firstFarmName,
                acresInput = form.acres,
                subUnitInput = form.subUnit,
                isSpeaking = speaking,
                teluguTtsReady = teluguReady,
                micTestState = mic,
                isSaving = isSaving,
                errorMessage = err,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    init {
        audioEngine.refreshTtsReadiness()
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefsRepo.setLanguage(language)
        }
    }

    fun playWelcomeCue(useTelugu: Boolean) {
        audioEngine.speakCue(AudioCueCatalog.ONBOARDING_WELCOME, useTelugu)
    }

    fun playAudioTest(useTelugu: Boolean) {
        audioEngine.refreshTtsReadiness()
        audioEngine.speakCue(AudioCueCatalog.ONBOARDING_AUDIO_TEST, useTelugu)
    }

    fun stopAudio() = audioEngine.stop()

    fun startMicTest() {
        val tempFile = File.createTempFile("mic_test", ".m4a")
        val started = voiceManager.startRecording(tempFile)
        if (started.isSuccess) {
            micTestState.value = VoiceRecordingState.Recording(0L)
            viewModelScope.launch {
                val startMs = System.currentTimeMillis()
                while (isActive && micTestState.value is VoiceRecordingState.Recording) {
                    delay(200)
                    if (micTestState.value is VoiceRecordingState.Recording) {
                        micTestState.value =
                            VoiceRecordingState.Recording(
                                elapsedMillis = System.currentTimeMillis() - startMs,
                                amplitudeNormalized = voiceManager.currentAmplitudeNormalized(),
                            )
                    }
                }
            }
        } else {
            error.value = "Mic unavailable"
        }
    }

    fun stopMicTest() {
        val result = voiceManager.stopRecording()
        micTestState.value =
            if (result != null) {
                VoiceRecordingState.Recorded(result.first.absolutePath, result.second)
            } else {
                VoiceRecordingState.Idle
            }
    }

    fun playMicTestSample(path: String) {
        voiceManager.startPlayback(File(path), onComplete = {}, onError = {})
    }

    fun clearMicTest() {
        voiceManager.stopPlayback()
        (micTestState.value as? VoiceRecordingState.Recorded)?.let { File(it.filePath).delete() }
        micTestState.value = VoiceRecordingState.Idle
    }

    fun updateFarmerName(v: String) {
        formState.value = formState.value.copy(farmerName = v)
    }

    fun updateVillageName(v: String) {
        formState.value = formState.value.copy(villageName = v)
    }

    fun updateFirstFarmName(v: String) {
        formState.value = formState.value.copy(firstFarmName = v)
    }

    fun updateAcres(v: String) {
        formState.value = formState.value.copy(acres = v)
    }

    fun updateSubUnit(v: String) {
        formState.value = formState.value.copy(subUnit = v)
    }

    fun completeOnboarding(
        skipOptionalFarm: Boolean,
        onCompleted: () -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val form = formState.value
            prefsRepo.updateProfile(form.farmerName, form.villageName)

            if (!skipOptionalFarm && form.firstFarmName.isNotBlank()) {
                val unit = uiState.value.prefs.preferredAreaUnit
                val acres = form.acres.trim().toIntOrNull() ?: 0
                val sub = form.subUnit.trim().toDoubleOrNull() ?: 0.0
                val areaMicro =
                    when (unit) {
                        AreaUnit.ACRE_GUNTA -> AreaConverter.fromAcresAndGuntas(acres, sub)
                        AreaUnit.ACRE_CENT -> AreaConverter.fromAcresAndCents(acres, sub)
                        AreaUnit.ACRE_DECIMAL -> {
                            val dec = form.acres.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                            AreaConverter.fromDecimalAcres(dec)
                        }
                    }
                val saved =
                    farmRepo.saveFarm(
                        Farm(
                            name = form.firstFarmName.trim(),
                            villageOrLocation = form.villageName.trim().ifEmpty { null },
                            totalAreaMicroAcres = areaMicro,
                            preferredAreaUnit = unit,
                            isPrimary = true,
                        ),
                    )
                if (saved is AppResult.Success) {
                    prefsRepo.setActiveFarmId(saved.data)
                }
            }
            prefsRepo.setOnboardingCompleted(true)
            audioEngine.playAcousticFeedback(AcousticCue.SAVED)
            saving.value = false
            onCompleted()
        }
    }

    override fun onCleared() {
        voiceManager.stopPlayback()
        super.onCleared()
    }

    private data class OnboardingForm(
        val farmerName: String = "",
        val villageName: String = "",
        val firstFarmName: String = "",
        val acres: String = "",
        val subUnit: String = "",
    )
}

data class SettingsAndBackupUiState(
    val prefs: UserPreferences = UserPreferences(),
    val dataCounts: DataOverviewCounts? = null,
    val isBusy: Boolean = false,
    val lastExportedBackup: BackupResult? = null,
    val pendingRestoreUri: Uri? = null,
    val pendingInspection: BackupInspection? = null,
    val statusMessage: String? = null,
    val isErrorStatus: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val activeCueKey: String? = null,
    val teluguTtsReady: Boolean = false,
    val isSlowSpeech: Boolean = false,
)

@HiltViewModel
class SettingsAndBackupViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val prefsRepo: UserPreferencesRepository,
    private val contactRepo: ContactAndSearchRepository,
    private val backupProvider: BackupProvider,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val speechPrefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val busy = MutableStateFlow(false)
    private val dataCounts = MutableStateFlow<DataOverviewCounts?>(null)
    private val lastExport = MutableStateFlow<BackupResult?>(null)
    private val pendingRestore = MutableStateFlow<Pair<Uri, BackupInspection>?>(null)
    private val statusMsg = MutableStateFlow<Pair<String, Boolean>?>(null)
    private val slowSpeechFlow = MutableStateFlow(false)

    val uiState: StateFlow<SettingsAndBackupUiState> =
        combine(
            prefsRepo.userPreferences,
            dataCounts,
            busy,
            lastExport,
            pendingRestore,
            statusMsg,
            audioEngine.isSpeaking,
            audioEngine.activeCueKey,
            audioEngine.isTeluguReady,
            slowSpeechFlow,
        ) { arr ->
            @Suppress("UNCHECKED_CAST")
            val pending = arr[4] as Pair<Uri, BackupInspection>?

            @Suppress("UNCHECKED_CAST")
            val status = arr[5] as Pair<String, Boolean>?
            SettingsAndBackupUiState(
                prefs = arr[0] as UserPreferences,
                dataCounts = arr[1] as DataOverviewCounts?,
                isBusy = arr[2] as Boolean,
                lastExportedBackup = arr[3] as BackupResult?,
                pendingRestoreUri = pending?.first,
                pendingInspection = pending?.second,
                statusMessage = status?.first,
                isErrorStatus = status?.second ?: false,
                isAudioPlaying = arr[6] as Boolean,
                activeCueKey = arr[7] as String?,
                teluguTtsReady = arr[8] as Boolean,
                isSlowSpeech = arr[9] as Boolean,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsAndBackupUiState())

    init {
        viewModelScope.launch {
            val slow = withContext(dispatchers.io) { speechPrefs.getBoolean(KEY_SLOW_SPEECH, false) }
            slowSpeechFlow.value = slow
            audioEngine.setSpeechRate(if (slow) SLOW_SPEECH_RATE else NORMAL_SPEECH_RATE)
        }
        audioEngine.refreshTtsReadiness()
        refreshCounts()
    }

    fun refreshCounts() {
        viewModelScope.launch {
            dataCounts.value = contactRepo.getDataOverviewCounts()
        }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { prefsRepo.setLanguage(lang) }
    }

    fun updateFarmerName(name: String) {
        val village = uiState.value.prefs.villageName
        viewModelScope.launch {
            prefsRepo.updateProfile(farmerName = name, villageName = village)
            audioEngine.playAcousticFeedback(AcousticCue.SAVED)
        }
    }

    fun setAudioGuidance(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAudioGuidanceEnabled(enabled) }
    }

    fun setSlowSpeech(slow: Boolean) {
        speechPrefs.edit().putBoolean(KEY_SLOW_SPEECH, slow).apply()
        slowSpeechFlow.value = slow
        audioEngine.setSpeechRate(if (slow) SLOW_SPEECH_RATE else NORMAL_SPEECH_RATE)
    }

    fun testTeluguVoice() {
        audioEngine.refreshTtsReadiness()
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        audioEngine.speakDynamicText(
            text =
            if (useTe) {
                "నమస్కారం రైతన్నా! తెలుగు వాయిస్ స్పష్టంగా వినిపిస్తోంది."
            } else {
                "నమస్కారం! Telugu voice test is playing clearly."
            },
            useTelugu = true,
        )
    }

    fun setAreaUnit(unit: AreaUnit) {
        viewModelScope.launch { prefsRepo.setPreferredAreaUnit(unit) }
    }

    fun setDailyReminder(
        enabled: Boolean,
        hour: Int,
    ) {
        viewModelScope.launch { prefsRepo.setDailyReminder(enabled, hour, 0) }
    }

    fun toggleCue(cueKey: String) {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        audioEngine.speakCue(cueKey, useTe)
    }

    fun formatDate(millis: Long): String = timeProvider.formatDate(millis, uiState.value.prefs.language == AppLanguage.TELUGU)

    fun exportBackup() {
        viewModelScope.launch {
            busy.value = true
            statusMsg.value = null
            runCatching { backupProvider.exportBackup() }
                .onSuccess { res ->
                    lastExport.value = res
                    val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
                    val msg =
                        if (useTe) {
                            "బ్యాకప్ సేవ్ అయింది: ${res.fileName}"
                        } else {
                            "Backup saved: ${res.fileName}"
                        }
                    statusMsg.value = msg to false
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                }
                .onFailure { err ->
                    statusMsg.value = (err.message ?: "Backup failed") to true
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            busy.value = false
        }
    }

    fun inspectRestoreUri(uri: Uri) {
        viewModelScope.launch {
            busy.value = true
            statusMsg.value = null
            val inspection = backupProvider.inspectBackup(uri)
            if (inspection.isValid) {
                pendingRestore.value = uri to inspection
            } else {
                statusMsg.value = (inspection.errorReason ?: "Invalid backup zip") to true
                audioEngine.playAcousticFeedback(AcousticCue.ERROR)
            }
            busy.value = false
        }
    }

    fun dismissRestoreDialog() {
        pendingRestore.value = null
    }

    fun confirmRestore(onRestored: () -> Unit) {
        val targetUri = pendingRestore.value?.first ?: return
        viewModelScope.launch {
            busy.value = true
            pendingRestore.value = null
            when (val res = backupProvider.restoreBackup(targetUri)) {
                is RestoreResult.Success -> {
                    val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
                    statusMsg.value = (
                        if (useTe) {
                            "పాత వివరాలు తిరిగి వచ్చాయి (${res.restoredMediaCount} ఫైళ్లు)"
                        } else {
                            "Restored successfully (${res.restoredMediaCount} media files)"
                        }
                        ) to false
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    refreshCounts()
                    onRestored()
                }
                is RestoreResult.AbortedBeforeMutation -> {
                    statusMsg.value = res.reason to true
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
                is RestoreResult.FailedAndRolledBack -> {
                    statusMsg.value = res.reason to true
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
            busy.value = false
        }
    }

    companion object {
        private const val PREFS_NAME = "farm_saathi_audio_prefs"
        private const val KEY_SLOW_SPEECH = "slow_speech"
        private const val SLOW_SPEECH_RATE = 0.78f
        private const val NORMAL_SPEECH_RATE = 0.95f
    }
}
