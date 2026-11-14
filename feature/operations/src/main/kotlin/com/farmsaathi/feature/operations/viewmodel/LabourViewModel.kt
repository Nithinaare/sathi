package com.farmsaathi.feature.operations.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmsaathi.core.audio.VoiceNoteManager
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.BatchAttendanceItem
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.FinanceRepository
import com.farmsaathi.core.common.repository.LabourRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.media.LocalPhotoStorage
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AttendanceStatus
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.ExpenseDetail
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.FinancialCalculators
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.Worker
import com.farmsaathi.core.model.WorkerAttendance
import com.farmsaathi.core.model.WorkerBalanceSummary
import com.farmsaathi.core.model.WorkerPayment
import com.farmsaathi.core.model.WorkerPaymentType
import com.farmsaathi.feature.operations.util.WageReceiptData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class LabourHubMode {
    CREW_CALCULATOR,
    WORKER_BALANCE,
}

data class WorkerFormValidation(
    val nameError: String? = null,
    val wageError: String? = null,
    val halfDayRateError: String? = null,
) {
    val isValid: Boolean
        get() = nameError == null && wageError == null && halfDayRateError == null
}

object WorkerFormValidator {
    fun validate(
        quickMode: Boolean,
        name: String,
        wageStr: String,
        useCustomHalfDayRate: Boolean,
        customHalfDayRateStr: String,
        requiredMessage: String,
        invalidNumberMessage: String,
    ): WorkerFormValidation {
        val wage = MoneyPaise.parseOrNull(wageStr)
        val wageError = if (wage == null || !wage.isPositive()) requiredMessage else null
        val nameError = if (!quickMode && name.isBlank()) requiredMessage else null
        val halfDayRateError =
            if (!quickMode && useCustomHalfDayRate) {
                val custom = MoneyPaise.parseOrNull(customHalfDayRateStr)
                if (custom == null || custom.isNegative()) invalidNumberMessage else null
            } else {
                null
            }
        return WorkerFormValidation(
            nameError = nameError,
            wageError = wageError,
            halfDayRateError = halfDayRateError,
        )
    }
}

data class LabourUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val workers: List<WorkerBalanceSummary> = emptyList(),
    val farms: List<Farm> = emptyList(),
    val crops: List<CropCycle> = emptyList(),
    val selectedDayEpochDay: Long = 0L,
    val todayEpochDay: Long = 0L,
    val hubMode: LabourHubMode = LabourHubMode.CREW_CALCULATOR,
    val dayAttendance: List<WorkerAttendance> = emptyList(),
    val isRecordingVoiceNameForWorkerId: Long? = null,
    val isRecordingFormVoiceName: Boolean = false,
    val playingVoicePath: String? = null,
    val isSaving: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LabourViewModel
