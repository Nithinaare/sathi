package com.farmsaathi.feature.operations.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.DropdownOption
import com.farmsaathi.core.designsystem.components.FarmActionTile
import com.farmsaathi.core.designsystem.components.FarmAmountInput
import com.farmsaathi.core.designsystem.components.FarmDatePicker
import com.farmsaathi.core.designsystem.components.FarmDropdown
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmImageGridPicker
import com.farmsaathi.core.designsystem.components.FarmImagePickerOption
import com.farmsaathi.core.designsystem.components.FarmListItem
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.components.FarmNumberInput
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPhotoCapture
import com.farmsaathi.core.designsystem.components.FarmPhotoViewer
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmRecordCard
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.components.FarmSectionHeader
import com.farmsaathi.core.designsystem.components.FarmSnackbar
import com.farmsaathi.core.designsystem.components.FarmStatusIndicator
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmStickyFooterBar
import com.farmsaathi.core.designsystem.components.FarmSummaryCard
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.components.SnackbarTone
import com.farmsaathi.core.designsystem.components.rememberFarmPhotoLaunchers
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.core.model.MoneyPaise
import com.farmsaathi.core.model.PaymentMode
import com.farmsaathi.core.model.QuantityUnit
import com.farmsaathi.core.model.StockMovementReason
import com.farmsaathi.feature.operations.viewmodel.InventoryViewModel
import java.math.BigDecimal

