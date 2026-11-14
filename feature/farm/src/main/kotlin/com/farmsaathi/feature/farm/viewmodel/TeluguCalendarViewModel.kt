package com.farmsaathi.feature.farm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AppDispatchers
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.DefaultAppDispatchers
import com.farmsaathi.core.common.TimeProvider
import com.farmsaathi.core.common.repository.FarmRepository
import com.farmsaathi.core.common.repository.OfflineTeluguCalendarRepositoryImpl
import com.farmsaathi.core.common.repository.TeluguCalendarRepository
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AppResult
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CurrentKarteInfo
import com.farmsaathi.core.model.LunarDayAlert
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.core.model.TeluguCalendarDay
import com.farmsaathi.core.model.TeluguKarte
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class CalendarViewMode(val teluguLabel: String, val englishLabel: String) {
    MONTH("నెలల పట్టిక", "Month View"),
    TIMELINE("కార్తెల కాలచక్రం", "Karte Timeline"),
}

enum class SeasonFilter(val teluguLabel: String, val englishLabel: String) {
    ALL("అన్నీ (27)", "All (27)"),
    KHARIF("వానాకాలం / ఖరీఫ్", "Kharif"),
    RABI("యాసంగి / రబీ", "Rabi"),
    SUMMER("వేసవి", "Summer"),
    ;

    fun matches(karteId: Int): Boolean = when (this) {
        ALL -> true
        KHARIF -> karteId in 5..14
        RABI -> karteId in 15..25
        SUMMER -> karteId in 26..27 || karteId in 1..4
    }
}

