package com.bookverse.contacts.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.ui.components.LiquidGlassSurface
import com.bookverse.contacts.ui.components.drawLiquidGlassLens
import com.bookverse.contacts.ui.theme.AppChrome
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun ModernNavigationDock(
    tabs: List<NavigationItem>,
    progress: Float,
    onTabSelected: (NavigationItem) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    // Glass shifts with whatever scrolls beneath it, so the accent leans toward the neutral content color.
    val selectedContent = lerp(scheme.primary, scheme.onSurface, if (dark) 0.40f else 0.45f)
    LiquidGlassSurface(
        Modifier.widthIn(max = AppChrome.dockMaxWidth).fillMaxWidth()
            .padding(horizontal = AppChrome.dockMargin),
    ) {
        val selectedIndex = progress.roundToInt().coerceIn(0, tabs.lastIndex)
        Row(
            Modifier.fillMaxWidth().padding(6.dp).selectableGroup().drawBehind {
                val cell = size.width / tabs.size
                val position = progress.coerceIn(0f, tabs.lastIndex.toFloat())
                val stretch = cell * 0.12f * sin((position - floor(position)) * PI).toFloat()
                val width = cell + stretch
                val left = (cell * position - stretch / 2f).coerceIn(0f, size.width - width)
                drawLiquidGlassLens(
                    topLeft = Offset(left, 0f), size = Size(width, size.height),
                    contentColor = scheme.onSurface, dark = dark,
                )
            },
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val emphasis = (1f - abs(progress - index)).coerceIn(0f, 1f)
                val color = lerp(scheme.onSurfaceVariant, selectedContent, emphasis)
                Column(
                    modifier = Modifier.weight(1f)
                        .clip(CircleShape)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onTabSelected(tab) })
                        .heightIn(min = 64.dp).padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                    Text(
                        stringResource(tab.titleResId),
                        color = color,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
