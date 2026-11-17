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
        activeGreenContainer = EmeraldContainerLight,
        warningAmber = SunlitHoneyGold,
        warningAmberContainer = AmberContainerLight,
        alertCoral = CoralAlert,
        alertCoralContainer = CoralContainerLight,
        infoCyan = CyanAzure,
        infoCyanContainer = CyanContainerLight,
        accentVolt = RoyalSkyBlue,
        onAccentVolt = PureIceWhite,
        isDark = false
    )
}

// Luminous Sky-Luxury Theme Color Scheme (Zero black, zero harsh dark backgrounds)
private val SkyLuxuryColorScheme = lightColorScheme(
    primary = RoyalSkyBlue,
    onPrimary = PureIceWhite,
    primaryContainer = PastelSkyCard,
    onPrimaryContainer = DeepRoyalInk,
    secondary = CyanAzure,
    onSecondary = PureIceWhite,
    secondaryContainer = PastelSkyCard,
    onSecondaryContainer = DeepRoyalInk,
    tertiary = OrchidLavender,
    onTertiary = PureIceWhite,
    tertiaryContainer = PastelSkyCard,
    onTertiaryContainer = DeepRoyalInk,
    background = SkyAzureCanvas,
    onBackground = DeepRoyalInk,
    surface = PastelSkySurface,
    onSurface = DeepRoyalInk,
    surfaceVariant = PastelSkyCard,
    onSurfaceVariant = RichSapphireInk,
    outline = OutlinePastelBlue,
    outlineVariant = OutlineGold,
    error = CoralAlert,
    onError = PureIceWhite,
    errorContainer = CoralContainerLight,
    onErrorContainer = CoralAlert
)

@Composable
fun ApexPulseTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val semanticColors = GymSemanticColors(
        activeGreen = EmeraldActive,
        activeGreenContainer = EmeraldContainerLight,
        warningAmber = SunlitHoneyGold,
        warningAmberContainer = AmberContainerLight,
        alertCoral = CoralAlert,
        alertCoralContainer = CoralContainerLight,
        infoCyan = CyanAzure,
        infoCyanContainer = CyanContainerLight,
        accentVolt = RoyalSkyBlue,
        onAccentVolt = PureIceWhite,
        isDark = false
    )

    CompositionLocalProvider(LocalGymSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = SkyLuxuryColorScheme,
            typography = ApexPulseTypography,
            shapes = ApexPulseShapes,
            content = content
        )
    }
}
