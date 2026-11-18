package com.bookverse.contacts.ui.screens.calls

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BluetoothAudio
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bookverse.contacts.ui.theme.CallEndColor
import com.bookverse.contacts.ui.theme.GradientScrimColor
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.App
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.domain.call.CallContactLookup
import com.bookverse.contacts.domain.call.CallContactPresentation
import com.bookverse.contacts.domain.call.AudioRoute
import com.bookverse.contacts.domain.call.CallConferencePolicy
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.service.CallController
import com.bookverse.contacts.ui.components.NumberKeypad
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.calls.InCallViewModel
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.ProximitySensorManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InCallScreen(
    callId: String?,
    number: String?,
    onAddCall: () -> Unit,
    onEnd: () -> Unit,
    callContactLookup: CallContactLookup,
    appSettingsViewModel: AppSettingsViewModel
) {
    val context = LocalContext.current
    val callRepository = (context.applicationContext as App).container.callRepository
    // Key the ViewModel by call: SecondaryScreenContent is not a NavBackStackEntry,
    // so without a key a reopened IN_CALL surface would reuse the previous call's
    // ViewModel instance (and its stale callId/callEnded state), instantly closing.
    val vm: InCallViewModel = viewModel(
        key = "in-call-${callId ?: "active"}",
        factory = InCallViewModel.provideFactory(
            callId = callId,
            callRepository = callRepository,
        ),
    )
    val vmActive by vm.activeCall.collectAsStateWithLifecycle()
    val repoActive by callRepository.activeCall.collectAsStateWithLifecycle()
    val active = vmActive ?: repoActive
    val duration by vm.durationSec.collectAsStateWithLifecycle()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val resolvedNumber = active?.number ?: number ?: ""
    val accentColor = MaterialTheme.colorScheme.primary
    val vibrationDuration by appSettingsViewModel.vibrationDuration.collectAsStateWithLifecycle()
    val vibrationPattern by appSettingsViewModel.vibrationPattern.collectAsStateWithLifecycle()
    val fontType by appSettingsViewModel.fontType.collectAsStateWithLifecycle()
    val dtmfTonesEnabled by appSettingsViewModel.dtmfTonesEnabled.collectAsStateWithLifecycle()

    var contact by remember(resolvedNumber) { mutableStateOf<CallContactPresentation?>(null) }
    LaunchedEffect(resolvedNumber) {
        contact = if (resolvedNumber.isNotEmpty()) {
            callContactLookup.findByPhoneNumber(resolvedNumber)
        } else null
    }

    val proximitySensorManager = remember {
        ProximitySensorManager(context) {
            DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_EVENT_RECEIVED)
        }
    }

    LaunchedEffect(active?.state, active?.audioRoute) {
        val shouldEnable = (active?.state == CallState.ACTIVE ||
                active?.state == CallState.DIALING ||
                active?.state == CallState.RINGING) &&
                active.audioRoute != AudioRoute.SPEAKER &&
                active.audioRoute != AudioRoute.BLUETOOTH

        DiagnosticLog.debug(DiagnosticEvent.CALL_STATE_UPDATED)

        if (shouldEnable) {
            proximitySensorManager.enable()
        } else {
            proximitySensorManager.forceDisable()
        }
    }

    // Single close path: the two previous LaunchedEffects could each invoke onEnd(),
    // double-closing the secondary surface right after a banner-tap reopen.
    val shouldClose = active?.state == CallState.DISCONNECTED || uiState.callEnded
    LaunchedEffect(shouldClose) {
        if (shouldClose) {
            proximitySensorManager.forceDisable()
            onEnd()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            DiagnosticLog.debug(DiagnosticEvent.IN_CALL_SCREEN_DISPOSED)
            vm.stopDtmf()
            proximitySensorManager.forceDisable()
        }
    }

    val photo = produceState<Bitmap?>(
        initialValue = null,
        contact?.photoUri,
    ) {
        value = contact?.photoUri?.let { callContactLookup.loadPhoto(it) }
    }.value?.asImageBitmap()
    val allCallsValue by vm.allCalls.collectAsStateWithLifecycle()
    val isConference = active?.isConference == true
    val participants = remember(active, allCallsValue) {
        active?.takeIf { it.isConference }
            ?.let { CallConferencePolicy.participants(it, allCallsValue) }
            .orEmpty()
    }
    val participantNames by produceState(
        initialValue = participants.map { it.displayName ?: it.number },
        participants.map { it.id to it.number },
    ) {
        value = participants.map { participant ->
            participant.displayName
                ?: participant.number.takeIf { it.isNotEmpty() }
                    ?.let { callContactLookup.findByPhoneNumber(it)?.name }
                ?: participant.number
        }
    }
    val displayName = if (isConference) {
        stringResource(R.string.conference_call)
    } else {
        contact?.name ?: resolvedNumber
    }
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val inCallBackgroundUri = contact?.inCallBackgroundUri
    var backgroundLoadFailed by remember(inCallBackgroundUri) { mutableStateOf(false) }
    val hasInCallBackground = inCallBackgroundUri != null && !backgroundLoadFailed
    val primaryTextColor = if (hasInCallBackground) Color.White else textColor
    val secondaryTextColor = primaryTextColor.copy(alpha = 0.7f)
    var showDialer by remember { mutableStateOf(false) }
    var dialedDigits by remember { mutableStateOf("") }
    val muted = active?.isMuted == true
    val onHold = active?.isOnHold == true
    val uiAudioRoute = active?.audioRoute ?: AudioRoute.EARPIECE
    val uiAudioEndpoint = active?.audioEndpoint
    var showOptions by remember { mutableStateOf(false) }
    val showAudioRouteDialog by vm.showAudioRouteDialog.collectAsStateWithLifecycle()
    val isAudioToggling by vm.isAudioToggling.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        if (inCallBackgroundUri != null && !backgroundLoadFailed) {
            AsyncImage(
                model = inCallBackgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { backgroundLoadFailed = true },
                onSuccess = { backgroundLoadFailed = false },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                GradientScrimColor.copy(alpha = 0.72f),
                                GradientScrimColor.copy(alpha = 0.58f),
                                GradientScrimColor.copy(alpha = 0.82f),
                            ),
                        ),
                    ),
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!hasInCallBackground) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.5f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                ContactUtils.getRandomColor(
                                    contact?.name ?: stringResource(R.string.unknown)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photo != null) {
                            Image(
                                bitmap = photo,
                                contentDescription = stringResource(
                                    R.string.contact_photo_description,
                                    displayName
                                ),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (isConference) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = primaryTextColor,
                                modifier = Modifier.fillMaxSize(0.5f),
                            )
                        } else {
                            Text(
                                text = (contact?.name ?: stringResource(R.string.unknown)
                                        ).take(1).uppercase(),
                                color = primaryTextColor,
                                fontSize = 120.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(IN_CALL_DISPLAY_NAME_TAG),
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isConference) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.conference_participants,
                            participants.size,
                            participants.size,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center,
                    )
                    val visibleNames = participantNames.filter { it.isNotBlank() }
                    if (visibleNames.isNotEmpty()) {
                        Text(
                            text = visibleNames.joinToString(separator = " · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag(IN_CALL_PARTICIPANTS_TAG),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                val contactSnapshot = contact
                if (!isConference && contactSnapshot != null && resolvedNumber != displayName) {

                    val toDisplay =
                        if (contactSnapshot.phones.any { it.number.contains(resolvedNumber) }) {
                            val phone =
                                contactSnapshot.phones.first { it.number.contains(resolvedNumber) }
                            "${stringResource(callPhoneTypeLabel(phone.type))} ${phone.number}"
                        } else {
                            resolvedNumber
                        }

                    Text(
                        text = toDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                val status = when {
                    active == null -> stringResource(R.string.connecting)
                    onHold -> stringResource(R.string.on_hold)
                    active.state == CallState.ACTIVE -> {
                        if (duration > 0) formatDuration(duration)
                        else formatDuration(0)
                    }
                    active.state == CallState.DIALING -> stringResource(R.string.calling)
                    active.state == CallState.RINGING -> stringResource(R.string.ringing)
                    active.state == CallState.DISCONNECTED -> stringResource(R.string.call_ended)
                    else -> if (duration > 0) formatDuration(duration) else stringResource(R.string.in_call)
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondaryTextColor
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        if (hasInCallBackground) {
                            GradientScrimColor.copy(alpha = 0.42f)
                        } else {
                            textColor.copy(alpha = 0.2f)
                        },
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        ActionButton(
                            selected = muted,
                            onClick = {
                                if (isAudioToggling) return@ActionButton
                                vm.toggleMute()
                            },
                            icon = if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = stringResource(if (muted) R.string.microphone_off else R.string.microphone_on),
                            accentColor = accentColor,
                            enabled = !isAudioToggling,
                            contentColor = if (hasInCallBackground) primaryTextColor else null,
                        )
                    }
                    item {
                        val endpointName =
                            if (uiAudioRoute == AudioRoute.BLUETOOTH) uiAudioEndpoint?.getDisplayName(
                                context
                            ) else null
                        val audioIcon = when (uiAudioRoute) {
                            AudioRoute.SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                            AudioRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
                            AudioRoute.EARPIECE -> Icons.Default.PhoneInTalk
                            AudioRoute.WIRED -> Icons.Default.Hearing
                        }
                        val audioLabel = when (uiAudioRoute) {
                            AudioRoute.SPEAKER -> stringResource(R.string.speaker)
                            AudioRoute.BLUETOOTH -> endpointName ?: "Bluetooth"
                            AudioRoute.EARPIECE -> stringResource(R.string.earpiece)
                            AudioRoute.WIRED -> stringResource(R.string.wired)
                        }
                        ActionButton(
                            selected = uiAudioRoute != AudioRoute.EARPIECE,
                            onClick = {
                                if (isAudioToggling) return@ActionButton
                                vm.toggleSpeaker(context)
                            },
                            onLongClick = {
                                vm.showAudioRouteDialog()
                            },
                            icon = audioIcon,
                            label = audioLabel,
                            accentColor = accentColor,
                            enabled = !isAudioToggling,
                            contentColor = if (hasInCallBackground) primaryTextColor else null,
                        )
                    }
                    item {
                        ActionButton(
                            onClick = { showDialer = true },
                            icon = Icons.Default.Dialpad,
                            label = stringResource(R.string.dialer),
                            accentColor = accentColor,
                            contentColor = if (hasInCallBackground) primaryTextColor else null,
                        )
                    }
                    item {
                        ActionButton(
                            onClick = { showOptions = true },
                            icon = if (showOptions) Icons.Default.KeyboardArrowDown else Icons.Default.MoreHoriz,
                            label = stringResource(R.string.more),
                            accentColor = accentColor,
                            contentColor = if (hasInCallBackground) primaryTextColor else null,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                ActionButton(
                    onClick = {
                        proximitySensorManager.forceDisable()
                        CallController.end(CallConferencePolicy.controlTargetId(callId, active))
                        onEnd()
                    },
                    icon = Icons.Default.CallEnd,
                    label = stringResource(R.string.hang_up),
                    containerColor = CallEndColor,
                    isLarge = true,
                    accentColor = accentColor,
                    contentColor = if (hasInCallBackground) primaryTextColor else null,
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showDialer) {
            ModalBottomSheet(
                onDismissRequest = { showDialer = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dialedDigits,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    NumberKeypad(
                        onKeyPressed = { digit ->
                            dialedDigits += digit
                        },
                        onKeyDown = { digit -> vm.startDtmf(digit.first()) },
                        onKeyUp = { vm.stopDtmf() },
                        accentColor = accentColor,
                        fontType = fontType,
                        vibrationDuration = vibrationDuration,
                        vibrationPattern = vibrationPattern,
                        dtmfTonesEnabled = dtmfTonesEnabled
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showOptions) {
            ModalBottomSheet(
                onDismissRequest = { showOptions = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.more_options),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item {
                            ActionButton(
                                selected = onHold,
                                onClick = { vm.toggleHold() },
                                icon = if (onHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                                label = if (onHold) stringResource(R.string.resume) else stringResource(
                                    R.string.hold
                                ),
                                accentColor = accentColor,
                                containerColor = textColor.copy(alpha = 0.2f)
                            )
                        }
                        item {
                            ActionButton(
                                onClick = onAddCall,
                                icon = Icons.Default.Phone,
                                label = stringResource(R.string.add_call),
                                accentColor = accentColor,
                                containerColor = textColor.copy(alpha = 0.2f)
                            )
                        }
                        if (CallConferencePolicy.topLevelCalls(allCallsValue).size > 1) {
                            item {
                                ActionButton(
                                    onClick = { vm.swap() },
                                    icon = Icons.Default.SwapHoriz,
                                    label = stringResource(R.string.swap),
                                    accentColor = accentColor,
                                    containerColor = textColor.copy(alpha = 0.2f)
                                )
                            }
                        }
                        if (CallConferencePolicy.canMerge(allCallsValue)) {
                            item {
                                ActionButton(
                                    onClick = {
                                        vm.merge()
                                        showOptions = false
                                    },
                                    icon = Icons.AutoMirrored.Filled.CallMerge,
                                    label = stringResource(R.string.merge_calls),
                                    accentColor = accentColor,
                                    containerColor = textColor.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showAudioRouteDialog) {
            ModalBottomSheet(
                onDismissRequest = { vm.hideAudioRouteDialog() },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.select_route),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    val availableEndpoints = vm.getAvailableAudioEndpoints()
                    val currentRoute = active?.audioRoute ?: AudioRoute.EARPIECE

                    availableEndpoints.forEach { endpoint ->
                        val routeIcon = when (endpoint.route) {
                            AudioRoute.SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                            AudioRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
                            AudioRoute.EARPIECE -> Icons.Default.PhoneInTalk
                            AudioRoute.WIRED -> Icons.Default.Hearing
                        }

                        val isSelected =
                            (currentRoute == endpoint.route && endpoint.identifier == null) ||
                                    (currentRoute == endpoint.route && endpoint.identifier != null && active?.audioEndpoint?.identifier == endpoint.identifier)

                        Button(
                            onClick = { vm.setAudioEndpoint(endpoint) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) accentColor else textColor.copy(
                                    alpha = 0.2f
                                ),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Icon(
                                    routeIcon,
                                    contentDescription = endpoint.getDisplayName(context),
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .size(24.dp)
                                )
                                Text(
                                    text = endpoint.getDisplayName(context),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector,
    label: String,
    containerColor: Color? = null,
    isLarge: Boolean = false,
    accentColor: Color,
    enabled: Boolean = true,
    contentColor: Color? = null,
) {
    val textColor = contentColor ?: MaterialTheme.colorScheme.onBackground
    val backgroundColor = MaterialTheme.colorScheme.background
    val defaultUnselected = if (contentColor != null) {
        Color.White.copy(alpha = 0.22f)
    } else {
        backgroundColor.copy(alpha = 0.5f)
    }
    val baseColor = containerColor ?: if (selected) accentColor else defaultUnselected
    val color by animateColorAsState(targetValue = baseColor, label = "actionButtonColor")
    val buttonHeight = if (isLarge) 70.dp else 64.dp
    val iconSize = if (isLarge) 36.dp else 32.dp
    val width = if (isLarge) 0.5f else 1f
    val targetScale = if (selected) 1.0f else 0.98f
    val scale by animateFloatAsState(targetValue = targetScale, label = "actionButtonScale")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = color),
            modifier = Modifier
                .fillMaxWidth(width)
                .height(buttonHeight)
                .clip(RoundedCornerShape(100))
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (onLongClick != null) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { onLongClick() }
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            enabled = enabled,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = textColor
        )
    }
}

internal const val IN_CALL_DISPLAY_NAME_TAG = "in_call_display_name"
internal const val IN_CALL_PARTICIPANTS_TAG = "in_call_participants"

private fun formatDuration(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return String.format(Locale.ROOT, "%d:%02d", m, s)
}
