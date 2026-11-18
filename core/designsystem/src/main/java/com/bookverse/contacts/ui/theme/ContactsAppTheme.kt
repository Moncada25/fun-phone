package com.bookverse.contacts.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.settings.GlassEffects

val LightTextColor = Color(0xFF161618)
val DarkTextColor  = Color(0xE6FFFFFF)

val CallAcceptColor = Color(0xFF4CAF50)
val CallRejectColor = Color(0xFFF44336)
val CallEndColor = Color(0xFFE53935)
val BadgeColor = Color(0xFFF44336)
val BadgeBackgroundColor = Color(0xFFFFEAEA)
val BudgetOkColor = Color(0xFF66BB6A)
val BudgetWarningColor = Color(0xFFFFB300)
val GradientScrimColor = Color(0x99000000)

const val SecondaryTextAlpha = 0.65f

@Immutable
data class StatusColors(
    val success: Color,
    val error: Color,
    val warning: Color,
    val info: Color,
)

private val LightStatusColors = StatusColors(
    success = Color(0xFF2E7D32),
    error = Color(0xFFD32F2F),
    warning = Color(0xFF9A5B00),
    info = Color(0xFF1976D2),
)

private val DarkStatusColors = StatusColors(
    success = Color(0xFF81C784),
    error = Color(0xFFFF8A80),
    warning = Color(0xFFFFCA28),
    info = Color(0xFF64B5F6),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

val LightBackgroundColor = Color(0xFFE3E5E8)
val LightSurfaceColor    = Color(0xFFFFFFFF)

val DarkBackgroundColor = Color(0xFF000000)
val DarkSurfaceColor    = Color(0xFF2C2C2E)

val DarkGrayBackgroundColor = Color(0xFF161618)
val DarkGraySurfaceColor    = Color(0xFF343436)
val DarkGrayTextColor       = Color.White

val DimBackgroundColor = Color(0xFF28282A)
val DimSurfaceColor    = Color(0xFF48484A)
val DimTextColor       = Color.White

private val LightColorScheme = lightColorScheme(
    background = LightBackgroundColor,
    onBackground = LightTextColor,
    surface = LightSurfaceColor,
    onSurface = LightTextColor
)

private val DarkColorScheme = darkColorScheme(
    background = DarkBackgroundColor,
    onBackground = DarkTextColor,
    surface = DarkSurfaceColor,
    onSurface = DarkTextColor
)

private val DarkGrayColorScheme = darkColorScheme(
    background = DarkGrayBackgroundColor,
    onBackground = DarkGrayTextColor,
    surface = DarkGraySurfaceColor,
    onSurface = DarkGrayTextColor
)

private val DimColorScheme = darkColorScheme(
    background = DimBackgroundColor,
    onBackground = DimTextColor,
    surface = DimSurfaceColor,
    onSurface = DimTextColor
)

val LocalFontFamily = compositionLocalOf<FontFamily?> { null }

@Composable
fun ContactsAppTheme(
    dynamicColor: Boolean = false,
    accentSeed: Color = DefaultAccentSeed,
    fontFamily: FontFamily? = null,
    fontScale: Float = 1.0f,
    themeMode: ThemeMode,
    darkModeStyle: DarkModeStyle = DarkModeStyle.AMOLED,
    accentBarStyle: AccentBarStyle = LocalAppChromeStyle.current,
    glassEffects: GlassEffects = LocalGlassEffects.current,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> when (darkModeStyle) {
            DarkModeStyle.AMOLED -> DarkColorScheme
            DarkModeStyle.DARK_GRAY -> DarkGrayColorScheme
            DarkModeStyle.DIM -> DimColorScheme
        }
        else -> LightColorScheme
    }
    val accentPalette = createAccentPalette(
        seed = accentSeed,
        background = baseColorScheme.background,
        surface = baseColorScheme.surface,
        onBackground = baseColorScheme.onBackground,
        isDark = darkTheme,
    )
    val colorScheme = rememberAnimatedColorScheme(
        target = baseColorScheme.withAccentPalette(accentPalette),
        key = listOf(dynamicColor, darkTheme, darkModeStyle, accentSeed),
        durationMs = LocalAnimationSpeed.current.durationMs,
    )

    val defaults = Typography()
    val typography = Typography(
        displayLarge = defaults.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = defaults.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = defaults.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = defaults.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = defaults.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = defaults.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = defaults.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = defaults.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = defaults.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = defaults.labelSmall.copy(fontFamily = fontFamily),
    )

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * fontScale
    )

    CompositionLocalProvider(
        LocalFontFamily provides fontFamily,
        LocalAccentPalette provides accentPalette,
        LocalStatusColors provides if (colorScheme.surface.luminance() < 0.5f) DarkStatusColors else LightStatusColors,
        LocalAppChromeStyle provides accentBarStyle,
        LocalGlassEffects provides glassEffects,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
