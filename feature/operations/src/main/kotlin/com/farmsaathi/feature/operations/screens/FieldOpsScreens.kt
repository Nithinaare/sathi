package com.farmsaathi.feature.operations.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.DropdownOption
import com.farmsaathi.core.designsystem.components.FarmConfirmationDialog
import com.farmsaathi.core.designsystem.components.FarmDatePicker
import com.farmsaathi.core.designsystem.components.FarmDropdown
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmImageGridPicker
import com.farmsaathi.core.designsystem.components.FarmImagePickerOption
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.components.FarmMoreDetailsAccordion
import com.farmsaathi.core.designsystem.components.FarmNumberInput
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPhotoCapture
import com.farmsaathi.core.designsystem.components.FarmPhotoViewer
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmRecordCard
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.components.FarmSectionHeader
import com.farmsaathi.core.designsystem.components.FarmStatusIndicator
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmStickyFooterBar
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.components.FarmTimeSlotPicker
import com.farmsaathi.core.designsystem.components.rememberFarmPhotoLaunchers
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.IrrigationLog
import com.farmsaathi.core.model.IrrigationMethod
import com.farmsaathi.core.model.IrrigationSource
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.SoilHealthRating
import com.farmsaathi.feature.operations.viewmodel.FieldOpsAndContactsViewModel
import java.math.BigDecimal

