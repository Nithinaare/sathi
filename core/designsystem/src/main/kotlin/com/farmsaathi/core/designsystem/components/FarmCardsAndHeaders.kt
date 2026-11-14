package com.farmsaathi.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.localization.farmCombinedClickable
import com.farmsaathi.core.designsystem.localization.rememberHoldToSpeakAction
import com.farmsaathi.core.designsystem.localization.safeFarmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography

enum class FarmStatusType {
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    NEUTRAL,
}

enum class FarmBadgeStyle {
    FILLED_SOFT,
    OUTLINED,
}

@Composable
fun FarmStatusIndicator(
    type: FarmStatusType,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: FarmBadgeStyle = FarmBadgeStyle.FILLED_SOFT,
    maxLines: Int = 3,
) {
    val palette =
        when (type) {
            FarmStatusType.SUCCESS ->
                StatusPalette(
                    FarmColors.SuccessContainer,
                    FarmColors.Success,
                    FarmColors.Success,
                    Icons.Rounded.CheckCircle,
                )
            FarmStatusType.WARNING ->
                StatusPalette(
                    FarmColors.WarningContainer,
                    FarmColors.Warning,
                    FarmColors.HarvestAmber,
                    Icons.Rounded.WarningAmber,
                )
            FarmStatusType.ERROR ->
                StatusPalette(
                    FarmColors.ErrorContainer,
                    FarmColors.Error,
                    FarmColors.Error,
                    Icons.Rounded.ErrorOutline,
                )
            FarmStatusType.INFO ->
                StatusPalette(
                    FarmColors.InfoContainer,
                    FarmColors.Info,
                    FarmColors.Info,
                    Icons.Rounded.Info,
                )
            FarmStatusType.NEUTRAL ->
                StatusPalette(
                    FarmColors.SurfaceVariant,
                    FarmColors.SecondaryText,
                    FarmColors.Outline,
                    Icons.Rounded.Schedule,
                )
        }
    val container = if (style == FarmBadgeStyle.FILLED_SOFT) palette.bg else FarmColors.Surface

    Surface(
        modifier = modifier,
        shape = FarmShapeTokens.tag,
        color = container,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FarmSpacing.dp8, vertical = FarmSpacing.dp4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon ?: palette.defaultIcon,
                contentDescription = null,
                tint = palette.fg,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(FarmSpacing.dp4))
            Text(
                text = label,
                style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = palette.fg,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun FarmPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    backContentDescription: String = safeFarmString(R.string.common_back),
    audioCueKey: String? = null,
    isAudioPlaying: Boolean = false,
    onToggleAudio: ((String) -> Unit)? = null,
    audioContentDescription: String = safeFarmString(R.string.common_listen),
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = FarmColors.Surface,
    ) {
        Column {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBackClick != null) {
                    FarmIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = backContentDescription,
                        onClick = onBackClick,
                        containerColor = FarmColors.SurfaceVariant,
                    )
                    Spacer(Modifier.width(FarmSpacing.dp12))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = FarmTypography.headlineMedium,
                        color = FarmColors.MainText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(FarmSpacing.dp4))
                        Text(
                            text = subtitle,
                            style = FarmTypography.bodySmall,
                            color = FarmColors.SecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (audioCueKey != null && onToggleAudio != null) {
                    Spacer(Modifier.width(FarmSpacing.dp8))
                    FarmAudioButton(
                        audioCueKey = audioCueKey,
                        isPlaying = isAudioPlaying,
                        onTogglePlay = onToggleAudio,
                        contentDescription = audioContentDescription,
                    )
                }
                if (trailingAction != null) {
                    Spacer(Modifier.width(FarmSpacing.dp4))
                    trailingAction()
                }
            }
            HorizontalDivider(thickness = 1.dp, color = FarmColors.Outline)
        }
    }
}

@Composable
fun FarmSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = FarmTypography.titleLarge,
            color = FarmColors.MainText,
            modifier =
            Modifier
                .weight(1f)
                .semantics { heading() },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.defaultMinSize(minHeight = FarmDimensions.minTouchTarget),
            ) {
                Text(
                    text = actionLabel,
                    style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FarmColors.PrimaryForest,
                )
            }
        }
    }
}

