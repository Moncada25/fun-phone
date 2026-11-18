package com.bookverse.contacts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.theme.accentPalette
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    title: String = "",
    count: Int? = null,
    appSettingsViewModel: AppSettingsViewModel,
    scrollBehavior: TopAppBarScrollBehavior = rememberAppBarScrollBehavior(),
    titleContent: (@Composable () -> Unit)? = null,
    actions: @Composable (ActionPillScope.() -> Unit)? = null,
    onCountClick: (() -> Unit)? = null,
    searchAction: AppBarSearchAction? = null,
    solidWhenCollapsed: Boolean = false,
) {
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    if (accentBarStyle == AccentBarStyle.MODERN) {
        ModernTopBar(
            title = title, count = count, onCountClick = onCountClick,
            scrollBehavior = scrollBehavior, expandable = true,
            searchContent = titleContent, searchAction = searchAction, actions = actions,
        )
        return
    }
    val isMinimal = accentBarStyle == AccentBarStyle.MINIMAL
    val fontSize = if (isMinimal) 20.sp else 26.sp
    val colors = topBarColors(accentBarStyle)
    val pillContainerColor = if (isMinimal) MaterialTheme.colorScheme.surface else MaterialTheme.accentPalette.seed
    val pillContentColor = colors.titleContentColor

    TopAppBar(
        modifier = Modifier.topBarBackground(accentBarStyle, scrollBehavior, solidWhenCollapsed),
        title = {
            val defaultTitle: @Composable () -> Unit = {
                MainTopBarTitle(
                    title = title,
                    fontSize = fontSize,
                    count = count,
                    badgeContainerColor = pillContentColor.copy(alpha = 0.25f),
                    badgeContentColor = pillContentColor,
                    onCountClick = onCountClick,
                    textColor = pillContentColor,
                    isMinimal = isMinimal
                )
            }
            val resolvedTitle = titleContent
            when {
                resolvedTitle != null && isMinimal -> {
                    CompositionLocalProvider(LocalContentColor provides pillContentColor) {
                        resolvedTitle()
                    }
                }
                resolvedTitle != null -> resolvedTitle()
                isMinimal -> {
                    FloatingPill(
                        modifier = Modifier,
                        backgroundColor = pillContainerColor,
                        contentColor = pillContentColor
                    ) {
                        defaultTitle()
                    }
                }
                else -> defaultTitle()
            }
        },
        actions = {
            if (actions != null || searchAction != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scope = ActionPillScope(
                        delegate = this,
                        isMinimal = isMinimal,
                        pillContainerColor = pillContainerColor,
                        pillContentColor = pillContentColor
                    )
                    if (titleContent == null) searchAction?.let { search ->
                        scope.IconAction(Lucide.Search, search.label, search.onClick, feedback = search.feedback)
                    }
                    actions?.invoke(scope)
                }
            }
        },
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}

/** Paints the app bar and its system insets as one surface while the controls are visible. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Modifier.topBarBackground(
    accentBarStyle: AccentBarStyle,
    scrollBehavior: TopAppBarScrollBehavior?,
    solidWhenCollapsed: Boolean = false,
): Modifier {
    if (accentBarStyle == AccentBarStyle.MODERN) return appBackdropSurface()
    val isRetro = accentBarStyle == AccentBarStyle.RETRO
    val color = if (isRetro) MaterialTheme.accentPalette.seed else MaterialTheme.colorScheme.background
    return drawBehind {
        if (isRetro || solidWhenCollapsed || scrollBehavior?.state?.collapsedFraction != 1f) {
            drawRect(color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun topBarColors(accentBarStyle: AccentBarStyle): TopAppBarColors {
    val contentColor = if (accentBarStyle == AccentBarStyle.RETRO) {
        MaterialTheme.accentPalette.onSeed
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    return TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
        navigationIconContentColor = contentColor,
        titleContentColor = contentColor,
        actionIconContentColor = contentColor,
    )
}

@Composable
private fun MainTopBarTitle(
    title: String,
    fontSize: TextUnit,
    count: Int?,
    badgeContainerColor: Color,
    badgeContentColor: Color,
    onCountClick: (() -> Unit)?,
    textColor: Color,
    isMinimal: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
        AnimatedVisibility(
            visible = count != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                CountBadge(
                    count = count ?: 0,
                    accent = badgeContainerColor,
                    content = badgeContentColor,
                    onClick = onCountClick,
                    isMinimal = isMinimal
                )
            }
        }
    }
}
