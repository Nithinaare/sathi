package com.farmsaathi.feature.operations.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.DropdownOption
import com.farmsaathi.core.designsystem.components.FarmConfirmationDialog
import com.farmsaathi.core.designsystem.components.FarmDatePicker
import com.farmsaathi.core.designsystem.components.FarmDropdown
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmIconButton
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.components.FarmMoreDetailsAccordion
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmRecordCard
import com.farmsaathi.core.designsystem.components.FarmRupeeChipInput
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.components.FarmSectionHeader
import com.farmsaathi.core.designsystem.components.FarmStatusIndicator
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmStickyFooterBar
import com.farmsaathi.core.designsystem.components.FarmSummaryCard
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.components.FarmVoiceNameBadge
import com.farmsaathi.core.designsystem.components.rememberFarmPhotoLaunchers
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.designsystem.localization.farmCombinedClickable
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AttendanceStatus
import com.farmsaathi.core.model.FinancialCalculators
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.Worker
import com.farmsaathi.core.model.WorkerBalanceSummary
import com.farmsaathi.core.model.WorkerPaymentType
import com.farmsaathi.feature.operations.util.WageReceiptData
import com.farmsaathi.feature.operations.util.WageReceiptShareHelper
import com.farmsaathi.feature.operations.viewmodel.LabourHubMode
import com.farmsaathi.feature.operations.viewmodel.LabourViewModel
import com.farmsaathi.feature.operations.viewmodel.WorkerFormValidator
import kotlinx.coroutines.launch

