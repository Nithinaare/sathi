package com.farmsaathi.feature.farm.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.media.LocalPhotoStorage
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.AreaConverter
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.IrrigationSource
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
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject

data class FarmUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val farms: List<Farm> = emptyList(),
    val plots: List<Plot> = emptyList(),
    val cropCycles: List<CropCycle> = emptyList(),
    val isSaving: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val activeCueKey: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class FarmAndPlotViewModel
@Inject
constructor(
    private val farmRepo: FarmRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val photoStorage: LocalPhotoStorage,
    private val audioEngine: AudioGuidanceEngine,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FarmUiState> =
        combine(
            prefsRepo.userPreferences,
            farmRepo.observeFarms(),
            farmRepo.observePlots(),
            farmRepo.observeCropCycles(),
            saving,
            audioEngine.isSpeaking,
            audioEngine.activeCueKey,
            error,
        ) { arr ->
            @Suppress("UNCHECKED_CAST")
            FarmUiState(
                isLoading = false,
                prefs = arr[0] as UserPreferences,
                farms = arr[1] as List<Farm>,
                plots = arr[2] as List<Plot>,
                cropCycles = arr[3] as List<CropCycle>,
                isSaving = arr[4] as Boolean,
                isAudioPlaying = arr[5] as Boolean,
                activeCueKey = arr[6] as String?,
                errorMessage = arr[7] as String?,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FarmUiState())

    fun toggleAudio(cueKey: String) {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        audioEngine.speakCue(cueKey, useTe)
    }

    fun resolvePhotoFile(path: String): File = photoStorage.resolveFile(path)

    fun createTempCaptureFile(): File = photoStorage.createTempCaptureFile()

    fun saveCapturedPhoto(
        tempFile: File,
        onSaved: (String) -> Unit,
    ) {
        viewModelScope.launch {
            photoStorage.compressAndPersist(tempFile)
                .onSuccess { relPath ->
                    tempFile.delete()
                    onSaved(relPath)
                }
                .onFailure { error.value = "Could not save photo" }
        }
    }

    fun importGalleryPhoto(
        uri: Uri,
        onSaved: (String) -> Unit,
    ) {
        viewModelScope.launch {
            photoStorage.saveCompressedFromUri(uri)
                .onSuccess(onSaved)
                .onFailure { error.value = "Could not import photo" }
        }
    }

    fun parseAreaMicroAcres(
        acresRaw: String,
        subUnitRaw: String,
        unit: AreaUnit,
    ): Long {
        val acres = acresRaw.trim().toIntOrNull() ?: 0
        val sub = subUnitRaw.trim().toDoubleOrNull() ?: 0.0
        return when (unit) {
            AreaUnit.ACRE_GUNTA -> AreaConverter.fromAcresAndGuntas(acres, sub)
            AreaUnit.ACRE_CENT -> AreaConverter.fromAcresAndCents(acres, sub)
            AreaUnit.ACRE_DECIMAL -> {
                val dec = acresRaw.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                AreaConverter.fromDecimalAcres(dec)
            }
        }
    }

    fun saveFarm(
        existing: Farm?,
        name: String,
        village: String,
        surveyNo: String,
        areaMicroAcres: Long,
        areaUnit: AreaUnit,
        waterSource: IrrigationSource?,
        photoPath: String?,
        notes: String,
        onSaved: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val model =
                Farm(
                    id = existing?.id ?: 0L,
                    name = name,
                    villageOrLocation = village.ifBlank { null },
                    surveyNumber = surveyNo.ifBlank { null },
                    totalAreaMicroAcres = areaMicroAcres,
                    preferredAreaUnit = areaUnit,
                    waterSource = waterSource,
                    photoPath = photoPath,
                    notes = notes.ifBlank { null },
                    isPrimary = existing?.isPrimary ?: uiState.value.farms.isEmpty(),
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: 0L,
                )
            when (val res = farmRepo.saveFarm(model)) {
                is AppResult.Success -> {
                    prefsRepo.setActiveFarmId(res.data)
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

    fun archiveFarm(
        farmId: Long,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            farmRepo.archiveFarm(farmId)
            onDone()
        }
    }

    fun savePlot(
        existing: Plot?,
        farmId: Long,
        name: String,
        areaMicroAcres: Long,
        areaUnit: AreaUnit,
        soilType: String,
        waterSource: IrrigationSource?,
        photoPaths: List<String>,
        notes: String,
        onSaved: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val model =
                Plot(
                    id = existing?.id ?: 0L,
                    farmId = farmId,
                    name = name,
                    areaMicroAcres = areaMicroAcres,
                    preferredAreaUnit = areaUnit,
                    soilType = soilType.ifBlank { null },
                    irrigationSource = waterSource,
                    photoPaths = photoPaths,
                    notes = notes.ifBlank { null },
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: 0L,
                )
            when (val res = farmRepo.savePlot(model)) {
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

    fun archivePlot(
        plotId: Long,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            farmRepo.archivePlot(plotId)
            onDone()
        }
    }

    fun clearError() {
        error.value = null
    }

    companion object {
        const val FARMS_CUE = AudioCueCatalog.FARMS_OVERVIEW
        const val PLOTS_CUE = AudioCueCatalog.PLOTS_HELP
    }
}
