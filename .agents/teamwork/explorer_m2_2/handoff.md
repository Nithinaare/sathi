# Milestone 2 UI Screens & ActivityForm Investigation Report: `TeluguCalendarScreen`, `KarteGuideScreen` & ActivityForm Wiring

**Target Components:**
1. `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/TeluguCalendarScreen.kt` (New)
2. `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/KarteGuideScreen.kt` (New)
3. `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/CropsAndCalendarScreens.kt` (Update `ActivityFormScreen`)
**Investigating Agent:** `explorer_m2_2`  
**Target Milestone:** Milestone 2 (`Farm Calendar, Timeline & Task Scheduling`)  
**Date:** 2026-11-14  

---

## 1. Observation

Direct source observations verified in the workspace:

### 1.1 `ActivityFormScreen` Due Date Wiring Gap
In `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/CropsAndCalendarScreens.kt:1152-1171`:
```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityFormScreen(
    initialCropCycleId: Long?,
    initialPlotId: Long?,
    viewModel: CropAndCalendarViewModel,
    onBack: () -> Unit,
    taskId: Long? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = remember(taskId, state.tasks) { taskId?.let { id -> state.tasks.firstOrNull { it.id == id } } }
    ...
    var dueEpochDay by remember(existing, state.todayEpochDay) {
        mutableStateOf(existing?.dueDateEpochDay ?: state.todayEpochDay)
    }
```
In `core/common/src/main/kotlin/com/farmsaathi/core/common/navigation/FarmRoute.kt:47-52`:
```kotlin
    @Serializable data class ActivityForm(
        val taskId: Long? = null,
        val cropCycleId: Long? = null,
        val plotId: Long? = null,
        val initialDueDateEpochDay: Long? = null,
    ) : FarmRoute
```
And in `app/src/main/kotlin/com/farmsaathi/app/navigation/FarmAndCropsNavGraph.kt:160-170`:
```kotlin
    composable<FarmRoute.ActivityForm> { backStack ->
        val route: FarmRoute.ActivityForm = backStack.toRoute()
        val vm: CropAndCalendarViewModel = hiltViewModel()
        ActivityFormScreen(
            initialCropCycleId = route.cropCycleId,
            initialPlotId = route.plotId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            taskId = route.taskId,
        )
    }
```
**Finding:** `FarmRoute.ActivityForm` already contains `initialDueDateEpochDay: Long? = null`, but `ActivityFormScreen` does not yet accept this parameter, defaulting instead to `state.todayEpochDay`. Adding `initialDueDateEpochDay: Long? = null` to `ActivityFormScreen` and initializing `dueEpochDay` with `existing?.dueDateEpochDay ?: initialDueDateEpochDay ?: state.todayEpochDay` seamlessly connects the 1-tap task creation flow from both the Calendar Month Grid and Karte Timeline.

### 1.2 Banner Styling & Reusable Patterns
In `feature/home/src/main/kotlin/com/farmsaathi/feature/home/components/HomePanchangKarteBanner.kt`:
- Uses `Surface` with `FarmShapeTokens.card` and `BorderStroke(1.dp, cardBorderColor)`.
- Lunar discs: `Box` with `CircleShape`, sized `16.dp`, colored `FarmColors.MainText` for Amavasya and `FarmColors.HarvestAmber` for Pournami.
- Audio buttons: 1-tap "వినండి" / "ఆపండి" with `Icons.AutoMirrored.Rounded.VolumeUp` and `Icons.Rounded.Stop`.
- Interactive cards use `clickable(role = Role.Button, onClick = ...)`.

### 1.3 Design System Tokens & Components
- **Colors** (`core/designsystem/src/main/kotlin/com/farmsaathi/core/designsystem/theme/Color.kt`):
  - `FarmColors.PrimaryForest` (0xFF245A3A), `PrimaryDark` (0xFF18432A), `PrimaryContainer` (0xFFE2F0E5).
  - `FarmColors.SecondaryLeaf` (0xFF637A43), `SecondaryContainer` (0xFFE9F0DD).
  - `FarmColors.HarvestAmber` (0xFFB7791F), `AmberContainer` (0xFFF7EBD3).
  - `FarmColors.Terracotta` (0xFFB65D42), `TerracottaContainer` (0xFFF9E5DF).
  - `FarmColors.MainText` (0xFF1E2B22), `SecondaryText` (0xFF47574B), `Outline` (0xFFC3CEC2), `Surface` (0xFFFFFFFF), `Background` (0xFFF7F8F2).
  - `FarmColors.Warning` (0xFF9A6200), `WarningContainer` (0xFFFDF0D5), `Success` (0xFF2F6F44), `SuccessContainer` (0xFFE1F2E6).
- **Shapes & Spacing** (`core/designsystem/src/main/kotlin/com/farmsaathi/core/designsystem/theme/DimensionsAndShapes.kt`):
  - `FarmSpacing.dp4`, `dp8`, `dp12`, `dp16`, `dp24`, `dp32`.
  - `FarmShapeTokens.card` (`RoundedCornerShape(16.dp)`), `tag` (`8.dp`), `inputField` (`12.dp`), `circular` (`CircleShape`).
- **Typography** (`core/designsystem/src/main/kotlin/com/farmsaathi/core/designsystem/theme/Type.kt`):
  - `FarmTypography.headlineMedium`, `titleLarge`, `titleMedium`, `bodyLarge`, `bodyMedium`, `bodySmall`, `labelLarge`, `labelMedium`.
- **Reusable Components** (`core/designsystem/components/`):
  - `FarmPageHeader`, `FarmPrimaryButton`, `FarmSecondaryButton`, `FarmIconButton`, `FarmSearchField`, `FarmEmptyState`, `FarmLoadingState`.
