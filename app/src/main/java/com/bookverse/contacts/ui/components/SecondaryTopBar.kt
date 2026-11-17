package com.bookverse.contacts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.bookverse.contacts.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle
import com.bookverse.contacts.ui.theme.accentPalette
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.TextUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryTopBar(
    title: String,
    onBackClick: () -> Unit,
    actions: @Composable (ActionPillScope.() -> Unit)? = null,
    appSettingsViewModel: AppSettingsViewModel,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    titleContent: (@Composable () -> Unit)? = null,
    count: Int? = null,
    onCountClick: (() -> Unit)? = null,
    expandable: Boolean = false,
    searchAction: AppBarSearchAction? = null,
) {
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(density) > 0
    val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigateBack = {
        when {
            imeVisible -> keyboard?.hide()
            titleContent != null && backDispatcher != null -> backDispatcher.onBackPressed()
            else -> onBackClick()
        }
        Unit
    }
    if (accentBarStyle == AccentBarStyle.MODERN) {
        ModernTopBar(
            title = title, onBackClick = navigateBack,
            count = count, onCountClick = onCountClick,
            scrollBehavior = scrollBehavior, expandable = expandable,
            searchContent = titleContent, searchAction = searchAction, actions = actions,
        )
        return
    }
    val isMinimal = accentBarStyle == AccentBarStyle.MINIMAL
    val fontSize = if (isMinimal) 18.sp else 24.sp
    val colors = topBarColors(accentBarStyle)
    val pillContainerColor = if (isMinimal) MaterialTheme.colorScheme.surface else MaterialTheme.accentPalette.seed
    val pillContentColor = colors.titleContentColor

    CenterAlignedTopAppBar(
        modifier = Modifier.topBarBackground(accentBarStyle, scrollBehavior),
        title = {
            val defaultTitle: @Composable () -> Unit = {
                SecondaryTitleContent(
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
            when {
                titleContent != null && isMinimal -> {
                    CompositionLocalProvider(LocalContentColor provides pillContentColor) {
                        titleContent()
                    }
                }
                titleContent != null -> titleContent()
                isMinimal -> {
                    FloatingPill(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        backgroundColor = pillContainerColor,
                        contentColor = pillContentColor
                    ) {
                        defaultTitle()
                    }
                }
                else -> defaultTitle()
            }
        },
        navigationIcon = {
            if (isMinimal) {
                FloatingIconPill(
                    modifier = Modifier.padding(start = 8.dp),
                    onClick = navigateBack,
                    backgroundColor = pillContainerColor,
                    contentColor = pillContentColor
                ) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(onClick = navigateBack) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier.size(28.dp),
                        tint = pillContentColor
                    )
                }
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

@Composable
private fun SecondaryTitleContent(
    title: String,
    fontSize: TextUnit,
    count: Int?,
    badgeContainerColor: Color,
    badgeContentColor: Color,
    onCountClick: (() -> Unit)?,
    textColor: Color,
    isMinimal : Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
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
