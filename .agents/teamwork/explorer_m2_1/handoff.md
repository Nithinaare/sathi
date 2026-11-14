# Milestone 2 Architectural Investigation & Recommendation Report: `TeluguCalendarViewModel` & Domain State Integration

**Target Component:** `TeluguCalendarViewModel.kt` (`:feature:farm`)  
**Investigating Agent:** `explorer_m2_1`  
**Target Milestone:** Milestone 2 (`Farm Calendar, Timeline & Task Scheduling`)  
**Date:** 2026-11-14  

---

## 1. Observation

Direct observations from source inspection across the workspace:

### 1.1 Existing Models (`core/model/src/main/kotlin/com/farmsaathi/core/model/TeluguCalendarModels.kt`)
Lines 6–65 define:
- `TeluguMonth(id: Int, teluguName: String, englishName: String, season: String)`
- `Paksham(teluguName: String, englishName: String)` with `SHUKLA` and `KRISHNA`
- `TeluguTithi(id: Int, teluguName: String, englishName: String)` with values `PADYAMI` (1) through `POURNAMI` (15) and `AMAVASYA` (30)

Lines 68–354 define:
- `data class TeluguKarte(val id: Int, val teluguName: String, val englishName: String, val approxDateSpan: String, val season: String, val proverb: String, val agriculturalSignificance: String, val recommendedCrops: List<String>)`
- `TeluguKarteluCatalog.ALL`: list of all 27 Kartis from `ASHWINI` (1) to `REVATI` (27)
- `TeluguKarteluCatalog.getById(id: Int): TeluguKarte`

Lines 357–408 define:
- `data class TeluguCalendarDay(val epochDay: Long, val gregorianDate: String, val teluguYear: String, val teluguMonth: TeluguMonth, val paksham: Paksham, val tithi: TeluguTithi, val nakshatram: String, val karteId: Int, val karteName: String, val karteDayNumber: Int, val totalKarteDays: Int, val isAmavasya: Boolean, val isPournami: Boolean, val isFestival: Boolean, val festivalName: String? = null, val seasonalAdvisory: String? = null)`
- `data class CurrentKarteInfo(val karteId: Int, val teluguName: String, val englishName: String, val dayNumber: Int, val totalDays: Int, val daysRemaining: Int, val progressPercentage: Float, val proverb: String, val agriculturalSignificance: String, val recommendedCrops: List<String>)`
- `data class LunarDayAlert(val alertType: LunarAlertType, val title: String, val subtitle: String, val ttsSpeechText: String, val actionText: String? = null, val actionRoute: String? = null, val isPriority: Boolean = false)`

### 1.2 Domain Models & Task Scheduling (`core/model/src/main/kotlin/com/farmsaathi/core/model/DomainModels.kt` & `FarmEnums.kt`)
In `DomainModels.kt:131-147`:
```kotlin
data class CropTask(
    val id: Long = 0L,
    val farmId: Long,
    val plotId: Long? = null,
    val cropCycleId: Long? = null,
    val title: String,
    val description: String? = null,
    val category: TaskCategory,
    val dueDateEpochDay: Long,
    val offsetFromSowingDays: Int? = null,
    val sourceTemplateStageKey: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val completedAtEpochMillis: Long? = null,
    val reminderEnabled: Boolean = true,
    val isArchived: Boolean = false,
)
```
In `FarmEnums.kt:60-66`:
```kotlin
enum class TaskStatus {
    PENDING,
    COMPLETED,
    RESCHEDULED,
    MISSED,
    CANCELLED,
}
```
In `DomainError.kt:44-48`:
```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val error: DomainError) : AppResult<Nothing>
}
```

