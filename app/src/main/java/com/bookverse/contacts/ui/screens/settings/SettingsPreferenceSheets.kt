package com.bookverse.contacts.ui.screens.settings

import kotlin.math.roundToInt
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.R
import com.bookverse.contacts.settings.AnimationSpeed
import com.bookverse.contacts.settings.GlassEffects
import com.bookverse.contacts.settings.ListDensity
import com.bookverse.contacts.settings.TransitionType
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppLanguage
import com.bookverse.contacts.ui.theme.DarkModeStyle
import com.bookverse.contacts.ui.theme.ThemeMode

internal enum class SettingsPreferenceSheet {
    ANIMATION_SPEED,
    LANGUAGE,
    FONT,
    ACCENT_STYLE,
    COLOR,
    VIBRATION_DURATION,
    VIBRATION_PATTERN,
    FONT_SCALE,
    LIST_DENSITY,
    GLASS_EFFECTS,
    STARTUP_SCREEN,
    CALL_ANSWER_STYLE,
    THEME,
    TRANSITION,
}

internal data class SettingsPreferenceSheetState(
    val fontType: String,
    val animationSpeed: AnimationSpeed,
    val transitionType: TransitionType,
    val vibrationDuration: Int,
    val vibrationPattern: String,
    val accentSeed: Color,
    val themeMode: ThemeMode,
    val darkModeStyle: DarkModeStyle,
    val accentBarStyle: AccentBarStyle,
    val appLanguage: AppLanguage,
    val startupScreen: String,
    val callAnswerStyle: String,
    val fontScale: Float,
    val listDensity: ListDensity,
    val glassEffects: GlassEffects,
    val animationOptions: List<AnimationSpeed>,
    val languageOptions: List<AppLanguage>,
    val availableColors: Map<String, Color>,
    val vibrationDurations: List<Int>,
    val vibrationPatterns: List<String>,
    val transitionOptions: List<TransitionType>,
    val themeModes: List<ThemeMode>,
    val darkModeStyles: List<DarkModeStyle>,
)

