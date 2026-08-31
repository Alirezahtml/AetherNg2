package io.github.immaghzbad.aetherst.shared.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// M3 Theme Colors - Dark
val ElegantPrimary = Color(0xFFD0BCFF)
val ElegantOnPrimary = Color(0xFF381E72)
val ElegantPrimaryContainer = Color(0xFF381E72)
val ElegantOnPrimaryContainer = Color(0xFFEADDFF)
val ElegantSecondary = Color(0xFFCCC2DC)
val ElegantBackground = Color(0xFF000000)
val ElegantSurface = Color(0xFF000000)
val ElegantSurfaceCard = Color(0xFF1C1B1F)
val ElegantSurfaceActive = Color(0xFF4A4458)
val ElegantOutline = Color(0xFF49454F)
val ElegantTextPrimary = Color(0xFFE6E1E5)
val ElegantTextSecondary = Color(0xFFCAC4D0)

// M3 Theme Colors - Light
val LightPrimary = Color(0xFF6750A4)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFEADDFF)
val LightOnPrimaryContainer = Color(0xFF21005D)
val LightSecondary = Color(0xFF625B71)
val LightBackground = Color(0xFFF2F2F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceCard = Color(0xFFFFFFFF)
val LightSurfaceActive = Color(0xFFE8DEF8)
val LightOutline = Color(0xFFC6C6C8)
val LightTextPrimary = Color(0xFF000000)
val LightTextSecondary = Color(0xFF6C6C70)

val ConnectedGreen = Color(0xFF34C759)
val ScanningAmber = Color(0xFFFF9500)
val ErrorRed = Color(0xFFFF3B30)

data class CustomAppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val cardBg: Color,
    val groupBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val dividerColor: Color,
    val cardBorder: Color = Color(0x29FFFFFF),
    val activeBlue: Color = Color(0xFF007AFF),
    val activeGreen: Color = Color(0xFF34C759),
    val activeAmber: Color = Color(0xFFFF9500),
    val activePurple: Color = Color(0xFFAF52DE),
    val errorRed: Color = Color(0xFFFF3B30),
    val navBackground: Color,
    val navActive: Color = Color(0xFF007AFF),
    val navInactive: Color = Color(0xFF8E8E93),
    val inactiveTrack: Color,
    val glassCardBg: Color,
    val glassCardBorder: Color,
    val glassGroupBg: Color,
    val glassHighlight: Color
)

val DarkCustomColors = CustomAppColors(
    isDark = true,
    background = Color(0xFF07090E),
    surface = Color(0xFF10141D),
    cardBg = Color(0xFF161A24),
    groupBg = Color(0xFF222838),
    cardBorder = Color(0x29FFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8E95A5),
    dividerColor = Color(0x33343C50),
    navBackground = Color(0xFF141822),
    inactiveTrack = Color(0xFF2A3142),
    glassCardBg = Color(0xD9181C28),
    glassCardBorder = Color(0x29FFFFFF),
    glassGroupBg = Color(0x99252C3D),
    glassHighlight = Color(0x1FFFFFFF)
)

val LightCustomColors = CustomAppColors(
    isDark = false,
    background = Color(0xFFF1F4F9),
    surface = Color(0xFFFFFFFF),
    cardBg = Color(0xFFFFFFFF),
    groupBg = Color(0xFFE5E9F0),
    cardBorder = Color(0x1F000000),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    dividerColor = Color(0x26000000),
    navBackground = Color(0xFFF8FAFC),
    inactiveTrack = Color(0xFFE2E8F0),
    glassCardBg = Color(0xEBFFFFFF),
    glassCardBorder = Color(0x1F000000),
    glassGroupBg = Color(0x80E2E8F0),
    glassHighlight = Color(0x40FFFFFF)
)

val LocalCustomColors = staticCompositionLocalOf { DarkCustomColors }

object AppTheme {
    val colors: CustomAppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomColors.current
}

val IosCardBg: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.cardBg
val IosGroupBg: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.groupBg
val IosSecondaryLabel: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.textSecondary
val IosDividerColor: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.dividerColor
val IosInactiveTrack: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.inactiveTrack
val IosNavBackground: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.navBackground
val IosTextPrimary: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.textPrimary
val IosBackground: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.background
val IosActiveBlue: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.activeBlue
val IosActiveGreen: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.activeGreen
val IosScanningAmber: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.activeAmber
val IosErrorRed: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.errorRed
val IosGlassCardBg: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.glassCardBg
val IosGlassCardBorder: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.glassCardBorder
val IosGlassGroupBg: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.glassGroupBg
val IosCardBorder: Color @Composable @ReadOnlyComposable get() = AppTheme.colors.cardBorder
