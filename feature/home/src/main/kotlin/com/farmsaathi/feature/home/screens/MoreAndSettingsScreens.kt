package com.farmsaathi.feature.home.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.DropdownOption
import com.farmsaathi.core.designsystem.components.FarmAudioButton
import com.farmsaathi.core.designsystem.components.FarmConfirmationDialog
import com.farmsaathi.core.designsystem.components.FarmDropdown
import com.farmsaathi.core.designsystem.components.FarmListItem
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.components.FarmSnackbar
import com.farmsaathi.core.designsystem.components.FarmStatusIndicator
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.components.FarmVisualGridTile
import com.farmsaathi.core.designsystem.components.SnackbarTone
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.designsystem.localization.LocalAppLanguage
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.feature.home.viewmodel.SettingsAndBackupViewModel

@Suppress("UNUSED_PARAMETER")
@Composable
fun MoreHubScreen(
    onOpenDiary: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenInventory: () -> Unit = {},
    onOpenIrrigation: () -> Unit = {},
    onOpenHarvest: () -> Unit = {},
    onOpenSoilTests: () -> Unit = {},
    onOpenContacts: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenTemplates: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val useTe = LocalAppLanguage.current == AppLanguage.TELUGU
    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.home_extras_tile),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            item(key = "extras_row_diary_reports", contentType = "two_col_tile_row") {
                TwoColTileRow(
                    first = {
                        FarmVisualGridTile(
                            title = if (useTe) "పొలం డైరీ" else "Field Diary",
                            icon = Icons.Rounded.PhotoCamera,
                            imageRes = FarmImages.diary,
                            fullBleedImage = true,
                            borderColor = FarmColors.HarvestAmber,
                            speakDescription = if (useTe) "ఫోటో మరియు మాట డైరీ" else "Photo & Voice Diary",
                            onClick = onOpenDiary,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                    second = {
                        FarmVisualGridTile(
                            title = if (useTe) "నివేదికలు" else "Reports",
                            icon = Icons.Rounded.Assessment,
                            imageRes = FarmImages.reports,
                            fullBleedImage = true,
                            borderColor = FarmColors.PrimaryForest,
                            speakDescription = if (useTe) "లాభ నష్టాల నివేదిక" else "Profit & loss reports",
                            onClick = onOpenReports,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                )
            }
            item(key = "extras_row_backup_guide", contentType = "two_col_tile_row") {
                TwoColTileRow(
                    first = {
                        FarmVisualGridTile(
                            title = if (useTe) "బ్యాకప్" else "Backup",
                            icon = Icons.Rounded.Backup,
                            borderColor = FarmColors.HarvestAmber,
                            speakDescription = if (useTe) "ఫోన్‌లో బ్యాకప్ మరియు రీస్టోర్" else "Offline zip backup and restore",
                            onClick = onOpenBackup,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                    second = {
                        FarmVisualGridTile(
                            title = if (useTe) "వాడే విధానం" else "App Guide",
                            icon = Icons.AutoMirrored.Rounded.HelpOutline,
                            borderColor = FarmColors.Info,
                            speakDescription = if (useTe) "యాప్ వాడే విధానం వినండి" else "Listen to app guide",
                            onClick = onOpenHelp,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TwoColTileRow(
    first: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    second: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
    ) {
        first()
        second()
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsAndBackupViewModel,
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val useTe = state.prefs.language == AppLanguage.TELUGU
    var farmerNameInput by remember(state.prefs.farmerName) { mutableStateOf(state.prefs.farmerName) }
    var nameSavedHint by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.set_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.SETTINGS_BACKUP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.SETTINGS_BACKUP,
                onToggleAudio = viewModel::toggleCue,
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
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            Surface(
                shape = FarmShapeTokens.card,
                color = FarmColors.Surface,
                border = BorderStroke(1.dp, FarmColors.Outline),
            ) {
                Column(
                    modifier = Modifier.padding(FarmSpacing.dp16),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmTextInput(
                        value = farmerNameInput,
                        onValueChange = {
                            farmerNameInput = it
                            nameSavedHint = false
                        },
                        label = farmString(R.string.onb_name_label),
                        placeholder = farmString(R.string.onb_name_hint),
                    )
                    FarmSecondaryButton(
                        text = if (useTe) "పేరు సేవ్ చేయండి" else "Save Name",
                        leadingIcon = Icons.Rounded.CheckCircle,
                        onClick = {
                            viewModel.updateFarmerName(farmerNameInput.trim())
                            nameSavedHint = true
                        },
                    )
                    if (nameSavedHint) {
                        FarmStatusIndicator(
                            type = FarmStatusType.SUCCESS,
                            label = if (useTe) "పేరు సేవ్ అయింది" else "Name saved",
                        )
                    }
                }
            }

            Surface(
                shape = FarmShapeTokens.card,
                color = FarmColors.Surface,
                border = BorderStroke(1.dp, FarmColors.Outline),
            ) {
                Column(
                    modifier = Modifier.padding(FarmSpacing.dp16),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    Text(
                        text = if (useTe) "యాప్ భాష" else "App Language",
                        style = FarmTypography.titleMedium,
                        color = FarmColors.MainText,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                    ) {
                        SettingsRadioPill(
                            label = "తెలుగు",
                            selected = useTe,
                            onClick = { viewModel.setLanguage(AppLanguage.TELUGU) },
                            modifier = Modifier.weight(1f),
                        )
                        SettingsRadioPill(
                            label = "English",
                            selected = !useTe,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Surface(
                shape = FarmShapeTokens.card,
                color = FarmColors.Surface,
                border = BorderStroke(1.dp, FarmColors.Outline),
            ) {
                Column {
                    FarmListItem(
                        title = if (useTe) "మాటలతో సాయం" else "Spoken Audio Help",
                        subtitle = if (useTe) "బటన్ నొక్కి పట్టుకుంటే వినిపిస్తుంది" else "Press and hold buttons to hear them spoken",
                        leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp,
                        checked = state.prefs.audioGuidanceEnabled,
                        onCheckedChange = viewModel::setAudioGuidance,
                    )
                    FarmListItem(
                        title = if (useTe) "ఉదయం పని గుర్తుచేయండి (ఉదయం 7:00)" else "Morning Task Reminder (7:00 AM)",
                        leadingIcon = Icons.Rounded.NotificationsActive,
                        checked = state.prefs.dailyReminderEnabled,
                        onCheckedChange = { on -> viewModel.setDailyReminder(on, state.prefs.reminderHour) },
                        showDivider = false,
                    )
                }
            }

            Surface(
                shape = FarmShapeTokens.card,
                color = FarmColors.Surface,
                border = BorderStroke(1.dp, FarmColors.Outline),
            ) {
                Column(
                    modifier = Modifier.padding(FarmSpacing.dp16),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    Text(
                        text = if (useTe) "మాట వేగం" else "Speech Speed",
                        style = FarmTypography.titleMedium,
                        color = FarmColors.MainText,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                    ) {
                        SettingsRadioPill(
                            label = if (useTe) "నెమ్మది" else "Slow",
                            selected = state.isSlowSpeech,
                            onClick = { viewModel.setSlowSpeech(true) },
                            modifier = Modifier.weight(1f),
                        )
                        SettingsRadioPill(
                            label = if (useTe) "సాధారణం" else "Normal",
                            selected = !state.isSlowSpeech,
                            onClick = { viewModel.setSlowSpeech(false) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    FarmStatusIndicator(
                        type = if (state.teluguTtsReady) FarmStatusType.SUCCESS else FarmStatusType.WARNING,
                        label =
                        if (state.teluguTtsReady) {
                            if (useTe) "తెలుగు మాట సిద్ధంగా ఉంది" else "Telugu voice ready"
                        } else {
                            if (useTe) "ఫోన్‌లో తెలుగు మాట ఇంజిన్ దొరకలేదు" else "Telugu voice engine not ready yet"
                        },
                    )
                    FarmSecondaryButton(
                        text = if (useTe) "తెలుగు వాయిస్ పరీక్షించండి" else "Test Telugu Voice",
                        leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp,
                        onClick = viewModel::testTeluguVoice,
                    )
                }
            }

            FarmDropdown(
                label = if (useTe) "పొలం కొలత విధానం" else "Preferred Area Unit",
                options =
                AreaUnit.entries.map {
                    DropdownOption(it, FarmLabels.areaUnit(it, state.prefs.language))
                },
                selectedValue = state.prefs.preferredAreaUnit,
                onSelect = { viewModel.setAreaUnit(it.value) },
                placeholder = "",
            )

            state.dataCounts?.let { c ->
                Surface(
                    shape = FarmShapeTokens.card,
                    color = FarmColors.Surface,
                    border = BorderStroke(1.dp, FarmColors.Outline),
                ) {
                    Column(modifier = Modifier.padding(FarmSpacing.dp16)) {
                        Text(
                            text = if (useTe) "ఫోన్‌లో సేవ్ అయిన వివరాలు" else "Stored Records Overview",
                            style = FarmTypography.titleMedium,
                            color = FarmColors.MainText,
                        )
                        Spacer(Modifier.height(FarmSpacing.dp8))
                        Text(
                            text =
                            if (useTe) {
                                "పొలాలు: ${c.farms} · మడులు: ${c.plots} · పంటలు: ${c.cropCycles} · " +
                                    "డైరీ: ${c.diaryEntries} · పద్దులు: ${c.ledgerEntries} · కూలీలు: ${c.workers}"
                            } else {
                                "Farms: ${c.farms} · Plots: ${c.plots} · Crops: ${c.cropCycles} · " +
                                    "Diary: ${c.diaryEntries} · Ledger: ${c.ledgerEntries} · Workers: ${c.workers}"
                            },
                            style = FarmTypography.bodyMedium,
                            color = FarmColors.SecondaryText,
                        )
                    }
                }
            }

            FarmPrimaryButton(
                text = farmString(R.string.bak_title),
                leadingIcon = Icons.Rounded.Backup,
                onClick = onOpenBackup,
            )

            Text(
                text =
                if (useTe) {
                    "ఫామ్ సాథీ v1.1.0 · మీ ఫోన్‌లోనే భద్రంగా ఉంటుంది (ఆఫ్‌లైన్ యాప్)"
                } else {
                    "Farm Saathi v1.1.0 · 100% Offline Private App"
                },
                style = FarmTypography.bodySmall,
                color = FarmColors.SecondaryText,
            )
        }
    }
}

@Composable
private fun SettingsRadioPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = FarmShapeTokens.circular,
        color = if (selected) FarmColors.PrimaryContainer else FarmColors.Surface,
        border =
        BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) FarmColors.PrimaryForest else FarmColors.Outline,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp12),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = FarmTypography.titleMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (selected) FarmColors.PrimaryForest else FarmColors.MainText,
            )
        }
    }
}

@Composable
fun BackupRestoreScreen(
    viewModel: SettingsAndBackupViewModel,
    onBack: () -> Unit,
    onRestoredRestartHint: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val useTe = state.prefs.language == AppLanguage.TELUGU
    val context = LocalContext.current

    val openBackupLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) viewModel.inspectRestoreUri(uri)
        }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.bak_title),
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
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            FarmStatusIndicator(
                type = FarmStatusType.INFO,
                label = if (useTe) "Downloads/FarmSaathiBackups లో సేవ్ అవుతుంది" else "Saves to Downloads/FarmSaathiBackups",
            )

            state.prefs.lastBackupEpochMillis?.let { ts ->
                Text(
                    text = if (useTe) "చివరి బ్యాకప్: ${viewModel.formatDate(ts)}" else "Last backup: ${viewModel.formatDate(ts)}",
                    style = FarmTypography.bodyLarge,
                    color = FarmColors.SecondaryText,
                )
            }

            FarmPrimaryButton(
                text = farmString(R.string.bak_export_btn),
                leadingIcon = Icons.Rounded.Backup,
                isLoading = state.isBusy,
                onClick = viewModel::exportBackup,
            )

            state.lastExportedBackup?.let { exported ->
                FarmSecondaryButton(
                    text = if (useTe) "బ్యాకప్ ఫైల్ షేర్ చేయండి (WhatsApp / Drive)" else "Share Backup ZIP",
                    leadingIcon = Icons.Rounded.Share,
                    onClick = {
                        val shareIntent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, exported.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        context.startActivity(Intent.createChooser(shareIntent, exported.fileName))
                    },
                )
            }

            FarmSecondaryButton(
                text = farmString(R.string.bak_restore_btn),
                enabled = !state.isBusy,
                onClick = { openBackupLauncher.launch(arrayOf("application/zip", "*/*")) },
            )

            state.statusMessage?.let { msg ->
                FarmSnackbar(
                    message = msg,
                    tone = if (state.isErrorStatus) SnackbarTone.ERROR else SnackbarTone.SUCCESS,
                )
            }
        }
    }

    val inspection = state.pendingInspection
    if (inspection != null) {
        FarmConfirmationDialog(
            title = if (useTe) "పాత బ్యాకప్ ఎక్కించాలా?" else "Restore from backup?",
            message =
            if (useTe) {
                "ఇందులో ${inspection.mediaCount} మీడియా ఫైళ్లు ఉన్నాయి. ఇప్పుడున్న డేటా స్థానంలో ఈ బ్యాకప్ వస్తుంది."
            } else {
                "Backup contains ${inspection.mediaCount} media files. Current records will be replaced safely."
            },
            confirmLabel = farmString(R.string.common_confirm),
            dismissLabel = farmString(R.string.common_cancel),
            isDestructive = true,
            onConfirm = { viewModel.confirmRestore(onRestoredRestartHint) },
            onDismiss = viewModel::dismissRestoreDialog,
        )
    }
}

@Composable
fun OfflineHelpScreen(
    viewModel: SettingsAndBackupViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val useTe = state.prefs.language == AppLanguage.TELUGU

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (useTe) "యాప్ వాడే విధానం" else "Offline Help Guide",
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
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
            item(key = "help_tip_card", contentType = "help_tip_card") {
                Surface(
                    shape = FarmShapeTokens.card,
                    color = FarmColors.PrimaryContainer,
                    border = BorderStroke(1.dp, FarmColors.PrimaryForest),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(FarmSpacing.dp16),
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = FarmColors.PrimaryForest,
                        )
                        Text(
                            text =
                            if (useTe) {
                                "ఏ బటన్ మీదనైనా వేలు పెట్టి పట్టుకుంటే వినిపిస్తుంది"
                            } else {
                                "Press and hold any button to hear it spoken aloud"
                            },
                            style = FarmTypography.titleMedium,
                            color = FarmColors.PrimaryForest,
                        )
                    }
                }
            }
            items(
                AudioCueCatalog.all,
                key = { it.key },
                contentType = { "audio_cue_card" },
            ) { cue ->
                Surface(
                    shape = FarmShapeTokens.card,
                    color = FarmColors.Surface,
                    border = BorderStroke(1.dp, FarmColors.Outline),
                ) {
                    Column(modifier = Modifier.padding(FarmSpacing.dp16)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = if (useTe) cue.titleTe else cue.titleEn,
                                style = FarmTypography.titleMedium,
                                color = FarmColors.MainText,
                                modifier = Modifier.weight(1f),
                            )
                            FarmAudioButton(
                                audioCueKey = cue.key,
                                isPlaying = state.isAudioPlaying && state.activeCueKey == cue.key,
                                onTogglePlay = viewModel::toggleCue,
                                contentDescription = if (useTe) cue.titleTe else cue.titleEn,
                            )
                        }
                        Spacer(Modifier.height(FarmSpacing.dp8))
                        Text(
                            text = if (useTe) cue.scriptTe else cue.scriptEn,
                            style = FarmTypography.bodyMedium,
                            color = FarmColors.SecondaryText,
                        )
                    }
                }
            }
        }
    }
}
