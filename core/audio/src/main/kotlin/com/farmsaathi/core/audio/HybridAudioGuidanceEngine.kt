package com.farmsaathi.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AudioCueCatalog
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.common.StoragePaths
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hybrid offline Telugu audio guidance engine:
 * 1) Plays custom family-recorded Telugu prompt file if present under filesDir/audio_prompts/<key>.m4a
 * 2) Otherwise speaks conversational Telugu (or English when toggled) via Android offline TextToSpeech
 * 3) Plays gentle non-blocking acoustic cues (cue_saved.ogg, etc.) from res/raw
 */
@Singleton
class HybridAudioGuidanceEngine
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : AudioGuidanceEngine {
    private val _activeCueKey = MutableStateFlow<String?>(null)
    override val activeCueKey: StateFlow<String?> = _activeCueKey.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isTeluguReady = MutableStateFlow(false)
    override val isTeluguReady: StateFlow<Boolean> = _isTeluguReady.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false

    @Volatile
    private var ttsInitAttempted: Boolean = false
    private var ttsInitFailed: Boolean = false
    private var currentSpeechRate: Float = DEFAULT_SPEECH_RATE
    private var pendingSpeech: PendingSpeech? = null
    private var promptPlayer: MediaPlayer? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val soundPoolLoadStarted = AtomicBoolean(false)
    private val cueSoundIds = ConcurrentHashMap<AcousticCue, Int>()
    private val loadedSoundIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var soundPool: SoundPool? = null

    @Volatile
    private var pendingCue: AcousticCue? = null

    @Synchronized
    private fun ensureTtsInitialized() {
        if (tts != null || ttsInitAttempted) return
        ttsInitAttempted = true
        runCatching {
            tts =
                TextToSpeech(context) { status ->
                    ttsReady = status == TextToSpeech.SUCCESS
                    ttsInitFailed = !ttsReady
                    val engine = tts
                    if (ttsReady && engine != null) {
                        configureUtteranceListener(engine)
                        _isTeluguReady.value = checkTeluguAvailable(engine)
                        pendingSpeech?.let { pending ->
                            pendingSpeech = null
                            speakWithTts(pending.text, pending.useTelugu, _activeCueKey.value, pending.speechRate)
                        }
                    } else {
                        _isTeluguReady.value = false
                        pendingSpeech = null
                    }
                }
        }.onFailure {
            tts = null
            ttsReady = false
            ttsInitFailed = true
            _isTeluguReady.value = false
        }
    }

    private fun configureUtteranceListener(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _activeCueKey.value = null
                }

                @Deprecated("Deprecated in API 21")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _activeCueKey.value = null
                }

                override fun onError(
                    utteranceId: String?,
                    errorCode: Int,
                ) {
                    _isSpeaking.value = false
                    _activeCueKey.value = null
                }
            },
        )
    }

    private fun checkTeluguAvailable(engine: TextToSpeech): Boolean =
        runCatching {
            engine.isLanguageAvailable(Locale("te", "IN")) >= TextToSpeech.LANG_AVAILABLE
        }.getOrDefault(false)

    override fun speakCue(
        cueKey: String,
        useTelugu: Boolean,
    ) {
        if (_activeCueKey.value == cueKey && _isSpeaking.value) {
            stop()
            return
        }
        stop()

        val customFile = File(customPromptFilePath(cueKey))
        if (useTelugu && customFile.exists() && customFile.length() > 0L) {
            playCustomFile(customFile, cueKey)
            return
        }

        val script = AudioCueCatalog.find(cueKey) ?: return
        val text = if (useTelugu) script.scriptTe else script.scriptEn
        _activeCueKey.value = cueKey
        speakWithTts(text, useTelugu, cueKey)
    }

    override fun speakDynamicText(
        text: String,
        useTelugu: Boolean,
    ) {
        speakDynamicText(text, useTelugu, currentSpeechRate)
    }

    override fun speakDynamicText(
        text: String,
        useTelugu: Boolean,
        speechRate: Float,
    ) {
        if (text.isBlank()) return
        stop()
        ensureTtsInitialized()
        val engine = tts
        val canSpeak =
            engine != null &&
                ttsReady &&
                (!useTelugu || checkTeluguAvailable(engine))
        if (!canSpeak) {
            playAcousticFeedback(AcousticCue.REMINDER)
            return
        }
        _activeCueKey.value = "dynamic"
        speakWithTts(text, useTelugu, "dynamic", speechRate)
    }

    override fun setSpeechRate(speechRate: Float) {
        currentSpeechRate = speechRate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
    }

    private fun speakWithTts(
        text: String,
        useTelugu: Boolean,
        cueKey: String?,
        speechRate: Float = currentSpeechRate,
    ) {
        ensureTtsInitialized()
        val engine = tts
        if (engine == null || !ttsReady) {
            if (!ttsInitFailed) {
                pendingSpeech = PendingSpeech(text, useTelugu, speechRate)
                _activeCueKey.value = cueKey
            } else {
                playAcousticFeedback(AcousticCue.REMINDER)
            }
            return
        }
        val clampedRate = speechRate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        val locale = if (useTelugu) Locale("te", "IN") else Locale("en", "IN")
        runCatching {
            val availability = engine.isLanguageAvailable(locale)
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = locale
            } else {
                engine.language = Locale.US
            }
            engine.setSpeechRate(clampedRate)
            _isSpeaking.value = true
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        }.onFailure {
            _isSpeaking.value = false
            _activeCueKey.value = null
            playAcousticFeedback(AcousticCue.REMINDER)
        }
    }

    private fun playCustomFile(
        file: File,
        cueKey: String,
    ) {
        try {
            val mp =
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .build(),
                    )
                    setDataSource(file.absolutePath)
                    setOnCompletionListener {
                        _isSpeaking.value = false
                        _activeCueKey.value = null
                        releasePromptPlayer()
                    }
                    setOnErrorListener { _, _, _ ->
                        _isSpeaking.value = false
                        _activeCueKey.value = null
                        releasePromptPlayer()
                        true
                    }
                    prepare()
                    start()
                }
            promptPlayer = mp
            _activeCueKey.value = cueKey
            _isSpeaking.value = true
        } catch (_: IOException) {
            resetPromptPlaybackState()
        } catch (_: IllegalStateException) {
            resetPromptPlaybackState()
        } catch (_: IllegalArgumentException) {
            resetPromptPlaybackState()
        } catch (_: SecurityException) {
            resetPromptPlaybackState()
        }
    }

    private fun resetPromptPlaybackState() {
        _isSpeaking.value = false
        _activeCueKey.value = null
        releasePromptPlayer()
    }

    override fun playAcousticFeedback(cue: AcousticCue) {
        ensureAcousticPoolPreloaded()
        val pool = soundPool
        val soundId = cueSoundIds[cue]
        if (pool != null && soundId != null && loadedSoundIds.contains(soundId)) {
            pendingCue = null
            playLoadedSound(pool, soundId)
        } else {
            pendingCue = cue
        }
    }

    private fun ensureAcousticPoolPreloaded() {
        if (!soundPoolLoadStarted.compareAndSet(false, true)) return
        ioScope.launch {
            runCatching {
                val attributes =
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                val pool =
                    SoundPool.Builder()
                        .setMaxStreams(MAX_ACOUSTIC_STREAMS)
                        .setAudioAttributes(attributes)
                        .build()
                pool.setOnLoadCompleteListener { sp, sampleId, status ->
                    if (status == 0 && sampleId > 0) {
                        loadedSoundIds.add(sampleId)
                        val queuedCue = pendingCue
                        if (queuedCue != null && cueSoundIds[queuedCue] == sampleId) {
                            pendingCue = null
                            playLoadedSound(sp, sampleId)
                        }
                    }
                }
                soundPool = pool
                for (cue in AcousticCue.entries) {
                    val soundId = pool.load(context, rawResIdForCue(cue), SOUND_LOAD_PRIORITY)
                    if (soundId > 0) {
                        cueSoundIds[cue] = soundId
                    }
                }
                val queuedCue = pendingCue
                if (queuedCue != null) {
                    val queuedId = cueSoundIds[queuedCue]
                    if (queuedId != null && loadedSoundIds.contains(queuedId)) {
                        pendingCue = null
                        playLoadedSound(pool, queuedId)
                    }
                }
            }.onFailure {
                soundPool = null
                cueSoundIds.clear()
                loadedSoundIds.clear()
            }
        }
    }

    private fun playLoadedSound(
        pool: SoundPool,
        soundId: Int,
    ) {
        runCatching {
            pool.play(soundId, 1f, 1f, SOUND_PLAY_PRIORITY, 0, 1f)
        }
    }

    private fun rawResIdForCue(cue: AcousticCue): Int =
        when (cue) {
            AcousticCue.SAVED -> R.raw.cue_saved
            AcousticCue.ERROR -> R.raw.cue_error
            AcousticCue.TIMER_START -> R.raw.cue_timer_start
            AcousticCue.TIMER_STOP -> R.raw.cue_timer_stop
            AcousticCue.REMINDER -> R.raw.cue_reminder
        }

    override fun stop() {
        pendingSpeech = null
        try {
            tts?.stop()
        } catch (_: IllegalStateException) {
            // TTS engine already torn down or unbound.
        }
        releasePromptPlayer()
        _isSpeaking.value = false
        _activeCueKey.value = null
    }

    override fun isTeluguVoiceReady(): Boolean {
        ensureTtsInitialized()
        val engine = tts ?: return false
        if (!ttsReady) return false
        val available = checkTeluguAvailable(engine)
        _isTeluguReady.value = available
        return available
    }

    override fun refreshTtsReadiness(): Boolean = isTeluguVoiceReady()

    internal fun hasAttemptedTtsInit(): Boolean = ttsInitAttempted

    override fun hasCustomPrompt(cueKey: String): Boolean {
        val file = File(customPromptFilePath(cueKey))
        return file.exists() && file.length() > 0L
    }

    override fun customPromptFilePath(cueKey: String): String {
        val dir = File(context.filesDir, StoragePaths.CUSTOM_PROMPTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$cueKey.m4a").absolutePath
    }

    override fun clearCustomPrompt(cueKey: String) {
        val file = File(customPromptFilePath(cueKey))
        if (file.exists()) file.delete()
    }

    private fun releasePromptPlayer() {
        try {
            promptPlayer?.stop()
        } catch (_: IllegalStateException) {
            // MediaPlayer not in started/paused state.
        }
        try {
            promptPlayer?.release()
        } catch (_: IllegalStateException) {
            // MediaPlayer already released.
        }
        promptPlayer = null
    }

    private data class PendingSpeech(
        val text: String,
        val useTelugu: Boolean,
        val speechRate: Float,
    )

    companion object {
        const val DEFAULT_SPEECH_RATE = 0.95f
        private const val MIN_SPEECH_RATE = 0.5f
        private const val MAX_SPEECH_RATE = 1.5f
        private const val MAX_ACOUSTIC_STREAMS = 4
        private const val SOUND_LOAD_PRIORITY = 1
        private const val SOUND_PLAY_PRIORITY = 1
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    @Binds
    @Singleton
    abstract fun bindAudioGuidanceEngine(impl: HybridAudioGuidanceEngine): AudioGuidanceEngine
}