@Inject
constructor(
    private val labourRepo: LabourRepository,
    private val farmRepo: FarmRepository,
    private val financeRepo: FinanceRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val photoStorage: LocalPhotoStorage,
    private val voiceManager: VoiceNoteManager,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val selectedDayFlow = MutableStateFlow(timeProvider.todayEpochDay())
    private val hubModeFlow = MutableStateFlow(LabourHubMode.CREW_CALCULATOR)
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val recordingWorkerIdFlow = MutableStateFlow<Long?>(null)
    private val recordingFormFlow = MutableStateFlow(false)
    private val playingVoicePathFlow = MutableStateFlow<String?>(null)

    private val dayAttendanceFlow =
        selectedDayFlow.flatMapLatest { day ->
            labourRepo.observeAttendanceForDay(day)
        }

    val uiState: StateFlow<LabourUiState> =
        combine(
            prefsRepo.userPreferences,
            labourRepo.observeWorkersWithBalance(),
            farmRepo.observeFarms(),
            farmRepo.observeCropCycles(),
            combine(selectedDayFlow, hubModeFlow) { day, mode -> day to mode },
            dayAttendanceFlow,
            combine(recordingWorkerIdFlow, recordingFormFlow, playingVoicePathFlow) { rId, rForm, playPath ->
                Triple(rId, rForm, playPath)
            },
            saving,
            audioEngine.isSpeaking,
            error,
        ) { arr ->
            val navState = arr[4] as Pair<*, *>
            val audioState = arr[6] as Triple<*, *, *>
            @Suppress("UNCHECKED_CAST")
            LabourUiState(
                isLoading = false,
                prefs = arr[0] as UserPreferences,
                workers = arr[1] as List<WorkerBalanceSummary>,
                farms = arr[2] as List<Farm>,
                crops = arr[3] as List<CropCycle>,
                selectedDayEpochDay = navState.first as Long,
                todayEpochDay = timeProvider.todayEpochDay(),
                hubMode = navState.second as LabourHubMode,
                dayAttendance = arr[5] as List<WorkerAttendance>,
                isRecordingVoiceNameForWorkerId = audioState.first as Long?,
                isRecordingFormVoiceName = audioState.second as Boolean,
                playingVoicePath = audioState.third as String?,
                isSaving = arr[7] as Boolean,
                isAudioPlaying = arr[8] as Boolean,
                errorMessage = arr[9] as String?,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LabourUiState())

    fun selectHubMode(mode: LabourHubMode) {
        hubModeFlow.value = mode
    }

    fun selectDay(epochDay: Long) {
        selectedDayFlow.value = epochDay
    }

    fun formatEpochDay(epochDay: Long): String =
        timeProvider.formatEpochDay(
            epochDay,
            uiState.value.prefs.language == AppLanguage.TELUGU,
        )

    fun epochMillisToDay(millis: Long): Long = timeProvider.epochMillisToEpochDay(millis)

    fun epochDayToMillis(day: Long): Long = timeProvider.epochDayToStartMillis(day)

    fun toggleGuideAudio(cueKey: String = AudioCueCatalog.LABOUR_HELP) {
        audioEngine.speakCue(
            cueKey,
            uiState.value.prefs.language == AppLanguage.TELUGU,
        )
    }

    fun speakText(text: String) {
        if (text.isNotBlank()) {
            audioEngine.speakDynamicText(text, uiState.value.prefs.language == AppLanguage.TELUGU)
        }
    }

    fun createTempCaptureFile(): File = photoStorage.createTempCaptureFile()

    fun resolvePhotoFile(path: String): File = photoStorage.resolveFile(path)

    fun saveCapturedPhoto(
        file: File,
        onSaved: (String) -> Unit,
    ) {
        viewModelScope.launch {
            photoStorage.compressAndPersist(file)
                .onSuccess { rel ->
                    file.delete()
                    onSaved(rel)
                }
                .onFailure { error.value = "Photo save failed" }
        }
    }

    fun importGalleryPhoto(
        uri: Uri,
        onSaved: (String) -> Unit,
    ) {
        viewModelScope.launch {
            photoStorage.saveCompressedFromUri(uri)
                .onSuccess(onSaved)
                .onFailure { error.value = "Photo import failed" }
        }
    }

    fun togglePlayWorkerVoice(worker: Worker) {
        togglePlayVoicePath(worker.voiceNamePath, worker.name)
    }

    fun togglePlayVoicePath(
        path: String?,
        fallbackText: String,
    ) {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        if (!path.isNullOrBlank()) {
            if (playingVoicePathFlow.value == path) {
                voiceManager.stopPlayback()
                playingVoicePathFlow.value = null
                return
            }
            val started =
                voiceManager.startPlayback(
                    file = voiceManager.resolveMediaFile(path),
                    onComplete = { playingVoicePathFlow.value = null },
                    onError = {
                        playingVoicePathFlow.value = null
                        if (fallbackText.isNotBlank()) audioEngine.speakDynamicText(fallbackText, useTe)
                    },
                )
            if (started) {
                playingVoicePathFlow.value = path
                return
            }
        }
        if (fallbackText.isNotBlank()) {
            audioEngine.speakDynamicText(fallbackText, useTe)
        }
    }

    fun toggleRecordWorkerVoiceName(worker: Worker) {
        val currentId = recordingWorkerIdFlow.value
        if (currentId == worker.id) {
            val recorded = voiceManager.stopRecording()
            recordingWorkerIdFlow.value = null
            if (recorded != null) {
                val rel = voiceManager.toRelativeMediaPath(recorded.first)
                viewModelScope.launch {
                    labourRepo.saveWorker(worker.copy(voiceNamePath = rel))
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                }
            }
        } else {
            recordingFormFlow.value = false
            if (voiceManager.startRecording().isSuccess) {
                recordingWorkerIdFlow.value = worker.id
            }
        }
    }

    fun toggleRecordFormVoiceName(onRecorded: (String) -> Unit) {
        if (recordingFormFlow.value) {
            val recorded = voiceManager.stopRecording()
            recordingFormFlow.value = false
            if (recorded != null) {
                onRecorded(voiceManager.toRelativeMediaPath(recorded.first))
                audioEngine.playAcousticFeedback(AcousticCue.SAVED)
            }
        } else {
            recordingWorkerIdFlow.value = null
            if (voiceManager.startRecording().isSuccess) {
                recordingFormFlow.value = true
            }
        }
    }

    fun observeWorkerAttendance(workerId: Long) = labourRepo.observeWorkerAttendance(workerId)

    fun observeWorkerPayments(workerId: Long) = labourRepo.observeWorkerPayments(workerId)

    fun saveWorker(
        existing: Worker?,
        name: String,
        phone: String,
        skillRole: String,
        dailyWage: MoneyPaise,
        halfDayPercent: Int = 50,
        notes: String,
        photoPath: String? = existing?.photoPath,
        voiceNamePath: String? = existing?.voiceNamePath,
        halfDayRatePaise: Long? = existing?.halfDayRatePaise,
        onSaved: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
            val nextIndex = uiState.value.workers.size + 1
            val finalName =
                name.trim().ifBlank {
                    if (useTe) "కూలీ #$nextIndex" else "Worker #$nextIndex"
                }
            val model =
                Worker(
                    id = existing?.id ?: 0L,
                    farmId = existing?.farmId ?: uiState.value.prefs.activeFarmId ?: uiState.value.farms.firstOrNull()?.id,
                    name = finalName,
                    phone = phone.ifBlank { null },
                    skillRole = skillRole.ifBlank { null },
                    photoPath = photoPath,
                    voiceNamePath = voiceNamePath,
                    daysWorkedTenths = existing?.daysWorkedTenths ?: 0,
                    defaultDailyWage = dailyWage,
                    defaultHalfDayPercent = halfDayPercent.coerceIn(1, 99),
                    halfDayRatePaise = halfDayRatePaise?.coerceAtLeast(0L),
                    notes = notes.ifBlank { null },
                )
            when (val res = labourRepo.saveWorker(model)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    onSaved(res.data)
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
            saving.value = false
        }
    }

    fun adjustWorkerDays(
        workerId: Long,
        deltaTenths: Int,
        onPreviousKnown: (Int) -> Unit = {},
    ) {
        viewModelScope.launch {
            val prev =
                uiState.value.workers.firstOrNull { it.worker.id == workerId }
                    ?.worker?.daysWorkedTenths ?: 0
            onPreviousKnown(prev)
            labourRepo.adjustWorkerDaysTenths(workerId, deltaTenths)
        }
    }

    fun restoreWorkerDays(
        workerId: Long,
        daysTenths: Int,
    ) {
        viewModelScope.launch {
            labourRepo.setWorkerDaysTenths(workerId, daysTenths)
        }
    }

    fun saveGroupWageExpense(
        workerCount: Int,
        daysTenths: Int,
        dailyRate: MoneyPaise,
        advancePaid: MoneyPaise,
        mode: PaymentMode,
        notes: String,
        onReceipt: (WageReceiptData) -> Unit = {},
        onSaved: () -> Unit,
    ) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
            val calc =
                FinancialCalculators.calculateQuickGroupWage(
                    workerCount = workerCount,
                    daysWorkedTenths = daysTenths,
                    dailyRate = dailyRate,
                    advancePaid = advancePaid,
                )
            if (!calc.netPayable.isPositive()) {
                error.value = "error_invalid_amount"
                audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                saving.value = false
                return@launch
            }
            val farmId = resolveTargetFarmId()
            val now = timeProvider.nowEpochMillis()
            val daysFormatted = formatTenthsValue(daysTenths)
            val title =
                if (useTe) {
                    "$workerCount కూలీలు × $daysFormatted రోజులు"
                } else {
                    "$workerCount Workers × $daysFormatted Days"
                }
            val detail =
                ExpenseDetail(
                    farmId = farmId,
                    category = LedgerCategory.LABOUR,
                    description = title,
                    totalAmount = calc.netPayable,
                    paidAmount = calc.netPayable,
                    paymentMode = mode,
                    expenseDateEpochMillis = now,
                    notes = notes.ifBlank { null },
                )
            when (val res = financeRepo.saveManualExpense(detail, calc.netPayable)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    val rupees = calc.netPayable.toWholeRupeesRounded()
                    val spoken =
                        if (useTe) {
                            "$workerCount గురు కూలీలకు $rupees రూపాయలు ఇచ్చారు."
                        } else {
                            "Paid $rupees rupees to $workerCount workers."
                        }
                    audioEngine.speakDynamicText(spoken, useTe)
                    onReceipt(
                        WageReceiptData(
                            title = if (useTe) "లెక్క చీటీ" else "Wage Receipt",
                            subtitle = if (useTe) "$workerCount గురు కూలీలు" else "$workerCount Workers",
                            fullSunCount = daysTenths / 10,
                            halfSunCount = if ((daysTenths % 10) >= 5) 1 else 0,
                            daysLabel = formatTenthsLabel(daysTenths, useTe),
                            paidAmount = calc.netPayable,
                            remainingBalance = MoneyPaise.ZERO,
                            paymentModeLabel = formatPaymentModeLabel(mode, useTe),
                            dateFormatted = timeProvider.formatDate(now, useTe),
                            useTelugu = useTe,
                            spokenSummary = spoken,
                        ),
                    )
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

    fun settleWorkerBalance(
        summary: WorkerBalanceSummary,
        amount: MoneyPaise,
        mode: PaymentMode,
        resetDaysCounter: Boolean = true,
        onReceipt: (WageReceiptData) -> Unit = {},
        onSaved: () -> Unit,
    ) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
            val resolvedFarmId = summary.worker.farmId ?: resolveTargetFarmId()
            val now = timeProvider.nowEpochMillis()
            val payment =
                WorkerPayment(
                    workerId = summary.worker.id,
                    farmId = resolvedFarmId,
                    paymentType = WorkerPaymentType.SETTLEMENT,
                    amount = amount,
                    paymentDateEpochMillis = now,
                    paymentMode = mode,
                )
            when (val res = labourRepo.recordWorkerPayment(payment)) {
                is AppResult.Success -> {
                    if (resetDaysCounter) {
                        labourRepo.setWorkerDaysTenths(summary.worker.id, 0)
                    }
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    val remaining = (summary.pendingBalance - amount).let { if (it.isNegative()) MoneyPaise.ZERO else it }
                    val spoken = buildWorkerSpokenConfirmation(summary.worker.name, amount, remaining, isAdvance = false, useTe = useTe)
                    audioEngine.speakDynamicText(spoken, useTe)
                    onReceipt(
                        buildWorkerReceipt(
                            worker = summary.worker,
                            totalDaysTenths = summary.totalDaysWorkedTenths,
                            paidAmount = amount,
                            remaining = remaining,
                            mode = mode,
                            dateFormatted = timeProvider.formatDate(now, useTe),
                            useTe = useTe,
                            spoken = spoken,
                        ),
                    )
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

    fun markAttendanceQuick(
        worker: Worker,
        status: AttendanceStatus,
        farmId: Long,
        cropCycleId: Long? = null,
    ) {
        viewModelScope.launch {
            val resolvedFarmId = if (farmId > 0L) farmId else worker.farmId ?: resolveTargetFarmId()
            val accrued =
                FinancialCalculators.calculateWageAccrued(
                    status = status,
                    baseDailyWage = worker.defaultDailyWage,
                    halfDayPercent = worker.defaultHalfDayPercent,
                    halfDayRateOverride = worker.halfDayRatePaise?.let(::MoneyPaise),
                )
            labourRepo.upsertAttendance(
                WorkerAttendance(
                    workerId = worker.id,
                    farmId = resolvedFarmId,
                    cropCycleId = cropCycleId,
                    workDateEpochDay = selectedDayFlow.value,
                    status = status,
                    baseDailyWage = worker.defaultDailyWage,
                    halfDayPercent = worker.defaultHalfDayPercent,
                    wageAccrued = accrued,
                ),
            )
            audioEngine.playAcousticFeedback(AcousticCue.SAVED)
        }
    }

    fun markAllFullDay(onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val activeWorkers = uiState.value.workers
            if (activeWorkers.isEmpty()) {
                onDone(0)
                return@launch
            }
            val farmId = resolveTargetFarmId()
            val items =
                activeWorkers.map { summary ->
                    BatchAttendanceItem(
                        workerId = summary.worker.id,
                        status = AttendanceStatus.FULL_DAY,
                    )
                }
            when (val res = labourRepo.markBatchAttendance(farmId, selectedDayFlow.value, items)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
                    val msg =
                        if (useTe) {
                            "${res.data} గురు కూలీలకు పూర్తి ఎండ హాజరు వేయబడింది."
                        } else {
                            "Marked full day for ${res.data} workers."
                        }
                    audioEngine.speakDynamicText(msg, useTe)
                    onDone(res.data)
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
        }
    }

    fun repeatYesterdayAttendance(onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val farmFilter = uiState.value.prefs.activeFarmId
            when (val res = labourRepo.repeatYesterdayAttendance(selectedDayFlow.value, farmFilter)) {
                is AppResult.Success -> {
                    val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
                    if (res.data > 0) {
                        audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                        val msg =
                            if (useTe) {
                                "నిన్నటి ${res.data} గురు కూలీల హాజరు మళ్ళీ వేయబడింది."
                            } else {
                                "Repeated yesterday's roll call for ${res.data} workers."
                            }
                        audioEngine.speakDynamicText(msg, useTe)
                    } else {
                        audioEngine.playAcousticFeedback(AcousticCue.REMINDER)
                    }
                    onDone(res.data)
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
        }
    }

    fun recordPayment(
        workerId: Long,
        farmId: Long,
        cropCycleId: Long?,
        paymentType: WorkerPaymentType,
        amount: MoneyPaise,
        mode: PaymentMode,
        notes: String,
        onReceipt: (WageReceiptData) -> Unit = {},
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
            val summary = uiState.value.workers.firstOrNull { it.worker.id == workerId }
            val resolvedFarmId = if (farmId > 0L) farmId else summary?.worker?.farmId ?: resolveTargetFarmId()
            val now = timeProvider.nowEpochMillis()
            val payment =
                WorkerPayment(
                    workerId = workerId,
                    farmId = resolvedFarmId,
                    cropCycleId = cropCycleId,
                    paymentType = paymentType,
                    amount = amount,
                    paymentDateEpochMillis = now,
                    paymentMode = mode,
                    notes = notes.ifBlank { null },
                )
            when (val res = labourRepo.recordWorkerPayment(payment)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    val prevBal = summary?.pendingBalance ?: MoneyPaise.ZERO
                    val remaining = (prevBal - amount).let { if (it.isNegative()) MoneyPaise.ZERO else it }
                    val workerName = summary?.worker?.name ?: if (useTe) "కూలీ" else "Worker"
                    val isAdvance = paymentType == WorkerPaymentType.ADVANCE
                    val spoken = buildWorkerSpokenConfirmation(workerName, amount, remaining, isAdvance = isAdvance, useTe = useTe)
                    audioEngine.speakDynamicText(spoken, useTe)
                    val workerObj = summary?.worker ?: Worker(id = workerId, name = workerName, defaultDailyWage = MoneyPaise.ZERO)
                    onReceipt(
                        buildWorkerReceipt(
                            worker = workerObj,
                            totalDaysTenths = summary?.totalDaysWorkedTenths ?: 0,
                            paidAmount = amount,
                            remaining = remaining,
                            mode = mode,
                            dateFormatted = timeProvider.formatDate(now, useTe),
                            useTe = useTe,
                            spoken = spoken,
                        ),
                    )
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

    fun archiveWorker(
        workerId: Long,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            labourRepo.archiveWorker(workerId)
            onDone()
        }
    }

    private suspend fun resolveTargetFarmId(): Long =
        uiState.value.prefs.activeFarmId
            ?: uiState.value.farms.firstOrNull()?.id
            ?: farmRepo.ensureDefaultFarmAndPlot().first

    override fun onCleared() {
        voiceManager.stopPlayback()
        voiceManager.stopRecording()
        super.onCleared()
    }
}

private fun formatTenthsValue(tenths: Int): String =
    if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.5"

private fun formatTenthsLabel(
    tenths: Int,
    useTe: Boolean,
): String {
    val num = formatTenthsValue(tenths)
    return if (useTe) "$num రోజులు" else "$num Days"
}

private fun formatPaymentModeLabel(
    mode: PaymentMode,
    useTe: Boolean,
): String =
    when (mode) {
        PaymentMode.UPI -> "UPI"
        PaymentMode.CASH -> if (useTe) "నగదు" else "Cash"
        PaymentMode.BANK_TRANSFER -> if (useTe) "బ్యాంకు" else "Bank"
        PaymentMode.CHEQUE -> if (useTe) "చెక్కు" else "Cheque"
        PaymentMode.CREDIT -> if (useTe) "అప్పు" else "Credit"
        PaymentMode.OTHER -> if (useTe) "ఇతర" else "Other"
    }

private fun buildWorkerSpokenConfirmation(
    workerName: String,
    paidAmount: MoneyPaise,
    remaining: MoneyPaise,
    isAdvance: Boolean,
    useTe: Boolean,
): String {
    val rupees = paidAmount.toWholeRupeesRounded()
    return when {
        isAdvance ->
            if (useTe) {
                "$workerName కు $rupees రూపాయలు అడ్వాన్స్ ఇచ్చారు."
            } else {
                "Paid $rupees rupees advance to $workerName."
            }
        remaining.isZero() ->
            if (useTe) {
                "$workerName కు $rupees రూపాయలు చెల్లించారు, బాకీ సున్నా."
            } else {
                "Paid $rupees rupees to $workerName, zero balance."
            }
        else -> {
            val remRupees = remaining.toWholeRupeesRounded()
            if (useTe) {
                "$workerName కు $rupees రూపాయలు చెల్లించారు, మిగిలిన బాకీ $remRupees రూపాయలు."
            } else {
                "Paid $rupees rupees to $workerName, remaining balance $remRupees rupees."
            }
        }
    }
}

private fun buildWorkerReceipt(
    worker: Worker,
    totalDaysTenths: Int,
    paidAmount: MoneyPaise,
    remaining: MoneyPaise,
    mode: PaymentMode,
    dateFormatted: String,
    useTe: Boolean,
    spoken: String,
): WageReceiptData =
    WageReceiptData(
        title = if (useTe) "లెక్క చీటీ" else "Wage Receipt",
        subtitle = worker.name,
        workerPhotoPath = worker.photoPath,
        fullSunCount = totalDaysTenths / 10,
        halfSunCount = if ((totalDaysTenths % 10) >= 5) 1 else 0,
        daysLabel = formatTenthsLabel(totalDaysTenths, useTe),
        paidAmount = paidAmount,
        remainingBalance = remaining,
        paymentModeLabel = formatPaymentModeLabel(mode, useTe),
        dateFormatted = dateFormatted,
        useTelugu = useTe,
        spokenSummary = spoken,
    )
