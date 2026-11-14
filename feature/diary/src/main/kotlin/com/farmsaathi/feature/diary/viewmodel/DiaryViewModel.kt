package com.farmsaathi.feature.diary.viewmodel

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
import com.farmsaathi.core.common.repository.DiaryRepository
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.designsystem.components.VoiceRecordingState
import com.farmsaathi.core.media.LocalPhotoStorage
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.DiaryCategory
import com.farmsaathi.core.model.DiaryEntry
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.Plot
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

data class DiaryUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val entries: List<DiaryEntry> = emptyList(),
    val farms: List<Farm> = emptyList(),
    val plots: List<Plot> = emptyList(),
    val crops: List<CropCycle> = emptyList(),
    val searchQuery: String = "",
    val recordingState: VoiceRecordingState = VoiceRecordingState.Idle,
    val playingPath: String? = null,
    val playbackPositionMillis: Long = 0L,
    val playbackDurationMillis: Long = 0L,
    val isSaving: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val activeCueKey: String? = null,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModel
@Inject
constructor(
    private val diaryRepo: DiaryRepository,
    private val farmRepo: FarmRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val photoStorage: LocalPhotoStorage,
    private val voiceManager: VoiceNoteManager,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val queryFlow = MutableStateFlow("")
    private val recordingState = MutableStateFlow<VoiceRecordingState>(VoiceRecordingState.Idle)
    private val playbackState = MutableStateFlow(PlaybackStatus())
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val entriesFlow =
        queryFlow.flatMapLatest { q ->
            diaryRepo.observeDiaryEntries(query = q)
        }

    val uiState: StateFlow<DiaryUiState> =
        combine(
            prefsRepo.userPreferences,
            entriesFlow,
            farmRepo.observeFarms(),
            farmRepo.observePlots(),
            farmRepo.observeCropCycles(),
            queryFlow,
            recordingState,
            playbackState,
            saving,
            audioEngine.isSpeaking,
            audioEngine.activeCueKey,
            error,
        ) { arr ->
            val play = arr[7] as PlaybackStatus
            @Suppress("UNCHECKED_CAST")
            DiaryUiState(
                isLoading = false,
                prefs = arr[0] as UserPreferences,
                entries = arr[1] as List<DiaryEntry>,
                farms = arr[2] as List<Farm>,
                plots = arr[3] as List<Plot>,
                crops = arr[4] as List<CropCycle>,
                searchQuery = arr[5] as String,
                recordingState = arr[6] as VoiceRecordingState,
                playingPath = play.path,
                playbackPositionMillis = play.positionMillis,
                playbackDurationMillis = play.durationMillis,
                isSaving = arr[8] as Boolean,
                isAudioPlaying = arr[9] as Boolean,
                activeCueKey = arr[10] as String?,
                errorMessage = arr[11] as String?,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState())

    fun updateSearch(q: String) {
        queryFlow.value = q
    }

    fun toggleAudio(cueKey: String) {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        audioEngine.speakCue(cueKey, useTe)
    }

    fun toggleGuideAudio() {
        toggleAudio(AudioCueCatalog.DIARY_HELP)
    }

    fun formatDate(millis: Long): String = timeProvider.formatDate(millis, uiState.value.prefs.language == AppLanguage.TELUGU)

    fun formatTime(millis: Long): String = timeProvider.formatTime(millis)

    fun resolveFile(path: String): File = photoStorage.resolveFile(path)

    fun createTempCaptureFile(): File = photoStorage.createTempCaptureFile()

    fun saveCapturedPhoto(
        tempFile: File,
        onSaved: (String) -> Unit,
    ) {
        viewModelScope.launch {
            photoStorage.compressAndPersist(tempFile)
                .onSuccess { rel ->
                    tempFile.delete()
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
                .onFailure { error.value = "Gallery import failed" }
        }
    }

    fun initVoiceState(
        existingPath: String?,
        existingDuration: Long?,
    ) {
        recordingState.value =
            if (!existingPath.isNullOrBlank()) {
                VoiceRecordingState.Recorded(existingPath, existingDuration ?: 0L)
            } else {
                VoiceRecordingState.Idle
            }
    }

    fun startVoiceRecording() {
        val started = voiceManager.startRecording()
        if (started.isSuccess) {
            recordingState.value = VoiceRecordingState.Recording(0L)
            viewModelScope.launch {
                val startMs = timeProvider.nowEpochMillis()
                while (isActive && recordingState.value is VoiceRecordingState.Recording) {
                    delay(200)
                    if (recordingState.value is VoiceRecordingState.Recording) {
                        recordingState.value =
                            VoiceRecordingState.Recording(
                                elapsedMillis = timeProvider.nowEpochMillis() - startMs,
                                amplitudeNormalized = voiceManager.currentAmplitudeNormalized(),
                            )
                    }
                }
            }
        } else {
            error.value = "Mic recording failed"
            audioEngine.playAcousticFeedback(AcousticCue.ERROR)
        }
    }

    fun stopVoiceRecording() {
        val res = voiceManager.stopRecording()
        recordingState.value =
            if (res != null) {
                VoiceRecordingState.Recorded(voiceManager.toRelativeMediaPath(res.first), res.second)
            } else {
                VoiceRecordingState.Idle
            }
    }

    fun clearVoiceRecording() {
        voiceManager.stopPlayback()
        playbackState.value = PlaybackStatus()
        recordingState.value = VoiceRecordingState.Idle
    }

    fun togglePlayVoice(
        storedPath: String,
        knownDurationMillis: Long,
    ) {
        val file = voiceManager.resolveMediaFile(storedPath)
        if (playbackState.value.path == storedPath) {
            voiceManager.stopPlayback()
            playbackState.value = PlaybackStatus()
            return
        }
        val started =
            voiceManager.startPlayback(
                file = file,
                onComplete = { playbackState.value = PlaybackStatus() },
                onError = {
                    playbackState.value = PlaybackStatus()
                    error.value = "Audio file missing or unreadable"
                },
            )
        if (started) {
            val dur = maxOf(knownDurationMillis, voiceManager.durationMillis())
            playbackState.value = PlaybackStatus(storedPath, 0L, dur)
            viewModelScope.launch {
                while (isActive && playbackState.value.path == storedPath && voiceManager.isPlaying()) {
                    playbackState.value =
                        PlaybackStatus(
                            path = storedPath,
                            positionMillis = voiceManager.currentPositionMillis(),
                            durationMillis = dur,
                        )
                    delay(200)
                }
            }
        }
    }

    fun seekVoice(posMillis: Long) {
        voiceManager.seekTo(posMillis)
        playbackState.value = playbackState.value.copy(positionMillis = posMillis)
    }

    fun saveDiaryEntry(
        existingId: Long?,
        farmId: Long,
        plotId: Long?,
        cropCycleId: Long?,
        entryDateMillis: Long,
        category: DiaryCategory,
        title: String,
        notes: String,
        photoPaths: List<String>,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val resolvedFarmId =
                if (farmId > 0L) farmId else farmRepo.ensureDefaultFarmAndPlot().first
            val rec = recordingState.value as? VoiceRecordingState.Recorded
            val model =
                DiaryEntry(
                    id = existingId ?: 0L,
                    farmId = resolvedFarmId,
                    plotId = plotId,
                    cropCycleId = cropCycleId,
                    entryDateEpochMillis = entryDateMillis,
                    category = category,
                    title = title,
                    notes = notes.ifBlank { null },
                    photoPaths = photoPaths,
                    voiceNotePath = rec?.filePath,
                    voiceNoteDurationMillis = rec?.durationMillis,
                )
            when (val res = diaryRepo.saveDiaryEntry(model)) {
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

    fun deleteDiaryEntry(
        entryId: Long,
        onDeleted: () -> Unit = {},
    ) {
        viewModelScope.launch {
            error.value = null
            when (val res = diaryRepo.archiveDiaryEntry(entryId)) {
                is AppResult.Success -> {
                    if (playbackState.value.path != null) {
                        voiceManager.stopPlayback()
                        playbackState.value = PlaybackStatus()
                    }
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    onDeleted()
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
        }
    }

    override fun onCleared() {
        voiceManager.stopPlayback()
        super.onCleared()
    }

    private data class PlaybackStatus(
        val path: String? = null,
        val positionMillis: Long = 0L,
        val durationMillis: Long = 0L,
    )
}
