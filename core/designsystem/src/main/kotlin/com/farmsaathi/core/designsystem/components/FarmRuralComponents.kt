package com.farmsaathi.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.farmsaathi.core.designsystem.localization.farmCombinedClickable
import com.farmsaathi.core.designsystem.localization.rememberHoldToSpeakAction
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Visual option rendered inside [FarmImageGridPicker] using a real farm photograph
 * or user-captured photo file.
 */
data class FarmImagePickerOption<T>(
    val value: T,
    val label: String,
    @DrawableRes val imageRes: Int,
    val localPhotoFile: File? = null,
    val speakDescription: String = label,
)

/**
 * Text-light, photorealistic grid selector replacing dense text dropdowns for emergent farmers.
 */
@Composable
fun <T> FarmImageGridPicker(
    label: String,
    options: List<FarmImagePickerOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = FarmColors.MainText,
        )
        Spacer(Modifier.height(FarmSpacing.dp8))
        options.chunked(columns.coerceAtLeast(2)).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            ) {
                rowItems.forEach { option ->
                    val isSelected = option.value == selected
                    val holdSpeak = rememberHoldToSpeakAction(option.speakDescription)
                    val borderColor = if (isSelected) FarmColors.PrimaryForest else FarmColors.Outline
                    Surface(
                        modifier =
                        Modifier
                            .weight(1f)
                            .defaultMinSize(minWidth = FarmDimensions.minTouchTarget, minHeight = 96.dp)
                            .clip(FarmShapeTokens.actionTile)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = borderColor,
                                shape = FarmShapeTokens.actionTile,
                            )
                            .farmCombinedClickable(
                                role = Role.RadioButton,
                                speakLabel = option.speakDescription,
                                onLongPressSpeak = holdSpeak,
                                onClick = { onSelect(option.value) },
                            )
                            .semantics { this.selected = isSelected },
                        shape = FarmShapeTokens.actionTile,
                        color = FarmColors.Surface,
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(96.dp)) {
                            val photoExists by produceState(initialValue = false, key1 = option.localPhotoFile) {
                                value =
                                    option.localPhotoFile?.let { file ->
                                        withContext(Dispatchers.IO) { file.exists() }
                                    } ?: false
                            }
                            if (option.localPhotoFile != null && photoExists) {
                                AsyncImage(
                                    model = option.localPhotoFile,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Image(
                                    painter = painterResource(option.imageRes),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Box(
                                modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colorStops =
                                            arrayOf(
                                                0.0f to Color.Transparent,
                                                0.52f to Color.Black.copy(alpha = 0.35f),
                                                1.0f to Color.Black.copy(alpha = 0.86f),
                                            ),
                                        ),
                                    ),
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = FarmColors.PrimaryContainer,
                                    modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(22.dp)
                                        .clip(FarmShapeTokens.circular)
                                        .background(FarmColors.PrimaryForest),
                                )
                            }
                            Text(
                                text = option.label,
                                style = FarmTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                repeat(columns.coerceAtLeast(2) - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(FarmSpacing.dp8))
        }
    }
}

