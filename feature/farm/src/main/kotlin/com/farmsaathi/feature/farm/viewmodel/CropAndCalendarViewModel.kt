package com.farmsaathi.feature.farm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CropTemplate
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.Plot
import com.farmsaathi.core.model.SeasonType
import com.farmsaathi.core.model.TaskCategory
import com.farmsaathi.core.model.TaskPriority
import com.farmsaathi.core.model.TaskStatus
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

data class CropCalendarUiState(
    val isLoading: Boolean = true,
    val prefs: UserPreferences = UserPreferences(),
    val farms: List<Farm> = emptyList(),
    val plots: List<Plot> = emptyList(),
    val templates: List<CropTemplate> = emptyList(),
    val cycles: List<CropCycle> = emptyList(),
    val tasks: List<CropTask> = emptyList(),
    val todayEpochDay: Long = 0L,
    val isSaving: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val activeCueKey: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class CropAndCalendarViewModel
@Inject
constructor(
    private val farmRepo: FarmRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CropCalendarUiState> =
        combine(
            prefsRepo.userPreferences,
            farmRepo.observeFarms(),
            farmRepo.observePlots(),
            farmRepo.observeCropTemplates(),
            farmRepo.observeCropCycles(),
            farmRepo.observeTasks(),
            saving,
            audioEngine.isSpeaking,
            audioEngine.activeCueKey,
            error,
        ) { arr ->
            @Suppress("UNCHECKED_CAST")
            CropCalendarUiState(
                isLoading = false,
                prefs = arr[0] as UserPreferences,
                farms = arr[1] as List<Farm>,
                plots = arr[2] as List<Plot>,
                templates = arr[3] as List<CropTemplate>,
                cycles = arr[4] as List<CropCycle>,
                tasks = arr[5] as List<CropTask>,
                todayEpochDay = timeProvider.todayEpochDay(),
                isSaving = arr[6] as Boolean,
                isAudioPlaying = arr[7] as Boolean,
                activeCueKey = arr[8] as String?,
                errorMessage = arr[9] as String?,
            )
        }.flowOn(dispatchers.default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CropCalendarUiState())

    fun toggleAudio(cueKey: String) {
        val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
        audioEngine.speakCue(cueKey, useTe)
    }

    fun formatEpochDay(epochDay: Long): String =
        timeProvider.formatEpochDay(
            epochDay,
            uiState.value.prefs.language == AppLanguage.TELUGU,
        )

    fun epochMillisToEpochDay(millis: Long): Long = timeProvider.epochMillisToEpochDay(millis)

    fun epochDayToMillis(day: Long): Long = timeProvider.epochDayToStartMillis(day)

    fun saveCropCycle(
        existing: CropCycle?,
        plot: Plot?,
        templateId: Long?,
        cropName: String,
        variety: String,
        season: SeasonType,
        sowingEpochDay: Long,
        expectedHarvestEpochDay: Long?,
        generateTasksFromTemplate: Boolean,
        recalculateTasksOnEdit: Boolean,
        notes: String,
        farmIdHint: Long? = null,
        onSaved: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
            val (resolvedFarmId, resolvedPlotId, resolvedArea) = resolveFarmAndPlotForCrop(plot, farmIdHint, useTe)
            if (existing == null) {
                val model =
                    CropCycle(
                        farmId = resolvedFarmId,
                        plotId = resolvedPlotId,
                        templateId = templateId,
                        cropName = cropName,
                        variety = variety.ifBlank { null },
                        season = season,
                        cropYear = java.time.LocalDate.ofEpochDay(sowingEpochDay).year,
                        plantedAreaMicroAcres = resolvedArea,
                        sowingDateEpochDay = sowingEpochDay,
                        expectedHarvestEpochDay = expectedHarvestEpochDay,
                        notes = notes.ifBlank { null },
                    )
                when (val res = farmRepo.createCropCycleWithTemplate(model, generateTasksFromTemplate, useTe)) {
                    is AppResult.Success -> {
                        audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                        onSaved(res.data)
                    }
                    is AppResult.Error -> {
                        error.value = res.error.messageKey
                        audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                    }
                }
            } else {
                val updated =
                    existing.copy(
                        plotId = resolvedPlotId,
                        farmId = resolvedFarmId,
                        templateId = templateId ?: existing.templateId,
                        cropName = cropName,
                        variety = variety.ifBlank { null },
                        season = season,
                        sowingDateEpochDay = sowingEpochDay,
                        expectedHarvestEpochDay = expectedHarvestEpochDay,
                        notes = notes.ifBlank { null },
                    )
                farmRepo.updateCropCycle(updated, recalculateTasksOnEdit)
                audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                onSaved(existing.id)
            }
            saving.value = false
        }
    }

    private suspend fun resolveFarmAndPlotForCrop(
        plot: Plot?,
        farmIdHint: Long?,
        useTe: Boolean,
    ): Triple<Long, Long, Long> {
        if (plot != null) return Triple(plot.farmId, plot.id, plot.areaMicroAcres)
        if (farmIdHint != null) {
            val farm = uiState.value.farms.firstOrNull { it.id == farmIdHint }
            val existingPlot = uiState.value.plots.firstOrNull { it.farmId == farmIdHint }
            if (existingPlot != null) {
                return Triple(existingPlot.farmId, existingPlot.id, existingPlot.areaMicroAcres)
            }
            if (farm != null) {
                val defaultArea = farm.totalAreaMicroAcres.takeIf { it > 0L } ?: 1_000_000L
                val created =
                    farmRepo.savePlot(
                        Plot(
                            farmId = farm.id,
                            name = if (useTe) "${farm.name} మడి 1" else "${farm.name} Plot 1",
                            areaMicroAcres = defaultArea,
                            preferredAreaUnit = farm.preferredAreaUnit,
                        ),
                    )
                if (created is AppResult.Success) {
                    return Triple(farm.id, created.data, defaultArea)
                }
            }
        }
        val (fId, pId) = farmRepo.ensureDefaultFarmAndPlot()
        return Triple(fId, pId, 1_000_000L)
    }

    fun updateCycleStatus(
        cycleId: Long,
        status: CropCycleStatus,
    ) {
        viewModelScope.launch {
            farmRepo.updateCropCycleStatus(cycleId, status)
            audioEngine.playAcousticFeedback(AcousticCue.SAVED)
        }
    }

    fun saveTask(
        existing: CropTask?,
        farmId: Long,
        plotId: Long?,
        cropCycleId: Long?,
        title: String,
        description: String,
        category: TaskCategory,
        dueEpochDay: Long,
        priority: TaskPriority,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val model =
                CropTask(
                    id = existing?.id ?: 0L,
                    farmId = farmId,
                    plotId = plotId,
                    cropCycleId = cropCycleId,
                    title = title,
                    description = description.ifBlank { null },
                    category = category,
                    dueDateEpochDay = dueEpochDay,
                    offsetFromSowingDays = existing?.offsetFromSowingDays,
                    sourceTemplateStageKey = existing?.sourceTemplateStageKey,
                    priority = priority,
                    status = existing?.status ?: TaskStatus.PENDING,
                )
            when (val res = farmRepo.saveTask(model)) {
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

    fun updateTaskStatus(
        taskId: Long,
        status: TaskStatus,
        newDueEpochDay: Long? = null,
    ) {
        viewModelScope.launch {
            farmRepo.updateTaskStatus(taskId, status, newDueEpochDay)
            audioEngine.playAcousticFeedback(AcousticCue.SAVED)
        }
    }

    fun saveTemplate(
        template: CropTemplate,
        onSaved: () -> Unit,
    ) {
        upsertCropTemplate(template) { onSaved() }
    }

    fun upsertCropTemplate(
        template: CropTemplate,
        onSaved: (Long) -> Unit = {},
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            when (val res = farmRepo.upsertCropTemplate(template)) {
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

    fun resetDefaultTemplates(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            when (val res = farmRepo.resetDefaultTemplates()) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    onDone()
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
            saving.value = false
        }
    }

    fun applyTemplateToCrop(
        cropCycleId: Long,
        templateId: Long,
        onApplied: (Int) -> Unit = {},
    ) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            val useTe = uiState.value.prefs.language == AppLanguage.TELUGU
            when (val res = farmRepo.applyTemplateToCrop(cropCycleId, templateId, useTe)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    onApplied(res.data)
                }
                is AppResult.Error -> {
                    error.value = res.error.messageKey
                    audioEngine.playAcousticFeedback(AcousticCue.ERROR)
                }
            }
            saving.value = false
        }
    }

    fun playErrorCue() {
        audioEngine.playAcousticFeedback(AcousticCue.ERROR)
    }

    fun clearError() {
        error.value = null
    }

    fun createQuickPlot(
        farm: Farm,
        useTe: Boolean,
        onCreated: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            val plot =
                Plot(
                    farmId = farm.id,
                    name = if (useTe) "${farm.name} మడి 1" else "${farm.name} Plot 1",
                    areaMicroAcres = farm.totalAreaMicroAcres.takeIf { it > 0L } ?: 1_000_000L,
                    preferredAreaUnit = farm.preferredAreaUnit,
                )
            when (val res = farmRepo.savePlot(plot)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                    onCreated(res.data)
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
