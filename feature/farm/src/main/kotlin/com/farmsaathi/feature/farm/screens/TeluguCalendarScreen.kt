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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.FarmIconButton
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
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
            FarmLoadingState(
                message = farmString(R.string.common_loading),
                modifier = Modifier.padding(padding),
            )
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
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
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
        if (monthDays.isEmpty()) {
            0
        } else {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
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
        val advisory = day.seasonalAdvisory
        if (!advisory.isNullOrBlank()) {
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
                        text = advisory,
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
                        val matchingKarte = state.allKartis.firstOrNull { it.id == currentKarte.karteId }
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
