package com.bookverse.contacts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.ui.theme.chromePolicy
import com.bookverse.contacts.ui.theme.AccentBarStyle

@Stable
class FloatingOptionsState internal constructor(initialExpanded: Boolean) {
    var expanded by mutableStateOf(initialExpanded)
        internal set

    fun collapse() {
        expanded = false
    }

    fun toggle() {
        expanded = !expanded
    }
}

@Composable
fun rememberFloatingOptionsState(initialExpanded: Boolean = false): FloatingOptionsState {
    return remember { FloatingOptionsState(initialExpanded) }
}

@Composable
fun floatingOptionsBlur(state: FloatingOptionsState): Dp {
    val target = if (state.expanded) 12.dp else 0.dp
    val blur by animateDpAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = LocalAnimationSpeed.current.durationMs / 2),
        label = "floatingOptionsBlur"
    )
    return blur
}

data class FloatingOptionItem(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun FloatingOptions(
    state: FloatingOptionsState,
    actions: List<FloatingOptionItem>,
    accentColor: Color,
    accentBarStyle: AccentBarStyle,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    hideOffset: Dp = 0.dp,
    buttonPadding: PaddingValues = PaddingValues(end = 24.dp, bottom = 24.dp),
    overlayColor: Color = MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
    iconAdd: ImageVector = Icons.Default.Add,
    buttonDescription: String = stringResource(R.string.more_options),
) {
    val speed = LocalAnimationSpeed.current.durationMs
    fun motionDuration(base: Int) = base * speed / 500
    val springFactor = if (speed == 0) 1f else 500f / speed
    val overlayInteractionSource = remember { MutableInteractionSource() }
    val latestActions by rememberUpdatedState(actions)

    LaunchedEffect(isVisible) {
        if (!isVisible && state.expanded) {
            state.collapse()
        }
    }

    if (!isVisible && !state.expanded) return

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = state.expanded,
            enter = fadeIn(animationSpec = tween(durationMillis = motionDuration(150))),
            exit = fadeOut(animationSpec = tween(durationMillis = motionDuration(150)))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
                    .clickable(
                        interactionSource = overlayInteractionSource,
                        indication = null,
                        onClick = { state.collapse() }
                    )
                    .clearAndSetSemantics { }
            )
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(120)),
            modifier = Modifier
                .fillMaxSize()
                .padding(buttonPadding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    latestActions.forEachIndexed { index, action ->
                        AnimatedVisibility(
                            visible = state.expanded,
                            modifier = Modifier.align(Alignment.End),
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = motionDuration(180),
                                    delayMillis = motionDuration(index * 60)
                                )
                            ) + scaleIn(
                                initialScale = 0.8f,
                                animationSpec = if (speed == 0) tween(0) else spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow * springFactor * springFactor,
                                )
                            ),
                            exit = fadeOut(
                                animationSpec = tween(durationMillis = motionDuration(120))
                            ) + scaleOut(
                                targetScale = 0.8f,
                                animationSpec = if (speed == 0) tween(0) else spring(stiffness = Spring.StiffnessMedium * springFactor * springFactor)
                            )
                        ) {
                            FloatingOptionChip(
                                label = action.label,
                                icon = action.icon,
                                accentColor = accentColor,
                                enabled = action.enabled
                            ) {
                                if (action.enabled) {
                                    state.collapse()
                                    action.onClick()
                                }
                            }
                        }
                    }

                    val isMinimal =
                        accentBarStyle.chromePolicy.neutralControls
                    val fabContainerColor =
                        if (isMinimal) MaterialTheme.colorScheme.surface else accentColor
                    val fabContentColor =
                        if (isMinimal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onBackground
                    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

                    if (accentBarStyle == AccentBarStyle.MODERN) {
                        ModernFloatingActionButton(
                            icon = if (state.expanded) Icons.Default.Clear else iconAdd,
                            contentDescription = if (state.expanded) stringResource(R.string.cancel) else buttonDescription,
                            onClick = { state.toggle() },
                            modifier = Modifier.offset(y = hideOffset),
                        )
                    } else if (isMinimal) {
                        Surface(
                            onClick = { state.toggle() },
                            modifier = Modifier
                                .offset(y = hideOffset)
                                .size(56.dp),
                            shape = if (state.expanded) CircleShape else RoundedCornerShape(16.dp),
                            color = fabContainerColor,
                            contentColor = fabContentColor,
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, outlineColor)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (state.expanded) Icons.Default.Clear else iconAdd,
                                    contentDescription = if (state.expanded) stringResource(R.string.cancel) else buttonDescription,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    } else {
                        FloatingActionButton(
                            onClick = { state.toggle() },
                            containerColor = fabContainerColor,
                            contentColor = fabContentColor,
                            shape = if (state.expanded) CircleShape else RoundedCornerShape(16.dp),
                            modifier = Modifier.offset(y = hideOffset)
                        ) {
                            Icon(
                                imageVector = if (state.expanded) Icons.Default.Clear else iconAdd,
                                contentDescription = if (state.expanded) stringResource(R.string.cancel) else buttonDescription,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingOptionChip(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    }
    val iconTint = if (enabled) accentColor else accentColor.copy(alpha = 0.4f)
    val backgroundColor = accentColor.copy(alpha = if (enabled) 0.15f else 0.08f)

    Surface(
        onClick = { if (enabled) onClick() },
        color = Color.Transparent,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .wrapContentWidth(Alignment.End)
                .padding(start = 0.dp, end = 0.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                maxLines = 1,
                textAlign = TextAlign.End
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint
                )
            }
        }
    }
}
