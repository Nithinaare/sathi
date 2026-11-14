package com.farmsaathi.feature.reports.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.repository.SearchHitType
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmIconButton
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmRecordCard
import com.farmsaathi.core.designsystem.components.FarmSearchField
import com.farmsaathi.core.designsystem.components.FarmSectionHeader
import com.farmsaathi.core.designsystem.components.FarmStatusIndicator
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmSummaryCard
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.FarmLabels
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.core.model.InputCategory
import com.farmsaathi.feature.reports.viewmodel.ReportsAndSearchViewModel
import com.farmsaathi.feature.reports.viewmodel.SearchCategoryFilter

@Composable
fun ReportsOverviewScreen(
    viewModel: ReportsAndSearchViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val context = LocalContext.current

    if (state.isLoading) {
        FarmLoadingState(message = farmString(R.string.common_loading))
        return
    }

    val t = state.seasonTotals
    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.rep_title),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = AudioCueCatalog.REPORTS_SUMMARY,
                isAudioPlaying = state.isAudioPlaying,
                onToggleAudio = { viewModel.toggleGuideAudio() },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(FarmSpacing.dp16)) {
                FarmPrimaryButton(
                    text = if (useTe) "లెక్క షేర్ చేయండి" else "Share Summary",
                    leadingIcon = Icons.Rounded.Share,
                    onClick = {
                        val intent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, viewModel.buildShareSummaryText())
                            }
                        context.startActivity(Intent.createChooser(intent, "Share Report"))
                    },
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
            item(key = "reports_disclaimer", contentType = "status_indicator") {
                FarmStatusIndicator(
                    type = FarmStatusType.INFO,
                    label =
                    if (useTe) {
                        "మీరు రాసిన ఖర్చులు, అమ్మకాల ఆధారంగానే లెక్క చూపిస్తున్నాము"
                    } else {
                        "Calculated strictly from your recorded entries"
                    },
                )
            }
            item(key = "overall_balance_card", contentType = "summary_card") {
                FarmSummaryCard(
                    title = if (useTe) "మొత్తం నికర తేడా" else "Overall Recorded Balance",
                    primaryValue = t.netBalance.formatInr(),
                    secondaryMetrics =
                    listOf(
                        farmString(R.string.home_expense_short) to t.totalExpense.formatInr(),
                        farmString(R.string.home_income_short) to t.totalIncome.formatInr(),
                    ),
                    status =
                    if (!t.pendingPayable.isZero() || !t.pendingReceivable.isZero()) {
                        FarmStatusType.WARNING to (
                            if (useTe) {
                                "ఇవ్వాలి: ${t.pendingPayable.formatInr()} · రావాలి: ${t.pendingReceivable.formatInr()}"
                            } else {
                                "Payable: ${t.pendingPayable.formatInr()} · Receivable: ${t.pendingReceivable.formatInr()}"
                            }
                            )
                    } else {
                        null
                    },
                )
            }
            if (state.categoryTotals.isNotEmpty()) {
                item(key = "category_totals_header", contentType = "section_header") {
                    FarmSectionHeader(title = if (useTe) "విభాగాల వారీ ఖర్చులు" else "Expenses by Category")
                }
                items(
                    state.categoryTotals,
                    key = { it.first.name },
                    contentType = { "category_total_card" },
                ) { (cat, amt) ->
                    FarmRecordCard(
                        title = FarmLabels.ledgerCategory(cat, lang),
                        leadingImageRes = FarmImages.forLedgerCategory(cat),
                        amountOrMetric = amt.formatInr(),
                        onClick = {},
                    )
                }
            }
            if (state.cropProfitabilities.isNotEmpty()) {
                item(key = "crop_profit_header", contentType = "section_header") {
                    FarmSectionHeader(
                        title = if (useTe) "పంటల వారీ లాభం / ఖర్చు" else "Crop-Wise Income & Expense",
                    )
                }
                items(
                    state.cropProfitabilities,
                    key = { it.cropCycleId },
                    contentType = { "crop_profit_card" },
                ) { prof ->
                    FarmSummaryCard(
                        title = prof.cropName,
                        primaryLabel = if (useTe) "తెలిసిన నిల్వ తేడా" else "Known Balance",
                        primaryValue = prof.knownBalance.formatInr(),
                        secondaryMetrics =
                        listOf(
                            farmString(R.string.home_expense_short) to prof.recordedExpense.formatInr(),
                            farmString(R.string.home_income_short) to prof.recordedIncome.formatInr(),
                        ),
                        status =
                        if (prof.isCalculationPartial) {
                            FarmStatusType.WARNING to (
                                if (useTe) {
                                    "పాక్షిక లెక్క (పంట పూర్తి / కోత / బాకీలు ఇంకా ఉన్నాయి)"
                                } else {
                                    "Partial data — cycle active, harvest or dues pending"
                                }
                                )
                        } else {
                            FarmStatusType.SUCCESS to (if (useTe) "పూర్తి లెక్క" else "Complete")
                        },
                    )
                }
            }
            if (state.categoryTotals.isEmpty() && state.cropProfitabilities.isEmpty()) {
                item(key = "reports_empty_state", contentType = "empty_state") {
                    FarmEmptyState(
                        title = farmString(R.string.rep_title),
                        message = if (useTe) "నివేదిక కోసం ఖర్చులు లేదా పంట అమ్మకాలు కలపండి." else "Add expenses or sales to see reports.",
                        icon = Icons.Rounded.Assessment,
                        imageRes = FarmImages.reports,
                    )
                }
            }
        }
    }
}