@Composable
internal fun SettingsPreferenceSheets(
    activeSheet: SettingsPreferenceSheet?,
    state: SettingsPreferenceSheetState,
    onDismiss: () -> Unit,
    onAnimationSpeedSelected: (AnimationSpeed) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onFontSelected: (String) -> Unit,
    onAccentStyleSelected: (AccentBarStyle) -> Unit,
    onColorSelected: (String) -> Unit,
    onVibrationDurationSelected: (Int) -> Unit,
    onVibrationPatternSelected: (String) -> Unit,
    onFontScaleSelected: (Float) -> Unit,
    onListDensitySelected: (ListDensity) -> Unit,
    onGlassEffectsSelected: (GlassEffects) -> Unit,
    onStartupScreenSelected: (String) -> Unit,
    onCallAnswerStyleSelected: (String) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDarkModeStyleSelected: (DarkModeStyle) -> Unit,
    onTransitionSelected: (TransitionType) -> Unit,
) {
    when (activeSheet) {
        SettingsPreferenceSheet.ANIMATION_SPEED -> SettingsSelectionSheet(
            title = stringResource(R.string.transition_speed),
            options = state.animationOptions,
            selectedOption = state.animationSpeed,
            onSelected = onAnimationSpeedSelected,
            onDismiss = onDismiss,
            optionKey = AnimationSpeed::name,
            optionLabel = { speed -> stringResource(animationSpeedLabelRes(speed)) },
        )

        SettingsPreferenceSheet.LANGUAGE -> SettingsSelectionSheet(
            title = stringResource(R.string.app_language_title),
            options = state.languageOptions,
            selectedOption = state.appLanguage,
            onSelected = onLanguageSelected,
            onDismiss = onDismiss,
            optionKey = AppLanguage::name,
            optionLabel = { language -> stringResource(language.labelRes) },
        )

        SettingsPreferenceSheet.FONT -> SettingsFontCatalogSheet(
            selectedFontKey = state.fontType,
            onFontSelected = onFontSelected,
            onDismiss = onDismiss,
        )

        SettingsPreferenceSheet.ACCENT_STYLE -> SettingsSelectionSheet(
            title = stringResource(R.string.accent_usage_title),
            options = AccentBarStyle.entries,
            selectedOption = state.accentBarStyle,
            onSelected = onAccentStyleSelected,
            onDismiss = onDismiss,
            optionKey = AccentBarStyle::name,
            optionLabel = { style -> stringResource(accentStyleLabelRes(style)) },
            optionSupportingText = { style ->
                if (style == AccentBarStyle.MODERN) stringResource(R.string.accent_usage_modern_description) else null
            },
        )

        SettingsPreferenceSheet.COLOR -> SettingsColorSheet(
            selectedColor = state.accentSeed,
            availableColors = state.availableColors,
            onColorSelected = onColorSelected,
            onDismiss = onDismiss,
        )

        SettingsPreferenceSheet.VIBRATION_DURATION -> SettingsSelectionSheet(
            title = stringResource(R.string.vibration_duration),
            options = state.vibrationDurations,
            selectedOption = state.vibrationDuration,
            onSelected = onVibrationDurationSelected,
            onDismiss = onDismiss,
            optionKey = { duration -> duration },
            optionLabel = { duration -> stringResource(R.string.vibration_duration_value, duration) },
        )

        SettingsPreferenceSheet.VIBRATION_PATTERN -> SettingsSelectionSheet(
            title = stringResource(R.string.vibration_pattern),
            options = state.vibrationPatterns,
            selectedOption = state.vibrationPattern,
            onSelected = onVibrationPatternSelected,
            onDismiss = onDismiss,
            optionKey = { pattern -> pattern },
            optionLabel = { pattern -> stringResource(vibrationPatternLabelRes(pattern)) },
        )

        SettingsPreferenceSheet.FONT_SCALE -> SettingsFontScaleSheet(
            fontScale = state.fontScale,
            onFontScaleSelected = onFontScaleSelected,
            onDismiss = onDismiss,
        )

        SettingsPreferenceSheet.LIST_DENSITY -> SettingsListDensitySheet(
            density = state.listDensity,
            onDensitySelected = onListDensitySelected,
            onDismiss = onDismiss,
        )

        SettingsPreferenceSheet.GLASS_EFFECTS -> SettingsSelectionSheet(
            title = stringResource(R.string.glass_effects),
            options = GlassEffects.entries,
            selectedOption = state.glassEffects,
            onSelected = onGlassEffectsSelected,
            onDismiss = onDismiss,
            optionKey = GlassEffects::name,
            optionLabel = { effects -> stringResource(glassEffectsLabelRes(effects)) },
        )

        SettingsPreferenceSheet.STARTUP_SCREEN -> SettingsSelectionSheet(
            title = stringResource(R.string.startup_screen_title),
            options = STARTUP_SCREEN_OPTIONS,
            selectedOption = state.startupScreen,
            onSelected = onStartupScreenSelected,
            onDismiss = onDismiss,
            optionKey = { screen -> screen },
            optionLabel = { screen -> stringResource(startupScreenLabelRes(screen)) },
        )

        SettingsPreferenceSheet.CALL_ANSWER_STYLE -> SettingsSelectionSheet(
            title = stringResource(R.string.call_answer_style_title),
            options = CALL_ANSWER_STYLE_OPTIONS,
            selectedOption = state.callAnswerStyle,
            onSelected = onCallAnswerStyleSelected,
            onDismiss = onDismiss,
            optionKey = { style -> style },
            optionLabel = { style -> stringResource(callAnswerStyleLabelRes(style)) },
        )

        SettingsPreferenceSheet.THEME -> SettingsThemeSheet(
            selectedMode = state.themeMode,
            selectedDarkStyle = state.darkModeStyle,
            themeModes = state.themeModes,
            darkModeStyles = state.darkModeStyles,
            onThemeModeSelected = onThemeModeSelected,
            onDarkModeStyleSelected = onDarkModeStyleSelected,
            onDismiss = onDismiss,
        )

        SettingsPreferenceSheet.TRANSITION -> SettingsSelectionSheet(
            title = stringResource(R.string.transition_type),
            options = state.transitionOptions,
            selectedOption = state.transitionType,
            onSelected = onTransitionSelected,
            onDismiss = onDismiss,
            optionKey = TransitionType::name,
            optionLabel = { transition -> stringResource(transitionTypeLabelRes(transition)) },
        )

        null -> Unit
    }
}