- **Strings** (`core/designsystem/src/main/res/values/strings.xml` and `values-en/strings.xml` lines 162–178):
  - `cal_telugu_title` ("తెలుగు వ్యవసాయ పంచాంగం" / "Telugu Farming Calendar")
  - `cal_month_view_tab` ("నెలల పట్టిక" / "Month View")
  - `cal_karte_view_tab` ("కార్తెల కాలచక్రం" / "Karte Wheel")
  - `cal_amavasya_badge` ("అమావాస్య" / "Amavasya")
  - `cal_pournami_badge` ("పౌర్ణమి" / "Pournami")
  - `cal_day_details_title` ("పంచాంగం & పొలం పనులు" / "Panchang & Farm Tasks")
  - `cal_add_task_for_day` ("+ ఈ రోజుకు పని జోడించండి" / "+ Add Task for this Day")
  - `cal_no_tasks_for_day` ("ఈ రోజుకు షెడ్యూల్ చేసిన పనులేవీ లేవు" / "No farm tasks scheduled for this day")
  - `cal_karte_guide_title` ("27 కార్తెలు & సాగు విజ్ఞానం" / "27 Kartis & Farm Wisdom")
  - `cal_days_remaining` ("%1$d రోజులు మిగిలి ఉన్నాయి" / "%1$d days remaining")
  - `cal_suited_crops` ("ఈ కార్తెకు అనువైన పంటలు" / "Suited Crops for this Karte")
  - `cal_proverb_label` ("కార్తె సామెత" / "Farming Saying")
  - `cal_advisory_label` ("సాగు సూచన (అదను)" / "Agricultural Advisory (Adanu)")

### 1.4 State Contract with `TeluguCalendarViewModel` (`explorer_m2_1/handoff.md`)
The ViewModel state `TeluguCalendarUiState` exposes:
```kotlin
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
```
ViewModel methods:
- `selectDay(epochDay: Long)`
- `dismissDayDetails()`
- `changeMonth(offset: Int)`
- `goToToday()`
- `setViewMode(mode: CalendarViewMode)`
- `filterSeason(season: SeasonFilter)`
- `updateTaskStatus(taskId: Long, status: TaskStatus)`
- `toggleTaskStatus(task: CropTask)`
- `togglePanchangAudio()`
- `speakKarte(karte: TeluguKarte)`
- `initializeWithEpochDay(epochDay: Long?)`
- `clearUserMessage()`

---

## 2. Logic Chain

1. **Dual-Mode Screen Layout Architecture (`TeluguCalendarScreen.kt`)**:
   - A single top-level screen hosting a 2-segment pill toggle (`CalendarViewMode.MONTH` vs `CalendarViewMode.TIMELINE`).
   - TopBar includes back navigation, month navigation controls (`<` and `>`), month name with Telugu masa title (e.g., "జూన్ 2026 (ఆషాఢ మాసం)"), and 1-tap audio guidance ("వినండి" / "ఆపండి").
   - A quick-action chip navigates to the dedicated `KarteGuideScreen`.

2. **Month Grid Structure & Day-of-Week Alignment**:
   - Header row contains 7 columns: `ఆది, సోమ, మంగళ, బుధ, గురు, శుక్ర, శని` (Sunday through Saturday).
   - Sunday column styled with `FarmColors.Terracotta` for visual orientation.
   - For any given month, `firstDay = monthDays.firstOrNull()`.
   - In Java Time, `LocalDate.ofEpochDay(firstDay.epochDay).dayOfWeek.value % 7` yields the number of leading empty spacer cells (Sunday = `7 % 7 = 0`, Monday = `1`, etc.).
   - Grid layout is rendered in rows of 7 cells, avoiding nested scroll conflicts within `LazyColumn`.

3. **Rich Agrarian Day Cell (`MonthDayCell`)**:
   - Cell height: 60.dp minimum for comfortable finger tapping on rural touchscreens (Realme 5s).
   - Cell visual hierarchy:
     - Top row: Gregorian day number (bold) + Lunar Disc badge:
       - Amavasya (`isAmavasya`): Filled slate-dark circle `●` (`FarmColors.MainText`).
       - Pournami (`isPournami`): Radiant amber disk `○` (`FarmColors.HarvestAmber`).
     - Middle row: Telugu Tithi name (e.g., "పాడ్యమి", "విదియ", "అమావాస్య", "పౌర్ణమి").
     - Bottom row: Task count badge (`taskCountsByDay[day.epochDay]`). If count > 0, an amber pill showing the uncompleted task count appears.
   - State highlights:
     - If `day.epochDay == todayEpochDay`: container bordered with `FarmColors.PrimaryForest` (1.5.dp).
     - If `day.epochDay == selectedDay?.epochDay`: container background `FarmColors.PrimaryContainer` with `FarmColors.PrimaryDark` border.

4. **Day Details Bottom Sheet (`DayDetailsBottomSheet`)**:
   - Triggered when `isDayDetailsVisible && selectedDay != null`.
   - Uses Material 3 `@OptIn(ExperimentalMaterial3Api::class) ModalBottomSheet` with `sheetState = rememberModalBottomSheetState()`.
   - Header: Full Telugu date, year name, Paksham, Tithi, Nakshatram, and active Karte day.
   - Dynamic Alerts:
     - If Amavasya: displays high-priority warning card for cattle rest (*ఎడ్లకు విశ్రాంతి*) and nocturnal light trap deployment (*దీపపు బుట్టలు*).
     - If Pournami: displays auspicious festival card.
   - Agricultural Advisory Card (*అదను*):
     - Displays `selectedDay.seasonalAdvisory` with clean rustic border.
   - Scheduled Farm Tasks Section:
     - Displays real `CropTask` records matching `dueDateEpochDay == selectedDay.epochDay`.
     - Each item contains an inline `Checkbox` to toggle `COMPLETED` / `PENDING` with immediate audio feedback.
   - 1-Tap Task Creation Button (`+ ఈ రోజుకు పని జోడించండి`):
     - Invokes `onAddTask(selectedDay.epochDay)`.