@Composable
fun InventoryListScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onAddProduct: () -> Unit,
    onAddPurchase: () -> Unit,
    onRecordSpray: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.inv_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.INVENTORY_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.INVENTORY_HELP,
                onToggleAudio = { viewModel.toggleAudio(AudioCueCatalog.INVENTORY_HELP) },
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                    if (state.products.isNotEmpty()) {
                        FarmPrimaryButton(
                            text = farmString(R.string.inv_purchase_btn),
                            leadingIcon = Icons.Rounded.ShoppingCart,
                            onClick = onAddPurchase,
                        )
                    }
                    FarmSecondaryButton(
                        text = farmString(R.string.inv_add_product),
                        leadingIcon = Icons.Rounded.Add,
                        onClick = onAddProduct,
                    )
                }
            }
        },
    ) { padding ->
        if (state.products.isEmpty()) {
            FarmEmptyState(
                title = farmString(R.string.inv_title),
                message =
                if (useTe) {
                    "ఇంకా ఎరువులు, విత్తనాలు, మందులు కలపలేదు. కింద ఉన్న బటన్ నొక్కండి."
                } else {
                    "No seeds, fertilizers, or pesticides added yet. Tap below to add a product."
                },
                icon = Icons.Rounded.Inventory2,
                imageRes = FarmImages.inputs,
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
                item(key = "record_spray_action", contentType = "action_tile") {
                    FarmActionTile(
                        title = if (useTe) "మందు కొట్టాము / పొలానికి వాడాము" else "Record Crop Spray / Usage",
                        subtitle = if (useTe) "ఏ పంటకు ఎంత వేశారో 3 ట్యాప్స్‌లో రాయండి" else "Log spray or fertilizer usage in 3 taps",
                        icon = Icons.Rounded.Inventory2,
                        imageRes = FarmImages.pesticide,
                        compact = true,
                        onClick = onRecordSpray,
                    )
                }
                items(
                    state.products,
                    key = { it.id },
                    contentType = { "input_product_card" },
                ) { prod ->
                    val low =
                        prod.currentStockMilliUnits <= prod.lowStockThresholdMilliUnits &&
                            prod.currentStockMilliUnits > 0L
                    val out = prod.currentStockMilliUnits == 0L
                    FarmRecordCard(
                        title = prod.name,
                        subtitle = FarmLabels.inputCategory(prod.category, lang),
                        amountOrMetric = FarmLabels.formatQuantity(prod.currentStockMilliUnits, prod.unit, lang),
                        statusType =
                        when {
                            out -> FarmStatusType.ERROR
                            low -> FarmStatusType.WARNING
                            else -> FarmStatusType.SUCCESS
                        },
                        statusLabel =
                        when {
                            out -> if (useTe) "నిల్వ లేదు" else "Out"
                            low -> if (useTe) "తక్కువ నిల్వ" else "Low"
                            else -> if (useTe) "నిల్వ ఉంది" else "In Stock"
                        },
                        leadingIcon = Icons.Rounded.Inventory2,
                        leadingImageRes = FarmImages.forInputCategory(prod.category),
                        onClick = { onOpenProduct(prod.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryProductDetailScreen(
    productId: Long,
    viewModel: InventoryViewModel,
    onBack: () -> Unit,
    onAddPurchase: (Long) -> Unit,
    onRecordUsage: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prod = remember(state.products, productId) { state.products.firstOrNull { it.id == productId } }
    val movements by viewModel.observeMovements(productId).collectAsStateWithLifecycle(emptyList())
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    var previewPhoto by remember { mutableStateOf<String?>(null) }

    if (prod == null) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = prod.name,
                subtitle = FarmLabels.inputCategory(prod.category, lang),
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
            item(key = "remaining_stock_card", contentType = "summary_card") {
                FarmSummaryCard(
                    title = if (useTe) "ప్రస్తుత నిల్వ" else "Remaining Stock",
                    primaryValue = FarmLabels.formatQuantity(prod.currentStockMilliUnits, prod.unit, lang),
                )
            }
            item(key = "inventory_action_buttons", contentType = "two_col_action_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmPrimaryButton(
                        text = farmString(R.string.inv_purchase_btn),
                        onClick = { onAddPurchase(prod.id) },
                        modifier = Modifier.weight(1f),
                    )
                    FarmSecondaryButton(
                        text = farmString(R.string.inv_use_btn),
                        onClick = { onRecordUsage(prod.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item(key = "movement_history_header", contentType = "section_header") {
                FarmSectionHeader(title = if (useTe) "నిల్వ మార్పుల చరిత్ర" else "Stock Movement History")
            }
            items(
                movements,
                key = { it.id },
                contentType = { "stock_movement_card" },
            ) { mov ->
                val sign = if (mov.signedDeltaMilliUnits > 0) "+" else ""
                FarmRecordCard(
                    title = FarmLabels.stockMovementReasonLabel(mov.reason, useTe),
                    subtitle = viewModel.formatDate(mov.movementDateEpochMillis),
                    amountOrMetric = "$sign${FarmLabels.formatQuantity(mov.signedDeltaMilliUnits, prod.unit, lang)}",
                    hasPhoto = mov.photoPath != null,
                    onClick = { mov.photoPath?.let { previewPhoto = it } },
                )
            }
        }
    }

    previewPhoto?.let { path ->
        FarmPhotoViewer(photoFile = viewModel.resolvePhotoFile(path), onDismiss = { previewPhoto = null })
    }
}

@Composable
fun InventoryProductFormScreen(
    productId: Long?,
    viewModel: InventoryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing =
        remember(productId, state.products) {
            productId?.let { id -> state.products.firstOrNull { it.id == id } }
        }
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var category by remember(existing) { mutableStateOf(existing?.category ?: InputCategory.FERTILIZER) }
    var unit by remember(existing) { mutableStateOf(existing?.unit ?: QuantityUnit.BAG) }
    var brand by remember(existing) { mutableStateOf(existing?.brand.orEmpty()) }
    var thresholdStr by remember(existing) { mutableStateOf("2") }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var nameErr by remember { mutableStateOf<String?>(null) }
    val reqMsg = farmString(R.string.common_required_field)

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.inv_add_product),
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
                            nameErr = reqMsg
                            viewModel.playErrorCue()
                        } else {
                            val thresh =
                                thresholdStr.toBigDecimalOrNull()
                                    ?.multiply(BigDecimal(1_000))?.toLong() ?: 0L
                            viewModel.saveProduct(existing, name, category, brand, unit, thresh, notes) {
                                onBack()
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
                    nameErr = null
                },
                label = if (useTe) "సరుకు పేరు (ఉదా: యూరియా / డీఏపీ)" else "Product Name",
                errorText = nameErr,
            )
            FarmDropdown(
                label = if (useTe) "రకం" else "Category",
                options = InputCategory.entries.map { DropdownOption(it, FarmLabels.inputCategory(it, lang)) },
                selectedValue = category,
                onSelect = { category = it.value },
                placeholder = "",
            )
            FarmDropdown(
                label = if (useTe) "కొలత" else "Stock Unit",
                options =
                listOf(QuantityUnit.BAG, QuantityUnit.KG, QuantityUnit.LITRE, QuantityUnit.ML, QuantityUnit.PIECE)
                    .map { DropdownOption(it, FarmLabels.quantityUnit(it, lang)) },
                selectedValue = unit,
                onSelect = { unit = it.value },
                placeholder = "",
            )
            FarmTextInput(
                value = brand,
                onValueChange = { brand = it },
                label = if (useTe) "కంపెనీ / రకం (ఐచ్ఛికం)" else "Brand / Variety (optional)",
            )
            FarmNumberInput(
                value = thresholdStr,
                onValueChange = { thresholdStr = it },
                label = if (useTe) "తక్కువ నిల్వ హెచ్చరిక స్థాయి" else "Low stock alert level",
            )
            FarmTextInput(
                value = notes,
                onValueChange = { notes = it },
                label = farmString(R.string.common_notes_optional),
            )
        }
    }
}

@Composable
fun InventoryPurchaseFormScreen(
    initialProductId: Long?,
    viewModel: InventoryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    var productId by remember(initialProductId, state.products) {
        mutableStateOf(initialProductId ?: state.products.firstOrNull()?.id)
    }
    var farmId by remember(state.farms) { mutableStateOf(state.prefs.activeFarmId ?: state.farms.firstOrNull()?.id) }
    var qtyStr by remember { mutableStateOf("") }
    var unitPriceStr by remember { mutableStateOf("") }
    var paidInFull by remember { mutableStateOf(true) }
    var paidStr by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(PaymentMode.CASH) }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var previewPhoto by remember { mutableStateOf<String?>(null) }
    var errMsg by remember { mutableStateOf<String?>(null) }

    val selectedProduct = state.products.firstOrNull { it.id == productId }
    val qtyMilli = qtyStr.toBigDecimalOrNull()?.multiply(BigDecimal(1_000))?.toLong() ?: 0L
    val unitPrice = MoneyPaise.parseOrNull(unitPriceStr) ?: MoneyPaise.ZERO
    val total = MoneyPaise.multiplyMilliUnits(unitPrice, qtyMilli)

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
                title = farmString(R.string.inv_purchase_btn),
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
                        val prod = selectedProduct
                        val fId = farmId ?: state.farms.firstOrNull()?.id
                        if (prod == null || fId == null || qtyMilli <= 0L) {
                            errMsg = if (useTe) "సరుకు, పొలం, పరిమాణం సరిగా రాయండి" else "Enter valid product & quantity"
                            viewModel.playErrorCue()
                        } else {
                            errMsg = null
                            val paid =
                                if (paidInFull) {
                                    total
                                } else {
                                    MoneyPaise.parseOrNull(paidStr) ?: MoneyPaise.ZERO
                                }
                            viewModel.recordPurchase(
                                product = prod,
                                farmId = fId,
                                cropCycleId = null,
                                vendor = vendor,
                                qtyMilli = qtyMilli,
                                unitPrice = unitPrice,
                                totalAmount = total,
                                paidAmount = paid,
                                mode = mode,
                                dateMillis = dateMillis,
                                notes = "",
                                receiptPhotoPath = photoPath,
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
                label = if (useTe) "సరుకు ఎంచుకోండి" else "Product",
                options = state.products.map { DropdownOption(it.id, it.name) },
                selectedValue = productId,
                onSelect = {
                    productId = it.value
                    errMsg = null
                },
                placeholder = if (useTe) "ఎంచుకోండి" else "Select",
            )
            FarmDropdown(
                label = farmString(R.string.farms_title),
                options = state.farms.map { DropdownOption(it.id, it.name) },
                selectedValue = farmId,
                onSelect = {
                    farmId = it.value
                    errMsg = null
                },
                placeholder = "",
            )
            FarmNumberInput(
                value = qtyStr,
                onValueChange = {
                    qtyStr = it
                    errMsg = null
                },
                label = if (useTe) "కొన్న పరిమాణం" else "Quantity Purchased",
                unitSuffix = selectedProduct?.let { FarmLabels.quantityUnit(it.unit, lang) },
            )
            FarmAmountInput(
                value = unitPriceStr,
                onValueChange = { unitPriceStr = it },
                label = if (useTe) "ఒక్కో దానికి ధర (₹)" else "Unit Price (₹)",
            )
            FarmSummaryCard(
                title = if (useTe) "మొత్తం ఖర్చు (ఖాతా & నిల్వలో కలుస్తుంది)" else "Total Cost (Updates Stock & Ledger)",
                primaryValue = total.formatInr(),
            )
            FarmListItem(
                title = if (useTe) "పూర్తి డబ్బు ఇప్పుడే ఇచ్చాము (బాకీ లేదు)" else "Paid in Full Now",
                subtitle =
                if (paidInFull) {
                    if (useTe) "మొత్తం ${total.formatInr()} నగదు/ఖాతాలో నమోదవుతుంది" else "Full ${total.formatInr()} marked paid"
                } else {
                    if (useTe) "ఇచ్చిన డబ్బు కింద రాయండి (మిగిలింది బాకీ)" else "Enter partial amount paid; balance becomes credit"
                },
                checked = paidInFull,
                onCheckedChange = { paidInFull = it },
                showDivider = false,
            )
            if (!paidInFull) {
                FarmAmountInput(
                    value = paidStr,
                    onValueChange = { paidStr = it },
                    label = if (useTe) "ఇచ్చిన డబ్బు (₹)" else "Paid Now (₹)",
                )
            }
            FarmPhotoCapture(
                photoPaths = listOfNotNull(photoPath),
                onTakePhotoClick = photoLaunchers.launchCamera,
                onPickFromGalleryClick = photoLaunchers.launchGallery,
                onRemovePhotoClick = { photoPath = null },
                onPhotoClick = { previewPhoto = it },
                title = if (useTe) "బిల్లు / డబ్బా ఫోటో (ఐచ్ఛికం)" else "Bill / Bottle Photo (optional)",
                cameraButtonLabel = farmString(R.string.diary_take_photo),
                galleryButtonLabel = farmString(R.string.diary_pick_gallery),
                resolveFile = viewModel::resolvePhotoFile,
                maxPhotos = 1,
            )
            FarmTextInput(
                value = vendor,
                onValueChange = { vendor = it },
                label = if (useTe) "షాప్ పేరు (ఐచ్ఛికం)" else "Vendor Shop (optional)",
            )
            FarmDropdown(
                label = if (useTe) "చెల్లింపు విధానం" else "Payment Mode",
                options = PaymentMode.entries.map { DropdownOption(it, FarmLabels.paymentMode(it, lang)) },
                selectedValue = mode,
                onSelect = { mode = it.value },
                placeholder = "",
            )
            FarmDatePicker(
                selectedDateMillis = dateMillis,
                onDateSelected = { dateMillis = it },
                label = if (useTe) "కొన్న తేదీ" else "Purchase Date",
                formattedDate = viewModel.formatDate(dateMillis),
            )
            errMsg?.let { Text(it, color = FarmColors.Error, style = FarmTypography.bodyMedium) }
        }
    }

    previewPhoto?.let { path ->
        FarmPhotoViewer(photoFile = viewModel.resolvePhotoFile(path), onDismiss = { previewPhoto = null })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryUsageFormScreen(
    productId: Long = 0L,
    viewModel: InventoryViewModel,
    onBack: () -> Unit,
    onOpenPurchase: (Long) -> Unit,
    initialPlotId: Long? = null,
    initialCropCycleId: Long? = null,
    initialPhotoPath: String? = null,
    initialDateMillis: Long? = null,
    onAddProduct: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU

    if (state.isLoading) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    var selectedProductId by remember(productId, state.products) {
        mutableStateOf(productId.takeIf { it > 0L } ?: state.products.firstOrNull()?.id)
    }
    var selectedCropId by remember(initialCropCycleId, state.crops) {
        mutableStateOf(initialCropCycleId ?: state.crops.firstOrNull()?.id)
    }
    var selectedPlotId by remember(initialPlotId, selectedCropId, state.plots, state.crops) {
        val cropPlot = selectedCropId?.let { cid -> state.crops.firstOrNull { it.id == cid }?.plotId }
        mutableStateOf(initialPlotId ?: cropPlot ?: state.plots.firstOrNull()?.id)
    }
    var qtyStr by remember { mutableStateOf("") }
    var dateMillis by remember(initialDateMillis) {
        mutableLongStateOf(initialDateMillis ?: System.currentTimeMillis())
    }
    var photoPath by remember(initialPhotoPath) { mutableStateOf(initialPhotoPath) }
    var previewPhoto by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var errMsg by remember { mutableStateOf<String?>(null) }

    val selectedProduct = state.products.firstOrNull { it.id == selectedProductId }
    val selectedCrop = state.crops.firstOrNull { it.id == selectedCropId }
    val selectedPlot = state.plots.firstOrNull { it.id == selectedPlotId }
    val resolvedFarmId =
        selectedCrop?.farmId
            ?: selectedPlot?.farmId
            ?: state.prefs.activeFarmId
            ?: state.farms.firstOrNull()?.id
            ?: 0L

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
                title =
                if (selectedProduct != null) {
                    "${if (useTe) "మందు / సరుకు వాడకం" else farmString(R.string.inv_use_btn)}: ${selectedProduct.name}"
                } else if (useTe) {
                    "మందు కొట్టాము"
                } else {
                    "Record Crop Spray / Usage"
                },
                subtitle = selectedProduct?.let { FarmLabels.formatQuantity(it.currentStockMilliUnits, it.unit, lang) },
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.SPRAY_FORM_HELP,
                isAudioPlaying = state.isAudioPlaying && state.activeCueKey == AudioCueCatalog.SPRAY_FORM_HELP,
                onToggleAudio = { viewModel.toggleAudio(AudioCueCatalog.SPRAY_FORM_HELP) },
                audioContentDescription = farmString(R.string.common_listen),
            )
        },
        bottomBar = {
            FarmStickyFooterBar {
                FarmPrimaryButton(
                    text = farmString(R.string.common_save),
                    isLoading = state.isSaving,
                    onClick = {
                        val prod = selectedProduct
                        val qtyMilli =
                            qtyStr.toBigDecimalOrNull()
                                ?.multiply(BigDecimal(1_000))?.toLong() ?: 0L
                        when {
                            prod == null -> {
                                errMsg = if (useTe) "ముందు వాడిన మందు / సరుకు ఎంచుకోండి" else "Select a product first"
                                viewModel.playErrorCue()
                            }
                            resolvedFarmId <= 0L -> {
                                errMsg = if (useTe) "ముందు పొలం కలపండి" else "Add a farm first"
                                viewModel.playErrorCue()
                            }
                            qtyMilli <= 0L -> {
                                errMsg = if (useTe) "వాడిన మోతాదు 0 కంటే ఎక్కువ రాయండి" else "Enter valid quantity greater than 0"
                                viewModel.playErrorCue()
                            }
                            else -> {
                                errMsg = null
                                viewModel.recordUsageOrAdjustment(
                                    productId = prod.id,
                                    farmId = resolvedFarmId,
                                    plotId = selectedPlotId,
                                    cropCycleId = selectedCropId,
                                    signedDeltaMilli = -qtyMilli,
                                    reason = StockMovementReason.FIELD_APPLICATION,
                                    notes = notes,
                                    photoPath = photoPath,
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
            if (state.crops.isNotEmpty()) {
                val cropOptions =
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
                FarmImageGridPicker(
                    label = if (useTe) "1. ఏ పంటకు కొట్టారు?" else "1. Select Crop",
                    options = cropOptions,
                    selected = selectedCropId,
                    onSelect = { chosenCropId ->
                        selectedCropId = chosenCropId
                        val linkedPlot = chosenCropId?.let { cid -> state.crops.firstOrNull { it.id == cid }?.plotId }
                        if (linkedPlot != null) {
                            selectedPlotId = linkedPlot
                        }
                    },
                    columns = 3,
                )
            }

            if (state.plots.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                    Text(
                        text = if (useTe) "మడి (Plot)" else "Plot",
                        style = FarmTypography.labelLarge,
                        color = FarmColors.MainText,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                        verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    ) {
                        state.plots.forEach { plot ->
                            FilterChip(
                                selected = selectedPlotId == plot.id,
                                onClick = { selectedPlotId = plot.id },
                                label = { Text(plot.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FarmColors.PrimaryContainer,
                                    selectedLabelColor = FarmColors.PrimaryForest,
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                            )
                        }
                    }
                }
            }

            if (state.products.isEmpty()) {
                FarmStatusIndicator(
                    type = FarmStatusType.WARNING,
                    label =
                    if (useTe) {
                        "ఇంకా మందులు లేదా ఎరువులు కలపలేదు. ముందు కొత్త సరుకు కలపండి."
                    } else {
                        "No spray/input products added yet. Add a product first."
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                FarmSecondaryButton(
                    text = if (useTe) "కొత్త సరుకు కలపండి" else "Add New Product",
                    leadingIcon = Icons.Rounded.Add,
                    onClick = onAddProduct,
                )
            } else {
                val productOptions =
                    state.products.map { item ->
                        val stockTxt = FarmLabels.formatQuantity(item.currentStockMilliUnits, item.unit, lang)
                        FarmImagePickerOption(
                            value = item.id,
                            label = "${item.name} ($stockTxt)",
                            imageRes = FarmImages.forInputCategory(item.category),
                        )
                    }
                FarmImageGridPicker(
                    label = if (useTe) "2. ఏ మందు / సరుకు కొట్టారు?" else "2. Select Product / Spray",
                    options = productOptions,
                    selected = selectedProductId,
                    onSelect = {
                        selectedProductId = it
                        errMsg = null
                        viewModel.clearStockPrompt()
                    },
                    columns = 2,
                )
                FarmSecondaryButton(
                    text = if (useTe) "+ కొత్త సరుకు కలపండి" else "+ Add New Product",
                    useMutedOutline = true,
                    onClick = onAddProduct,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8)) {
                Text(
                    text = if (useTe) "3. మోతాదు" else "3. Dosage / Quantity",
                    style = FarmTypography.labelLarge,
                    color = FarmColors.MainText,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    listOf("100", "250", "500", "1000").forEach { preset ->
                        FilterChip(
                            selected = qtyStr == preset,
                            onClick = {
                                qtyStr = preset
                                errMsg = null
                                viewModel.clearStockPrompt()
                            },
                            label = {
                                val unitSuffix = selectedProduct?.let { FarmLabels.quantityUnit(it.unit, lang) }.orEmpty()
                                Text(if (unitSuffix.isNotBlank()) "$preset $unitSuffix" else preset)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmColors.PrimaryContainer,
                                selectedLabelColor = FarmColors.PrimaryForest,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                        )
                    }
                }
                FarmNumberInput(
                    value = qtyStr,
                    onValueChange = {
                        qtyStr = it
                        errMsg = null
                        viewModel.clearStockPrompt()
                    },
                    label = if (useTe) "వాడిన పరిమాణం" else "Quantity Used",
                    unitSuffix = selectedProduct?.let { FarmLabels.quantityUnit(it.unit, lang) },
                    errorText = errMsg,
                )
            }

            FarmPhotoCapture(
                photoPaths = listOfNotNull(photoPath),
                onTakePhotoClick = photoLaunchers.launchCamera,
                onPickFromGalleryClick = photoLaunchers.launchGallery,
                onRemovePhotoClick = { photoPath = null },
                onPhotoClick = { previewPhoto = it },
                title = if (useTe) "మందు డబ్బా / స్ప్రే ఫోటో (ఐచ్ఛికం)" else "Bottle / Spray Photo (optional)",
                cameraButtonLabel = farmString(R.string.diary_take_photo),
                galleryButtonLabel = farmString(R.string.diary_pick_gallery),
                resolveFile = viewModel::resolvePhotoFile,
                maxPhotos = 1,
            )

            FarmDatePicker(
                selectedDateMillis = dateMillis,
                onDateSelected = { dateMillis = it },
                label = if (useTe) "మందు కొట్టిన తేదీ" else "Spray Date",
                formattedDate = viewModel.formatDate(dateMillis),
            )

            FarmTextInput(
                value = notes,
                onValueChange = { notes = it },
                label = farmString(R.string.common_notes_optional),
            )

            if (state.insufficientStockPrompt && selectedProduct != null) {
                FarmSnackbar(
                    message =
                    if (useTe) {
                        "నిల్వ సరిపోదు. ముందు కొనుగోలు లేదా నిల్వ కలపండి."
                    } else {
                        "Insufficient stock! Add stock via Purchase first."
                    },
                    tone = SnackbarTone.WARNING,
                    actionLabel = farmString(R.string.inv_purchase_btn),
                    onActionClick = { onOpenPurchase(selectedProduct.id) },
                )
            }
        }
    }

    previewPhoto?.let { path ->
        FarmPhotoViewer(photoFile = viewModel.resolvePhotoFile(path), onDismiss = { previewPhoto = null })
    }
}
