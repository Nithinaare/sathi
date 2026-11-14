package com.farmsaathi.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object FarmSpacing {
    val dp4 = 4.dp
    val dp8 = 8.dp
    val dp12 = 12.dp
    val dp16 = 16.dp
    val dp20 = 20.dp
    val dp24 = 24.dp
    val dp32 = 32.dp
    val dp40 = 40.dp

    val screenHorizontal = dp16
    val cardPadding = dp16
    val sectionGap = dp24
    val itemGap = dp12
}

object FarmDimensions {
    val minTouchTarget = 48.dp
    val primaryButtonHeight = 64.dp
    val actionTileHeight = 96.dp
    val audioButtonSize = 56.dp
    val iconStandard = 24.dp
    val iconMainAction = 36.dp
}

object FarmShapeTokens {
    val actionTile = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(16.dp)
    val inputField = RoundedCornerShape(12.dp)
    val tag = RoundedCornerShape(8.dp)
    val circular = CircleShape
}

val FarmShapes =
    Shapes(
        extraSmall = FarmShapeTokens.tag,
        small = FarmShapeTokens.tag,
        medium = FarmShapeTokens.inputField,
        large = FarmShapeTokens.card,
        extraLarge = FarmShapeTokens.actionTile,
    )
