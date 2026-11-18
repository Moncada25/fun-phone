package com.bookverse.contacts.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.domain.call.AudioRoute
import com.bookverse.contacts.domain.call.CallContactLookup
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.UiCall
import com.bookverse.contacts.service.CallController
import com.bookverse.contacts.service.DefaultCallRepository
import com.bookverse.contacts.ui.theme.CallEndColor
import com.bookverse.contacts.utils.SecondaryScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun CallStateBanner(
    currentSecondaryScreen: SecondaryScreen?,
    accentColor: Color,
    callContactLookup: CallContactLookup,
    onNavigate: (UiCall) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCall by DefaultCallRepository.activeCall.collectAsStateWithLifecycle()

    val shouldHide = currentSecondaryScreen == SecondaryScreen.IN_CALL || currentSecondaryScreen == SecondaryScreen.INCOMING_CALL

    val nonNullCall = activeCall
    if (shouldHide || nonNullCall == null || nonNullCall.state == CallState.DISCONNECTED) {
        return
    }

    val call: UiCall = nonNullCall

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    val bannerWidth = screenWidthDp - 32.dp
    val margin = 16.dp

    val statusBarTopPx = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }
    val topbarHeightPx = statusBarTopPx + with(density) { 56.dp.toPx() }

    val minX = with(density) { margin.toPx() }
    val maxX = with(density) { (screenWidthDp - bannerWidth - margin).toPx() }
    val minY = topbarHeightPx + with(density) { 4.dp.toPx() }
    val maxY = with(density) { (screenHeightDp - 140.dp - margin).toPx() }

    var offsetX by remember { mutableFloatStateOf(minX) }
    var offsetY by remember(topbarHeightPx) { mutableFloatStateOf(minY) }

    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    val isRinging = call.state == CallState.RINGING
    val isActive = call.state == CallState.ACTIVE
    val isDialing = call.state == CallState.DIALING
    val isOnHold = call.state == CallState.ON_HOLD
    val isMuted = call.isMuted
    val audioRoute = call.audioRoute
    val isSpeakerOn = audioRoute == AudioRoute.SPEAKER

    var contactName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(call.number) {
        contactName = resolveCallDisplayName(call, callContactLookup)
    }

    LaunchedEffect(call.id, isActive) {
        if (isActive) {
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                val currentCall = DefaultCallRepository.activeCall.value
                if (currentCall?.state != CallState.ACTIVE) break
                val connectTime = currentCall.connectTimeMillis?.takeIf { it > 0 }
                    ?: System.currentTimeMillis()
                elapsedSeconds = (System.currentTimeMillis() - connectTime) / 1000L
                delay(1000.milliseconds)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    val containerColor = accentColor

    val statusText = when {
        isRinging -> stringResource(R.string.banner_incoming_call)
        isDialing -> stringResource(R.string.calling)
        isOnHold -> stringResource(R.string.banner_on_hold)
        isActive -> {
            val m = (elapsedSeconds / 60).toInt()
            val s = (elapsedSeconds % 60).toInt()
            String.format(LocalLocale.current.platformLocale, "%d:%02d", m, s)
        }
        else -> ""
    }

    val displayName = contactName ?: call.displayName ?: call.number

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Box(
            modifier = modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .width(bannerWidth)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(containerColor)
                // Single gesture handler on the container: drag only. Tap-to-open lives on
                // the info area below via clickable, so a tap no longer competes with a
                // drag detector on the same node (which swallowed first taps when the
                // finger jitter exceeded touch slop) and taps on the action buttons
                // can no longer trigger navigation as a side effect.
                .pointerInput(minX, maxX, minY, maxY) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(minX, maxX)
                        offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                    }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.in_call),
                            onClick = { onNavigate(call) },
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isRinging) Icons.AutoMirrored.Filled.PhoneCallback else Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }

                if (isRinging) {
                    BannerActionButton(
                        icon = Icons.Default.Phone,
                        contentDescription = stringResource(R.string.answer),
                        containerColor = Color(0xFF4CAF50),
                        accentColor = accentColor,
                        onClick = { CallController.answer(call.id) }
                    )
                    BannerActionButton(
                        icon = Icons.Default.CallEnd,
                        contentDescription = stringResource(R.string.reject),
                        containerColor = CallEndColor,
                        accentColor = accentColor,
                        onClick = { CallController.reject(call.id) }
                    )
                } else {
                    BannerActionButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = stringResource(R.string.microphone),
                        selected = isMuted,
                        accentColor = accentColor,
                        onClick = { CallController.mute(!isMuted) }
                    )
                    BannerActionButton(
                        icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.PhoneInTalk,
                        contentDescription = stringResource(R.string.speaker),
                        selected = isSpeakerOn,
                        accentColor = accentColor,
                        onClick = {
                            val newRoute = if (isSpeakerOn) AudioRoute.EARPIECE else AudioRoute.SPEAKER
                            CallController.setAudioRoute(newRoute)
                        }
                    )
                    BannerActionButton(
                        icon = Icons.Default.CallEnd,
                        contentDescription = stringResource(R.string.hang_up),
                        containerColor = CallEndColor,
                        accentColor = accentColor,
                        onClick = { CallController.end(call.id) }
                    )
                }
            }
        }
    }
}

internal suspend fun resolveCallDisplayName(
    call: UiCall,
    callContactLookup: CallContactLookup,
): String {
    if (call.number.isNotEmpty()) {
        callContactLookup.findByPhoneNumber(call.number)?.name?.let { return it }
    }
    return call.displayName ?: call.number
}

@Composable
private fun BannerActionButton(
    icon: ImageVector,
    contentDescription: String?,
    accentColor: Color,
    containerColor: Color? = null,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = containerColor
        ?: if (selected) Color.White else Color.White.copy(alpha = 0.18f)
    val iconTint = when {
        containerColor != null -> Color.White
        selected -> accentColor
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}
