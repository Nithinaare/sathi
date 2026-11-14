package com.farmsaathi.feature.finance.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.DropdownOption
import com.farmsaathi.core.designsystem.components.FarmActionTile
import com.farmsaathi.core.designsystem.components.FarmAmountInput
import com.farmsaathi.core.designsystem.components.FarmAudioPlayer
import com.farmsaathi.core.designsystem.components.FarmConfirmationDialog
import com.farmsaathi.core.designsystem.components.FarmDatePicker
import com.farmsaathi.core.designsystem.components.FarmDropdown
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmImageGridPicker
import com.farmsaathi.core.designsystem.components.FarmImagePickerOption
import com.farmsaathi.core.designsystem.components.FarmListItem
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.components.FarmMoreDetailsAccordion
import com.farmsaathi.core.designsystem.components.FarmNumberInput
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPermissionExplanation
import com.farmsaathi.core.designsystem.components.FarmPhotoCapture
import com.farmsaathi.core.designsystem.components.FarmPhotoViewer
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmRecordCard
import com.farmsaathi.core.designsystem.components.FarmSearchField
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.components.FarmSectionHeader
import com.farmsaathi.core.designsystem.components.FarmStatusIndicator
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmStickyFooterBar
import com.farmsaathi.core.designsystem.components.FarmSummaryCard
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.components.FarmVisualGridTile
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
import com.farmsaathi.core.model.LedgerCategory
import com.farmsaathi.core.model.LedgerEntry
import com.farmsaathi.core.model.LedgerEntryType
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.PaymentStatus
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.feature.finance.viewmodel.FinanceUiState
import com.farmsaathi.feature.finance.viewmodel.FinanceViewModel
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsHubScreen(
    viewModel: FinanceViewModel,
    onAddExpense: () -> Unit,
    onAddSale: () -> Unit,
    onOpenLabour: () -> Unit,
    onOpenMachinery: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenEntryDetail: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    if (state.isLoading) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    val chipColors =
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = FarmColors.PrimaryContainer,
            selectedLabelColor = FarmColors.OnPrimaryContainer,
        )

    Column(modifier = Modifier.fillMaxSize()) {
        FarmPageHeader(
            title = farmString(R.string.acc_title),
            audioCueKey = FinanceViewModel.ACCOUNTS_CUE,
            isAudioPlaying = state.isAudioPlaying && state.activeCueKey == FinanceViewModel.ACCOUNTS_CUE,
            onToggleAudio = viewModel::toggleAudio,
        )
        HorizontalDivider(thickness = 1.dp, color = FarmColors.Outline)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            contentPadding = PaddingValues(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            item(key = "finance_summary_card", contentType = "summary_card") {
                val totals = state.totals
                FarmSummaryCard(
                    title = farmString(R.string.home_season_card),
                    primaryLabel = farmString(R.string.home_net_short),
                    primaryValue = totals.netBalance.formatInr(),
                    secondaryMetrics =
                    listOf(
                        farmString(R.string.home_expense_short) to totals.totalExpense.formatInr(),
                        farmString(R.string.home_income_short) to totals.totalIncome.formatInr(),
                    ),
                )
            }
            item(key = "action_row_expense_sale", contentType = "two_col_action_row") {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmVisualGridTile(
                        title = farmString(R.string.home_tile_expense),
                        icon = Icons.Rounded.Payments,
                        imageRes = FarmImages.expense,
                        fullBleedImage = true,
                        borderColor = FarmColors.Terracotta,
                        speakDescription =
                        if (useTe) {
                            "కొత్త ఖర్చు రాయండి. విత్తనాలు, ఎరువులు లేదా మందులకు పెట్టిన ఖర్చు ఇక్కడ రాయొచ్చు."
                        } else {
                            "Add a new farm expense."
                        },
                        onClick = onAddExpense,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    FarmVisualGridTile(
                        title = farmString(R.string.home_tile_sale),
                        icon = Icons.Rounded.AccountBalanceWallet,
                        imageRes = FarmImages.income,
                        fullBleedImage = true,
                        borderColor = FarmColors.PrimaryForest,
                        speakDescription =
                        if (useTe) {
                            "కొత్త అమ్మకం రాయండి. పంట అమ్మితే వచ్చిన ఆదాయం, రావలసిన బాకీ ఇక్కడ రాయొచ్చు."
                        } else {
                            "Record crop harvest income and dues."
                        },
                        onClick = onAddSale,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
            item(key = "action_row_labour_tractor", contentType = "two_col_action_row") {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmVisualGridTile(
                        title = farmString(R.string.home_tile_labour),
                        icon = Icons.Rounded.Groups,
                        imageRes = FarmImages.wageCalc,
                        fullBleedImage = true,
                        borderColor = FarmColors.HarvestAmber,
                        speakDescription =
                        if (useTe) {
                            "కూలీల లెక్క. కూలీల రోజుల కౌంటర్, వేతన క్యాలిక్యులేటర్ ఇక్కడ ఉంటాయి."
                        } else {
                            "Labour hub. Track day counters, wage calculator, and payouts."
                        },
                        onClick = onOpenLabour,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    FarmVisualGridTile(
                        title = farmString(R.string.home_tile_tractor),
                        icon = Icons.Rounded.Agriculture,
                        imageRes = FarmImages.tractor,
                        fullBleedImage = true,
                        borderColor = FarmColors.Terracotta,
                        speakDescription =
                        if (useTe) {
                            "యంత్రాల అద్దె. ట్రాక్టర్, హార్వెస్టర్ పని గంటలు, అద్దె బాకీల వివరాలు చూడొచ్చు."
                        } else {
                            "Machinery hire logs and live rental timer."
                        },
                        onClick = onOpenMachinery,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
            item(key = "reports_action_tile", contentType = "action_tile") {
                FarmActionTile(
                    title = farmString(R.string.rep_title),
                    subtitle = if (useTe) "పంటల వారీ పూర్తి ఆదాయ, ఖర్చు లెక్క" else "Crop-wise profit & loss summary",
                    icon = Icons.Rounded.Assessment,
                    imageRes = FarmImages.reports,
                    compact = true,
                    speakDescription =
                    if (useTe) {
                        "లాభ నష్టాల నివేదిక. ప్రతి పంటకు ఎంత ఖర్చు అయింది, ఎంత ఆదాయం వచ్చింది ఇక్కడ చూడొచ్చు."
                    } else {
                        "Profit and Loss reports for each crop."
                    },
                    onClick = onOpenReports,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "unified_ledger_controls", contentType = "ledger_controls") {
                FarmSectionHeader(title = if (useTe) "అన్ని పద్దులు (ఖాతా పుస్తకం)" else "Unified Ledger")
                Spacer(Modifier.height(FarmSpacing.dp8))
                FarmSearchField(
                    query = state.searchQuery,
                    onQueryChange = viewModel::setSearch,
                    placeholder = farmString(R.string.common_search),
                )
                Spacer(Modifier.height(FarmSpacing.dp8))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    FilterChip(
                        selected = state.filterType == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text(farmString(R.string.common_all), style = FarmTypography.bodySmall) },
                        colors = chipColors,
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                    FilterChip(
                        selected = state.filterType == LedgerEntryType.EXPENSE,
                        onClick = { viewModel.setFilter(LedgerEntryType.EXPENSE) },
                        label = { Text(farmString(R.string.home_expense_short), style = FarmTypography.bodySmall) },
                        colors = chipColors,
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                    FilterChip(
                        selected = state.filterType == LedgerEntryType.INCOME,
                        onClick = { viewModel.setFilter(LedgerEntryType.INCOME) },
                        label = { Text(farmString(R.string.home_income_short), style = FarmTypography.bodySmall) },
                        colors = chipColors,
                        modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                    )
                }
            }
            if (state.ledgerEntries.isEmpty()) {
                item(key = "ledger_empty_state", contentType = "empty_state") {
                    FarmEmptyState(
                        title = farmString(R.string.acc_title),
                        message = if (useTe) "ఇంకా ఖర్చులు లేదా ఆదాయం రాయలేదు." else "No financial entries yet.",
                        icon = Icons.Rounded.Payments,
                        imageRes = FarmImages.expense,
                        actionLabel = farmString(R.string.exp_add_btn),
                        onActionClick = onAddExpense,
                    )
                }
            } else {
                items(
                    state.ledgerEntries,
                    key = { it.id },
                    contentType = { "ledger_entry_card" },
                ) { entry ->
                    val isIncome = entry.entryType == LedgerEntryType.INCOME
                    val categoryLabel = FarmLabels.ledgerCategory(entry.category, lang)
                    val cleanTitle = entry.title.trim()
                    val cleanNote = entry.notes?.trim()?.takeIf { it.isNotBlank() }
                    val isDuplicateTitle =
                        cleanTitle.isBlank() || cleanTitle.equals(categoryLabel, ignoreCase = true)
                    val displayTitle = if (isDuplicateTitle) categoryLabel else cleanTitle
                    val displaySubtitle = if (isDuplicateTitle) cleanNote else categoryLabel
                    val formattedDate = viewModel.formatDate(entry.transactionDateEpochMillis)
                    val displayMetadata =
                        if (!isDuplicateTitle && cleanNote != null) {
                            "$formattedDate • $cleanNote"
                        } else {
                            formattedDate
                        }

                    FarmRecordCard(
                        title = displayTitle,
                        subtitle = displaySubtitle,
                        metadata = displayMetadata,
                        leadingImageRes = FarmImages.forLedgerCategory(entry.category),
                        amountOrMetric = (if (isIncome) "+" else "-") + entry.totalAmount.formatInr(),
                        metricColor = if (isIncome) FarmColors.Success else FarmColors.MainText,
                        statusType = ledgerStatusType(entry.paymentStatus),
                        statusLabel = FarmLabels.paymentStatus(entry.paymentStatus, lang),
                        hasPhoto = entry.receiptPhotoPath != null,
                        hasAudio = entry.voiceNotePath != null,
                        onClick = { onOpenEntryDetail(entry.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseFormScreen(
    initialCropCycleId: Long?,
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    initialPhotoPath: String? = null,
    initialDateMillis: Long? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var farmId by remember(state.farms) { mutableStateOf(state.prefs.activeFarmId ?: state.farms.firstOrNull()?.id) }
    var plotId by remember { mutableStateOf<Long?>(null) }
    var cropCycleId by remember(initialCropCycleId) { mutableStateOf(initialCropCycleId) }
    var category by remember { mutableStateOf(LedgerCategory.SEEDS) }
    var amountStr by remember { mutableStateOf("") }
    var isPaidFull by remember { mutableStateOf(true) }
    var paidStr by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var vendor by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dateMillis by remember(initialDateMillis) {
        mutableLongStateOf(initialDateMillis ?: System.currentTimeMillis())
    }
    var exifDetected by remember(initialDateMillis) { mutableStateOf(initialDateMillis != null) }
    var photoPath by remember(initialPhotoPath) { mutableStateOf(initialPhotoPath) }
    var showMore by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var previewPhoto by remember { mutableStateOf<String?>(null) }

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

    val categoryOptions =
        remember(lang) {
            EXPENSE_GRID_CATEGORIES.map { cat ->
                FarmImagePickerOption(
                    value = cat,
                    label = FarmLabels.ledgerCategory(cat, lang),
                    imageRes = FarmImages.forLedgerCategory(cat),
                )
            }
        }

    val invalidAmt = farmString(R.string.common_invalid_number)

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.exp_add_btn),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.EXPENSE_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.EXPENSE_FORM_HELP,
                onToggleAudio = viewModel::toggleAudio,
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.common_save),
                    isLoading = state.isSaving,
                    onClick = {
                        val total = MoneyPaise.parseOrNull(amountStr)
                        if (total == null || !total.isPositive()) {
                            amountError = invalidAmt
                        } else {
                            val paid =
                                if (isPaidFull) {
                                    total
                                } else {
                                    (MoneyPaise.parseOrNull(paidStr) ?: MoneyPaise.ZERO).coerceIn(MoneyPaise.ZERO, total)
                                }
                            viewModel.saveExpense(
                                farmId = farmId ?: state.farms.firstOrNull()?.id ?: 0L,
                                plotId = plotId,
                                cropCycleId = cropCycleId,
                                category = category,
                                description = description.ifBlank { FarmLabels.ledgerCategory(category, lang) },
                                vendor = vendor,
                                totalAmount = total,
                                paidAmount = paid,
                                paymentMode = paymentMode,
                                dateMillis = dateMillis,
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
                .clipToBounds()
                .verticalScroll(rememberScrollState())
                .padding(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            FarmImageGridPicker(
                label = if (useTe) "1. దేని కోసం ఖర్చు?" else "1. Select Category",
                options = categoryOptions,
                selected = category,
                onSelect = { category = it },
                columns = 3,
            )

            ExpenseRupeeAmountSection(
                value = amountStr,
                onValueChange = {
                    amountStr = it
                    amountError = null
                },
                label = if (useTe) "2. ఖర్చు మొత్తం (₹)" else "2. Total Amount (₹)",
                useTelugu = useTe,
                errorText = amountError,
            )

            if (state.crops.isNotEmpty()) {
                FarmImageGridPicker(
                    label = if (useTe) "పంట (ఐచ్ఛికం)" else "Crop (optional)",
                    options =
                    state.crops.map { crop ->
                        FarmImagePickerOption<Long?>(
                            value = crop.id,
                            label = crop.cropName,
                            imageRes = FarmImages.forCropName(crop.cropName),
                        )
                    } +
                        FarmImagePickerOption<Long?>(
                            value = null,
                            label = if (useTe) "అన్నింటికీ" else "General",
                            imageRes = FarmImages.farmField,
                        ),
                    selected = cropCycleId,
                    onSelect = { cropCycleId = it },
                    columns = 3,
                )
            }

            FarmPhotoCapture(
                photoPaths = listOfNotNull(photoPath),
                onTakePhotoClick = photoLaunchers.launchCamera,
                onPickFromGalleryClick = photoLaunchers.launchGallery,
                onRemovePhotoClick = {
                    photoPath = null
                    exifDetected = false
                },
                onPhotoClick = { previewPhoto = it },
                title = if (useTe) "3. బిల్లు ఫోటో (తేదీ ఆటోగా వస్తుంది)" else "3. Bill photo (auto date)",
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
                                "బిల్లు ఫోటో సమయం: ${viewModel.formatDateTime(dateMillis)}"
                            } else {
                                "Photo EXIF time: ${viewModel.formatDateTime(dateMillis)}"
                            },
                            style = FarmTypography.bodyMedium,
                            color = FarmColors.OnPrimaryContainer,
                        )
                    }
                }
            }

            FarmMoreDetailsAccordion(
                expanded = showMore,
                onToggle = { showMore = !showMore },
                useTelugu = useTe,
            ) {
                if (state.farms.size > 1) {
                    FarmDropdown(
                        label = farmString(R.string.farms_title),
                        options = state.farms.map { DropdownOption(it.id, it.name) },
                        selectedValue = farmId,
                        onSelect = { farmId = it.value },
                        placeholder = if (useTe) "పొలం ఎంచుకోండి" else "Select Farm",
                    )
                }
                FarmDatePicker(
                    selectedDateMillis = dateMillis,
                    onDateSelected = {
                        dateMillis = it
                        exifDetected = false
                    },
                    label = if (useTe) "తేదీ" else "Date",
                    formattedDate = viewModel.formatDate(dateMillis),
                    todayLabel = farmString(R.string.common_today),
                    yesterdayLabel = farmString(R.string.common_yesterday),
                )
                FarmDropdown(
                    label = if (useTe) "చెల్లింపు విధానం" else "Payment Mode",
                    options = PaymentMode.entries.map { DropdownOption(it, FarmLabels.paymentMode(it, lang)) },
                    selectedValue = paymentMode,
                    onSelect = { paymentMode = it.value },
                    placeholder = "",
                )
                FarmTextInput(
                    value = description,
                    onValueChange = { description = it },
                    label = if (useTe) "వివరం" else "Description",
                    placeholder = FarmLabels.ledgerCategory(category, lang),
                )
                FarmTextInput(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = if (useTe) "షాప్ / ఎవరికి ఇచ్చారు" else "Shop / Vendor",
                )
                Surface(
                    shape = FarmShapeTokens.inputField,
                    color = FarmColors.SurfaceVariant,
                    border = BorderStroke(1.dp, FarmColors.Outline),
                ) {
                    FarmListItem(
                        title = if (useTe) "మొత్తం డబ్బు ఇచ్చేశారా?" else "Paid in full?",
                        checked = isPaidFull,
                        onCheckedChange = { isPaidFull = it },
                        showDivider = false,
                    )
                }
                if (!isPaidFull) {
                    FarmAmountInput(
                        value = paidStr,
                        onValueChange = { paidStr = it },
                        label = if (useTe) "ఇప్పటికి ఇచ్చిన మొత్తం (₹)" else "Amount paid so far (₹)",
                        helperText = if (useTe) "మిగిలిన మొత్తం బాకీగా సేవ్ అవుతుంది" else "Remaining balance is saved as due",
                    )
                    Spacer(Modifier.height(FarmSpacing.dp8))
                }
                FinanceVoiceNoteSection(
                    state = state,
                    viewModel = viewModel,
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

    previewPhoto?.let { path ->
        FarmPhotoViewer(photoFile = viewModel.resolvePhotoFile(path), onDismiss = { previewPhoto = null })
    }
}

@Composable
fun SaleFormScreen(
    initialCropCycleId: Long?,
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var selectedCropId by remember(initialCropCycleId, state.crops) {
        mutableStateOf(initialCropCycleId ?: state.crops.firstOrNull()?.id)
    }
    var selectedStorageId by remember { mutableStateOf<Long?>(null) }
    var buyerName by remember { mutableStateOf("") }
    var mandi by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(QuantityUnit.QUINTAL) }
    var rateStr by remember { mutableStateOf("") }
    var deductionsStr by remember { mutableStateOf("") }
    var receivedStr by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }
    var showSaleMore by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val qtyMilli =
        remember(qtyStr) {
            qtyStr.toBigDecimalOrNull()?.multiply(BigDecimal(1_000))?.toLong() ?: 0L
        }
    val ratePaise = remember(rateStr) { MoneyPaise.parseOrNull(rateStr) ?: MoneyPaise.ZERO }
    val dedPaise = remember(deductionsStr) { MoneyPaise.parseOrNull(deductionsStr) ?: MoneyPaise.ZERO }
    val grossPaise = MoneyPaise.multiplyMilliUnits(ratePaise, qtyMilli)
    val netPaise = (grossPaise - dedPaise).let { if (it.isNegative()) MoneyPaise.ZERO else it }

    val selectedCrop = state.crops.firstOrNull { it.id == selectedCropId }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.sale_add_btn),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.SALE_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.SALE_FORM_HELP,
                onToggleAudio = viewModel::toggleAudio,
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.common_save),
                    isLoading = state.isSaving,
                    onClick = {
                        val crop = selectedCrop
                        val fallbackFarmId = state.prefs.activeFarmId ?: state.farms.firstOrNull()?.id
                        when {
                            qtyMilli <= 0L -> errorMsg = if (useTe) "సరైన పరిమాణం రాయండి" else "Enter valid quantity"
                            !ratePaise.isPositive() -> errorMsg = if (useTe) "సరైన ధర రాయండి" else "Enter valid rate"
                            crop == null -> {
                                val rec = MoneyPaise.parseOrNull(receivedStr) ?: netPaise
                                if (!netPaise.isPositive()) {
                                    errorMsg = if (useTe) "సరైన ధర రాయండి" else "Enter valid rate"
                                } else {
                                    viewModel.saveDirectFarmSale(
                                        farmId = fallbackFarmId ?: 0L,
                                        buyerName = buyerName.ifBlank { if (useTe) "పంట అమ్మకం" else "Crop Sale" },
                                        amount = netPaise,
                                        receivedAmount = rec,
                                        paymentMode = paymentMode,
                                        dateMillis = dateMillis,
                                        notes = notes,
                                    ) { onBack() }
                                }
                            }
                            else -> {
                                val rec = MoneyPaise.parseOrNull(receivedStr) ?: netPaise
                                viewModel.saveCropSale(
                                    cropCycle = crop,
                                    storageId = selectedStorageId,
                                    buyerName = buyerName.ifBlank { if (useTe) "పంట అమ్మకం" else "Crop Sale" },
                                    mandiLocation = mandi,
                                    qtyMilliUnits = qtyMilli,
                                    unit = unit,
                                    ratePerUnit = ratePaise,
                                    deductions = dedPaise,
                                    amountReceived = rec,
                                    paymentMode = paymentMode,
                                    dateMillis = dateMillis,
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
                .clipToBounds()
                .verticalScroll(rememberScrollState())
                .padding(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            if (state.crops.isNotEmpty()) {
                FarmImageGridPicker(
                    label = farmString(R.string.crops_title),
                    options =
                    state.crops.map { crop ->
                        FarmImagePickerOption<Long?>(
                            value = crop.id,
                            label = crop.cropName,
                            imageRes = FarmImages.forCropName(crop.cropName),
                        )
                    } +
                        FarmImagePickerOption<Long?>(
                            value = null,
                            label = if (useTe) "సాధారణం" else "General",
                            imageRes = FarmImages.farmField,
                        ),
                    selected = selectedCropId,
                    onSelect = { selectedCropId = it },
                    columns = 3,
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
                        errorMsg = null
                    },
                    label = if (useTe) "పరిమాణం" else "Quantity",
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
            FarmAmountInput(
                value = rateStr,
                onValueChange = {
                    rateStr = it
                    errorMsg = null
                },
                label = if (useTe) "ఒక్కో దానికి ధర (₹)" else "Rate per unit (₹)",
            )
            FarmSummaryCard(
                title = if (useTe) "మొత్తం అమ్మకం విలువ" else "Net Sale Total",
                primaryValue = netPaise.formatInr(),
                secondaryMetrics =
                listOf(
                    (if (useTe) "స్థూల విలువ" else "Gross") to grossPaise.formatInr(),
                    (if (useTe) "తగ్గింపు" else "Deductions") to dedPaise.formatInr(),
                ),
            )
            FarmMoreDetailsAccordion(
                expanded = showSaleMore,
                onToggle = { showSaleMore = !showSaleMore },
                useTelugu = useTe,
            ) {
                FarmDatePicker(
                    selectedDateMillis = dateMillis,
                    onDateSelected = { dateMillis = it },
                    label = if (useTe) "అమ్మిన తేదీ" else "Sale Date",
                    formattedDate = viewModel.formatDate(dateMillis),
                    todayLabel = farmString(R.string.common_today),
                    yesterdayLabel = farmString(R.string.common_yesterday),
                )
                FarmDropdown(
                    label = if (useTe) "చెల్లింపు విధానం" else "Payment Mode",
                    options = PaymentMode.entries.map { DropdownOption(it, FarmLabels.paymentMode(it, lang)) },
                    selectedValue = paymentMode,
                    onSelect = { paymentMode = it.value },
                    placeholder = "",
                )
                FarmTextInput(
                    value = buyerName,
                    onValueChange = {
                        buyerName = it
                        errorMsg = null
                    },
                    label = if (useTe) "కొనుగోలుదారు / వ్యాపారి (ఐచ్ఛికం)" else "Buyer / Trader (optional)",
                )
                FarmAmountInput(
                    value = deductionsStr,
                    onValueChange = { deductionsStr = it },
                    label = if (useTe) "తరుగు / హమాలీ తగ్గింపు (₹)" else "Mandi / Hamali Deductions (₹)",
                )
                FarmAmountInput(
                    value = receivedStr,
                    onValueChange = { receivedStr = it },
                    label = if (useTe) "ఇప్పటికి చేతికి అందిన డబ్బు (₹)" else "Amount Received Now (₹)",
                    helperText = if (useTe) "ఖాళీగా ఉంచితే మొత్తం అందినట్లు సేవ్ అవుతుంది" else "Leave blank if paid in full",
                )
                Spacer(Modifier.height(FarmSpacing.dp8))
                FinanceVoiceNoteSection(
                    state = state,
                    viewModel = viewModel,
                )
                FarmTextInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = farmString(R.string.common_notes_optional),
                    singleLine = false,
                )
            }
            errorMsg?.let { Text(it, style = FarmTypography.bodyMedium, color = FarmColors.Error) }
        }
    }
}

@Composable
fun LedgerDetailScreen(
    ledgerEntryId: Long,
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val entry = state.ledgerEntries.firstOrNull { it.id == ledgerEntryId }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    var showSettleDialog by remember { mutableStateOf(false) }
    var showVoidConfirm by remember { mutableStateOf(false) }
    var settleAmtStr by remember { mutableStateOf("") }
    var settleMode by remember { mutableStateOf(PaymentMode.CASH) }
    var settleError by remember { mutableStateOf<String?>(null) }
    var previewPhoto by remember { mutableStateOf<String?>(null) }

    if (entry == null) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    val categoryLabel = FarmLabels.ledgerCategory(entry.category, lang)
    val cleanTitle = entry.title.trim()
    val isShortOrDuplicate =
        cleanTitle.length <= 2 || cleanTitle.equals(categoryLabel, ignoreCase = true)
    val headerTitle = if (isShortOrDuplicate) categoryLabel else cleanTitle
    val headerSubtitle =
        if (isShortOrDuplicate) {
            viewModel.formatDate(entry.transactionDateEpochMillis)
        } else {
            categoryLabel
        }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = headerTitle,
                subtitle = headerSubtitle,
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.LEDGER_DETAIL_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.LEDGER_DETAIL_HELP,
                onToggleAudio = viewModel::toggleAudio,
            )
        },
        bottomBar = {
            if (!entry.isVoided && entry.balanceDue.isPositive()) {
                FarmStickyFooterBar {
                    FarmPrimaryButton(
                        text = if (useTe) "బాకీ డబ్బు నమోదు చేయండి" else "Record Settlement",
                        onClick = {
                            settleAmtStr = entry.balanceDue.toWholeRupeesRounded().toString()
                            settleMode = entry.paymentMode
                            settleError = null
                            showSettleDialog = true
                        },
                    )
                }
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
            if (entry.isVoided) {
                FarmStatusIndicator(
                    type = FarmStatusType.ERROR,
                    label = if (useTe) "రద్దు చేయబడిన పద్దు" else "Voided Entry",
                )
            }
            FarmSummaryCard(
                title = categoryLabel,
                primaryLabel = FarmLabels.paymentStatus(entry.paymentStatus, lang),
                primaryValue = entry.totalAmount.formatInr(),
                status = ledgerStatusType(entry.paymentStatus) to FarmLabels.paymentStatus(entry.paymentStatus, lang),
                secondaryMetrics =
                listOf(
                    (if (useTe) "అందినది / ఇచ్చినది" else "Settled") to entry.settledAmount.formatInr(),
                    (if (useTe) "బాకీ" else "Balance") to entry.balanceDue.formatInr(),
                ),
            )
            LedgerDetailInfoCard(
                entry = entry,
                categoryLabel = categoryLabel,
                formattedDate = viewModel.formatDate(entry.transactionDateEpochMillis),
                paymentModeLabel = FarmLabels.paymentMode(entry.paymentMode, lang),
                useTelugu = useTe,
            )
            entry.receiptPhotoPath?.let { path ->
                FarmSecondaryButton(
                    text = if (useTe) "బిల్లు ఫోటో చూడండి" else "View Bill Photo",
                    onClick = { previewPhoto = path },
                )
            }
            entry.voiceNotePath?.let { path ->
                val playing = state.playingPath == path
                FarmAudioPlayer(
                    isPlaying = playing,
                    currentPositionMillis = if (playing) state.playbackPosMs else 0L,
                    totalDurationMillis = if (playing) state.playbackDurMs else 0L,
                    onPlayPauseClick = { viewModel.togglePlayVoice(path, state.playbackDurMs) },
                    onSeekTo = viewModel::seekVoice,
                    playContentDescription = farmString(R.string.common_listen),
                    pauseContentDescription = farmString(R.string.common_stop_audio),
                )
            }
            if (!entry.isVoided) {
                FarmSecondaryButton(
                    text = if (useTe) "ఈ పద్దు రద్దు చేయండి (Void)" else "Void This Entry",
                    leadingIcon = Icons.Rounded.Delete,
                    useMutedOutline = true,
                    onClick = { showVoidConfirm = true },
                )
            }
        }
    }

    if (showSettleDialog) {
        val invalidSettleMsg = farmString(R.string.common_invalid_number)
        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            containerColor = FarmColors.Surface,
            shape = FarmShapeTokens.card,
            title = {
                Text(
                    text = if (useTe) "బాకీ చెల్లింపు" else "Record Payment",
                    style = FarmTypography.titleLarge,
                    color = FarmColors.MainText,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12)) {
                    FarmAmountInput(
                        value = settleAmtStr,
                        onValueChange = {
                            settleAmtStr = it
                            settleError = null
                        },
                        label = if (useTe) "మొత్తం (₹)" else "Amount (₹)",
                        errorText = settleError,
                    )
                    FarmDropdown(
                        label = if (useTe) "చెల్లింపు విధానం" else "Payment Mode",
                        options = PaymentMode.entries.map { DropdownOption(it, FarmLabels.paymentMode(it, lang)) },
                        selectedValue = settleMode,
                        onSelect = { settleMode = it.value },
                        placeholder = "",
                    )
                }
            },
            confirmButton = {
                FarmPrimaryButton(
                    text = farmString(R.string.common_save),
                    onClick = {
                        val amt = MoneyPaise.parseOrNull(settleAmtStr)
                        if (amt == null || !amt.isPositive()) {
                            settleError = invalidSettleMsg
                        } else {
                            showSettleDialog = false
                            viewModel.recordSettlement(entry.id, amt, settleMode) {}
                        }
                    },
                )
            },
            dismissButton = {
                FarmSecondaryButton(
                    text = farmString(R.string.common_cancel),
                    useMutedOutline = true,
                    onClick = { showSettleDialog = false },
                )
            },
        )
    }

    if (showVoidConfirm) {
        FarmConfirmationDialog(
            title = if (useTe) "ఈ పద్దు రద్దు చేయాలా?" else "Void ledger entry?",
            message = if (useTe) "లెక్కల్లో నుండి తీసేస్తాము కానీ ఆడిట్ చరిత్ర ఉంటుంది." else "Excluded from totals; audit trail kept.",
            confirmLabel = farmString(R.string.common_confirm),
            dismissLabel = farmString(R.string.common_cancel),
            isDestructive = true,
            onConfirm = {
                showVoidConfirm = false
                viewModel.voidEntry(entry.id, "Voided from detail") { onBack() }
            },
            onDismiss = { showVoidConfirm = false },
        )
    }

    previewPhoto?.let { path ->
        FarmPhotoViewer(photoFile = viewModel.resolvePhotoFile(path), onDismiss = { previewPhoto = null })
    }
}

@Composable
private fun ExpenseRupeeAmountSection(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    useTelugu: Boolean,
    errorText: String?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FarmAmountInput(
            value = value,
            onValueChange = onValueChange,
            label = label,
            errorText = errorText,
        )
        Spacer(Modifier.height(FarmSpacing.dp8))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
        ) {
            QUICK_RUPEE_CHIPS.forEach { step ->
                Surface(
                    modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = FarmDimensions.minTouchTarget)
                        .clip(FarmShapeTokens.tag)
                        .clickable {
                            val current = value.toDoubleOrNull()?.toLong() ?: 0L
                            onValueChange((current + step).toString())
                        }
                        .semantics { role = Role.Button },
                    shape = FarmShapeTokens.tag,
                    color = FarmColors.PrimaryContainer,
                    border = BorderStroke(1.dp, FarmColors.PrimaryForest.copy(alpha = 0.25f)),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = FarmSpacing.dp4, vertical = FarmSpacing.dp8),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+₹$step",
                            style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = FarmColors.OnPrimaryContainer,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.height(FarmSpacing.dp8))
            FilterChip(
                selected = false,
                onClick = { onValueChange("") },
                label = {
                    Text(
                        text = if (useTelugu) "సున్నా" else "Clear",
                        style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
            )
        }
    }
}

@Composable
private fun FinanceVoiceNoteSection(
    state: FinanceUiState,
    viewModel: FinanceViewModel,
) {
    val context = LocalContext.current
    var showMicExplainer by remember { mutableStateOf(false) }

    val micPermLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            showMicExplainer = false
            if (granted) viewModel.startVoiceNote()
        }

    if (showMicExplainer) {
        FarmPermissionExplanation(
            title = farmString(R.string.perm_mic_title),
            explanation = farmString(R.string.perm_mic_msg),
            icon = Icons.Rounded.Mic,
            grantLabel = farmString(R.string.perm_grant),
            denyLabel = farmString(R.string.perm_deny),
            onGrantClick = { micPermLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onDenyClick = { showMicExplainer = false },
        )
    } else {
        FarmVoiceRecorder(
            state = state.recordingState,
            onStartRecording = {
                val ok =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                if (ok) viewModel.startVoiceNote() else showMicExplainer = true
            },
            onStopRecording = viewModel::stopVoiceNote,
            onDeleteRecording = viewModel::clearVoiceNote,
            idleLabel = farmString(R.string.diary_voice_idle),
            recordingLabel = farmString(R.string.diary_voice_recording),
            audioPlayerContent = { path, dur ->
                val playing = state.playingPath == path
                FarmAudioPlayer(
                    isPlaying = playing,
                    currentPositionMillis = if (playing) state.playbackPosMs else 0L,
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

@Composable
private fun LedgerDetailInfoCard(
    entry: LedgerEntry,
    categoryLabel: String,
    formattedDate: String,
    paymentModeLabel: String,
    useTelugu: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        Column(
            modifier = Modifier.padding(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
        ) {
            DetailMetaRow(
                label = if (useTelugu) "వర్గం" else "Category",
                value = categoryLabel,
            )
            DetailMetaRow(
                label = if (useTelugu) "తేదీ" else "Date",
                value = formattedDate,
            )
            DetailMetaRow(
                label = if (useTelugu) "చెల్లింపు విధానం" else "Payment Mode",
                value = paymentModeLabel,
            )
            val cleanTitle = entry.title.trim()
            if (cleanTitle.isNotEmpty() && !cleanTitle.equals(categoryLabel, ignoreCase = true)) {
                DetailMetaRow(
                    label = if (useTelugu) "వివరం" else "Item",
                    value = cleanTitle,
                )
            }
            entry.notes?.trim()?.takeIf { it.isNotEmpty() }?.let { note ->
                DetailMetaRow(
                    label = if (useTelugu) "గమనిక" else "Note",
                    value = note,
                )
            }
            entry.voidReason?.trim()?.takeIf { entry.isVoided && it.isNotEmpty() }?.let { reason ->
                DetailMetaRow(
                    label = if (useTelugu) "రద్దు కారణం" else "Void Reason",
                    value = reason,
                )
            }
        }
    }
}

@Composable
private fun DetailMetaRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = FarmTypography.bodyMedium,
            color = FarmColors.SecondaryText,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = FarmTypography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = FarmColors.MainText,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f),
        )
    }
}

private fun ledgerStatusType(status: PaymentStatus): FarmStatusType =
    when (status) {
        PaymentStatus.PAID -> FarmStatusType.SUCCESS
        PaymentStatus.PARTIAL -> FarmStatusType.INFO
        PaymentStatus.PENDING -> FarmStatusType.WARNING
    }

private val QUICK_RUPEE_CHIPS = listOf(100, 200, 500, 1000)

private val EXPENSE_GRID_CATEGORIES =
    listOf(
        LedgerCategory.SEEDS,
        LedgerCategory.FERTILIZERS,
        LedgerCategory.PESTICIDES,
        LedgerCategory.LABOUR,
        LedgerCategory.MACHINERY,
        LedgerCategory.DIESEL,
        LedgerCategory.IRRIGATION,
        LedgerCategory.OTHER_EXPENSE,
    )