5. **Karte Timeline View (`కార్తెల కాలచక్రం`)**:
   - Active Karte Hero Card:
     - Progress bar showing `%` completed of the 13–14 day solar transit.
     - Days remaining countdown pill.
     - Proverb box (*సామెత*).
     - Farming advisory (*అదను*).
     - Suited crop tags (*అనుకూల పంటలు*).
     - 1-tap Telugu audio button speaking the wisdom.
     - 1-tap `+ ఈ కార్తెలో పని కలపండి` adding task to today.
   - Seasonal Filter Chips:
     - `అన్నీ (27)`, `వానాకాలం / ఖరీఫ్` (5..14), `యాసంగి / రబీ` (15..25), `వేసవి` (26..27, 1..4).
   - 27 Kartis List:
     - Displayed with status badges: `● ప్రస్తుత కార్తె`, `ముగిసింది ✓`, `రాబోయే కార్తె ⏳`.

6. **Dedicated `KarteGuideScreen` Catalog**:
   - `FarmSearchField` for quick filtering across Telugu names, English names, suited crops, or proverb text.
   - Season filter chips.
   - Expandable cards with complete proverbs, agricultural significance (*అదను*), suited crops, and 1-tap audio speech synthesis for each individual Karte.

7. **1-Tap Task Creation Wiring in `ActivityFormScreen`**:
   - `initialDueDateEpochDay: Long? = null` added to `ActivityFormScreen` parameters.
   - State `dueEpochDay` initialized as `existing?.dueDateEpochDay ?: initialDueDateEpochDay ?: state.todayEpochDay`.
   - This completes the round-trip: Tapping "+ పని జోడించండి" in Calendar opens Activity Form with that date pre-selected; saving returns to Calendar where the task dot appears immediately.

---

## 3. Caveats

1. **Material 3 ModalBottomSheet State**:
   - `rememberModalBottomSheetState(skipPartiallyExpanded = true)` ensures the sheet expands fully for the day details on small-screen phones.
   - When user swipes down or taps outside, `onDismissRequest` must invoke `viewModel.dismissDayDetails()`.
2. **Offline Font Shaping**:
   - All Telugu text uses system font `NotoSansTelugu` via `FarmFontFamily = FontFamily.SansSerif`. No external font network download is needed.
3. **Month Boundaries**:
   - Month switching handles Gregorian month/year math via `java.time.LocalDate.plusMonths()`, while `OfflineTeluguCalendarRepositoryImpl` provides full 2025–2027 Chandramana ephemeris data without crashing.

---

## 4. Conclusion & Concrete Implementation Code

### 4.1 Implementation: `TeluguCalendarScreen.kt`

Path: `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/TeluguCalendarScreen.kt`