### 1.3 Repository & Infrastructure Contracts (`core/common`)
In `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/TeluguCalendarRepository.kt:9-33`:
```kotlin
interface TeluguCalendarRepository {
    fun observeDay(epochDay: Long): Flow<TeluguCalendarDay?>
    suspend fun getDay(epochDay: Long): TeluguCalendarDay?
    fun observeMonth(year: Int, month: Int): Flow<List<TeluguCalendarDay>>
    suspend fun getMonthDays(year: Int, month: Int): List<TeluguCalendarDay>
    fun observeCurrentKarte(epochDay: Long): Flow<CurrentKarteInfo>
    suspend fun getCurrentKarte(epochDay: Long): CurrentKarteInfo
    fun observeTodayAlert(epochDay: Long): Flow<LunarDayAlert>
    suspend fun getTodayAlert(epochDay: Long): LunarDayAlert
    fun observeAllKartis(): Flow<List<TeluguKarte>>
}
```
In `core/common/src/main/kotlin/com/farmsaathi/core/common/repository/FarmRepository.kt:78-96`:
```kotlin
fun observeTasks(
    cropCycleId: Long? = null,
    plotId: Long? = null,
    farmId: Long? = null,
): Flow<List<CropTask>>

suspend fun updateTaskStatus(
    taskId: Long,
    status: TaskStatus,
    newDueEpochDay: Long? = null,
): AppResult<Unit>
```
In `core/common/src/main/kotlin/com/farmsaathi/core/common/BackupAndAudioContracts.kt:56-96`:
```kotlin
interface AudioGuidanceEngine {
    val activeCueKey: StateFlow<String?>
    val isSpeaking: StateFlow<Boolean>
    fun speakDynamicText(text: String, useTelugu: Boolean)
    fun playAcousticFeedback(cue: AcousticCue)
    fun stop()
}
```
In `core/common/src/main/kotlin/com/farmsaathi/core/common/TimeAndUtils.kt:12-36`:
```kotlin
interface TimeProvider {
    fun todayEpochDay(): Long
    fun nowEpochMillis(): Long
    fun formatDate(epochMillis: Long, useTelugu: Boolean): String
    fun formatEpochDay(epochDay: Long, useTelugu: Boolean): String
}
```
In `core/common/src/main/kotlin/com/farmsaathi/core/common/navigation/FarmRoute.kt:47-57`:
```kotlin
@Serializable data class ActivityForm(
    val taskId: Long? = null,
    val cropCycleId: Long? = null,
    val plotId: Long? = null,
    val initialDueDateEpochDay: Long? = null,
) : FarmRoute

@Serializable data class TeluguCalendar(val initialEpochDay: Long? = null) : FarmRoute
@Serializable data class KarteGuide(val highlightKarteId: Int? = null) : FarmRoute
```

### 1.4 Navigation Form Defect / Touchpoint (`feature/farm/screens/CropsAndCalendarScreens.kt:1152-1171`)
In `ActivityFormScreen`:
```kotlin
fun ActivityFormScreen(
    initialCropCycleId: Long?,
    initialPlotId: Long?,
    viewModel: CropAndCalendarViewModel,
    onBack: () -> Unit,
    taskId: Long? = null,
) {
    ...
    var dueEpochDay by remember(existing, state.todayEpochDay) {
        mutableStateOf(existing?.dueDateEpochDay ?: state.todayEpochDay)
    }
```
`ActivityFormScreen` does not yet expose `initialDueDateEpochDay: Long? = null` parameter, even though `FarmRoute.ActivityForm` defines it. Adding this parameter ensures 1-tap task creation pre-fills the calendar day selected by the farmer.

---

## 2. Logic Chain

1. **Clean Separation of Concerns**:
   - `TeluguCalendarDay` in `core:model` is a pure immutable domain representation of astronomical/lunar day data (masam, tithi, nakshatram, karte).
   - Dynamic user-scheduled tasks (`CropTask`) should NOT pollute `TeluguCalendarDay` directly. Instead, `TeluguCalendarUiState` should hold:
     - `tasksForSelectedDay: List<CropTask>` (all tasks matching `dueDateEpochDay == selectedDay.epochDay`)
     - `taskCountsByDay: Map<Long, Int>` (a map from `epochDay` to the count of uncompleted tasks).
   - This prevents unnecessary schema/model alterations and allows O(1) task dot lookup when rendering calendar cells.

