package com.farmsaathi.feature.farm.measurement.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.localization.LocalAppLanguage
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AreaConverter
import com.farmsaathi.core.model.LandMeasurement
import com.farmsaathi.core.model.LandMeasurementMethod
import com.farmsaathi.feature.farm.measurement.location.GpsLocationSample
import com.farmsaathi.feature.farm.measurement.viewmodel.GpsCaptureStatus
import com.farmsaathi.feature.farm.measurement.viewmodel.LandMeasurementStep
import com.farmsaathi.feature.farm.measurement.viewmodel.LandMeasurementUiState
import com.farmsaathi.feature.farm.measurement.viewmodel.LandMeasurementViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun LandMeasurementScreen(
    plotId: Long,
    onBack: () -> Unit,
    viewModel: LandMeasurementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    val useTelugu = language == AppLanguage.TELUGU
    val context = LocalContext.current
    val localView = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showLocationRationale by remember { mutableStateOf(false) }
    var showApplyAreaConfirmation by remember { mutableStateOf(false) }
    var showDiscardCaptureConfirmation by remember { mutableStateOf(false) }
    val hasUnfinishedMeasurement = when (state.step) {
        LandMeasurementStep.GPS_CAPTURE ->
            state.acceptedPoints.isNotEmpty() ||
                state.gpsStatus in setOf(GpsCaptureStatus.RECORDING, GpsCaptureStatus.WAITING_FOR_FIX, GpsCaptureStatus.PAUSED)
        LandMeasurementStep.MANUAL_INPUT ->
            state.manualLengthFeet.isNotBlank() || state.manualWidthFeet.isNotBlank()
        LandMeasurementStep.REVIEW -> state.calculation != null
        else -> false
    }

    BackHandler(enabled = hasUnfinishedMeasurement) {
        viewModel.pauseGps()
        showDiscardCaptureConfirmation = true
    }

    LaunchedEffect(plotId) { viewModel.loadPlot(plotId) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineGranted) viewModel.startGps() else viewModel.onPermissionDenied()
    }

    val isCapturing = state.step == LandMeasurementStep.GPS_CAPTURE &&
        state.gpsStatus in setOf(GpsCaptureStatus.WAITING_FOR_FIX, GpsCaptureStatus.RECORDING)

    DisposableEffect(lifecycleOwner, state.step, isCapturing) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && isCapturing) viewModel.pauseGps()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(localView, isCapturing) {
        localView.keepScreenOn = isCapturing
        onDispose { localView.keepScreenOn = false }
    }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (useTelugu) "భూమి కొలత" else "Land measurement",
                subtitle = state.plot?.name,
                onBackClick = {
                    if (hasUnfinishedMeasurement) {
                        viewModel.pauseGps()
                        showDiscardCaptureConfirmation = true
                    } else {
                        viewModel.pauseGps()
                        onBack()
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state.step) {
                LandMeasurementStep.CHOOSE_METHOD -> MethodChoiceContent(
                    state = state,
                    useTelugu = useTelugu,
                    onChooseGps = viewModel::chooseGpsMethod,
                    onChooseManual = viewModel::chooseManualMethod,
                )
                LandMeasurementStep.GPS_CAPTURE -> GpsCaptureContent(
                    state = state,
                    useTelugu = useTelugu,
                    onStart = {
                        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fineGranted) {
                            viewModel.startGps()
                        } else {
                            showLocationRationale = true
                        }
                    },
                    onPause = viewModel::pauseGps,
                    onUndo = viewModel::undoLastPoint,
                    onFinish = viewModel::finishGps,
                    onOpenLocationSettings = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                )
                LandMeasurementStep.MANUAL_INPUT -> ManualMeasurementContent(
                    state = state,
                    useTelugu = useTelugu,
                    onLengthChange = viewModel::updateManualLength,
                    onWidthChange = viewModel::updateManualWidth,
                    onReview = viewModel::reviewManualMeasurement,
                    onBack = viewModel::backToMethodChoice,
                )
                LandMeasurementStep.REVIEW -> MeasurementReviewContent(
                    state = state,
                    useTelugu = useTelugu,
                    onSaveOnly = { viewModel.saveMeasurement(applyAreaToPlot = false) },
                    onSaveAndApply = { showApplyAreaConfirmation = true },
                    onBack = {
                        when (state.method) {
                            LandMeasurementMethod.GPS_BOUNDARY -> viewModel.chooseGpsMethod()
                            LandMeasurementMethod.MANUAL_RECTANGLE -> viewModel.chooseManualMethod()
                            null -> viewModel.backToMethodChoice()
                        }
                    },
                )
                LandMeasurementStep.SAVED -> MeasurementSavedContent(
                    state = state,
                    useTelugu = useTelugu,
                    onBack = onBack,
                    onAnother = viewModel::startAnotherMeasurement,
                )
            }

            state.messageTe?.let { message ->
                val display = if (useTelugu) message else state.messageEn ?: message
                Card(colors = CardDefaults.cardColors(containerColor = FarmColors.WarningContainer)) {
                    Text(display, modifier = Modifier.padding(12.dp), color = FarmColors.OnAmberContainer, style = MaterialTheme.typography.bodyMedium)
                }
                if (state.gpsStatus == GpsCaptureStatus.ERROR &&
                    state.messageEn?.contains("permission", ignoreCase = true) == true
                ) {
                    FarmSecondaryButton(
                        text = if (useTelugu) "యాప్ అనుమతులు తెరవండి" else "Open app permissions",
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    if (showLocationRationale) {
        AlertDialog(
            onDismissRequest = { showLocationRationale = false },
            title = {
                Text(if (useTelugu) "GPS అనుమతి ఎందుకు?" else "Why does Farm Saathi need location?")
            },
            text = {
                Text(
                    if (useTelugu) {
                        "మీరు పొలం చుట్టూ నడిచి కొలుస్తున్నప్పుడు మాత్రమే మీ ఫోన్ GPS స్థానాన్ని ఉపయోగిస్తాం. కొలత ఆపితే GPS కూడా ఆగుతుంది."
                    } else {
                        "Farm Saathi uses GPS only while you actively measure a field boundary. Pausing or leaving this screen stops location recording."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationRationale = false
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                        )
                    },
                ) {
                    Text(if (useTelugu) "కొనసాగించండి" else "Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationRationale = false }) {
                    Text(if (useTelugu) "వద్దు" else "Not now")
                }
            },
        )
    }

    if (showApplyAreaConfirmation) {
        AlertDialog(
            onDismissRequest = { showApplyAreaConfirmation = false },
            title = {
                Text(if (useTelugu) "మడి పాత విస్తీర్ణం మార్చాలా?" else "Update the saved plot area?")
            },
            text = {
                Text(
                    if (useTelugu) {
                        "కొత్త కొలత మడి వివరాల్లో ఉన్న విస్తీర్ణాన్ని మార్చుతుంది. పాత కొలత చరిత్రలో ఉంటుంది. కొనసాగించాలా?"
                    } else {
                        "This will replace the plot's currently saved area with this new estimate. The previous measurement remains in history. Continue?"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showApplyAreaConfirmation = false
                        viewModel.saveMeasurement(applyAreaToPlot = true)
                    },
                ) {
                    Text(if (useTelugu) "అవును, మార్చండి" else "Yes, update area")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyAreaConfirmation = false }) {
                    Text(if (useTelugu) "వద్దు" else "Cancel")
                }
            },
        )
    }

    if (showDiscardCaptureConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardCaptureConfirmation = false },
            title = {
                Text(if (useTelugu) "కొలతను వదిలేయాలా?" else "Discard this measurement?")
            },
            text = {
                Text(
                    if (useTelugu) {
                        "ఇప్పటివరకు చేసిన కొలత సేవ్ కాలేదు. బయటకు వెళ్తే ఈ వివరాలు పోతాయి."
                    } else {
                        "This measurement has not been saved yet. Leaving now will discard the work captured on this screen."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardCaptureConfirmation = false
                        viewModel.backToMethodChoice()
                        onBack()
                    },
                ) {
                    Text(if (useTelugu) "కొలత వదిలేయండి" else "Discard and leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardCaptureConfirmation = false }) {
                    Text(if (useTelugu) "ఇక్కడే ఉండండి" else "Keep measuring")
                }
            },
        )
    }
}