```kotlin
package com.farmsaathi.feature.farm.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmIconButton
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CurrentKarteInfo
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.core.model.TeluguCalendarDay
import com.farmsaathi.core.model.TeluguKarte
import com.farmsaathi.feature.farm.viewmodel.CalendarViewMode
import com.farmsaathi.feature.farm.viewmodel.SeasonFilter
import com.farmsaathi.feature.farm.viewmodel.TeluguCalendarUiState
import com.farmsaathi.feature.farm.viewmodel.TeluguCalendarViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeluguCalendarScreen(
    viewModel: TeluguCalendarViewModel,
    initialEpochDay: Long? = null,
    onBack: () -> Unit,
    onOpenKarteGuide: (Int?) -> Unit,
    onAddTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(initialEpochDay) {
        if (initialEpochDay != null) {
            viewModel.initializeWithEpochDay(initialEpochDay)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FarmColors.Background,
        topBar = {
            TeluguCalendarTopBar(
                state = state,
                onBack = onBack,
                onPreviousMonth = { viewModel.changeMonth(-1) },
                onNextMonth = { viewModel.changeMonth(1) },
                onGoToToday = viewModel::goToToday,
                onToggleAudio = viewModel::togglePanchangAudio,
                onOpenKarteGuide = { onOpenKarteGuide(null) },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            FarmLoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Segmented View Toggle: [నెలల పట్టిక (Month Grid)] | [కార్తెల కాలచక్రం (Karte Timeline)]
            ViewModeSegmentedToggle(
                currentMode = state.viewMode,
                onModeSelected = viewModel::setViewMode,
                isTelugu = state.isTelugu,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp8),
            )

            when (state.viewMode) {
                CalendarViewMode.MONTH -> {
                    MonthGridView(
                        state = state,
                        onSelectDay = viewModel::selectDay,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = FarmSpacing.dp12),
                    )
                }
                CalendarViewMode.TIMELINE -> {
                    KarteTimelineView(
                        state = state,
                        onFilterSeason = viewModel::filterSeason,
                        onSpeakKarte = viewModel::speakKarte,
                        onOpenGuide = onOpenKarteGuide,
                        onAddTask = onAddTask,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Day Details Bottom Sheet
        if (state.isDayDetailsVisible && state.selectedDay != null) {
            ModalBottomSheet(
                onDismissRequest = viewModel::dismissDayDetails,
                sheetState = sheetState,
                containerColor = FarmColors.Surface,
                dragHandle = { BottomSheetDefaults.DragHandle() },
            ) {
                DayDetailsBottomSheetContent(
                    day = state.selectedDay!!,
                    tasks = state.tasksForSelectedDay,
                    isAudioSpeaking = state.isAudioSpeaking,
                    onToggleAudio = viewModel::togglePanchangAudio,
                    onToggleTask = viewModel::toggleTaskStatus,
                    onAddTask = { epochDay ->
                        viewModel.dismissDayDetails()
                        onAddTask(epochDay)
                    },
                    onClose = viewModel::dismissDayDetails,
                )
            }
        }
    }
}

@Composable
private fun TeluguCalendarTopBar(
    state: TeluguCalendarUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
    onToggleAudio: () -> Unit,
    onOpenKarteGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthTitle = remember(state.selectedYear, state.selectedMonth, state.monthDays, state.isTelugu) {
        val primaryMasa = state.monthDays.firstOrNull()?.teluguMonth?.teluguName
        val gregorianMonthName = formatGregorianMonthName(state.selectedMonth, state.isTelugu)
        if (primaryMasa != null) {
            "$gregorianMonthName ${state.selectedYear} ($primaryMasa మాసం)"
        } else {
            "$gregorianMonthName ${state.selectedYear}"
        }
    }

    Surface(
        color = FarmColors.Surface,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, FarmColors.Outline.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp8)) {
            // Row 1: Back, Title, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FarmIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = farmString(R.string.common_back),
                        onClick = onBack,
                        containerColor = FarmColors.SurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(FarmSpacing.dp8))
                    Column {
                        Text(
                            text = farmString(R.string.cal_telugu_title),
                            style = FarmTypography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = FarmColors.MainText,
                        )
                        Text(
                            text = monthTitle,
                            style = FarmTypography.bodySmall,
                            color = FarmColors.PrimaryForest,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp4),
                ) {
                    // Quick Guide button
                    FarmIconButton(
                        icon = Icons.Rounded.MenuBook,
                        contentDescription = farmString(R.string.cal_karte_guide_title),
                        onClick = onOpenKarteGuide,
                        containerColor = FarmColors.PrimaryContainer,
                        tint = FarmColors.PrimaryForest,
                    )

                    // Audio Readout button
                    FarmIconButton(
                        icon = if (state.isAudioSpeaking) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = farmString(R.string.home_panchang_listen),
                        onClick = onToggleAudio,
                        containerColor = if (state.isAudioSpeaking) FarmColors.Warning else FarmColors.PrimaryContainer,
                        tint = if (state.isAudioSpeaking) FarmColors.MainText else FarmColors.PrimaryForest,
                    )
                }
            }

            // Row 2: Month Navigation controls (Only visible in Month Grid mode)
            if (state.viewMode == CalendarViewMode.MONTH) {
                Spacer(modifier = Modifier.height(FarmSpacing.dp8))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FarmIconButton(
                        icon = Icons.Rounded.ChevronLeft,
                        contentDescription = "మునుపటి నెల",
                        onClick = onPreviousMonth,
                        containerColor = FarmColors.SurfaceVariant,
                    )

                    Surface(
                        shape = FarmShapeTokens.tag,
                        color = FarmColors.SecondaryContainer,
                        modifier = Modifier.clickable(role = Role.Button, onClick = onGoToToday),
                    ) {
                        Text(
                            text = if (state.isTelugu) "నేటి రోజుకు వెళ్లండి" else "Go to Today",
                            style = FarmTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FarmColors.SecondaryLeaf,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }

                    FarmIconButton(
                        icon = Icons.Rounded.ChevronRight,
                        contentDescription = "తర్వాతి నెల",
                        onClick = onNextMonth,
                        containerColor = FarmColors.SurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewModeSegmentedToggle(
    currentMode: CalendarViewMode,
    onModeSelected: (CalendarViewMode) -> Unit,
    isTelugu: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = FarmShapeTokens.card,
        color = FarmColors.SurfaceVariant,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CalendarViewMode.entries.forEach { mode ->
                val isSelected = mode == currentMode
                val label = if (isTelugu) mode.teluguLabel else mode.englishLabel
                Surface(
                    shape = FarmShapeTokens.tag,
                    color = if (isSelected) FarmColors.PrimaryForest else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.Tab) { onModeSelected(mode) },
                ) {
                    Text(
                        text = label,
                        style = FarmTypography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) FarmColors.OnPrimary else FarmColors.SecondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthGridView(
    state: TeluguCalendarUiState,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayNames = if (state.isTelugu) {
        listOf("ఆది", "సోమ", "మంగళ", "బుధ", "గురు", "శుక్ర", "శని")
    } else {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }

    val monthDays = state.monthDays
    val startDayOfWeek = remember(monthDays) {
        if (monthDays.isEmpty()) 0
        else {
            val firstDate = LocalDate.ofEpochDay(monthDays.first().epochDay)
            firstDate.dayOfWeek.value % 7 // Sunday = 0, Monday = 1, ..., Sat = 6
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = FarmSpacing.dp8),
        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp4),
    ) {
        // 1. Day of Week Header Row
        item(key = "day_header_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FarmSpacing.dp4),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                dayNames.forEachIndexed { index, name ->
                    val isSunday = index == 0
                    Text(
                        text = name,
                        style = FarmTypography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSunday) FarmColors.Terracotta else FarmColors.SecondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 2. Month Cells (Chunks of 7)
        val totalCells = startDayOfWeek + monthDays.size
        val totalRows = (totalCells + 6) / 7

        for (row in 0 until totalRows) {
            item(key = "calendar_row_$row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayIndex = cellIndex - startDayOfWeek
                        if (dayIndex in monthDays.indices) {
                            val day = monthDays[dayIndex]
                            val isSelected = state.selectedDay?.epochDay == day.epochDay
                            val isToday = state.todayEpochDay == day.epochDay
                            val taskCount = state.taskCountsByDay[day.epochDay] ?: 0

                            MonthDayCell(
                                day = day,
                                isSelected = isSelected,
                                isToday = isToday,
                                taskCount = taskCount,
                                onClick = { onSelectDay(day.epochDay) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    day: TeluguCalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    taskCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gregorianDayNum = remember(day.gregorianDate) {
        day.gregorianDate.takeLast(2).toIntOrNull()?.toString() ?: ""
    }

    val cellBackground = when {
        isSelected -> FarmColors.PrimaryContainer
        isToday -> FarmColors.SecondaryContainer
        else -> FarmColors.Surface
    }

    val borderColor = when {
        isSelected -> FarmColors.PrimaryDark
        isToday -> FarmColors.PrimaryForest
        else -> FarmColors.Outline.copy(alpha = 0.5f)
    }

    val borderWidth = when {
        isSelected -> 2.dp
        isToday -> 1.5.dp
        else -> 0.5.dp
    }

    Surface(
        shape = FarmShapeTokens.inputField,
        color = cellBackground,
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .height(64.dp)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top: Day number + Lunar Disc Badge
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = gregorianDayNum,
                    style = FarmTypography.bodyMedium,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) FarmColors.PrimaryForest else FarmColors.MainText,
                )

                // Lunar Badge: ● Amavasya (dark) / ○ Pournami (amber)
                when {
                    day.isAmavasya -> {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(FarmColors.MainText),
                        )
                    }
                    day.isPournami -> {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(FarmColors.HarvestAmber),
                        )
                    }
                }
            }

            // Middle: Telugu Tithi
            Text(
                text = day.tithi.teluguName,
                style = FarmTypography.labelSmall.copy(fontSize = 9.sp),
                color = FarmColors.SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Bottom: Task count badge or dot
            if (taskCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = FarmColors.HarvestAmber,
                    modifier = Modifier.size(16.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$taskCount",
                            style = FarmTypography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = FarmColors.OnPrimary,
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayDetailsBottomSheetContent(
    day: TeluguCalendarDay,
    tasks: List<CropTask>,
    isAudioSpeaking: Boolean,
    onToggleAudio: () -> Unit,
    onToggleTask: (CropTask) -> Unit,
    onAddTask: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp8),
        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
    ) {
        // Header Row: Full Date, Tithi, Audio Button, Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${day.gregorianDate} • ${day.teluguYear} సంవత్సరం",
                    style = FarmTypography.bodySmall,
                    color = FarmColors.SecondaryText,
                )
                Text(
                    text = "${day.teluguMonth.teluguName} • ${day.paksham.teluguName} ${day.tithi.teluguName}",
                    style = FarmTypography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = FarmColors.MainText,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                FarmIconButton(
                    icon = if (isAudioSpeaking) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = farmString(R.string.home_panchang_listen),
                    onClick = onToggleAudio,
                    containerColor = if (isAudioSpeaking) FarmColors.Warning else FarmColors.PrimaryContainer,
                    tint = if (isAudioSpeaking) FarmColors.MainText else FarmColors.PrimaryForest,
                )
                FarmIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = farmString(R.string.common_cancel),
                    onClick = onClose,
                    containerColor = FarmColors.SurfaceVariant,
                )
            }
        }

        // Nakshatram & Karte Badge Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = FarmShapeTokens.tag,
                color = FarmColors.PrimaryContainer,
            ) {
                Text(
                    text = "నక్షత్రం: ${day.nakshatram}",
                    style = FarmTypography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FarmColors.PrimaryForest,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Surface(
                shape = FarmShapeTokens.tag,
                color = FarmColors.SecondaryContainer,
            ) {
                Text(
                    text = "${day.karteName} కార్తె (${day.karteDayNumber}/${day.totalKarteDays}వ రోజు)",
                    style = FarmTypography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FarmColors.SecondaryLeaf,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        // Special Agro-Lunar Warnings (Amavasya / Festival)
        if (day.isAmavasya) {
            Surface(
                shape = FarmShapeTokens.card,
                color = FarmColors.WarningContainer,
                border = BorderStroke(1.dp, FarmColors.Warning),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(FarmSpacing.dp12),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = FarmColors.Warning,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "నేడు అమావాస్య: కాడెడ్లకు విశ్రాంతి ఇవ్వండి • రాత్రి పొలంలో దీపపు బుట్టలు (Light Traps) అమర్చండి.",
                        style = FarmTypography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = FarmColors.MainText,
                    )
                }
            }
        }

        if (day.isPournami && day.festivalName != null) {
            Surface(
                shape = FarmShapeTokens.card,
                color = FarmColors.AmberContainer,
                border = BorderStroke(1.dp, FarmColors.HarvestAmber),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "🌕 నేడు ${day.festivalName}: పవిత్ర పౌర్ణమి దినం.",
                    style = FarmTypography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = FarmColors.Terracotta,
                    modifier = Modifier.padding(FarmSpacing.dp12),
                )
            }
        }

        // Agricultural Advisory Card (అదను)
        if (!day.seasonalAdvisory.isNullOrBlank()) {
            Surface(
                shape = FarmShapeTokens.card,
                color = FarmColors.SurfaceVariant,
                border = BorderStroke(0.5.dp, FarmColors.Outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(FarmSpacing.dp12)) {
                    Text(
                        text = farmString(R.string.cal_advisory_label),
                        style = FarmTypography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FarmColors.PrimaryForest,
                    )
                    Spacer(modifier = Modifier.height(FarmSpacing.dp4))
                    Text(
                        text = day.seasonalAdvisory,
                        style = FarmTypography.bodyMedium,
                        color = FarmColors.MainText,
                    )
                }
            }
        }

        // Scheduled Crop Tasks Section
        Text(
            text = "ఈ రోజు మీ పొలం పనులు (${tasks.size})",
            style = FarmTypography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = FarmColors.MainText,
        )

        if (tasks.isEmpty()) {
            Surface(
                shape = FarmShapeTokens.tag,
                color = FarmColors.SurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = farmString(R.string.cal_no_tasks_for_day),
                    style = FarmTypography.bodySmall,
                    color = FarmColors.SecondaryText,
                    modifier = Modifier.padding(FarmSpacing.dp12),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                tasks.forEach { task ->
                    val isDone = task.status == TaskStatus.COMPLETED
                    Surface(
                        shape = FarmShapeTokens.tag,
                        color = if (isDone) FarmColors.SuccessContainer else FarmColors.Surface,
                        border = BorderStroke(1.dp, if (isDone) FarmColors.Success else FarmColors.Outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FarmSpacing.dp8),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = { onToggleTask(task) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = FarmColors.Success,
                                    uncheckedColor = FarmColors.Outline,
                                ),
                            )
                            Spacer(modifier = Modifier.width(FarmSpacing.dp8))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = FarmTypography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDone) FarmColors.SecondaryText else FarmColors.MainText,
                                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                                )
                                Text(
                                    text = task.category.name,
                                    style = FarmTypography.labelSmall,
                                    color = FarmColors.SecondaryText,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1-Tap Action Button: "+ ఈ రోజుకు పని జోడించండి"
        FarmPrimaryButton(
            text = farmString(R.string.cal_add_task_for_day),
            leadingIcon = Icons.Rounded.Add,
            onClick = { onAddTask(day.epochDay) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(FarmSpacing.dp16))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KarteTimelineView(
    state: TeluguCalendarUiState,
    onFilterSeason: (SeasonFilter) -> Unit,
    onSpeakKarte: (TeluguKarte) -> Unit,
    onOpenGuide: (Int?) -> Unit,
    onAddTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(FarmSpacing.dp16),
        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
    ) {
        // 1. Active Karte Hero Card
        state.currentKarte?.let { currentKarte ->
            item(key = "active_karte_hero") {
                ActiveKarteHeroCard(
                    karteInfo = currentKarte,
                    onOpenGuide = { onOpenGuide(currentKarte.karteId) },
                    onSpeak = {
                        val matchingKarte = state.allKartes.firstOrNull { it.id == currentKarte.karteId }
                        if (matchingKarte != null) onSpeakKarte(matchingKarte)
                    },
                    onAddTask = { onAddTask(state.todayEpochDay) },
                )
            }
        }

        // 2. Season Filter Chips Row
        item(key = "season_filter_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                SeasonFilter.entries.forEach { filter ->
                    val isSelected = filter == state.activeSeasonFilter
                    val label = if (state.isTelugu) filter.teluguLabel else filter.englishLabel
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSeason(filter) },
                        label = { Text(label, style = FarmTypography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmColors.PrimaryForest,
                            selectedLabelColor = FarmColors.OnPrimary,
                        ),
                    )
                }
            }
        }

        // 3. 27 Kartis Visual Timeline List
        items(
            items = state.filteredKartis,
            key = { it.id },
            contentType = { "karte_timeline_card" },
        ) { karte ->
            val isActive = state.currentKarte?.karteId == karte.id
            val isPassed = (state.currentKarte?.karteId ?: 0) > karte.id

            KarteTimelineCard(
                karte = karte,
                isActive = isActive,
                isPassed = isPassed,
                onSpeak = { onSpeakKarte(karte) },
                onClick = { onOpenGuide(karte.id) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveKarteHeroCard(
    karteInfo: CurrentKarteInfo,
    onOpenGuide: () -> Unit,
    onSpeak: () -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(2.dp, FarmColors.PrimaryForest),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            // Header Row: Active Badge & Days remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = FarmShapeTokens.tag,
                    color = FarmColors.PrimaryContainer,
                    border = BorderStroke(1.dp, FarmColors.PrimaryForest),
                ) {
                    Text(
                        text = "🌾 ప్రస్తుత కార్తె (Active)",
                        style = FarmTypography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FarmColors.PrimaryForest,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                Text(
                    text = "ఇంకా ${karteInfo.daysRemaining} రోజులు మిగిలాయి",
                    style = FarmTypography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FarmColors.HarvestAmber,
                )
            }

            // Karte Name & Day count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${karteInfo.teluguName} కార్తె (${karteInfo.englishName})",
                    style = FarmTypography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = FarmColors.MainText,
                )

                FarmIconButton(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = farmString(R.string.home_panchang_listen),
                    onClick = onSpeak,
                    containerColor = FarmColors.PrimaryContainer,
                    tint = FarmColors.PrimaryForest,
                )
            }

            Text(
                text = "${karteInfo.dayNumber}వ రోజు / ${karteInfo.totalDays} రోజులు",
                style = FarmTypography.bodySmall,
                color = FarmColors.SecondaryText,
            )

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { karteInfo.progressPercentage },
                    color = FarmColors.PrimaryForest,
                    trackColor = FarmColors.PrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                )
                Text(
                    text = "${(karteInfo.progressPercentage * 100).toInt()}% పూర్తి",
                    style = FarmTypography.labelSmall,
                    color = FarmColors.SecondaryText,
                )
            }

            // Proverb Card
            Surface(
                shape = FarmShapeTokens.tag,
                color = FarmColors.AmberContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(FarmSpacing.dp8)) {
                    Text(
                        text = "సామెత: \"${karteInfo.proverb}\"",
                        style = FarmTypography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = FarmColors.Terracotta,
                    )
                }
            }

            // Agricultural Significance
            Text(
                text = "అదను: ${karteInfo.agriculturalSignificance}",
                style = FarmTypography.bodyMedium,
                color = FarmColors.MainText,
            )

            // Recommended Crops
            if (karteInfo.recommendedCrops.isNotEmpty()) {
                Text(
                    text = farmString(R.string.cal_suited_crops),
                    style = FarmTypography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = FarmColors.SecondaryText,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    karteInfo.recommendedCrops.forEach { crop ->
                        Surface(
                            shape = FarmShapeTokens.tag,
                            color = FarmColors.SecondaryContainer,
                        ) {
                            Text(
                                text = crop,
                                style = FarmTypography.labelSmall,
                                color = FarmColors.SecondaryLeaf,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                FarmSecondaryButton(
                    text = "+ పని జోడించండి",
                    leadingIcon = Icons.Rounded.Add,
                    onClick = onAddTask,
                    modifier = Modifier.weight(1f),
                )
                FarmSecondaryButton(
                    text = "గైడ్ తెరవండి",
                    useMutedOutline = true,
                    onClick = onOpenGuide,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KarteTimelineCard(
    karte: TeluguKarte,
    isActive: Boolean,
    isPassed: Boolean,
    onSpeak: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardBackground = when {
        isActive -> FarmColors.PrimaryContainer.copy(alpha = 0.5f)
        isPassed -> FarmColors.SurfaceVariant.copy(alpha = 0.5f)
        else -> FarmColors.Surface
    }

    val cardBorder = when {
        isActive -> BorderStroke(1.5.dp, FarmColors.PrimaryForest)
        else -> BorderStroke(0.5.dp, FarmColors.Outline)
    }

    Surface(
        shape = FarmShapeTokens.card,
        color = cardBackground,
        border = cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(FarmSpacing.dp12),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${karte.id}. ${karte.teluguName} (${karte.englishName})",
                        style = FarmTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FarmColors.MainText,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        isActive -> {
                            Surface(
                                shape = FarmShapeTokens.tag,
                                color = FarmColors.PrimaryForest,
                            ) {
                                Text(
                                    text = "● ప్రస్తుత కార్తె",
                                    style = FarmTypography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmColors.OnPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        isPassed -> {
                            Text(
                                text = "ముగిసింది ✓",
                                style = FarmTypography.labelSmall,
                                color = FarmColors.SecondaryText,
                            )
                        }
                        else -> {
                            Text(
                                text = "రాబోయే కార్తె ⏳",
                                style = FarmTypography.labelSmall,
                                color = FarmColors.HarvestAmber,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(FarmSpacing.dp4))
                    FarmIconButton(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "వినండి",
                        onClick = onSpeak,
                        containerColor = Color.Transparent,
                        tint = FarmColors.PrimaryForest,
                    )
                }
            }

            Text(
                text = "${karte.approxDateSpan} • ${karte.season}",
                style = FarmTypography.bodySmall,
                color = FarmColors.SecondaryText,
            )

            Text(
                text = "సామెత: \"${karte.proverb}\"",
                style = FarmTypography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = FarmColors.Terracotta,
            )

            Text(
                text = "అదను: ${karte.agriculturalSignificance}",
                style = FarmTypography.bodySmall,
                color = FarmColors.MainText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatGregorianMonthName(month: Int, isTelugu: Boolean): String = when (month) {
    1 -> if (isTelugu) "జనవరి" else "January"
    2 -> if (isTelugu) "ఫిబ్రవరి" else "February"
    3 -> if (isTelugu) "మార్చి" else "March"
    4 -> if (isTelugu) "ఏప్రిల్" else "April"
    5 -> if (isTelugu) "మే" else "May"
    6 -> if (isTelugu) "జూన్" else "June"
    7 -> if (isTelugu) "జూలై" else "July"
    8 -> if (isTelugu) "ఆగస్టు" else "August"
    9 -> if (isTelugu) "సెప్టెంబర్" else "September"
    10 -> if (isTelugu) "అక్టోబర్" else "October"
    11 -> if (isTelugu) "నవంబర్" else "November"
    12 -> if (isTelugu) "డిసెంబర్" else "December"
    else -> ""
}
```

