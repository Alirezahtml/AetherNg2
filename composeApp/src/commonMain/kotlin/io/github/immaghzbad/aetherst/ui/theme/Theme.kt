package io.github.immaghzbad.aetherst.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import io.github.immaghzbad.aetherst.shared.model.AppLanguage
import io.github.immaghzbad.aetherst.shared.model.AppThemeMode
import io.github.immaghzbad.aetherst.shared.ui.localization.EnglishStrings
import io.github.immaghzbad.aetherst.shared.ui.localization.LocalAppStrings
import io.github.immaghzbad.aetherst.shared.ui.localization.PersianStrings
import java.util.Locale

private val DarkColorScheme = darkColorScheme(
    primary = ElegantPrimary,
    onPrimary = ElegantOnPrimary,
    primaryContainer = ElegantPrimaryContainer,
    onPrimaryContainer = ElegantOnPrimaryContainer,
    secondary = ElegantSecondary,
    background = ElegantBackground,
    onBackground = ElegantTextPrimary,
    surface = ElegantSurface,
    onSurface = ElegantTextPrimary,
    surfaceVariant = ElegantSurfaceCard,
    onSurfaceVariant = ElegantTextSecondary,
    outline = ElegantOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val customColors = if (useDark) DarkCustomColors else LightCustomColors

    val isPersian = when (appLanguage) {
        AppLanguage.SYSTEM -> runCatching {
            val lang = Locale.getDefault().language
            lang.equals("fa", ignoreCase = true) || lang.equals("per", ignoreCase = true)
        }.getOrDefault(false)
        AppLanguage.PERSIAN -> true
        AppLanguage.ENGLISH -> false
    }

    val strings = if (isPersian) PersianStrings else EnglishStrings
    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalCustomColors provides customColors,
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
