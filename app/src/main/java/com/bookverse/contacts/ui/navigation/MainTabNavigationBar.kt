package com.bookverse.contacts.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import com.bookverse.contacts.ui.theme.chromePolicy
import com.bookverse.contacts.ui.theme.LocalAppChromeStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun MainTabNavigationBar(
    tabs: List<NavigationItem>,
    indicatorProgress: Float,
    backgroundColor: Color,
    onTabSelected: (NavigationItem) -> Unit,
) {
    if (LocalAppChromeStyle.current.chromePolicy.floatingDock) {
        ModernNavigationDock(tabs, indicatorProgress, onTabSelected)
        return
    }
    val textColor = MaterialTheme.colorScheme.onBackground
    val selectedColor = MaterialTheme.colorScheme.primary
    Column {
        HorizontalDivider(
            thickness = 0.3.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        )
        NavigationBar(
            containerColor = backgroundColor,
            tonalElevation = 0.dp,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected =
                    indicatorProgress >= index - 0.5f && indicatorProgress < index + 0.5f
                NavigationBarItem(
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = {
                        Text(
                            text = stringResource(id = tab.titleResId),
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                        )
                    },
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = textColor,
                        unselectedTextColor = textColor,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }
    }
}