@Composable
private fun MethodChoiceContent(
    state: LandMeasurementUiState,
    useTelugu: Boolean,
    onChooseGps: () -> Unit,
    onChooseManual: () -> Unit,
) {
    Text(
        text = if (useTelugu) "ఈ మడి ఎంత ఉందో కొలవండి" else "Measure this plot",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = FarmColors.MainText,
    )
    Text(
        text = if (useTelugu) "మీకు సులభమైన పద్ధతి ఎంచుకోండి." else "Choose the method that is easiest for you.",
        color = FarmColors.SecondaryText,
    )
    state.plot?.let { plot ->
        Card(colors = CardDefaults.cardColors(containerColor = FarmColors.PrimaryContainer)) {
            Column(Modifier.padding(14.dp)) {
                Text(if (useTelugu) "ప్రస్తుత మడి విస్తీర్ణం" else "Current saved plot area", color = FarmColors.OnPrimaryContainer)
                Text(
                    text = formatArea(plot.areaMicroAcres, useTelugu),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = FarmColors.OnPrimaryContainer,
                )
            }
        }
    }
    MethodCard(
        icon = Icons.Rounded.GpsFixed,
        title = if (useTelugu) "చుట్టూ నడిచి కొలవండి" else "Walk around the field",
        description = if (useTelugu) "పొలం అంచు వెంట నడిస్తే ఫోన్ GPS కొలుస్తుంది." else "Walk along the field boundary and let GPS record the shape.",
        onClick = onChooseGps,
    )
    MethodCard(
        icon = Icons.Rounded.SquareFoot,
        title = if (useTelugu) "పొడవు, వెడల్పు రాయండి" else "Enter length and width",
        description = if (useTelugu) "దీర్ఘచతురస్రం లాంటి మడికి మాత్రమే." else "For plots that are approximately rectangular.",
        onClick = onChooseManual,
    )
    NoticeCard(
        text = if (useTelugu) "ఫోన్‌తో వచ్చిన కొలత సుమారు అంచనా మాత్రమే. చట్టబద్ధమైన భూమి సర్వేకు ఇది బదులు కాదు." else "Phone GPS gives an estimate. It is not a legal land survey.",
    )
    if (state.history.isNotEmpty()) {
        Text(if (useTelugu) "పాత కొలతలు" else "Previous measurements", fontWeight = FontWeight.Bold, color = FarmColors.MainText)
        state.history.take(5).forEach { MeasurementHistoryRow(it, useTelugu) }
    }
}