@Composable
fun WorkerListScreen(
    viewModel: LabourViewModel,
    onBack: () -> Unit,
    onOpenDailyAttendance: () -> Unit,
    onOpenWorker: (Long) -> Unit,
    onAddWorker: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val useTe = state.prefs.language == AppLanguage.TELUGU
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultFarmId = state.prefs.activeFarmId ?: state.farms.firstOrNull()?.id ?: 0L
    val attByWorker = remember(state.dayAttendance) { state.dayAttendance.associateBy { it.workerId } }

    var pendingRecordWorker by remember { mutableStateOf<Worker?>(null) }
    var workerCount by remember { mutableIntStateOf(2) }
    var daysTenths by remember { mutableIntStateOf(10) }
    var rateRupeesStr by remember { mutableStateOf("400") }
    var advanceStr by remember { mutableStateOf("") }
    var showAdvance by remember { mutableStateOf(false) }
    var selectedWorkerId by remember(state.workers) {
        mutableStateOf(
            state.workers.firstOrNull { it.pendingBalance.isPositive() }?.worker?.id
                ?: state.workers.firstOrNull()?.worker?.id,
        )
    }

    var showCrewConfirm by remember { mutableStateOf(false) }
    var pendingSettleSummary by remember { mutableStateOf<WorkerBalanceSummary?>(null) }
    var activeReceipt by remember { mutableStateOf<WageReceiptData?>(null) }

    val ratePaise = MoneyPaise.parseOrNull(rateRupeesStr) ?: MoneyPaise.ZERO
    val advancePaise = MoneyPaise.parseOrNull(advanceStr) ?: MoneyPaise.ZERO
    val crewCalc =
        remember(workerCount, daysTenths, ratePaise, advancePaise) {
            FinancialCalculators.calculateQuickGroupWage(
                workerCount = workerCount,
                daysWorkedTenths = daysTenths,
                dailyRate = ratePaise,
                advancePaid = advancePaise,
            )
        }
    val selectedBalanceSummary =
        remember(state.workers, selectedWorkerId) {
            state.workers.firstOrNull { it.worker.id == selectedWorkerId }
        }
    val stickyShowsAddWorker =
        state.hubMode == LabourHubMode.WORKER_BALANCE &&
            (state.workers.isEmpty() || selectedBalanceSummary?.pendingBalance?.isPositive() != true)

    val micPermLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                pendingRecordWorker?.let { viewModel.toggleRecordWorkerVoiceName(it) }
            }
            pendingRecordWorker = null
        }

    Scaffold(
        containerColor = FarmColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.lab_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.LABOUR_HELP,
                isAudioPlaying = state.isAudioPlaying,
                onToggleAudio = { viewModel.toggleGuideAudio() },
                trailingAction = {
                    FilterChip(
                        selected = false,
                        onClick = onAddWorker,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = FarmColors.PrimaryForest,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = {
                            Text(
                                text = if (useTe) "కొత్త కూలీ" else "Add",
                                style = FarmTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = FarmColors.PrimaryForest,
                            )
                        },
                        colors =
                        FilterChipDefaults.filterChipColors(
                            containerColor = FarmColors.PrimaryContainer,
                            labelColor = FarmColors.PrimaryForest,
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                },
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                when {
                    state.hubMode == LabourHubMode.CREW_CALCULATOR -> {
                        val payableStr = crewCalc.netPayable.formatInr()
                        FarmPrimaryButton(
                            text =
                            if (useTe) {
                                "$payableStr ఖర్చులో సేవ్ చేయండి"
                            } else {
                                "Save $payableStr Expense"
                            },
                            enabled = crewCalc.netPayable.isPositive(),
                            isLoading = state.isSaving,
                            speakDescription =
                            if (useTe) {
                                "$payableStr కూలీ ఖర్చులో సేవ్ చేయండి"
                            } else {
                                "Save $payableStr as crew wage expense"
                            },
                            onClick = { showCrewConfirm = true },
                        )
                    }
                    selectedBalanceSummary != null && selectedBalanceSummary.pendingBalance.isPositive() -> {
                        val balStr = selectedBalanceSummary.pendingBalance.formatInr()
                        FarmPrimaryButton(
                            text =
                            if (useTe) {
                                "$balStr బాకీ చెల్లించండి"
                            } else {
                                "Pay & Settle $balStr"
                            },
                            isLoading = state.isSaving,
                            onClick = { pendingSettleSummary = selectedBalanceSummary },
                        )
                    }
                    else -> {
                        FarmPrimaryButton(
                            text = farmString(R.string.lab_add_worker),
                            leadingIcon = Icons.Rounded.Add,
                            speakDescription =
                            if (useTe) {
                                "కొత్త కూలీని ఫోటోతో సులువుగా కలపండి"
                            } else {
                                "Add a new worker with photo and daily wage"
                            },
                            onClick = onAddWorker,
                        )
                    }
                }
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
            item(key = "wage_calculator_hero", contentType = "wage_calculator_hero") {
                WageCalculatorHeroCard(
                    hubMode = state.hubMode,
                    onSelectHubMode = viewModel::selectHubMode,
                    workers = state.workers,
                    workerCount = workerCount,
                    onWorkerCountChange = { workerCount = it },
                    daysTenths = daysTenths,
                    onDaysTenthsChange = { daysTenths = it },
                    rateRupeesStr = rateRupeesStr,
                    onRateRupeesChange = { rateRupeesStr = it },
                    advanceStr = advanceStr,
                    onAdvanceChange = { advanceStr = it },
                    showAdvance = showAdvance,
                    onToggleAdvance = { showAdvance = !showAdvance },
                    netPayable = crewCalc.netPayable,
                    selectedWorkerId = selectedWorkerId,
                    onSelectWorker = { selectedWorkerId = it },
                    resolvePhoto = viewModel::resolvePhotoFile,
                    useTe = useTe,
                    onAddWorker = onAddWorker,
                )
            }

            item(key = "workers_roll_call_header", contentType = "section_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FarmSectionHeader(
                        title = if (useTe) "కూలీలు & హాజరు" else "Workers & Roll Call",
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = false,
                        onClick = onOpenDailyAttendance,
                        label = {
                            Text(
                                text = if (useTe) "రోజువారీ హాజరు" else "Date Roll Call",
                                style = FarmTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                        },
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                }
            }

            if (state.workers.isEmpty()) {
                item(key = "workers_empty_state", contentType = "empty_state") {
                    FarmEmptyState(
                        title = farmString(R.string.lab_title),
                        message =
                        if (useTe) {
                            "ఫోటో + రోజువారీ రేటుతో ఒక్క నిమిషంలో కూలీని కలపండి."
                        } else {
                            "Add a worker in seconds with a photo and daily wage rate."
                        },
                        icon = Icons.Rounded.Groups,
                        imageRes = FarmImages.labour,
                        actionLabel = if (stickyShowsAddWorker) null else farmString(R.string.lab_add_worker),
                        onActionClick = if (stickyShowsAddWorker) null else onAddWorker,
                    )
                }
            } else {
                item(key = "attendance_roll_call_controls", contentType = "roll_call_controls") {
                    AttendanceRollCallControls(
                        selectedDayEpochDay = state.selectedDayEpochDay,
                        todayEpochDay = state.todayEpochDay,
                        formattedDate = viewModel.formatEpochDay(state.selectedDayEpochDay),
                        selectedDateMillis = viewModel.epochDayToMillis(state.selectedDayEpochDay),
                        useTe = useTe,
                        onSelectDay = viewModel::selectDay,
                        onDatePickedMillis = { millis ->
                            viewModel.selectDay(viewModel.epochMillisToDay(millis))
                        },
                        onRepeatYesterday = {
                            viewModel.repeatYesterdayAttendance { count ->
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val msg =
                                        if (count > 0) {
                                            if (useTe) {
                                                "నిన్నటి $count గురు కూలీల హాజరు వేయబడింది"
                                            } else {
                                                "Repeated yesterday for $count workers"
                                            }
                                        } else {
                                            if (useTe) {
                                                "నిన్నటి హాజరు రికార్డు లేదు"
                                            } else {
                                                "No unmarked records from yesterday"
                                            }
                                        }
                                    snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                                }
                            }
                        },
                        onMarkAllFullDay = {
                            viewModel.markAllFullDay { count ->
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val msg =
                                        if (useTe) {
                                            "$count గురు కూలీలకు పూర్తి ఎండ వేయబడింది"
                                        } else {
                                            "Marked Full Day for $count workers"
                                        }
                                    snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                                }
                            }
                        },
                    )
                }

                items(
                    state.workers,
                    key = { it.worker.id },
                    contentType = { "worker_sun_stamp_card" },
                ) { summary ->
                    WorkerSunStampCard(
                        summary = summary,
                        currentStatus = attByWorker[summary.worker.id]?.status,
                        resolvePhoto = viewModel::resolvePhotoFile,
                        isPlayingVoice =
                        summary.worker.voiceNamePath != null &&
                            state.playingVoicePath == summary.worker.voiceNamePath,
                        isRecordingVoice = state.isRecordingVoiceNameForWorkerId == summary.worker.id,
                        useTe = useTe,
                        onOpenDetail = { onOpenWorker(summary.worker.id) },
                        onPlayVoice = { viewModel.togglePlayWorkerVoice(summary.worker) },
                        onRecordVoice = {
                            val ok =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED
                            if (ok) {
                                viewModel.toggleRecordWorkerVoiceName(summary.worker)
                            } else {
                                pendingRecordWorker = summary.worker
                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onMarkStatus = { status ->
                            viewModel.markAttendanceQuick(summary.worker, status, defaultFarmId)
                        },
                    )
                }
            }
        }
    }

    if (showCrewConfirm) {
        CrewPayoutConfirmDialog(
            workerCount = workerCount,
            daysLabel = formatTenthsFull(daysTenths, useTe),
            netPayable = crewCalc.netPayable,
            useTe = useTe,
            isSaving = state.isSaving,
            onConfirm = { mode ->
                showCrewConfirm = false
                viewModel.saveGroupWageExpense(
                    workerCount = workerCount,
                    daysTenths = daysTenths,
                    dailyRate = ratePaise,
                    advancePaid = advancePaise,
                    mode = mode,
                    notes = "",
                    onReceipt = { activeReceipt = it },
                ) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = if (useTe) "కూలీ ఖర్చు సేవ్ అయింది" else "Wage expense saved",
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            },
            onDismiss = { showCrewConfirm = false },
        )
    }

    pendingSettleSummary?.let { target ->
        WorkerSettleConfirmDialog(
            summary = target,
            useTe = useTe,
            isSaving = state.isSaving,
            onConfirm = { mode ->
                pendingSettleSummary = null
                viewModel.settleWorkerBalance(
                    summary = target,
                    amount = target.pendingBalance,
                    mode = mode,
                    resetDaysCounter = true,
                    onReceipt = { activeReceipt = it },
                ) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = if (useTe) "${target.worker.name} లెక్క సరిపోయింది" else "${target.worker.name} settled",
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            },
            onDismiss = { pendingSettleSummary = null },
        )
    }

    activeReceipt?.let { receipt ->
        WageReceiptDialog(
            receipt = receipt,
            resolvePhoto = viewModel::resolvePhotoFile,
            onSpeak = { viewModel.speakText(receipt.spokenSummary) },
            onShare = {
                scope.launch {
                    val photoFile = receipt.workerPhotoPath?.let(viewModel::resolvePhotoFile)
                    val res = WageReceiptShareHelper.shareReceipt(context, receipt, photoFile)
                    if (res.isFailure) {
                        snackbarHostState.showSnackbar(
                            message = if (useTe) "చీటీ షేర్ చేయడం కుదరలేదు" else "Unable to share receipt",
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            },
            onDismiss = { activeReceipt = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WageCalculatorHeroCard(
    hubMode: LabourHubMode,
    onSelectHubMode: (LabourHubMode) -> Unit,
    workers: List<WorkerBalanceSummary>,
    workerCount: Int,
    onWorkerCountChange: (Int) -> Unit,
    daysTenths: Int,
    onDaysTenthsChange: (Int) -> Unit,
    rateRupeesStr: String,
    onRateRupeesChange: (String) -> Unit,
    advanceStr: String,
    onAdvanceChange: (String) -> Unit,
    showAdvance: Boolean,
    onToggleAdvance: () -> Unit,
    netPayable: MoneyPaise,
    selectedWorkerId: Long?,
    onSelectWorker: (Long) -> Unit,
    resolvePhoto: (String) -> java.io.File,
    useTe: Boolean,
    onAddWorker: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(1.5.dp, FarmColors.PrimaryForest.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                Image(
                    painter = painterResource(FarmImages.wageCalc),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops =
                                arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.78f),
                                    1.0f to Color.Black.copy(alpha = 0.45f),
                                ),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = FarmSpacing.dp16),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Calculate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                    Text(
                        text = if (useTe) "కూలీల వేతన క్యాలిక్యులేటర్" else "Wage Calculator",
                        style = FarmTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(FarmSpacing.dp16),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                    FilterChip(
                        selected = hubMode == LabourHubMode.CREW_CALCULATOR,
                        onClick = { onSelectHubMode(LabourHubMode.CREW_CALCULATOR) },
                        label = {
                            Text(
                                text = if (useTe) "గుంపు లెక్క" else "Crew Calculator",
                                style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        },
                        colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmColors.PrimaryForest,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    )
                    FilterChip(
                        selected = hubMode == LabourHubMode.WORKER_BALANCE,
                        onClick = { onSelectHubMode(LabourHubMode.WORKER_BALANCE) },
                        enabled = true,
                        label = {
                            Text(
                                text = if (useTe) "కూలీ బాకీ" else "Worker Balance",
                                style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        },
                        colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmColors.PrimaryForest,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    )
                }

                if (hubMode == LabourHubMode.CREW_CALCULATOR) {
                    WorkerStepperRow(
                        label = if (useTe) "కూలీలు" else "Workers",
                        valueText = "$workerCount",
                        canDecrement = workerCount > 1,
                        onMinus = { if (workerCount > 1) onWorkerCountChange(workerCount - 1) },
                        onPlus = { onWorkerCountChange(workerCount + 1) },
                    )
                    SymmetricDaysStepper(
                        daysTenths = daysTenths,
                        useTe = useTe,
                        onChange = onDaysTenthsChange,
                    )

                    Text(
                        text = if (useTe) "రోజు కూలీ రేటు (₹)" else "Daily Rate (₹)",
                        style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = FarmColors.SecondaryText,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    ) {
                        listOf(300, 400, 500, 600).forEach { preset ->
                            FilterChip(
                                selected = rateRupeesStr == preset.toString(),
                                onClick = { onRateRupeesChange(preset.toString()) },
                                label = {
                                    Text(
                                        text = "₹$preset",
                                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    )
                                },
                                colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FarmColors.PrimaryContainer,
                                    selectedLabelColor = FarmColors.OnPrimaryContainer,
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            )
                        }
                    }
                    FarmRupeeChipInput(
                        value = rateRupeesStr,
                        onValueChange = onRateRupeesChange,
                        label = if (useTe) "లేదా రూపాయల నోట్లతో పెంచండి (₹)" else "Or add with note chips (₹)",
                        useTelugu = useTe,
                        presetAmounts = listOf(50, 100, 500),
                    )
                    FarmMoreDetailsAccordion(
                        expanded = showAdvance,
                        onToggle = onToggleAdvance,
                        useTelugu = useTe,
                        title = if (useTe) "అడ్వాన్స్ తీసివేయండి (ఐచ్ఛికం)" else "Deduct Advance (optional)",
                    ) {
                        FarmRupeeChipInput(
                            value = advanceStr,
                            onValueChange = onAdvanceChange,
                            label = if (useTe) "ముందే ఇచ్చిన అడ్వాన్స్ (₹)" else "Advance already paid (₹)",
                            useTelugu = useTe,
                            presetAmounts = listOf(50, 100, 500),
                        )
                    }

                    HorizontalDivider(color = FarmColors.Outline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (useTe) "ఇవ్వవలసిన మొత్తం" else "Net Payable",
                            style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = FarmColors.MainText,
                        )
                        Text(
                            text = netPayable.formatInr(),
                            style = FarmTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = FarmColors.PrimaryForest,
                        )
                    }
                } else if (workers.isEmpty()) {
                    EmptyWorkerBalanceCard(
                        useTe = useTe,
                        onAddWorker = onAddWorker,
                    )
                } else {
                    val selectedSummary =
                        remember(workers, selectedWorkerId) {
                            workers.firstOrNull { it.worker.id == selectedWorkerId }
                        }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                        items(
                            workers,
                            key = { "sel_${it.worker.id}" },
                            contentType = { "worker_balance_chip" },
                        ) { item ->
                            val isSel = item.worker.id == selectedWorkerId
                            FilterChip(
                                selected = isSel,
                                onClick = { onSelectWorker(item.worker.id) },
                                label = {
                                    Text(
                                        text = "${item.worker.name} (${item.pendingBalance.formatInr()})",
                                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                },
                                colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FarmColors.PrimaryContainer,
                                    selectedLabelColor = FarmColors.OnPrimaryContainer,
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            )
                        }
                    }
                    if (selectedSummary != null) {
                        val bal = selectedSummary.pendingBalance
                        val photoFile = selectedSummary.worker.photoPath?.let(resolvePhoto)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (photoFile != null && photoFile.exists()) {
                                AsyncImage(
                                    model = photoFile,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                    Modifier
                                        .size(52.dp)
                                        .clip(FarmShapeTokens.circular)
                                        .border(1.5.dp, FarmColors.PrimaryForest, FarmShapeTokens.circular),
                                )
                            } else {
                                Image(
                                    painter = painterResource(FarmImages.labour),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(52.dp).clip(FarmShapeTokens.circular),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedSummary.worker.name,
                                    style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = FarmColors.MainText,
                                )
                                Text(
                                    text =
                                    if (useTe) {
                                        "${formatTenths(selectedSummary.totalDaysWorkedTenths)} రోజుల బాకీ"
                                    } else {
                                        "${formatTenths(selectedSummary.totalDaysWorkedTenths)} days worked"
                                    },
                                    style = FarmTypography.bodyMedium,
                                    color = FarmColors.SecondaryText,
                                )
                            }
                            Text(
                                text = bal.formatInr(),
                                style = FarmTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (bal.isPositive()) FarmColors.PrimaryForest else FarmColors.Success,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkerBalanceCard(
    useTe: Boolean,
    onAddWorker: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FarmShapeTokens.inputField,
        color = FarmColors.SurfaceVariant,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(FarmSpacing.dp16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            Image(
                painter = painterResource(FarmImages.labour),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                Modifier
                    .size(64.dp)
                    .clip(FarmShapeTokens.circular)
                    .border(1.5.dp, FarmColors.PrimaryForest, FarmShapeTokens.circular),
            )
            Text(
                text =
                if (useTe) {
                    "కూలీల పేర్లు ఇంకా కలపలేదు — ముందు ఫోటోతో కలపండి"
                } else {
                    "No workers added yet — add one with a photo first"
                },
                style = FarmTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = FarmColors.MainText,
                textAlign = TextAlign.Center,
            )
            FarmPrimaryButton(
                text = farmString(R.string.lab_add_worker),
                leadingIcon = Icons.Rounded.Add,
                onClick = onAddWorker,
            )
        }
    }
}

@Composable
private fun WorkerStepperRow(
    label: String,
    valueText: String,
    canDecrement: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FarmColors.MainText,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FarmIconButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Decrease $label",
                containerColor = if (canDecrement) FarmColors.SurfaceVariant else FarmColors.SurfaceVariant.copy(alpha = 0.5f),
                enabled = canDecrement,
                onClick = onMinus,
            )
            Surface(
                shape = FarmShapeTokens.inputField,
                color = FarmColors.PrimaryContainer,
                modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 48.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(
                        text = valueText,
                        style = FarmTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = FarmColors.OnPrimaryContainer,
                    )
                }
            }
            FarmIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Increase $label",
                tint = Color.White,
                containerColor = FarmColors.PrimaryForest,
                onClick = onPlus,
            )
        }
    }
}

@Composable
private fun SymmetricDaysStepper(
    daysTenths: Int,
    useTe: Boolean,
    onChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
    ) {
        Text(
            text = if (useTe) "రోజులు" else "Days",
            style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FarmColors.MainText,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CounterChipButton(
                text = "-1",
                enabled = daysTenths > 0,
                containerColor = FarmColors.SurfaceVariant,
                contentColor = FarmColors.MainText,
                modifier = Modifier.weight(0.9f),
                onClick = { onChange((daysTenths - 10).coerceAtLeast(0)) },
            )
            CounterChipButton(
                text = "-0.5",
                enabled = daysTenths > 0,
                containerColor = FarmColors.SurfaceVariant,
                contentColor = FarmColors.MainText,
                modifier = Modifier.weight(1f),
                onClick = { onChange((daysTenths - 5).coerceAtLeast(0)) },
            )
            Surface(
                shape = FarmShapeTokens.inputField,
                color = FarmColors.PrimaryContainer,
                modifier = Modifier.weight(1.45f).defaultMinSize(minHeight = 48.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
                    Text(
                        text = formatTenthsFull(daysTenths, useTe),
                        style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = FarmColors.OnPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            CounterChipButton(
                text = "+0.5",
                containerColor = FarmColors.AmberContainer,
                contentColor = FarmColors.OnAmberContainer,
                modifier = Modifier.weight(1f),
                onClick = { onChange(daysTenths + 5) },
            )
            CounterChipButton(
                text = "+1",
                containerColor = FarmColors.PrimaryForest,
                contentColor = Color.White,
                modifier = Modifier.weight(0.9f),
                onClick = { onChange(daysTenths + 10) },
            )
        }
    }
}

@Composable
private fun CounterChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = FarmColors.PrimaryForest,
    contentColor: Color = Color.White,
) {
    Surface(
        modifier =
        modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 48.dp)
            .clip(FarmShapeTokens.inputField)
            .farmCombinedClickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = FarmShapeTokens.inputField,
        color = if (enabled) containerColor else FarmColors.SurfaceVariant.copy(alpha = 0.45f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
            Text(
                text = text,
                style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) contentColor else FarmColors.Disabled,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AttendanceRollCallControls(
    selectedDayEpochDay: Long,
    todayEpochDay: Long,
    formattedDate: String,
    selectedDateMillis: Long,
    useTe: Boolean,
    onSelectDay: (Long) -> Unit,
    onDatePickedMillis: (Long) -> Unit,
    onRepeatYesterday: () -> Unit,
    onMarkAllFullDay: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
        ) {
            FilterChip(
                selected = selectedDayEpochDay == todayEpochDay,
                onClick = { onSelectDay(todayEpochDay) },
                label = {
                    Text(
                        text = farmString(R.string.common_today),
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FarmColors.PrimaryForest,
                    selectedLabelColor = Color.White,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
            FilterChip(
                selected = selectedDayEpochDay == todayEpochDay - 1L,
                onClick = { onSelectDay(todayEpochDay - 1L) },
                label = {
                    Text(
                        text = farmString(R.string.common_yesterday),
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FarmColors.PrimaryForest,
                    selectedLabelColor = Color.White,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
            FilterChip(
                selected = selectedDayEpochDay < todayEpochDay - 1L || selectedDayEpochDay > todayEpochDay,
                onClick = { showDatePicker = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = {
                    Text(
                        text = if (useTe) "తేదీ: $formattedDate" else "Date: $formattedDate",
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
        ) {
            FilterChip(
                selected = false,
                onClick = onRepeatYesterday,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Replay,
                        contentDescription = null,
                        tint = FarmColors.PrimaryForest,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = {
                    Text(
                        text = if (useTe) "నిన్నటి కూలీలనే మళ్ళీ వేయండి" else "Repeat Yesterday's Crew",
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = FarmColors.PrimaryForest,
                    )
                },
                colors =
                FilterChipDefaults.filterChipColors(
                    containerColor = FarmColors.PrimaryContainer,
                    labelColor = FarmColors.OnPrimaryContainer,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
            FilterChip(
                selected = false,
                onClick = onMarkAllFullDay,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = FarmColors.OnAmberContainer,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = {
                    Text(
                        text = if (useTe) "అందరూ పూర్తి ఎండ" else "All Full Sun",
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = FarmColors.OnAmberContainer,
                    )
                },
                colors =
                FilterChipDefaults.filterChipColors(
                    containerColor = FarmColors.AmberContainer,
                    labelColor = FarmColors.OnAmberContainer,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onDatePickedMillis)
                        showDatePicker = false
                    },
                ) {
                    Text(farmString(R.string.common_ok), color = FarmColors.PrimaryForest)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(farmString(R.string.common_cancel), color = FarmColors.SecondaryText)
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun WorkerSunStampCard(
    summary: WorkerBalanceSummary,
    currentStatus: AttendanceStatus?,
    resolvePhoto: (String) -> java.io.File,
    isPlayingVoice: Boolean,
    isRecordingVoice: Boolean,
    useTe: Boolean,
    onOpenDetail: () -> Unit,
    onPlayVoice: () -> Unit,
    onRecordVoice: () -> Unit,
    onMarkStatus: (AttendanceStatus) -> Unit,
) {
    val worker = summary.worker
    val bal = summary.pendingBalance

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(FarmSpacing.dp12),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(FarmShapeTokens.inputField)
                    .farmCombinedClickable(role = Role.Button, onClick = onOpenDetail)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                val photoFile = worker.photoPath?.let(resolvePhoto)
                if (photoFile != null && photoFile.exists()) {
                    AsyncImage(
                        model = photoFile,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                        Modifier
                            .size(64.dp)
                            .clip(FarmShapeTokens.circular)
                            .border(2.dp, FarmColors.PrimaryForest, FarmShapeTokens.circular),
                    )
                } else {
                    Image(
                        painter = painterResource(FarmImages.labour),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                        Modifier
                            .size(64.dp)
                            .clip(FarmShapeTokens.circular)
                            .border(1.dp, FarmColors.Outline, FarmShapeTokens.circular),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = worker.name,
                        style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = FarmColors.MainText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${worker.defaultDailyWage.formatInr()} / ${if (useTe) "రోజు" else "day"}",
                        style = FarmTypography.bodyMedium,
                        color = FarmColors.SecondaryText,
                    )
                    Spacer(Modifier.height(4.dp))
                    FarmVoiceNameBadge(
                        hasVoiceRecording = !worker.voiceNamePath.isNullOrBlank(),
                        isPlaying = isPlayingVoice,
                        isRecording = isRecordingVoice,
                        onPlayClick = onPlayVoice,
                        onRecordClick = onRecordVoice,
                        useTelugu = useTe,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = bal.formatInr(),
                        style = FarmTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (bal.isPositive()) FarmColors.Warning else FarmColors.PrimaryForest,
                    )
                    FarmStatusIndicator(
                        type = if (bal.isPositive()) FarmStatusType.WARNING else FarmStatusType.SUCCESS,
                        label =
                        if (bal.isPositive()) {
                            if (useTe) "బాకీ" else "Due"
                        } else {
                            if (useTe) "సరి" else "OK"
                        },
                    )
                }
            }

            HorizontalDivider(color = FarmColors.Outline.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                SunStampButton(
                    label = if (useTe) "పూర్తి ఎండ" else "Full Sun",
                    icon = Icons.Rounded.WbSunny,
                    selected = currentStatus == AttendanceStatus.FULL_DAY,
                    selectedContainer = FarmColors.PrimaryForest,
                    selectedContent = Color.White,
                    unselectedIconTint = FarmColors.HarvestAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(AttendanceStatus.FULL_DAY) },
                )
                SunStampButton(
                    label = if (useTe) "సగం ఎండ" else "Half Sun",
                    icon = Icons.Rounded.Contrast,
                    selected = currentStatus == AttendanceStatus.HALF_DAY,
                    selectedContainer = FarmColors.HarvestAmber,
                    selectedContent = Color.White,
                    unselectedIconTint = FarmColors.HarvestAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(AttendanceStatus.HALF_DAY) },
                )
                SunStampButton(
                    label = if (useTe) "రాలేదు" else "Absent",
                    icon = Icons.Rounded.Cancel,
                    selected = currentStatus == AttendanceStatus.ABSENT,
                    selectedContainer = FarmColors.SecondaryText,
                    selectedContent = Color.White,
                    unselectedIconTint = FarmColors.SecondaryText,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(AttendanceStatus.ABSENT) },
                )
            }
        }
    }
}

@Composable
private fun SunStampButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedContainer: Color,
    selectedContent: Color,
    unselectedIconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) selectedContainer else FarmColors.SurfaceVariant
    val fg = if (selected) selectedContent else FarmColors.MainText
    val iconTint = if (selected) selectedContent else unselectedIconTint
    val borderColor = if (selected) selectedContainer else FarmColors.Outline

    Surface(
        modifier =
        modifier
            .defaultMinSize(minHeight = 56.dp)
            .clip(FarmShapeTokens.inputField)
            .border(if (selected) 2.dp else 1.dp, borderColor, FarmShapeTokens.inputField)
            .farmCombinedClickable(role = Role.Button, speakLabel = label, onClick = onClick),
        shape = FarmShapeTokens.inputField,
        color = bg,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = FarmTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyAttendanceScreen(
    viewModel: LabourViewModel,
    onBack: () -> Unit,
    onAddWorker: () -> Unit = {},
    onOpenWorkerList: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val useTe = state.prefs.language == AppLanguage.TELUGU
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultFarmId = state.prefs.activeFarmId ?: state.farms.firstOrNull()?.id ?: 0L
    val attByWorker = remember(state.dayAttendance) { state.dayAttendance.associateBy { it.workerId } }
    var pendingRecordWorker by remember { mutableStateOf<Worker?>(null) }

    val micPermLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                pendingRecordWorker?.let { viewModel.toggleRecordWorkerVoiceName(it) }
            }
            pendingRecordWorker = null
        }

    Scaffold(
        containerColor = FarmColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.lab_daily_attendance),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.LABOUR_HELP,
                isAudioPlaying = state.isAudioPlaying,
                onToggleAudio = { viewModel.toggleGuideAudio() },
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmSecondaryButton(
                        text = if (useTe) "క్యాలిక్యులేటర్" else "Calculator",
                        onClick = onOpenWorkerList,
                        modifier = Modifier.weight(1f),
                    )
                    FarmPrimaryButton(
                        text = farmString(R.string.lab_add_worker),
                        leadingIcon = Icons.Rounded.Add,
                        onClick = onAddWorker,
                        modifier = Modifier.weight(1f),
                    )
                }
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
            item(key = "attendance_date_picker", contentType = "date_picker") {
                FarmDatePicker(
                    selectedDateMillis = viewModel.epochDayToMillis(state.selectedDayEpochDay),
                    onDateSelected = { millis -> viewModel.selectDay(viewModel.epochMillisToDay(millis)) },
                    label = if (useTe) "తేదీ ఎంచుకోండి" else "Select Date",
                    formattedDate = viewModel.formatEpochDay(state.selectedDayEpochDay),
                    todayLabel = farmString(R.string.common_today),
                    yesterdayLabel = farmString(R.string.common_yesterday),
                )
            }
            if (state.workers.isEmpty()) {
                item(key = "daily_workers_empty_state", contentType = "empty_state") {
                    FarmEmptyState(
                        title = farmString(R.string.lab_daily_attendance),
                        message = if (useTe) "ముందు కూలీని కలపండి." else "Add a worker first.",
                        icon = Icons.Rounded.Groups,
                        imageRes = FarmImages.labour,
                        actionLabel = null,
                        onActionClick = null,
                    )
                }
            } else {
                item(key = "daily_bulk_actions", contentType = "bulk_action_chips") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.repeatYesterdayAttendance { count ->
                                    scope.launch {
                                        val msg =
                                            if (count > 0) {
                                                if (useTe) {
                                                    "నిన్నటి $count గురు కూలీల హాజరు వేయబడింది"
                                                } else {
                                                    "Repeated yesterday for $count workers"
                                                }
                                            } else {
                                                if (useTe) {
                                                    "నిన్నటి హాజరు రికార్డు లేదు"
                                                } else {
                                                    "No unmarked records from yesterday"
                                                }
                                            }
                                        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Replay,
                                    contentDescription = null,
                                    tint = FarmColors.PrimaryForest,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = if (useTe) "నిన్నటి కూలీలనే మళ్ళీ వేయండి" else "Repeat Yesterday's Crew",
                                    style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = FarmColors.PrimaryForest,
                                )
                            },
                            colors =
                            FilterChipDefaults.filterChipColors(
                                containerColor = FarmColors.PrimaryContainer,
                                labelColor = FarmColors.OnPrimaryContainer,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.markAllFullDay { count ->
                                    scope.launch {
                                        val msg =
                                            if (useTe) {
                                                "$count గురు కూలీలకు పూర్తి ఎండ వేయబడింది"
                                            } else {
                                                "Marked Full Day for $count workers"
                                            }
                                        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.WbSunny,
                                    contentDescription = null,
                                    tint = FarmColors.OnAmberContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = if (useTe) "అందరూ పూర్తి ఎండ" else "All Full Sun",
                                    style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = FarmColors.OnAmberContainer,
                                )
                            },
                            colors =
                            FilterChipDefaults.filterChipColors(
                                containerColor = FarmColors.AmberContainer,
                                labelColor = FarmColors.OnAmberContainer,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        )
                    }
                }
                items(
                    state.workers,
                    key = { it.worker.id },
                    contentType = { "worker_sun_stamp_card" },
                ) { summary ->
                    val w = summary.worker
                    WorkerSunStampCard(
                        summary = summary,
                        currentStatus = attByWorker[w.id]?.status,
                        resolvePhoto = viewModel::resolvePhotoFile,
                        isPlayingVoice =
                        w.voiceNamePath != null && state.playingVoicePath == w.voiceNamePath,
                        isRecordingVoice = state.isRecordingVoiceNameForWorkerId == w.id,
                        useTe = useTe,
                        onOpenDetail = onOpenWorkerList,
                        onPlayVoice = { viewModel.togglePlayWorkerVoice(w) },
                        onRecordVoice = {
                            val ok =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED
                            if (ok) {
                                viewModel.toggleRecordWorkerVoiceName(w)
                            } else {
                                pendingRecordWorker = w
                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onMarkStatus = { status ->
                            viewModel.markAttendanceQuick(w, status, defaultFarmId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun WorkerDetailScreen(
    workerId: Long,
    viewModel: LabourViewModel,
    onBack: () -> Unit,
    onEditWorker: (Long) -> Unit,
    onAddPayment: (Long, Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = state.workers.firstOrNull { it.worker.id == workerId }
    val payments by viewModel.observeWorkerPayments(workerId).collectAsStateWithLifecycle(emptyList())
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    var showArchive by remember { mutableStateOf(false) }

    if (summary == null) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = summary.worker.name,
                subtitle = summary.worker.phone ?: summary.worker.skillRole,
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                trailingAction = {
                    Row {
                        FarmIconButton(
                            icon = Icons.Rounded.Edit,
                            contentDescription = farmString(R.string.common_edit),
                            onClick = { onEditWorker(workerId) },
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
    ) { padding ->
        LazyColumn(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            item(key = "worker_photo_avatar", contentType = "worker_photo") {
                summary.worker.photoPath?.let { path ->
                    val file = viewModel.resolvePhotoFile(path)
                    if (file.exists()) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = file,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                Modifier
                                    .size(120.dp)
                                    .clip(FarmShapeTokens.circular)
                                    .border(2.dp, FarmColors.PrimaryForest, FarmShapeTokens.circular),
                            )
                        }
                    }
                }
            }
            item(key = "worker_balance_summary", contentType = "summary_card") {
                FarmSummaryCard(
                    title = if (useTe) "మిగిలిన బాకీ" else "Pending Balance",
                    primaryValue = summary.pendingBalance.formatInr(),
                    secondaryMetrics =
                    listOf(
                        (if (useTe) "రోజులు" else "Days") to formatTenths(summary.totalDaysWorkedTenths),
                        (if (useTe) "పని కూలీ" else "Earned") to summary.totalWageAccrued.formatInr(),
                        (if (useTe) "చెల్లించినది" else "Paid") to
                            (summary.totalAdvancesPaid + summary.totalSettlementsPaid).formatInr(),
                    ),
                )
            }
            item(key = "worker_payment_actions", contentType = "two_col_action_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmSecondaryButton(
                        text = if (useTe) "అడ్వాన్స్ ఇవ్వండి" else "Give Advance",
                        onClick = { onAddPayment(workerId, true) },
                        modifier = Modifier.weight(1f),
                    )
                    FarmPrimaryButton(
                        text = if (useTe) "కూలీ చెల్లించండి" else "Pay Wages",
                        onClick = { onAddPayment(workerId, false) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item(key = "worker_payments_header", contentType = "section_header") {
                FarmSectionHeader(title = if (useTe) "చెల్లింపుల చరిత్ర" else "Payments & Advances")
            }
            items(
                payments,
                key = { "p_${it.id}" },
                contentType = { "worker_payment_card" },
            ) { pay ->
                FarmRecordCard(
                    title = FarmLabels.workerPaymentType(pay.paymentType, lang),
                    subtitle = FarmLabels.paymentMode(pay.paymentMode, lang),
                    amountOrMetric = pay.amount.formatInr(),
                    onClick = {},
                )
            }
        }
    }

    if (showArchive) {
        FarmConfirmationDialog(
            title = if (useTe) "కూలీ పేరు పక్కన పెట్టాలా?" else "Archive worker?",
            message = if (useTe) "పాత కూలీ లెక్కలన్నీ భద్రంగా ఉంటాయి." else "Financial records remain preserved.",
            confirmLabel = farmString(R.string.common_confirm),
            dismissLabel = farmString(R.string.common_cancel),
            isDestructive = true,
            onConfirm = {
                showArchive = false
                viewModel.archiveWorker(workerId, onBack)
            },
            onDismiss = { showArchive = false },
        )
    }
}

private enum class HalfDayRateOption {
    HALF_OF_DAILY,
    FIXED_200,
    FIXED_250,
    CUSTOM,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkerFormScreen(
    workerId: Long?,
    viewModel: LabourViewModel,
    onBack: () -> Unit,
    initialPhotoPath: String? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing =
        remember(workerId, state.workers) {
            workerId?.let { id -> state.workers.firstOrNull { it.worker.id == id }?.worker }
        }
    val useTe = state.prefs.language == AppLanguage.TELUGU
    val context = LocalContext.current

    var quickMode by remember(existing) { mutableStateOf(existing == null) }
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var phone by remember(existing) { mutableStateOf(existing?.phone.orEmpty()) }
    var role by remember(existing) { mutableStateOf(existing?.skillRole.orEmpty()) }
    var wageStr by remember(existing) {
        mutableStateOf(existing?.defaultDailyWage?.toWholeRupeesRounded()?.toString() ?: "400")
    }
    var halfOption by remember(existing) {
        val paise = existing?.halfDayRatePaise
        val opt =
            when (paise) {
                null -> HalfDayRateOption.HALF_OF_DAILY
                20_000L -> HalfDayRateOption.FIXED_200
                25_000L -> HalfDayRateOption.FIXED_250
                else -> HalfDayRateOption.CUSTOM
            }
        mutableStateOf(opt)
    }
    var customHalfStr by remember(existing) {
        val paise = existing?.halfDayRatePaise
        mutableStateOf(if (paise != null) MoneyPaise(paise).toWholeRupeesRounded().toString() else "")
    }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var photoPath by remember(existing, initialPhotoPath) {
        mutableStateOf(existing?.photoPath ?: initialPhotoPath)
    }
    var voiceNamePath by remember(existing) { mutableStateOf(existing?.voiceNamePath) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var wageError by remember { mutableStateOf<String?>(null) }
    var halfDayRateError by remember { mutableStateOf<String?>(null) }

    val reqMsg = farmString(R.string.common_required_field)
    val invalidNumMsg = farmString(R.string.common_invalid_number)

    val photoLaunchers =
        rememberFarmPhotoLaunchers(
            createTempFile = viewModel::createTempCaptureFile,
            onCapturedFile = { f -> viewModel.saveCapturedPhoto(f) { photoPath = it } },
            onGalleryUri = { uri -> viewModel.importGalleryPhoto(uri) { photoPath = it } },
        )

    val micPermLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                viewModel.toggleRecordFormVoiceName { voiceNamePath = it }
            }
        }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.lab_add_worker),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.WORKER_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying,
                onToggleAudio = { viewModel.toggleGuideAudio(AudioCueCatalog.WORKER_FORM_HELP) },
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.common_save),
                    isLoading = state.isSaving,
                    onClick = {
                        val validation =
                            WorkerFormValidator.validate(
                                quickMode = quickMode,
                                name = name,
                                wageStr = wageStr,
                                useCustomHalfDayRate = halfOption == HalfDayRateOption.CUSTOM,
                                customHalfDayRateStr = customHalfStr,
                                requiredMessage = reqMsg,
                                invalidNumberMessage = invalidNumMsg,
                            )
                        nameError = validation.nameError
                        wageError = validation.wageError
                        halfDayRateError = validation.halfDayRateError

                        if (validation.isValid) {
                            val wage = MoneyPaise.parseOrNull(wageStr) ?: MoneyPaise.ZERO
                            val resolvedHalfPaise =
                                when (halfOption) {
                                    HalfDayRateOption.HALF_OF_DAILY -> null
                                    HalfDayRateOption.FIXED_200 -> 20_000L
                                    HalfDayRateOption.FIXED_250 -> 25_000L
                                    HalfDayRateOption.CUSTOM -> MoneyPaise.parseOrNull(customHalfStr)?.paise
                                }
                            viewModel.saveWorker(
                                existing = existing,
                                name = name,
                                phone = phone,
                                skillRole = role,
                                dailyWage = wage,
                                halfDayPercent = 50,
                                notes = notes,
                                photoPath = photoPath,
                                voiceNamePath = voiceNamePath,
                                halfDayRatePaise = resolvedHalfPaise,
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
                .padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp16)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                FilterChip(
                    selected = quickMode,
                    onClick = {
                        quickMode = true
                        nameError = null
                        halfDayRateError = null
                    },
                    label = {
                        Text(
                            text = if (useTe) "ఫోటో + రేటు" else "Photo + Rate",
                            style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmColors.PrimaryForest,
                        selectedLabelColor = Color.White,
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                )
                FilterChip(
                    selected = !quickMode,
                    onClick = { quickMode = false },
                    label = {
                        Text(
                            text = if (useTe) "పూర్తి వివరాలు" else "Full Form",
                            style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmColors.PrimaryForest,
                        selectedLabelColor = Color.White,
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                )
            }

            WorkerPhotoSectionCard(
                photoPath = photoPath,
                resolvePhoto = viewModel::resolvePhotoFile,
                useTe = useTe,
                onCameraClick = photoLaunchers.launchCamera,
                onGalleryClick = photoLaunchers.launchGallery,
            )

            WorkerVoiceSectionCard(
                hasRecording = !voiceNamePath.isNullOrBlank(),
                isPlaying = voiceNamePath != null && state.playingVoicePath == voiceNamePath,
                isRecording = state.isRecordingFormVoiceName,
                canListen = !voiceNamePath.isNullOrBlank() || name.isNotBlank(),
                useTe = useTe,
                onRecordClick = {
                    val ok =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                    if (ok) {
                        viewModel.toggleRecordFormVoiceName { voiceNamePath = it }
                    } else {
                        micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onPlayClick = { viewModel.togglePlayVoicePath(voiceNamePath, name) },
            )

            Text(
                text = if (useTe) "రోజువారీ కూలీ రేటు (₹)" else "Daily Wage Rate (₹)",
                style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = FarmColors.MainText,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                listOf(300, 400, 500, 600).forEach { preset ->
                    FilterChip(
                        selected = wageStr == preset.toString(),
                        onClick = {
                            wageStr = preset.toString()
                            wageError = null
                        },
                        label = { Text("₹$preset", style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    )
                }
            }
            FarmRupeeChipInput(
                value = wageStr,
                onValueChange = {
                    wageStr = it
                    wageError = null
                },
                label = if (useTe) "కూలీ రేటు (₹)" else "Daily Wage (₹)",
                useTelugu = useTe,
                presetAmounts = listOf(50, 100, 500),
                errorText = wageError,
            )

            FarmTextInput(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label =
                if (quickMode) {
                    if (useTe) "పేరు (ఖాళీగా ఉంచొచ్చు)" else "Name (optional — auto-named)"
                } else {
                    if (useTe) "కూలీ పేరు" else "Worker Name"
                },
                placeholder = if (useTe) "కూలీ #${state.workers.size + 1}" else "Worker #${state.workers.size + 1}",
                errorText = nameError,
            )

            if (!quickMode) {
                HalfDayRupeeSelector(
                    selectedOption = halfOption,
                    dailyWageStr = wageStr,
                    customHalfStr = customHalfStr,
                    customError = halfDayRateError,
                    useTe = useTe,
                    onSelectOption = { opt ->
                        halfOption = opt
                        halfDayRateError = null
                    },
                    onCustomChange = {
                        customHalfStr = it
                        halfDayRateError = null
                    },
                )
                FarmTextInput(
                    value = phone,
                    onValueChange = { phone = it },
                    label = if (useTe) "ఫోన్ నంబర్ (ఐచ్ఛికం)" else "Phone (optional)",
                )
                FarmTextInput(
                    value = role,
                    onValueChange = { role = it },
                    label = if (useTe) "పని నైపుణ్యం (ఐచ్ఛికం)" else "Role / Skill (optional)",
                )
                FarmTextInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = farmString(R.string.common_notes_optional),
                    singleLine = false,
                )
            }
            Spacer(Modifier.height(FarmSpacing.dp16))
        }
    }
}

@Composable
private fun WorkerPhotoSectionCard(
    photoPath: String?,
    resolvePhoto: (String) -> java.io.File,
    useTe: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(FarmSpacing.dp12),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            Text(
                text = if (useTe) "1. కూలీ ఫోటో" else "1. Worker Photo",
                style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = FarmColors.MainText,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                val file = photoPath?.let(resolvePhoto)
                if (file != null && file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                        Modifier
                            .size(76.dp)
                            .clip(FarmShapeTokens.circular)
                            .border(2.dp, FarmColors.PrimaryForest, FarmShapeTokens.circular),
                    )
                } else {
                    Image(
                        painter = painterResource(FarmImages.labour),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                        Modifier
                            .size(76.dp)
                            .clip(FarmShapeTokens.circular)
                            .border(1.dp, FarmColors.Outline, FarmShapeTokens.circular),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    Surface(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(FarmShapeTokens.inputField)
                            .farmCombinedClickable(role = Role.Button, onClick = onCameraClick),
                        shape = FarmShapeTokens.inputField,
                        color = FarmColors.PrimaryForest,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = FarmSpacing.dp12, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(FarmSpacing.dp8))
                            Text(
                                text = farmString(R.string.diary_take_photo),
                                style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                        }
                    }
                    Surface(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(FarmShapeTokens.inputField)
                            .border(1.dp, FarmColors.Outline, FarmShapeTokens.inputField)
                            .farmCombinedClickable(role = Role.Button, onClick = onGalleryClick),
                        shape = FarmShapeTokens.inputField,
                        color = FarmColors.PrimaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = FarmSpacing.dp12, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                tint = FarmColors.OnPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(FarmSpacing.dp8))
                            Text(
                                text = farmString(R.string.diary_pick_gallery),
                                style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = FarmColors.OnPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerVoiceSectionCard(
    hasRecording: Boolean,
    isPlaying: Boolean,
    isRecording: Boolean,
    canListen: Boolean,
    useTe: Boolean,
    onRecordClick: () -> Unit,
    onPlayClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(FarmSpacing.dp12),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (useTe) "2. మాటలో పేరు" else "2. Voice Name",
                    style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FarmColors.MainText,
                )
                if (hasRecording) {
                    FarmStatusIndicator(
                        type = FarmStatusType.SUCCESS,
                        label = if (useTe) "రికార్డ్ ఉంది" else "Recorded",
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                Surface(
                    modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(FarmShapeTokens.inputField)
                        .border(
                            1.dp,
                            if (isRecording) FarmColors.Error else FarmColors.Outline,
                            FarmShapeTokens.inputField,
                        )
                        .farmCombinedClickable(role = Role.Button, onClick = onRecordClick),
                    shape = FarmShapeTokens.inputField,
                    color = if (isRecording) FarmColors.ErrorContainer else FarmColors.SurfaceVariant,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = if (isRecording) FarmColors.Error else FarmColors.MainText,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text =
                            if (isRecording) {
                                if (useTe) "ఆపండి" else "Stop"
                            } else {
                                if (useTe) "పేరు రికార్డ్" else "Record Name"
                            },
                            style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isRecording) FarmColors.Error else FarmColors.MainText,
                        )
                    }
                }

                Surface(
                    modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(FarmShapeTokens.inputField)
                        .border(
                            1.dp,
                            if (isPlaying || hasRecording) FarmColors.PrimaryForest else FarmColors.Outline,
                            FarmShapeTokens.inputField,
                        )
                        .farmCombinedClickable(enabled = canListen, role = Role.Button, onClick = onPlayClick),
                    shape = FarmShapeTokens.inputField,
                    color =
                    when {
                        isPlaying -> FarmColors.PrimaryForest
                        canListen -> FarmColors.PrimaryContainer
                        else -> FarmColors.SurfaceVariant
                    },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            tint =
                            when {
                                isPlaying -> Color.White
                                canListen -> FarmColors.OnPrimaryContainer
                                else -> FarmColors.Disabled
                            },
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text =
                            if (isPlaying) {
                                if (useTe) "ఆపండి" else "Stop"
                            } else {
                                if (useTe) "వినండి" else "Listen"
                            },
                            style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color =
                            when {
                                isPlaying -> Color.White
                                canListen -> FarmColors.OnPrimaryContainer
                                else -> FarmColors.Disabled
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HalfDayRupeeSelector(
    selectedOption: HalfDayRateOption,
    dailyWageStr: String,
    customHalfStr: String,
    customError: String?,
    useTe: Boolean,
    onSelectOption: (HalfDayRateOption) -> Unit,
    onCustomChange: (String) -> Unit,
) {
    val computedHalf = (MoneyPaise.parseOrNull(dailyWageStr) ?: MoneyPaise.ZERO).percentage(50)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
    ) {
        Text(
            text = if (useTe) "సగం రోజు కూలీ (₹)" else "Half-Day Wage (₹)",
            style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = FarmColors.MainText,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
        ) {
            FilterChip(
                selected = selectedOption == HalfDayRateOption.HALF_OF_DAILY,
                onClick = { onSelectOption(HalfDayRateOption.HALF_OF_DAILY) },
                label = {
                    Text(
                        text = if (useTe) "సగం రేటు (${computedHalf.formatInr()})" else "Half (${computedHalf.formatInr()})",
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FarmColors.PrimaryForest,
                    selectedLabelColor = Color.White,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
            FilterChip(
                selected = selectedOption == HalfDayRateOption.FIXED_200,
                onClick = { onSelectOption(HalfDayRateOption.FIXED_200) },
                label = {
                    Text(
                        text = "₹200",
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FarmColors.PrimaryForest,
                    selectedLabelColor = Color.White,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
            FilterChip(
                selected = selectedOption == HalfDayRateOption.FIXED_250,
                onClick = { onSelectOption(HalfDayRateOption.FIXED_250) },
                label = {
                    Text(
                        text = "₹250",
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FarmColors.PrimaryForest,
                    selectedLabelColor = Color.White,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
            FilterChip(
                selected = selectedOption == HalfDayRateOption.CUSTOM,
                onClick = { onSelectOption(HalfDayRateOption.CUSTOM) },
                label = {
                    Text(
                        text = if (useTe) "ఇతర ₹" else "Custom ₹",
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FarmColors.PrimaryForest,
                    selectedLabelColor = Color.White,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
        }
        if (selectedOption == HalfDayRateOption.CUSTOM) {
            FarmRupeeChipInput(
                value = customHalfStr,
                onValueChange = onCustomChange,
                label = if (useTe) "సగం రోజు కూలీ మొత్తం (₹)" else "Custom Half-Day Amount (₹)",
                useTelugu = useTe,
                presetAmounts = listOf(50, 100, 200),
                errorText = customError,
            )
        }
    }
}

@Composable
fun WorkerPaymentFormScreen(
    workerId: Long,
    isAdvance: Boolean,
    viewModel: LabourViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val summary = state.workers.firstOrNull { it.worker.id == workerId }
    val farmId = state.prefs.activeFarmId ?: state.farms.firstOrNull()?.id ?: 0L

    var amountStr by remember(summary, isAdvance) {
        mutableStateOf(
            if (!isAdvance && summary != null && summary.pendingBalance.isPositive()) {
                summary.pendingBalance.toWholeRupeesRounded().toString()
            } else {
                ""
            },
        )
    }
    var mode by remember { mutableStateOf(PaymentMode.CASH) }
    var notes by remember { mutableStateOf("") }
    var showMore by remember { mutableStateOf(false) }
    var errMsg by remember { mutableStateOf<String?>(null) }
    var activeReceipt by remember { mutableStateOf<WageReceiptData?>(null) }
    val invalidAmt = farmString(R.string.common_invalid_number)

    Scaffold(
        containerColor = FarmColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FarmPageHeader(
                title =
                if (isAdvance) {
                    if (useTe) "అడ్వాన్స్ ఇవ్వండి" else "Record Advance"
                } else {
                    if (useTe) "కూలీ చెల్లింపు" else "Pay Wages"
                },
                subtitle = summary?.worker?.name,
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
                        val amt = MoneyPaise.parseOrNull(amountStr)
                        if (amt == null || !amt.isPositive()) {
                            errMsg = invalidAmt
                        } else {
                            viewModel.recordPayment(
                                workerId = workerId,
                                farmId = farmId,
                                cropCycleId = null,
                                paymentType = if (isAdvance) WorkerPaymentType.ADVANCE else WorkerPaymentType.SETTLEMENT,
                                amount = amt,
                                mode = mode,
                                notes = notes,
                                onReceipt = { activeReceipt = it },
                            ) {}
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
            FarmRupeeChipInput(
                value = amountStr,
                onValueChange = {
                    amountStr = it
                    errMsg = null
                },
                label = if (useTe) "మొత్తం (₹)" else "Amount (₹)",
                useTelugu = useTe,
                presetAmounts = listOf(50, 100, 500, 1000),
                errorText = errMsg,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                FilterChip(
                    selected = mode == PaymentMode.CASH,
                    onClick = { mode = PaymentMode.CASH },
                    label = {
                        Text(
                            text = if (useTe) "నగదు" else "Cash",
                            style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmColors.PrimaryForest,
                        selectedLabelColor = Color.White,
                    ),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                )
                FilterChip(
                    selected = mode == PaymentMode.UPI,
                    onClick = { mode = PaymentMode.UPI },
                    label = {
                        Text(
                            text = "UPI",
                            style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmColors.PrimaryForest,
                        selectedLabelColor = Color.White,
                    ),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                )
            }

            FarmMoreDetailsAccordion(
                expanded = showMore,
                onToggle = { showMore = !showMore },
                useTelugu = useTe,
            ) {
                FarmDropdown(
                    label = if (useTe) "విధానం" else "Payment Mode",
                    options = PaymentMode.entries.map { DropdownOption(it, FarmLabels.paymentMode(it, lang)) },
                    selectedValue = mode,
                    onSelect = { mode = it.value },
                    placeholder = "",
                )
                FarmTextInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = farmString(R.string.common_notes_optional),
                )
            }
            Spacer(Modifier.height(FarmSpacing.dp16))
        }
    }

    activeReceipt?.let { receipt ->
        WageReceiptDialog(
            receipt = receipt,
            resolvePhoto = viewModel::resolvePhotoFile,
            onSpeak = { viewModel.speakText(receipt.spokenSummary) },
            onShare = {
                scope.launch {
                    val photoFile = receipt.workerPhotoPath?.let(viewModel::resolvePhotoFile)
                    val res = WageReceiptShareHelper.shareReceipt(context, receipt, photoFile)
                    if (res.isFailure) {
                        snackbarHostState.showSnackbar(
                            message = if (useTe) "చీటీ షేర్ చేయడం కుదరలేదు" else "Unable to share receipt",
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            },
            onDismiss = {
                activeReceipt = null
                onBack()
            },
        )
    }
}

@Composable
private fun CrewPayoutConfirmDialog(
    workerCount: Int,
    daysLabel: String,
    netPayable: MoneyPaise,
    useTe: Boolean,
    isSaving: Boolean,
    onConfirm: (PaymentMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf(PaymentMode.CASH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FarmColors.Surface,
        shape = FarmShapeTokens.card,
        title = {
            Text(
                text = if (useTe) "కూలీ చెల్లింపు నిర్ధారణ" else "Confirm Crew Payout",
                style = FarmTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = FarmColors.MainText,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12)) {
                Surface(
                    shape = FarmShapeTokens.inputField,
                    color = FarmColors.PrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(FarmSpacing.dp12),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (useTe) "$workerCount గురు కూలీలు • $daysLabel" else "$workerCount Workers • $daysLabel",
                            style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = FarmColors.OnPrimaryContainer,
                        )
                        Text(
                            text = netPayable.formatInr(),
                            style = FarmTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = FarmColors.PrimaryForest,
                        )
                    }
                }
                Text(
                    text = if (useTe) "చెల్లింపు విధానం" else "Payment Mode",
                    style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = FarmColors.SecondaryText,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    FilterChip(
                        selected = mode == PaymentMode.CASH,
                        onClick = { mode = PaymentMode.CASH },
                        label = {
                            Text(
                                text = if (useTe) "నగదు" else "Cash",
                                style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        },
                        colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmColors.PrimaryForest,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    )
                    FilterChip(
                        selected = mode == PaymentMode.UPI,
                        onClick = { mode = PaymentMode.UPI },
                        label = {
                            Text(
                                text = "UPI",
                                style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        },
                        colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmColors.PrimaryForest,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    )
                }
            }
        },
        confirmButton = {
            FarmPrimaryButton(
                text = if (useTe) "నిర్ధారించి సేవ్ చేయండి" else "Confirm & Save",
                isLoading = isSaving,
                onClick = { onConfirm(mode) },
            )
        },
        dismissButton = {
            FarmSecondaryButton(
                text = farmString(R.string.common_cancel),
                useMutedOutline = true,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun WorkerSettleConfirmDialog(
    summary: WorkerBalanceSummary,
    useTe: Boolean,
    isSaving: Boolean,
    onConfirm: (PaymentMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf(PaymentMode.CASH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FarmColors.Surface,
        shape = FarmShapeTokens.card,
        title = {
            Text(
                text = if (useTe) "బాకీ చెల్లింపు నిర్ధారణ" else "Confirm Settlement",
                style = FarmTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = FarmColors.MainText,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12)) {
                Surface(
                    shape = FarmShapeTokens.inputField,
                    color = FarmColors.PrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(FarmSpacing.dp12),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text =
                            "${summary.worker.name} • ${formatTenthsFull(summary.totalDaysWorkedTenths, useTe)}",
                            style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = FarmColors.OnPrimaryContainer,
                        )
                        Text(
                            text = summary.pendingBalance.formatInr(),
                            style = FarmTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = FarmColors.PrimaryForest,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    FilterChip(
                        selected = mode == PaymentMode.CASH,
                        onClick = { mode = PaymentMode.CASH },
                        label = {
                            Text(
                                text = if (useTe) "నగదు" else "Cash",
                                style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        },
                        colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmColors.PrimaryForest,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    )
                    FilterChip(
                        selected = mode == PaymentMode.UPI,
                        onClick = { mode = PaymentMode.UPI },
                        label = {
                            Text(
                                text = "UPI",
                                style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        },
                        colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmColors.PrimaryForest,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    )
                }
            }
        },
        confirmButton = {
            FarmPrimaryButton(
                text = if (useTe) "చెల్లించి సేవ్ చేయండి" else "Pay & Save",
                isLoading = isSaving,
                onClick = { onConfirm(mode) },
            )
        },
        dismissButton = {
            FarmSecondaryButton(
                text = farmString(R.string.common_cancel),
                useMutedOutline = true,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun WageReceiptDialog(
    receipt: WageReceiptData,
    resolvePhoto: (String) -> java.io.File,
    onSpeak: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val useTe = receipt.useTelugu
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FarmColors.Surface,
        shape = FarmShapeTokens.card,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = receipt.title,
                        style = FarmTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = FarmColors.PrimaryForest,
                    )
                    Text(
                        text = receipt.dateFormatted,
                        style = FarmTypography.bodySmall,
                        color = FarmColors.SecondaryText,
                    )
                }
                FarmIconButton(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (useTe) "వినండి" else "Speak",
                    containerColor = FarmColors.PrimaryContainer,
                    tint = FarmColors.OnPrimaryContainer,
                    onClick = onSpeak,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    val photoFile = receipt.workerPhotoPath?.let(resolvePhoto)
                    if (photoFile != null && photoFile.exists()) {
                        AsyncImage(
                            model = photoFile,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                            Modifier
                                .size(56.dp)
                                .clip(FarmShapeTokens.circular)
                                .border(2.dp, FarmColors.PrimaryForest, FarmShapeTokens.circular),
                        )
                    } else {
                        Box(
                            modifier =
                            Modifier
                                .size(56.dp)
                                .clip(FarmShapeTokens.circular)
                                .background(FarmColors.PrimaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = null,
                                tint = FarmColors.PrimaryForest,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = receipt.subtitle,
                            style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = FarmColors.MainText,
                        )
                        Text(
                            text = receipt.paymentModeLabel,
                            style = FarmTypography.bodyMedium,
                            color = FarmColors.SecondaryText,
                        )
                    }
                }

                ReceiptSunTallyRow(
                    fullSunCount = receipt.fullSunCount,
                    halfSunCount = receipt.halfSunCount,
                    daysLabel = receipt.daysLabel,
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = FarmShapeTokens.inputField,
                    color = FarmColors.SuccessContainer,
                    border = BorderStroke(1.dp, FarmColors.PrimaryForest),
                ) {
                    Column(
                        modifier = Modifier.padding(FarmSpacing.dp12),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (useTe) "చెల్లించిన మొత్తం" else "Paid Amount",
                            style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = FarmColors.OnPrimaryContainer,
                        )
                        Text(
                            text = receipt.paidAmount.formatInr(),
                            style = FarmTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = FarmColors.PrimaryForest,
                        )
                    }
                }

                val hasDue = receipt.remainingBalance.isPositive()
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = FarmShapeTokens.inputField,
                    color = if (hasDue) FarmColors.WarningContainer else FarmColors.PrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FarmSpacing.dp12, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!hasDue) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = FarmColors.Success,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text =
                            if (hasDue) {
                                if (useTe) {
                                    "మిగిలిన బాకీ: ${receipt.remainingBalance.formatInr()}"
                                } else {
                                    "Remaining Due: ${receipt.remainingBalance.formatInr()}"
                                }
                            } else {
                                if (useTe) "బాకీ సున్నా" else "Zero Balance"
                            },
                            style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (hasDue) FarmColors.Warning else FarmColors.Success,
                        )
                    }
                }
            }
        },
        confirmButton = {
            FarmPrimaryButton(
                text = if (useTe) "చీటీ పంపండి" else "Share Receipt",
                leadingIcon = Icons.Rounded.Share,
                onClick = onShare,
            )
        },
        dismissButton = {
            FarmSecondaryButton(
                text = farmString(R.string.common_ok),
                useMutedOutline = true,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun ReceiptSunTallyRow(
    fullSunCount: Int,
    halfSunCount: Int,
    daysLabel: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FarmShapeTokens.inputField,
        color = FarmColors.SurfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = FarmSpacing.dp12, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val maxIcons = 5
                repeat(fullSunCount.coerceAtMost(maxIcons)) {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = FarmColors.HarvestAmber,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (fullSunCount > maxIcons) {
                    Text(
                        text = "+${fullSunCount - maxIcons}",
                        style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = FarmColors.Warning,
                    )
                }
                if (halfSunCount > 0) {
                    Icon(
                        imageVector = Icons.Rounded.Contrast,
                        contentDescription = null,
                        tint = FarmColors.HarvestAmber,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (fullSunCount == 0 && halfSunCount == 0) {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = FarmColors.Disabled,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = daysLabel,
                style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = FarmColors.MainText,
            )
        }
    }
}

private fun formatTenths(tenths: Int): String =
    if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.5"

private fun formatTenthsFull(
    tenths: Int,
    useTe: Boolean,
): String {
    val dec = if (tenths % 10 == 0) "${tenths / 10}.0" else "${tenths / 10}.5"
    return if (useTe) "$dec రోజు" else "$dec day"
}
