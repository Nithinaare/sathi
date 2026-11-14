package com.farmsaathi.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmShapeTokens
import com.farmsaathi.core.designsystem.theme.FarmSpacing
import com.farmsaathi.core.designsystem.theme.FarmTypography
import java.io.File

sealed interface VoiceRecordingState {
    data object Idle : VoiceRecordingState

    data class Recording(val elapsedMillis: Long, val amplitudeNormalized: Float = 0f) : VoiceRecordingState

    data class Recorded(val filePath: String, val durationMillis: Long) : VoiceRecordingState
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FarmPhotoCapture(
    photoPaths: List<String>,
    onTakePhotoClick: () -> Unit,
    onPickFromGalleryClick: () -> Unit,
    onRemovePhotoClick: (String) -> Unit,
    onPhotoClick: (String) -> Unit,
    title: String,
    cameraButtonLabel: String,
    galleryButtonLabel: String,
    resolveFile: (String) -> File,
    modifier: Modifier = Modifier,
    maxPhotos: Int = 5,
    removeContentDescription: String = "Remove photo",
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FarmShapeTokens.card,
        color = FarmColors.Surface,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        Column(modifier = Modifier.padding(FarmSpacing.dp16)) {
            Text(
                text = title,
                style = FarmTypography.titleMedium,
                color = FarmColors.MainText,
            )
            Spacer(Modifier.height(FarmSpacing.dp12))

            if (photoPaths.size < maxPhotos) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    FarmSecondaryButton(
                        text = cameraButtonLabel,
                        onClick = onTakePhotoClick,
                        leadingIcon = Icons.Rounded.PhotoCamera,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    FarmSecondaryButton(
                        text = galleryButtonLabel,
                        onClick = onPickFromGalleryClick,
                        leadingIcon = Icons.Rounded.PhotoLibrary,
                        useMutedOutline = false,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            if (photoPaths.isNotEmpty()) {
                Spacer(Modifier.height(FarmSpacing.dp12))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.dp12),
                ) {
                    photoPaths.forEach { path ->
                        Box(modifier = Modifier.size(96.dp)) {
                            AsyncImage(
                                model = resolveFile(path),
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(FarmShapeTokens.tag)
                                    .border(1.dp, FarmColors.Outline, FarmShapeTokens.tag)
                                    .clickable { onPhotoClick(path) },
                            )
                            FarmIconButton(
                                icon = Icons.Rounded.Close,
                                contentDescription = removeContentDescription,
                                onClick = { onRemovePhotoClick(path) },
                                tint = FarmColors.OnPrimary,
                                containerColor = FarmColors.MainText.copy(alpha = 0.8f),
                                modifier = Modifier.align(Alignment.TopEnd),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FarmPhotoViewer(
    photoFile: File,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    timestampLabel: String? = null,
    onDeleteClick: (() -> Unit)? = null,
    closeContentDescription: String = "Close",
    deleteContentDescription: String = "Delete",
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
            modifier
                .fillMaxSize()
                .background(FarmColors.MainText),
        ) {
            AsyncImage(
                model = photoFile,
                contentDescription = caption,
                contentScale = ContentScale.Fit,
                modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            offset = if (newScale > 1f) offset + pan else Offset.Zero
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
            )

            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(FarmColors.MainText.copy(alpha = 0.85f))
                    .padding(FarmSpacing.dp12),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FarmIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = closeContentDescription,
                    onClick = onDismiss,
                    tint = FarmColors.OnPrimary,
                )
                if (onDeleteClick != null) {
                    FarmIconButton(
                        icon = Icons.Rounded.Delete,
                        contentDescription = deleteContentDescription,
                        onClick = onDeleteClick,
                        tint = FarmColors.OnPrimary,
                    )
                }
            }

            if (!caption.isNullOrBlank() || !timestampLabel.isNullOrBlank()) {
                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(FarmColors.MainText.copy(alpha = 0.85f))
                        .padding(FarmSpacing.dp16),
                ) {
                    if (!caption.isNullOrBlank()) {
                        Text(caption, style = FarmTypography.bodyLarge, color = FarmColors.OnPrimary)
                    }
                    if (!timestampLabel.isNullOrBlank()) {
                        Spacer(Modifier.height(FarmSpacing.dp4))
                        Text(timestampLabel, style = FarmTypography.bodySmall, color = Color(0xFFD5DED4))
                    }
                }
            }
        }
    }
}

@Composable
fun FarmVoiceRecorder(
    state: VoiceRecordingState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDeleteRecording: () -> Unit,
    idleLabel: String,
    recordingLabel: String,
    modifier: Modifier = Modifier,
    deleteContentDescription: String = "Delete voice note",
    audioPlayerContent: (@Composable (filePath: String, durationMillis: Long) -> Unit)? = null,
) {
    Surface(
        modifier =
        modifier
            .fillMaxWidth()
            .semantics {
                stateDescription =
                    when (state) {
                        is VoiceRecordingState.Idle -> "Ready"
                        is VoiceRecordingState.Recording -> "Recording"
                        is VoiceRecordingState.Recorded -> "Recorded"
                    }
            },
        shape = FarmShapeTokens.actionTile,
        color = FarmColors.SurfaceVariant,
        border = BorderStroke(1.dp, FarmColors.Outline),
    ) {
        when (state) {
            is VoiceRecordingState.Idle -> {
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 72.dp)
                        .clickable { onStartRecording() }
                        .padding(FarmSpacing.dp16),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                        Modifier
                            .size(56.dp)
                            .clip(FarmShapeTokens.circular)
                            .background(FarmColors.PrimaryForest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = idleLabel,
                            tint = FarmColors.OnPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(FarmSpacing.dp16))
                    Text(
                        text = idleLabel,
                        style = FarmTypography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = FarmColors.MainText,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            is VoiceRecordingState.Recording -> {
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 72.dp)
                        .clickable { onStopRecording() }
                        .padding(FarmSpacing.dp16),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                        Modifier
                            .size(56.dp)
                            .clip(FarmShapeTokens.circular)
                            .background(FarmColors.Error),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = recordingLabel,
                            tint = FarmColors.OnPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(FarmSpacing.dp16))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recordingLabel,
                            style = FarmTypography.titleMedium,
                            color = FarmColors.Error,
                        )
                        Spacer(Modifier.height(FarmSpacing.dp4))
                        Text(
                            text = formatDurationMmSs(state.elapsedMillis),
                            style = FarmTypography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            color = FarmColors.MainText,
                        )
                    }
                }
            }

            is VoiceRecordingState.Recorded -> {
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(FarmSpacing.dp12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        audioPlayerContent?.invoke(state.filePath, state.durationMillis)
                            ?: Text(
                                text = formatDurationMmSs(state.durationMillis),
                                style = FarmTypography.bodyLarge,
                                color = FarmColors.MainText,
                            )
                    }
                    Spacer(Modifier.width(FarmSpacing.dp8))
                    FarmIconButton(
                        icon = Icons.Rounded.Delete,
                        contentDescription = deleteContentDescription,
                        onClick = onDeleteRecording,
                        tint = FarmColors.Error,
                        containerColor = FarmColors.ErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
fun FarmAudioPlayer(
    isPlaying: Boolean,
    currentPositionMillis: Long,
    totalDurationMillis: Long,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    playContentDescription: String,
    pauseContentDescription: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val safeTotal = totalDurationMillis.coerceAtLeast(1L)
    val progress = (currentPositionMillis.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier =
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp),
        shape = FarmShapeTokens.inputField,
        color = FarmColors.PrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FarmSpacing.dp12, vertical = FarmSpacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FarmIconButton(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) pauseContentDescription else playContentDescription,
                onClick = onPlayPauseClick,
                tint = FarmColors.OnPrimary,
                containerColor = FarmColors.PrimaryForest,
            )
            Spacer(Modifier.width(FarmSpacing.dp12))
            Column(modifier = Modifier.weight(1f)) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = FarmTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = FarmColors.OnPrimaryContainer,
                    )
                }
                Slider(
                    value = progress,
                    onValueChange = { frac -> onSeekTo((frac * safeTotal).toLong()) },
                    colors =
                    SliderDefaults.colors(
                        thumbColor = FarmColors.PrimaryForest,
                        activeTrackColor = FarmColors.PrimaryForest,
                        inactiveTrackColor = FarmColors.Outline,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDurationMmSs(currentPositionMillis),
                        style = FarmTypography.labelMedium,
                        color = FarmColors.OnPrimaryContainer,
                    )
                    Text(
                        text = formatDurationMmSs(totalDurationMillis),
                        style = FarmTypography.labelMedium,
                        color = FarmColors.OnPrimaryContainer,
                    )
                }
            }
        }
    }
}

fun formatDurationMmSs(millis: Long): String {
    val totalSecs = (millis / 1000L).coerceAtLeast(0L)
    val mins = totalSecs / 60L
    val secs = totalSecs % 60L
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}