@Composable
fun FarmSummaryCard(
    title: String,
    primaryValue: String,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    secondaryMetrics: List<Pair<String, String>> = emptyList(),
    status: Pair<FarmStatusType, String>? = null,
    containerColor: Color = FarmColors.Surface,
    accentStripColor: Color? = FarmColors.PrimaryForest,
    audioCueKey: String? = null,
    isAudioPlaying: Boolean = false,
    onToggleAudio: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    speakDescription: String? = null,
) {
    val autoSpeak =
        speakDescription
            ?: buildString {
                append(title)
                if (!primaryLabel.isNullOrBlank()) append(". ").append(primaryLabel)
                append(" ").append(primaryValue)
                if (status != null) append(". ").append(status.second)
                secondaryMetrics.forEach { (l, v) -> append(". ").append(l).append(" ").append(v) }
            }
    val onHoldToSpeak = rememberHoldToSpeakAction(autoSpeak)

    Surface(
        modifier =
        modifier
            .fillMaxWidth()
            .farmCombinedClickable(
                enabled = onClick != null || onHoldToSpeak != null,
                speakLabel = autoSpeak,
                onLongPressSpeak = onHoldToSpeak,
                onClick = { onClick?.invoke() },
            ),
        shape = FarmShapeTokens.card,
        color = containerColor,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            if (accentStripColor != null) {
                Box(
                    modifier =
                    Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(accentStripColor),
                )
            }
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(FarmSpacing.cardPadding),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        style = FarmTypography.titleMedium,
                        color = FarmColors.MainText,
                        modifier = Modifier.weight(1f),
                    )
                    if (audioCueKey != null && onToggleAudio != null) {
                        FarmAudioButton(
                            audioCueKey = audioCueKey,
                            isPlaying = isAudioPlaying,
                            onTogglePlay = onToggleAudio,
                            contentDescription = title,
                        )
                    }
                }
                if (!primaryLabel.isNullOrBlank()) {
                    Spacer(Modifier.height(FarmSpacing.dp4))
                    Text(
                        text = primaryLabel,
                        style = FarmTypography.bodySmall,
                        color = FarmColors.SecondaryText,
                    )
                }
                Spacer(Modifier.height(FarmSpacing.dp8))
                Text(
                    text = primaryValue,
                    style = FarmTypography.displaySmall,
                    color = FarmColors.MainText,
                )
                if (status != null) {
                    Spacer(Modifier.height(FarmSpacing.dp8))
                    FarmStatusIndicator(type = status.first, label = status.second)
                }
                if (secondaryMetrics.isNotEmpty()) {
                    Spacer(Modifier.height(FarmSpacing.dp12))
                    HorizontalDivider(color = FarmColors.Outline)
                    Spacer(Modifier.height(FarmSpacing.dp12))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                    ) {
                        secondaryMetrics.take(3).forEach { (label, value) ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = FarmTypography.bodySmall,
                                    color = FarmColors.SecondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = value,
                                    style = FarmTypography.titleMedium,
                                    color = FarmColors.MainText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FarmRecordCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    metadata: String? = null,
    amountOrMetric: String? = null,
    metricColor: Color = FarmColors.MainText,
    leadingIcon: ImageVector? = null,
    @DrawableRes leadingImageRes: Int? = null,
    leadingIconContainerColor: Color = FarmColors.PrimaryContainer,
    leadingIconTint: Color = FarmColors.PrimaryForest,
    statusType: FarmStatusType? = null,
    statusLabel: String? = null,
    hasPhoto: Boolean = false,
    hasAudio: Boolean = false,
    speakDescription: String? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val speakTarget =
        speakDescription
            ?: listOfNotNull(title, subtitle, amountOrMetric, statusLabel).joinToString(". ")
    val onHoldToSpeak = rememberHoldToSpeakAction(speakTarget)

    Surface(
        modifier =
        modifier
            .fillMaxWidth()
            .clip(FarmShapeTokens.card)
            .border(1.dp, FarmColors.Outline, FarmShapeTokens.card)
            .farmCombinedClickable(
                speakLabel = speakTarget,
                onLongPressSpeak = onHoldToSpeak,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
    ) {
        Row(
            modifier = Modifier.padding(FarmSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingImageRes != null) {
                Box(
                    modifier =
                    Modifier
                        .size(52.dp)
                        .clip(FarmShapeTokens.inputField)
                        .border(1.dp, FarmColors.Outline, FarmShapeTokens.inputField),
                ) {
                    Image(
                        painter = painterResource(leadingImageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.width(FarmSpacing.dp12))
            } else if (leadingIcon != null) {
                Box(
                    modifier =
                    Modifier
                        .size(44.dp)
                        .clip(FarmShapeTokens.inputField)
                        .background(leadingIconContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = leadingIconTint,
                        modifier = Modifier.size(FarmDimensions.iconStandard),
                    )
                }
                Spacer(Modifier.width(FarmSpacing.dp12))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = FarmTypography.titleMedium,
                    color = FarmColors.MainText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(FarmSpacing.dp4))
                    Text(
                        text = subtitle,
                        style = FarmTypography.bodySmall,
                        color = FarmColors.SecondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!metadata.isNullOrBlank() || hasPhoto || hasAudio) {
                    Spacer(Modifier.height(FarmSpacing.dp4))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!metadata.isNullOrBlank()) {
                            Text(
                                text = metadata,
                                style = FarmTypography.labelMedium,
                                color = FarmColors.SecondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        if (hasPhoto) {
                            Spacer(Modifier.width(FarmSpacing.dp8))
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                tint = FarmColors.SecondaryText,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        if (hasAudio) {
                            Spacer(Modifier.width(FarmSpacing.dp4))
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = FarmColors.SecondaryText,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            if (!amountOrMetric.isNullOrBlank() || (statusType != null && !statusLabel.isNullOrBlank())) {
                Spacer(Modifier.width(FarmSpacing.dp12))
                Column(horizontalAlignment = Alignment.End) {
                    if (!amountOrMetric.isNullOrBlank()) {
                        Text(
                            text = amountOrMetric,
                            style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = metricColor,
                            maxLines = 1,
                        )
                    }
                    if (statusType != null && !statusLabel.isNullOrBlank()) {
                        Spacer(Modifier.height(FarmSpacing.dp4))
                        FarmStatusIndicator(type = statusType, label = statusLabel)
                    }
                }
            }

            if (trailingContent != null) {
                Spacer(Modifier.width(FarmSpacing.dp8))
                trailingContent()
            }
        }
    }
}

@Composable
fun FarmListItem(
    title: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = FarmColors.PrimaryForest,
    trailingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    enabled: Boolean = true,
    speakDescription: String? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    selected: Boolean? = null,
) {
    val speakTarget = speakDescription ?: listOfNotNull(title, subtitle, trailingText).joinToString(". ")
    val onHoldToSpeak = rememberHoldToSpeakAction(speakTarget)
    val togglePair = if (checked != null && onCheckedChange != null) checked to onCheckedChange else null
    val interactionModifier =
        if (togglePair != null) {
            Modifier.toggleable(
                value = togglePair.first,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = togglePair.second,
            )
        } else {
            Modifier.farmCombinedClickable(
                enabled = enabled,
                role = Role.Button,
                speakLabel = speakTarget,
                onLongPressSpeak = onHoldToSpeak,
                onClick = onClick,
            )
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .then(interactionModifier)
                .padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp12)
                .semantics(mergeDescendants = true) {
                    selected?.let { this.selected = it }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (enabled) leadingTint else FarmColors.Disabled,
                    modifier = Modifier.size(FarmDimensions.iconStandard),
                )
                Spacer(Modifier.width(FarmSpacing.dp16))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = FarmTypography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (enabled) FarmColors.MainText else FarmColors.Disabled,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(FarmSpacing.dp4))
                    Text(
                        text = subtitle,
                        style = FarmTypography.bodySmall,
                        color = FarmColors.SecondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(Modifier.width(FarmSpacing.dp8))
                trailingContent()
            } else if (togglePair != null) {
                Spacer(Modifier.width(FarmSpacing.dp8))
                Switch(
                    checked = togglePair.first,
                    onCheckedChange = null,
                    enabled = enabled,
                    colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = FarmColors.OnPrimary,
                        checkedTrackColor = FarmColors.PrimaryForest,
                    ),
                )
            } else if (!trailingText.isNullOrBlank()) {
                Spacer(Modifier.width(FarmSpacing.dp8))
                Text(
                    text = trailingText,
                    style = FarmTypography.bodyMedium,
                    color = FarmColors.SecondaryText,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = FarmColors.SecondaryText,
                    modifier = Modifier.size(FarmDimensions.iconStandard),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(thickness = 1.dp, color = FarmColors.Outline)
        }
    }
}

private data class StatusPalette(
    val bg: Color,
    val fg: Color,
    val border: Color,
    val defaultIcon: ImageVector,
)