2. **Reactive Flow Architecture**:
   - The user can switch view modes (`CalendarViewMode.MONTH` vs `CalendarViewMode.TIMELINE`), navigate months (`changeMonth(offset)`), filter seasons (`SeasonFilter.ALL`, `KHARIF`, `RABI`, `SUMMER`), and select days (`selectDay(epochDay)`).
   - `displayedYearMonthFlow` triggers `teluguCalendarRepo.observeMonth(year, month)` via `flatMapLatest`.
   - `selectedEpochDayFlow` triggers `teluguCalendarRepo.observeDay(epochDay)` via `flatMapLatest`.
   - `farmRepo.observeTasks()` emits live updates when tasks are added, modified, or completed.
   - We combine these flows using typed state slices (`StateSlice1`, `StateSlice2`, `StateSlice3`) to avoid arbitrary unchecked casts and runtime errors while retaining high performance.

3. **Season Filtering Mathematical Specification**:
   From `PROJECT.md` line 39 and `TeluguKarteluCatalog.ALL`:
   - Kharif / వానాకాలం: Kartis 5..14 (Mrigasira through Chitta) -> 10 Kartis.
   - Rabi / యాసంగి: Kartis 15..25 (Swati through Purvabhadra) -> 11 Kartis.
   - Summer / వేసవి: Kartis 26..27 and 1..4 (Uttarabhadra through Rohini) -> 6 Kartis.
   - Total: 10 + 11 + 6 = 27 Kartis.

4. **1-Tap Inline Task Completion**:
   - When the farmer toggles a task in the Day Details bottom sheet, `viewModel.updateTaskStatus(taskId, newStatus)` executes on `dispatchers.io`.
   - On `AppResult.Success`, `audioEngine.playAcousticFeedback(AcousticCue.SAVED)` gives immediate sensory confirmation.
   - Room automatically re-emits `observeTasks()`, instantly updating the task dot count in the calendar grid and the checkbox in the bottom sheet.

5. **Audio Synthesis Text Generation**:
   - Authentic Telugu strings must avoid generic robotic translations and use rural agronomic terms ("అదను", "ఎడ్లకు విశ్రాంతి", "దీపపు బుట్టలు").
   - Selected Day speech: Constructs date, Tithi, Nakshatram, active Karte day count, Amavasya/Pournami advice, agricultural advisory, and pending task count.
   - Current Karte speech: Constructs Karte name, day count, days remaining, traditional proverb (*సామెత*), farming advisory (*అదను*), and suited crops (*అనుకూల పంటలు*).

---

## 3. Caveats

1. **State Restoration with Navigation**:
   If navigating to `TeluguCalendarScreen` from external routes (such as Home Banner), `initialEpochDay: Long?` can be passed. The ViewModel should provide `initializeWithEpochDay(epochDay: Long?)` or accept it via `SavedStateHandle` so that if `initialEpochDay` is present, it immediately navigates to that month and opens that day's details.
2. **Date Boundaries**:
   `OfflineTeluguCalendarRepositoryImpl` covers 2025–2027 with pre-computed astronomical precision and uses mean synodic fallback outside that window. Navigating months backwards or forwards is safe and will never crash.

---

## 4. Conclusion & Concrete Architectural Design

### 4.1 Recommended Implementation: `TeluguCalendarViewModel.kt`

Path: `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/viewmodel/TeluguCalendarViewModel.kt`