@Composable
private fun GpsCaptureContent(
    state: LandMeasurementUiState,
    useTelugu: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    Text(
        if (useTelugu) "పొలం అంచు వెంబడి నడవండి" else "Walk along the field boundary",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = FarmColors.MainText,
    )
    Text(
        if (useTelugu) "ప్రారంభించిన దగ్గరికి తిరిగి రండి. ఫోన్‌ను చేతిలో ఉంచండి." else "Walk around the edge and return near your starting point. Keep the phone with you.",
        color = FarmColors.SecondaryText,
    )
    GpsQualityCard(state, useTelugu)
    BoundaryPreview(state.acceptedPoints, useTelugu)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        MetricPill(
            label = if (useTelugu) "నమోదైన మూలలు" else "Points recorded",
            value = state.acceptedPoints.size.toString(),
            modifier = Modifier.weight(1f),
        )
        MetricPill(
            label = if (useTelugu) "GPS స్పష్టత" else "Latest GPS accuracy",
            value = state.latestAccuracyMeters?.let { "± ${it.toInt()} m" } ?: "—",
            modifier = Modifier.weight(1f),
        )
    }
    if (state.gpsStatus == GpsCaptureStatus.RECORDING || state.gpsStatus == GpsCaptureStatus.WAITING_FOR_FIX) {
        FarmSecondaryButton(
            text = if (useTelugu) "కొలత ఆపండి" else "Pause measurement",
            onClick = onPause,
        )
    } else {
        FarmPrimaryButton(
            text = when (state.gpsStatus) {
                GpsCaptureStatus.PAUSED -> if (useTelugu) "మళ్లీ కొనసాగించండి" else "Resume measurement"
                GpsCaptureStatus.ERROR -> if (useTelugu) "మళ్లీ ప్రయత్నించండి" else "Try GPS again"
                else -> if (useTelugu) "GPS ప్రారంభించండి" else "Start GPS measurement"
            },
            onClick = onStart,
        )
    }
    FarmSecondaryButton(
        text = if (useTelugu) "చివరి స్థానం తీసేయండి" else "Undo last point",
        leadingIcon = Icons.Rounded.Undo,
        onClick = onUndo,
        enabled = state.acceptedPoints.isNotEmpty(),
    )
    FarmSecondaryButton(
        text = if (useTelugu) "ఫోన్ GPS సెట్టింగ్స్" else "Open phone GPS settings",
        leadingIcon = Icons.Rounded.MyLocation,
        onClick = onOpenLocationSettings,
    )
    FarmPrimaryButton(
        text = if (state.isCalculating) {
            if (useTelugu) "లెక్కిస్తున్నాం..." else "Calculating..."
        } else if (useTelugu) {
            "నడక పూర్తయింది — విస్తీర్ణం చూడండి"
        } else {
            "Finish walk and calculate area"
        },
        onClick = onFinish,
        enabled = !state.isCalculating && state.acceptedPoints.size >= 3,
        isLoading = state.isCalculating,
    )
    NoticeCard(
        text = if (useTelugu) "GPS కొలత జరుగుతున్నప్పుడు ఈ తెర తెరిచి ఉంచండి. ఫోన్ లాక్ చేస్తే కొలత ఆగుతుంది." else "Keep this screen open during the walk. The measurement pauses when the app goes to the background or the phone is locked.",
    )
}