@Composable
fun OfflineSearchScreen(
    viewModel: ReportsAndSearchViewModel,
    onBack: () -> Unit,
    onSelectHit: (SearchHitType, Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.prefs.language
    val useTe = lang == AppLanguage.TELUGU
    val context = LocalContext.current

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.common_search),
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
                audioCueKey = "offline_search_guide",
                isAudioPlaying = state.isAudioPlaying,
                onToggleAudio = { viewModel.toggleSearchGuideAudio() },
            )
        },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FarmSearchField(
                query = state.searchQuery,
                onQueryChange = viewModel::updateSearch,
                placeholder = if (useTe) "పొలం, పంట, ఖర్చు, ఫోన్ నంబర్..." else "Search farms, crops, expenses, contacts...",
                modifier = Modifier.padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp12),
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = FarmSpacing.dp16),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                items(
                    SearchCategoryFilter.entries,
                    key = { it.name },
                    contentType = { "search_category_chip" },
                ) { category ->
                    val selected = state.selectedCategory == category
                    val icon =
                        when (category) {
                            SearchCategoryFilter.ALL -> Icons.Rounded.Search
                            SearchCategoryFilter.CONTACTS -> Icons.Rounded.Person
                            SearchCategoryFilter.CROPS -> Icons.Rounded.Grass
                            SearchCategoryFilter.EXPENSES -> Icons.Rounded.Payments
                            SearchCategoryFilter.WORKERS -> Icons.Rounded.Groups
                            SearchCategoryFilter.DIARY -> Icons.Rounded.PhotoCamera
                            SearchCategoryFilter.INPUTS -> Icons.Rounded.Science
                        }
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectCategory(category) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        shape = FarmShapeTokens.circular,
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = {
                            Text(
                                text = if (useTe) category.labelTe else category.labelEn,
                                style = FarmTypography.labelLarge,
                            )
                        },
                        colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmColors.PrimaryContainer,
                            selectedLabelColor = FarmColors.PrimaryForest,
                            selectedLeadingIconColor = FarmColors.PrimaryForest,
                        ),
                    )
                }
            }
            if (state.searchHits.isEmpty()) {
                FarmEmptyState(
                    title =
                    if (state.searchQuery.isBlank()) {
                        farmString(R.string.common_search)
                    } else {
                        if (useTe) "ఏమీ దొరకలేదు" else "No matching records"
                    },
                    message =
                    if (state.searchQuery.isBlank()) {
                        if (useTe) "ఈ విభాగంలో ఇంకా వివరాలు లేవు." else "No records saved in this category yet."
                    } else {
                        state.searchQuery
                    },
                    icon = Icons.Rounded.Search,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(FarmSpacing.dp16),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    items(
                        state.searchHits,
                        key = { "${it.type}_${it.id}" },
                        contentType = { it.type },
                    ) { hit ->
                        val typeLabel = FarmLabels.searchHitTypeLabel(hit.type.name, useTe)
                        val localizedSubtitle =
                            if (hit.type == SearchHitType.INVENTORY) {
                                InputCategory.entries
                                    .firstOrNull { it.name == hit.subtitle }
                                    ?.let { FarmLabels.inputCategory(it, lang) }
                                    ?: hit.subtitle
                            } else {
                                hit.subtitle
                            }
                        val contactPhone =
                            if (hit.type == SearchHitType.CONTACT) {
                                state.contactPhonesById[hit.id] ?: hit.subtitle.takeIf { it.any(Char::isDigit) }
                            } else {
                                null
                            }
                        val imageRes =
                            when (hit.type) {
                                SearchHitType.FARM, SearchHitType.PLOT -> FarmImages.farmField
                                SearchHitType.CROP -> FarmImages.forCropName(hit.title)
                                SearchHitType.DIARY -> FarmImages.diary
                                SearchHitType.LEDGER -> FarmImages.expense
                                SearchHitType.WORKER -> FarmImages.labour
                                SearchHitType.INVENTORY -> FarmImages.inputs
                                SearchHitType.CONTACT -> null
                            }
                        FarmRecordCard(
                            title = hit.title,
                            subtitle = localizedSubtitle.ifBlank { typeLabel },
                            metadata = typeLabel,
                            leadingIcon = Icons.Rounded.Person,
                            leadingImageRes = imageRes,
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp4),
                                ) {
                                    FarmIconButton(
                                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = if (useTe) "చదివి వినిపించు" else "Read aloud",
                                        tint = FarmColors.PrimaryForest,
                                        containerColor = FarmColors.PrimaryContainer,
                                        onClick = { viewModel.speakSearchHit(hit, typeLabel, localizedSubtitle) },
                                    )
                                    if (!contactPhone.isNullOrBlank()) {
                                        FarmIconButton(
                                            icon = Icons.Rounded.Call,
                                            contentDescription = if (useTe) "కాల్" else "Call",
                                            tint = FarmColors.OnPrimary,
                                            containerColor = FarmColors.PrimaryForest,
                                            speakDescription = if (useTe) "${hit.title} కు ఫోన్ కాల్ చేయండి" else "Call ${hit.title}",
                                            onClick = {
                                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone"))
                                                context.startActivity(dialIntent)
                                            },
                                        )
                                    }
                                }
                            },
                            onClick = { onSelectHit(hit.type, hit.id) },
                        )
                    }
                }
            }
        }
    }
}
