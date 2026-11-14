package com.farmsaathi.core.designsystem.localization

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.farmsaathi.core.model.AppLanguage
import java.util.Locale

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.TELUGU }
val LocalLocalizedContext =
    staticCompositionLocalOf<Context> {
        error("LocalLocalizedContext not provided")
    }
private val LocalOptionalLocalizedContext = staticCompositionLocalOf<Context?> { null }

@Composable
fun ProvideFarmLocale(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val localizedContext =
        remember(baseContext, language) {
            val locale =
                when (language) {
                    AppLanguage.TELUGU -> Locale("te", "IN")
                    AppLanguage.ENGLISH -> Locale.ENGLISH
                }
            val config =
                Configuration(baseContext.resources.configuration).apply {
                    setLocale(locale)
                }
            baseContext.createConfigurationContext(config)
        }
    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalLocalizedContext provides localizedContext,
        LocalOptionalLocalizedContext provides localizedContext,
    ) {
        content()
    }
}

@Composable
@ReadOnlyComposable
fun farmString(
    @StringRes resId: Int,
): String = LocalLocalizedContext.current.getString(resId)

@Composable
@ReadOnlyComposable
fun farmString(
    @StringRes resId: Int,
    vararg formatArgs: Any,
): String = LocalLocalizedContext.current.getString(resId, *formatArgs)

@Composable
@ReadOnlyComposable
internal fun safeFarmString(
    @StringRes resId: Int,
): String = (LocalOptionalLocalizedContext.current ?: LocalContext.current).getString(resId)
