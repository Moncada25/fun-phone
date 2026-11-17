package com.bookverse.contacts.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.bookverse.contacts.settings.FONT_SCALE_MAX_PERCENT
import com.bookverse.contacts.settings.FONT_SCALE_MIN_PERCENT
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.settings.AppearanceSettings
import com.bookverse.contacts.settings.FontCatalog
import com.bookverse.contacts.settings.FontCategory
import com.bookverse.contacts.settings.Fonts
import com.bookverse.contacts.settings.ListDensity
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.DarkModeStyle
import com.bookverse.contacts.ui.viewmodel.ThemeMode

internal enum class AppearanceSection(val title: Int) {
    STYLE(R.string.accent_usage_title), COLOR(R.string.accent_color), FONT(R.string.font_type),
    THEME(R.string.theme), SIZE(R.string.font_scale), DENSITY(R.string.list_density),
}

@Composable
internal fun AppearanceEditorSheet(
    initial: AppearanceSettings,
    initialSection: AppearanceSection,
    colors: Map<String, Color>,
    saving: Boolean,
    error: Boolean,
    onApply: (AppearanceSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    var section by remember { mutableStateOf(initialSection) }
    SettingsSheetScaffold(
        title = stringResource(R.string.appearance),
        subtitle = stringResource(R.string.appearance_preview_hint),
        onDismiss = onDismiss,
        confirmDismiss = { !saving },
    ) {
        LazyRow(
            state = rememberLazyListState(initialFirstVisibleItemIndex = initialSection.ordinal),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(AppearanceSection.entries, key = { it.name }) { option ->
                FilterChip(
                    selected = section == option, onClick = { section = option },
                    label = { Text(stringResource(option.title)) }, enabled = !saving,
                )
            }
        }
        LazyColumn(
            Modifier.weight(1f, fill = false).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "preview") { AppearancePreview(draft, initial.fontScale) }
            when (section) {
                AppearanceSection.STYLE -> items(AccentBarStyle.entries, key = { it.name }) { style ->
                    SettingsChoiceRow(
                        label = stringResource(accentStyleLabelRes(style)), selected = draft.style == style,
                        onClick = { draft = draft.copy(style = style) }, enabled = !saving,
                        supportingText = if (style == AccentBarStyle.MODERN) stringResource(R.string.accent_usage_modern_description) else null,
                    )
                }
                AppearanceSection.COLOR -> items(colors.entries.toList(), key = { it.key }) { color ->
                    SettingsChoiceRow(
                        label = settingsColorName(color.key), selected = draft.accent == color.value,
                        onClick = { draft = draft.copy(accent = color.value, accentName = color.key) }, enabled = !saving,
                        leadingContent = { Box(Modifier.size(32.dp).clip(CircleShape).background(color.value)) },
                    )
                }
                AppearanceSection.FONT -> {
                    FontCategory.entries.forEach { category ->
                        item(key = category.name) {
                            Text(
                                stringResource(fontCategoryLabel(category)),
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        items(FontCatalog.orderedKeys().filter { FontCatalog.category(it) == category }, key = { it }) { font ->
                            SettingsChoiceRow(
                                label = settingsFontDisplayName(font), selected = draft.font == font,
                                onClick = { draft = draft.copy(font = font) }, enabled = !saving,
                                fontFamily = Fonts.getFontFamily(font),
                                supportingText = stringResource(R.string.font_preview_text),
                            )
                        }
                    }
                }
                AppearanceSection.THEME -> {
                    items(ThemeMode.entries, key = { it.name }) { mode ->
                        SettingsChoiceRow(
                            label = stringResource(themeModeLabelRes(mode)), selected = draft.theme == mode,
                            onClick = { draft = draft.copy(theme = mode) }, enabled = !saving,
                        )
                    }
                    if (draft.theme != ThemeMode.LIGHT) {
                        items(DarkModeStyle.entries, key = { it.name }) { style ->
                            SettingsChoiceRow(
                                label = stringResource(darkModeStyleLabelRes(style)), selected = draft.darkStyle == style,
                                onClick = { draft = draft.copy(darkStyle = style) }, enabled = !saving,
                            )
                        }
                    }
                }
                AppearanceSection.SIZE -> item(key = "font_scale") {
                    val percent = fontScalePercent(draft.fontScale).coerceIn(FONT_SCALE_MIN_PERCENT, FONT_SCALE_MAX_PERCENT)
                    AppearanceStepSlider(
                        label = stringResource(R.string.font_scale),
                        valueLabel = stringResource(R.string.font_scale_value, percent),
                        value = percent, range = FONT_SCALE_MIN_PERCENT..FONT_SCALE_MAX_PERCENT, defaultValue = 100,
                        decreaseLabel = stringResource(R.string.font_scale_decrease),
                        increaseLabel = stringResource(R.string.font_scale_increase),
                        enabled = !saving, onValueChange = { draft = draft.copy(fontScale = it / 100f) },
                    )
                }
                AppearanceSection.DENSITY -> item(key = "list_density") {
                    AppearanceStepSlider(
                        label = stringResource(R.string.list_density),
                        valueLabel = listDensityLabel(draft.density),
                        value = draft.density.spacing,
                        range = ListDensity.MIN_SPACING..ListDensity.MAX_SPACING,
                        defaultValue = ListDensity.NORMAL.spacing,
                        decreaseLabel = stringResource(R.string.list_density_decrease),
                        increaseLabel = stringResource(R.string.list_density_increase),
                        enabled = !saving, onValueChange = { draft = draft.copy(density = ListDensity.of(it)) },
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ListDensity.presets.forEach { preset ->
                                FilterChip(
                                    selected = draft.density == preset, onClick = { draft = draft.copy(density = preset) },
                                    label = { Text(listDensityLabel(preset), maxLines = 1) }, enabled = !saving,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (error) Text(stringResource(R.string.appearance_save_error), color = MaterialTheme.colorScheme.error)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(R.string.cancel)) }
            Button(onClick = { onApply(draft) }, enabled = !saving) { Text(stringResource(R.string.apply)) }
        }
    }
}

@Composable
private fun AppearanceStepSlider(
    label: String,
    valueLabel: String,
    value: Int,
    range: IntRange,
    defaultValue: Int,
    decreaseLabel: String,
    increaseLabel: String,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    presets: (@Composable () -> Unit)? = null,
) {
    val setValue: (Int) -> Unit = { onValueChange(it.coerceIn(range.first, range.last)) }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    valueLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { setValue(defaultValue) }, enabled = enabled && value != defaultValue) {
                    Text(stringResource(R.string.reset_label))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { setValue(value - 1) }, enabled = enabled && value > range.first) {
                    Icon(Icons.Default.Remove, decreaseLabel)
                }
                Slider(
                    value = value.toFloat(),
                    onValueChange = { setValue(it.roundToInt()) },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    enabled = enabled,
                    modifier = Modifier.weight(1f).semantics {
                        contentDescription = label
                        stateDescription = valueLabel
                    },
                )
                IconButton(onClick = { setValue(value + 1) }, enabled = enabled && value < range.last) {
                    Icon(Icons.Default.Add, increaseLabel)
                }
            }
            presets?.invoke()
        }
    }
}

private fun fontCategoryLabel(category: FontCategory): Int = when (category) {
    FontCategory.RECOMMENDED -> R.string.font_category_recommended
    FontCategory.SANS -> R.string.font_category_sans
    FontCategory.SERIF -> R.string.font_category_serif
    FontCategory.MONOSPACE -> R.string.font_category_monospace
    FontCategory.EXPRESSIVE -> R.string.font_category_expressive
}
