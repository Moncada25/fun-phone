package com.bookverse.contacts.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.feature.settings.R
import com.bookverse.contacts.settings.FONT_SCALE_DEFAULT_PERCENT
import com.bookverse.contacts.settings.FONT_SCALE_MAX_PERCENT
import com.bookverse.contacts.settings.FONT_SCALE_MIN_PERCENT
import com.bookverse.contacts.settings.FontCatalog
import com.bookverse.contacts.settings.FontCategory
import com.bookverse.contacts.settings.Fonts
import com.bookverse.contacts.settings.ListDensity
import kotlin.math.roundToInt
import com.bookverse.contacts.core.ui.R as CoreUiR

@Composable
internal fun SettingsFontCatalogSheet(
    selectedFontKey: String,
    onFontSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val previewText = stringResource(R.string.font_preview_text)
    val grouped = remember { FontCatalog.orderedKeys().groupBy(FontCatalog::category) }
    SettingsSheetScaffold(
        title = stringResource(R.string.font_type),
        onDismiss = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FontCategory.entries.forEach { category ->
                val fonts = grouped[category].orEmpty()
                if (fonts.isNotEmpty()) {
                    item(key = category.name) {
                        Text(
                            stringResource(fontCategoryLabel(category)),
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    items(fonts, key = { it }) { font ->
                        val family = Fonts.getFontFamily(font)
                        SettingsChoiceRow(
                            label = settingsFontDisplayName(font),
                            selected = font == selectedFontKey,
                            onClick = {
                                onFontSelected(font)
                                onDismiss()
                            },
                            fontFamily = family,
                            supportingText = previewText,
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(R.string.font_sample_glyphs),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = family,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsFontScaleSheet(
    fontScale: Float,
    onFontScaleSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val applied = fontScalePercent(fontScale).coerceIn(FONT_SCALE_MIN_PERCENT, FONT_SCALE_MAX_PERCENT)
    SettingsSheetScaffold(title = stringResource(R.string.font_scale), onDismiss = onDismiss) {
        SettingsStepSlider(
            label = stringResource(R.string.font_scale),
            appliedValue = applied,
            valueLabel = { stringResource(R.string.font_scale_value, it) },
            range = FONT_SCALE_MIN_PERCENT..FONT_SCALE_MAX_PERCENT,
            defaultValue = FONT_SCALE_DEFAULT_PERCENT,
            decreaseLabel = stringResource(R.string.font_scale_decrease),
            increaseLabel = stringResource(R.string.font_scale_increase),
            onCommit = { onFontScaleSelected(it / 100f) },
        )
    }
}

@Composable
internal fun SettingsListDensitySheet(
    density: ListDensity,
    onDensitySelected: (ListDensity) -> Unit,
    onDismiss: () -> Unit,
) {
    SettingsSheetScaffold(title = stringResource(R.string.list_density), onDismiss = onDismiss) {
        SettingsStepSlider(
            label = stringResource(R.string.list_density),
            appliedValue = density.spacing,
            valueLabel = { listDensityLabel(ListDensity.of(it)) },
            range = ListDensity.MIN_SPACING..ListDensity.MAX_SPACING,
            defaultValue = ListDensity.NORMAL.spacing,
            decreaseLabel = stringResource(R.string.list_density_decrease),
            increaseLabel = stringResource(R.string.list_density_increase),
            onCommit = { onDensitySelected(ListDensity.of(it)) },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListDensity.presets.forEach { preset ->
                    FilterChip(
                        selected = density == preset,
                        onClick = { onDensitySelected(preset) },
                        label = { Text(listDensityLabel(preset), maxLines = 1) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsStepSlider(
    label: String,
    appliedValue: Int,
    valueLabel: @Composable (Int) -> String,
    range: IntRange,
    defaultValue: Int,
    decreaseLabel: String,
    increaseLabel: String,
    onCommit: (Int) -> Unit,
    presets: (@Composable () -> Unit)? = null,
) {
    var dragValue by remember(appliedValue) { mutableIntStateOf(appliedValue) }
    val commit: (Int) -> Unit = { value ->
        val clamped = value.coerceIn(range.first, range.last)
        dragValue = clamped
        if (clamped != appliedValue) onCommit(clamped)
    }
    val currentLabel = valueLabel(dragValue)
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    currentLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { commit(defaultValue) }, enabled = dragValue != defaultValue) {
                    Text(stringResource(CoreUiR.string.reset_label))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { commit(dragValue - 1) }, enabled = dragValue > range.first) {
                    Icon(Icons.Default.Remove, decreaseLabel)
                }
                Slider(
                    value = dragValue.toFloat(),
                    onValueChange = { dragValue = it.roundToInt().coerceIn(range.first, range.last) },
                    onValueChangeFinished = { commit(dragValue) },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    modifier = Modifier.weight(1f).semantics {
                        contentDescription = label
                        stateDescription = currentLabel
                    },
                )
                IconButton(onClick = { commit(dragValue + 1) }, enabled = dragValue < range.last) {
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