```kotlin
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
    SUMMER("వేసవి", "Summer");

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

    private val _viewModeFlow = MutableStateFlow(CalendarViewMode.MONTH)
    private val _displayedYearMonthFlow = MutableStateFlow(Pair(todayDate.year, todayDate.monthValue))
    private val _selectedEpochDayFlow = MutableStateFlow<Long?>(todayEpoch)
    private val _seasonFilterFlow = MutableStateFlow(SeasonFilter.ALL)
    private val _userMessageFlow = MutableStateFlow<String?>(null)
    private val _isDayDetailsVisibleFlow = MutableStateFlow(false)

    private val _monthDaysFlow = _displayedYearMonthFlow.flatMapLatest { (year, month) ->
        teluguCalendarRepo.observeMonth(year, month)
    }

    private val _selectedDayFlow = _selectedEpochDayFlow.flatMapLatest { epochDay ->
        if (epochDay == null) flowOf(null)
        else teluguCalendarRepo.observeDay(epochDay)
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
            _viewModeFlow,
            _displayedYearMonthFlow,
            _monthDaysFlow,
            _selectedDayFlow,
            farmRepo.observeTasks(),
        ) { viewMode, yearMonth, monthDays, selectedDay, tasks ->
            StateSlice1(viewMode, yearMonth, monthDays, selectedDay, tasks)
        },
        combine(
            teluguCalendarRepo.observeAllKartis(),
            _seasonFilterFlow,
            teluguCalendarRepo.observeCurrentKarte(todayEpoch),
            teluguCalendarRepo.observeTodayAlert(todayEpoch),
            audioEngine.isSpeaking,
        ) { kartis, filter, currentKarte, todayAlert, speaking ->
            StateSlice2(kartis, filter, currentKarte, todayAlert, speaking)
        },
        combine(
            prefsRepo.userPreferences,
            _userMessageFlow,
            _isDayDetailsVisibleFlow,
        ) { prefs, userMsg, detailsVisible ->
            StateSlice3(prefs, userMsg, detailsVisible)
        }
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
            )
        )

    fun selectDay(epochDay: Long) {
        _selectedEpochDayFlow.value = epochDay
        _isDayDetailsVisibleFlow.value = true
        val date = LocalDate.ofEpochDay(epochDay)
        if (_displayedYearMonthFlow.value != (date.year to date.monthValue)) {
            _displayedYearMonthFlow.value = date.year to date.monthValue
        }
    }

    fun dismissDayDetails() {
        _isDayDetailsVisibleFlow.value = false
    }

    fun changeMonth(offset: Int) {
        val (year, month) = _displayedYearMonthFlow.value
        val newDate = LocalDate.of(year, month, 1).plusMonths(offset.toLong())
        _displayedYearMonthFlow.value = Pair(newDate.year, newDate.monthValue)
    }

    fun goToToday() {
        _displayedYearMonthFlow.value = Pair(todayDate.year, todayDate.monthValue)
        selectDay(todayEpoch)
    }

    fun setViewMode(mode: CalendarViewMode) {
        _viewModeFlow.value = mode
    }

    fun filterSeason(season: SeasonFilter) {
        _seasonFilterFlow.value = season
    }

    fun updateTaskStatus(taskId: Long, status: TaskStatus) {
        viewModelScope.launch(dispatchers.io) {
            when (val result = farmRepo.updateTaskStatus(taskId, status)) {
                is AppResult.Success -> {
                    audioEngine.playAcousticFeedback(AcousticCue.SAVED)
                }
                is AppResult.Error -> {
                    _userMessageFlow.value = "పని స్థితి మార్పు విఫలమైంది"
                }
            }
        }
    }

    fun toggleTaskStatus(task: CropTask) {
        val newStatus = if (task.status == TaskStatus.COMPLETED) TaskStatus.PENDING else TaskStatus.COMPLETED
        updateTaskStatus(task.id, newStatus)
    }

    fun clearUserMessage() {
        _userMessageFlow.value = null
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
        val speech = "${karte.teluguName} కార్తె. కాలం: ${karte.approxDateSpan}. సామెత: ${karte.proverb}. వ్యవసాయ సలహా: ${karte.agriculturalSignificance}. అనుకూల పంటలు: ${karte.recommendedCrops.joinToString(", ")}."
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
                sb.append("నేడు అమావాస్య. కాడెడ్లకు విశ్రాంతి ఇవ్వండి. చీకటి రాత్రి పురుగులు గుడ్లు పెట్టకుండా పొలంలో దీపపు బుట్టలు అమర్చండి. ")
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
```

