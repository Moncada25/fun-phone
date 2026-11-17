package com.bookverse.contacts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.theme.LocalStatusColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class SnackbarType { Success, Error, Info, Warning }

data class AppSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val type: SnackbarType = SnackbarType.Info,
    val accentColor: Color? = null
) : SnackbarVisuals

@Stable
class AppSnackbarState internal constructor(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    fun show(
        message: String,
        type: SnackbarType = SnackbarType.Info,
        actionLabel: String? = null,
        withDismissAction: Boolean = true,
        duration: SnackbarDuration = SnackbarDuration.Short,
        accentColor: Color? = null,
        onAction: (() -> Unit)? = null
    ) {
        scope.launch {
            val result = hostState.showSnackbar(
                AppSnackbarVisuals(
                    message = message,
                    actionLabel = actionLabel,
                    withDismissAction = withDismissAction,
                    duration = duration,
                    type = type,
                    accentColor = accentColor
                )
            )
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }
}

@Composable
fun rememberAppSnackbarState(): AppSnackbarState {
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    return remember(host, scope) { AppSnackbarState(host, scope) }
}

@Composable
fun AppSnackbarHost(
    state: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val elevatedSurface = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    val onBackground = MaterialTheme.colorScheme.onBackground
    val statusColors = LocalStatusColors.current

    SnackbarHost(
        hostState = state,
        modifier = modifier.padding(bottom = LocalMainContentBottomPadding.current)
    ) { data ->
        val visuals = data.visuals as? AppSnackbarVisuals
        val type = visuals?.type ?: SnackbarType.Info

        val icon: ImageVector
        val iconTint: Color

        when (type) {
            SnackbarType.Success -> {
                icon = Icons.Filled.CheckCircle
                iconTint = visuals?.accentColor ?: statusColors.success
            }

            SnackbarType.Error -> {
                icon = Icons.Filled.Error
                iconTint = visuals?.accentColor ?: statusColors.error
            }

            SnackbarType.Warning -> {
                icon = Icons.Filled.Warning
                iconTint = visuals?.accentColor ?: statusColors.warning
            }

            SnackbarType.Info -> {
                icon = Icons.Filled.Info
                iconTint = visuals?.accentColor ?: statusColors.info
            }
        }

        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
            Snackbar(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = elevatedSurface,
                contentColor = onBackground,
                actionContentColor = onBackground,
                dismissActionContentColor = onBackground,
                action = {
                    data.visuals.actionLabel?.let { label ->
                        TextButton(onClick = { data.performAction() }) {
                            Text(label, fontWeight = FontWeight.SemiBold, color = onBackground)
                        }
                    }
                    if (data.visuals.withDismissAction) {
                        TextButton(onClick = { data.dismiss() }) {
                            Text(
                                stringResource(R.string.snackbar_ok),
                                fontWeight = FontWeight.SemiBold,
                                color = onBackground,
                            )
                        }
                    }
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint
                    )
                    Text(data.visuals.message, color = onBackground)
                }
            }

        }
    }
}
