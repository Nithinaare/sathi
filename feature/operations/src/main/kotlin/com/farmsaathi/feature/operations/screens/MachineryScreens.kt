package com.farmsaathi.feature.operations.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.repository.StartMachinerySessionRequest
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.DropdownOption
import com.farmsaathi.core.designsystem.components.FarmAmountInput
import com.farmsaathi.core.designsystem.components.FarmDropdown
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.components.FarmNumberInput
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmRecordCard
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmStickyFooterBar
import com.farmsaathi.core.designsystem.components.FarmSummaryCard
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.AreaConverter
import com.farmsaathi.core.model.ContactType
import com.farmsaathi.core.model.FinancialCalculators
import com.farmsaathi.core.model.MachineryBillingMode
import com.farmsaathi.core.model.MachinerySessionStatus
import com.farmsaathi.core.model.MachineryUsageType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.feature.operations.viewmodel.MachineryViewModel
import com.farmsaathi.feature.operations.viewmodel.ManualMachineryLogRequest
import kotlinx.coroutines.delay

@Composable
fun MachineryListScreen(
    cropCycleId: Long?,
    viewModel: MachineryViewModel,
    onBack: () -> Unit,
    onStartTimer: (Long?) -> Unit,
    onOpenSession: (Long) -> Unit,
    onAddManualLog: (Long?) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val list =
        remember(state.sessions, cropCycleId) {
            if (cropCycleId == null) state.sessions else state.sessions.filter { it.cropCycleId == cropCycleId }
        }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.mach_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.MACHINERY_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.MACHINERY_HELP,
                onToggleAudio = { viewModel.toggleGuideAudio() },
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                    FarmPrimaryButton(
                        text = farmString(R.string.mach_start_btn),
                        leadingIcon = Icons.Rounded.PlayArrow,
                        onClick = { onStartTimer(cropCycleId) },
                        modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                    )
                    FarmSecondaryButton(
                        text = if (useTe) "పాత అద్దె చేత్తో రాయండి" else "Log Past Rental Manually",
                        useMutedOutline = true,
                        onClick = { onAddManualLog(cropCycleId) },
                        modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                    )
                }
            }
        },
    ) { padding ->
        if (list.isEmpty()) {
            FarmEmptyState(
                title = farmString(R.string.mach_title),
                message =
                if (useTe) {
                    "ట్రాక్టర్ లేదా హార్వెస్టర్ పని మొదలైనప్పుడు స్టార్ట్ నొక్కండి."
                } else {
                    "Tap Start Timer when machinery work begins."
                },
                icon = Icons.Rounded.Agriculture,
                imageRes = FarmImages.tractor,
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
                    list,
                    key = { it.id },
                    contentType = { "machinery_session_card" },
                ) { session ->
                    val running = session.status == MachinerySessionStatus.RUNNING
                    val stType =
                        when {
                            running -> FarmStatusType.WARNING
                            session.paymentStatus == PaymentStatus.PAID -> FarmStatusType.SUCCESS
                            session.paymentStatus == PaymentStatus.PARTIAL -> FarmStatusType.INFO
                            else -> FarmStatusType.WARNING
                        }
                    val stLabel =
                        if (running) {
                            if (useTe) "నడుస్తోంది" else "Running"
                        } else {
                            FarmLabels.paymentStatus(session.paymentStatus, lang)
                        }
                    FarmRecordCard(
                        title = "${session.machineName} • ${session.operationType}",
                        subtitle = viewModel.formatDateTime(session.startEpochMillis),
                        metadata = session.elapsedMinutes?.let { if (useTe) "$it నిమి." else "$it min" },
                        amountOrMetric = if (running) "⏱" else session.computedAmount.formatInr(),
                        statusType = stType,
                        statusLabel = stLabel,
                        leadingIcon = if (running) Icons.Rounded.Timer else Icons.Rounded.Agriculture,
                        leadingImageRes = if (running) null else FarmImages.tractor,
                        onClick = { onOpenSession(session.id) },
                        modifier = Modifier.defaultMinSize(minHeight = 96.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun MachineryStartScreen(
    initialCropCycleId: Long?,
    viewModel: MachineryViewModel,
    onBack: () -> Unit,
    onStarted: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var farmId by remember(state.farms) { mutableStateOf(state.prefs.activeFarmId ?: state.farms.firstOrNull()?.id) }
    var cropCycleId by remember(initialCropCycleId) { mutableStateOf(initialCropCycleId) }
    var machineName by remember { mutableStateOf(if (useTe) "ట్రాక్టర్" else "Tractor") }
    var operatorName by remember { mutableStateOf("") }
    var operationType by remember { mutableStateOf(if (useTe) "దున్నుడు" else "Ploughing") }
    var providerId by remember { mutableStateOf<Long?>(null) }
    var usageType by remember { mutableStateOf(MachineryUsageType.HIRED_IN) }
    var billingMode by remember { mutableStateOf(MachineryBillingMode.HOURLY) }
    var rateStr by remember { mutableStateOf("900") }
    var notes by remember { mutableStateOf("") }
    var errMsg by remember { mutableStateOf<String?>(null) }

    val machineryProviders =
        remember(state.contacts) {
            state.contacts.filter {
                it.contactType == ContactType.TRACTOR_OWNER || it.contactType == ContactType.HARVESTER_OWNER
            }
        }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.mach_start_btn),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.TRACTOR_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.TRACTOR_FORM_HELP,
                onToggleAudio = { viewModel.toggleAudio(AudioCueCatalog.TRACTOR_FORM_HELP) },
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.mach_start_btn),
                    leadingIcon = Icons.Rounded.PlayArrow,
                    isLoading = state.isSaving,
                    modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                    onClick = {
                        val fId = farmId ?: state.farms.firstOrNull()?.id
                        val rate = MoneyPaise.parseOrNull(rateStr)
                        if (fId == null || machineName.isBlank()) {
                            errMsg = if (useTe) "పొలం, యంత్రం పేరు అవసరం" else "Farm & machine name required"
                            viewModel.playErrorCue()
                        } else {
                            viewModel.startTimerSession(
                                StartMachinerySessionRequest(
                                    farmId = fId,
                                    plotId = null,
                                    cropCycleId = cropCycleId,
                                    providerContactId = providerId,
                                    machineName = machineName,
                                    operatorName = operatorName.ifBlank { null },
                                    operationType = operationType.ifBlank { "Work" },
                                    usageType = usageType,
                                    billingMode = billingMode,
                                    hourlyRate = if (billingMode == MachineryBillingMode.HOURLY) rate else null,
                                    perAcreRate = if (billingMode == MachineryBillingMode.PER_ACRE) rate else null,
                                    notes = notes.ifBlank { null },
                                ),
                                onStarted = onStarted,
                            )
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
            FarmDropdown(
                label = farmString(R.string.farms_title),
                options = state.farms.map { DropdownOption(it.id, it.name) },
                selectedValue = farmId,
                onSelect = { farmId = it.value },
                placeholder = if (useTe) "పొలం ఎంచుకోండి" else "Select Farm",
            )
            FarmTextInput(
                value = machineName,
                onValueChange = {
                    machineName = it
                    errMsg = null
                },
                label = if (useTe) "యంత్రం పేరు" else "Equipment Name",
            )
            FarmTextInput(
                value = operationType,
                onValueChange = { operationType = it },
                label = if (useTe) "పని రకం (దున్నుడు / కోత)" else "Operation Type",
            )
            if (machineryProviders.isNotEmpty()) {
                FarmDropdown(
                    label = if (useTe) "యజమాని (ఐచ్ఛికం)" else "Provider Contact",
                    options = machineryProviders.map { DropdownOption(it.id, it.name) },
                    selectedValue = providerId,
                    onSelect = { providerId = it.value },
                    placeholder = if (useTe) "ఎంచుకోండి" else "Select Contact",
                )
            }
            FarmDropdown(
                label = if (useTe) "అద్దె విధానం" else "Billing Mode",
                options =
                MachineryBillingMode.entries.map {
                    DropdownOption(it, FarmLabels.machineryBillingMode(it, lang))
                },
                selectedValue = billingMode,
                onSelect = { billingMode = it.value },
                placeholder = "",
            )
            if (billingMode != MachineryBillingMode.FIXED_FLAT) {
                FarmAmountInput(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label =
                    if (billingMode == MachineryBillingMode.HOURLY) {
                        if (useTe) "గంటకు రేటు (₹)" else "Hourly Rate (₹)"
                    } else {
                        if (useTe) "ఎకరాకు రేటు (₹)" else "Per-Acre Rate (₹)"
                    },
                )
            }
            FarmTextInput(
                value = operatorName,
                onValueChange = { operatorName = it },
                label = if (useTe) "డ్రైవర్ పేరు (ఐచ్ఛికం)" else "Operator Name (optional)",
            )
            (errMsg ?: state.errorMessage)?.let { Text(it, color = FarmColors.Error) }
        }
    }
}

@Composable
fun MachineryDetailScreen(
    sessionId: Long,
    viewModel: MachineryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session = state.sessions.firstOrNull { it.id == sessionId }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    if (session == null) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    val isRunning = session.status == MachinerySessionStatus.RUNNING
    var elapsedSecs by remember(session.id, isRunning) {
        mutableLongStateOf(if (isRunning) viewModel.computeElapsedSeconds(session) else (session.elapsedMinutes ?: 0L) * 60L)
    }
    LaunchedEffect(session.id, isRunning) {
        while (isRunning) {
            elapsedSecs = viewModel.computeElapsedSeconds(session)
            delay(1_000L)
        }
    }

    val minsNow = (elapsedSecs + 30L) / 60L
    var overrideMinutesStr by remember(session.id, isRunning) { mutableStateOf("") }
    var acresStr by remember(session.id) { mutableStateOf("") }
    var flatAmountStr by remember(session.id) { mutableStateOf("") }
    var paidAmountStr by remember(session.id) { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }

    val effectiveMins = overrideMinutesStr.toLongOrNull() ?: minsNow
    val previewCharge =
        FinancialCalculators.computeMachineryCharge(
            billingMode = session.billingMode,
            elapsedMinutes = effectiveMins,
            hourlyRate = session.hourlyRate ?: MoneyPaise.ZERO,
            perAcreRate = session.perAcreRate ?: MoneyPaise.ZERO,
            areaCoveredMicroAcres = acresStr.toBigDecimalOrNull()?.let { AreaConverter.fromDecimalAcres(it) } ?: 0L,
            flatAmount = MoneyPaise.parseOrNull(flatAmountStr) ?: MoneyPaise.ZERO,
        )

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = session.machineName,
                subtitle = session.operationType,
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.TRACTOR_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.TRACTOR_FORM_HELP,
                onToggleAudio = { viewModel.toggleAudio(AudioCueCatalog.TRACTOR_FORM_HELP) },
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            if (isRunning) {
                FarmStickyFooterBar {
                    Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                        FarmPrimaryButton(
                            text = farmString(R.string.mach_stop_btn),
                            leadingIcon = Icons.Rounded.Stop,
                            isDestructive = true,
                            isLoading = state.isSaving,
                            modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                            onClick = {
                                val areaMicro =
                                    acresStr.toBigDecimalOrNull()?.let {
                                        AreaConverter.fromDecimalAcres(it)
                                    }
                                viewModel.stopTimerSession(
                                    sessionId = session.id,
                                    overrideElapsedMinutes = overrideMinutesStr.toLongOrNull(),
                                    areaCoveredMicroAcres = areaMicro,
                                    flatAmount = MoneyPaise.parseOrNull(flatAmountStr),
                                    paidAmount = MoneyPaise.parseOrNull(paidAmountStr) ?: MoneyPaise.ZERO,
                                    paymentMode = paymentMode,
                                    notes = null,
                                ) { onBack() }
                            },
                        )
                        FarmSecondaryButton(
                            text = farmString(R.string.common_cancel),
                            useMutedOutline = true,
                            modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                            onClick = { viewModel.cancelTimer(session.id, onBack) },
                        )
                    }
                }
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
            val h = elapsedSecs / 3600
            val m = (elapsedSecs % 3600) / 60
            val s = elapsedSecs % 60
            FarmSummaryCard(
                title =
                if (isRunning) {
                    if (useTe) "నడుస్తున్న సమయం" else "Live Elapsed Time"
                } else {
                    if (useTe) "మొత్తం బిల్లు" else "Total Charge"
                },
                primaryValue = if (isRunning) "%02d:%02d:%02d".format(h, m, s) else session.computedAmount.formatInr(),
                secondaryMetrics =
                listOf(
                    (if (useTe) "నిమిషాలు" else "Minutes") to effectiveMins.toString(),
                    (if (useTe) "అంచనా బిల్లు" else "Charge") to (
                        if (isRunning) previewCharge.formatInr() else session.computedAmount.formatInr()
                        ),
                ),
            )

            if (isRunning) {
                Surface(
                    shape = FarmShapeTokens.card,
                    color = FarmColors.Surface,
                    border = BorderStroke(1.dp, FarmColors.Outline),
                ) {
                    Column(
                        modifier = Modifier.padding(FarmSpacing.dp16),
                        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                    ) {
                        if (session.billingMode == MachineryBillingMode.HOURLY) {
                            FarmNumberInput(
                                value = overrideMinutesStr,
                                onValueChange = { overrideMinutesStr = it },
                                label = if (useTe) "నిమిషాలు మార్చాలంటే రాయండి (ఐచ్ఛికం)" else "Adjust Minutes (optional)",
                                placeholder = minsNow.toString(),
                                allowDecimal = false,
                            )
                        } else if (session.billingMode == MachineryBillingMode.PER_ACRE) {
                            FarmNumberInput(
                                value = acresStr,
                                onValueChange = { acresStr = it },
                                label = if (useTe) "దున్నిన ఎకరాలు" else "Acres Covered",
                            )
                        } else {
                            FarmAmountInput(
                                value = flatAmountStr,
                                onValueChange = { flatAmountStr = it },
                                label = if (useTe) "మొత్తం బాడుగ (₹)" else "Total Flat Amount (₹)",
                            )
                        }
                        FarmAmountInput(
                            value = paidAmountStr,
                            onValueChange = { paidAmountStr = it },
                            label = if (useTe) "ఇప్పుడు ఇచ్చిన డబ్బు / అడ్వాన్స్ (₹)" else "Paid / Advance Now (₹)",
                        )
                        FarmDropdown(
                            label = if (useTe) "చెల్లింపు విధానం" else "Payment Mode",
                            options = PaymentMode.entries.map { DropdownOption(it, FarmLabels.paymentMode(it, lang)) },
                            selectedValue = paymentMode,
                            onSelect = { paymentMode = it.value },
                            placeholder = "",
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MachineryManualFormScreen(
    initialCropCycleId: Long?,
    viewModel: MachineryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var farmId by remember(state.farms) { mutableStateOf(state.prefs.activeFarmId ?: state.farms.firstOrNull()?.id) }
    var machineName by remember { mutableStateOf("") }
    var operationType by remember { mutableStateOf("") }
    var minsStr by remember { mutableStateOf("") }
    var totalStr by remember { mutableStateOf("") }
    var paidStr by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(PaymentMode.CASH) }
    var notes by remember { mutableStateOf("") }
    var errMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (useTe) "యంత్రం అద్దె రాయండి" else "Manual Machinery Log",
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.TRACTOR_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.TRACTOR_FORM_HELP,
                onToggleAudio = { viewModel.toggleAudio(AudioCueCatalog.TRACTOR_FORM_HELP) },
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.common_save),
                    isLoading = state.isSaving,
                    modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                    onClick = {
                        val fId = farmId ?: state.farms.firstOrNull()?.id
                        val total = MoneyPaise.parseOrNull(totalStr)
                        if (fId == null || machineName.isBlank() || total == null || total.isNegative()) {
                            errMsg = if (useTe) "వివరాలు సరిగా రాయండి" else "Enter valid details"
                            viewModel.playErrorCue()
                        } else {
                            errMsg = null
                            val paid = (MoneyPaise.parseOrNull(paidStr) ?: total).coerceIn(MoneyPaise.ZERO, total)
                            viewModel.saveManualLog(
                                ManualMachineryLogRequest(
                                    farmId = fId,
                                    cropCycleId = initialCropCycleId,
                                    machineName = machineName,
                                    operationType = operationType.ifBlank { "Field Work" },
                                    usageType = MachineryUsageType.HIRED_IN,
                                    billingMode = MachineryBillingMode.FIXED_FLAT,
                                    elapsedMinutes = minsStr.toLongOrNull(),
                                    totalAmount = total,
                                    paidAmount = paid,
                                    paymentMode = mode,
                                    notes = notes,
                                ),
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
            FarmDropdown(
                label = farmString(R.string.farms_title),
                options = state.farms.map { DropdownOption(it.id, it.name) },
                selectedValue = farmId,
                onSelect = { farmId = it.value },
                placeholder = if (useTe) "పొలం ఎంచుకోండి" else "Select Farm",
                modifier = Modifier.fillMaxWidth(),
            )
            FarmTextInput(
                value = machineName,
                onValueChange = {
                    machineName = it
                    errMsg = null
                },
                label = if (useTe) "యంత్రం పేరు (ట్రాక్టర్ / హార్వెస్టర్)" else "Machine Name",
            )
            FarmTextInput(
                value = operationType,
                onValueChange = { operationType = it },
                label = if (useTe) "చేసిన పని" else "Work Type",
            )
            FarmNumberInput(
                value = minsStr,
                onValueChange = { minsStr = it },
                label = if (useTe) "పని నిమిషాలు (ఐచ్ఛికం)" else "Duration (Minutes)",
                allowDecimal = false,
            )
            FarmAmountInput(
                value = totalStr,
                onValueChange = {
                    totalStr = it
                    errMsg = null
                },
                label = if (useTe) "మొత్తం బాడుగ (₹)" else "Total Rent (₹)",
            )
            FarmAmountInput(
                value = paidStr,
                onValueChange = { paidStr = it },
                label = if (useTe) "ఇచ్చిన డబ్బు (₹)" else "Paid Amount (₹)",
            )
            FarmDropdown(
                label = if (useTe) "చెల్లింపు విధానం" else "Payment Mode",
                options = PaymentMode.entries.map { DropdownOption(it, FarmLabels.paymentMode(it, lang)) },
                selectedValue = mode,
                onSelect = { mode = it.value },
                placeholder = "",
                modifier = Modifier.fillMaxWidth(),
            )
            FarmTextInput(
                value = notes,
                onValueChange = { notes = it },
                label = farmString(R.string.common_notes_optional),
                singleLine = false,
            )
            errMsg?.let { Text(it, color = FarmColors.Error, style = FarmTypography.bodyMedium) }
        }
    }
}
