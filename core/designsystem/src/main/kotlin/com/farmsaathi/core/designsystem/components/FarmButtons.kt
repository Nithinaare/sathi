package com.farmsaathi.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.localization.LocalFarmSpeakHandler
import com.farmsaathi.core.designsystem.localization.farmCombinedClickable
import com.farmsaathi.core.designsystem.localization.rememberHoldToSpeakAction
import com.farmsaathi.core.designsystem.localization.safeFarmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography

@Composable
fun FarmPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isHero: Boolean = false,
    isDestructive: Boolean = false,
    speakDescription: String? = null,
) {
    val container =
        when {
            !enabled -> FarmColors.SurfaceVariant
            isDestructive -> FarmColors.Error
            else -> FarmColors.PrimaryForest
        }
    val content = if (enabled) FarmColors.OnPrimary else FarmColors.Disabled
    val minHeight = if (isHero) FarmDimensions.primaryButtonHeight else 56.dp
    val speakTarget = speakDescription ?: text
    val onHoldToSpeak = rememberHoldToSpeakAction(speakTarget)

    Surface(
        modifier =
        modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = FarmDimensions.minTouchTarget, minHeight = minHeight)
            .clip(FarmShapeTokens.inputField)
            .farmCombinedClickable(
                enabled = enabled && !isLoading,
                role = Role.Button,
                speakLabel = speakTarget,
                onLongPressSpeak = onHoldToSpeak,
                onClick = onClick,
            ),
        shape = FarmShapeTokens.inputField,
        color = container,
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = FarmSpacing.dp20, vertical = FarmSpacing.dp12),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = content,
                    strokeWidth = 2.5.dp,
                )
                Spacer(Modifier.width(FarmSpacing.dp12))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(FarmDimensions.iconStandard),
                )
                Spacer(Modifier.width(FarmSpacing.dp12))
            }
            Text(
                text = text,
                style = FarmTypography.titleMedium,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun FarmSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    useMutedOutline: Boolean = false,
    speakDescription: String? = null,
) {
    val borderColor =
        when {
            !enabled -> FarmColors.Outline
            useMutedOutline -> FarmColors.Outline
            else -> FarmColors.PrimaryForest
        }
    val textColor =
        when {
            !enabled -> FarmColors.Disabled
            useMutedOutline -> FarmColors.MainText
            else -> FarmColors.PrimaryForest
        }
    val speakTarget = speakDescription ?: text
    val onHoldToSpeak = rememberHoldToSpeakAction(speakTarget)

    Surface(
        modifier =
        modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = FarmDimensions.minTouchTarget, minHeight = 52.dp)
            .clip(FarmShapeTokens.inputField)
            .border(1.5.dp, borderColor, FarmShapeTokens.inputField)
            .farmCombinedClickable(
                enabled = enabled,
                role = Role.Button,
                speakLabel = speakTarget,
                onLongPressSpeak = onHoldToSpeak,
                onClick = onClick,
            ),
        shape = FarmShapeTokens.inputField,
        color = FarmColors.Surface,
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp12),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(FarmDimensions.iconStandard),
                )
                Spacer(Modifier.width(FarmSpacing.dp8))
            }
            Text(
                text = text,
                style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun FarmActionTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int? = null,
    subtitle: String? = null,
    containerColor: Color = FarmColors.Surface,
    iconTint: Color = FarmColors.PrimaryForest,
    borderColor: Color = FarmColors.Outline,
    compact: Boolean = false,
    badgeText: String? = null,
    enabled: Boolean = true,
    speakDescription: String? = null,
) {
    val shape = if (compact) FarmShapeTokens.card else FarmShapeTokens.actionTile
    val minHeight = if (compact) 68.dp else 96.dp
    val speakTarget =
        remember(speakDescription, title, subtitle) {
            speakDescription ?: listOfNotNull(title, subtitle).joinToString(". ")
        }
    val onHoldToSpeak = rememberHoldToSpeakAction(speakTarget)

    Surface(
        modifier =
        modifier
            .defaultMinSize(minWidth = FarmDimensions.minTouchTarget, minHeight = minHeight)
            .clip(shape)
            .border(1.dp, if (enabled) borderColor else FarmColors.Outline, shape)
            .farmCombinedClickable(
                enabled = enabled,
                role = Role.Button,
                speakLabel = speakTarget,
                onLongPressSpeak = onHoldToSpeak,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                this.contentDescription = title
            },
        shape = shape,
        color = if (enabled) containerColor else FarmColors.SurfaceVariant,
    ) {
        if (compact) {
            Row(
                modifier = Modifier.padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TileIconBox(icon = icon, imageRes = imageRes, tint = iconTint, enabled = enabled, boxSize = 52.dp)
                Spacer(Modifier.width(FarmSpacing.dp8))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = FarmTypography.titleMedium,
                        color = if (enabled) FarmColors.MainText else FarmColors.Disabled,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = FarmTypography.bodySmall,
                            color = FarmColors.SecondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!badgeText.isNullOrBlank()) {
                    Spacer(Modifier.width(FarmSpacing.dp8))
                    SmallTagBadge(badgeText)
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(FarmSpacing.dp16),
                verticalArrangement = Arrangement.Top,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    TileIconBox(icon = icon, imageRes = imageRes, tint = iconTint, enabled = enabled, boxSize = 56.dp)
                    if (!badgeText.isNullOrBlank()) {
                        SmallTagBadge(badgeText)
                    }
                }
                Spacer(Modifier.height(FarmSpacing.dp12))
                Text(
                    text = title,
                    style = FarmTypography.titleMedium,
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
        }
    }
}

@Composable
fun FarmVisualGridTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int? = null,
    subtitle: String? = null,
    fullBleedImage: Boolean = false,
    containerColor: Color = FarmColors.Surface,
    iconTint: Color = FarmColors.PrimaryForest,
    borderColor: Color = FarmColors.Outline,
    enabled: Boolean = true,
    speakDescription: String? = null,
) {
    val speakTarget =
        remember(speakDescription, title, subtitle) {
            speakDescription ?: listOfNotNull(title, subtitle).joinToString(". ")
        }
    val onHoldToSpeak = rememberHoldToSpeakAction(speakTarget)
    val speakHandler = LocalFarmSpeakHandler.current
    val useFullBleed = fullBleedImage && imageRes != null
    val minTileHeight = if (useFullBleed) 152.dp else 116.dp

    Box(
        modifier =
        modifier
            .defaultMinSize(minWidth = FarmDimensions.minTouchTarget, minHeight = minTileHeight),
    ) {
        Surface(
            modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minWidth = FarmDimensions.minTouchTarget, minHeight = minTileHeight)
                .clip(FarmShapeTokens.actionTile)
                .border(1.dp, if (enabled) borderColor else FarmColors.Outline, FarmShapeTokens.actionTile)
                .farmCombinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    speakLabel = speakTarget,
                    onLongPressSpeak = onHoldToSpeak,
                    onClick = onClick,
                )
                .semantics(mergeDescendants = true) {
                    this.contentDescription = title
                },
            shape = FarmShapeTokens.actionTile,
            color = if (enabled) containerColor else FarmColors.SurfaceVariant,
        ) {
            if (useFullBleed) {
                FullBleedTileContent(
                    imageRes = checkNotNull(imageRes),
                    title = title,
                    enabled = enabled,
                    accentColor = borderColor,
                )
            } else {
                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp12),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    TileIconBox(
                        icon = icon,
                        imageRes = imageRes,
                        tint = iconTint,
                        enabled = enabled,
                        boxSize = 58.dp,
                    )
                    Spacer(Modifier.height(FarmSpacing.dp8))
                    Text(
                        text = title,
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (enabled) FarmColors.MainText else FarmColors.Disabled,
                        textAlign = TextAlign.Center,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(FarmSpacing.dp4))
                        Text(
                            text = subtitle,
                            style = FarmTypography.bodySmall,
                            color = FarmColors.SecondaryText,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (useFullBleed && !speakDescription.isNullOrBlank()) {
            val listenLabel = "${safeFarmString(R.string.common_listen)}: $title"
            Box(
                modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .size(FarmDimensions.minTouchTarget)
                    .clip(FarmShapeTokens.circular)
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClickLabel = listenLabel,
                    ) {
                        speakHandler(speakDescription)
                    }
                    .semantics {
                        this.contentDescription = listenLabel
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                    Modifier
                        .clip(FarmShapeTokens.circular)
                        .background(SpeakerPillBackground)
                        .border(1.dp, borderColor.copy(alpha = 0.7f), FarmShapeTokens.circular)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FullBleedTileContent(
    @DrawableRes imageRes: Int,
    title: String,
    enabled: Boolean,
    accentColor: Color = FarmColors.PrimaryForest,
) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 152.dp),
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier =
            Modifier
                .matchParentSize()
                .background(FullBleedTileScrimBrush),
        )
        Row(
            modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp12),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                Modifier
                    .size(8.dp)
                    .clip(FarmShapeTokens.circular)
                    .background(accentColor),
            )
            Spacer(Modifier.width(FarmSpacing.dp8))
            Text(
                text = title,
                style = FarmTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) Color.White else FarmColors.Disabled,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun TileIconBox(
    icon: ImageVector,
    tint: Color,
    enabled: Boolean,
    @DrawableRes imageRes: Int? = null,
    boxSize: androidx.compose.ui.unit.Dp = 48.dp,
) {
    if (imageRes != null) {
        Box(
            modifier =
            Modifier
                .size(boxSize)
                .clip(FarmShapeTokens.inputField)
                .border(1.dp, FarmColors.Outline, FarmShapeTokens.inputField),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        Box(
            modifier =
            Modifier
                .size(boxSize)
                .clip(FarmShapeTokens.inputField)
                .background(FarmColors.PrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) tint else FarmColors.Disabled,
                modifier = Modifier.size(FarmDimensions.iconMainAction - 4.dp),
            )
        }
    }
}

@Composable
private fun SmallTagBadge(text: String) {
    Box(
        modifier =
        Modifier
            .clip(FarmShapeTokens.tag)
            .background(FarmColors.AmberContainer)
            .padding(horizontal = FarmSpacing.dp8, vertical = FarmSpacing.dp4),
    ) {
        Text(
            text = text,
            style = FarmTypography.labelMedium,
            color = FarmColors.OnAmberContainer,
            maxLines = 1,
        )
    }
}

@Composable
fun FarmIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = FarmColors.MainText,
    containerColor: Color = Color.Transparent,
    enabled: Boolean = true,
    speakDescription: String? = null,
) {
    val speakTarget = speakDescription ?: contentDescription
    val onHoldToSpeak = rememberHoldToSpeakAction(speakTarget)
    Surface(
        modifier =
        modifier
            .size(FarmDimensions.minTouchTarget)
            .clip(FarmShapeTokens.circular)
            .farmCombinedClickable(
                enabled = enabled,
                role = Role.Button,
                speakLabel = speakTarget,
                onLongPressSpeak = onHoldToSpeak,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        shape = FarmShapeTokens.circular,
        color = containerColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) tint else FarmColors.Disabled,
                modifier = Modifier.size(FarmDimensions.iconStandard),
            )
        }
    }
}