@Composable
fun IrrigationListScreen(
    plotId: Long?,
    cropCycleId: Long?,
    viewModel: FieldOpsAndContactsViewModel,
    onBack: () -> Unit,
    onAddIrrigation: (Long?, Long?) -> Unit,
    onEditIrrigation: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val logs =
        remember(state.irrigationLogs, plotId, cropCycleId) {
            state.irrigationLogs.filter {
                (plotId == null || it.plotId == plotId) && (cropCycleId == null || it.cropCycleId == cropCycleId)
            }
        }
    var selectedLog by remember { mutableStateOf<IrrigationLog?>(null) }
    var deleteConfirmLog by remember { mutableStateOf<IrrigationLog?>(null) }
    var previewPhoto by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.irr_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.IRRIGATION_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.IRRIGATION_HELP,
                onToggleAudio = viewModel::toggleAudio,
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.irr_add_btn),
                    leadingIcon = Icons.Rounded.Add,
                    onClick = { onAddIrrigation(plotId, cropCycleId) },
                )
            }
        },
    ) { padding ->
        if (logs.isEmpty()) {
            FarmEmptyState(
                title = farmString(R.string.irr_title),
                message =
                if (useTe) {
                    "ఇంకా నీటి తడులు రాయలేదు. కింద ఉన్న బటన్ నొక్కి నమోదు చేయండి."
                } else {
                    "No irrigation logs yet. Tap the button below to add one."
                },
                icon = Icons.Rounded.WaterDrop,
                imageRes = FarmImages.irrigation,
                modifier = Modifier.padding(padding),
            )
        } else {
            val cropsById = remember(state.crops) { state.crops.associateBy { it.id } }
            val plotsById = remember(state.plots) { state.plots.associateBy { it.id } }
            LazyColumn(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(FarmSpacing.dp16),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                items(
                    logs,
                    key = { it.id },
                    contentType = { "irrigation_log_card" },
                ) { log ->
                    val cropName = log.cropCycleId?.let { cid -> cropsById[cid]?.cropName }
                    val cropTitle = cropName ?: plotsById[log.plotId]?.name.orEmpty()
                    val methodLabel = FarmLabels.irrigationMethod(log.method, lang)
                    FarmRecordCard(
                        title = "$cropTitle • ${FarmLabels.irrigationSource(log.source, lang)}",
                        subtitle = "${viewModel.formatDateTime(log.irrigationDateEpochMillis)} • $methodLabel",
                        amountOrMetric = log.durationMinutes?.let { if (useTe) "$it నిమి." else "$it min" },
                        hasPhoto = log.photoPath != null,
                        leadingIcon = Icons.Rounded.WaterDrop,
                        leadingImageRes =
                        cropName?.let(FarmImages::forCropName)
                            ?: FarmImages.forIrrigationSource(log.source),
                        onClick = { selectedLog = log },
                    )
                }
            }
        }
    }

    selectedLog?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedLog = null },
            title = { Text(FarmLabels.irrigationSource(log.source, lang), style = FarmTypography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                    Text(
                        text = "${viewModel.formatDateTime(log.irrigationDateEpochMillis)} • ${FarmLabels.irrigationMethod(log.method, lang)}",
                        style = FarmTypography.bodyMedium,
                        color = FarmColors.SecondaryText,
                    )
                    log.nextWateringEpochDay?.let { nextDay ->
                        Text(
                            text =
                            if (useTe) {
                                "తదుపరి తడి: ${viewModel.formatEpochDay(nextDay)}"
                            } else {
                                "Next watering: ${viewModel.formatEpochDay(nextDay)}"
                            },
                            style = FarmTypography.bodyMedium,
                            color = FarmColors.PrimaryForest,
                        )
                    }
                    log.photoPath?.let { path ->
                        FarmSecondaryButton(
                            text = if (useTe) "ఫోటో చూడండి" else "View Photo",
                            leadingIcon = Icons.Rounded.PhotoCamera,
                            useMutedOutline = true,
                            onClick = {
                                selectedLog = null
                                previewPhoto = path
                            },
                        )
                    }
                    FarmSecondaryButton(
                        text = farmString(R.string.common_edit),
                        leadingIcon = Icons.Rounded.Edit,
                        onClick = {
                            selectedLog = null
                            onEditIrrigation(log.id)
                        },
                    )
                    FarmSecondaryButton(
                        text = if (useTe) "రికార్డు తొలగించు" else "Delete Log",
                        leadingIcon = Icons.Rounded.Delete,
                        useMutedOutline = true,
                        onClick = {
                            selectedLog = null
                            deleteConfirmLog = log
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLog = null }) {
                    Text(farmString(R.string.common_cancel))
                }
            },
        )
    }

    deleteConfirmLog?.let { log ->
        FarmConfirmationDialog(
            title = if (useTe) "నీటి తడి రికార్డు తీసేయాలా?" else "Delete irrigation log?",
            message = viewModel.formatDateTime(log.irrigationDateEpochMillis),
            confirmLabel = farmString(R.string.common_confirm),
            dismissLabel = farmString(R.string.common_cancel),
            isDestructive = true,
            onConfirm = {
                viewModel.archiveIrrigationLog(log)
                deleteConfirmLog = null
            },
            onDismiss = { deleteConfirmLog = null },
        )
    }

    previewPhoto?.let { path ->
        FarmPhotoViewer(photoFile = viewModel.resolvePhotoFile(path), onDismiss = { previewPhoto = null })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IrrigationFormScreen(
    initialPlotId: Long?,
    initialCropCycleId: Long?,
    viewModel: FieldOpsAndContactsViewModel,
    onBack: () -> Unit,
    initialPhotoPath: String? = null,
    initialDateMillis: Long? = null,
    logId: Long? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = remember(logId, state.irrigationLogs) { logId?.let { id -> state.irrigationLogs.firstOrNull { it.id == id } } }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var selectedCropId by remember(existing, initialCropCycleId, state.crops) {
        mutableStateOf(existing?.cropCycleId ?: initialCropCycleId ?: state.crops.firstOrNull()?.id)
    }
    var selectedPlotId by remember(existing, initialPlotId, state.plots) {
        mutableStateOf(existing?.plotId ?: initialPlotId ?: state.plots.firstOrNull()?.id)
    }
    var source by remember(existing) {
        mutableStateOf(
            when (existing?.source) {
                IrrigationSource.DRIP -> IrrigationSource.BOREWELL
                null -> IrrigationSource.BOREWELL
                else -> existing.source
            },
        )
    }
    var method by remember(existing) { mutableStateOf(existing?.method ?: IrrigationMethod.FLOOD) }
    var dateMillis by remember(existing, initialDateMillis) {
        mutableLongStateOf(existing?.irrigationDateEpochMillis ?: initialDateMillis ?: System.currentTimeMillis())
    }
    var nextReminderDays by remember(existing) {
        mutableStateOf(
            existing?.nextWateringEpochDay?.let { next ->
                val baseDay = viewModel.epochMillisToDay(existing.irrigationDateEpochMillis)
                (next - baseDay).toInt().takeIf { it > 0 }
            },
        )
    }
    var exifDetected by remember(existing, initialDateMillis) {
        mutableStateOf(existing == null && initialDateMillis != null)
    }
    var photoPath by remember(existing, initialPhotoPath) { mutableStateOf(existing?.photoPath ?: initialPhotoPath) }
    var minsStr by remember(existing) { mutableStateOf(existing?.durationMinutes?.toString().orEmpty()) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var showMore by remember { mutableStateOf(false) }
    var previewPhoto by remember { mutableStateOf<String?>(null) }

    val selectedPlot = state.plots.firstOrNull { it.id == selectedPlotId }
    val photoLaunchers =
        rememberFarmPhotoLaunchers(
            createTempFile = viewModel::createTempCaptureFile,
            onCapturedFile = { f ->
                viewModel.capturePhotoWithExif(f) { rel, exifTs ->
                    photoPath = rel
                    if (exifTs != null) {
                        dateMillis = exifTs
                        exifDetected = true
                    }
                }
            },
            onGalleryUri = { uri ->
                viewModel.importGalleryWithExif(uri) { rel, exifTs ->
                    photoPath = rel
                    if (exifTs != null) {
                        dateMillis = exifTs
                        exifDetected = true
                    }
                }
            },
        )

    val cropOptions =
        remember(state.crops, useTe) {
            state.crops.map { crop ->
                FarmImagePickerOption<Long?>(
                    value = crop.id,
                    label = crop.cropName,
                    imageRes = FarmImages.forCropName(crop.cropName),
                )
            } +
                FarmImagePickerOption<Long?>(
                    value = null,
                    label = if (useTe) "మా పొలం" else "My Farm",
                    imageRes = FarmImages.farmField,
                )
        }

    val sourceOptions =
        remember(lang) {
            listOf(
                IrrigationSource.BOREWELL,
                IrrigationSource.CANAL,
                IrrigationSource.OPEN_WELL,
                IrrigationSource.TANK,
            ).map { src ->
                FarmImagePickerOption(
                    value = src,
                    label = FarmLabels.irrigationSource(src, lang),
                    imageRes = FarmImages.forIrrigationSource(src),
                )
            }
        }

    val methodOptions =
        remember(lang) {
            listOf(
                IrrigationMethod.FLOOD to FarmImages.irrigation,
                IrrigationMethod.DRIP to FarmImages.forIrrigationSource(IrrigationSource.DRIP),
                IrrigationMethod.SPRINKLER to FarmImages.irrigation,
            ).map { (m, img) ->
                FarmImagePickerOption(
                    value = m,
                    label = FarmLabels.irrigationMethod(m, lang),
                    imageRes = img,
                )
            }
        }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (existing == null) farmString(R.string.irr_add_btn) else farmString(R.string.common_edit),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.WATERING_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.WATERING_FORM_HELP,
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
                        val baseDay = viewModel.epochMillisToDay(dateMillis)
                        val computedNextDay = nextReminderDays?.let { baseDay + it.toLong() }
                        viewModel.saveIrrigationQuick(
                            selectedPlot = selectedPlot,
                            cropCycleId = selectedCropId,
                            dateMillis = dateMillis,
                            durationMins = minsStr.toIntOrNull(),
                            source = source,
                            method = method,
                            nextDay = computedNextDay,
                            photoPath = photoPath,
                            notes = notes,
                            existingId = existing?.id,
                        ) { onBack() }
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
            FarmImageGridPicker(
                label = if (useTe) "1. ఏ పంటకు నీరు పెట్టారు?" else "1. Select Crop",
                options = cropOptions,
                selected = selectedCropId,
                onSelect = { selectedCropId = it },
                columns = 3,
            )

            FarmPhotoCapture(
                photoPaths = listOfNotNull(photoPath),
                onTakePhotoClick = photoLaunchers.launchCamera,
                onPickFromGalleryClick = photoLaunchers.launchGallery,
                onRemovePhotoClick = {
                    photoPath = null
                    exifDetected = false
                },
                onPhotoClick = { previewPhoto = it },
                title = if (useTe) "2. ఫోటో తీస్తే సమయం ఆటోగా వస్తుంది" else "2. Snap photo (auto date & time)",
                cameraButtonLabel = farmString(R.string.diary_take_photo),
                galleryButtonLabel = farmString(R.string.diary_pick_gallery),
                resolveFile = viewModel::resolvePhotoFile,
                maxPhotos = 1,
            )

            if (exifDetected) {
                Surface(
                    shape = FarmShapeTokens.inputField,
                    color = FarmColors.PrimaryContainer,
                    border = BorderStroke(1.dp, FarmColors.PrimaryForest.copy(alpha = 0.35f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(FarmSpacing.dp12),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = FarmColors.PrimaryForest,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text =
                            if (useTe) {
                                "ఫోటో సమయం (ఆటో): ${viewModel.formatDateTime(dateMillis)}"
                            } else {
                                "Photo EXIF time: ${viewModel.formatDateTime(dateMillis)}"
                            },
                            style = FarmTypography.bodyMedium,
                            color = FarmColors.OnPrimaryContainer,
                        )
                    }
                }
            }

            FarmImageGridPicker(
                label = if (useTe) "3. నీటి వనరు" else "3. Water Source",
                options = sourceOptions,
                selected = source,
                onSelect = { source = it },
                columns = 2,
            )

            FarmImageGridPicker(
                label = if (useTe) "4. పారుదల విధానం" else "4. Irrigation Method",
                options = methodOptions,
                selected = method,
                onSelect = { method = it },
                columns = 3,
            )

            FarmTimeSlotPicker(
                label = if (useTe) "నీరు పెట్టిన సమయం" else "Time Slot",
                epochMillis = dateMillis,
                onEpochMillisChange = {
                    dateMillis = it
                    exifDetected = false
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                Text(
                    text = if (useTe) "తదుపరి తడి రిమైండర్ (ఐచ్ఛికం)" else "Next Watering Reminder",
                    style = FarmTypography.labelLarge,
                    color = FarmColors.MainText,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    listOf(3, 5, 7).forEach { days ->
                        val isSelected = nextReminderDays == days
                        FilterChip(
                            selected = isSelected,
                            onClick = { nextReminderDays = if (isSelected) null else days },
                            label = { Text(if (useTe) "$days రోజుల్లో" else "In $days Days") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmColors.PrimaryContainer,
                                selectedLabelColor = FarmColors.PrimaryForest,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                        )
                    }
                }
                nextReminderDays?.let { days ->
                    val nextDay = viewModel.epochMillisToDay(dateMillis) + days.toLong()
                    Text(
                        text =
                        if (useTe) {
                            "రిమైండర్ తేదీ: ${viewModel.formatEpochDay(nextDay)}"
                        } else {
                            "Reminder date: ${viewModel.formatEpochDay(nextDay)}"
                        },
                        style = FarmTypography.bodyMedium,
                        color = FarmColors.PrimaryForest,
                    )
                }
            }

            FarmMoreDetailsAccordion(
                expanded = showMore,
                onToggle = { showMore = !showMore },
                useTelugu = useTe,
            ) {
                if (state.plots.isNotEmpty()) {
                    FarmDropdown(
                        label = farmString(R.string.plots_title),
                        options = state.plots.map { DropdownOption(it.id, it.name) },
                        selectedValue = selectedPlotId,
                        onSelect = { selectedPlotId = it.value },
                        placeholder = if (useTe) "మడి ఎంచుకోండి" else "Select Plot",
                    )
                }
                FarmNumberInput(
                    value = minsStr,
                    onValueChange = { minsStr = it },
                    label = if (useTe) "మోటారు నిమిషాలు (ఐచ్ఛికం)" else "Duration Minutes (optional)",
                    allowDecimal = false,
                )
                FarmDatePicker(
                    selectedDateMillis = dateMillis,
                    onDateSelected = {
                        dateMillis = it
                        exifDetected = false
                    },
                    label = if (useTe) "తేదీ మార్చండి" else "Change Date",
                    formattedDate = viewModel.formatDate(dateMillis),
                )
                FarmTextInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = farmString(R.string.common_notes_optional),
                )
            }
        }
    }

    previewPhoto?.let { path ->
        FarmPhotoViewer(photoFile = viewModel.resolvePhotoFile(path), onDismiss = { previewPhoto = null })
    }
}

@Composable
fun HarvestAndStorageListScreen(
    cropCycleId: Long?,
    viewModel: FieldOpsAndContactsViewModel,
    onBack: () -> Unit,
    onAddHarvest: (Long?) -> Unit,
    onAddCrop: () -> Unit = {},
    onEditHarvest: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val harvests =
        remember(state.harvestRecords, cropCycleId) {
            if (cropCycleId == null) state.harvestRecords else state.harvestRecords.filter { it.cropCycleId == cropCycleId }
        }
    val storages =
        remember(state.storageRecords, cropCycleId) {
            if (cropCycleId == null) state.storageRecords else state.storageRecords.filter { it.cropCycleId == cropCycleId }
        }
    var selectedStorageInfo by remember { mutableStateOf<Pair<String, String>?>(null) }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.harv_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.HARVEST_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.HARVEST_HELP,
                onToggleAudio = viewModel::toggleAudio,
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                if (state.crops.isEmpty()) {
                    FarmPrimaryButton(
                        text = if (useTe) "ముందు పంట కలపండి" else "Add Crop First",
                        leadingIcon = Icons.Rounded.Add,
                        onClick = onAddCrop,
                    )
                } else {
                    FarmPrimaryButton(
                        text = farmString(R.string.harv_add_btn),
                        leadingIcon = Icons.Rounded.Add,
                        onClick = { onAddHarvest(cropCycleId) },
                    )
                }
            }
        },
    ) { padding ->
        if (harvests.isEmpty() && storages.isEmpty()) {
            FarmEmptyState(
                title = farmString(R.string.harv_title),
                message =
                if (state.crops.isEmpty()) {
                    if (useTe) {
                        "కోత నమోదు చేయాలంటే ముందు ఒక పంట కలపండి."
                    } else {
                        "Add a crop first before logging harvest."
                    }
                } else if (useTe) {
                    "కోత తర్వాత దిగుబడి, నిల్వ స్థలం ఇక్కడ రాసుకోవచ్చు."
                } else {
                    "No harvest records yet. Tap below to add one."
                },
                icon = Icons.Rounded.Warehouse,
                imageRes = FarmImages.harvest,
                modifier = Modifier.padding(padding),
            )
        } else {
            val cropsById = remember(state.crops) { state.crops.associateBy { it.id } }
            LazyColumn(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(FarmSpacing.dp16),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                if (storages.isNotEmpty()) {
                    item(key = "storage_lots_header", contentType = "section_header") {
                        FarmSectionHeader(title = if (useTe) "నిల్వలో ఉన్న పంట" else "Storage Lots")
                    }
                    items(
                        storages,
                        key = { "st_${it.id}" },
                        contentType = { "storage_lot_card" },
                    ) { st ->
                        val qtyText = FarmLabels.formatQuantity(st.remainingQuantityMilliUnits, st.quantityUnit, lang)
                        FarmRecordCard(
                            title = st.storageLocation,
                            subtitle = viewModel.formatEpochDay(st.storedDateEpochDay),
                            amountOrMetric = qtyText,
                            statusType = FarmStatusType.INFO,
                            statusLabel = FarmLabels.storageStatus(st.status, lang),
                            leadingIcon = Icons.Rounded.Warehouse,
                            leadingImageRes = FarmImages.harvest,
                            onClick = {
                                val linkedHarvestId = st.harvestRecordId
                                if (linkedHarvestId != null) {
                                    onEditHarvest(linkedHarvestId)
                                } else {
                                    selectedStorageInfo = st.storageLocation to qtyText
                                }
                            },
                        )
                    }
                }
                item(key = "harvest_history_header", contentType = "section_header") {
                    FarmSectionHeader(title = if (useTe) "కోత రికార్డులు" else "Harvest History")
                }
                items(
                    harvests,
                    key = { "hr_${it.id}" },
                    contentType = { "harvest_record_card" },
                ) { h ->
                    val cropName = cropsById[h.cropCycleId]?.cropName.orEmpty()
                    FarmRecordCard(
                        title = cropName,
                        subtitle = viewModel.formatEpochDay(h.harvestDateEpochDay),
                        amountOrMetric = FarmLabels.formatQuantity(h.quantityMilliUnits, h.quantityUnit, lang),
                        metadata = h.qualityGrade,
                        hasPhoto = h.photoPath != null,
                        leadingIcon = Icons.Rounded.Warehouse,
                        leadingImageRes = FarmImages.forCropName(cropName),
                        onClick = { onEditHarvest(h.id) },
                    )
                }
            }
        }
    }

    selectedStorageInfo?.let { (loc, qty) ->
        AlertDialog(
            onDismissRequest = { selectedStorageInfo = null },
            title = { Text(loc, style = FarmTypography.titleMedium) },
            text = { Text(qty, style = FarmTypography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = { selectedStorageInfo = null }) {
                    Text(farmString(R.string.common_confirm))
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HarvestFormScreen(
    initialCropCycleId: Long?,
    viewModel: FieldOpsAndContactsViewModel,
    onBack: () -> Unit,
    harvestId: Long? = null,
    initialPhotoPath: String? = null,
    initialDateMillis: Long? = null,
    onAddCrop: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing =
        remember(harvestId, state.harvestRecords) {
            harvestId?.let { id -> state.harvestRecords.firstOrNull { it.id == id } }
        }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var selectedCropId by remember(existing, initialCropCycleId, state.crops) {
        mutableStateOf(existing?.cropCycleId ?: initialCropCycleId ?: state.crops.firstOrNull()?.id)
    }
    var harvestDay by remember(existing, initialDateMillis, state.todayEpochDay) {
        mutableLongStateOf(
            existing?.harvestDateEpochDay
                ?: initialDateMillis?.let { viewModel.epochMillisToDay(it) }
                ?: state.todayEpochDay,
        )
    }
    var qtyStr by remember(existing) {
        mutableStateOf(
            existing?.let {
                BigDecimal.valueOf(it.quantityMilliUnits).divide(BigDecimal(1_000)).stripTrailingZeros().toPlainString()
            }.orEmpty(),
        )
    }
    var unit by remember(existing) { mutableStateOf(existing?.quantityUnit ?: QuantityUnit.QUINTAL) }
    var bagsStr by remember(existing) { mutableStateOf(existing?.bagCount?.toString().orEmpty()) }
    var grade by remember(existing) { mutableStateOf(existing?.qualityGrade.orEmpty()) }
    var storageLoc by remember(existing) { mutableStateOf("") }
    var photoPath by remember(existing, initialPhotoPath) { mutableStateOf(existing?.photoPath ?: initialPhotoPath) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var errMsg by remember { mutableStateOf<String?>(null) }
    var previewPhoto by remember { mutableStateOf<String?>(null) }

    val selectedCrop = state.crops.firstOrNull { it.id == selectedCropId }
    val photoLaunchers =
        rememberFarmPhotoLaunchers(
            createTempFile = viewModel::createTempCaptureFile,
            onCapturedFile = { f -> viewModel.saveCapturedPhoto(f) { photoPath = it } },
            onGalleryUri = { uri -> viewModel.importGalleryPhoto(uri) { photoPath = it } },
        )

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (existing == null) farmString(R.string.harv_add_btn) else farmString(R.string.common_edit),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.HARVEST_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.HARVEST_FORM_HELP,
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
                        val crop = selectedCrop
                        val qtyMilli = qtyStr.toBigDecimalOrNull()?.multiply(BigDecimal(1_000))?.toLong() ?: 0L
                        if (crop == null || qtyMilli <= 0L) {
                            errMsg = if (useTe) "పంట, దిగుబడి సరిగా రాయండి" else "Select crop & valid quantity"
                            viewModel.playErrorCue()
                        } else {
                            errMsg = null
                            viewModel.saveHarvestAndOptionalStorage(
                                cycle = crop,
                                harvestDay = harvestDay,
                                qtyMilli = qtyMilli,
                                unit = unit,
                                bagCount = bagsStr.toIntOrNull(),
                                qualityGrade = grade,
                                storageLocation = storageLoc,
                                notes = notes,
                                existingId = existing?.id,
                                photoPath = photoPath,
                            ) { onBack() }
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
            if (state.crops.isEmpty()) {
                FarmStatusIndicator(
                    type = FarmStatusType.WARNING,
                    label =
                    if (useTe) {
                        "కోత నమోదు చేయాలంటే ముందు ఒక పంట కలపండి."
                    } else {
                        "Please add a crop before recording harvest."
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                FarmSecondaryButton(
                    text = if (useTe) "ముందు పంట కలపండి" else "Add Crop First",
                    leadingIcon = Icons.Rounded.Add,
                    onClick = onAddCrop,
                )
            } else {
                FarmDropdown(
                    label = farmString(R.string.crops_title),
                    options = state.crops.map { DropdownOption(it.id, it.cropName) },
                    selectedValue = selectedCropId,
                    onSelect = {
                        selectedCropId = it.value
                        errMsg = null
                    },
                    placeholder = if (useTe) "పంట ఎంచుకోండి" else "Select Crop",
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                verticalAlignment = Alignment.Top,
            ) {
                FarmNumberInput(
                    value = qtyStr,
                    onValueChange = {
                        qtyStr = it
                        errMsg = null
                    },
                    label = if (useTe) "దిగుబడి" else "Quantity",
                    modifier = Modifier.weight(1f),
                )
                FarmDropdown(
                    label = if (useTe) "కొలత" else "Unit",
                    options =
                    listOf(QuantityUnit.QUINTAL, QuantityUnit.BAG, QuantityUnit.KG, QuantityUnit.TON)
                        .map { DropdownOption(it, FarmLabels.quantityUnit(it, lang)) },
                    selectedValue = unit,
                    onSelect = { unit = it.value },
                    placeholder = "",
                    modifier = Modifier.weight(1f),
                )
            }
            FarmPhotoCapture(
                photoPaths = listOfNotNull(photoPath),
                onTakePhotoClick = photoLaunchers.launchCamera,
                onPickFromGalleryClick = photoLaunchers.launchGallery,
                onRemovePhotoClick = { photoPath = null },
                onPhotoClick = { previewPhoto = it },
                title = if (useTe) "కాటా చీటీ / కోత ఫోటో (ఐచ్ఛికం)" else "Weighing Slip / Harvest Photo (optional)",
                cameraButtonLabel = farmString(R.string.diary_take_photo),
                galleryButtonLabel = farmString(R.string.diary_pick_gallery),
                resolveFile = viewModel::resolvePhotoFile,
                maxPhotos = 1,
            )
            FarmNumberInput(
                value = bagsStr,
                onValueChange = { bagsStr = it },
                label = if (useTe) "బస్తాల సంఖ్య (ఐచ్ఛికం)" else "Bag Count (optional)",
                allowDecimal = false,
            )
            if (existing == null) {
                val storagePresets =
                    if (useTe) {
                        listOf("ఇంటి గది", "మార్కెట్ యార్డు", "కోల్డ్ స్టోరేజ్")
                    } else {
                        listOf("Home Shed", "Market Yard", "Cold Storage")
                    }
                Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                    Text(
                        text = if (useTe) "నిల్వ చేసిన స్థలం (ఐచ్ఛికం)" else "Storage Location (optional)",
                        style = FarmTypography.labelLarge,
                        color = FarmColors.MainText,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    ) {
                        storagePresets.forEach { preset ->
                            FilterChip(
                                selected = storageLoc == preset,
                                onClick = { storageLoc = if (storageLoc == preset) "" else preset },
                                label = { Text(preset) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FarmColors.PrimaryContainer,
                                    selectedLabelColor = FarmColors.PrimaryForest,
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                            )
                        }
                    }
                    FarmTextInput(
                        value = storageLoc,
                        onValueChange = { storageLoc = it },
                        label = if (useTe) "వేరే స్థలం ఉంటే రాయండి" else "Or enter custom storage location",
                    )
                }
            }
            FarmTextInput(
                value = grade,
                onValueChange = { grade = it },
                label = if (useTe) "నాణ్యత / గ్రేడ్ (ఐచ్ఛికం)" else "Quality / Grade (optional)",
            )
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                FarmDatePicker(
                    selectedDateMillis = viewModel.epochDayToMillis(harvestDay),
                    onDateSelected = { harvestDay = viewModel.epochMillisToDay(it) },
                    label = if (useTe) "కోత తేదీ" else "Harvest Date",
                    formattedDate = viewModel.formatEpochDay(harvestDay),
                    showQuickTodayYesterday = false,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    val todayDay = state.todayEpochDay
                    listOf(
                        todayDay to farmString(R.string.common_today),
                        (todayDay - 1L) to farmString(R.string.common_yesterday),
                    ).forEach { (day, label) ->
                        FilterChip(
                            selected = harvestDay == day,
                            onClick = { harvestDay = day },
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
            )
            errMsg?.let { Text(it, color = FarmColors.Error, style = FarmTypography.bodyMedium) }
        }
    }

    previewPhoto?.let { path ->
        FarmPhotoViewer(photoFile = viewModel.resolvePhotoFile(path), onDismiss = { previewPhoto = null })
    }
}

@Composable
fun SoilTestListScreen(
    plotId: Long?,
    viewModel: FieldOpsAndContactsViewModel,
    onBack: () -> Unit,
    onAddSoilTest: (Long?) -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val tests =
        remember(state.soilTests, plotId) {
            if (plotId == null) state.soilTests else state.soilTests.filter { it.plotId == plotId }
        }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.soil_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.SOIL_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.SOIL_HELP,
                onToggleAudio = viewModel::toggleAudio,
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.soil_add_btn),
                    leadingIcon = Icons.Rounded.Add,
                    onClick = { onAddSoilTest(plotId) },
                )
            }
        },
    ) { padding ->
        if (tests.isEmpty()) {
            FarmEmptyState(
                title = farmString(R.string.soil_title),
                message =
                if (useTe) {
                    "మట్టి కార్డు ఫోటో లేదా పరీక్ష విలువలు దాచుకోవచ్చు. కింద ఉన్న బటన్ నొక్కండి."
                } else {
                    "No soil test records yet. Tap below to add one."
                },
                icon = Icons.Rounded.Science,
                imageRes = FarmImages.soilTest,
                modifier = Modifier.padding(padding),
            )
        } else {
            val plotsById = remember(state.plots) { state.plots.associateBy { it.id } }
            LazyColumn(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(FarmSpacing.dp16),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                items(
                    tests,
                    key = { it.id },
                    contentType = { "soil_test_card" },
                ) { item ->
                    val plotName = plotsById[item.plotId]?.name.orEmpty()
                    val phLabel = item.phTenths?.let { "pH ${it / 10.0}" }
                    FarmRecordCard(
                        title = "$plotName • ${viewModel.formatEpochDay(item.testDateEpochDay)}",
                        subtitle = listOfNotNull(item.labName, phLabel).joinToString(" • "),
                        statusType =
                        when (item.overallRating) {
                            SoilHealthRating.OPTIMAL -> FarmStatusType.SUCCESS
                            SoilHealthRating.MEDIUM, SoilHealthRating.HIGH -> FarmStatusType.WARNING
                            SoilHealthRating.LOW, SoilHealthRating.SALINE_ALKALINE -> FarmStatusType.ERROR
                            null -> FarmStatusType.INFO
                        },
                        statusLabel = item.overallRating?.let { FarmLabels.soilHealthRating(it, lang) },
                        hasPhoto = item.reportPhotoPath != null,
                        leadingIcon = Icons.Rounded.Science,
                        leadingImageRes = FarmImages.soilTest,
                        onClick = { onOpenDetail(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun SoilTestDetailScreen(
    testId: Long,
    viewModel: FieldOpsAndContactsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rec = state.soilTests.firstOrNull { it.id == testId }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    var previewPhoto by remember { mutableStateOf<String?>(null) }

    if (rec == null) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.soil_title),
                subtitle = viewModel.formatEpochDay(rec.testDateEpochDay),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
            )
        },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(FarmSpacing.dp16)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            rec.overallRating?.let {
                FarmStatusIndicator(
                    type =
                    when (it) {
                        SoilHealthRating.OPTIMAL -> FarmStatusType.SUCCESS
                        SoilHealthRating.MEDIUM, SoilHealthRating.HIGH -> FarmStatusType.WARNING
                        SoilHealthRating.LOW, SoilHealthRating.SALINE_ALKALINE -> FarmStatusType.ERROR
                    },
                    label = FarmLabels.soilHealthRating(it, lang),
                )
            }
            rec.labName?.let { Text(it, style = FarmTypography.titleMedium, color = FarmColors.MainText) }
            rec.phTenths?.let { Text("pH: ${it / 10.0}", style = FarmTypography.bodyLarge) }
            rec.nitrogenKgPerHaTenths?.let { Text("N: ${it / 10.0}", style = FarmTypography.bodyLarge) }
            rec.phosphorusKgPerHaTenths?.let { Text("P: ${it / 10.0}", style = FarmTypography.bodyLarge) }
            rec.potassiumKgPerHaTenths?.let { Text("K: ${it / 10.0}", style = FarmTypography.bodyLarge) }
            rec.recommendationsNote?.let { Text(it, style = FarmTypography.bodyLarge) }
            rec.reportPhotoPath?.let { path ->
                FarmSecondaryButton(
                    text = if (useTe) "రిపోర్ట్ ఫోటో చూడండి" else "View Report Photo",
                    leadingIcon = Icons.Rounded.PhotoCamera,
                    onClick = { previewPhoto = path },
                )
            }
        }
    }

    previewPhoto?.let { p ->
        FarmPhotoViewer(photoFile = viewModel.resolvePhotoFile(p), onDismiss = { previewPhoto = null })
    }
}

@Composable
fun SoilTestFormScreen(
    initialPlotId: Long?,
    viewModel: FieldOpsAndContactsViewModel,
    onBack: () -> Unit,
    testId: Long? = null,
    initialPhotoPath: String? = null,
    initialDateMillis: Long? = null,
    onAddPlot: (Long) -> Unit = { onBack() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = remember(testId, state.soilTests) { testId?.let { id -> state.soilTests.firstOrNull { it.id == id } } }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var selectedPlotId by remember(existing, initialPlotId, state.plots) {
        mutableStateOf(existing?.plotId ?: initialPlotId ?: state.plots.firstOrNull()?.id)
    }
    var testDay by remember(existing, initialDateMillis, state.todayEpochDay) {
        mutableLongStateOf(
            existing?.testDateEpochDay
                ?: initialDateMillis?.let { viewModel.epochMillisToDay(it) }
                ?: state.todayEpochDay,
        )
    }
    var labName by remember(existing) { mutableStateOf(existing?.labName.orEmpty()) }
    var phStr by remember(existing) { mutableStateOf(existing?.phTenths?.let { (it / 10.0).toString() }.orEmpty()) }
    var nStr by remember(existing) { mutableStateOf(existing?.nitrogenKgPerHaTenths?.let { (it / 10.0).toString() }.orEmpty()) }
    var pStr by remember(existing) { mutableStateOf(existing?.phosphorusKgPerHaTenths?.let { (it / 10.0).toString() }.orEmpty()) }
    var kStr by remember(existing) { mutableStateOf(existing?.potassiumKgPerHaTenths?.let { (it / 10.0).toString() }.orEmpty()) }
    var rating by remember(existing) { mutableStateOf(existing?.overallRating) }
    var photoPath by remember(existing, initialPhotoPath) { mutableStateOf(existing?.reportPhotoPath ?: initialPhotoPath) }
    var notes by remember(existing) { mutableStateOf(existing?.recommendationsNote.orEmpty()) }
    var showLabDetails by remember { mutableStateOf(false) }
    var errMsg by remember { mutableStateOf<String?>(null) }
    var previewPhoto by remember { mutableStateOf<String?>(null) }
    val selectedPlot = state.plots.firstOrNull { it.id == selectedPlotId }
    val firstFarmId = state.farms.firstOrNull()?.id ?: state.prefs.activeFarmId ?: 0L

    val photoLaunchers =
        rememberFarmPhotoLaunchers(
            createTempFile = viewModel::createTempCaptureFile,
            onCapturedFile = { f -> viewModel.saveCapturedPhoto(f) { photoPath = it } },
            onGalleryUri = { uri -> viewModel.importGalleryPhoto(uri) { photoPath = it } },
        )

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (existing == null) farmString(R.string.soil_add_btn) else farmString(R.string.common_edit),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.SOIL_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.SOIL_FORM_HELP,
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
                        val plot = selectedPlot
                        if (plot == null) {
                            errMsg =
                                if (useTe) {
                                    "ముందు కనీసం ఒక మడి కలపండి"
                                } else {
                                    "Add at least one plot first"
                                }
                            viewModel.playErrorCue()
                        } else {
                            errMsg = null
                            fun toTenths(s: String): Int? = s.toDoubleOrNull()?.let { Math.round(it * 10.0).toInt() }
                            viewModel.saveSoilTest(
                                plot = plot,
                                testDay = testDay,
                                labName = labName,
                                phTenths = toTenths(phStr),
                                nitrogenTenths = toTenths(nStr),
                                phosphorusTenths = toTenths(pStr),
                                potassiumTenths = toTenths(kStr),
                                rating = rating,
                                photoPath = photoPath,
                                notes = notes,
                                existingId = existing?.id,
                            ) { onBack() }
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
            if (state.plots.isEmpty()) {
                FarmStatusIndicator(
                    type = FarmStatusType.ERROR,
                    label =
                    if (useTe) {
                        "ముందు 'నా పొలాలు'లో పొలం తెరిచి కనీసం ఒక మడి కలపండి."
                    } else {
                        "Add at least one plot under a farm first."
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                FarmSecondaryButton(
                    text = if (useTe) "ముందు మడి కలపండి" else "Add Plot First",
                    leadingIcon = Icons.Rounded.Add,
                    onClick = { onAddPlot(firstFarmId) },
                )
            } else {
                FarmDropdown(
                    label = farmString(R.string.plots_title),
                    options = state.plots.map { DropdownOption(it.id, it.name) },
                    selectedValue = selectedPlotId,
                    onSelect = {
                        selectedPlotId = it.value
                        errMsg = null
                    },
                    placeholder = if (useTe) "మడి ఎంచుకోండి" else "Select Plot",
                )
            }
            FarmPhotoCapture(
                photoPaths = listOfNotNull(photoPath),
                onTakePhotoClick = photoLaunchers.launchCamera,
                onPickFromGalleryClick = photoLaunchers.launchGallery,
                onRemovePhotoClick = { photoPath = null },
                onPhotoClick = { previewPhoto = it },
                title = if (useTe) "1. మట్టి కార్డు ఫోటో తీయండి" else "1. Soil Health Card Photo",
                cameraButtonLabel = farmString(R.string.diary_take_photo),
                galleryButtonLabel = farmString(R.string.diary_pick_gallery),
                resolveFile = viewModel::resolvePhotoFile,
                maxPhotos = 1,
            )
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                Text(
                    text = if (useTe) "2. మట్టి ఆరోగ్యం ఎలా ఉంది?" else "2. Soil Health Rating",
                    style = FarmTypography.labelLarge,
                    color = FarmColors.MainText,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    val cards =
                        listOf(
                            Triple(
                                SoilHealthRating.OPTIMAL,
                                if (useTe) "బాగుంది" else "Good",
                                FarmColors.Success to FarmColors.SuccessContainer,
                            ),
                            Triple(
                                SoilHealthRating.MEDIUM,
                                if (useTe) "మధ్యస్థం" else "Medium",
                                FarmColors.Warning to FarmColors.WarningContainer,
                            ),
                            Triple(
                                SoilHealthRating.LOW,
                                if (useTe) "బలహీనం" else "Weak",
                                FarmColors.Error to FarmColors.ErrorContainer,
                            ),
                        )
                    cards.forEach { (option, label, colors) ->
                        val isSelected = rating == option
                        val (accentColor, containerColor) = colors
                        Surface(
                            shape = FarmShapeTokens.card,
                            color = if (isSelected) containerColor else FarmColors.Surface,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) accentColor else FarmColors.Outline),
                            modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .defaultMinSize(minHeight = 64.dp)
                                .semantics { this.selected = isSelected }
                                .clickable { rating = if (isSelected) null else option },
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(FarmSpacing.dp12),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isSelected) accentColor else FarmColors.SecondaryText,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.height(FarmSpacing.dp4))
                                Text(
                                    text = label,
                                    style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) accentColor else FarmColors.MainText,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
            FarmDatePicker(
                selectedDateMillis = viewModel.epochDayToMillis(testDay),
                onDateSelected = { testDay = viewModel.epochMillisToDay(it) },
                label = if (useTe) "పరీక్ష తేదీ" else "Test Date",
                formattedDate = viewModel.formatEpochDay(testDay),
            )
            FarmMoreDetailsAccordion(
                expanded = showLabDetails,
                onToggle = { showLabDetails = !showLabDetails },
                useTelugu = useTe,
                title = if (useTe) "ల్యాబ్ అంకెలు రాయాలంటే (ఐచ్ఛికం)" else "Enter Lab Numbers (Optional)",
            ) {
                FarmTextInput(
                    value = labName,
                    onValueChange = { labName = it },
                    label = if (useTe) "ల్యాబ్ / కేంద్రం పేరు" else "Lab Name",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    FarmNumberInput(value = phStr, onValueChange = { phStr = it }, label = "pH", modifier = Modifier.weight(1f))
                    FarmNumberInput(value = nStr, onValueChange = { nStr = it }, label = "N", modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    FarmNumberInput(value = pStr, onValueChange = { pStr = it }, label = "P", modifier = Modifier.weight(1f))
                    FarmNumberInput(value = kStr, onValueChange = { kStr = it }, label = "K", modifier = Modifier.weight(1f))
                }
            }
            FarmTextInput(
                value = notes,
                onValueChange = { notes = it },
                label = farmString(R.string.common_notes_optional),
                singleLine = false,
            )
            errMsg?.let { Text(it, color = FarmColors.Error, style = FarmTypography.bodyMedium) }
        }
    }

    previewPhoto?.let { p ->
        FarmPhotoViewer(photoFile = viewModel.resolvePhotoFile(p), onDismiss = { previewPhoto = null })
    }
}