@Composable
private fun ManualMeasurementContent(
    state: LandMeasurementUiState,
    useTelugu: Boolean,
    onLengthChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    onReview: () -> Unit,
    onBack: () -> Unit,
) {
    Text(if (useTelugu) "పొడవు, వెడల్పు రాయండి" else "Enter the dimensions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(if (useTelugu) "కొలత అడుగుల్లో రాయండి." else "Enter both dimensions in feet.", color = FarmColors.SecondaryText)
    OutlinedTextField(
        value = state.manualLengthFeet,
        onValueChange = onLengthChange,
        label = { Text(if (useTelugu) "పొడవు (అడుగులు)" else "Length (feet)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.manualWidthFeet,
        onValueChange = onWidthChange,
        label = { Text(if (useTelugu) "వెడల్పు (అడుగులు)" else "Width (feet)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    NoticeCard(
        text = if (useTelugu) "ఈ పద్ధతి నాలుగు వైపులా సుమారుగా సమకోణాలున్న మడికి మాత్రమే. వంకర ఆకారానికి GPS పద్ధతి ఎంచుకోండి." else "Use this only for an approximately rectangular plot. Use GPS for irregular boundaries.",
    )
    FarmPrimaryButton(
        text = if (useTelugu) "విస్తీర్ణం లెక్కించండి" else "Calculate area",
        onClick = onReview,
        enabled = !state.isCalculating,
        isLoading = state.isCalculating,
    )
    FarmSecondaryButton(text = if (useTelugu) "వెనక్కి" else "Back", onClick = onBack)
}

@Composable
private fun MeasurementReviewContent(
    state: LandMeasurementUiState,
    useTelugu: Boolean,
    onSaveOnly: () -> Unit,
    onSaveAndApply: () -> Unit,
    onBack: () -> Unit,
) {
    val result = state.calculation
    Text(if (useTelugu) "కొలతను ఒకసారి చూడండి" else "Review the measurement", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    if (result != null) {
        Card(colors = CardDefaults.cardColors(containerColor = FarmColors.PrimaryForest)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text(if (useTelugu) "సుమారు విస్తీర్ణం" else "Estimated area", color = FarmColors.OnPrimary)
                Text(
                    formatArea(result.areaMicroAcres, useTelugu),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "${formatNumber(result.areaSquareMeters)} m²  ·  ${formatNumber(result.areaSquareMeters * 10.7639104167)} sq ft",
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        DetailLine(if (useTelugu) "కొలిచిన పద్ధతి" else "Method", if (state.method == LandMeasurementMethod.GPS_BOUNDARY) if (useTelugu) "GPS తో నడిచి" else "GPS boundary walk" else if (useTelugu) "పొడవు × వెడల్పు" else "Manual rectangle")
        DetailLine(if (useTelugu) "చుట్టుకొలత" else "Perimeter", "${formatNumber(result.perimeterMeters)} m")
        if (state.method == LandMeasurementMethod.GPS_BOUNDARY) {
            DetailLine(if (useTelugu) "నమోదైన మూలలు" else "Boundary points", result.pointCount.toString())
            val accuracyValues = state.acceptedPoints.map { it.accuracyMeters }
            if (accuracyValues.isNotEmpty()) {
                DetailLine(if (useTelugu) "సగటు GPS స్పష్టత" else "Average GPS accuracy", "± ${formatNumber(accuracyValues.average())} m")
                DetailLine(if (useTelugu) "గరిష్ఠ GPS లోపం" else "Worst recorded GPS accuracy", "± ${formatNumber(accuracyValues.maxOrNull() ?: 0.0)} m")
                if ((accuracyValues.maxOrNull() ?: 0.0) > 10.0) {
                    NoticeCard(
                        text = if (useTelugu) {
                            "కొన్ని GPS స్థానాల్లో స్పష్టత తక్కువగా ఉంది. ఈ విస్తీర్ణాన్ని జాగ్రత్తగా అంచనాగా మాత్రమే చూడండి."
                        } else {
                            "Some recorded GPS fixes had weak accuracy. Treat this area as a rough estimate."
                        },
                    )
                }
            }
            BoundaryPreview(state.acceptedPoints, useTelugu, closeBoundary = true)
        }
        NoticeCard(text = if (useTelugu) "ఇది ఫోన్‌తో వచ్చిన అంచనా మాత్రమే. మీ పాత మడి విస్తీర్ణం చరిత్రలో అలాగే ఉంటుంది." else "This is a phone-based estimate. The previous saved plot area remains in measurement history.")
        FarmPrimaryButton(
            text = if (useTelugu) "కొలత మాత్రమే దాచండి" else "Save measurement only",
            onClick = onSaveOnly,
            isLoading = state.isSaving,
        )
        FarmSecondaryButton(
            text = if (useTelugu) "దాచండి + మడి విస్తీర్ణం మార్చండి" else "Save and update plot area",
            onClick = onSaveAndApply,
            enabled = !state.isSaving,
        )
        FarmSecondaryButton(text = if (useTelugu) "మళ్లీ కొలవండి" else "Measure again", onClick = onBack, enabled = !state.isSaving)
    }
}

@Composable
private fun MeasurementSavedContent(
    state: LandMeasurementUiState,
    useTelugu: Boolean,
    onBack: () -> Unit,
    onAnother: () -> Unit,
) {
    Text("✓", style = MaterialTheme.typography.displayLarge, color = FarmColors.Success, fontWeight = FontWeight.Bold)
    Text(if (useTelugu) "కొలత దాచబడింది" else "Measurement saved", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    state.calculation?.let { result ->
        Text(formatArea(result.areaMicroAcres, useTelugu), style = MaterialTheme.typography.titleLarge, color = FarmColors.PrimaryForest, fontWeight = FontWeight.Bold)
    }
    Text(
        text = if (state.savedAndAppliedToPlot) {
            if (useTelugu) "కొత్త విస్తీర్ణం మడి వివరాల్లో కూడా మార్చబడింది." else "The plot's saved area has also been updated."
        } else {
            if (useTelugu) "మడి పాత విస్తీర్ణం మార్చలేదు. ఈ కొలత చరిత్రలో దాచబడింది." else "The plot's saved area was not changed. This estimate is in history."
        },
        color = FarmColors.SecondaryText,
    )
    FarmPrimaryButton(text = if (useTelugu) "మడికి తిరిగి వెళ్ళండి" else "Return to plot", onClick = onBack)
    FarmSecondaryButton(text = if (useTelugu) "మరోసారి కొలవండి" else "Take another measurement", onClick = onAnother)
}

@Composable
private fun MethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = FarmColors.Surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).background(FarmColors.PrimaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = FarmColors.PrimaryForest, modifier = Modifier.size(27.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = FarmColors.MainText)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = FarmColors.SecondaryText)
            }
            Text("›", color = FarmColors.PrimaryForest, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun GpsQualityCard(state: LandMeasurementUiState, useTelugu: Boolean) {
    val accuracy = state.latestAccuracyMeters
    val statusText = when {
        state.gpsStatus == GpsCaptureStatus.ERROR -> if (useTelugu) "GPS అందుబాటులో లేదు" else "GPS unavailable"
        accuracy == null -> if (useTelugu) "GPS స్థానం కోసం వేచి ఉండండి" else "Waiting for GPS fix"
        accuracy <= 5.0 -> if (useTelugu) "GPS బాగుంది" else "Good GPS signal"
        accuracy <= 10.0 -> if (useTelugu) "GPS సరిపోతుంది" else "Usable GPS signal"
        else -> if (useTelugu) "GPS స్పష్టత తక్కువ" else "Weak GPS accuracy"
    }
    val bg = if (accuracy != null && accuracy <= 10.0) FarmColors.SuccessContainer else FarmColors.WarningContainer
    Card(colors = CardDefaults.cardColors(containerColor = bg)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.GpsFixed, contentDescription = null, tint = if (accuracy != null && accuracy <= 10.0) FarmColors.Success else FarmColors.Warning)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(statusText, fontWeight = FontWeight.Bold, color = FarmColors.MainText)
                Text(
                    if (accuracy != null) "± ${formatNumber(accuracy)} m" else if (useTelugu) "బయట ఖాళీ ఆకాశం వైపు ఉంచండి" else "Move to an open area with a clear sky view",
                    style = MaterialTheme.typography.bodySmall,
                    color = FarmColors.SecondaryText,
                )
            }
        }
    }
}

@Composable
private fun BoundaryPreview(
    points: List<GpsLocationSample>,
    useTelugu: Boolean,
    closeBoundary: Boolean = false,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF0E2))) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Text(if (useTelugu) "మీరు నడిచిన హద్దు" else "Boundary you have walked", fontWeight = FontWeight.Bold, color = FarmColors.MainText)
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(8.dp)
                    .semantics {
                        contentDescription = if (useTelugu) {
                            "నమోదైన ${points.size} GPS స్థానాల రేఖాచిత్రం"
                        } else {
                            "Sketch of ${points.size} recorded GPS boundary points"
                        }
                    },
            ) {
                val inset = 20.dp.toPx()
                val width = size.width - inset * 2f
                val height = size.height - inset * 2f
                // A normalized sketch only. It is not a geographic map or a north-up map.
                repeat(4) { index ->
                    val x = inset + width * index / 3f
                    val y = inset + height * index / 3f
                    drawLine(Color(0xFFD5E0CC), androidx.compose.ui.geometry.Offset(x, inset), androidx.compose.ui.geometry.Offset(x, inset + height), 1.dp.toPx())
                    drawLine(Color(0xFFD5E0CC), androidx.compose.ui.geometry.Offset(inset, y), androidx.compose.ui.geometry.Offset(inset + width, y), 1.dp.toPx())
                }
                if (points.isNotEmpty()) {
                    val minLat = points.minOf { it.latitude }
                    val maxLat = points.maxOf { it.latitude }
                    val minLon = points.minOf { it.longitude }
                    val maxLon = points.maxOf { it.longitude }
                    val latSpan = max(maxLat - minLat, 0.000001)
                    val lonSpan = max(maxLon - minLon, 0.000001)
                    val projected = points.map { point ->
                        androidx.compose.ui.geometry.Offset(
                            x = inset + ((point.longitude - minLon) / lonSpan).toFloat() * width,
                            y = inset + ((maxLat - point.latitude) / latSpan).toFloat() * height,
                        )
                    }
                    val path = Path().apply {
                        moveTo(projected.first().x, projected.first().y)
                        projected.drop(1).forEach { lineTo(it.x, it.y) }
                        if (closeBoundary && projected.size >= 3) close()
                    }
                    if (closeBoundary && projected.size >= 3) drawPath(path, Color(0x554D8750))
                    drawPath(path, FarmColors.PrimaryForest, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    projected.forEachIndexed { index, point ->
                        drawCircle(if (index == 0) FarmColors.HarvestAmber else FarmColors.PrimaryForest, radius = if (index == 0) 7.dp.toPx() else 4.dp.toPx(), center = point)
                    }
                }
            }
            Text(
                text = if (useTelugu) "ఇది పాయింట్ల రేఖాచిత్రం మాత్రమే; మ్యాప్ కాదు." else "Sketch of recorded points only; this is not a map.",
                style = MaterialTheme.typography.labelSmall,
                color = FarmColors.SecondaryText,
            )
        }
    }
}

