package com.farmsaathi.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import com.farmsaathi.core.model.CurrentKarteInfo
import com.farmsaathi.core.model.LunarDayAlert
import com.farmsaathi.core.model.TeluguCalendarDay

@Composable
fun HomePanchangKarteBanner(
    calendarDay: TeluguCalendarDay?,
    karteInfo: CurrentKarteInfo?,
    alert: LunarDayAlert?,
    isPlayingAudio: Boolean,
    onPlayAudio: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (calendarDay == null || alert == null) return

    val isAlertPriority = alert.isPriority
    val cardBackground = if (isAlertPriority) FarmColors.WarningContainer else FarmColors.Surface
    val cardBorderColor = if (isAlertPriority) FarmColors.Warning else FarmColors.Outline

    Surface(
        shape = FarmShapeTokens.card,
        color = cardBackground,
        border = BorderStroke(1.dp, cardBorderColor),
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmSpacing.dp16),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
        ) {
            // Header Row: Panchang Date + Audio "వినండి" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                    modifier = Modifier.weight(1f),
                ) {
                    // Lunar Phase Disk indicator
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    calendarDay.isAmavasya -> FarmColors.MainText
                                    calendarDay.isPournami -> FarmColors.HarvestAmber
                                    else -> FarmColors.PrimaryForest
                                },
                            ),
                    )

                    // Month + Paksham + Tithi
                    Text(
                        text = "${calendarDay.teluguMonth.teluguName} • ${calendarDay.paksham.teluguName} ${calendarDay.tithi.teluguName}",
                        style = FarmTypography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FarmColors.MainText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // 1-Tap Audio Button
                Surface(
                    shape = FarmShapeTokens.tag,
                    color = if (isPlayingAudio) FarmColors.Warning else FarmColors.PrimaryContainer,
                    border = BorderStroke(1.dp, if (isPlayingAudio) FarmColors.Warning else FarmColors.Outline),
                    modifier = Modifier.clickable(role = Role.Button, onClick = onPlayAudio),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlayingAudio) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (isPlayingAudio) "ఆపండి" else "వినండి",
                            tint = if (isPlayingAudio) FarmColors.MainText else FarmColors.PrimaryForest,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (isPlayingAudio) "ఆపండి" else "వినండి",
                            style = FarmTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FarmColors.MainText,
                        )
                    }
                }
            }

            // Karte info row: [కార్తె పేరు] (రోజు X/Y)
            if (karteInfo != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp8),
                ) {
                    Surface(
                        shape = FarmShapeTokens.tag,
                        color = FarmColors.PrimaryContainer,
                        border = BorderStroke(0.5.dp, FarmColors.PrimaryForest),
                    ) {
                        Text(
                            text = "${karteInfo.teluguName} కార్తె",
                            style = FarmTypography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FarmColors.PrimaryForest,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    Text(
                        text = "${karteInfo.dayNumber}/${karteInfo.totalDays}వ రోజు • ఇంకా ${karteInfo.daysRemaining} రోజులు",
                        style = FarmTypography.labelSmall,
                        color = FarmColors.SecondaryText,
                    )
                }
            }

            // Dynamic Alert Content Box
            Surface(
                shape = FarmShapeTokens.tag,
                color = if (isAlertPriority) FarmColors.AmberContainer else FarmColors.SurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(FarmSpacing.dp12),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp4),
                ) {
                    Text(
                        text = alert.title,
                        style = FarmTypography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAlertPriority) FarmColors.Terracotta else FarmColors.MainText,
                    )
                    Text(
                        text = alert.subtitle,
                        style = FarmTypography.bodySmall,
                        color = FarmColors.SecondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Bottom Navigation Hint: "పంచాంగం చూడండి >"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "పంచాంగం & పనులు చూడండి",
                    style = FarmTypography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FarmColors.PrimaryForest,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = FarmColors.PrimaryForest,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}
