package com.farmsaathi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farmsaathi.app.navigation.FarmSaathiAppRoot
import com.farmsaathi.core.common.AudioGuidanceEngine
import com.farmsaathi.core.datastore.UserPreferences
import com.farmsaathi.core.datastore.UserPreferencesRepository
import com.farmsaathi.core.designsystem.R
import com.farmsaathi.core.designsystem.components.FarmLoadingState
import com.farmsaathi.core.designsystem.localization.LocalFarmSpeakHandler
import com.farmsaathi.core.designsystem.localization.ProvideFarmLocale
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmColors
import com.farmsaathi.core.designsystem.theme.FarmSaathiTheme
import com.farmsaathi.core.model.AppLanguage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var prefsRepository: UserPreferencesRepository

    @Inject lateinit var audioGuidanceEngine: AudioGuidanceEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            val prefsFlow = remember(prefsRepository) {
                prefsRepository.userPreferences.map<UserPreferences, UserPreferences?> { it }
            }
            val prefsOrNull by prefsFlow.collectAsStateWithLifecycle(initialValue = null)

            FarmSaathiTheme {
                val prefs = prefsOrNull
                val lang = prefs?.language
                val enabled = prefs?.audioGuidanceEnabled
                val speakHandler: (String) -> Unit =
                    remember(audioGuidanceEngine, lang, enabled) {
                        { text ->
                            if (enabled != false) {
                                audioGuidanceEngine.speakDynamicText(
                                    text = text,
                                    useTelugu = lang != AppLanguage.ENGLISH,
                                )
                            }
                        }
                    }
                ProvideFarmLocale(language = prefs?.language ?: UserPreferences().language) {
                    CompositionLocalProvider(LocalFarmSpeakHandler provides speakHandler) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = FarmColors.Background,
                        ) {
                            Box(
                                modifier =
                                Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding(),
                            ) {
                                if (prefs == null) {
                                    FarmLoadingState(message = farmString(R.string.common_loading))
                                } else {
                                    FarmSaathiAppRoot(onboardingCompleted = prefs.onboardingCompleted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
