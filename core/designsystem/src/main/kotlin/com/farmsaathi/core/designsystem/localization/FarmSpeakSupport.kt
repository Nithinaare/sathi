package com.farmsaathi.core.designsystem.localization

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

/**
 * App-wide handler invoked when the farmer press-and-holds a control to hear spoken meaning.
 */
val LocalFarmSpeakHandler = staticCompositionLocalOf<(String) -> Unit> { {} }

@Composable
fun rememberHoldToSpeakAction(speakText: String?): (() -> Unit)? {
    val handler = LocalFarmSpeakHandler.current
    val haptic = LocalHapticFeedback.current
    return remember(speakText, handler, haptic) {
        val trimmed = speakText?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            null
        } else {
            {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                handler(trimmed)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.farmCombinedClickable(
    enabled: Boolean = true,
    role: Role? = null,
    speakLabel: String? = null,
    onLongPressSpeak: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier =
    combinedClickable(
        enabled = enabled,
        role = role,
        onLongClickLabel = speakLabel,
        onLongClick = onLongPressSpeak,
        onClick = onClick,
    )