@Composable
private fun NoticeCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = FarmColors.WarningContainer)) {
        Text(text, modifier = Modifier.padding(12.dp), color = FarmColors.OnAmberContainer, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = FarmColors.SurfaceVariant)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = FarmColors.SecondaryText)
            Text(value, fontWeight = FontWeight.Bold, color = FarmColors.MainText)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = FarmColors.SecondaryText, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = FarmColors.MainText, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MeasurementHistoryRow(measurement: LandMeasurement, useTelugu: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = FarmColors.Surface)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(formatArea(measurement.areaMicroAcres, useTelugu), fontWeight = FontWeight.Bold, color = FarmColors.PrimaryForest)
            Text(
                text = buildString {
                    append(if (measurement.method == LandMeasurementMethod.GPS_BOUNDARY) if (useTelugu) "GPS కొలత" else "GPS walk" else if (useTelugu) "మాన్యువల్ కొలత" else "Manual")
                    append(" · ")
                    append(formatDate(measurement.completedAtEpochMillis))
                    if (measurement.appliedToPlot) append(if (useTelugu) " · మడి విస్తీర్ణం మార్చారు" else " · applied to plot")
                },
                style = MaterialTheme.typography.bodySmall,
                color = FarmColors.SecondaryText,
            )
        }
    }
}

private fun formatArea(microAcres: Long, useTelugu: Boolean): String {
    val (acres, guntas) = AreaConverter.toAcresAndGuntas(microAcres)
    return if (useTelugu) "$acres ఎకరాలు $guntas గుంటలు" else "$acres acres $guntas guntas"
}

private fun formatNumber(value: Double): String = String.format(java.util.Locale.US, "%.1f", value)

private fun formatDate(epochMillis: Long): String =
    runCatching {
        DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(epochMillis))
    }.getOrDefault("—")
