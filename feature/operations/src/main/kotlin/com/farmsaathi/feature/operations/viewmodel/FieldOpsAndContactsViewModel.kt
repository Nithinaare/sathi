package com.farmsaathi.feature.operations.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmsaathi.core.audio.VoiceNoteManager
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.ContactAndSearchRepository
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.FieldOpsRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.media.LocalPhotoStorage
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.Contact
import com.farmsaathi.core.model.ContactType
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.HarvestRecord
import com.farmsaathi.core.model.IrrigationLog
import com.farmsaathi.core.model.IrrigationMethod
import com.farmsaathi.core.model.IrrigationSource
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.SoilHealthRating
import com.farmsaathi.core.model.SoilTestRecord
import com.farmsaathi.core.model.StorageRecord
import com.farmsaathi.core.model.StorageStatus
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

data class FieldOpsUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val farms: List<Farm> = emptyList(),
    val plots: List<Plot> = emptyList(),
    val crops: List<CropCycle> = emptyList(),
    val irrigationLogs: List<IrrigationLog> = emptyList(),
    val harvestRecords: List<HarvestRecord> = emptyList(),
    val storageRecords: List<StorageRecord> = emptyList(),
    val soilTests: List<SoilTestRecord> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val contactFilter: ContactType? = null,
    val contactQuery: String = "",
    val todayEpochDay: Long = 0L,
    val isRecordingVoiceName: Boolean = false,
    val playingVoicePath: String? = null,
    val isSaving: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val activeCueKey: String? = null,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FieldOpsAndContactsViewModel