/**
 * Large Rupee numeric field paired with 1-tap currency chips (+₹100, +₹200, +₹500, +₹1000)
 * grounded in research showing rural numeracy even alongside limited script literacy.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FarmRupeeChipInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    useTelugu: Boolean,
    modifier: Modifier = Modifier,
    presetAmounts: List<Int> = listOf(100, 200, 500, 1000),
    errorText: String? = null,
    helperText: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FarmAmountInput(
            value = value,
            onValueChange = onValueChange,
            label = label,
            errorText = errorText,
            helperText = helperText,
        )
        Spacer(Modifier.height(FarmSpacing.dp8))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
        ) {
            presetAmounts.forEach { step ->
                FilterChip(
                    selected = false,
                    onClick = {
                        val current = value.toDoubleOrNull()?.toLong() ?: 0L
                        onValueChange((current + step).toString())
                    },
                    label = {
                        Text(
                            text = "+₹$step",
                            style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    colors =
                    FilterChipDefaults.filterChipColors(
                        containerColor = FarmColors.PrimaryContainer,
                        labelColor = FarmColors.OnPrimaryContainer,
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
                )
            }
            if (value.isNotEmpty()) {
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
}

/**
 * Collapsible container hiding advanced/secondary form inputs under a collapsed toggle by default.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FarmMoreDetailsAccordion(
    expanded: Boolean,
    onToggle: () -> Unit,
    useTelugu: Boolean,
    modifier: Modifier = Modifier,
    title: String = if (useTelugu) "మరిన్ని వివరాలు" else "More Details",
    onExpanded: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(expanded) {
        if (expanded) {
            onExpanded()
            bringIntoViewRequester.bringIntoView()
            delay(320L)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Surface(
        modifier =
        modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester),
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(FarmSpacing.dp12)) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = FarmDimensions.minTouchTarget)
                    .clip(FarmShapeTokens.inputField)
                    .farmCombinedClickable(
                        role = Role.Button,
                        speakLabel = title,
                        onLongPressSpeak = rememberHoldToSpeakAction(title),
                        onClick = onToggle,
                    )
                    .padding(horizontal = FarmSpacing.dp8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.Remove else Icons.Rounded.Add,
                        contentDescription = null,
                        tint = FarmColors.PrimaryForest,
                        modifier = Modifier.size(FarmDimensions.iconStandard),
                    )
                    Spacer(Modifier.width(FarmSpacing.dp8))
                    Text(
                        text = title,
                        style = FarmTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = FarmColors.PrimaryForest,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = FarmColors.SecondaryText,
                    modifier = Modifier.size(FarmDimensions.iconStandard),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = FarmSpacing.dp12),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Compact audio identity pill allowing farmers to hear (tap Speaker) or record (tap Mic)
 * a spoken name badge for workers, crops, or contacts without reading text.
 */
@Composable
fun FarmVoiceNameBadge(
    hasVoiceRecording: Boolean,
    isPlaying: Boolean,
    isRecording: Boolean,
    onPlayClick: () -> Unit,
    onRecordClick: () -> Unit,
    useTelugu: Boolean,
    modifier: Modifier = Modifier,
    showRecordAction: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier =
            Modifier
                .defaultMinSize(minWidth = FarmDimensions.minTouchTarget, minHeight = FarmDimensions.minTouchTarget)
                .clip(FarmShapeTokens.circular)
                .border(
                    1.dp,
                    if (isPlaying || hasVoiceRecording) FarmColors.PrimaryForest else FarmColors.Outline,
                    FarmShapeTokens.circular,
                )
                .farmCombinedClickable(
                    role = Role.Button,
                    speakLabel = if (useTelugu) "పేరు వినండి" else "Hear name",
                    onLongPressSpeak = rememberHoldToSpeakAction(if (useTelugu) "పేరు వినండి" else "Hear name"),
                    onClick = onPlayClick,
                ),
            shape = FarmShapeTokens.circular,
            color = if (isPlaying) FarmColors.PrimaryForest else FarmColors.PrimaryContainer,
        ) {
            Box(
                modifier = Modifier.padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp8),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (useTelugu) "వినండి" else "Listen",
                    tint = if (isPlaying) Color.White else FarmColors.OnPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (showRecordAction) {
            Surface(
                modifier =
                Modifier
                    .defaultMinSize(minWidth = FarmDimensions.minTouchTarget, minHeight = FarmDimensions.minTouchTarget)
                    .clip(FarmShapeTokens.circular)
                    .border(
                        1.dp,
                        if (isRecording) FarmColors.Error else FarmColors.Outline,
                        FarmShapeTokens.circular,
                    )
                    .farmCombinedClickable(
                        role = Role.Button,
                        speakLabel = if (useTelugu) "పేరు మాటలో రికార్డ్ చేయండి" else "Record spoken name",
                        onLongPressSpeak =
                        rememberHoldToSpeakAction(
                            if (useTelugu) "పేరు మాటలో రికార్డ్ చేయండి" else "Record spoken name",
                        ),
                        onClick = onRecordClick,
                    ),
                shape = FarmShapeTokens.circular,
                color = if (isRecording) FarmColors.ErrorContainer else FarmColors.SurfaceVariant,
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp8),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = if (useTelugu) "పేరు రికార్డ్" else "Record name",
                        tint = if (isRecording) FarmColors.Error else FarmColors.MainText,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