@Composable
fun FarmAudioButton(
    audioCueKey: String,
    isPlaying: Boolean,
    onTogglePlay: (String) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    speakDescription: String? = null,
) {
    val container = if (isPlaying) FarmColors.PrimaryForest else FarmColors.PrimaryContainer
    val content = if (isPlaying) FarmColors.OnPrimary else FarmColors.OnPrimaryContainer
    val icon = if (isPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp
    val speakTarget = speakDescription ?: label ?: contentDescription
    val onHoldToSpeak = rememberHoldToSpeakAction(speakTarget)

    Surface(
        modifier =
        modifier
            .defaultMinSize(minWidth = FarmDimensions.minTouchTarget, minHeight = FarmDimensions.minTouchTarget)
            .clip(FarmShapeTokens.inputField)
            .border(1.dp, FarmColors.PrimaryForest.copy(alpha = 0.25f), FarmShapeTokens.inputField)
            .farmCombinedClickable(
                role = Role.Button,
                speakLabel = speakTarget,
                onLongPressSpeak = onHoldToSpeak,
            ) { onTogglePlay(audioCueKey) }
            .semantics {
                this.contentDescription = contentDescription
                this.stateDescription = if (isPlaying) "Playing" else "Stopped"
            },
        shape = FarmShapeTokens.inputField,
        color = container,
    ) {
        Row(
            modifier =
            Modifier.padding(
                horizontal = if (label != null) FarmSpacing.dp12 else FarmSpacing.dp12,
                vertical = FarmSpacing.dp8,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(FarmDimensions.iconStandard),
            )
            if (!label.isNullOrBlank()) {
                Spacer(Modifier.width(FarmSpacing.dp8))
                Text(
                    text = label,
                    style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}

private val FullBleedTileScrimBrush =
    Brush.verticalGradient(
        colorStops =
        arrayOf(
            0.0f to Color.Black.copy(alpha = 0.08f),
            0.48f to Color.Black.copy(alpha = 0.25f),
            0.74f to Color.Black.copy(alpha = 0.78f),
            1.0f to Color.Black.copy(alpha = 0.90f),
        ),
    )

private val SpeakerPillBackground = Color.Black.copy(alpha = 0.56f)
