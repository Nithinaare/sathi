package com.farmsaathi.core.common

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Replaceable backup provider contract. Stage 1 binds LocalZipBackupProvider.
 * Stage 2 Google Drive integration can plug in another implementation without altering domain logic.
 */
interface BackupProvider {
    suspend fun exportBackup(): BackupResult

    suspend fun inspectBackup(sourceUri: Uri): BackupInspection

    suspend fun restoreBackup(sourceUri: Uri): RestoreResult
}

data class BackupResult(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val manifestSha256: String,
    val createdAtEpochMillis: Long,
)

data class BackupInspection(
    val isValid: Boolean,
    val formatVersion: Int,
    val dbSchemaVersion: Int,
    val createdAtEpochMillis: Long,
    val mediaCount: Int,
    val errorReason: String? = null,
)

sealed interface RestoreResult {
    data class Success(val restoredMediaCount: Int) : RestoreResult

    data class AbortedBeforeMutation(val reason: String) : RestoreResult

    data class FailedAndRolledBack(val reason: String) : RestoreResult
}

enum class AcousticCue {
    SAVED,
    ERROR,
    TIMER_START,
    TIMER_STOP,
    REMINDER,
}

/**
 * Stage 1 hybrid Telugu spoken audio guidance + acoustic confirmation cues.
 * Never depends on speech-to-text (ASR) or cloud TTS.
 */
interface AudioGuidanceEngine {
    val activeCueKey: StateFlow<String?>
    val isSpeaking: StateFlow<Boolean>
    val isTeluguReady: StateFlow<Boolean>
        get() = MutableStateFlow(isTeluguVoiceReady())

    fun speakCue(
        cueKey: String,
        useTelugu: Boolean,
    )

    fun speakDynamicText(
        text: String,
        useTelugu: Boolean,
    )

    fun speakDynamicText(
        text: String,
        useTelugu: Boolean,
        speechRate: Float,
    ) {
        setSpeechRate(speechRate)
        speakDynamicText(text, useTelugu)
    }

    fun setSpeechRate(speechRate: Float) {}

    fun playAcousticFeedback(cue: AcousticCue)

    fun stop()

    fun isTeluguVoiceReady(): Boolean

    fun refreshTtsReadiness(): Boolean = isTeluguVoiceReady()

    fun hasCustomPrompt(cueKey: String): Boolean

    fun customPromptFilePath(cueKey: String): String

    fun clearCustomPrompt(cueKey: String)
}
