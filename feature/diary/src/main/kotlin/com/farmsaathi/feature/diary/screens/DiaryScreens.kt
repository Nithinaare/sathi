package com.farmsaathi.feature.diary.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.DropdownOption
import com.farmsaathi.core.designsystem.components.FarmAudioPlayer
import com.farmsaathi.core.designsystem.components.FarmConfirmationDialog
import com.farmsaathi.core.designsystem.components.FarmDatePicker
import com.farmsaathi.core.designsystem.components.FarmDropdown
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmMoreDetailsAccordion
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPermissionExplanation
import com.farmsaathi.core.designsystem.components.FarmPhotoCapture
import com.farmsaathi.core.designsystem.components.FarmPhotoViewer
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmRecordCard
import com.farmsaathi.core.designsystem.components.FarmSearchField
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.components.FarmStatusIndicator
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmStickyFooterBar
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.components.FarmTimeSlotPicker
import com.farmsaathi.core.designsystem.components.FarmVoiceRecorder
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
import com.farmsaathi.core.model.CropCycle
import com.farmsaathi.core.model.DiaryCategory
import com.farmsaathi.core.model.DiaryEntry
import com.farmsaathi.core.model.Farm
import com.farmsaathi.core.model.Plot
import com.farmsaathi.feature.diary.viewmodel.DiaryUiState
import com.farmsaathi.feature.diary.viewmodel.DiaryViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    cropCycleId: Long?,
    plotId: Long?,
    viewModel: DiaryViewModel,
    onBack: () -> Unit,
    onAddEntry: (Long?, Long?) -> Unit,
    onEditEntry: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val itemsList =
        remember(state.entries, cropCycleId, plotId) {
            state.entries.filter {
                (cropCycleId == null || it.cropCycleId == cropCycleId) &&
                    (plotId == null || it.plotId == plotId)
            }
        }
    var selectedEntryId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteEntry by remember { mutableStateOf<DiaryEntry?>(null) }
    var previewPhoto by remember { mutableStateOf<Pair<String, String>?>(null) }

    val selectedEntry = selectedEntryId?.let { id -> itemsList.firstOrNull { it.id == id } }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.diary_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.DIARY_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.DIARY_HELP,
                onToggleAudio = viewModel::toggleAudio,
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.diary_add_btn),
                    leadingIcon = Icons.Rounded.Add,
                    onClick = { onAddEntry(cropCycleId, plotId) },
                )
            }
        },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .clipToBounds(),
        ) {
            FarmSearchField(
                query = state.searchQuery,
                onQueryChange = viewModel::updateSearch,
                placeholder = farmString(R.string.common_search),
                modifier = Modifier.padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp8),
            )

            if (itemsList.isEmpty()) {
                FarmEmptyState(
                    title = farmString(R.string.diary_title),
                    message =
                    if (useTe) {
                        "ఇంకా ఫోటోలు లేదా మాటలు దాచలేదు. కింద బటన్ నొక్కి పొలం ఫోటో లేదా వాయిస్ నోట్ కలపండి."
                    } else {
                        "No diary entries yet. Tap the button below to capture a field photo or voice note."
                    },
                    icon = Icons.Rounded.PhotoCamera,
                    imageRes = FarmImages.diary,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
                    contentPadding = PaddingValues(FarmSpacing.dp16),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    items(
                        itemsList,
                        key = { it.id },
                        contentType = { "diary_entry_card" },
                    ) { entry ->
                        val categoryLabel = FarmLabels.diaryCategory(entry.category, lang)
                        val cleanTitle = entry.title.trim()
                        val isDuplicateTitle =
                            cleanTitle.isBlank() || cleanTitle.equals(categoryLabel, ignoreCase = true)
                        val displayTitle = if (isDuplicateTitle) categoryLabel else cleanTitle
                        val displaySubtitle =
                            if (isDuplicateTitle) {
                                entry.notes?.trim()?.takeIf { it.isNotEmpty() }
                            } else {
                                categoryLabel
                            }

                        Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                            FarmRecordCard(
                                title = displayTitle,
                                subtitle = displaySubtitle,
                                metadata = "${viewModel.formatDate(entry.entryDateEpochMillis)} • ${
                                    viewModel.formatTime(entry.entryDateEpochMillis)
                                }",
                                hasPhoto = entry.photoPaths.isNotEmpty(),
                                hasAudio = entry.voiceNotePath != null,
                                leadingIcon = Icons.Rounded.PhotoCamera,
                                leadingImageRes = FarmImages.forDiaryCategory(entry.category),
                                onClick = { selectedEntryId = entry.id },
                            )
                            entry.voiceNotePath?.let { path ->
                                val isPlaying = state.playingPath == path
                                FarmAudioPlayer(
                                    isPlaying = isPlaying,
                                    currentPositionMillis = if (isPlaying) state.playbackPositionMillis else 0L,
                                    totalDurationMillis = entry.voiceNoteDurationMillis ?: 0L,
                                    onPlayPauseClick = {
                                        viewModel.togglePlayVoice(path, entry.voiceNoteDurationMillis ?: 0L)
                                    },
                                    onSeekTo = viewModel::seekVoice,
                                    playContentDescription = farmString(R.string.common_listen),
                                    pauseContentDescription = farmString(R.string.common_stop_audio),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedEntry != null) {
        DiaryEntryDetailSheet(
            entry = selectedEntry,
            state = state,
            viewModel = viewModel,
            lang = lang,
            useTe = useTe,
            onDismiss = { selectedEntryId = null },
            onPhotoClick = { path -> previewPhoto = path to selectedEntry.title },
            onEditClick = {
                val id = selectedEntry.id
                selectedEntryId = null
                onEditEntry(id)
            },
            onDeleteClick = { pendingDeleteEntry = selectedEntry },
        )
    }

    pendingDeleteEntry?.let { target ->
        FarmConfirmationDialog(
            title = if (useTe) "ఈ డైరీ నమోదు తొలగించాలా?" else "Delete diary entry?",
            message = if (useTe) "ఈ ఫోటో మరియు మాట డైరీ నుండి తీసివేయబడుతుంది." else "This entry will be removed from your diary.",
            confirmLabel = if (useTe) "తొలగించండి" else "Delete",
            dismissLabel = farmString(R.string.common_cancel),
            isDestructive = true,
            onConfirm = {
                pendingDeleteEntry = null
                viewModel.deleteDiaryEntry(target.id) { selectedEntryId = null }
            },
            onDismiss = { pendingDeleteEntry = null },
        )
    }

    previewPhoto?.let { (path, caption) ->
        FarmPhotoViewer(
            photoFile = viewModel.resolveFile(path),
            onDismiss = { previewPhoto = null },
            caption = caption,
        )
    }
}

@Composable
fun DiaryFormScreen(
    entryId: Long?,
    initialCropCycleId: Long?,
    initialPlotId: Long?,
    viewModel: DiaryViewModel,
    onBack: () -> Unit,
    initialPhotoPath: String? = null,
    initialDateMillis: Long? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing =
        remember(entryId, state.entries) {
            entryId?.let { id -> state.entries.firstOrNull { it.id == id } }
        }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var selectedFarmId by remember(existing, state.farms) {
        mutableStateOf(existing?.farmId ?: state.prefs.activeFarmId ?: state.farms.firstOrNull()?.id)
    }
    var selectedPlotId by remember(existing, initialPlotId) {
        mutableStateOf(existing?.plotId ?: initialPlotId)
    }
    var selectedCropId by remember(existing, initialCropCycleId) {
        mutableStateOf(existing?.cropCycleId ?: initialCropCycleId)
    }
    var category by remember(existing) { mutableStateOf(existing?.category ?: DiaryCategory.CROP_GROWTH) }
    var dateMillis by remember(existing, initialDateMillis) {
        mutableLongStateOf(existing?.entryDateEpochMillis ?: initialDateMillis ?: System.currentTimeMillis())
    }
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var photos by remember(existing, initialPhotoPath) {
        mutableStateOf(existing?.photoPaths ?: listOfNotNull(initialPhotoPath))
    }
    var showMore by remember { mutableStateOf(false) }
    var previewPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existing?.id) {
        viewModel.initVoiceState(existing?.voiceNotePath, existing?.voiceNoteDurationMillis)
    }

    val photoLaunchers =
        rememberFarmPhotoLaunchers(
            createTempFile = viewModel::createTempCaptureFile,
            onCapturedFile = { f -> viewModel.saveCapturedPhoto(f) { photos = photos + it } },
            onGalleryUri = { uri -> viewModel.importGalleryPhoto(uri) { photos = photos + it } },
        )

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title =
                if (existing != null) {
                    if (useTe) "డైరీ మార్చండి" else "Edit Diary Entry"
                } else {
                    farmString(R.string.diary_add_btn)
                },
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.DIARY_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.DIARY_FORM_HELP,
                onToggleAudio = viewModel::toggleAudio,
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.common_save),
                    isLoading = state.isSaving,
                    onClick = {
                        val fId = selectedFarmId ?: state.farms.firstOrNull()?.id ?: 0L
                        val finalTitle =
                            title.trim().ifBlank {
                                "${FarmLabels.diaryCategory(category, lang)} • ${viewModel.formatTime(dateMillis)}"
                            }
                        viewModel.saveDiaryEntry(
                            existingId = existing?.id,
                            farmId = fId,
                            plotId = selectedPlotId,
                            cropCycleId = selectedCropId,
                            entryDateMillis = dateMillis,
                            category = category,
                            title = finalTitle,
                            notes = notes,
                            photoPaths = photos,
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
                .clipToBounds()
                .verticalScroll(rememberScrollState())
                .padding(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            // 1. Field Photo (#1)
            FarmPhotoCapture(
                photoPaths = photos,
                onTakePhotoClick = photoLaunchers.launchCamera,
                onPickFromGalleryClick = photoLaunchers.launchGallery,
                onRemovePhotoClick = { p -> photos = photos.filterNot { it == p } },
                onPhotoClick = { previewPath = it },
                title = if (useTe) "1. పొలం ఫోటో" else "1. Field Photo",
                cameraButtonLabel = farmString(R.string.diary_take_photo),
                galleryButtonLabel = farmString(R.string.diary_pick_gallery),
                resolveFile = viewModel::resolveFile,
            )

            // 2. Voice Recorder (#2)
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                Text(
                    text = if (useTe) "2. మాట రికార్డ్" else "2. Voice Note",
                    style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FarmColors.MainText,
                )
                DiaryVoiceSection(state = state, viewModel = viewModel)
            }

            // 3. Farm / Plot / Crop chips (#3)
            if (state.farms.isNotEmpty() || state.plots.isNotEmpty() || state.crops.isNotEmpty()) {
                DiaryScopeChipsSection(
                    farms = state.farms,
                    plots = state.plots,
                    crops = state.crops,
                    selectedFarmId = selectedFarmId,
                    selectedPlotId = selectedPlotId,
                    selectedCropId = selectedCropId,
                    onSelectFarm = { newFarmId ->
                        selectedFarmId = newFarmId
                        if (selectedPlotId != null && state.plots.none { it.id == selectedPlotId && it.farmId == newFarmId }) {
                            selectedPlotId = null
                        }
                        if (selectedCropId != null && state.crops.none { it.id == selectedCropId && it.farmId == newFarmId }) {
                            selectedCropId = null
                        }
                    },
                    onSelectPlot = { newPlotId ->
                        selectedPlotId = newPlotId
                        if (newPlotId != null &&
                            selectedCropId != null &&
                            state.crops.none { it.id == selectedCropId && it.plotId == newPlotId }
                        ) {
                            selectedCropId = null
                        }
                    },
                    onSelectCrop = { newCropId ->
                        selectedCropId = newCropId
                        if (newCropId != null) {
                            state.crops.firstOrNull { it.id == newCropId }?.let { crop ->
                                selectedFarmId = crop.farmId
                                selectedPlotId = crop.plotId
                            }
                        }
                    },
                    useTelugu = useTe,
                )
            }

            // 4. Date & Time (#4)
            FarmDatePicker(
                selectedDateMillis = dateMillis,
                onDateSelected = { picked -> dateMillis = mergeDateKeepingTime(picked, dateMillis) },
                label = if (useTe) "4. తేదీ" else "4. Date",
                formattedDate = viewModel.formatDate(dateMillis),
                todayLabel = farmString(R.string.common_today),
                yesterdayLabel = farmString(R.string.common_yesterday),
            )
            FarmTimeSlotPicker(
                label = if (useTe) "సమయం" else "Time",
                epochMillis = dateMillis,
                onEpochMillisChange = { dateMillis = it },
            )

            // Collapsed More Details (Category + Optional Title + Optional Notes)
            FarmMoreDetailsAccordion(
                expanded = showMore,
                onToggle = { showMore = !showMore },
                useTelugu = useTe,
            ) {
                FarmDropdown(
                    label = if (useTe) "విభాగం" else "Category",
                    options = DiaryCategory.entries.map { DropdownOption(it, FarmLabels.diaryCategory(it, lang)) },
                    selectedValue = category,
                    onSelect = { category = it.value },
                    placeholder = "",
                )
                FarmTextInput(
                    value = title,
                    onValueChange = { title = it },
                    label = if (useTe) "శీర్షిక (ఐచ్ఛికం)" else "Title (optional)",
                    placeholder = "${FarmLabels.diaryCategory(category, lang)} • ${viewModel.formatTime(dateMillis)}",
                )
                FarmTextInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = farmString(R.string.common_notes_optional),
                    singleLine = false,
                )
            }

            state.errorMessage?.let {
                Text(it, style = FarmTypography.bodyMedium, color = FarmColors.Error)
            }
        }
    }

    previewPath?.let { p ->
        FarmPhotoViewer(
            photoFile = viewModel.resolveFile(p),
            onDismiss = { previewPath = null },
            caption = title.ifBlank { FarmLabels.diaryCategory(category, lang) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DiaryEntryDetailSheet(
    entry: DiaryEntry,
    state: DiaryUiState,
    viewModel: DiaryViewModel,
    lang: AppLanguage,
    useTe: Boolean,
    onDismiss: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val farmName = state.farms.firstOrNull { it.id == entry.farmId }?.name
    val plotName = entry.plotId?.let { id -> state.plots.firstOrNull { it.id == id }?.name }
    val cropName = entry.cropCycleId?.let { id -> state.crops.firstOrNull { it.id == id }?.cropName }
    val scopeSummary = listOfNotNull(farmName, plotName, cropName).joinToString(" • ")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FarmColors.Surface,
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FarmSpacing.dp20)
                .padding(bottom = FarmSpacing.dp24),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            Text(
                text = entry.title.ifBlank { FarmLabels.diaryCategory(entry.category, lang) },
                style = FarmTypography.titleLarge,
                color = FarmColors.MainText,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                FarmStatusIndicator(
                    type = FarmStatusType.INFO,
                    label = FarmLabels.diaryCategory(entry.category, lang),
                )
                FarmStatusIndicator(
                    type = FarmStatusType.NEUTRAL,
                    label = "${viewModel.formatDate(entry.entryDateEpochMillis)} • ${
                        viewModel.formatTime(entry.entryDateEpochMillis)
                    }",
                )
            }
            if (scopeSummary.isNotBlank()) {
                Text(
                    text = scopeSummary,
                    style = FarmTypography.bodyMedium,
                    color = FarmColors.SecondaryText,
                )
            }

            if (entry.photoPaths.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                    entry.photoPaths.forEach { path ->
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(FarmShapeTokens.card)
                                .border(1.dp, FarmColors.Outline, FarmShapeTokens.card)
                                .clickable { onPhotoClick(path) },
                        ) {
                            AsyncImage(
                                model = viewModel.resolveFile(path),
                                contentDescription = entry.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    Text(
                        text = if (useTe) "ఫోటో పూర్తిగా చూడటానికి నొక్కండి" else "Tap photo to view full screen",
                        style = FarmTypography.bodySmall,
                        color = FarmColors.SecondaryText,
                    )
                }
            }

            entry.voiceNotePath?.let { path ->
                val isPlaying = state.playingPath == path
                FarmAudioPlayer(
                    isPlaying = isPlaying,
                    currentPositionMillis = if (isPlaying) state.playbackPositionMillis else 0L,
                    totalDurationMillis = entry.voiceNoteDurationMillis ?: 0L,
                    onPlayPauseClick = {
                        viewModel.togglePlayVoice(path, entry.voiceNoteDurationMillis ?: 0L)
                    },
                    onSeekTo = viewModel::seekVoice,
                    playContentDescription = farmString(R.string.common_listen),
                    pauseContentDescription = farmString(R.string.common_stop_audio),
                )
            }

            entry.notes?.trim()?.takeIf { it.isNotEmpty() }?.let { note ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = FarmShapeTokens.inputField,
                    color = FarmColors.SurfaceVariant,
                    border = BorderStroke(1.dp, FarmColors.Outline),
                ) {
                    Text(
                        text = note,
                        style = FarmTypography.bodyLarge,
                        color = FarmColors.MainText,
                        modifier = Modifier.padding(FarmSpacing.dp12),
                    )
                }
            }

            Spacer(Modifier.height(FarmSpacing.dp4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                FarmSecondaryButton(
                    text = if (useTe) "తొలగించండి" else "Delete",
                    leadingIcon = Icons.Rounded.Delete,
                    useMutedOutline = true,
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                )
                FarmPrimaryButton(
                    text = if (useTe) "సవరించండి" else "Edit",
                    leadingIcon = Icons.Rounded.Edit,
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DiaryVoiceSection(
    state: DiaryUiState,
    viewModel: DiaryViewModel,
) {
    val context = LocalContext.current
    var showMicExplainer by remember { mutableStateOf(false) }

    val micPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            showMicExplainer = false
            if (granted) viewModel.startVoiceRecording()
        }

    if (showMicExplainer) {
        FarmPermissionExplanation(
            title = farmString(R.string.perm_mic_title),
            explanation = farmString(R.string.perm_mic_msg),
            icon = Icons.Rounded.Mic,
            grantLabel = farmString(R.string.perm_grant),
            denyLabel = farmString(R.string.perm_deny),
            onGrantClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onDenyClick = { showMicExplainer = false },
        )
    } else {
        FarmVoiceRecorder(
            state = state.recordingState,
            onStartRecording = {
                val granted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.startVoiceRecording() else showMicExplainer = true
            },
            onStopRecording = viewModel::stopVoiceRecording,
            onDeleteRecording = viewModel::clearVoiceRecording,
            idleLabel = farmString(R.string.diary_voice_idle),
            recordingLabel = farmString(R.string.diary_voice_recording),
            audioPlayerContent = { path, dur ->
                val playing = state.playingPath == path
                FarmAudioPlayer(
                    isPlaying = playing,
                    currentPositionMillis = if (playing) state.playbackPositionMillis else 0L,
                    totalDurationMillis = dur,
                    onPlayPauseClick = { viewModel.togglePlayVoice(path, dur) },
                    onSeekTo = viewModel::seekVoice,
                    playContentDescription = farmString(R.string.common_listen),
                    pauseContentDescription = farmString(R.string.common_stop_audio),
                )
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiaryScopeChipsSection(
    farms: List<Farm>,
    plots: List<Plot>,
    crops: List<CropCycle>,
    selectedFarmId: Long?,
    selectedPlotId: Long?,
    selectedCropId: Long?,
    onSelectFarm: (Long) -> Unit,
    onSelectPlot: (Long?) -> Unit,
    onSelectCrop: (Long?) -> Unit,
    useTelugu: Boolean,
) {
    val filteredPlots =
        remember(plots, selectedFarmId) {
            if (selectedFarmId == null) plots else plots.filter { it.farmId == selectedFarmId }
        }
    val filteredCrops =
        remember(crops, selectedFarmId, selectedPlotId) {
            crops.filter {
                (selectedFarmId == null || it.farmId == selectedFarmId) &&
                    (selectedPlotId == null || it.plotId == selectedPlotId)
            }
        }
    val chipColors =
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = FarmColors.PrimaryContainer,
            selectedLabelColor = FarmColors.OnPrimaryContainer,
        )

    Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
        Text(
            text = if (useTelugu) "3. పొలం / మడి / పంట" else "3. Farm / Plot / Crop",
            style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = FarmColors.MainText,
        )
        if (farms.size > 1 || (farms.isNotEmpty() && filteredPlots.isEmpty() && filteredCrops.isEmpty())) {
            Text(
                text = farmString(R.string.farms_title),
                style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = FarmColors.SecondaryText,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                farms.forEach { farm ->
                    FilterChip(
                        selected = farm.id == selectedFarmId,
                        onClick = { onSelectFarm(farm.id) },
                        label = { Text(farm.name, style = FarmTypography.bodySmall) },
                        colors = chipColors,
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                }
            }
        }
        if (filteredPlots.isNotEmpty()) {
            Text(
                text = farmString(R.string.plots_title),
                style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = FarmColors.SecondaryText,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                FilterChip(
                    selected = selectedPlotId == null,
                    onClick = { onSelectPlot(null) },
                    label = { Text(if (useTelugu) "అన్ని మడులు" else "All Plots", style = FarmTypography.bodySmall) },
                    colors = chipColors,
                    modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                )
                filteredPlots.forEach { plot ->
                    FilterChip(
                        selected = plot.id == selectedPlotId,
                        onClick = { onSelectPlot(plot.id) },
                        label = { Text(plot.name, style = FarmTypography.bodySmall) },
                        colors = chipColors,
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                }
            }
        }
        if (filteredCrops.isNotEmpty()) {
            Text(
                text = farmString(R.string.crops_title),
                style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = FarmColors.SecondaryText,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                FilterChip(
                    selected = selectedCropId == null,
                    onClick = { onSelectCrop(null) },
                    label = { Text(if (useTelugu) "సాధారణం" else "General", style = FarmTypography.bodySmall) },
                    colors = chipColors,
                    modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                )
                filteredCrops.forEach { crop ->
                    FilterChip(
                        selected = crop.id == selectedCropId,
                        onClick = { onSelectCrop(crop.id) },
                        label = { Text(crop.cropName, style = FarmTypography.bodySmall) },
                        colors = chipColors,
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                }
            }
        }
    }
}

private fun mergeDateKeepingTime(
    newDateMillis: Long,
    currentDateTimeMillis: Long,
): Long {
    val dateCal = Calendar.getInstance().apply { timeInMillis = newDateMillis }
    val timeCal = Calendar.getInstance().apply { timeInMillis = currentDateTimeMillis }
    return Calendar.getInstance().apply {
        set(
            dateCal.get(Calendar.YEAR),
            dateCal.get(Calendar.MONTH),
            dateCal.get(Calendar.DAY_OF_MONTH),
            timeCal.get(Calendar.HOUR_OF_DAY),
            timeCal.get(Calendar.MINUTE),
            0,
        )
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
