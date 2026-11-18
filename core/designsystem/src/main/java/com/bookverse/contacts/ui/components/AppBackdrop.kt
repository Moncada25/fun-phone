package com.bookverse.contacts.ui.components

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bookverse.contacts.settings.GlassLevel
import com.bookverse.contacts.ui.theme.AppChrome
import com.bookverse.contacts.ui.theme.LocalGlassEffects
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

val LocalAppBackdrop = staticCompositionLocalOf<HazeState?> { null }
val LocalBackdropAllowed = staticCompositionLocalOf { true }

@Composable
internal fun rememberGlassLevel(): GlassLevel {
    val context = LocalContext.current.applicationContext
    val glassEffects = LocalGlassEffects.current
    return remember(glassEffects, context) { glassEffects.levelOnDevice(context) }
}

@Composable
fun rememberAppBackdrop(): HazeState? {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var visible by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> visible = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val power = remember(context) { context.getSystemService(PowerManager::class.java) }
    val lowMemory = remember(context) {
        context.getSystemService(ActivityManager::class.java).isLowRamDevice
    }
    var powerSave by remember(power) { mutableStateOf(power.isPowerSaveMode) }
    DisposableEffect(context, power) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                powerSave = power.isPowerSaveMode
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    val capable = rememberGlassLevel() != GlassLevel.NONE
    val allowed = visible && LocalBackdropAllowed.current && capable && !lowMemory && !powerSave
    // Dropping the state detaches source nodes when hidden or when the route disallows capture.
    return remember(allowed) { if (allowed) HazeState(initialBlurEnabled = true) else null }
}

@Composable
fun Modifier.appBackdropSource(): Modifier {
    val state = LocalAppBackdrop.current ?: return this
    return hazeSource(state)
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun Modifier.appBackdropSurface(
    color: Color = MaterialTheme.colorScheme.background,
    tintAlpha: Float = 0.86f,
    blurRadius: Dp = AppChrome.blurRadius,
): Modifier {
    val state = LocalAppBackdrop.current ?: return background(color)
    val style = remember(color, tintAlpha, blurRadius) {
        HazeStyle(
            backgroundColor = color,
            tint = HazeTint(color.copy(alpha = tintAlpha)),
            blurRadius = blurRadius,
            noiseFactor = 0f,
            fallbackTint = HazeTint(color),
        )
    }
    return hazeEffect(state, style) {
        // Only the already-blurred backdrop is downsampled; foreground text stays full resolution.
        inputScale = HazeInputScale.Auto
    }
}
