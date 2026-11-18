package com.bookverse.contacts.ui.screens.qr

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.TextField
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.QRHistoryViewModel
import com.bookverse.contacts.data.qr.QRType
import com.bookverse.contacts.utils.UrlUtils
import com.bookverse.contacts.utils.QRContentClassifier
import com.bookverse.contacts.utils.QRContentType
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumMap
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRGeneratorScreen(
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    qrHistoryViewModel: QRHistoryViewModel
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val outlineColor = MaterialTheme.colorScheme.outline

    var inputText by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var linkPreviewState by remember { mutableStateOf<LinkPreviewState>(LinkPreviewState.None) }
    val templateState = remember { QrTemplateState() }
    val payload = templateState.payload(inputText)
    val contentType = remember(payload) { QRContentClassifier.classify(payload) }
    val isWebUrl = contentType == QRContentType.WEB_URL
    val snackbar = rememberAppSnackbarState()
    var showHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(payload) {
        val trimmed = payload.trim()
        if (!isWebUrl) {
            linkPreviewState = LinkPreviewState.None
            return@LaunchedEffect
        }

        if (linkPreviewState is LinkPreviewState.Ready) {
            val ready = linkPreviewState as LinkPreviewState.Ready
            if (ready.originalInput == trimmed) return@LaunchedEffect
        }

        linkPreviewState = LinkPreviewState.Loading
        delay(350.milliseconds)
        linkPreviewState = fetchLinkPreview(trimmed)
    }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.qr_generator_title),
                onBackClick = onBackClick,
                actions = {
                    IconAction(
                        imageVector = Icons.Default.History,
                        contentDescription = stringResource(R.string.qr_history_title),
                        onClick = { showHistoryDialog = true }
                    )
                },
                appSettingsViewModel = appSettingsViewModel
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.qr_generator_instructions),
                color = textColor,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            QrTemplateSelector(
                selected = templateState.template,
                onSelect = {
                    if (it != templateState.template) {
                        templateState.template = it
                        errorMessage = null
                        qrBitmap = null
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (templateState.template == QrTemplate.TEXT) {
                TextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        errorMessage = null
                        if (it.isBlank()) {
                            qrBitmap = null
                        }
                    },
                    label = stringResource(R.string.qr_generator_hint),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                QrTemplateForm(
                    state = templateState,
                    onChanged = {
                        errorMessage = null
                        qrBitmap = null
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = payload.isNotBlank(),
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {
                Text(
                    text = stringResource(
                        if (contentType.isOpenable) {
                            R.string.qr_content_link
                        } else {
                            R.string.qr_content_text
                        }
                    ),
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            AnimatedVisibility(
                visible = linkPreviewState !is LinkPreviewState.None,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {
                LinkPreviewCard(
                    state = linkPreviewState,
                    accentColor = accentColor,
                    textColor = textColor,
                    onOpenLink = { normalizedUrl ->
                        UrlUtils.openUrl(context, normalizedUrl)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (payload.isBlank()) {
                        errorMessage = resources.getString(
                            if (templateState.template == QrTemplate.TEXT) {
                                R.string.qr_generator_hint
                            } else {
                                R.string.qr_template_incomplete
                            },
                        )
                        return@Button
                    }

                    try {
                        qrBitmap = generateQRCode(payload)
                        errorMessage = null
                        if (templateState.template != QrTemplate.WIFI) {
                            qrHistoryViewModel.addQRHistory(
                                content = payload,
                                isUrl = contentType.isOpenable,
                                type = QRType.GENERATED
                            )
                        }
                    } catch (_: Exception) {
                        errorMessage = resources.getString(R.string.qr_error_generating)
                    }
                },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = textColor
                )
            ) {
                Text(stringResource(R.string.qr_generator_button))
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .border(
                        width = 1.dp,
                        color = outlineColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = qrBitmap,
                        animationSpec = tween(durationMillis = 350)
                    ) { qr ->
                        if (qr != null) {
                            Image(
                                bitmap = qr.asImageBitmap(),
                                contentDescription = stringResource(R.string.qr_generator_title),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            EmptyQrState(
                                textColor = textColor,
                                accentColor = accentColor
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = qrBitmap != null,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                qrBitmap?.let { bitmap ->
                                    val result = shareQRCode(context, bitmap)
                                    if (result.isSuccess) {
                                        snackbar.show(
                                            message = resources.getString(R.string.qr_share_ready),
                                            type = SnackbarType.Info
                                        )
                                    } else {
                                        snackbar.show(
                                            message = resources.getString(R.string.qr_share_error),
                                            type = SnackbarType.Error
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.qr_share_code),
                                tint = textColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.share),
                                color = textColor
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                qrBitmap?.let { bitmap ->
                                    val result = saveQRCodeToGallery(context, bitmap)
                                    if (result.isSuccess) {
                                        snackbar.show(
                                            message = resources.getString(R.string.qr_saved_success),
                                            type = SnackbarType.Success
                                        )
                                    } else {
                                        snackbar.show(
                                            message = resources.getString(R.string.qr_saved_error),
                                            type = SnackbarType.Error
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = stringResource(R.string.qr_save_code),
                                tint = textColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.save),
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        QRHistoryDialog(
            onDismiss = { showHistoryDialog = false },
            qrHistoryViewModel = qrHistoryViewModel,
            appSettingsViewModel = appSettingsViewModel
        )
    }
}

@Composable
private fun EmptyQrState(
    textColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = stringResource(R.string.qr_empty_state_icon),
            tint = accentColor,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.qr_empty_state_message),
            color = textColor.copy(alpha = 0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LinkPreviewCard(
    state: LinkPreviewState,
    accentColor: Color,
    textColor: Color,
    onOpenLink: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        when (state) {
            LinkPreviewState.None -> Unit
            LinkPreviewState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                    Text(
                        text = stringResource(R.string.loading),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is LinkPreviewState.Ready -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clickable {
                            onOpenLink(state.normalizedUrl)
                        },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = stringResource(R.string.qr_link_preview_icon),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        tint = accentColor
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.displayUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private sealed interface LinkPreviewState {
    object None : LinkPreviewState
    object Loading : LinkPreviewState
    data class Ready(
        val title: String,
        val normalizedUrl: String,
        val displayUrl: String,
        val originalInput: String
    ) : LinkPreviewState
}

private fun fetchLinkPreview(rawInput: String): LinkPreviewState {
    val normalized = normalizeUrl(rawInput)
    val displayHost = runCatching {
        normalized.toUri().host?.removePrefix("www.")
    }.getOrNull().orEmpty()
    val displayUrl = displayHost.ifBlank { normalized }
    return LinkPreviewState.Ready(
        title = displayUrl,
        normalizedUrl = normalized,
        displayUrl = displayUrl,
        originalInput = rawInput
    )
}

private fun normalizeUrl(rawUrl: String): String {
    return QRContentClassifier.normalizeForOpening(rawUrl)
        ?.replace(" ", "%20")
        ?: rawUrl.trim()
}

private fun generateQRCode(content: String, size: Int = 512): Bitmap {
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
    hints[EncodeHintType.MARGIN] = 1
    hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
    hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

    val width = bitMatrix.width
    val height = bitMatrix.height
    val pixels = IntArray(width * height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] =
                if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }

    val bitmap = createBitmap(width, height)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

    return bitmap
}

private fun shareQRCode(context: Context, bitmap: Bitmap): Result<Unit> {
    return runCatching {
        val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(cachePath, "qr_code_${System.currentTimeMillis()}.png")

        FileOutputStream(file).use { outputStream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                throw IllegalStateException("QR compression failed")
            }
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(R.string.qr_share_title)
            )
        )
    }
}

private fun saveQRCodeToGallery(context: Context, bitmap: Bitmap): Result<Unit> {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
    val filename = "QR_${timestamp}.png"

    return runCatching {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IllegalStateException("Unable to create MediaStore entry")

        context.contentResolver.openOutputStream(uri)?.use { fos ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                throw IllegalStateException("QR compression failed")
            }
        } ?: throw IllegalStateException("Unable to open MediaStore stream")
    }
}
