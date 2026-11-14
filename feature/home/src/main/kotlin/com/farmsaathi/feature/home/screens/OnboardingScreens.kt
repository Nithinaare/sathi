package com.farmsaathi.feature.home.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Translate
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.FarmActionTile
import com.farmsaathi.core.designsystem.components.FarmAudioButton
import com.farmsaathi.core.designsystem.components.FarmAudioPlayer
import com.farmsaathi.core.designsystem.components.FarmNumberInput
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPermissionExplanation
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.components.FarmStatusIndicator
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.components.FarmVoiceRecorder
import com.farmsaathi.core.designsystem.components.VoiceRecordingState
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AreaUnit
import com.farmsaathi.feature.home.viewmodel.OnboardingViewModel

@Composable
fun OnboardingWelcomeScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val useTelugu = state.prefs.language == AppLanguage.TELUGU

    Scaffold(containerColor = FarmColors.Background) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(FarmSpacing.dp20)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FarmStatusIndicator(
                        type = FarmStatusType.INFO,
                        label = farmString(R.string.common_offline_badge),
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 2,
                    )
                    Spacer(Modifier.width(FarmSpacing.dp8))
                    FarmAudioButton(
                        audioCueKey = AudioCueCatalog.ONBOARDING_WELCOME,
                        isPlaying = state.isSpeaking,
                        onTogglePlay = {
                            if (state.isSpeaking) viewModel.stopAudio() else viewModel.playWelcomeCue(useTelugu)
                        },
                        label = farmString(R.string.common_listen),
                        contentDescription = farmString(R.string.common_listen),
                    )
                }

                Spacer(Modifier.height(FarmSpacing.dp24))
                Image(
                    painter = painterResource(FarmImages.sunriseWelcome),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                    Modifier
                        .size(88.dp)
                        .clip(FarmShapeTokens.actionTile)
                        .border(1.dp, FarmColors.Outline, FarmShapeTokens.actionTile),
                )
                Spacer(Modifier.height(FarmSpacing.dp16))
                Text(
                    text = farmString(R.string.onb_title),
                    style = FarmTypography.displaySmall,
                    color = FarmColors.MainText,
                )
                Spacer(Modifier.height(FarmSpacing.dp8))
                Text(
                    text = farmString(R.string.onb_subtitle),
                    style = FarmTypography.bodyLarge,
                    color = FarmColors.SecondaryText,
                )
                Spacer(Modifier.height(FarmSpacing.dp24))
                Text(
                    text = farmString(R.string.onb_lang_label),
                    style = FarmTypography.titleMedium,
                    color = FarmColors.MainText,
                )
                Spacer(Modifier.height(FarmSpacing.dp12))

                FarmActionTile(
                    title = "తెలుగు (Telugu)",
                    subtitle = "సులువైన రోజువారీ తెలుగులో",
                    icon = Icons.Rounded.Translate,
                    containerColor = if (useTelugu) FarmColors.PrimaryContainer else FarmColors.Surface,
                    borderColor = if (useTelugu) FarmColors.PrimaryForest else FarmColors.Outline,
                    compact = true,
                    onClick = { viewModel.setLanguage(AppLanguage.TELUGU) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(FarmSpacing.dp12))
                FarmActionTile(
                    title = "English",
                    subtitle = "Simple English labels",
                    icon = Icons.Rounded.Translate,
                    containerColor = if (!useTelugu) FarmColors.PrimaryContainer else FarmColors.Surface,
                    borderColor = if (!useTelugu) FarmColors.PrimaryForest else FarmColors.Outline,
                    compact = true,
                    onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column {
                Spacer(Modifier.height(FarmSpacing.dp24))
                FarmPrimaryButton(
                    text = farmString(R.string.onb_get_started),
                    isHero = true,
                    onClick = onContinue,
                )
            }
        }
    }
}

