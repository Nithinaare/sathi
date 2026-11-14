package com.farmsaathi.feature.farm.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.DropdownOption
import com.farmsaathi.core.designsystem.components.FarmActionTile
import com.farmsaathi.core.designsystem.components.FarmConfirmationDialog
import com.farmsaathi.core.designsystem.components.FarmDatePicker
import com.farmsaathi.core.designsystem.components.FarmDropdown
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmIconButton
import com.farmsaathi.core.designsystem.components.FarmImageGridPicker
import com.farmsaathi.core.designsystem.components.FarmImagePickerOption
import com.farmsaathi.core.designsystem.components.FarmListItem
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.components.FarmMoreDetailsAccordion
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmRecordCard
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.components.FarmSectionHeader
import com.farmsaathi.core.designsystem.components.FarmSnackbar
import com.farmsaathi.core.designsystem.components.FarmStatusIndicator
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmStickyFooterBar
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.components.SnackbarTone
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.CropCycleStatus
import com.farmsaathi.core.model.CropStageTemplate
import com.farmsaathi.core.model.CropTask
import com.farmsaathi.core.model.CropTemplate
import com.farmsaathi.core.model.SeasonType
import com.farmsaathi.core.model.TaskCategory
import com.farmsaathi.core.model.TaskPriority
import com.farmsaathi.core.model.TaskStatus
import com.farmsaathi.feature.farm.viewmodel.CropAndCalendarViewModel

