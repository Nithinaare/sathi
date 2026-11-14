package com.farmsaathi.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import com.farmsaathi.core.common.StoragePaths
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight AAC/M4A voice-note recorder & player tuned for API 29+ (Realme 5s).
 * Uses MediaRecorder() constructor compatible with Android 10 without API 31+ dependencies.
 */
@Singleton
class VoiceNoteManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var recordingStartMillis: Long = 0L

    private var player: MediaPlayer? = null
    private var playingPath: String? = null

    fun startRecording(targetFile: File? = null): Result<File> {
        stopRecording()
        stopPlayback()
        val outputFile =
            targetFile ?: run {
                val dir = File(context.filesDir, StoragePaths.AUDIO_NOTES_DIR)
                if (!dir.exists()) dir.mkdirs()
                File(dir, "voice_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.m4a")
            }
        outputFile.parentFile?.mkdirs()

        return try {
            @Suppress("DEPRECATION")
            val mediaRecorder =
                MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(16_000)
                    setAudioEncodingBitRate(32_000)
                    setAudioChannels(1)
                    setMaxDuration(120_000)
                    setOutputFile(outputFile.absolutePath)
                    prepare()
                    start()
                }
            recorder = mediaRecorder
            currentRecordingFile = outputFile
            recordingStartMillis = System.currentTimeMillis()
            Result.success(outputFile)
        } catch (_: java.io.IOException) {
            releaseRecorder()
            if (outputFile.exists()) outputFile.delete()
            Result.failure(IllegalStateException("Audio recording failed"))
        } catch (_: IllegalStateException) {
            releaseRecorder()
            if (outputFile.exists()) outputFile.delete()
            Result.failure(IllegalStateException("Audio recorder state invalid"))
        } catch (_: SecurityException) {
            releaseRecorder()
            if (outputFile.exists()) outputFile.delete()
            Result.failure(SecurityException("Microphone permission missing"))
        }
    }

    /**
     * Stops active recording and returns (relativeOrAbsolutePath, durationMillis).
     */
    @Suppress("TooGenericExceptionCaught") // MediaRecorder.stop() throws RuntimeException for short clips on Android
    fun stopRecording(): Pair<File, Long>? {
        val rec = recorder ?: return null
        val file = currentRecordingFile
        val elapsed = maxOf(0L, System.currentTimeMillis() - recordingStartMillis)
        recorder = null
        currentRecordingFile = null
        return try {
            rec.stop()
            rec.reset()
            rec.release()
            if (file != null && file.exists() && file.length() > 0L) {
                file to elapsed
            } else {
                null
            }
        } catch (_: RuntimeException) {
            safelyResetAndRelease(rec)
            if (file != null && file.exists()) file.delete()
            null
        } catch (_: IOException) {
            safelyResetAndRelease(rec)
            if (file != null && file.exists()) file.delete()
            null
        }
    }

    private fun safelyResetAndRelease(rec: MediaRecorder) {
        try {
            rec.reset()
        } catch (_: IllegalStateException) {
            // Recorder already uninitialized.
        }
        try {
            rec.release()
        } catch (_: IllegalStateException) {
            // Recorder already released.
        }
    }

    fun currentAmplitudeNormalized(): Float {
        val rec = recorder ?: return 0f
        return try {
            (rec.maxAmplitude / 32767f).coerceIn(0f, 1f)
        } catch (_: IllegalStateException) {
            0f
        }
    }

    fun startPlayback(
        file: File,
        onComplete: () -> Unit,
        onError: () -> Unit,
    ): Boolean {
        if (!file.exists() || file.length() == 0L) {
            onError()
            return false
        }
        stopPlayback()
        return try {
            val mp =
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build(),
                    )
                    setDataSource(file.absolutePath)
                    setOnCompletionListener {
                        stopPlayback()
                        onComplete()
                    }
                    setOnErrorListener { _, _, _ ->
                        stopPlayback()
                        onError()
                        true
                    }
                    prepare()
                    start()
                }
            player = mp
            playingPath = file.absolutePath
            true
        } catch (_: IOException) {
            handlePlaybackFailure(onError)
        } catch (_: IllegalStateException) {
            handlePlaybackFailure(onError)
        } catch (_: IllegalArgumentException) {
            handlePlaybackFailure(onError)
        } catch (_: SecurityException) {
            handlePlaybackFailure(onError)
        }
    }

    private fun handlePlaybackFailure(onError: () -> Unit): Boolean {
        stopPlayback()
        onError()
        return false
    }

    fun seekTo(positionMillis: Long) {
        try {
            player?.seekTo(positionMillis.toInt().coerceAtLeast(0))
        } catch (_: IllegalStateException) {
            // Player not in seekable state.
        }
    }

    fun currentPositionMillis(): Long =
        try {
            player?.currentPosition?.toLong() ?: 0L
        } catch (_: IllegalStateException) {
            0L
        }

    fun durationMillis(): Long =
        try {
            player?.duration?.toLong() ?: 0L
        } catch (_: IllegalStateException) {
            0L
        }

    fun isPlaying(path: String? = null): Boolean {
        val mp = player ?: return false
        val playing =
            try {
                mp.isPlaying
            } catch (_: IllegalStateException) {
                false
            }
        return playing && (path == null || playingPath == path)
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: IllegalStateException) {
            // Player not in started state.
        }
        try {
            player?.release()
        } catch (_: IllegalStateException) {
            // Player already released.
        }
        player = null
        playingPath = null
    }

    fun toRelativeMediaPath(file: File): String {
        val base = context.filesDir.absolutePath.trimEnd(File.separatorChar) + File.separator
        val abs = file.absolutePath
        return if (abs.startsWith(base)) abs.removePrefix(base).replace('\\', '/') else abs
    }

    fun resolveMediaFile(storedPath: String): File {
        val direct = File(storedPath)
        return if (direct.isAbsolute) direct else File(context.filesDir, storedPath)
    }

    private fun releaseRecorder() {
        recorder?.let { safelyResetAndRelease(it) }
        recorder = null
        currentRecordingFile = null
    }
}
