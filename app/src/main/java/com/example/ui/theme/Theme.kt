package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}

data class GymSemanticColors(
    val activeGreen: Color,
    val activeGreenContainer: Color,
    val warningAmber: Color,
    val warningAmberContainer: Color,
    val alertCoral: Color,
    val alertCoralContainer: Color,
    val infoCyan: Color,
    val infoCyanContainer: Color,
    val accentVolt: Color,
    val onAccentVolt: Color,
    val isDark: Boolean
)

val LocalGymSemanticColors = staticCompositionLocalOf {
    GymSemanticColors(
        activeGreen = EmeraldActive,
        activeGreenContainer = EmeraldContainerDark,
        warningAmber = AmberWarning,
        warningAmberContainer = AmberContainerDark,
        alertCoral = CoralAlert,
        alertCoralContainer = CoralContainerDark,
        infoCyan = CyanInfo,
        infoCyanContainer = CyanContainerDark,
        accentVolt = VoltLime,
        onAccentVolt = OnVoltLime,
        isDark = true
    )
}

private val ApexDarkColorScheme = darkColorScheme(
    primary = VoltLime,
    onPrimary = OnVoltLime,
    primaryContainer = VoltLimeContainer,
    onPrimaryContainer = VoltLime,
    secondary = CyanInfo,
    onSecondary = ObsidianBg,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanInfo,
    tertiary = AmberWarning,
    onTertiary = ObsidianBg,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = AmberWarning,
    background = ObsidianBg,
    onBackground = PureIceWhite,
    surface = CarbonSurface,
    onSurface = PureIceWhite,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = SlateTextMuted,
    outline = SlateOutline,
    outlineVariant = Color(0xFF202838),
    error = CoralAlert,
    onError = PureIceWhite,
    errorContainer = CoralContainerDark,
    onErrorContainer = CoralAlert
)

private val ApexLightColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = OnForestPrimary,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = ForestPrimary,
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = Color(0xFF075985),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF92400E),
    background = DaylightBg,
    onBackground = InkDark,
    surface = DaylightSurface,
    onSurface = InkDark,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = DaylightOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = CoralAlert,
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun ApexPulseTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) ApexDarkColorScheme else ApexLightColorScheme
    val semanticColors = if (useDarkTheme) {
        GymSemanticColors(
            activeGreen = EmeraldActive,
            activeGreenContainer = EmeraldContainerDark,
            warningAmber = AmberWarning,
            warningAmberContainer = AmberContainerDark,
            alertCoral = CoralAlert,
            alertCoralContainer = CoralContainerDark,
            infoCyan = CyanInfo,
            infoCyanContainer = CyanContainerDark,
            accentVolt = VoltLime,
            onAccentVolt = OnVoltLime,
            isDark = true
        )
    } else {
        GymSemanticColors(
            activeGreen = Color(0xFF15803D),
            activeGreenContainer = EmeraldContainerLight,
            warningAmber = Color(0xFFB45309),
            warningAmberContainer = AmberContainerLight,
            alertCoral = Color(0xFFDC2626),
            alertCoralContainer = CoralContainerLight,
            infoCyan = Color(0xFF0369A1),
            infoCyanContainer = CyanContainerLight,
            accentVolt = Color(0xFF65A30D),
            onAccentVolt = Color.White,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalGymSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ApexPulseTypography,
            shapes = ApexPulseShapes,
            content = content
        )
    }
}
