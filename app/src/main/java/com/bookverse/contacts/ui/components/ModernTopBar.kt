package com.bookverse.contacts.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModernTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior?,
    expandable: Boolean,
    onBackClick: (() -> Unit)? = null,
    count: Int? = null,
    onCountClick: (() -> Unit)? = null,
    searchContent: (@Composable () -> Unit)? = null,
    searchAction: AppBarSearchAction? = null,
    actions: @Composable (ActionPillScope.() -> Unit)? = null,
) {
    val canExpand = useExpandedAppBar(expandable)
    val expansionRange = TopAppBarDefaults.LargeAppBarExpandedHeight - TopAppBarDefaults.LargeAppBarCollapsedHeight
    val limit = with(LocalDensity.current) { -expansionRange.toPx() }
    SideEffect { if (canExpand) scrollBehavior?.state?.heightOffsetLimit = limit }
    val searchTransition by animateFloatAsState(
        targetValue = if (searchContent == null) 1f else 0f,
        animationSpec = tween(LocalAnimationSpeed.current.durationMs.coerceAtMost(250)),
        label = "Search header expansion",
    )
    // Scroll is followed directly; only opening/closing search uses a timed transition.
    val expansion = if (canExpand) (1f - (scrollBehavior?.state?.collapsedFraction ?: 1f)) * searchTransition else 0f
    val navigation: @Composable () -> Unit = {
        onBackClick?.let { back ->
            IconButton(onClick = back) {
                Icon(Lucide.ArrowLeft, stringResource(R.string.back))
            }
        }
    }
    val titleSlot: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                modifier = Modifier.weight(1f, fill = false),
                fontWeight = FontWeight.SemiBold,
                style = lerp(MaterialTheme.typography.titleLarge, MaterialTheme.typography.headlineLarge, expansion),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                CountBadge(
                    count = count,
                    accent = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onCountClick,
                    isMinimal = false,
                )
            }
        }
    }
    val actionSlot: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            val scope = ActionPillScope(
                delegate = this,
                isMinimal = false,
                pillContainerColor = MaterialTheme.colorScheme.surface,
                pillContentColor = MaterialTheme.colorScheme.onSurface,
            )
            actions?.invoke(scope)
        }
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        LiquidGlassSurface(Modifier.fillMaxWidth(), shape = LiquidGlassShape.TOP_BAR) {
            Column(Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets)) {
                Spacer(Modifier.height(expansionRange * expansion))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = TopAppBarDefaults.LargeAppBarCollapsedHeight)
                        .padding(start = if (onBackClick == null) 16.dp else 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    navigation()
                    Box(Modifier.weight(1f)) { titleSlot() }
                    actionSlot()
                }
                if (searchContent != null || searchAction != null) {
                    Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
                        if (searchContent != null) searchContent() else searchAction?.let { AppBarSearchEntry(it) }
                    }
                }
            }
        }
    }
}