@Inject
constructor(
    private val fieldOpsRepo: FieldOpsRepository,
    private val contactRepo: ContactAndSearchRepository,
    private val farmRepo: FarmRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val photoStorage: LocalPhotoStorage,
    private val voiceManager: VoiceNoteManager,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val contactFilterFlow = MutableStateFlow<ContactType?>(null)
    private val contactQueryFlow = MutableStateFlow("")
    private val recordingVoiceFlow = MutableStateFlow(false)
    private val playingVoiceFlow = MutableStateFlow<String?>(null)
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val contactsFlow =
        combine(contactFilterFlow, contactQueryFlow) { t, q -> t to q }
            .flatMapLatest { (t, q) -> contactRepo.observeContacts(t, q) }

    val uiState: StateFlow<FieldOpsUiState> =
        combine(
            prefsRepo.userPreferences,
            farmRepo.observeFarms(),
            farmRepo.observePlots(),
            farmRepo.observeCropCycles(),
            fieldOpsRepo.observeIrrigationLogs(),
            fieldOpsRepo.observeHarvestRecords(),
            fieldOpsRepo.observeStorageRecords(),
            fieldOpsRepo.observeSoilTests(),
            contactsFlow,
            contactFilterFlow,
            contactQueryFlow,
            combine(recordingVoiceFlow, playingVoiceFlow) { r, p -> r to p },
            saving,
            audioEngine.isSpeaking,
            audioEngine.activeCueKey,
            error,
        ) { arr ->
            val audioState = arr[11] as Pair<*, *>
            @Suppress("UNCHECKED_CAST")
            FieldOpsUiState(
                isLoading = false,
                prefs = arr[0] as UserPreferences,
                farms = arr[1] as List<Farm>,
                plots = arr[2] as List<Plot>,
                crops = arr[3] as List<CropCycle>,
                irrigationLogs = arr[4] as List<IrrigationLog>,
                harvestRecords = arr[5] as List<HarvestRecord>,
                storageRecords = arr[6] as List<StorageRecord>,
                soilTests = arr[7] as List<SoilTestRecord>,
                contacts = arr[8] as List<Contact>,
                contactFilter = arr[9] as ContactType?,
                contactQuery = arr[10] as String,
                todayEpochDay = timeProvider.todayEpochDay(),
                isRecordingVoiceName = audioState.first as Boolean,
                playingVoicePath = audioState.second as String?,
                isSaving = arr[12] as Boolean,
                isAudioPlaying = arr[13] as Boolean,
                activeCueKey = arr[14] as String?,
                errorMessage = arr[15] as String?,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FieldOpsUiState())

    fun toggleAudio(cueKey: String) {
        audioEngine.speakCue(cueKey, uiState.value.prefs.language == AppLanguage.TELUGU)
    }

    fun setContactFilter(type: ContactType?) {
        contactFilterFlow.value = type
    }

    fun setContactQuery(q: String) {
        contactQueryFlow.value = q
    }

    fun formatDate(millis: Long): String = timeProvider.formatDate(millis, uiState.value.prefs.language == AppLanguage.TELUGU)

    fun formatEpochDay(day: Long): String = timeProvider.formatEpochDay(day, uiState.value.prefs.language == AppLanguage.TELUGU)

    fun epochMillisToDay(millis: Long): Long = timeProvider.epochMillisToEpochDay(millis)

    fun epochDayToMillis(day: Long): Long = timeProvider.epochDayToStartMillis(day)

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

    fun formatDateTime(epochMillis: Long): String {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        return "${timeProvider.formatDate(epochMillis, useTe)} • ${timeProvider.formatTime(epochMillis)}"
    }

    fun togglePlayVoicePath(
        path: String?,
        fallbackName: String,
    ) {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        if (!path.isNullOrBlank()) {
            if (playingVoiceFlow.value == path) {
                voiceManager.stopPlayback()
                playingVoiceFlow.value = null
                return
            }
            val started =
                voiceManager.startPlayback(
                    file = voiceManager.resolveMediaFile(path),
                    onComplete = { playingVoiceFlow.value = null },
                    onError = {
                        playingVoiceFlow.value = null
                        if (fallbackName.isNotBlank()) audioEngine.speakDynamicText(fallbackName, useTe)
                    },
                )
            if (started) {
                playingVoiceFlow.value = path
                return
            }
        }
        if (fallbackName.isNotBlank()) audioEngine.speakDynamicText(fallbackName, useTe)
    }

    fun toggleRecordContactVoice(onRecorded: (String) -> Unit) {
        if (recordingVoiceFlow.value) {
            val res = voiceManager.stopRecording()
            recordingVoiceFlow.value = false
            if (res != null) {
                onRecorded(voiceManager.toRelativeMediaPath(res.first))
                audioEngine.playAcousticFeedback(AcousticCue.SAVED)
            }
        } else if (voiceManager.startRecording().isSuccess) {
            recordingVoiceFlow.value = true
        }
    }

    fun saveIrrigationQuick(
        selectedPlot: Plot?,
        cropCycleId: Long?,
        dateMillis: Long,
        durationMins: Int?,
        source: IrrigationSource,
        method: IrrigationMethod,
        nextDay: Long?,
        photoPath: String?,
        notes: String,
        existingId: Long? = null,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
            val crop = cropCycleId?.let { cid -> uiState.value.crops.firstOrNull { it.id == cid } }
            val (farmId, plotId) =
                when {
                    crop != null -> crop.farmId to crop.plotId
                    selectedPlot != null -> selectedPlot.farmId to selectedPlot.id
                    else -> farmRepo.ensureDefaultFarmAndPlot()
                }
            fieldOpsRepo.saveIrrigationLog(
                IrrigationLog(
                    id = existingId ?: 0L,
                    farmId = farmId,
                    plotId = plotId,
                    cropCycleId = cropCycleId,
                    irrigationDateEpochMillis = dateMillis,
                    durationMinutes = durationMins,
                    source = source,
                    method = method,
                    nextWateringEpochDay = nextDay,
                    photoPath = photoPath,
                    notes = notes.ifBlank { null },
                ),
            )
            audioEngine.playAcousticFeedback(AcousticCue.SAVED)
            val cropTitle = crop?.cropName ?: if (useTe) "పొలం" else "Farm"
            audioEngine.speakDynamicText(
                if (useTe) "$cropTitle నీటి తడి సేవ్ అయింది." else "$cropTitle watering saved.",
                useTe,
            )
            saving.value = false
            onSaved()
        }
    }

    fun archiveIrrigationLog(
        log: IrrigationLog,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            fieldOpsRepo.saveIrrigationLog(log.copy(isArchived = true))
            audioEngine.playAcousticFeedback(AcousticCue.SAVED)
            onDone()
        }
    }

    fun saveIrrigation(
        plot: Plot,
        cropCycleId: Long?,
        dateMillis: Long,
        durationMins: Int?,
        source: IrrigationSource,
        method: IrrigationMethod,
        nextDay: Long?,
        notes: String,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            fieldOpsRepo.saveIrrigationLog(
                IrrigationLog(
                    farmId = plot.farmId,
                    plotId = plot.id,
                    cropCycleId = cropCycleId,
                    irrigationDateEpochMillis = dateMillis,
                    durationMinutes = durationMins,
                    source = source,
                    method = method,
                    nextWateringEpochDay = nextDay,
                    notes = notes.ifBlank { null },
                ),
            )
            audioEngine.playAcousticFeedback(AcousticCue.SAVED)
            saving.value = false
            onSaved()
        }
    }

    fun saveHarvestAndOptionalStorage(
        cycle: CropCycle,
        harvestDay: Long,
        qtyMilli: Long,
        unit: QuantityUnit,
        bagCount: Int?,
        qualityGrade: String,
        storageLocation: String,
        notes: String,
        existingId: Long? = null,
        photoPath: String? = null,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            when (
                val res =
                    fieldOpsRepo.saveHarvestRecord(
                        HarvestRecord(
                            id = existingId ?: 0L,
                            farmId = cycle.farmId,
                            plotId = cycle.plotId,
                            cropCycleId = cycle.id,
                            harvestDateEpochDay = harvestDay,
                            quantityMilliUnits = qtyMilli,
                            quantityUnit = unit,
                            bagCount = bagCount,
                            qualityGrade = qualityGrade.ifBlank { null },
                            photoPath = photoPath,
                            notes = notes.ifBlank { null },
                        ),
                    )
            ) {
                is AppResult.Success -> {
                    if (existingId == null && storageLocation.isNotBlank()) {
                        fieldOpsRepo.saveStorageRecord(
                            StorageRecord(
                                farmId = cycle.farmId,
                                cropCycleId = cycle.id,
                                harvestRecordId = res.data,
                                storageLocation = storageLocation,
                                quantityMilliUnits = qtyMilli,
                                remainingQuantityMilliUnits = qtyMilli,
                                quantityUnit = unit,
                                storedDateEpochDay = harvestDay,
                                bagCount = bagCount,
                                status = StorageStatus.IN_STORAGE,
                                notes = notes.ifBlank { null },
                            ),
                        )
                    }
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

    fun saveSoilTest(
        plot: Plot,
        testDay: Long,
        labName: String,
        phTenths: Int?,
        nitrogenTenths: Int?,
        phosphorusTenths: Int?,
        potassiumTenths: Int?,
        rating: SoilHealthRating?,
        photoPath: String?,
        notes: String,
        existingId: Long? = null,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            fieldOpsRepo.saveSoilTest(
                SoilTestRecord(
                    id = existingId ?: 0L,
                    farmId = plot.farmId,
                    plotId = plot.id,
                    testDateEpochDay = testDay,
                    labName = labName.ifBlank { null },
                    phTenths = phTenths,
                    nitrogenKgPerHaTenths = nitrogenTenths,
                    phosphorusKgPerHaTenths = phosphorusTenths,
                    potassiumKgPerHaTenths = potassiumTenths,
                    overallRating = rating,
                    reportPhotoPath = photoPath,
                    recommendationsNote = notes.ifBlank { null },
                ),
            )
            audioEngine.playAcousticFeedback(AcousticCue.SAVED)
            saving.value = false
            onSaved()
        }
    }

    fun saveContact(
        existing: Contact?,
        name: String,
        phone: String,
        type: ContactType,
        village: String,
        notes: String,
        voiceNamePath: String? = existing?.voiceNamePath,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
            val fallbackLabel =
                if (useTe) "పరిచయం #${uiState.value.contacts.size + 1}" else "Contact #${uiState.value.contacts.size + 1}"
            val finalName =
                name.trim().ifBlank {
                    if (!voiceNamePath.isNullOrBlank() || phone.isNotBlank()) fallbackLabel else ""
                }
            when (
                val res =
                    contactRepo.saveContact(
                        Contact(
                            id = existing?.id ?: 0L,
                            name = finalName,
                            phone = phone.ifBlank { null },
                            contactType = type,
                            villageOrMandi = village.ifBlank { null },
                            voiceNamePath = voiceNamePath,
                            notes = notes.ifBlank { null },
                        ),
                    )
            ) {
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

    fun archiveContact(
        contactId: Long,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            contactRepo.archiveContact(contactId)
            onDone()
        }
    }

    fun playErrorCue() {
        audioEngine.playAcousticFeedback(AcousticCue.ERROR)
    }

    fun speakContactDetails(
        name: String,
        roleLabel: String,
        phone: String?,
    ) {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        val text =
            listOfNotNull(
                name.takeIf { it.isNotBlank() },
                roleLabel.takeIf { it.isNotBlank() },
                phone?.takeIf { it.isNotBlank() },
            ).joinToString(". ")
        if (text.isNotBlank()) {
            audioEngine.speakDynamicText(text, useTe)
        }
    }

    fun speakBlankContactVoiceHint() {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        val prompt =
            if (useTe) {
                "ముందు పేరు రాయండి లేదా మైక్ నొక్కి పేరు రికార్డ్ చేయండి."
            } else {
                "Enter a name first or tap the mic button to record a voice name."
            }
        audioEngine.speakDynamicText(prompt, useTe)
    }

    override fun onCleared() {
        voiceManager.stopPlayback()
        voiceManager.stopRecording()
        super.onCleared()
    }
}
