package com.bookverse.contacts.ui.screens.calls

import android.app.Activity
import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bookverse.contacts.ui.theme.CallAcceptColor
import com.bookverse.contacts.ui.theme.CallRejectColor
import com.bookverse.contacts.ui.theme.GradientScrimColor
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.bookverse.contacts.App
import com.bookverse.contacts.R
import com.bookverse.contacts.domain.call.CallContactLookup
import com.bookverse.contacts.domain.call.CallContactPresentation
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.service.CallController
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.theme.ThemeMode
import com.bookverse.contacts.ui.viewmodel.calls.IncomingCallViewModel
import com.bookverse.contacts.utils.ContactUtils
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingCallScreen(
    callId: String?,
    onAnswer: () -> Unit,
    onDismiss: () -> Unit,
    onAutoMissed: () -> Unit,
    callContactLookup: CallContactLookup,
    themeMode: ThemeMode,
    appSettingsViewModel: AppSettingsViewModel
) {
    val context = LocalContext.current
    val callRepository = (context.applicationContext as App).container.callRepository
    val vm: IncomingCallViewModel =
        viewModel(
            factory = IncomingCallViewModel.provideFactory(
                callId = callId,
                callRepository = callRepository,
            ),
        )
    val call by vm.call.collectAsStateWithLifecycle()
    val number = call?.number ?: ""
    val callAnswerStyle by appSettingsViewModel.callAnswerStyle.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    var userAction by remember { mutableStateOf(false) }
    var hasNavigatedToInCall by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(call?.id) {
        userAction = false
        hasNavigatedToInCall = false
        dragOffset = 0f
        isDragging = false
    }

    LaunchedEffect(call) {
        if (call == null) {
            if (!userAction) onAutoMissed() else onDismiss()
            return@LaunchedEffect
        }
    }

    val callState = call?.state

    fun answerCall() {
        if (userAction) return
        userAction = true
        hasNavigatedToInCall = true
        CallController.answer(callId)
        onAnswer()
        dragOffset = 0f
    }

    fun rejectCall() {
        if (userAction) return
        userAction = true
        CallController.reject(callId)
        onDismiss()
        dragOffset = 0f
    }

    var showReplySheet by remember(call?.id) { mutableStateOf(false) }
    val canReplyWithMessage = call?.canRespondViaText == true && callState == CallState.RINGING

    fun rejectWithMessage(message: String) {
        if (userAction) return
        userAction = true
        showReplySheet = false
        CallController.rejectWithMessage(call?.id ?: callId, message)
        onDismiss()
        dragOffset = 0f
    }

    LaunchedEffect(callState) {
        if (callState == CallState.ACTIVE || callState == CallState.DIALING || callState == CallState.ON_HOLD) {
            if (!hasNavigatedToInCall) {
                hasNavigatedToInCall = true
                userAction = true
                onAnswer()
            }
            dragOffset = 0f
            isDragging = false
        }
    }

    var contact by remember(number) { mutableStateOf<CallContactPresentation?>(null) }
    LaunchedEffect(number) {
        contact = callContactLookup.findByPhoneNumber(number)
    }

    val prefilledName = call?.displayName
    val prefilledAvatarBytes = call?.avatarBytes
    val prefilledBitmap = remember(prefilledAvatarBytes) {
        prefilledAvatarBytes?.let { bytes ->
            runCatching {
                BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size
                )
            }.getOrNull()
        }
    }

    val photo = produceState(
        initialValue = prefilledBitmap,
        contact?.photoUri,
        prefilledBitmap,
    ) {
        val contactBitmap = contact?.photoUri?.let { callContactLookup.loadPhoto(it) }
        value = contactBitmap ?: prefilledBitmap
    }.value?.asImageBitmap()
    val displayName = prefilledName ?: contact?.name ?: number

    val incomingCallBackgroundUri = contact?.incomingCallBackgroundUri
    var backgroundLoadFailed by remember(incomingCallBackgroundUri) { mutableStateOf(false) }
    val hasIncomingCallBackground = incomingCallBackgroundUri != null && !backgroundLoadFailed

    val primaryTextColor = if (hasIncomingCallBackground) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val secondaryTextColor = primaryTextColor.copy(alpha = 0.76f)
    val tertiaryTextColor = primaryTextColor.copy(alpha = 0.58f)
    val unknown = stringResource(R.string.unknown)
    val callLabel = stringResource(R.string.recent_type_call_received)
    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val callBadgeAlpha = if (isDarkTheme) 0.18f else 0.12f
    val view = LocalView.current

    DisposableEffect(view, hasIncomingCallBackground, isDarkTheme) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatusBars = controller?.isAppearanceLightStatusBars
        val previousLightNavigationBars = controller?.isAppearanceLightNavigationBars
        if (controller != null) {
            val useLightSystemBarIcons = !hasIncomingCallBackground && !isDarkTheme
            controller.isAppearanceLightStatusBars = useLightSystemBarIcons
            controller.isAppearanceLightNavigationBars = useLightSystemBarIcons
        }
        onDispose {
            if (controller != null) {
                previousLightStatusBars?.let { controller.isAppearanceLightStatusBars = it }
                previousLightNavigationBars?.let {
                    controller.isAppearanceLightNavigationBars = it
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (incomingCallBackgroundUri != null && !backgroundLoadFailed) {
            AsyncImage(
                model = incomingCallBackgroundUri,
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

        val compactHeight = maxHeight < 600.dp || LocalDensity.current.fontScale >= 1.5f
        val horizontalPadding = if (maxWidth < 360.dp) 16.dp else 24.dp
        val verticalPadding = if (compactHeight) 16.dp else 40.dp
        val actionBottomPadding = if (compactHeight) 8.dp else 24.dp
        val identitySpacing = if (compactHeight) 4.dp else 12.dp
        val avatarSize = when {
            compactHeight -> 72.dp
            hasIncomingCallBackground -> 112.dp
            else -> 160.dp
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val badgeContentColor = if (hasIncomingCallBackground) Color.White else accentColor
            val badgeContainerColor = if (hasIncomingCallBackground) {
                GradientScrimColor.copy(alpha = 0.42f)
            } else {
                accentColor.copy(alpha = callBadgeAlpha)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeContainerColor)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PhoneCallback,
                    contentDescription = callLabel,
                    tint = badgeContentColor
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = callLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = badgeContentColor
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        identitySpacing,
                        Alignment.CenterVertically,
                    ),
                ) {
                    if (!hasIncomingCallBackground) {
                        Box(
                            modifier = Modifier
                                .size(avatarSize)
                                .shadow(20.dp, CircleShape, clip = false)
                                .border(
                                    width = if (hasIncomingCallBackground) 2.dp else 0.dp,
                                    color = primaryTextColor.copy(alpha = 0.82f),
                                    shape = CircleShape,
                                )
                                .clip(CircleShape)
                                .background(ContactUtils.getRandomColor(contact?.name ?: unknown)),
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
                                    contentScale = ContentScale.Crop,
                                    filterQuality = FilterQuality.High
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = stringResource(
                                        R.string.contact_no_photo_description,
                                        displayName
                                    ),
                                    modifier = Modifier.fillMaxSize(0.64f),
                                    tint = tertiaryTextColor
                                )
                            }
                        }
                    }

                    Text(
                        text = displayName,
                        style = if (compactHeight) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.headlineLarge
                        },
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val contactSnapshot = contact
                    val phoneLabel = when {
                        contactSnapshot != null && number != displayName -> {
                            val normalizedIncoming = number.filter(Char::isDigit)
                            val matchedPhone = contactSnapshot.phones.firstOrNull { phone ->
                                val normalized = phone.number.filter(Char::isDigit)
                                normalizedIncoming.isNotEmpty() &&
                                        (normalized.endsWith(normalizedIncoming) ||
                                                normalizedIncoming.endsWith(normalized))
                            } ?: contactSnapshot.phones.firstOrNull()

                            matchedPhone?.let { phone ->
                                val phoneTypeLabel = stringResource(callPhoneTypeLabel(phone.type))
                                "$phoneTypeLabel ${phone.number}"
                            } ?: number.ifBlank { null }
                        }

                        contactSnapshot == null && number.isNotBlank() && number != displayName -> number
                        else -> null
                    }

                    if (phoneLabel != null) {
                        Text(
                            text = phoneLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (canReplyWithMessage) {
                Row(
                    modifier = Modifier
                        .padding(bottom = if (compactHeight) 8.dp else 20.dp)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(badgeContainerColor)
                        .clickable(role = Role.Button) { showReplySheet = true }
                        .testTag(REJECT_MESSAGE_BUTTON_TAG)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        tint = badgeContentColor,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.reject_with_message),
                        style = MaterialTheme.typography.labelLarge,
                        color = badgeContentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (callAnswerStyle == "buttons") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = actionBottomPadding,
                            start = 24.dp,
                            end = 24.dp,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(CallRejectColor)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    role = Role.Button,
                                ) { rejectCall() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = stringResource(R.string.reject),
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.reject),
                            color = primaryTextColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(CallAcceptColor)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    role = Role.Button,
                                ) { answerCall() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = stringResource(R.string.answer),
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.answer),
                            color = primaryTextColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            } else {
                val trackShape = RoundedCornerShape(40.dp)
                val onSurface = primaryTextColor
                val neutralKnob = MaterialTheme.colorScheme.surface
                val acceptColor = CallAcceptColor
                val rejectColor = CallRejectColor

                var trackWidthPx by remember { mutableFloatStateOf(0f) }
                val knobSize = 64.dp
                val knobSizePx = with(LocalDensity.current) { knobSize.toPx() }

                val maxDrag = remember(trackWidthPx, knobSizePx) {
                    ((trackWidthPx - knobSizePx) / 2f).coerceAtLeast(0f)
                }
                val animatedOffset by animateFloatAsState(
                    targetValue = dragOffset,
                    animationSpec = if (isDragging) {
                        spring(stiffness = Spring.StiffnessMedium)
                    } else {
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    },
                    label = "knobOffsetAnimation"
                )
                val knobOffset = if (isDragging) dragOffset else animatedOffset
                val rightProgress =
                    if (maxDrag > 0f) (knobOffset / maxDrag).coerceIn(0f, 1f) else 0f
                val leftProgress =
                    if (maxDrag > 0f) (-knobOffset / maxDrag).coerceIn(0f, 1f) else 0f
                val colorProgress =
                    if (maxDrag > 0f) (abs(knobOffset) / (maxDrag * 0.55f)).coerceIn(0f, 1f) else 0f

                val smoothLeft by animateFloatAsState(
                    targetValue = leftProgress,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "smoothLeft"
                )
                val smoothRight by animateFloatAsState(
                    targetValue = rightProgress,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "smoothRight"
                )
                val smoothColorProgress by animateFloatAsState(
                    targetValue = colorProgress,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "smoothColorProgress"
                )
                val knobFraction =
                    if (maxDrag > 0f) ((knobOffset / (maxDrag * 2f)) + 0.5f).coerceIn(
                        0f,
                        1f
                    ) else 0.5f
                val animatedKnobFraction by animateFloatAsState(
                    targetValue = knobFraction,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "knobFraction"
                )

                val leftColor = lerp(
                    rejectColor.copy(alpha = 0.22f),
                    rejectColor.copy(alpha = 0.88f),
                    smoothLeft.pow(0.6f)
                )
                val rightColor = lerp(
                    acceptColor.copy(alpha = 0.22f),
                    acceptColor.copy(alpha = 0.88f),
                    smoothRight.pow(0.6f)
                )
                val centerGlowAlpha = 0.16f + 0.28f * smoothColorProgress
                val glowStart = (animatedKnobFraction - 0.18f).coerceIn(0f, 1f)
                val glowEnd = (animatedKnobFraction + 0.18f).coerceIn(0f, 1f)
                val trackBrush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to leftColor,
                        glowStart to leftColor.copy(alpha = leftColor.alpha * 0.7f),
                        animatedKnobFraction to onSurface.copy(alpha = centerGlowAlpha),
                        glowEnd to rightColor.copy(alpha = rightColor.alpha * 0.7f),
                        1f to rightColor
                    )
                )
                val velocityThresholdPx = 700f
                val answerLabel = stringResource(R.string.answer)
                val rejectLabel = stringResource(R.string.reject)
                val slideDescription = stringResource(
                    R.string.slide_to_answer_or_reject,
                    displayName,
                )
                val draggableState = rememberDraggableState { delta ->
                    if (maxDrag > 0f) {
                        val clamped = (dragOffset + delta).coerceIn(-maxDrag, maxDrag)
                        dragOffset = clamped
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = actionBottomPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .onSizeChanged { trackWidthPx = it.toSize().width }
                            .clip(trackShape)
                            .background(trackBrush)
                            .semantics {
                                contentDescription = slideDescription
                                customActions = listOf(
                                    CustomAccessibilityAction(answerLabel) {
                                        answerCall()
                                        true
                                    },
                                    CustomAccessibilityAction(rejectLabel) {
                                        rejectCall()
                                        true
                                    },
                                )
                            }
                            .draggable(
                                state = draggableState,
                                orientation = Orientation.Horizontal,
                                onDragStarted = {
                                    isDragging = true
                                },
                                onDragStopped = { velocity ->
                                    isDragging = false
                                    if (maxDrag <= 0f) {
                                        dragOffset = 0f
                                        return@draggable
                                    }
                                    val threshold = maxDrag * 0.28f
                                    val current = dragOffset
                                    val shouldAnswer =
                                        current > threshold || velocity > velocityThresholdPx
                                    val shouldReject =
                                        current < -threshold || velocity < -velocityThresholdPx

                                    when {
                                        shouldAnswer -> {
                                            answerCall()
                                        }

                                        shouldReject -> {
                                            rejectCall()
                                        }

                                        else -> {
                                            dragOffset = 0f
                                        }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val targetKnobColor =
                            if (knobOffset >= 0f) lerp(
                                neutralKnob,
                                acceptColor,
                                smoothColorProgress
                            )
                            else lerp(neutralKnob, rejectColor, smoothColorProgress)
                        val knobColor by animateColorAsState(
                            targetValue = targetKnobColor,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            ),
                            label = "knobColor"
                        )

                        val rejectTextAlphaTarget =
                            ((1f - rightProgress).coerceIn(0f, 1f).toDouble()
                                .pow(1.3)).toFloat()
                        val answerTextAlphaTarget = ((1f - leftProgress).coerceIn(0f, 1f).toDouble()
                            .pow(1.3)).toFloat()
                        val rejectTextAlpha by animateFloatAsState(
                            targetValue = rejectTextAlphaTarget,
                            animationSpec = tween(
                                durationMillis = 180,
                                easing = FastOutSlowInEasing
                            ),
                            label = "rejectTextAlpha"
                        )
                        val answerTextAlpha by animateFloatAsState(
                            targetValue = answerTextAlphaTarget,
                            animationSpec = tween(
                                durationMillis = 180,
                                easing = FastOutSlowInEasing
                            ),
                            label = "answerTextAlpha"
                        )
                        val knobScale by animateFloatAsState(
                            targetValue = if (abs(knobOffset) > knobSizePx * 0.25f) 1.05f else 1f,
                            animationSpec = tween(
                                durationMillis = 180,
                                easing = FastOutSlowInEasing
                            ),
                            label = "knobScale"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.reject),
                                color = onSurface.copy(alpha = rejectTextAlpha),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.answer),
                                color = onSurface.copy(alpha = answerTextAlpha),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(knobSize)
                                .offset { IntOffset(knobOffset.roundToInt(), 0) }
                                .graphicsLayer {
                                    scaleX = knobScale
                                    scaleY = knobScale
                                }
                                .shadow(12.dp, CircleShape, clip = false)
                                .clip(CircleShape)
                                .background(knobColor),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                knobOffset > 8f -> Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = stringResource(R.string.answer),
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )

                                knobOffset < -8f -> Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = stringResource(R.string.reject),
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )

                                else -> Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PhoneCallback,
                                    contentDescription = stringResource(
                                        R.string.slide_to_answer_or_reject,
                                        displayName
                                    ),
                                    tint = onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showReplySheet && canReplyWithMessage) {
            RejectWithMessageSheet(
                onSend = ::rejectWithMessage,
                onDismiss = { showReplySheet = false },
            )
        }
    }
}