private val STARTUP_SCREEN_OPTIONS = listOf("favorites", "contacts", "history", "extras")
private val CALL_ANSWER_STYLE_OPTIONS = listOf("swipe", "buttons")

@StringRes
internal fun animationSpeedLabelRes(speed: AnimationSpeed): Int = when (speed) {
    AnimationSpeed.FAST -> R.string.fast
    AnimationSpeed.NORMAL -> R.string.normal
    AnimationSpeed.SLOW -> R.string.slow
    AnimationSpeed.OFF -> R.string.off
}

@StringRes
internal fun accentStyleLabelRes(style: AccentBarStyle): Int = when (style) {
    AccentBarStyle.MODERN -> R.string.accent_usage_modern
    AccentBarStyle.MINIMAL -> R.string.accent_usage_minimal
    AccentBarStyle.RETRO -> R.string.accent_usage_retro
}

@StringRes
internal fun glassEffectsLabelRes(effects: GlassEffects): Int = when (effects) {
    GlassEffects.AUTO -> R.string.glass_effects_auto
    GlassEffects.ON -> R.string.glass_effects_on
    GlassEffects.OFF -> R.string.glass_effects_off
}

@StringRes
internal fun vibrationPatternLabelRes(pattern: String): Int = when (pattern) {
    "off" -> R.string.off
    "simple" -> R.string.vibration_pattern_simple
    "double" -> R.string.vibration_pattern_double
    "triple" -> R.string.vibration_pattern_triple
    else -> R.string.vibration_pattern_simple
}

@StringRes
internal fun listDensityPresetLabelRes(density: ListDensity): Int? = when (density) {
    ListDensity.COMPACT -> R.string.list_density_compact
    ListDensity.NORMAL -> R.string.list_density_normal
    ListDensity.SPACIOUS -> R.string.list_density_spacious
    else -> null
}

@Composable
internal fun listDensityLabel(density: ListDensity): String =
    listDensityPresetLabelRes(density)?.let { stringResource(it) }
        ?: stringResource(R.string.list_density_custom_value, density.spacing)

@StringRes
internal fun startupScreenLabelRes(screen: String): Int = when (screen) {
    "favorites" -> R.string.tab_favorites
    "contacts" -> R.string.tab_contacts
    "history" -> R.string.tab_history
    "extras" -> R.string.tab_extras
    else -> R.string.tab_contacts
}

@StringRes
internal fun callAnswerStyleLabelRes(style: String): Int = when (style) {
    "buttons" -> R.string.call_answer_style_buttons
    else -> R.string.call_answer_style_swipe
}

@StringRes
internal fun transitionTypeLabelRes(transition: TransitionType): Int = when (transition) {
    TransitionType.SCALE -> R.string.transition_scale
    TransitionType.SLIDE -> R.string.transition_slide
    TransitionType.FADE -> R.string.transition_fade
    TransitionType.FLIP -> R.string.transition_flip
    TransitionType.SHRINK -> R.string.transition_shrink
    TransitionType.EXPAND -> R.string.transition_expand
    TransitionType.ELASTIC -> R.string.transition_elastic
    TransitionType.CIRCLE_REVEAL -> R.string.transition_circle_reveal
    TransitionType.ROTATE -> R.string.transition_rotate
    TransitionType.ZOOM_BLUR -> R.string.transition_zoom_blur
    TransitionType.WAVE -> R.string.transition_wave
}

internal fun fontScalePercent(scale: Float): Int = (scale * 100).roundToInt()
