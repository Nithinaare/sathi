package com.farmsaathi.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farmsaathi.core.designsystem.localization.farmCombinedClickable
import com.farmsaathi.core.designsystem.localization.rememberHoldToSpeakAction
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmDimensions
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography

data class FarmNavTab(
    val routeId: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val contentDescription: String = label,
    val speakText: String = label,
)

enum class SnackbarTone { INFO, SUCCESS, WARNING, ERROR }

@Composable
fun FarmBottomNavigation(
    tabs: List<FarmNavTab>,
    selectedRouteId: String,
    onTabSelected: (FarmNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(tabs.size <= 4) { "Bottom navigation must have at most 4 destinations" }
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .background(FarmColors.Surface)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(thickness = 1.dp, color = FarmColors.Outline)
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = FarmSpacing.dp8, vertical = FarmSpacing.dp4),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.routeId == selectedRouteId
                val holdToSpeak = rememberHoldToSpeakAction(tab.speakText)
                val iconTint = if (isSelected) FarmColors.PrimaryForest else FarmColors.SecondaryText
                val textColor = if (isSelected) FarmColors.PrimaryForest else FarmColors.SecondaryText

                Column(
                    modifier =
                    Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(FarmShapeTokens.card)
                        .farmCombinedClickable(
                            role = Role.Tab,
                            speakLabel = tab.speakText,
                            onLongPressSpeak = holdToSpeak,
                            onClick = { onTabSelected(tab) },
                        )
                        .semantics { selected = isSelected },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier =
                        Modifier
                            .clip(FarmShapeTokens.circular)
                            .background(if (isSelected) FarmColors.PrimaryContainer else Color.Transparent)
                            .padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp4),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(FarmDimensions.iconStandard),
                        )
                    }
                    Spacer(Modifier.height(FarmSpacing.dp4))
                    Text(
                        text = tab.label,
                        style =
                        FarmTypography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = textColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun FarmConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
    icon: ImageVector? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FarmColors.Surface,
        shape = FarmShapeTokens.card,
        icon =
        icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isDestructive) FarmColors.Error else FarmColors.PrimaryForest,
                    modifier = Modifier.size(36.dp),
                )
            }
        },
        title = {
            Text(
                text = title,
                style = FarmTypography.titleLarge,
                color = FarmColors.MainText,
            )
        },
        text = {
            Text(
                text = message,
                style = FarmTypography.bodyLarge,
                color = FarmColors.SecondaryText,
            )
        },
        confirmButton = {
            FarmPrimaryButton(
                text = confirmLabel,
                onClick = onConfirm,
                isDestructive = isDestructive,
            )
        },
        dismissButton = {
            FarmSecondaryButton(
                text = dismissLabel,
                onClick = onDismiss,
                useMutedOutline = true,
            )
        },
    )
}

@Composable
fun FarmSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    tone: SnackbarTone = SnackbarTone.INFO,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val (bg, fg, icon) =
        when (tone) {
            SnackbarTone.SUCCESS -> Triple(FarmColors.Success, FarmColors.OnPrimary, Icons.Rounded.CheckCircle)
            SnackbarTone.WARNING -> Triple(FarmColors.AmberContainer, FarmColors.OnAmberContainer, Icons.Rounded.WarningAmber)
            SnackbarTone.ERROR -> Triple(FarmColors.Error, FarmColors.OnPrimary, Icons.Rounded.ErrorOutline)
            SnackbarTone.INFO -> Triple(FarmColors.Info, FarmColors.OnPrimary, Icons.Rounded.Info)
        }

    Surface(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp8)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = FarmShapeTokens.card,
        color = bg,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(FarmDimensions.iconStandard),
            )
            Spacer(Modifier.width(FarmSpacing.dp12))
            Text(
                text = message,
                style = FarmTypography.bodyLarge,
                color = fg,
                modifier = Modifier.weight(1f),
            )
            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    Text(
                        text = actionLabel,
                        style = FarmTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = fg,
                    )
                }
            }
        }
    }
}