data class TeluguCalendarUiState(
    val isLoading: Boolean = false,
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val selectedYear: Int = 2026,
    val selectedMonth: Int = 6,
    val todayEpochDay: Long = 0L,
    val monthDays: List<TeluguCalendarDay> = emptyList(),
    val selectedDay: TeluguCalendarDay? = null,
    val tasksForSelectedDay: List<CropTask> = emptyList(),
    val taskCountsByDay: Map<Long, Int> = emptyMap(),
    val allKartis: List<TeluguKarte> = emptyList(),
    val filteredKartis: List<TeluguKarte> = emptyList(),
    val currentKarte: CurrentKarteInfo? = null,
    val todayAlert: LunarDayAlert? = null,
    val isAudioSpeaking: Boolean = false,
    val userMessage: String? = null,
    val activeSeasonFilter: SeasonFilter = SeasonFilter.ALL,
    val isDayDetailsVisible: Boolean = false,
    val isTelugu: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TeluguCalendarViewModel @Inject constructor(
    private val teluguCalendarRepo: TeluguCalendarRepository = OfflineTeluguCalendarRepositoryImpl(),
    private val farmRepo: FarmRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val audioEngine: AudioGuidanceEngine,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers,
) : ViewModel() {

    private val todayEpoch = timeProvider.todayEpochDay()
    private val todayDate = LocalDate.ofEpochDay(todayEpoch)

    private val viewModeFlow = MutableStateFlow(CalendarViewMode.MONTH)
    private val displayedYearMonthFlow = MutableStateFlow(Pair(todayDate.year, todayDate.monthValue))
    private val selectedEpochDayFlow = MutableStateFlow<Long?>(todayEpoch)
    private val seasonFilterFlow = MutableStateFlow(SeasonFilter.ALL)
    private val userMessageFlow = MutableStateFlow<String?>(null)
    private val isDayDetailsVisibleFlow = MutableStateFlow(false)

    private val monthDaysFlow = displayedYearMonthFlow.flatMapLatest { (year, month) ->
        teluguCalendarRepo.observeMonth(year, month)
    }

    private val selectedDayFlow = selectedEpochDayFlow.flatMapLatest { epochDay ->
        if (epochDay == null) {
            flowOf(null)
        } else {
            teluguCalendarRepo.observeDay(epochDay)
        }
    }

    private data class StateSlice1(
        val viewMode: CalendarViewMode,
        val yearMonth: Pair<Int, Int>,
        val monthDays: List<TeluguCalendarDay>,
        val selectedDay: TeluguCalendarDay?,
        val tasks: List<CropTask>,
    )

    private data class StateSlice2(
        val kartis: List<TeluguKarte>,
        val filter: SeasonFilter,
        val currentKarte: CurrentKarteInfo,
        val todayAlert: LunarDayAlert,
        val speaking: Boolean,
    )

    private data class StateSlice3(
        val prefs: UserPreferences,
        val userMsg: String?,
        val detailsVisible: Boolean,
    )

    val uiState: StateFlow<TeluguCalendarUiState> = combine(
        combine(
            viewModeFlow,
            displayedYearMonthFlow,
            monthDaysFlow,
            selectedDayFlow,
            farmRepo.observeTasks(),
        ) { viewMode, yearMonth, monthDays, selectedDay, tasks ->
            StateSlice1(viewMode, yearMonth, monthDays, selectedDay, tasks)
        },
        combine(
            teluguCalendarRepo.observeAllKartis(),
            seasonFilterFlow,
            teluguCalendarRepo.observeCurrentKarte(todayEpoch),
            teluguCalendarRepo.observeTodayAlert(todayEpoch),
            audioEngine.isSpeaking,
        ) { kartis, filter, currentKarte, todayAlert, speaking ->
            StateSlice2(kartis, filter, currentKarte, todayAlert, speaking)
        },
        combine(
            prefsRepo.userPreferences,
            userMessageFlow,
            isDayDetailsVisibleFlow,
        ) { prefs, userMsg, detailsVisible ->
            StateSlice3(prefs, userMsg, detailsVisible)
        },
    ) { slice1, slice2, slice3 ->
        val selectedDay = slice1.selectedDay
        val tasks = slice1.tasks
        val activeTasks = tasks.filter { !it.isArchived && it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED }
        val taskCountsByDay = activeTasks.groupBy { it.dueDateEpochDay }.mapValues { it.value.size }
        val tasksForSelectedDay = if (selectedDay != null) {
            tasks.filter { it.dueDateEpochDay == selectedDay.epochDay && !it.isArchived }
        } else {
            emptyList()
        }
        val filteredKartis = when (slice2.filter) {
            SeasonFilter.ALL -> slice2.kartis
            SeasonFilter.KHARIF -> slice2.kartis.filter { it.id in 5..14 }
            SeasonFilter.RABI -> slice2.kartis.filter { it.id in 15..25 }
            SeasonFilter.SUMMER -> slice2.kartis.filter { it.id in 26..27 || it.id in 1..4 }
        }

        TeluguCalendarUiState(
            isLoading = false,
            viewMode = slice1.viewMode,
            selectedYear = slice1.yearMonth.first,
            selectedMonth = slice1.yearMonth.second,
            todayEpochDay = todayEpoch,
            monthDays = slice1.monthDays,
            selectedDay = selectedDay,
            tasksForSelectedDay = tasksForSelectedDay,
            taskCountsByDay = taskCountsByDay,
            allKartis = slice2.kartis,
            filteredKartis = filteredKartis,
            currentKarte = slice2.currentKarte,
            todayAlert = slice2.todayAlert,
            isAudioSpeaking = slice2.speaking,
            userMessage = slice3.userMsg,
            activeSeasonFilter = slice2.filter,
            isDayDetailsVisible = slice3.detailsVisible,
            isTelugu = slice3.prefs.language == AppLanguage.TELUGU,
        )
    }.flowOn(dispatchers.default)
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TeluguCalendarUiState(
                isLoading = true,
                selectedYear = todayDate.year,
                selectedMonth = todayDate.monthValue,
                todayEpochDay = todayEpoch,
            ),
        )

    fun selectDay(epochDay: Long) {
        selectedEpochDayFlow.value = epochDay
        isDayDetailsVisibleFlow.value = true
        val date = LocalDate.ofEpochDay(epochDay)
        if (displayedYearMonthFlow.value != (date.year to date.monthValue)) {
            displayedYearMonthFlow.value = date.year to date.monthValue
        }
    }

    fun dismissDayDetails() {
        isDayDetailsVisibleFlow.value = false
    }

    fun changeMonth(offset: Int) {
        val (year, month) = displayedYearMonthFlow.value
        val newDate = LocalDate.of(year, month, 1).plusMonths(offset.toLong())
        displayedYearMonthFlow.value = Pair(newDate.year, newDate.monthValue)
    }

    fun goToToday() {
        displayedYearMonthFlow.value = Pair(todayDate.year, todayDate.monthValue)
        selectDay(todayEpoch)
    }

    fun setViewMode(mode: CalendarViewMode) {
        viewModeFlow.value = mode
    }

    fun filterSeason(season: SeasonFilter) {
        seasonFilterFlow.value = season
    }

    fun updateTaskStatus(taskId: Long, status: TaskStatus) {
        viewModelScope.launch(dispatchers.io) {
            when (farmRepo.updateTaskStatus(taskId, status)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                }
                is AppResult.Error -> {
                    userMessageFlow.value = "పని స్థితి మార్పు విఫలమైంది"
                }
            }
        }
    }

    fun toggleTaskStatus(task: CropTask) {
        val newStatus = if (task.status == TaskStatus.COMPLETED) TaskStatus.PENDING else TaskStatus.COMPLETED
        updateTaskStatus(task.id, newStatus)
    }

    fun clearUserMessage() {
        userMessageFlow.value = null
    }

    fun togglePanchangAudio() {
        if (audioEngine.isSpeaking.value) {
            audioEngine.stop()
            return
        }
        val state = uiState.value
        val speechText = when {
            state.isDayDetailsVisible && state.selectedDay != null -> {
                buildDayAudioText(state.selectedDay, state.tasksForSelectedDay.size)
            }
            state.viewMode == CalendarViewMode.TIMELINE && state.currentKarte != null -> {
                buildCurrentKarteAudioText(state.currentKarte)
            }
            state.todayAlert != null && state.todayAlert.ttsSpeechText.isNotBlank() -> {
                state.todayAlert.ttsSpeechText
            }
            state.currentKarte != null -> {
                buildCurrentKarteAudioText(state.currentKarte)
            }
            else -> null
        }
        if (!speechText.isNullOrBlank()) {
            audioEngine.speakDynamicText(speechText, useTelugu = true)
        }
    }

    fun speakKarte(karte: TeluguKarte) {
        if (audioEngine.isSpeaking.value) {
            audioEngine.stop()
            return
        }
        val speech = "${karte.teluguName} కార్తె. కాలం: ${karte.approxDateSpan}. " +
            "సామెత: ${karte.proverb}. వ్యవసాయ సలహా: ${karte.agriculturalSignificance}. " +
            "అనుకూల పంటలు: ${karte.recommendedCrops.joinToString(", ")}."
        audioEngine.speakDynamicText(speech, useTelugu = true)
    }

    fun initializeWithEpochDay(epochDay: Long?) {
        if (epochDay != null) {
            selectDay(epochDay)
        }
    }

    override fun onCleared() {
        if (audioEngine.isSpeaking.value) {
            audioEngine.stop()
        }
        super.onCleared()
    }

    companion object {
        fun buildDayAudioText(day: TeluguCalendarDay, tasksCount: Int = 0): String {
            val sb = StringBuilder()
            sb.append("${day.teluguMonth.teluguName} ")
            sb.append("${day.paksham.teluguName} ")
            sb.append("${day.tithi.teluguName}. ")
            sb.append("${day.nakshatram} నక్షత్రం. ")
            sb.append("${day.karteName} కార్తె ${day.karteDayNumber}వ రోజు. ")

            if (day.isAmavasya) {
                if (day.festivalName != null) {
                    sb.append("నేడు ${day.festivalName}. నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి ఇవ్వండి. ")
                    sb.append("చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
                } else {
                    sb.append("నేడు అమావాస్య. ఎడ్లకు విశ్రాంతి ఇవ్వండి. ")
                    sb.append("చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
                }
            } else if (day.isPournami) {
                if (day.festivalName != null) {
                    sb.append("నేడు పౌర్ణమి, ${day.festivalName}. ")
                } else {
                    sb.append("నేడు పౌర్ణమి. ")
                }
            } else if (day.isFestival && day.festivalName != null) {
                sb.append("నేడు ${day.festivalName}. ")
            }

            if (!day.seasonalAdvisory.isNullOrBlank()) {
                sb.append("సలహా: ${day.seasonalAdvisory}. ")
            }

            if (tasksCount > 0) {
                sb.append("ఈరోజు మీకు $tasksCount వ్యవసాయ పనులు ఉన్నాయి.")
            }

            return sb.toString().trim()
        }

        fun buildCurrentKarteAudioText(karteInfo: CurrentKarteInfo): String {
            val sb = StringBuilder()
            sb.append("ప్రస్తుతం ${karteInfo.teluguName} కార్తె నడుస్తున్నది. ")
            sb.append("ఇది ${karteInfo.dayNumber}వ రోజు, మొత్తం ${karteInfo.totalDays} రోజులు. ")
            sb.append("ఇంకా ${karteInfo.daysRemaining} రోజులు మిగిలాయి. ")
            sb.append("సామెత: ${karteInfo.proverb}. ")
            sb.append("వ్యవసాయ సలహా: ${karteInfo.agriculturalSignificance}. ")
            if (karteInfo.recommendedCrops.isNotEmpty()) {
                sb.append("అనుకూల పంటలు: ${karteInfo.recommendedCrops.joinToString(", ")}.")
            }
            return sb.toString().trim()
        }
    }
}
