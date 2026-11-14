package com.farmsaathi.feature.farm.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.FarmEmptyState
import com.farmsaathi.core.designsystem.components.FarmIconButton
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmSearchField
import com.farmsaathi.core.designsystem.components.FarmSecondaryButton
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.TeluguKarte
import com.farmsaathi.feature.farm.viewmodel.SeasonFilter
import com.farmsaathi.feature.farm.viewmodel.TeluguCalendarViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KarteGuideScreen(
    viewModel: TeluguCalendarViewModel,
    highlightKarteId: Int? = null,
    onBack: () -> Unit,
    onAddTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Filter kartis by search query and active season filter
    val displayedKartis = remember(state.filteredKartis, searchQuery) {
        if (searchQuery.isBlank()) {
            state.filteredKartis
        } else {
            val q = searchQuery.trim().lowercase()
            state.filteredKartis.filter { karte ->
                karte.teluguName.lowercase().contains(q) ||
                    karte.englishName.lowercase().contains(q) ||
                    karte.proverb.lowercase().contains(q) ||
                    karte.agriculturalSignificance.lowercase().contains(q) ||
                    karte.recommendedCrops.any { it.lowercase().contains(q) }
            }
        }
    }

    LaunchedEffect(highlightKarteId, displayedKartis) {
        if (highlightKarteId != null) {
            val targetIdx = displayedKartis.indexOfFirst { it.id == highlightKarteId }
            if (targetIdx >= 0) {
                listState.animateScrollToItem(targetIdx + 2) // +2 accounts for search and chips header items
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = farmString(R.string.cal_karte_guide_title),
                subtitle = "27 సూర్య నక్షత్రాల సాగు విజ్ఞానం & సామెతలు",
                onBackClick = onBack,
                audioCueKey = "karte_guide_overview",
                isAudioPlaying = state.isAudioSpeaking,
                onToggleAudio = { _ -> viewModel.togglePanchangAudio() },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
        ) {
            // 1. Search Bar
            item(key = "search_bar") {
                FarmSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "కార్తె పేరు, పంట లేదా సామెత వెతకండి...",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 2. Season Filter Chips
            item(key = "season_filter_chips") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    SeasonFilter.entries.forEach { filter ->
                        val isSelected = filter == state.activeSeasonFilter
                        val label = if (state.isTelugu) filter.teluguLabel else filter.englishLabel
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.filterSeason(filter) },
                            label = { Text(label, style = FarmTypography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmColors.PrimaryForest,
                                selectedLabelColor = FarmColors.OnPrimary,
                            ),
                        )
                    }
                }
            }

            // 3. Empty Search State
            if (displayedKartis.isEmpty()) {
                item(key = "empty_state") {
                    FarmEmptyState(
                        title = "ఫలితాలు లేవు",
                        message = "వెతికిన పదానికి సరిపడే కార్తెలు కనబడలేదు.",
                        icon = Icons.Rounded.Search,
                    )
                }
            } else {
                // 4. Master 27 Kartis Expandable Cards
                items(
                    items = displayedKartis,
                    key = { it.id },
                    contentType = { "karte_guide_item" },
                ) { karte ->
                    val isCurrent = state.currentKarte?.karteId == karte.id
                    val isInitiallyExpanded = karte.id == highlightKarteId || isCurrent

                    ExpandableKarteGuideCard(
                        karte = karte,
                        isCurrent = isCurrent,
                        initiallyExpanded = isInitiallyExpanded,
                        onSpeak = { viewModel.speakKarte(karte) },
                        onAddTask = { onAddTask(state.todayEpochDay) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandableKarteGuideCard(
    karte: TeluguKarte,
    isCurrent: Boolean,
    initiallyExpanded: Boolean,
    onSpeak: () -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Surface(
        shape = FarmShapeTokens.card,
        color = if (isCurrent) FarmColors.PrimaryContainer.copy(alpha = 0.4f) else FarmColors.Surface,
        border = BorderStroke(if (isCurrent) 1.5.dp else 1.dp, if (isCurrent) FarmColors.PrimaryForest else FarmColors.Outline),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(FarmSpacing.dp16)) {
            // Header Row: [ID. Name] + Current Badge + Audio + Expand Arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = FarmShapeTokens.circular,
                        color = if (isCurrent) FarmColors.PrimaryForest else FarmColors.SurfaceVariant,
                    ) {
                        Text(
                            text = "${karte.id}",
                            style = FarmTypography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) FarmColors.OnPrimary else FarmColors.MainText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    Column {
                        Text(
                            text = "${karte.teluguName} (${karte.englishName})",
                            style = FarmTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FarmColors.MainText,
                        )
                        Text(
                            text = "${karte.approxDateSpan} • ${karte.season}",
                            style = FarmTypography.bodySmall,
                            color = FarmColors.SecondaryText,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        Surface(
                            shape = FarmShapeTokens.tag,
                            color = FarmColors.PrimaryForest,
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            Text(
                                text = "ప్రస్తుతం",
                                style = FarmTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FarmColors.OnPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    FarmIconButton(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "వినండి",
                        onClick = onSpeak,
                        containerColor = FarmColors.PrimaryContainer,
                        tint = FarmColors.PrimaryForest,
                    )

                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "కుదించు" else "వివరాలు చూడండి",
                        tint = FarmColors.SecondaryText,
                    )
                }
            }

            // Expandable Content Body
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FarmSpacing.dp12),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    // Proverb Box
                    Surface(
                        shape = FarmShapeTokens.tag,
                        color = FarmColors.AmberContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(FarmSpacing.dp12)) {
                            Text(
                                text = farmString(R.string.cal_proverb_label),
                                style = FarmTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FarmColors.Warning,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "\"${karte.proverb}\"",
                                style = FarmTypography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FarmColors.Terracotta,
                            )
                        }
                    }

                    // Agricultural Significance
                    Column {
                        Text(
                            text = farmString(R.string.cal_advisory_label),
                            style = FarmTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FarmColors.PrimaryForest,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = karte.agriculturalSignificance,
                            style = FarmTypography.bodyMedium,
                            color = FarmColors.MainText,
                        )
                    }

                    // Recommended Crops
                    if (karte.recommendedCrops.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = farmString(R.string.cal_suited_crops),
                                style = FarmTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FarmColors.SecondaryText,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                karte.recommendedCrops.forEach { crop ->
                                    Surface(
                                        shape = FarmShapeTokens.tag,
                                        color = FarmColors.SecondaryContainer,
                                    ) {
                                        Text(
                                            text = crop,
                                            style = FarmTypography.labelSmall,
                                            color = FarmColors.SecondaryLeaf,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(FarmSpacing.dp4))

                    // 1-Tap Action: Add activity based on this Karte
                    FarmSecondaryButton(
                        text = "+ ఈ కార్తెలో పని రాయండి",
                        leadingIcon = Icons.Rounded.Add,
                        onClick = onAddTask,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
