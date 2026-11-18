package com.bookverse.contacts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.ui.theme.AppChrome
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import com.bookverse.contacts.core.ui.R as CoreUiR

@Composable
fun retroGameDialogBlur(isVisible: Boolean): Dp {
    val target = if (isVisible) 12.dp else 0.dp
    val blur by animateDpAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 300),
        label = "retroGameDialogBlur"
    )
    return blur
}

@Composable
fun RetroGameInstructionsDialog(
    title: String,
    instructions: String,
    onPlayClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        isVisible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = fadeOut(
                animationSpec = tween(200)
            ) + scaleOut(
                targetScale = 0.9f,
                animationSpec = tween(200)
            )
        ) {
            Surface(
                shape = RoundedCornerShape(AppChrome.sheetCorner),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ArcadeDialogHeader(
                        title = title,
                        eyebrow = stringResource(R.string.games_how_to_play),
                        onClose = onDismiss,
                    )

                    ArcadeScreenPanel {
                        Text(
                            text = instructions,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = AppChrome.touchTarget),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = stringResource(CoreUiR.string.cancel),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        ArcadePlayButton(
                            onClick = onPlayClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArcadeScreenPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .crtScanlines(
                lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f),
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun ArcadePlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "play_button_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AppChrome.touchTarget)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
        )
        Text(
            text = stringResource(R.string.play_game),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