### 4.2 NavGraph & ActivityForm Navigation Pre-fill Recommendation

In `feature/farm/screens/CropsAndCalendarScreens.kt`:
Update `ActivityFormScreen`:
```kotlin
fun ActivityFormScreen(
    initialCropCycleId: Long?,
    initialPlotId: Long?,
    viewModel: CropAndCalendarViewModel,
    onBack: () -> Unit,
    taskId: Long? = null,
    initialDueDateEpochDay: Long? = null, // <- ADDED
) {
    ...
    var dueEpochDay by remember(existing, initialDueDateEpochDay, state.todayEpochDay) {
        mutableStateOf(existing?.dueDateEpochDay ?: initialDueDateEpochDay ?: state.todayEpochDay)
    }
```
And in `app/src/main/kotlin/com/farmsaathi/app/navigation/FarmAndCropsNavGraph.kt`:
```kotlin
composable<FarmRoute.ActivityForm> { backStack ->
    val route: FarmRoute.ActivityForm = backStack.toRoute()
    val vm: CropAndCalendarViewModel = hiltViewModel()
    ActivityFormScreen(
        initialCropCycleId = route.cropCycleId,
        initialPlotId = route.plotId,
        taskId = route.taskId,
        initialDueDateEpochDay = route.initialDueDateEpochDay,
        viewModel = vm,
        onBack = { nav.popBackStack() },
    )
}
```

---

## 5. Verification Method

To independently verify this implementation:

1. **Unit Testing Command**:
   Execute Gradle unit tests for `:feature:farm`:
   ```powershell
   .\gradlew.bat :feature:farm:testDebugUnitTest
   ```

2. **Automated Test Scenarios to Include in `TeluguCalendarViewModelTest.kt`**:
   - `initialState_loadsCurrentMonthAndActiveKarte`: Asserts `monthDays` is populated for the current month, `currentKarte` is non-null, and `allKartis.size == 27`.
   - `changeMonth_advancesAndRetreatsDateWindow`: Asserts `changeMonth(1)` increments month and fetches new days.
   - `selectDay_updatesSelectedDayAndDetailsVisibility`: Asserts `selectDay(epochDay)` sets `isDayDetailsVisible = true` and `selectedDay?.epochDay == epochDay`.
   - `dismissDayDetails_hidesSheet`: Asserts `dismissDayDetails()` sets `isDayDetailsVisible = false`.
   - `taskIntegration_mergesTasksByDueDate`: Mocks `farmRepo.observeTasks()` with tasks for day `20650L`. Asserts `taskCountsByDay[20650L] == 1` and `tasksForSelectedDay` contains the task.
   - `updateTaskStatus_callsRepoAndTriggersAcousticFeedback`: Asserts `farmRepo.updateTaskStatus` is invoked with `TaskStatus.COMPLETED` and `audioEngine.cues.contains(AcousticCue.SAVED)`.
   - `seasonFilter_correctlyFilters27Kartis`: Asserts `SeasonFilter.KHARIF` returns 10 Kartis (5..14), `SeasonFilter.RABI` returns 11 Kartis (15..25), `SeasonFilter.SUMMER` returns 6 Kartis (26..27 & 1..4).
   - `audioSynthesis_generatesAuthenticTeluguSpokenText`: Asserts `buildDayAudioText` produces authentic Telugu strings matching Amavasya or Pournami conditions.

3. **Invalidation Conditions**:
   - If `farmRepo.observeTasks()` changes its return type from `Flow<List<CropTask>>`.
   - If `OfflineTeluguCalendarRepositoryImpl` is relocated from `:core:common`.
