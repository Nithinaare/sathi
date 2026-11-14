package com.farmsaathi.feature.home.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmVisualGridTile
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.AppLanguage
import com.farmsaathi.feature.home.viewmodel.HomeViewModel

@Composable
fun SnapToLogScreen(
    photoPath: String,
    exifEpochMillis: Long?,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onRouteExpense: (String, Long?) -> Unit,
    onRouteIrrigation: (String, Long?) -> Unit,
    onRouteDiary: (String, Long?) -> Unit,
    onRouteWorker: (String) -> Unit,
    onRouteSpray: (String, Long?) -> Unit = { _, _ -> },
    onRouteSoilTest: (String, Long?) -> Unit = { _, _ -> },
    onRouteHarvest: (String, Long?) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val useTe = state.prefs.language == AppLanguage.TELUGU
    val photoTimeLabel = viewModel.formatExifDateTime(exifEpochMillis)

    Scaffold(
        containerColor = FarmColors.Background,
        topBar = {
            FarmPageHeader(
                title = if (useTe) "ఈ ఫోటో దేని కోసం?" else "What is this photo for?",
                onBackClick = onBack,
                backContentDescription = farmString(R.string.common_back),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp16),
        ) {
            item(key = "photo_preview", contentType = "photo_preview") {
                Surface(
                    shape = FarmShapeTokens.card,
                    color = FarmColors.Surface,
                    border = BorderStroke(1.dp, FarmColors.Outline),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(FarmSpacing.dp12)) {
                        AsyncImage(
                            model = viewModel.resolvePhotoFile(photoPath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(FarmShapeTokens.actionTile),
                        )
                        Spacer(Modifier.height(FarmSpacing.dp12))
                        Row(
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
                                    "సమయం: $photoTimeLabel"
                                } else {
                                    "Captured: $photoTimeLabel"
                                },
                                style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = FarmColors.PrimaryForest,
                            )
                        }
                    }
                }
            }

            item(key = "route_row_expense_irrigation", contentType = "two_col_route_row") {
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
                            "బిల్లు లేదా ఖర్చు రాయడానికి నొక్కండి. ఈ ఫోటో జత అవుతుంది."
                        } else {
                            "Log an expense with this photo."
                        },
                        onClick = { onRouteExpense(photoPath, exifEpochMillis) },
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
                            "పంటకు నీరు పెట్టిన సమయం రాయడానికి నొక్కండి."
                        } else {
                            "Log crop watering with this photo time."
                        },
                        onClick = { onRouteIrrigation(photoPath, exifEpochMillis) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            item(key = "route_row_diary_labour", contentType = "two_col_route_row") {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmVisualGridTile(
                        title = farmString(R.string.home_tile_diary),
                        icon = Icons.Rounded.PhotoCamera,
                        imageRes = FarmImages.diary,
                        fullBleedImage = true,
                        borderColor = FarmColors.HarvestAmber,
                        speakDescription =
                        if (useTe) {
                            "పొలం ఫోటో డైరీలో భద్రపరచడానికి నొక్కండి."
                        } else {
                            "Save this photo in field diary."
                        },
                        onClick = { onRouteDiary(photoPath, exifEpochMillis) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    FarmVisualGridTile(
                        title = farmString(R.string.home_tile_labour),
                        icon = Icons.Rounded.Groups,
                        imageRes = FarmImages.labour,
                        fullBleedImage = true,
                        borderColor = FarmColors.PrimaryForest,
                        speakDescription =
                        if (useTe) {
                            "ఈ ఫోటోతో కొత్త కూలీని జోడించడానికి నొక్కండి."
                        } else {
                            "Add a worker using this photo."
                        },
                        onClick = { onRouteWorker(photoPath) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            item(key = "route_row_spray_soil", contentType = "two_col_route_row") {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmVisualGridTile(
                        title = if (useTe) "మందు కొట్టాము" else "Spray / Inputs",
                        icon = Icons.Rounded.Science,
                        imageRes = FarmImages.pesticide,
                        fullBleedImage = true,
                        borderColor = FarmColors.PrimaryForest,
                        speakDescription =
                        if (useTe) {
                            "మందు కొట్టిన లెక్కలో ఈ ఫోటోను దాచడానికి నొక్కండి."
                        } else {
                            "Log pesticide or fertilizer spray with this photo."
                        },
                        onClick = { onRouteSpray(photoPath, exifEpochMillis) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    FarmVisualGridTile(
                        title = if (useTe) "మట్టి కార్డు" else "Soil Card",
                        icon = Icons.Rounded.Science,
                        imageRes = FarmImages.soilTest,
                        fullBleedImage = true,
                        borderColor = FarmColors.Terracotta,
                        speakDescription =
                        if (useTe) {
                            "మట్టి పరీక్ష కార్డులో ఈ ఫోటోను దాచడానికి నొక్కండి."
                        } else {
                            "Save this photo as a soil health card."
                        },
                        onClick = { onRouteSoilTest(photoPath, exifEpochMillis) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            item(key = "route_row_harvest", contentType = "two_col_route_row") {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmVisualGridTile(
                        title = if (useTe) "కోత స్లిప్" else "Harvest Slip",
                        icon = Icons.Rounded.Warehouse,
                        imageRes = FarmImages.harvest,
                        fullBleedImage = true,
                        borderColor = FarmColors.HarvestAmber,
                        speakDescription =
                        if (useTe) {
                            "పంట కోత దిగుబడి స్లిప్‌లో ఈ ఫోటోను దాచడానికి నొక్కండి."
                        } else {
                            "Save this photo as a harvest yield slip."
                        },
                        onClick = { onRouteHarvest(photoPath, exifEpochMillis) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