---

### 4.2 Implementation: `KarteGuideScreen.kt`

Path: `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/KarteGuideScreen.kt`

```kotlin
package com.farmsaathi.feature.farm.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmIconButton
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmSearchField
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.TeluguKarte
import com.farmsaathi.feature.farm.viewmodel.SeasonFilter
import com.farmsaathi.feature.farm.viewmodel.TeluguCalendarViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KarteGuideScreen(
    viewModel: TeluguCalendarViewModel,
    highlightKarteId: Int? = null,
    onBack: () -> Unit,
    onAddTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Filter kartis by search query and active season filter
    val displayedKartis = remember(state.filteredKartis, searchQuery) {
        if (searchQuery.isBlank()) {
            state.filteredKartis
        } else {
            val q = searchQuery.trim().lowercase()
            state.filteredKartis.filter { karte ->
                karte.teluguName.lowercase().contains(q) ||
                    karte.englishName.lowercase().contains(q) ||
                    karte.proverb.lowercase().contains(q) ||
                    karte.agriculturalSignificance.lowercase().contains(q) ||
                    karte.recommendedCrops.any { it.lowercase().contains(q) }
            }
        }
    }

    LaunchedEffect(highlightKarteId, displayedKartis) {
        if (highlightKarteId != null) {
            val targetIdx = displayedKartis.indexOfFirst { it.id == highlightKarteId }
            if (targetIdx >= 0) {
                listState.animateScrollToItem(targetIdx + 2) // +2 accounts for search and chips header items
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.cal_karte_guide_title),
                subtitle = "27 సూర్య నక్షత్రాల సాగు విజ్ఞానం & సామెతలు",
                onBackClick = onBack,
                audioCueKey = "karte_guide_overview",
                isAudioPlaying = state.isAudioSpeaking,
                onToggleAudio = { viewModel.togglePanchangAudio() },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            // 1. Search Bar
            item(key = "search_bar") {
                FarmSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "కార్తె పేరు, పంట లేదా సామెత వెతకండి...",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 2. Season Filter Chips
            item(key = "season_filter_chips") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    SeasonFilter.entries.forEach { filter ->
                        val isSelected = filter == state.activeSeasonFilter
                        val label = if (state.isTelugu) filter.teluguLabel else filter.englishLabel
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.filterSeason(filter) },
                            label = { Text(label, style = FarmTypography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmColors.PrimaryForest,
                                selectedLabelColor = FarmColors.OnPrimary,
                            ),
                        )
                    }
                }
            }

            // 3. Empty Search State
            if (displayedKartis.isEmpty()) {
                item(key = "empty_state") {
                    FarmEmptyState(
                        message = "వెతికిన పదానికి సరిపడే కార్తెలు కనబడలేదు.",
                        icon = Icons.Rounded.Search,
                    )
                }
            } else {
                // 4. Master 27 Kartis Expandable Cards
                items(
                    items = displayedKartis,
                    key = { it.id },
                    contentType = { "karte_guide_item" },
                ) { karte ->
                    val isCurrent = state.currentKarte?.karteId == karte.id
                    val isInitiallyExpanded = karte.id == highlightKarteId || isCurrent

                    ExpandableKarteGuideCard(
                        karte = karte,
                        isCurrent = isCurrent,
                        initiallyExpanded = isInitiallyExpanded,
                        onSpeak = { viewModel.speakKarte(karte) },
                        onAddTask = { onAddTask(state.todayEpochDay) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandableKarteGuideCard(
    karte: TeluguKarte,
    isCurrent: Boolean,
    initiallyExpanded: Boolean,
    onSpeak: () -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Surface(
        shape = FarmShapeTokens.card,
        color = if (isCurrent) FarmColors.PrimaryContainer.copy(alpha = 0.4f) else FarmColors.Surface,
        border = BorderStroke(if (isCurrent) 1.5.dp else 1.dp, if (isCurrent) FarmColors.PrimaryForest else FarmColors.Outline),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(FarmSpacing.dp16)) {
            // Header Row: [ID. Name] + Current Badge + Audio + Expand Arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = FarmShapeTokens.circular,
                        color = if (isCurrent) FarmColors.PrimaryForest else FarmColors.SurfaceVariant,
                    ) {
                        Text(
                            text = "${karte.id}",
                            style = FarmTypography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) FarmColors.OnPrimary else FarmColors.MainText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    Column {
                        Text(
                            text = "${karte.teluguName} (${karte.englishName})",
                            style = FarmTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FarmColors.MainText,
                        )
                        Text(
                            text = "${karte.approxDateSpan} • ${karte.season}",
                            style = FarmTypography.bodySmall,
                            color = FarmColors.SecondaryText,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        Surface(
                            shape = FarmShapeTokens.tag,
                            color = FarmColors.PrimaryForest,
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            Text(
                                text = "ప్రస్తుతం",
                                style = FarmTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FarmColors.OnPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    FarmIconButton(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "వినండి",
                        onClick = onSpeak,
                        containerColor = FarmColors.PrimaryContainer,
                        tint = FarmColors.PrimaryForest,
                    )

                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "కుదించు" else "వివరాలు చూడండి",
                        tint = FarmColors.SecondaryText,
                    )
                }
            }

            // Expandable Content Body
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FarmSpacing.dp12),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    // Proverb Box
                    Surface(
                        shape = FarmShapeTokens.tag,
                        color = FarmColors.AmberContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(FarmSpacing.dp12)) {
                            Text(
                                text = farmString(R.string.cal_proverb_label),
                                style = FarmTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FarmColors.Warning,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "\"${karte.proverb}\"",
                                style = FarmTypography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FarmColors.Terracotta,
                            )
                        }
                    }

                    // Agricultural Significance
                    Column {
                        Text(
                            text = farmString(R.string.cal_advisory_label),
                            style = FarmTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FarmColors.PrimaryForest,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = karte.agriculturalSignificance,
                            style = FarmTypography.bodyMedium,
                            color = FarmColors.MainText,
                        )
                    }

                    // Recommended Crops
                    if (karte.recommendedCrops.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = farmString(R.string.cal_suited_crops),
                                style = FarmTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FarmColors.SecondaryText,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                karte.recommendedCrops.forEach { crop ->
                                    Surface(
                                        shape = FarmShapeTokens.tag,
                                        color = FarmColors.SecondaryContainer,
                                    ) {
                                        Text(
                                            text = crop,
                                            style = FarmTypography.labelSmall,
                                            color = FarmColors.SecondaryLeaf,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(FarmSpacing.dp4))

                    // 1-Tap Action: Add activity based on this Karte
                    FarmSecondaryButton(
                        text = "+ ఈ కార్తెలో పని రాయండి",
                        leadingIcon = Icons.Rounded.Add,
                        onClick = onAddTask,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
```

