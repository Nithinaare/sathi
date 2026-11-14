package com.farmsaathi.feature.finance.viewmodel

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
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.FieldOpsRepository
import com.farmsaathi.core.common.repository.FinanceRepository
import com.farmsaathi.core.common.repository.SeasonFinancialTotals
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.designsystem.components.VoiceRecordingState
import com.farmsaathi.core.media.LocalPhotoStorage
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.Contact
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropSale
import com.farmsaathi.core.model.ExpenseDetail
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.FinancialCalculators
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.StorageRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class FinanceUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val totals: SeasonFinancialTotals =
        SeasonFinancialTotals(
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
            MoneyPaise.ZERO,
        ),
    val ledgerEntries: List<LedgerEntry> = emptyList(),
    val sales: List<CropSale> = emptyList(),
    val farms: List<Farm> = emptyList(),
    val plots: List<Plot> = emptyList(),
    val crops: List<CropCycle> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val storageLots: List<StorageRecord> = emptyList(),
    val filterType: LedgerEntryType? = null,
    val searchQuery: String = "",
    val recordingState: VoiceRecordingState = VoiceRecordingState.Idle,
    val playingPath: String? = null,
    val playbackPosMs: Long = 0L,
    val playbackDurMs: Long = 0L,
    val isSaving: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val activeCueKey: String? = null,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinanceViewModel
