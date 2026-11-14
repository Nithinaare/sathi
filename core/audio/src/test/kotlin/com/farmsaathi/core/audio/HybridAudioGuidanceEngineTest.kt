package com.farmsaathi.core.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmsaathi.core.common.AcousticCue
import com.farmsaathi.core.common.AudioCueCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HybridAudioGuidanceEngineTest {
    @Test
    fun `tts initialization is deferred until spoken or readiness checked`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = HybridAudioGuidanceEngine(context)

        assertFalse(engine.hasAttemptedTtsInit())

        engine.setSpeechRate(0.85f)
        for (cue in AcousticCue.entries) {
            engine.playAcousticFeedback(cue)
        }
        assertFalse(engine.hasAttemptedTtsInit())

        assertFalse(engine.isTeluguVoiceReady())
        assertTrue(engine.hasAttemptedTtsInit())
    }

    @Test
    fun `tts warm up speakDynamicText fallback and speech rate do not crash under Robolectric`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = HybridAudioGuidanceEngine(context)

        // Under Robolectric without a bound system TTS service, readiness is false and never throws.
        assertFalse(engine.isTeluguVoiceReady())
        assertFalse(engine.refreshTtsReadiness())
        assertFalse(engine.isTeluguReady.value)

        engine.setSpeechRate(0.75f)
        engine.speakDynamicText("నమస్కారం", useTelugu = true)
        engine.speakDynamicText("Saved", useTelugu = false, speechRate = 1.1f)
        engine.speakCue(AudioCueCatalog.EXPENSE_FORM_HELP, useTelugu = true)
        engine.playAcousticFeedback(AcousticCue.SAVED)
        engine.stop()
        assertFalse(engine.isSpeaking.value)
    }
}
