package com.farmsaathi.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FarmLightColorScheme =
    lightColorScheme(
        primary = FarmColors.PrimaryForest,
        onPrimary = FarmColors.OnPrimary,
        primaryContainer = FarmColors.PrimaryContainer,
        onPrimaryContainer = FarmColors.OnPrimaryContainer,
        secondary = FarmColors.SecondaryLeaf,
        onSecondary = FarmColors.OnPrimary,
        secondaryContainer = FarmColors.SecondaryContainer,
        onSecondaryContainer = FarmColors.OnSecondaryContainer,
        tertiary = FarmColors.HarvestAmber,
        onTertiary = FarmColors.OnHarvestAmber,
        tertiaryContainer = FarmColors.AmberContainer,
        onTertiaryContainer = FarmColors.OnAmberContainer,
        background = FarmColors.Background,
        onBackground = FarmColors.MainText,
        surface = FarmColors.Surface,
        onSurface = FarmColors.MainText,
        surfaceVariant = FarmColors.SurfaceVariant,
        onSurfaceVariant = FarmColors.SecondaryText,
        outline = FarmColors.Outline,
        outlineVariant = FarmColors.Outline,
        error = FarmColors.Error,
        onError = FarmColors.OnPrimary,
        errorContainer = FarmColors.ErrorContainer,
        onErrorContainer = FarmColors.Error,
    )

/**
 * Consistent light theme for outdoor farming use. Dynamic Android colors are intentionally disabled.
 */
@Composable
fun FarmSaathiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FarmLightColorScheme,
        typography = FarmTypography,
        shapes = FarmShapes,
        content = content,
    )
}