@Inject
constructor(
    private val financeRepo: FinanceRepository,
    private val farmRepo: FarmRepository,
    private val fieldOpsRepo: FieldOpsRepository,
    private val contactRepo: ContactAndSearchRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val photoStorage: LocalPhotoStorage,
    private val voiceManager: VoiceNoteManager,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val filterFlow = MutableStateFlow<LedgerEntryType?>(null)
    private val queryFlow = MutableStateFlow("")
    private val recordingState = MutableStateFlow<VoiceRecordingState>(VoiceRecordingState.Idle)
    private val playbackInfo = MutableStateFlow(Triple<String?, Long, Long>(null, 0L, 0L))
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val ledgerFlow =
        combine(filterFlow, queryFlow) { type, q -> type to q }
            .flatMapLatest { (type, q) -> financeRepo.observeLedgerEntries(entryType = type, query = q) }

    val uiState: StateFlow<FinanceUiState> =
        combine(
            prefsRepo.userPreferences,
            financeRepo.observeSeasonTotals(),
            ledgerFlow,
            financeRepo.observeCropSales(),
            farmRepo.observeFarms(),
            farmRepo.observePlots(),
            farmRepo.observeCropCycles(),
            contactRepo.observeContacts(),
            fieldOpsRepo.observeStorageRecords(),
            filterFlow,
            queryFlow,
            recordingState,
            playbackInfo,
            saving,
            audioEngine.isSpeaking,
            audioEngine.activeCueKey,
            error,
        ) { arr ->
            val play = arr[12] as Triple<*, *, *>
            @Suppress("UNCHECKED_CAST")
            FinanceUiState(
                isLoading = false,
                prefs = arr[0] as UserPreferences,
                totals = arr[1] as SeasonFinancialTotals,
                ledgerEntries = arr[2] as List<LedgerEntry>,
                sales = arr[3] as List<CropSale>,
                farms = arr[4] as List<Farm>,
                plots = arr[5] as List<Plot>,
                crops = arr[6] as List<CropCycle>,
                contacts = arr[7] as List<Contact>,
                storageLots = arr[8] as List<StorageRecord>,
                filterType = arr[9] as LedgerEntryType?,
                searchQuery = arr[10] as String,
                recordingState = arr[11] as VoiceRecordingState,
                playingPath = play.first as String?,
                playbackPosMs = play.second as Long,
                playbackDurMs = play.third as Long,
                isSaving = arr[13] as Boolean,
                isAudioPlaying = arr[14] as Boolean,
                activeCueKey = arr[15] as String?,
                errorMessage = arr[16] as String?,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState())

    fun setFilter(type: LedgerEntryType?) {
        filterFlow.value = type
    }

    fun setSearch(q: String) {
        queryFlow.value = q
    }

    fun toggleAudio(cueKey: String) {
        audioEngine.speakCue(cueKey, uiState.value.prefs.language == AppLanguage.TELUGU)
    }

    fun formatDate(millis: Long): String = timeProvider.formatDate(millis, uiState.value.prefs.language == AppLanguage.TELUGU)

    fun formatDateTime(millis: Long): String {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        return "${timeProvider.formatDate(millis, useTe)} • ${timeProvider.formatTime(millis)}"
    }

    fun resolvePhotoFile(path: String): File = photoStorage.resolveFile(path)

    fun createTempCaptureFile(): File = photoStorage.createTempCaptureFile()

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

    fun capturePhotoWithExif(
        file: File,
        onSaved: (String, Long?) -> Unit,
    ) {
        viewModelScope.launch {
            photoStorage.compressAndPersistWithExif(file)
                .onSuccess { meta ->
                    file.delete()
                    onSaved(meta.relativePath, meta.capturedAtEpochMillis)
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

    fun importGalleryWithExif(
        uri: Uri,
        onSaved: (String, Long?) -> Unit,
    ) {
        viewModelScope.launch {
            photoStorage.saveCompressedWithExifFromUri(uri)
                .onSuccess { meta ->
                    onSaved(meta.relativePath, meta.capturedAtEpochMillis)
                }
                .onFailure { error.value = "Photo import failed" }
        }
    }

    fun startVoiceNote() {
        if (voiceManager.startRecording().isSuccess) {
            recordingState.value = VoiceRecordingState.Recording(0L)
            viewModelScope.launch {
                val startMs = timeProvider.nowEpochMillis()
                while (isActive && recordingState.value is VoiceRecordingState.Recording) {
                    delay(200)
                    if (recordingState.value is VoiceRecordingState.Recording) {
                        recordingState.value =
                            VoiceRecordingState.Recording(
                                timeProvider.nowEpochMillis() - startMs,
                                voiceManager.currentAmplitudeNormalized(),
                            )
                    }
                }
            }
        } else {
            error.value = "Mic recording failed"
            audioEngine.playAcousticFeedback(AcousticCue.ERROR)
        }
    }

    fun stopVoiceNote() {
        val res = voiceManager.stopRecording()
        recordingState.value =
            if (res != null) {
                VoiceRecordingState.Recorded(voiceManager.toRelativeMediaPath(res.first), res.second)
            } else {
                VoiceRecordingState.Idle
            }
    }

    fun clearVoiceNote() {
        voiceManager.stopPlayback()
        playbackInfo.value = Triple(null, 0L, 0L)
        recordingState.value = VoiceRecordingState.Idle
    }

    fun togglePlayVoice(
        path: String,
        knownDur: Long,
    ) {
        if (playbackInfo.value.first == path) {
            voiceManager.stopPlayback()
            playbackInfo.value = Triple(null, 0L, 0L)
            return
        }
        val started =
            voiceManager.startPlayback(
                file = voiceManager.resolveMediaFile(path),
                onComplete = { playbackInfo.value = Triple(null, 0L, 0L) },
                onError = { playbackInfo.value = Triple(null, 0L, 0L) },
            )
        if (started) {
            val dur = maxOf(knownDur, voiceManager.durationMillis())
            playbackInfo.value = Triple(path, 0L, dur)
            viewModelScope.launch {
                while (isActive && playbackInfo.value.first == path && voiceManager.isPlaying()) {
                    playbackInfo.value = Triple(path, voiceManager.currentPositionMillis(), dur)
                    delay(200)
                }
            }
        }
    }

    fun seekVoice(posMillis: Long) {
        voiceManager.seekTo(posMillis)
        playbackInfo.value = playbackInfo.value.copy(second = posMillis)
    }

    fun saveExpense(
        farmId: Long,
        plotId: Long?,
        cropCycleId: Long?,
        category: LedgerCategory,
        description: String,
        vendor: String,
        totalAmount: MoneyPaise,
        paidAmount: MoneyPaise,
        paymentMode: PaymentMode,
        dateMillis: Long,
        photoPath: String?,
        notes: String,
        onSaved: () -> Unit,
    ) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
            val resolvedFarmId =
                if (farmId > 0L) farmId else farmRepo.ensureDefaultFarmAndPlot().first
            val voice = recordingState.value as? VoiceRecordingState.Recorded
            val detail =
                ExpenseDetail(
                    farmId = resolvedFarmId,
                    plotId = plotId,
                    cropCycleId = cropCycleId,
                    category = category,
                    description = description.ifBlank { "Farm Expense" },
                    vendorNameSnapshot = vendor.ifBlank { null },
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    paymentMode = paymentMode,
                    expenseDateEpochMillis = dateMillis,
                    receiptPhotoPath = photoPath,
                    voiceNotePath = voice?.filePath,
                    voiceNoteDurationMillis = voice?.durationMillis,
                    notes = notes.ifBlank { null },
                )
            when (val res = financeRepo.saveManualExpense(detail, paidAmount)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    val rupees = totalAmount.toWholeRupeesRounded()
                    audioEngine.speakDynamicText(
                        if (useTe) "$rupees రూపాయలు ఖర్చు సేవ్ అయింది." else "$rupees rupees expense saved.",
                        useTe,
                    )
                    clearVoiceNote()
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

    fun saveCropSale(
        cropCycle: CropCycle,
        storageId: Long?,
        buyerName: String,
        mandiLocation: String,
        qtyMilliUnits: Long,
        unit: QuantityUnit,
        ratePerUnit: MoneyPaise,
        deductions: MoneyPaise,
        amountReceived: MoneyPaise,
        paymentMode: PaymentMode,
        dateMillis: Long,
        notes: String,
        onSaved: () -> Unit,
    ) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val gross = MoneyPaise.multiplyMilliUnits(ratePerUnit, qtyMilliUnits)
            val net = (gross - deductions).let { if (it.isNegative()) MoneyPaise.ZERO else it }
            val safeReceived = amountReceived.coerceIn(MoneyPaise.ZERO, net)
            val sale =
                CropSale(
                    farmId = cropCycle.farmId,
                    plotId = cropCycle.plotId,
                    cropCycleId = cropCycle.id,
                    storageRecordId = storageId,
                    buyerNameSnapshot = buyerName,
                    mandiOrLocation = mandiLocation.ifBlank { null },
                    quantityMilliUnits = qtyMilliUnits,
                    quantityUnit = unit,
                    ratePerUnit = ratePerUnit,
                    grossAmount = gross,
                    deductions = deductions,
                    netTotalAmount = net,
                    amountReceived = safeReceived,
                    paymentStatus = FinancialCalculators.derivePaymentStatus(net, safeReceived),
                    paymentMode = paymentMode,
                    saleDateEpochMillis = dateMillis,
                    notes = notes.ifBlank { null },
                )
            when (val res = financeRepo.saveCropSale(sale)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
                    val rupees = net.toWholeRupeesRounded()
                    audioEngine.speakDynamicText(
                        if (useTe) "$rupees రూపాయలు అమ్మకం సేవ్ అయింది." else "$rupees rupees sale saved.",
                        useTe,
                    )
                    clearVoiceNote()
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

    fun saveDirectFarmSale(
        farmId: Long,
        buyerName: String,
        amount: MoneyPaise,
        receivedAmount: MoneyPaise,
        paymentMode: PaymentMode,
        dateMillis: Long,
        notes: String,
        onSaved: () -> Unit,
    ) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
            val resolvedFarmId =
                if (farmId > 0L) farmId else farmRepo.ensureDefaultFarmAndPlot().first
            val safeReceived = receivedAmount.coerceIn(MoneyPaise.ZERO, amount)
            val title = buyerName.ifBlank { "Crop Sale" }
            when (
                val res =
                    financeRepo.saveManualIncome(
                        farmId = resolvedFarmId,
                        plotId = null,
                        cropCycleId = null,
                        title = title,
                        amount = amount,
                        settledAmount = safeReceived,
                        paymentMode = paymentMode,
                        dateEpochMillis = dateMillis,
                        notes = notes.ifBlank { null },
                    )
            ) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    val rupees = amount.toWholeRupeesRounded()
                    audioEngine.speakDynamicText(
                        if (useTe) "$rupees రూపాయలు అమ్మకం సేవ్ అయింది." else "$rupees rupees sale saved.",
                        useTe,
                    )
                    clearVoiceNote()
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

    fun recordSettlement(
        ledgerEntryId: Long,
        additional: MoneyPaise,
        mode: PaymentMode,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            when (val res = financeRepo.recordLedgerSettlement(ledgerEntryId, additional, mode)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    onDone()
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
        }
    }

    fun voidEntry(
        ledgerEntryId: Long,
        reason: String,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            financeRepo.voidLedgerEntry(ledgerEntryId, reason.ifBlank { "Cancelled by user" })
            audioEngine.playAcousticFeedback(AcousticCue.SAVED)
            onDone()
        }
    }

    override fun onCleared() {
        voiceManager.stopPlayback()
        super.onCleared()
    }

    companion object {
        val EXPENSE_CATEGORIES =
            listOf(
                LedgerCategory.SEEDS,
                LedgerCategory.FERTILIZERS,
                LedgerCategory.PESTICIDES,
                LedgerCategory.LABOUR,
                LedgerCategory.MACHINERY,
                LedgerCategory.DIESEL,
                LedgerCategory.TRANSPORT,
                LedgerCategory.IRRIGATION,
                LedgerCategory.REPAIRS,
                LedgerCategory.OTHER_EXPENSE,
            )
        const val ACCOUNTS_CUE = AudioCueCatalog.ACCOUNTS_SUMMARY
        const val SALES_CUE = AudioCueCatalog.SALES_SUMMARY
        const val EXPENSE_FORM_CUE = AudioCueCatalog.EXPENSE_FORM_HELP
        const val SALE_FORM_CUE = AudioCueCatalog.SALE_FORM_HELP
        const val LEDGER_DETAIL_CUE = AudioCueCatalog.LEDGER_DETAIL_HELP
    }
}