@Composable
fun CropListScreen(
    plotId: Long?,
    viewModel: CropAndCalendarViewModel,
    onBack: () -> Unit,
    onOpenCrop: (Long) -> Unit,
    onAddCrop: (Long?) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val cycles =
        remember(state.cycles, plotId) {
            if (plotId == null) state.cycles else state.cycles.filter { it.plotId == plotId }
        }
    val plotsById = remember(state.plots) { state.plots.associateBy { it.id } }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.crops_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.CROPS_OVERVIEW,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.CROPS_OVERVIEW,
                onToggleAudio = viewModel::toggleAudio,
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.crops_add_btn),
                    leadingIcon = Icons.Rounded.Add,
                    onClick = { onAddCrop(plotId) },
                )
            }
        },
    ) { padding ->
        if (cycles.isEmpty()) {
            FarmEmptyState(
                title = farmString(R.string.crops_title),
                message = if (useTe) "ఇంకా ఏ పంట రాయలేదు. కింద ఉన్న బటన్ నొక్కి కొత్త పంట కలపండి." else "No crop cycles recorded yet.",
                icon = Icons.Rounded.Grass,
                imageRes = FarmImages.cropPaddy,
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(FarmSpacing.dp16),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                items(
                    cycles,
                    key = { it.id },
                    contentType = { "crop_cycle_card" },
                ) { cycle ->
                    val plotName = plotsById[cycle.plotId]?.name.orEmpty()
                    FarmRecordCard(
                        title = listOfNotNull(cycle.cropName, cycle.variety).joinToString(" • "),
                        subtitle = "$plotName • ${FarmLabels.season(cycle.season, lang)}",
                        metadata = viewModel.formatEpochDay(cycle.sowingDateEpochDay),
                        statusType =
                        if (cycle.status == CropCycleStatus.ACTIVE) {
                            FarmStatusType.SUCCESS
                        } else {
                            FarmStatusType.NEUTRAL
                        },
                        statusLabel = FarmLabels.cropCycleStatus(cycle.status, lang),
                        leadingIcon = Icons.Rounded.Grass,
                        leadingImageRes = FarmImages.forCropName(cycle.cropName),
                        onClick = { onOpenCrop(cycle.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun CropDetailScreen(
    cropCycleId: Long,
    viewModel: CropAndCalendarViewModel,
    onBack: () -> Unit,
    onEditCrop: (Long) -> Unit,
    onOpenActivities: (Long) -> Unit,
    onOpenDiary: (Long) -> Unit,
    onAddExpense: (Long) -> Unit,
    onAddHarvest: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cycle = remember(state.cycles, cropCycleId) { state.cycles.firstOrNull { it.id == cropCycleId } }
    val tasks = remember(state.tasks, cropCycleId) { state.tasks.filter { it.cropCycleId == cropCycleId } }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    if (cycle == null) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = listOfNotNull(cycle.cropName, cycle.variety).joinToString(" • "),
                subtitle = FarmLabels.season(cycle.season, lang),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                trailingAction = {
                    FarmIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = farmString(R.string.common_edit),
                        onClick = { onEditCrop(cycle.id) },
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            item(key = "crop_status_indicator", contentType = "status_indicator") {
                FarmStatusIndicator(
                    type = if (cycle.status == CropCycleStatus.ACTIVE) FarmStatusType.SUCCESS else FarmStatusType.INFO,
                    label = FarmLabels.cropCycleStatus(cycle.status, lang),
                )
            }
            item(key = "crop_actions_row_1", contentType = "two_col_action_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmActionTile(
                        title = farmString(R.string.act_title),
                        icon = Icons.AutoMirrored.Rounded.EventNote,
                        imageRes = FarmImages.farmWork,
                        compact = true,
                        onClick = { onOpenActivities(cycle.id) },
                        modifier = Modifier.weight(1f),
                    )
                    FarmActionTile(
                        title = farmString(R.string.diary_title),
                        icon = Icons.Rounded.PhotoCamera,
                        imageRes = FarmImages.diary,
                        compact = true,
                        onClick = { onOpenDiary(cycle.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item(key = "crop_actions_row_2", contentType = "two_col_action_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmActionTile(
                        title = farmString(R.string.exp_add_btn),
                        icon = Icons.Rounded.Payments,
                        imageRes = FarmImages.expense,
                        compact = true,
                        onClick = { onAddExpense(cycle.id) },
                        modifier = Modifier.weight(1f),
                    )
                    FarmActionTile(
                        title = farmString(R.string.harv_add_btn),
                        icon = Icons.Rounded.Warehouse,
                        imageRes = FarmImages.harvest,
                        compact = true,
                        onClick = { onAddHarvest(cycle.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (cycle.status == CropCycleStatus.ACTIVE) {
                item(key = "mark_harvested_btn", contentType = "action_button") {
                    FarmSecondaryButton(
                        text = if (useTe) "కోత పూర్తయినట్లు గుర్తు పెట్టండి" else "Mark as Harvested",
                        onClick = { viewModel.updateCycleStatus(cycle.id, CropCycleStatus.HARVESTED) },
                    )
                }
            }
            item(key = "crop_tasks_header", contentType = "section_header") {
                FarmSectionHeader(
                    title = farmString(R.string.act_title),
                    actionLabel = farmString(R.string.common_see_all),
                    onActionClick = { onOpenActivities(cycle.id) },
                )
            }
            items(
                tasks.take(6),
                key = { it.id },
                contentType = { "crop_task_card" },
            ) { task ->
                val done = task.status == TaskStatus.COMPLETED
                FarmRecordCard(
                    title = task.title,
                    subtitle = viewModel.formatEpochDay(task.dueDateEpochDay),
                    statusType = if (done) FarmStatusType.SUCCESS else FarmStatusType.WARNING,
                    statusLabel = FarmLabels.taskStatus(task.status, lang),
                    leadingIcon = Icons.Rounded.CheckCircle,
                    leadingImageRes = FarmImages.forTaskCategory(task.category),
                    onClick = {
                        if (!done) viewModel.updateTaskStatus(task.id, TaskStatus.COMPLETED)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CropFormScreen(
    initialPlotId: Long?,
    cropCycleId: Long?,
    viewModel: CropAndCalendarViewModel,
    onBack: () -> Unit,
    initialFarmId: Long? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing =
        remember(cropCycleId, state.cycles) {
            cropCycleId?.let { id -> state.cycles.firstOrNull { it.id == id } }
        }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    val effectiveFarmId = existing?.farmId ?: initialFarmId
    val availablePlots =
        remember(state.plots, effectiveFarmId) {
            if (effectiveFarmId != null) {
                val farmPlots = state.plots.filter { it.farmId == effectiveFarmId }
                if (farmPlots.isNotEmpty()) farmPlots else state.plots
            } else {
                state.plots
            }
        }
    var selectedPlotId by remember(existing, initialPlotId, effectiveFarmId, availablePlots) {
        mutableStateOf(
            existing?.plotId
                ?: initialPlotId
                ?: availablePlots.firstOrNull { effectiveFarmId == null || it.farmId == effectiveFarmId }?.id,
        )
    }
    var selectedTemplateId by remember(existing) { mutableStateOf(existing?.templateId) }
    var cropName by remember(existing) { mutableStateOf(existing?.cropName.orEmpty()) }
    var variety by remember(existing) { mutableStateOf(existing?.variety.orEmpty()) }
    var season by remember(existing) { mutableStateOf(existing?.season ?: SeasonType.KHARIF) }
    var sowingEpochDay by remember(existing, state.todayEpochDay) {
        mutableStateOf(existing?.sowingDateEpochDay ?: state.todayEpochDay)
    }
    var harvestDurationDays by remember(existing) {
        mutableStateOf(
            existing?.expectedHarvestEpochDay?.let { exp ->
                (exp - existing.sowingDateEpochDay).toInt().takeIf { it > 0 }
            },
        )
    }
    var generateFromTemplate by remember { mutableStateOf(true) }
    var recalculateTasks by remember { mutableStateOf(true) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val selectedPlot = state.plots.firstOrNull { it.id == selectedPlotId }
    val reqField = farmString(R.string.common_required_field)

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (existing == null) farmString(R.string.crops_add_btn) else farmString(R.string.common_edit),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.CROP_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.CROP_FORM_HELP,
                onToggleAudio = viewModel::toggleAudio,
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.common_save),
                    isLoading = state.isSaving,
                    onClick = {
                        if (cropName.isBlank()) {
                            errorMsg = reqField
                            viewModel.playErrorCue()
                        } else {
                            errorMsg = null
                            viewModel.saveCropCycle(
                                existing = existing,
                                plot = selectedPlot,
                                templateId = selectedTemplateId,
                                cropName = cropName,
                                variety = variety,
                                season = season,
                                sowingEpochDay = sowingEpochDay,
                                expectedHarvestEpochDay = harvestDurationDays?.let { sowingEpochDay + it.toLong() },
                                generateTasksFromTemplate = generateFromTemplate && selectedTemplateId != null,
                                recalculateTasksOnEdit = recalculateTasks,
                                notes = notes,
                                farmIdHint = effectiveFarmId,
                            ) { onBack() }
                        }
                    },
                )
            }
        },
    ) { padding ->
        var showMore by remember { mutableStateOf(false) }
        val visualCrops =
            remember(useTe) {
                listOf(
                    (if (useTe) "వరి" else "Paddy") to FarmImages.cropPaddy,
                    (if (useTe) "పత్తి" else "Cotton") to FarmImages.forCropName("cotton"),
                    (if (useTe) "మిర్చి" else "Chilli") to FarmImages.forCropName("chilli"),
                    (if (useTe) "మొక్కజొన్న" else "Maize") to FarmImages.forCropName("maize"),
                    (if (useTe) "వేరుశనగ" else "Groundnut") to FarmImages.forCropName("groundnut"),
                    (if (useTe) "ఇతర పంట" else "Other Crop") to FarmImages.cropGeneric,
                )
            }
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(FarmSpacing.dp16)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            if (existing == null) {
                FarmImageGridPicker(
                    label = if (useTe) "1. పంట ఫోటో ఎంచుకోండి" else "1. Pick Crop Photo",
                    options =
                    visualCrops.map { (title, resId) ->
                        FarmImagePickerOption(
                            value = title,
                            label = title,
                            imageRes = resId,
                        )
                    },
                    selected = cropName.takeIf { it.isNotBlank() },
                    onSelect = { chosen ->
                        val isCustom = chosen == "ఇతర పంట" || chosen == "Other Crop"
                        cropName = if (isCustom) "" else chosen
                        errorMsg = null
                        val matchedTmpl =
                            state.templates.firstOrNull { tmpl ->
                                tmpl.cropNameTe.equals(chosen, ignoreCase = true) ||
                                    tmpl.cropNameEn.equals(chosen, ignoreCase = true)
                            }
                        if (matchedTmpl != null) {
                            selectedTemplateId = matchedTmpl.id
                            season = matchedTmpl.defaultSeason
                        }
                    },
                    columns = 3,
                )
            }
            FarmTextInput(
                value = cropName,
                onValueChange = {
                    cropName = it
                    errorMsg = null
                },
                label = farmString(R.string.crops_name_label),
                errorText = errorMsg,
            )
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                Text(
                    text = if (useTe) "మడి ఎంచుకోండి" else "Select Plot",
                    style = FarmTypography.labelLarge,
                    color = FarmColors.MainText,
                )
                val targetFarmPlots =
                    if (effectiveFarmId != null) state.plots.filter { it.farmId == effectiveFarmId } else state.plots
                val targetFarm = effectiveFarmId?.let { fId -> state.farms.firstOrNull { it.id == fId } }
                if (targetFarmPlots.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    ) {
                        targetFarmPlots.forEach { plot ->
                            val isSelected = selectedPlotId == plot.id
                            val farmLabel = state.farms.firstOrNull { it.id == plot.farmId }?.name
                            val chipLabel = if (effectiveFarmId == null && farmLabel != null) "${plot.name} ($farmLabel)" else plot.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPlotId = plot.id },
                                label = { Text(chipLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FarmColors.PrimaryContainer,
                                    selectedLabelColor = FarmColors.PrimaryForest,
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                            )
                        }
                    }
                } else if (targetFarm != null) {
                    FarmSecondaryButton(
                        text = if (useTe) "${targetFarm.name} లో మడి 1 కలపండి" else "Create Plot 1 in ${targetFarm.name}",
                        leadingIcon = Icons.Rounded.Add,
                        onClick = {
                            viewModel.createQuickPlot(targetFarm, useTe) { newPlotId ->
                                selectedPlotId = newPlotId
                            }
                        },
                    )
                }
            }
            FarmDatePicker(
                selectedDateMillis = viewModel.epochDayToMillis(sowingEpochDay),
                onDateSelected = { millis -> sowingEpochDay = viewModel.epochMillisToEpochDay(millis) },
                label = farmString(R.string.crops_sowing_date),
                formattedDate = viewModel.formatEpochDay(sowingEpochDay),
                todayLabel = farmString(R.string.common_today),
                yesterdayLabel = farmString(R.string.common_yesterday),
            )
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                Text(
                    text = if (useTe) "అంచనా కోత కాలం (ఐచ్ఛికం)" else "Expected Harvest Duration",
                    style = FarmTypography.labelLarge,
                    color = FarmColors.MainText,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    listOf(90, 120, 150).forEach { days ->
                        val isSelected = harvestDurationDays == days
                        FilterChip(
                            selected = isSelected,
                            onClick = { harvestDurationDays = if (isSelected) null else days },
                            label = { Text(if (useTe) "$days రోజులు" else "$days Days") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmColors.PrimaryContainer,
                                selectedLabelColor = FarmColors.PrimaryForest,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                        )
                    }
                }
                harvestDurationDays?.let { days ->
                    Text(
                        text =
                        if (useTe) {
                            "అంచనా కోత తేదీ: ${viewModel.formatEpochDay(sowingEpochDay + days)}"
                        } else {
                            "Est. Harvest: ${viewModel.formatEpochDay(sowingEpochDay + days)}"
                        },
                        style = FarmTypography.bodyMedium,
                        color = FarmColors.SecondaryText,
                    )
                }
            }
            if (existing == null && state.templates.isNotEmpty()) {
                FarmDropdown(
                    label = if (useTe) "పంట క్యాలెండర్ పట్టిక (టెంప్లేట్)" else "Crop Schedule Template",
                    options =
                    state.templates.map {
                        DropdownOption(it.id, if (useTe) it.cropNameTe else it.cropNameEn)
                    },
                    selectedValue = selectedTemplateId,
                    onSelect = { opt ->
                        selectedTemplateId = opt.value
                        val tmpl = state.templates.firstOrNull { it.id == opt.value }
                        if (tmpl != null) {
                            if (cropName.isBlank()) {
                                cropName = if (useTe) tmpl.cropNameTe else tmpl.cropNameEn
                            }
                            season = tmpl.defaultSeason
                        }
                    },
                    placeholder = if (useTe) "వరి / పత్తి / మిర్చి / కస్టమ్" else "Select template",
                )
            }
            if (existing == null && selectedTemplateId != null) {
                Surface(
                    shape = FarmShapeTokens.card,
                    color = FarmColors.Surface,
                    border = BorderStroke(1.dp, FarmColors.Outline),
                ) {
                    FarmListItem(
                        title = if (useTe) "పంట పనుల క్యాలెండర్ ఆటోమేటిక్‌గా కలపండి" else "Auto-add crop task calendar",
                        subtitle = if (useTe) "టెంప్లేట్ ప్రకారం తేదీలతో రిమైండర్లు వస్తాయి" else "Creates dated task reminders from template",
                        checked = generateFromTemplate,
                        onCheckedChange = { generateFromTemplate = it },
                        showDivider = false,
                    )
                }
            } else if (existing != null) {
                Surface(
                    shape = FarmShapeTokens.card,
                    color = FarmColors.Surface,
                    border = BorderStroke(1.dp, FarmColors.Outline),
                ) {
                    FarmListItem(
                        title = if (useTe) "నాటిన తేదీ మారితే రాబోయే పనుల తేదీలు మార్చండి" else "Shift pending task dates on edit",
                        checked = recalculateTasks,
                        onCheckedChange = { recalculateTasks = it },
                        showDivider = false,
                    )
                }
            }
            FarmMoreDetailsAccordion(
                expanded = showMore,
                onToggle = { showMore = !showMore },
                useTelugu = useTe,
            ) {
                FarmTextInput(
                    value = variety,
                    onValueChange = { variety = it },
                    label = farmString(R.string.crops_variety_label),
                )
                FarmDropdown(
                    label = if (useTe) "సీజన్" else "Season",
                    options = SeasonType.entries.map { DropdownOption(it, FarmLabels.season(it, lang)) },
                    selectedValue = season,
                    onSelect = { season = it.value },
                    placeholder = "",
                )
                FarmTextInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = farmString(R.string.common_notes_optional),
                    singleLine = false,
                )
            }
        }
    }
}

@Composable
fun CropTemplatesScreen(
    viewModel: CropAndCalendarViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val useTe = state.prefs.language == AppLanguage.TELUGU
    var editingTemplate by remember { mutableStateOf<CropTemplate?>(null) }
    var applyingTemplate by remember { mutableStateOf<CropTemplate?>(null) }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (useTe) "పంట పనుల టెంప్లేట్లు" else "Crop Templates",
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                trailingAction = {
                    FarmIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = if (useTe) "డిఫాల్ట్ పట్టికలు" else "Reset defaults",
                        onClick = {
                            viewModel.resetDefaultTemplates {
                                feedbackMsg = if (useTe) "డిఫాల్ట్ పట్టికలు పునరుద్ధరించబడ్డాయి" else "Default templates restored"
                            }
                        },
                    )
                },
            )
        },
        snackbarHost = {
            feedbackMsg?.let { msg ->
                FarmSnackbar(
                    message = msg,
                    tone = SnackbarTone.SUCCESS,
                    actionLabel = if (useTe) "సరే" else "OK",
                    onActionClick = { feedbackMsg = null },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            item(key = "templates_info_banner", contentType = "status_indicator") {
                FarmStatusIndicator(
                    type = FarmStatusType.INFO,
                    label = if (useTe) "మీ అనుభవం బట్టి రోజులు, పనులు మార్చుకొని పంటకు వేసుకోండి" else "Edit stages and apply to any active crop",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(
                state.templates,
                key = { it.id },
                contentType = { "crop_template_card" },
            ) { tmpl ->
                Surface(
                    shape = FarmShapeTokens.card,
                    color = FarmColors.Surface,
                    border = BorderStroke(1.dp, FarmColors.Outline),
                ) {
                    Column(
                        modifier = Modifier.padding(FarmSpacing.dp16),
                        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(FarmImages.forCropName(tmpl.cropNameEn)),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                Modifier
                                    .size(44.dp)
                                    .clip(FarmShapeTokens.inputField)
                                    .border(1.dp, FarmColors.Outline, FarmShapeTokens.inputField),
                            )
                            Spacer(Modifier.width(FarmSpacing.dp12))
                            Text(
                                text = if (useTe) tmpl.cropNameTe else tmpl.cropNameEn,
                                style = FarmTypography.titleMedium,
                                color = FarmColors.MainText,
                                modifier = Modifier.weight(1f),
                            )
                            FarmIconButton(
                                icon = Icons.Rounded.Edit,
                                contentDescription = farmString(R.string.common_edit),
                                onClick = { editingTemplate = tmpl },
                            )
                        }
                        tmpl.stages.forEach { st ->
                            val dayText = if (useTe) "• ${st.offsetDaysFromSowing}వ రోజు:" else "• Day ${st.offsetDaysFromSowing}:"
                            Text(
                                text = "$dayText ${if (useTe) st.titleTe else st.titleEn}",
                                style = FarmTypography.bodyMedium,
                                color = FarmColors.SecondaryText,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                        ) {
                            FarmSecondaryButton(
                                text = if (useTe) "మార్చండి" else "Edit Stages",
                                useMutedOutline = true,
                                onClick = { editingTemplate = tmpl },
                                modifier = Modifier.weight(1f),
                            )
                            FarmSecondaryButton(
                                text = if (useTe) "ఈ పట్టిక నా పంటకు వేయండి" else "Apply to Crop",
                                onClick = { applyingTemplate = tmpl },
                                modifier = Modifier.weight(1.4f),
                            )
                        }
                    }
                }
            }
        }
    }

    editingTemplate?.let { tmpl ->
        TemplateStagesEditorDialog(
            template = tmpl,
            useTe = useTe,
            isSaving = state.isSaving,
            onDismiss = { editingTemplate = null },
            onSave = { updated ->
                viewModel.upsertCropTemplate(updated) {
                    editingTemplate = null
                    feedbackMsg = if (useTe) "పట్టిక సేవ్ అయింది" else "Template saved"
                }
            },
        )
    }

    applyingTemplate?.let { tmpl ->
        val activeCycles = state.cycles.filter { it.status == CropCycleStatus.ACTIVE }
        AlertDialog(
            onDismissRequest = { applyingTemplate = null },
            title = {
                Text(
                    text = if (useTe) "ఏ పంటకు వేయాలి?" else "Apply to which crop?",
                    style = FarmTypography.titleMedium,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                    if (activeCycles.isEmpty()) {
                        Text(
                            text = if (useTe) "ముందు ఒక యాక్టివ్ పంటను కలపండి." else "Add an active crop first.",
                            style = FarmTypography.bodyMedium,
                            color = FarmColors.SecondaryText,
                        )
                    } else {
                        activeCycles.forEach { cycle ->
                            FarmActionTile(
                                title = listOfNotNull(cycle.cropName, cycle.variety).joinToString(" • "),
                                subtitle = viewModel.formatEpochDay(cycle.sowingDateEpochDay),
                                icon = Icons.Rounded.Grass,
                                imageRes = FarmImages.forCropName(cycle.cropName),
                                compact = true,
                                onClick = {
                                    viewModel.applyTemplateToCrop(cycle.id, tmpl.id) { added ->
                                        applyingTemplate = null
                                        feedbackMsg =
                                            if (useTe) {
                                                "${cycle.cropName} లో $added పనులు కలపబడ్డాయి!"
                                            } else {
                                                "Added $added tasks to ${cycle.cropName}"
                                            }
                                    }
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { applyingTemplate = null }) {
                    Text(farmString(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun TemplateStagesEditorDialog(
    template: CropTemplate,
    useTe: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (CropTemplate) -> Unit,
) {
    var stages by remember(template) { mutableStateOf(template.stages) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (useTe) "${template.cropNameTe} పట్టిక మార్చండి" else "Edit ${template.cropNameEn}",
                style = FarmTypography.titleMedium,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                stages.forEachIndexed { idx, st ->
                    Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                        ) {
                            TextButton(
                                onClick = {
                                    val nextDay = (st.offsetDaysFromSowing - 5).coerceAtLeast(0)
                                    stages = stages.mapIndexed { i, item -> if (i == idx) item.copy(offsetDaysFromSowing = nextDay) else item }
                                },
                            ) { Text("-5") }
                            Text(
                                text = if (useTe) "${st.offsetDaysFromSowing}వ రోజు" else "Day ${st.offsetDaysFromSowing}",
                                style = FarmTypography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = {
                                    val nextDay = st.offsetDaysFromSowing + 5
                                    stages = stages.mapIndexed { i, item -> if (i == idx) item.copy(offsetDaysFromSowing = nextDay) else item }
                                },
                            ) { Text("+5") }
                            if (stages.size > 1) {
                                FarmIconButton(
                                    icon = Icons.Rounded.Delete,
                                    contentDescription = if (useTe) "తొలగించు" else "Remove stage",
                                    tint = FarmColors.Error,
                                    onClick = { stages = stages.filterIndexed { i, _ -> i != idx } },
                                )
                            }
                        }
                        FarmTextInput(
                            value = if (useTe) st.titleTe else st.titleEn,
                            onValueChange = { nextTitle ->
                                stages =
                                    stages.mapIndexed { i, item ->
                                        if (i != idx) {
                                            item
                                        } else if (useTe) {
                                            item.copy(titleTe = nextTitle, titleEn = item.titleEn.ifBlank { nextTitle })
                                        } else {
                                            item.copy(titleEn = nextTitle, titleTe = item.titleTe.ifBlank { nextTitle })
                                        }
                                    }
                            },
                            label = if (useTe) "పని పేరు" else "Stage Task",
                        )
                        HorizontalDivider(color = FarmColors.Outline)
                    }
                }
                FarmSecondaryButton(
                    text = if (useTe) "+ కొత్త దశ కలపండి" else "+ Add Stage",
                    leadingIcon = Icons.Rounded.Add,
                    useMutedOutline = true,
                    onClick = {
                        val nextOffset = (stages.maxOfOrNull { it.offsetDaysFromSowing } ?: 0) + 15
                        val nextStage =
                            CropStageTemplate(
                                stageKey = "custom_${System.currentTimeMillis()}_${stages.size}",
                                offsetDaysFromSowing = nextOffset,
                                titleTe = "పొలం తనిఖీ / పని",
                                titleEn = "Field Check / Task",
                                category = TaskCategory.OTHER,
                            )
                        stages = stages + nextStage
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && stages.isNotEmpty(),
                onClick = {
                    val cleaned =
                        stages
                            .filter { it.titleTe.isNotBlank() || it.titleEn.isNotBlank() }
                            .sortedBy { it.offsetDaysFromSowing }
                    onSave(template.copy(stages = cleaned))
                },
            ) {
                Text(farmString(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(farmString(R.string.common_cancel))
            }
        },
    )
}

@Composable
fun ActivityListScreen(
    cropCycleId: Long?,
    plotId: Long?,
    viewModel: CropAndCalendarViewModel,
    onBack: () -> Unit,
    onAddActivity: (Long?, Long?) -> Unit,
    onEditActivity: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val tasks =
        remember(state.tasks, cropCycleId, plotId) {
            state.tasks.filter { task ->
                task.status != TaskStatus.CANCELLED &&
                    (cropCycleId == null || task.cropCycleId == cropCycleId) &&
                    (plotId == null || task.plotId == plotId)
            }
        }
    var actionTask by remember { mutableStateOf<CropTask?>(null) }
    var deleteConfirmTask by remember { mutableStateOf<CropTask?>(null) }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.act_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.ACTIVITIES_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.ACTIVITIES_HELP,
                onToggleAudio = viewModel::toggleAudio,
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.act_add_btn),
                    leadingIcon = Icons.Rounded.Add,
                    onClick = { onAddActivity(cropCycleId, plotId) },
                )
            }
        },
    ) { padding ->
        if (tasks.isEmpty()) {
            FarmEmptyState(
                title = farmString(R.string.act_title),
                message =
                if (useTe) {
                    "ఇంకా పనులు లేవు. కింద ఉన్న బటన్ నొక్కి కొత్త పొలం పని కలపండి."
                } else {
                    "No farm tasks yet. Tap below to add a task."
                },
                icon = Icons.Rounded.CalendarToday,
                imageRes = FarmImages.farmWork,
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(FarmSpacing.dp16),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                items(
                    tasks,
                    key = { it.id },
                    contentType = { "crop_task_card" },
                ) { task ->
                    val effectiveStatus =
                        if (task.status == TaskStatus.PENDING &&
                            task.dueDateEpochDay < state.todayEpochDay
                        ) {
                            TaskStatus.MISSED
                        } else {
                            task.status
                        }
                    val statusType =
                        when (effectiveStatus) {
                            TaskStatus.COMPLETED -> FarmStatusType.SUCCESS
                            TaskStatus.MISSED -> FarmStatusType.ERROR
                            TaskStatus.RESCHEDULED -> FarmStatusType.INFO
                            else -> FarmStatusType.WARNING
                        }
                    Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                        FarmRecordCard(
                            title = task.title,
                            subtitle = "${FarmLabels.taskCategory(task.category, lang)} • ${
                                viewModel.formatEpochDay(task.dueDateEpochDay)
                            }",
                            statusType = statusType,
                            statusLabel = FarmLabels.taskStatus(effectiveStatus, lang),
                            leadingIcon = Icons.AutoMirrored.Rounded.EventNote,
                            leadingImageRes = FarmImages.forTaskCategory(task.category),
                            onClick = { actionTask = task },
                        )
                        if (task.status != TaskStatus.COMPLETED) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                            ) {
                                FarmSecondaryButton(
                                    text = if (useTe) "పూర్తయింది" else "Mark Done",
                                    onClick = { viewModel.updateTaskStatus(task.id, TaskStatus.COMPLETED) },
                                    modifier = Modifier.weight(1f),
                                )
                                FarmSecondaryButton(
                                    text = if (useTe) "+3 రోజులు వాయిదా" else "+3 Days",
                                    useMutedOutline = true,
                                    onClick = {
                                        viewModel.updateTaskStatus(
                                            taskId = task.id,
                                            status = TaskStatus.RESCHEDULED,
                                            newDueEpochDay = state.todayEpochDay + 3L,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    actionTask?.let { task ->
        val isCompleted = task.status == TaskStatus.COMPLETED
        AlertDialog(
            onDismissRequest = { actionTask = null },
            title = { Text(task.title, style = FarmTypography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                    Text(
                        text = "${FarmLabels.taskCategory(task.category, lang)} • ${viewModel.formatEpochDay(task.dueDateEpochDay)}",
                        style = FarmTypography.bodyMedium,
                        color = FarmColors.SecondaryText,
                    )
                    if (isCompleted) {
                        FarmSecondaryButton(
                            text = if (useTe) "మళ్లీ తెరవండి (Reopen)" else "Reopen Task",
                            onClick = {
                                viewModel.updateTaskStatus(task.id, TaskStatus.PENDING)
                                actionTask = null
                            },
                        )
                    } else {
                        FarmSecondaryButton(
                            text = if (useTe) "పూర్తయింది" else "Mark Done",
                            onClick = {
                                viewModel.updateTaskStatus(task.id, TaskStatus.COMPLETED)
                                actionTask = null
                            },
                        )
                    }
                    FarmSecondaryButton(
                        text = farmString(R.string.common_edit),
                        leadingIcon = Icons.Rounded.Edit,
                        useMutedOutline = true,
                        onClick = {
                            actionTask = null
                            onEditActivity(task.id)
                        },
                    )
                    FarmSecondaryButton(
                        text = if (useTe) "పని తొలగించు" else "Delete Task",
                        leadingIcon = Icons.Rounded.Delete,
                        useMutedOutline = true,
                        onClick = {
                            actionTask = null
                            deleteConfirmTask = task
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { actionTask = null }) {
                    Text(farmString(R.string.common_cancel))
                }
            },
        )
    }

    deleteConfirmTask?.let { task ->
        FarmConfirmationDialog(
            title = if (useTe) "పని తీసేయాలా?" else "Delete task?",
            message = task.title,
            confirmLabel = farmString(R.string.common_confirm),
            dismissLabel = farmString(R.string.common_cancel),
            isDestructive = true,
            onConfirm = {
                viewModel.updateTaskStatus(task.id, TaskStatus.CANCELLED)
                deleteConfirmTask = null
            },
            onDismiss = { deleteConfirmTask = null },
        )
    }
}

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
    var notes by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var titleError by remember { mutableStateOf<String?>(null) }
    val reqMsg = farmString(R.string.common_required_field)

    val cycle = state.cycles.firstOrNull { it.id == selectedCycleId }
    val farmId =
        existing?.farmId
            ?: cycle?.farmId
            ?: state.plots.firstOrNull()?.farmId
            ?: state.prefs.activeFarmId
            ?: state.farms.firstOrNull()?.id
            ?: 0L

    val quickTasks =
        remember(useTe) {
            listOf(
                Triple(TaskCategory.LAND_PREP, if (useTe) "దున్నుడు" else "Ploughing", if (useTe) "పొలం దున్నుడు" else "Land preparation"),
                Triple(TaskCategory.SOWING, if (useTe) "విత్తనాలు" else "Sowing", if (useTe) "విత్తనాలు వేయడం" else "Sowing seeds"),
                Triple(TaskCategory.WEEDING, if (useTe) "కలుపు తీత" else "Weeding", if (useTe) "కలుపు తీత" else "Weeding"),
                Triple(TaskCategory.SPRAYING, if (useTe) "మందు స్ప్రే" else "Spray", if (useTe) "మందు స్ప్రే చేయడం" else "Crop spraying"),
                Triple(TaskCategory.FERTILIZER, if (useTe) "ఎరువు" else "Fertilizer", if (useTe) "ఎరువు వేయడం" else "Fertilizer application"),
            )
        }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (existing == null) farmString(R.string.act_add_btn) else farmString(R.string.common_edit),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.ACTIVITIES_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.ACTIVITIES_HELP,
                onToggleAudio = viewModel::toggleAudio,
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.common_save),
                    isLoading = state.isSaving,
                    enabled = farmId != 0L,
                    onClick = {
                        when {
                            farmId == 0L -> viewModel.playErrorCue()
                            title.isBlank() -> {
                                titleError = reqMsg
                                viewModel.playErrorCue()
                            }
                            else -> {
                                viewModel.saveTask(
                                    existing = existing,
                                    farmId = farmId,
                                    plotId = cycle?.plotId ?: existing?.plotId ?: initialPlotId,
                                    cropCycleId = cycle?.id,
                                    title = title,
                                    description = notes,
                                    category = category,
                                    dueEpochDay = dueEpochDay,
                                    priority = existing?.priority ?: TaskPriority.MEDIUM,
                                ) { onBack() }
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(FarmSpacing.dp16)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            if (farmId == 0L) {
                FarmStatusIndicator(
                    type = FarmStatusType.ERROR,
                    label =
                    if (useTe) {
                        "ముందు 'నా పొలాలు'లో పొలం కలపండి, ఆ తర్వాతే పని సేవ్ చేయగలరు."
                    } else {
                        "Add a farm in the Farms tab first before adding tasks."
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.cycles.isNotEmpty()) {
                FarmDropdown(
                    label = farmString(R.string.crops_title),
                    options = state.cycles.map { DropdownOption(it.id, it.cropName) },
                    selectedValue = selectedCycleId,
                    onSelect = { selectedCycleId = it.value },
                    placeholder = if (useTe) "పంట ఎంచుకోండి" else "Select Crop",
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                Text(
                    text = if (useTe) "తక్షణ పని రకాలు" else "Quick Activity Presets",
                    style = FarmTypography.labelLarge,
                    color = FarmColors.MainText,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    quickTasks.forEach { (cat, chipLabel, defaultTitle) ->
                        val isSelected = category == cat && title == defaultTitle
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                category = cat
                                title = defaultTitle
                                titleError = null
                            },
                            label = { Text(chipLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmColors.PrimaryContainer,
                                selectedLabelColor = FarmColors.PrimaryForest,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                        )
                    }
                }
            }
            FarmDropdown(
                label = if (useTe) "పని రకం" else "Work Type",
                options = TaskCategory.entries.map { DropdownOption(it, FarmLabels.taskCategory(it, lang)) },
                selectedValue = category,
                onSelect = { category = it.value },
                placeholder = "",
            )
            FarmTextInput(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = if (useTe) "పని పేరు" else "Task title",
                errorText = titleError,
            )
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                FarmDatePicker(
                    selectedDateMillis = viewModel.epochDayToMillis(dueEpochDay),
                    onDateSelected = { millis -> dueEpochDay = viewModel.epochMillisToEpochDay(millis) },
                    label = if (useTe) "చేయవలసిన తేదీ" else "Due Date",
                    formattedDate = viewModel.formatEpochDay(dueEpochDay),
                    todayLabel = farmString(R.string.common_today),
                    yesterdayLabel = farmString(R.string.common_yesterday),
                    showQuickTodayYesterday = false,
                )
                val futurePresets =
                    listOf(
                        0L to (if (useTe) "ఈరోజు" else "Today"),
                        1L to (if (useTe) "రేపు" else "Tomorrow"),
                        3L to (if (useTe) "3 రోజుల్లో" else "In 3 Days"),
                        7L to (if (useTe) "వచ్చే వారం" else "Next Week"),
                    )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    futurePresets.forEach { (offset, label) ->
                        val targetDay = state.todayEpochDay + offset
                        FilterChip(
                            selected = dueEpochDay == targetDay,
                            onClick = { dueEpochDay = targetDay },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmColors.PrimaryContainer,
                                selectedLabelColor = FarmColors.PrimaryForest,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                        )
                    }
                }
            }
            FarmTextInput(
                value = notes,
                onValueChange = { notes = it },
                label = farmString(R.string.common_notes_optional),
                singleLine = false,
            )
        }
    }
}
