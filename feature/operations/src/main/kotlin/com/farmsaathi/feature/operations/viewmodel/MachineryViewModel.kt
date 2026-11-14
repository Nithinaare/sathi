package com.farmsaathi.feature.operations.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.MachineryRepository
import com.farmsaathi.core.common.repository.StartMachinerySessionRequest
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.Contact
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.FinancialCalculators
import com.farmsaathi.core.model.MachineryBillingMode
import com.farmsaathi.core.model.MachinerySession
import com.farmsaathi.core.model.MachineryUsageType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.Plot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManualMachineryLogRequest(
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val providerContactId: Long? = null,
    val machineName: String,
    val operatorName: String = "",
    val operationType: String,
    val usageType: MachineryUsageType,
    val billingMode: MachineryBillingMode,
    val elapsedMinutes: Long?,
    val totalAmount: MoneyPaise,
    val paidAmount: MoneyPaise,
    val paymentMode: PaymentMode,
    val notes: String = "",
)

data class MachineryUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val sessions: List<MachinerySession> = emptyList(),
    val farms: List<Farm> = emptyList(),
    val plots: List<Plot> = emptyList(),
    val crops: List<CropCycle> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val isSaving: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val activeCueKey: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class MachineryViewModel
@Inject
constructor(
    private val machineryRepo: MachineryRepository,
    private val farmRepo: FarmRepository,
    private val contactRepo: ContactAndSearchRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MachineryUiState> =
        combine(
            prefsRepo.userPreferences,
            machineryRepo.observeSessions(),
            farmRepo.observeFarms(),
            farmRepo.observePlots(),
            farmRepo.observeCropCycles(),
            contactRepo.observeContacts(),
            saving,
            combine(audioEngine.isSpeaking, audioEngine.activeCueKey) { s, k -> s to k },
            error,
        ) { arr ->
            val audioState = arr[7] as Pair<*, *>
            @Suppress("UNCHECKED_CAST")
            MachineryUiState(
                isLoading = false,
                prefs = arr[0] as UserPreferences,
                sessions = arr[1] as List<MachinerySession>,
                farms = arr[2] as List<Farm>,
                plots = arr[3] as List<Plot>,
                crops = arr[4] as List<CropCycle>,
                contacts = arr[5] as List<Contact>,
                isSaving = arr[6] as Boolean,
                isAudioPlaying = audioState.first as Boolean,
                activeCueKey = audioState.second as String?,
                errorMessage = arr[8] as String?,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MachineryUiState())

    fun toggleGuideAudio() {
        toggleAudio(AudioCueCatalog.MACHINERY_HELP)
    }

    fun toggleAudio(cueKey: String) {
        audioEngine.speakCue(
            cueKey,
            uiState.value.prefs.language == AppLanguage.TELUGU,
        )
    }

    fun playErrorCue() {
        audioEngine.playAcousticFeedback(AcousticCue.ERROR)
    }

    fun formatDateTime(millis: Long): String {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        return "${timeProvider.formatDate(millis, useTe)} • ${timeProvider.formatTime(millis)}"
    }

    fun computeElapsedSeconds(session: MachinerySession): Long {
        val ms =
            FinancialCalculators.computeMachineryElapsedMillis(
                startEpochMillis = session.startEpochMillis,
                startElapsedRealtimeMillis = session.startElapsedRealtimeMillis,
                startBootCount = session.startBootCount,
                nowEpochMillis = timeProvider.nowEpochMillis(),
                nowElapsedRealtimeMillis = timeProvider.elapsedRealtimeMillis(),
                nowBootCount = timeProvider.bootCount(),
            )
        return ms / 1000L
    }

    fun startTimerSession(
        request: StartMachinerySessionRequest,
        onStarted: (Long) -> Unit,
    ) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            error.value = null
            when (val res = machineryRepo.startLiveSession(request)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.TIMER_START)
                    onStarted(res.data)
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
            saving.value = false
        }
    }

    fun stopTimerSession(
        sessionId: Long,
        overrideElapsedMinutes: Long?,
        areaCoveredMicroAcres: Long?,
        flatAmount: MoneyPaise?,
        paidAmount: MoneyPaise,
        paymentMode: PaymentMode,
        notes: String?,
        onStopped: () -> Unit,
    ) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            error.value = null
            when (
                val res =
                    machineryRepo.stopLiveSession(
                        sessionId = sessionId,
                        overrideElapsedMinutes = overrideElapsedMinutes,
                        areaCoveredMicroAcres = areaCoveredMicroAcres,
                        flatAmount = flatAmount,
                        paidAmount = paidAmount,
                        paymentMode = paymentMode,
                        notes = notes,
                    )
            ) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.TIMER_STOP)
                    onStopped()
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
            saving.value = false
        }
    }

    fun cancelTimer(
        sessionId: Long,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            machineryRepo.cancelLiveSession(sessionId)
            onDone()
        }
    }

    fun saveManualLog(
        request: ManualMachineryLogRequest,
        onSaved: () -> Unit,
    ) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val session =
                MachinerySession(
                    farmId = request.farmId,
                    plotId = request.plotId,
                    cropCycleId = request.cropCycleId,
                    providerContactId = request.providerContactId,
                    machineName = request.machineName,
                    operatorName = request.operatorName.ifBlank { null },
                    operationType = request.operationType,
                    equipmentKey = request.machineName.trim().lowercase(),
                    usageType = request.usageType,
                    billingMode = request.billingMode,
                    startEpochMillis = timeProvider.nowEpochMillis(),
                    endEpochMillis = timeProvider.nowEpochMillis(),
                    elapsedMinutes = request.elapsedMinutes,
                    computedAmount = request.totalAmount,
                    paidAmount = request.paidAmount,
                    notes = request.notes.ifBlank { null },
                )
            when (val res = machineryRepo.saveManualSession(session, request.paymentMode)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    onSaved()
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
            saving.value = false
        }
    }
}
