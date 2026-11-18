package com.bookverse.contacts.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.feature.settings.R
import com.bookverse.contacts.settings.Fonts
import com.bookverse.contacts.ui.theme.DarkBackgroundColor
import com.bookverse.contacts.ui.theme.DarkGrayBackgroundColor
import com.bookverse.contacts.ui.theme.DimBackgroundColor
import com.bookverse.contacts.ui.theme.createAccentPalette
import com.bookverse.contacts.ui.theme.DarkModeStyle
import com.bookverse.contacts.ui.theme.ThemeMode

@Composable
internal fun SettingsColorSheet(
    selectedColor: Color,
    availableColors: Map<String, Color>,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedKey = availableColors.entries.firstOrNull { it.value == selectedColor }?.key ?: "blue"
    SettingsSelectionSheet(
        title = stringResource(R.string.accent_color),
        options = availableColors.entries.toList(),
        selectedOption = availableColors.entries.firstOrNull { it.key == selectedKey },
        onSelected = { entry -> onColorSelected(entry.key) },
        onDismiss = onDismiss,
        optionKey = { entry -> entry.key },
        optionLabel = { entry -> settingsColorName(entry.key) },
        optionLeadingContent = { entry ->
            val colorScheme = MaterialTheme.colorScheme
            val preview = remember(entry.value, colorScheme) {
                createAccentPalette(
                    seed = entry.value,
                    background = colorScheme.background,
                    surface = colorScheme.surface,
                    onBackground = colorScheme.onBackground,
                    isDark = colorScheme.background.luminance() < 0.5f,
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(preview.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(preview.seed),
                )
            }
        },
    )
}

@Composable
internal fun SettingsThemeSheet(
    selectedMode: ThemeMode,
    selectedDarkStyle: DarkModeStyle,
    themeModes: List<ThemeMode>,
    darkModeStyles: List<DarkModeStyle>,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDarkModeStyleSelected: (DarkModeStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    SettingsSheetScaffold(
        title = stringResource(R.string.theme),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.theme_mode_label),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            themeModes.forEach { mode ->
                SettingsChoiceRow(
                    label = stringResource(themeModeLabelRes(mode)),
                    selected = mode == selectedMode,
                    onClick = { onThemeModeSelected(mode) },
                    leadingContent = { ThemeModeSwatch(mode) },
                )
            }

            if (selectedMode != ThemeMode.LIGHT) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.dark_mode_style_label),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                darkModeStyles.forEach { style ->
                    SettingsChoiceRow(
                        label = stringResource(darkModeStyleLabelRes(style)),
                        supportingText = stringResource(darkModeStyleDescriptionRes(style)),
                        selected = style == selectedDarkStyle,
                        onClick = { onDarkModeStyleSelected(style) },
                        leadingContent = { DarkStyleSwatch(style) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun settingsFontDisplayName(key: String): String =
    stringResource(FONT_NAME_RES[key] ?: R.string.font_default)

@Composable
internal fun settingsColorName(key: String): String =
    stringResource(COLOR_NAME_RES[key] ?: R.string.accent_color)

@StringRes
internal fun themeModeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.SYSTEM -> R.string.theme_system
}

@StringRes
internal fun darkModeStyleLabelRes(style: DarkModeStyle): Int = when (style) {
    DarkModeStyle.AMOLED -> R.string.dark_style_amoled
    DarkModeStyle.DARK_GRAY -> R.string.dark_style_dark_gray
    DarkModeStyle.DIM -> R.string.dark_style_dim
}

@StringRes
private fun darkModeStyleDescriptionRes(style: DarkModeStyle): Int = when (style) {
    DarkModeStyle.AMOLED -> R.string.dark_style_amoled_desc
    DarkModeStyle.DARK_GRAY -> R.string.dark_style_dark_gray_desc
    DarkModeStyle.DIM -> R.string.dark_style_dim_desc
}

@Composable
private fun ThemeModeSwatch(mode: ThemeMode) {
    val lightColor = Color(0xFFF3F4F6)
    val darkColor = Color(0xFF0F1114)
    Canvas(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape),
    ) {
        when (mode) {
            ThemeMode.LIGHT -> drawCircle(lightColor)
            ThemeMode.DARK -> drawCircle(darkColor)
            ThemeMode.SYSTEM -> {
                drawArc(lightColor, 90f, 180f, true)
                drawArc(darkColor, 270f, 180f, true)
            }
        }
        drawCircle(
            color = Color.Gray,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
private fun DarkStyleSwatch(style: DarkModeStyle) {
    val color = when (style) {
        DarkModeStyle.AMOLED -> DarkBackgroundColor
        DarkModeStyle.DARK_GRAY -> DarkGrayBackgroundColor
        DarkModeStyle.DIM -> DimBackgroundColor
    }
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

private val FONT_NAME_RES = mapOf(
    Fonts.DEFAULT.font to R.string.font_default,
    Fonts.SERIF.font to R.string.font_serif,
    Fonts.MONOSPACE.font to R.string.font_monospace,
    Fonts.CURSIVE.font to R.string.font_cursive,
    Fonts.LATO.font to R.string.font_lato,
    Fonts.PLAYFAIR.font to R.string.font_playfair,
    Fonts.PLAYPEN.font to R.string.font_playpen,
    Fonts.PLAYWRITE.font to R.string.font_playwrite,
    Fonts.YELLOWTAIL.font to R.string.font_yellowtail,
    Fonts.DOTO.font to R.string.doto,
    Fonts.HENNY_PENNY.font to R.string.font_henny_penny,
    Fonts.INTER.font to R.string.font_inter,
    Fonts.ROBOTO.font to R.string.font_roboto,
    Fonts.ROBOTO_CONDENSED.font to R.string.font_roboto_condensed,
    Fonts.ROBOTO_SERIF.font to R.string.font_roboto_serif,
    Fonts.NOTO_SANS.font to R.string.font_noto_sans,
    Fonts.NOTO_SERIF.font to R.string.font_noto_serif,
    Fonts.MANROPE.font to R.string.font_manrope,
    Fonts.RUBIK.font to R.string.font_rubik,
    Fonts.URBANIST.font to R.string.font_urbanist,
    Fonts.NUNITO.font to R.string.font_nunito,
    Fonts.JETBRAINS_MONO.font to R.string.font_jetbrains_mono,
    Fonts.CAVEAT.font to R.string.font_caveat,
    Fonts.LOBSTER.font to R.string.font_lobster,
    Fonts.PACIFICO.font to R.string.font_pacifico,
    Fonts.GREAT_VIBES.font to R.string.font_great_vibes,
    Fonts.PIXELIFY_SANS.font to R.string.font_pixelify_sans,
)

private val COLOR_NAME_RES = mapOf(
    "sage" to R.string.color_sage,
    "petrol" to R.string.color_petrol,
    "terracotta" to R.string.color_terracotta,
    "mauve" to R.string.color_mauve,
    "sand" to R.string.color_sand,
    "graphite" to R.string.color_graphite,
    "blue" to R.string.blue,
    "blue_dark" to R.string.blue_dark,
    "blue_light" to R.string.blue_light,
    "green" to R.string.green,
    "green_dark" to R.string.green_dark,
    "green_light" to R.string.green_light,
    "red" to R.string.red,
    "red_dark" to R.string.red_dark,
    "red_light" to R.string.red_light,
    "orange" to R.string.orange,
    "orange_dark" to R.string.orange_dark,
    "orange_light" to R.string.orange_light,
    "purple" to R.string.purple,
    "purple_dark" to R.string.purple_dark,
    "purple_light" to R.string.purple_light,
    "pink" to R.string.pink,
    "pink_dark" to R.string.pink_dark,
    "pink_light" to R.string.pink_light,
    "yellow" to R.string.yellow,
    "amber" to R.string.amber,
    "teal" to R.string.teal,
    "teal_dark" to R.string.teal_dark,
    "teal_light" to R.string.teal_light,
    "cyan" to R.string.cyan,
    "cyan_light" to R.string.cyan_light,
    "indigo" to R.string.indigo,
    "blue_grey" to R.string.blue_grey,
)