@Composable
fun FarmEmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    audioCueKey: String? = null,
    isAudioPlaying: Boolean = false,
    onToggleAudio: ((String) -> Unit)? = null,
    audioLabel: String? = null,
) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(FarmSpacing.dp32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (imageRes != null) {
            Box(
                modifier =
                Modifier
                    .size(88.dp)
                    .clip(FarmShapeTokens.circular)
                    .border(2.dp, FarmColors.Outline, FarmShapeTokens.circular),
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
                    .size(80.dp)
                    .clip(FarmShapeTokens.circular)
                    .background(FarmColors.PrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FarmColors.PrimaryForest,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Spacer(Modifier.height(FarmSpacing.dp20))
        Text(
            text = title,
            style = FarmTypography.titleLarge,
            color = FarmColors.MainText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FarmSpacing.dp8))
        Text(
            text = message,
            style = FarmTypography.bodyLarge,
            color = FarmColors.SecondaryText,
            textAlign = TextAlign.Center,
        )
        if (audioCueKey != null && onToggleAudio != null) {
            Spacer(Modifier.height(FarmSpacing.dp16))
            FarmAudioButton(
                audioCueKey = audioCueKey,
                isPlaying = isAudioPlaying,
                onTogglePlay = onToggleAudio,
                label = audioLabel,
                contentDescription = title,
            )
        }
        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            Spacer(Modifier.height(FarmSpacing.dp24))
            FarmPrimaryButton(text = actionLabel, onClick = onActionClick)
        }
    }
}

@Composable
fun FarmLoadingState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .padding(FarmSpacing.dp32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(44.dp),
            color = FarmColors.PrimaryForest,
            strokeWidth = 4.dp,
        )
        Spacer(Modifier.height(FarmSpacing.dp16))
        Text(
            text = message,
            style = FarmTypography.bodyLarge,
            color = FarmColors.SecondaryText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun FarmErrorState(
    title: String,
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(FarmSpacing.dp32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
            Modifier
                .size(72.dp)
                .clip(FarmShapeTokens.circular)
                .background(FarmColors.ErrorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = FarmColors.Error,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(FarmSpacing.dp16))
        Text(
            text = title,
            style = FarmTypography.titleLarge,
            color = FarmColors.MainText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FarmSpacing.dp8))
        Text(
            text = message,
            style = FarmTypography.bodyLarge,
            color = FarmColors.SecondaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FarmSpacing.dp24))
        FarmPrimaryButton(text = retryLabel, onClick = onRetry)
        if (!secondaryLabel.isNullOrBlank() && onSecondaryClick != null) {
            Spacer(Modifier.height(FarmSpacing.dp12))
            FarmSecondaryButton(text = secondaryLabel, onClick = onSecondaryClick, useMutedOutline = true)
        }
    }
}

@Composable
fun FarmPermissionExplanation(
    title: String,
    explanation: String,
    icon: ImageVector,
    grantLabel: String,
    denyLabel: String,
    onGrantClick: () -> Unit,
    onDenyClick: () -> Unit,
    modifier: Modifier = Modifier,
    audioCueKey: String? = null,
    isAudioPlaying: Boolean = false,
    onToggleAudio: ((String) -> Unit)? = null,
    audioLabel: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        Column(modifier = Modifier.padding(FarmSpacing.dp20)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                    Modifier
                        .size(48.dp)
                        .clip(FarmShapeTokens.inputField)
                        .background(FarmColors.PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = FarmColors.PrimaryForest,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(FarmSpacing.dp12))
                Text(
                    text = title,
                    style = FarmTypography.titleLarge,
                    color = FarmColors.MainText,
                    modifier = Modifier.weight(1f),
                )
                if (audioCueKey != null && onToggleAudio != null) {
                    FarmAudioButton(
                        audioCueKey = audioCueKey,
                        isPlaying = isAudioPlaying,
                        onTogglePlay = onToggleAudio,
                        label = audioLabel,
                        contentDescription = title,
                    )
                }
            }
            Spacer(Modifier.height(FarmSpacing.dp12))
            Text(
                text = explanation,
                style = FarmTypography.bodyLarge,
                color = FarmColors.SecondaryText,
            )
            Spacer(Modifier.height(FarmSpacing.dp20))
            FarmPrimaryButton(text = grantLabel, onClick = onGrantClick)
            Spacer(Modifier.height(FarmSpacing.dp12))
            FarmSecondaryButton(text = denyLabel, onClick = onDenyClick, useMutedOutline = true)
        }
    }
}

@Composable
fun FarmStickyFooterBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = FarmColors.Surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = FarmColors.Outline)
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = FarmSpacing.dp16, vertical = FarmSpacing.dp12),
                content = content,
            )
        }
    }
}
