package com.farmsaathi.feature.farm.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.DropdownOption
import com.farmsaathi.core.designsystem.components.FarmActionTile
import com.farmsaathi.core.designsystem.components.FarmConfirmationDialog
import com.farmsaathi.core.designsystem.components.FarmDropdown
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmIconButton
import com.farmsaathi.core.designsystem.components.FarmLoadingState
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
import com.farmsaathi.core.designsystem.components.FarmSummaryCard
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.components.FarmVisualGridTile
import com.farmsaathi.core.designsystem.components.rememberFarmPhotoLaunchers
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AreaConverter
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.core.model.IrrigationSource
import com.farmsaathi.feature.farm.viewmodel.FarmAndPlotViewModel

@Composable
fun FarmsListScreen(
    viewModel: FarmAndPlotViewModel,
    onOpenFarm: (Long) -> Unit,
    onAddFarm: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenCrops: () -> Unit = {},
    onAddPlot: (Long) -> Unit = {},
    onAddCropDirect: () -> Unit = {},
    onOpenActivities: () -> Unit = {},
    onOpenIrrigation: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
    onOpenHarvest: () -> Unit = {},
    onOpenSoilTests: () -> Unit = {},
    onOpenSpray: () -> Unit = onOpenInventory,
    onAddCropForFarm: (Long) -> Unit = { onAddCropDirect() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    if (state.isLoading) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.farms_title),
                audioCueKey = FarmAndPlotViewModel.FARMS_CUE,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == FarmAndPlotViewModel.FARMS_CUE,
                onToggleAudio = viewModel::toggleAudio,
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.farms_add_btn),
                    leadingIcon = Icons.Rounded.Add,
                    onClick = onAddFarm,
                )
            }
        },
    ) { padding ->
        if (state.farms.isEmpty()) {
            FarmEmptyState(
                title = farmString(R.string.farms_title),
                message =
                if (useTe) {
                    "ఇంకా ఏ పొలం కలపలేదు. కింద ఉన్న బటన్ నొక్కి మీ పొలం పేరు, ఎకరాలు రాసుకోండి."
                } else {
                    "No farms added yet. Tap the button below to add your first farm name and area."
                },
                icon = Icons.Rounded.Landscape,
                imageRes = FarmImages.sunriseWelcome,
                modifier = Modifier.padding(padding),
            )
        } else {
            val plotCountsByFarmId =
                remember(state.plots) {
                    state.plots.groupingBy { it.farmId }.eachCount()
                }
            val cropCountsByFarmId =
                remember(state.cropCycles) {
                    state.cropCycles.groupingBy { it.farmId }.eachCount()
                }
            LazyColumn(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(FarmSpacing.dp16),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                item(key = "crops_actions_header", contentType = "section_header") {
                    FarmSectionHeader(title = if (useTe) "పంటలు & పొలం పనులు" else "Crops & Field Actions")
                }
                item(key = "crops_actions_row", contentType = "three_col_action_row") {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    ) {
                        FarmVisualGridTile(
                            title = farmString(R.string.crops_title),
                            icon = Icons.Rounded.Grass,
                            imageRes = FarmImages.cropPaddy,
                            fullBleedImage = true,
                            borderColor = FarmColors.PrimaryForest,
                            speakDescription =
                            if (useTe) {
                                "పంటలు. మీరు వేసిన పంటల జాబితా, నాటిన తేదీలు ఇక్కడ చూడొచ్చు."
                            } else {
                                "Crops. View active and harvested crops."
                            },
                            onClick = onOpenCrops,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        FarmVisualGridTile(
                            title = farmString(R.string.home_tile_activity),
                            icon = Icons.AutoMirrored.Rounded.EventNote,
                            imageRes = FarmImages.farmWork,
                            fullBleedImage = true,
                            borderColor = FarmColors.HarvestAmber,
                            speakDescription =
                            if (useTe) {
                                "పొలం పని. దున్నుడు, కలుపు, మందు కొట్టే పనులు రాసుకోవచ్చు."
                            } else {
                                "Farm Work. Track daily crop activities."
                            },
                            onClick = onOpenActivities,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        FarmVisualGridTile(
                            title = farmString(R.string.home_tile_irrigation),
                            icon = Icons.Rounded.WaterDrop,
                            imageRes = FarmImages.irrigation,
                            fullBleedImage = true,
                            borderColor = FarmColors.Info,
                            speakDescription =
                            if (useTe) {
                                "నీటి తడులు. ఏ మడికి ఎప్పుడు నీరు పెట్టారో ఇక్కడ రాయండి."
                            } else {
                                "Irrigation. Log watering records."
                            },
                            onClick = onOpenIrrigation,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
                item(key = "your_farms_header", contentType = "section_header") {
                    FarmSectionHeader(title = if (useTe) "మీ పొలాలు" else "Your Farms")
                }
                items(
                    state.farms,
                    key = { it.id },
                    contentType = { "farm_record_card" },
                ) { farm ->
                    val plotCount = plotCountsByFarmId[farm.id] ?: 0
                    val cropCount = cropCountsByFarmId[farm.id] ?: 0
                    val areaLabel = FarmLabels.formatArea(farm.totalAreaMicroAcres, farm.preferredAreaUnit, lang)
                    val counts =
                        if (useTe) {
                            "$plotCount మడులు · $cropCount పంటలు"
                        } else {
                            "$plotCount Plots · $cropCount Crops"
                        }
                    Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                        FarmRecordCard(
                            title = farm.name,
                            subtitle = listOfNotNull(farm.villageOrLocation, areaLabel).joinToString(" • "),
                            metadata = counts,
                            leadingIcon = Icons.Rounded.Landscape,
                            leadingImageRes = FarmImages.farmField,
                            hasPhoto = farm.photoPath != null,
                            onClick = { onOpenFarm(farm.id) },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                        ) {
                            FarmSecondaryButton(
                                text = farmString(R.string.plots_add_btn),
                                leadingIcon = Icons.Rounded.Add,
                                useMutedOutline = true,
                                speakDescription =
                                if (useTe) {
                                    "${farm.name} పొలంలో కొత్త మడి లేదా భాగం కలపడానికి నొక్కండి."
                                } else {
                                    "Add a new plot inside ${farm.name}."
                                },
                                onClick = { onAddPlot(farm.id) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            FarmSecondaryButton(
                                text = farmString(R.string.crops_add_btn),
                                leadingIcon = Icons.Rounded.Add,
                                speakDescription =
                                if (useTe) {
                                    "${farm.name} లో కొత్త పంట వివరాలు మరియు క్యాలెండర్ కలపడానికి నొక్కండి."
                                } else {
                                    "Add a new crop cycle for ${farm.name}."
                                },
                                onClick = { onAddCropForFarm(farm.id) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
                item(key = "crop_soil_tools_header", contentType = "section_header") {
                    Spacer(Modifier.height(FarmSpacing.dp4))
                    FarmSectionHeader(title = if (useTe) "సాగు సాధనాలు" else "Crop & Soil Tools")
                }
                item(key = "crop_soil_tools_grid", contentType = "tools_grid") {
                    Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                        ) {
                            FarmVisualGridTile(
                                title = if (useTe) "మందు కొట్టాము" else "Crop Spray",
                                icon = Icons.Rounded.Inventory2,
                                imageRes = FarmImages.pesticide,
                                fullBleedImage = true,
                                borderColor = FarmColors.PrimaryForest,
                                onClick = onOpenSpray,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            FarmVisualGridTile(
                                title = farmString(R.string.harv_title),
                                icon = Icons.Rounded.Warehouse,
                                imageRes = FarmImages.harvest,
                                fullBleedImage = true,
                                borderColor = FarmColors.HarvestAmber,
                                onClick = onOpenHarvest,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                        ) {
                            FarmVisualGridTile(
                                title = if (useTe) "మట్టి కార్డు" else "Soil Card",
                                icon = Icons.Rounded.Science,
                                imageRes = FarmImages.soilTest,
                                fullBleedImage = true,
                                borderColor = FarmColors.Info,
                                onClick = onOpenSoilTests,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            FarmVisualGridTile(
                                title = if (useTe) "పంట పట్టికలు" else "Crop Schedules",
                                icon = Icons.Rounded.Grass,
                                imageRes = FarmImages.cropGeneric,
                                fullBleedImage = true,
                                borderColor = FarmColors.PrimaryForest,
                                onClick = onOpenTemplates,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        FarmActionTile(
                            title = farmString(R.string.inv_title),
                            subtitle = if (useTe) "విత్తనాలు, ఎరువులు, మందుల నిల్వ చూడండి" else "Seeds, fertilizer & pesticide stock",
                            icon = Icons.Rounded.Inventory2,
                            imageRes = FarmImages.inputs,
                            compact = true,
                            onClick = onOpenInventory,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FarmDetailScreen(
    farmId: Long,
    viewModel: FarmAndPlotViewModel,
    onBack: () -> Unit,
    onEditFarm: (Long) -> Unit,
    onAddPlot: (Long) -> Unit,
    onOpenPlot: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val farm = remember(state.farms, farmId) { state.farms.firstOrNull { it.id == farmId } }
    val plots = remember(state.plots, farmId) { state.plots.filter { it.farmId == farmId } }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    var showArchiveDialog by remember { mutableStateOf(false) }

    if (farm == null) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farm.name,
                subtitle = farm.villageOrLocation,
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = FarmAndPlotViewModel.PLOTS_CUE,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == FarmAndPlotViewModel.PLOTS_CUE,
                onToggleAudio = viewModel::toggleAudio,
                audioContentDescription = farmString(R.string.common_listen),
                trailingAction = {
                    Row {
                        FarmIconButton(
                            icon = Icons.Rounded.Edit,
                            contentDescription = farmString(R.string.common_edit),
                            onClick = { onEditFarm(farm.id) },
                        )
                        FarmIconButton(
                            icon = Icons.Rounded.Delete,
                            contentDescription = farmString(R.string.common_archive),
                            onClick = { showArchiveDialog = true },
                            tint = FarmColors.Error,
                        )
                    }
                },
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.plots_add_btn),
                    leadingIcon = Icons.Rounded.Add,
                    onClick = { onAddPlot(farm.id) },
                )
            }
        },
    ) { padding ->
        val plottedAreaSum = remember(plots) { plots.sumOf { it.areaMicroAcres } }
        LazyColumn(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            item(key = "farm_summary_card", contentType = "summary_card") {
                FarmSummaryCard(
                    title = farmString(R.string.farms_title),
                    primaryValue = FarmLabels.formatArea(farm.totalAreaMicroAcres, farm.preferredAreaUnit, lang),
                    secondaryMetrics =
                    listOfNotNull(
                        (if (useTe) "మడులు" else "Plots") to plots.size.toString(),
                        farm.waterSource?.let {
                            (if (useTe) "నీటి వసతి" else "Water") to FarmLabels.irrigationSource(it, lang)
                        },
                    ),
                )
            }
            if (plots.isNotEmpty() && farm.totalAreaMicroAcres > 0L && plottedAreaSum != farm.totalAreaMicroAcres) {
                item(key = "area_mismatch_indicator", contentType = "status_indicator") {
                    val sumFormatted = FarmLabels.formatArea(plottedAreaSum, farm.preferredAreaUnit, lang)
                    val farmFormatted = FarmLabels.formatArea(farm.totalAreaMicroAcres, farm.preferredAreaUnit, lang)
                    FarmStatusIndicator(
                        type = FarmStatusType.INFO,
                        label =
                        if (useTe) {
                            "మడుల మొత్తం ($sumFormatted) పొలం విస్తీర్ణం ($farmFormatted) కంటే వేరుగా ఉంది."
                        } else {
                            "Sum of plot areas ($sumFormatted) differs from farm area ($farmFormatted)."
                        },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item(key = "plots_section_header", contentType = "section_header") {
                FarmSectionHeader(title = farmString(R.string.plots_title))
            }
            if (plots.isEmpty()) {
                item(key = "plots_empty_state", contentType = "empty_state") {
                    FarmEmptyState(
                        title = farmString(R.string.plots_title),
                        message =
                        if (useTe) {
                            "ఈ పొలంలో ఇంకా మడులు కలపలేదు."
                        } else {
                            "No plots added to this farm yet."
                        },
                        icon = Icons.Rounded.Terrain,
                        imageRes = FarmImages.farmField,
                    )
                }
            } else {
                items(
                    plots,
                    key = { it.id },
                    contentType = { "plot_record_card" },
                ) { plot ->
                    FarmRecordCard(
                        title = plot.name,
                        subtitle = FarmLabels.formatArea(plot.areaMicroAcres, plot.preferredAreaUnit, lang),
                        metadata = plot.soilType,
                        leadingIcon = Icons.Rounded.Terrain,
                        leadingImageRes = FarmImages.farmField,
                        hasPhoto = plot.photoPaths.isNotEmpty(),
                        onClick = { onOpenPlot(plot.id) },
                    )
                }
            }
        }
    }

    if (showArchiveDialog) {
        FarmConfirmationDialog(
            title = if (useTe) "పొలం పక్కన పెట్టాలా?" else "Archive farm?",
            message =
            if (useTe) {
                "పాత ఖర్చులు, పంట రికార్డులు చెరిగిపోవు. కేవలం పొలం జాబితా నుండి తీసేస్తాము."
            } else {
                "Historical expenses and crop records will remain safely preserved."
            },
            confirmLabel = farmString(R.string.common_confirm),
            dismissLabel = farmString(R.string.common_cancel),
            isDestructive = true,
            onConfirm = {
                showArchiveDialog = false
                viewModel.archiveFarm(farm.id, onBack)
            },
            onDismiss = { showArchiveDialog = false },
        )
    }
}

@Composable
fun FarmFormScreen(
    farmId: Long?,
    viewModel: FarmAndPlotViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = remember(farmId, state.farms) { farmId?.let { id -> state.farms.firstOrNull { it.id == id } } }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var village by remember(existing) { mutableStateOf(existing?.villageOrLocation.orEmpty()) }
    var survey by remember(existing) { mutableStateOf(existing?.surveyNumber.orEmpty()) }
    var unit by remember(existing) { mutableStateOf(existing?.preferredAreaUnit ?: state.prefs.preferredAreaUnit) }
    val initPair =
        remember(existing, unit) {
            val micros = existing?.totalAreaMicroAcres ?: 0L
            when (unit) {
                AreaUnit.ACRE_GUNTA -> AreaConverter.toAcresAndGuntas(micros)
                AreaUnit.ACRE_CENT -> AreaConverter.toAcresAndCents(micros)
                AreaUnit.ACRE_DECIMAL -> 0 to 0
            }
        }
    var acres by remember(existing) {
        mutableStateOf(
            if (existing == null) {
                ""
            } else if (unit == AreaUnit.ACRE_DECIMAL) {
                AreaConverter.toDecimalAcres(existing.totalAreaMicroAcres).toPlainString()
            } else {
                initPair.first.toString()
            },
        )
    }
    var subUnit by remember(existing) { mutableStateOf(if (existing == null) "" else initPair.second.toString()) }
    var waterSource by remember(existing) { mutableStateOf(existing?.waterSource) }
    var photoPath by remember(existing) { mutableStateOf(existing?.photoPath) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var previewPhoto by remember { mutableStateOf<String?>(null) }
    val requiredMsg = farmString(R.string.common_required_field)

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
                title = if (existing == null) farmString(R.string.farms_add_btn) else farmString(R.string.farms_edit_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.common_save),
                    isLoading = state.isSaving,
                    onClick = {
                        if (name.isBlank()) {
                            nameError = requiredMsg
                        } else {
                            nameError = null
                            val microAcres = viewModel.parseAreaMicroAcres(acres, subUnit, unit)
                            viewModel.saveFarm(
                                existing = existing,
                                name = name,
                                village = village,
                                surveyNo = survey,
                                areaMicroAcres = microAcres,
                                areaUnit = unit,
                                waterSource = waterSource,
                                photoPath = photoPath,
                                notes = notes,
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
            FarmTextInput(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = farmString(R.string.farms_name_label),
                errorText = nameError,
            )
            FarmTextInput(
                value = village,
                onValueChange = { village = it },
                label = farmString(R.string.onb_village_label),
            )
            FarmDropdown(
                label = if (useTe) "కొలత విధానం" else "Area Unit",
                options = AreaUnit.entries.map { DropdownOption(it, FarmLabels.areaUnit(it, lang)) },
                selectedValue = unit,
                onSelect = { unit = it.value },
                placeholder = "",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                FarmNumberInput(
                    value = acres,
                    onValueChange = { acres = it },
                    label = farmString(R.string.farms_acres_label),
                    allowDecimal = unit == AreaUnit.ACRE_DECIMAL,
                    modifier = Modifier.weight(1f),
                )
                if (unit != AreaUnit.ACRE_DECIMAL) {
                    FarmNumberInput(
                        value = subUnit,
                        onValueChange = { subUnit = it },
                        label =
                        if (unit == AreaUnit.ACRE_GUNTA) {
                            farmString(R.string.farms_guntas_label)
                        } else {
                            farmString(R.string.farms_cents_label)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            FarmDropdown(
                label = if (useTe) "నీటి వసతి" else "Water Source",
                options = IrrigationSource.entries.map { DropdownOption(it, FarmLabels.irrigationSource(it, lang)) },
                selectedValue = waterSource,
                onSelect = { waterSource = it.value },
                placeholder = if (useTe) "ఎంచుకోండి" else "Select",
            )
            FarmTextInput(
                value = survey,
                onValueChange = { survey = it },
                label = if (useTe) "సర్వే నంబర్ (ఐచ్ఛికం)" else "Survey Number (optional)",
            )
            FarmPhotoCapture(
                photoPaths = listOfNotNull(photoPath),
                onTakePhotoClick = photoLaunchers.launchCamera,
                onPickFromGalleryClick = photoLaunchers.launchGallery,
                onRemovePhotoClick = { photoPath = null },
                onPhotoClick = { previewPhoto = it },
                title = if (useTe) "పొలం ఫోటో (ఐచ్ఛికం)" else "Farm Photo (optional)",
                cameraButtonLabel = farmString(R.string.diary_take_photo),
                galleryButtonLabel = farmString(R.string.diary_pick_gallery),
                resolveFile = viewModel::resolvePhotoFile,
                maxPhotos = 1,
            )
            FarmTextInput(
                value = notes,
                onValueChange = { notes = it },
                label = farmString(R.string.common_notes_optional),
                singleLine = false,
            )
        }
    }

    previewPhoto?.let { path ->
        FarmPhotoViewer(
            photoFile = viewModel.resolvePhotoFile(path),
            onDismiss = { previewPhoto = null },
            caption = name,
        )
    }
}

@Composable
fun PlotDetailScreen(
    plotId: Long,
    viewModel: FarmAndPlotViewModel,
    onBack: () -> Unit,
    onEditPlot: (Long, Long) -> Unit,
    onAddCrop: (Long) -> Unit,
    onOpenCrop: (Long) -> Unit,
    onOpenIrrigation: (Long) -> Unit,
    onOpenSoilTests: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val plot = remember(state.plots, plotId) { state.plots.firstOrNull { it.id == plotId } }
    val crops = remember(state.cropCycles, plotId) { state.cropCycles.filter { it.plotId == plotId } }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    var showArchive by remember { mutableStateOf(false) }

    if (plot == null) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = plot.name,
                subtitle = FarmLabels.formatArea(plot.areaMicroAcres, plot.preferredAreaUnit, lang),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = FarmAndPlotViewModel.PLOTS_CUE,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == FarmAndPlotViewModel.PLOTS_CUE,
                onToggleAudio = viewModel::toggleAudio,
                audioContentDescription = farmString(R.string.common_listen),
                trailingAction = {
                    Row {
                        FarmIconButton(
                            icon = Icons.Rounded.Edit,
                            contentDescription = farmString(R.string.common_edit),
                            onClick = { onEditPlot(plot.farmId, plot.id) },
                        )
                        FarmIconButton(
                            icon = Icons.Rounded.Delete,
                            contentDescription = farmString(R.string.common_archive),
                            tint = FarmColors.Error,
                            onClick = { showArchive = true },
                        )
                    }
                },
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.crops_add_btn),
                    leadingIcon = Icons.Rounded.Add,
                    onClick = { onAddCrop(plot.id) },
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
            item(key = "plot_actions_row", contentType = "two_col_action_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmActionTile(
                        title = farmString(R.string.irr_title),
                        icon = Icons.Rounded.WaterDrop,
                        imageRes = FarmImages.irrigation,
                        compact = true,
                        onClick = { onOpenIrrigation(plot.id) },
                        modifier = Modifier.weight(1f),
                    )
                    FarmActionTile(
                        title = farmString(R.string.soil_title),
                        icon = Icons.Rounded.Science,
                        imageRes = FarmImages.soilTest,
                        compact = true,
                        onClick = { onOpenSoilTests(plot.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item(key = "plot_crops_header", contentType = "section_header") {
                FarmSectionHeader(title = farmString(R.string.crops_title))
            }
            if (crops.isEmpty()) {
                item(key = "plot_crops_empty_state", contentType = "empty_state") {
                    FarmEmptyState(
                        title = farmString(R.string.crops_title),
                        message = if (useTe) "ఈ మడిలో ఇంకా పంట వేయలేదు." else "No crop cycles yet in this plot.",
                        icon = Icons.Rounded.Grass,
                        imageRes = FarmImages.cropPaddy,
                    )
                }
            } else {
                items(
                    crops,
                    key = { it.id },
                    contentType = { "plot_crop_card" },
                ) { cycle ->
                    FarmRecordCard(
                        title = listOfNotNull(cycle.cropName, cycle.variety).joinToString(" • "),
                        subtitle = FarmLabels.season(cycle.season, lang),
                        metadata = FarmLabels.cropCycleStatus(cycle.status, lang),
                        leadingIcon = Icons.Rounded.Grass,
                        leadingImageRes = FarmImages.forCropName(cycle.cropName),
                        onClick = { onOpenCrop(cycle.id) },
                    )
                }
            }
        }
    }

    if (showArchive) {
        FarmConfirmationDialog(
            title = if (useTe) "మడి పక్కన పెట్టాలా?" else "Archive plot?",
            message = if (useTe) "పాత పంట, ఖర్చుల వివరాలు చెరిగిపోవు." else "Existing history remains intact.",
            confirmLabel = farmString(R.string.common_confirm),
            dismissLabel = farmString(R.string.common_cancel),
            isDestructive = true,
            onConfirm = {
                showArchive = false
                viewModel.archivePlot(plot.id, onBack)
            },
            onDismiss = { showArchive = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlotFormScreen(
    farmId: Long,
    plotId: Long?,
    viewModel: FarmAndPlotViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = remember(plotId, state.plots) { plotId?.let { id -> state.plots.firstOrNull { it.id == id } } }
    val parentFarm = remember(farmId, state.farms) { state.farms.firstOrNull { it.id == farmId } }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var unit by remember(existing, parentFarm) {
        mutableStateOf(existing?.preferredAreaUnit ?: parentFarm?.preferredAreaUnit ?: state.prefs.preferredAreaUnit)
    }
    val initPair =
        remember(existing, unit) {
            val micros = existing?.areaMicroAcres ?: 0L
            when (unit) {
                AreaUnit.ACRE_GUNTA -> AreaConverter.toAcresAndGuntas(micros)
                AreaUnit.ACRE_CENT -> AreaConverter.toAcresAndCents(micros)
                AreaUnit.ACRE_DECIMAL -> 0 to 0
            }
        }
    var acres by remember(existing) {
        mutableStateOf(
            if (existing == null) {
                ""
            } else if (unit == AreaUnit.ACRE_DECIMAL) {
                AreaConverter.toDecimalAcres(existing.areaMicroAcres).toPlainString()
            } else {
                initPair.first.toString()
            },
        )
    }
    var subUnit by remember(existing) { mutableStateOf(if (existing == null) "" else initPair.second.toString()) }
    val soilPresets = remember(useTe) {
        if (useTe) {
            listOf("నల్ల రేగడి", "ఎర్ర నేల", "చల్కా", "ఒండ్రు")
        } else {
            listOf("Black Cotton", "Red Soil", "Chalka", "Alluvial")
        }
    }
    var soilType by remember(existing) { mutableStateOf(existing?.soilType.orEmpty()) }
    var customSoilExpanded by remember(existing, soilPresets) {
        val current = existing?.soilType.orEmpty()
        mutableStateOf(current.isNotBlank() && current !in soilPresets)
    }
    var waterSource by remember(existing) { mutableStateOf(existing?.irrigationSource) }
    var photos by remember(existing) { mutableStateOf(existing?.photoPaths.orEmpty()) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var areaError by remember { mutableStateOf<String?>(null) }
    var previewPhoto by remember { mutableStateOf<String?>(null) }
    val reqMsg = farmString(R.string.common_required_field)
    val invalidAreaMsg = if (useTe) "మడి విస్తీర్ణం 0 కంటే ఎక్కువ ఉండాలి" else "Plot area must be greater than 0"

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
                title = if (existing == null) farmString(R.string.plots_add_btn) else farmString(R.string.common_edit),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = FarmAndPlotViewModel.PLOTS_CUE,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == FarmAndPlotViewModel.PLOTS_CUE,
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
                        val areaMicro = viewModel.parseAreaMicroAcres(acres, subUnit, unit)
                        when {
                            name.isBlank() -> {
                                nameError = reqMsg
                                areaError = null
                            }
                            areaMicro <= 0L -> {
                                nameError = null
                                areaError = invalidAreaMsg
                            }
                            else -> {
                                nameError = null
                                areaError = null
                                viewModel.savePlot(
                                    existing = existing,
                                    farmId = farmId,
                                    name = name,
                                    areaMicroAcres = areaMicro,
                                    areaUnit = unit,
                                    soilType = soilType,
                                    waterSource = waterSource,
                                    photoPaths = photos,
                                    notes = notes,
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
            FarmTextInput(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = farmString(R.string.plots_name_label),
                errorText = nameError,
            )
            FarmDropdown(
                label = if (useTe) "కొలత విధానం" else "Area Unit",
                options = AreaUnit.entries.map { DropdownOption(it, FarmLabels.areaUnit(it, lang)) },
                selectedValue = unit,
                onSelect = { opt ->
                    val currentMicro = viewModel.parseAreaMicroAcres(acres, subUnit, unit)
                    val newUnit = opt.value
                    unit = newUnit
                    if (currentMicro > 0L) {
                        when (newUnit) {
                            AreaUnit.ACRE_GUNTA -> {
                                val (a, g) = AreaConverter.toAcresAndGuntas(currentMicro)
                                acres = a.toString()
                                subUnit = g.toString()
                            }
                            AreaUnit.ACRE_CENT -> {
                                val (a, c) = AreaConverter.toAcresAndCents(currentMicro)
                                acres = a.toString()
                                subUnit = c.toString()
                            }
                            AreaUnit.ACRE_DECIMAL -> {
                                acres = AreaConverter.toDecimalAcres(currentMicro).toPlainString()
                                subUnit = ""
                            }
                        }
                    }
                    areaError = null
                },
                placeholder = "",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                FarmNumberInput(
                    value = acres,
                    onValueChange = {
                        acres = it
                        areaError = null
                    },
                    label = farmString(R.string.farms_acres_label),
                    allowDecimal = unit == AreaUnit.ACRE_DECIMAL,
                    modifier = Modifier.weight(1f),
                )
                if (unit != AreaUnit.ACRE_DECIMAL) {
                    FarmNumberInput(
                        value = subUnit,
                        onValueChange = {
                            subUnit = it
                            areaError = null
                        },
                        label =
                        if (unit == AreaUnit.ACRE_GUNTA) {
                            farmString(R.string.farms_guntas_label)
                        } else {
                            farmString(R.string.farms_cents_label)
                        },
                        allowDecimal = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (areaError != null) {
                Text(
                    text = areaError.orEmpty(),
                    style = FarmTypography.bodyMedium,
                    color = FarmColors.Error,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                Text(
                    text = farmString(R.string.plots_soil_label),
                    style = FarmTypography.labelLarge,
                    color = FarmColors.MainText,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    soilPresets.forEach { preset ->
                        val isSelected = !customSoilExpanded && soilType == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                customSoilExpanded = false
                                soilType = if (isSelected) "" else preset
                            },
                            label = { Text(preset) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmColors.PrimaryContainer,
                                selectedLabelColor = FarmColors.PrimaryForest,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                        )
                    }
                    FilterChip(
                        selected = customSoilExpanded,
                        onClick = {
                            customSoilExpanded = !customSoilExpanded
                            if (!customSoilExpanded && soilType !in soilPresets) {
                                soilType = ""
                            } else if (customSoilExpanded && soilType in soilPresets) {
                                soilType = ""
                            }
                        },
                        label = { Text(if (useTe) "ఇతర" else "Other") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmColors.PrimaryContainer,
                            selectedLabelColor = FarmColors.PrimaryForest,
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                }
                if (customSoilExpanded) {
                    FarmTextInput(
                        value = soilType,
                        onValueChange = { soilType = it },
                        label = if (useTe) "మట్టి రకం రాయండి (ఐచ్ఛికం)" else "Custom Soil Type (optional)",
                        placeholder = if (useTe) "ఉదా: గరప నేల" else "e.g. Sandy loam",
                    )
                }
            }
            FarmDropdown(
                label = if (useTe) "నీటి వసతి" else "Water Source",
                options = IrrigationSource.entries.map { DropdownOption(it, FarmLabels.irrigationSource(it, lang)) },
                selectedValue = waterSource,
                onSelect = { waterSource = it.value },
                placeholder = if (useTe) "ఎంచుకోండి" else "Select",
            )
            FarmPhotoCapture(
                photoPaths = photos,
                onTakePhotoClick = photoLaunchers.launchCamera,
                onPickFromGalleryClick = photoLaunchers.launchGallery,
                onRemovePhotoClick = { path -> photos = photos.filterNot { it == path } },
                onPhotoClick = { previewPhoto = it },
                title = if (useTe) "మడి ఫోటోలు (ఐచ్ఛికం)" else "Plot Photos (optional)",
                cameraButtonLabel = farmString(R.string.diary_take_photo),
                galleryButtonLabel = farmString(R.string.diary_pick_gallery),
                resolveFile = viewModel::resolvePhotoFile,
            )
            FarmTextInput(
                value = notes,
                onValueChange = { notes = it },
                label = farmString(R.string.common_notes_optional),
                singleLine = false,
            )
        }
    }

    previewPhoto?.let { path ->
        FarmPhotoViewer(
            photoFile = viewModel.resolvePhotoFile(path),
            onDismiss = { previewPhoto = null },
            caption = name,
        )
    }
}
