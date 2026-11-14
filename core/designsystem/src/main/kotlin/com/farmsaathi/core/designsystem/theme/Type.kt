package com.farmsaathi.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Android 10 (API 29+) includes NotoSansTelugu and Roboto in /system/fonts/.
 * Using SansSerif delegates complex Telugu script shaping to HarfBuzz/Minikin with zero runtime download.
 */
val FarmFontFamily = FontFamily.SansSerif

val FarmTypography =
    Typography(
        displaySmall =
        TextStyle(
            fontFamily = FarmFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
        ),
        headlineMedium =
        TextStyle(
            fontFamily = FarmFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
        ),
        titleLarge =
        TextStyle(
            fontFamily = FarmFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
        ),
        titleMedium =
        TextStyle(
            fontFamily = FarmFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
        ),
        bodyLarge =
        TextStyle(
            fontFamily = FarmFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyMedium =
        TextStyle(
            fontFamily = FarmFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        ),
        bodySmall =
        TextStyle(
            fontFamily = FarmFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelLarge =
        TextStyle(
            fontFamily = FarmFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        labelMedium =
        TextStyle(
            fontFamily = FarmFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
    )