---

### 4.3 Implementation: Wiring `ActivityFormScreen` in `CropsAndCalendarScreens.kt`

Path: `feature/farm/src/main/kotlin/com/farmsaathi/feature/farm/screens/CropsAndCalendarScreens.kt`

**Target lines: 1152–1171**

#### Before:
```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityFormScreen(
    initialCropCycleId: Long?,
    initialPlotId: Long?,
    viewModel: CropAndCalendarViewModel,
    onBack: () -> Unit,
    taskId: Long? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = remember(taskId, state.tasks) { taskId?.let { id -> state.tasks.firstOrNull { it.id == id } } }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var selectedCycleId by remember(existing, initialCropCycleId, state.cycles) {
        mutableStateOf(existing?.cropCycleId ?: initialCropCycleId ?: state.cycles.firstOrNull()?.id)
    }
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var category by remember(existing) { mutableStateOf(existing?.category ?: TaskCategory.OTHER) }
    var dueEpochDay by remember(existing, state.todayEpochDay) {
        mutableStateOf(existing?.dueDateEpochDay ?: state.todayEpochDay)
    }
```

#### After:
```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityFormScreen(
    initialCropCycleId: Long?,
    initialPlotId: Long?,
    viewModel: CropAndCalendarViewModel,
    onBack: () -> Unit,
    taskId: Long? = null,
    initialDueDateEpochDay: Long? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = remember(taskId, state.tasks) { taskId?.let { id -> state.tasks.firstOrNull { it.id == id } } }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var selectedCycleId by remember(existing, initialCropCycleId, state.cycles) {
        mutableStateOf(existing?.cropCycleId ?: initialCropCycleId ?: state.cycles.firstOrNull()?.id)
    }
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var category by remember(existing) { mutableStateOf(existing?.category ?: TaskCategory.OTHER) }
    var dueEpochDay by remember(existing, initialDueDateEpochDay, state.todayEpochDay) {
        mutableStateOf(existing?.dueDateEpochDay ?: initialDueDateEpochDay ?: state.todayEpochDay)
    }
```