@Composable
fun OnboardingAudioMicTestScreen(
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val useTelugu = state.prefs.language == AppLanguage.TELUGU
    val context = LocalContext.current
    var showPermissionExplainer by remember { mutableStateOf(false) }

    val micPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            showPermissionExplainer = false
            if (granted) viewModel.startMicTest()
        }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.onb_audio_mic_title),
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
            Surface(
                shape = FarmShapeTokens.card,
                color = FarmColors.Surface,
                border = BorderStroke(1.dp, FarmColors.Outline),
            ) {
                Column(modifier = Modifier.padding(FarmSpacing.dp16)) {
                    FarmPrimaryButton(
                        text = farmString(R.string.onb_audio_test_btn),
                        onClick = { viewModel.playAudioTest(useTelugu) },
                    )
                    Spacer(Modifier.height(FarmSpacing.dp12))
                    val ttsLabel =
                        if (state.teluguTtsReady) {
                            if (useTelugu) "తెలుగు వాయిస్ సిద్ధంగా ఉంది" else "Telugu voice ready"
                        } else {
                            if (useTelugu) "ఫోన్ వాయిస్ ప్యాక్ లేకున్నా ఆడియో పనిచేస్తుంది" else "Tone cues active; install Telugu TTS for speech"
                        }
                    FarmStatusIndicator(
                        type = if (state.teluguTtsReady) FarmStatusType.SUCCESS else FarmStatusType.INFO,
                        label = ttsLabel,
                    )
                }
            }

            Text(
                text = farmString(R.string.onb_mic_test_hint),
                style = FarmTypography.bodyLarge,
                color = FarmColors.SecondaryText,
            )

            if (showPermissionExplainer) {
                FarmPermissionExplanation(
                    title = farmString(R.string.perm_mic_title),
                    explanation = farmString(R.string.perm_mic_msg),
                    icon = Icons.Rounded.Mic,
                    grantLabel = farmString(R.string.perm_grant),
                    denyLabel = farmString(R.string.perm_deny),
                    onGrantClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onDenyClick = { showPermissionExplainer = false },
                )
            } else {
                FarmVoiceRecorder(
                    state = state.micTestState,
                    onStartRecording = {
                        val granted =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.startMicTest() else showPermissionExplainer = true
                    },
                    onStopRecording = { viewModel.stopMicTest() },
                    onDeleteRecording = { viewModel.clearMicTest() },
                    idleLabel = farmString(R.string.diary_voice_idle),
                    recordingLabel = farmString(R.string.diary_voice_recording),
                    audioPlayerContent = { path, dur ->
                        FarmAudioPlayer(
                            isPlaying = false,
                            currentPositionMillis = 0L,
                            totalDurationMillis = dur,
                            onPlayPauseClick = { viewModel.playMicTestSample(path) },
                            onSeekTo = {},
                            playContentDescription = farmString(R.string.common_listen),
                            pauseContentDescription = farmString(R.string.common_stop_audio),
                        )
                    },
                )
            }

            if (state.micTestState is VoiceRecordingState.Recorded) {
                FarmStatusIndicator(
                    type = FarmStatusType.SUCCESS,
                    label = if (useTelugu) "మైక్ బాగానే పనిచేస్తోంది" else "Microphone verified",
                    icon = Icons.Rounded.CheckCircle,
                )
            }

            Spacer(Modifier.height(FarmSpacing.dp16))
            FarmPrimaryButton(
                text = farmString(R.string.onb_continue),
                isHero = true,
                onClick = onContinue,
            )
            FarmSecondaryButton(
                text = farmString(R.string.onb_skip),
                useMutedOutline = true,
                onClick = onContinue,
            )
        }
    }
}

@Composable
fun OnboardingProfileScreen(
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.onb_profile_title),
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
                .imePadding()
                .padding(FarmSpacing.dp16)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            FarmTextInput(
                value = state.farmerName,
                onValueChange = viewModel::updateFarmerName,
                label = farmString(R.string.onb_name_label),
                placeholder = farmString(R.string.onb_name_hint),
            )
            FarmTextInput(
                value = state.villageName,
                onValueChange = viewModel::updateVillageName,
                label = farmString(R.string.onb_village_label),
                placeholder = farmString(R.string.onb_village_hint),
            )
            FarmTextInput(
                value = state.firstFarmName,
                onValueChange = viewModel::updateFirstFarmName,
                label = farmString(R.string.onb_first_farm_label),
                placeholder = farmString(R.string.onb_first_farm_hint),
            )
            if (state.prefs.preferredAreaUnit == AreaUnit.ACRE_DECIMAL) {
                FarmNumberInput(
                    value = state.acresInput,
                    onValueChange = viewModel::updateAcres,
                    label = farmString(R.string.farms_acres_label),
                    allowDecimal = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    FarmNumberInput(
                        value = state.acresInput,
                        onValueChange = viewModel::updateAcres,
                        label = farmString(R.string.farms_acres_label),
                        allowDecimal = false,
                        modifier = Modifier.weight(1f),
                    )
                    FarmNumberInput(
                        value = state.subUnitInput,
                        onValueChange = viewModel::updateSubUnit,
                        label =
                        if (state.prefs.preferredAreaUnit == AreaUnit.ACRE_CENT) {
                            farmString(R.string.farms_cents_label)
                        } else {
                            farmString(R.string.farms_guntas_label)
                        },
                        allowDecimal = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(FarmSpacing.dp12))
            FarmPrimaryButton(
                text = farmString(R.string.onb_finish_btn),
                isHero = true,
                isLoading = state.isSaving,
                onClick = { viewModel.completeOnboarding(skipOptionalFarm = false, onCompleted = onCompleted) },
            )
            FarmSecondaryButton(
                text = farmString(R.string.onb_skip),
                useMutedOutline = true,
                onClick = { viewModel.completeOnboarding(skipOptionalFarm = true, onCompleted = onCompleted) },
            )
        }
    }
}