#### Corresponding NavGraph Wiring (`FarmAndCropsNavGraph.kt:163-170`):
```kotlin
        ActivityFormScreen(
            initialCropCycleId = route.cropCycleId,
            initialPlotId = route.plotId,
            viewModel = vm,
            onBack = { nav.popBackStack() },
            taskId = route.taskId,
            initialDueDateEpochDay = route.initialDueDateEpochDay,
        )
```

---

## 5. Verification Method

### 5.1 Compilation & Build Verification
Execute from workspace root:
```powershell
./gradlew.bat :feature:farm:compileDebugKotlin
./gradlew.bat :feature:farm:assembleDebug
```
**Pass Criteria:** Zero compilation errors; zero unresolved references for `TeluguCalendarScreen`, `KarteGuideScreen`, and `ActivityFormScreen`.

### 5.2 Unit Test Verification
Execute:
```powershell
./gradlew.bat :feature:farm:testDebugUnitTest
```
**Pass Criteria:** All unit tests in `:feature:farm` pass cleanly.

### 5.3 Invalidation Conditions
- If `CalendarViewMode` or `SeasonFilter` enum names differ in `TeluguCalendarViewModel`, the screen imports must be matched.
- If `initialDueDateEpochDay` is not routed from `FarmRoute.ActivityForm`, the calendar date selection will not propagate to `ActivityFormScreen`.
