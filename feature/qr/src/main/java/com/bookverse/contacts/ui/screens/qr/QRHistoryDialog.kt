package com.bookverse.contacts.ui.screens.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.bookverse.contacts.data.qr.QRHistoryEntity
import com.bookverse.contacts.data.qr.QRType
import com.bookverse.contacts.feature.qr.R
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.viewmodel.QRHistoryViewModel
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.utils.ClipboardUtils
import com.bookverse.contacts.utils.UrlUtils
import com.bookverse.contacts.utils.QRContentClassifier
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumMap
import com.bookverse.contacts.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRHistoryDialog(
    onDismiss: () -> Unit,
    qrHistoryViewModel: QRHistoryViewModel,) {
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    val uiState by qrHistoryViewModel.uiState.collectAsStateWithLifecycle()
    val qrHistoryList = uiState.items
    val context = LocalContext.current
    val operationErrorMessage = stringResource(R.string.qr_history_operation_error)

    LaunchedEffect(qrHistoryViewModel) {
        qrHistoryViewModel.effects.collect {
            Toast.makeText(
                context,
                operationErrorMessage,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    var showClearConfirmation by remember { mutableStateOf(false) }
    var selectedQR by remember { mutableStateOf<QRHistoryEntity?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.qr_history_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (qrHistoryList.isNotEmpty()) {
                            IconButton(onClick = { showClearConfirmation = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = stringResource(R.string.qr_history_clear),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = stringResource(CoreUiR.string.cancel),
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                } else if (uiState.loadFailed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.qr_history_load_error),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else if (qrHistoryList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = stringResource(R.string.qr_history_empty),
                                color = textColor.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(qrHistoryList, key = { it.id }) { qrItem ->
                            QRHistoryItem(
                                qrHistoryEntity = qrItem,
                                onViewQR = { selectedQR = it },
                                onDelete = { qrHistoryViewModel.deleteQRHistory(it.id) },
                                accentColor = accentColor,
                                textColor = textColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        ConfirmDialog(
            onDismiss = { showClearConfirmation = false },
            onConfirm = { qrHistoryViewModel.clearAllHistory() },
            title = stringResource(R.string.qr_history_clear_title),
            message = stringResource(R.string.qr_history_clear_message),
            confirmButtonLabel = stringResource(CoreUiR.string.delete)
        )
    }

    if (selectedQR != null) {
        QRViewDialog(
            qrHistoryEntity = selectedQR!!,
            onDismiss = { selectedQR = null },
            accentColor = accentColor,
            backgroundColor = backgroundColor,
            textColor = textColor
        )
    }
}

@Composable
fun QRHistoryItem(
    qrHistoryEntity: QRHistoryEntity,
    onViewQR: (QRHistoryEntity) -> Unit,
    onDelete: (QRHistoryEntity) -> Unit,
    accentColor: Color,
    textColor: Color
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    val dateFormat = remember(locale) { SimpleDateFormat("dd/MM/yyyy HH:mm", locale) }
    val formattedDate = remember(qrHistoryEntity.timestamp) {
        dateFormat.format(Date(qrHistoryEntity.timestamp))
    }

    val truncatedContent = remember(qrHistoryEntity.content) {
        if (qrHistoryEntity.content.length > 100) {
            "${qrHistoryEntity.content.take(100)}..."
        } else {
            qrHistoryEntity.content
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewQR(qrHistoryEntity) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (qrHistoryEntity.type == QRType.GENERATED) {
                            stringResource(R.string.qr_history_generated)
                        } else {
                            stringResource(R.string.qr_history_scanned)
                        },
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "•",
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = formattedDate,
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = truncatedContent,
                    color = textColor,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { onDelete(qrHistoryEntity) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(CoreUiR.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun QRViewDialog(
    qrHistoryEntity: QRHistoryEntity,
    onDismiss: () -> Unit,
    accentColor: Color,
    backgroundColor: Color,
    textColor: Color
) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val contentType = remember(qrHistoryEntity.content, qrHistoryEntity.isUrl) {
        QRContentClassifier.classify(
            qrHistoryEntity.content,
            detectedAsUrl = qrHistoryEntity.isUrl,
        )
    }

    LaunchedEffect(qrHistoryEntity.content) {
        withContext(Dispatchers.IO) {
            qrBitmap = generateQRCodeInternal(qrHistoryEntity.content)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (qrHistoryEntity.type == QRType.GENERATED) {
                            stringResource(R.string.qr_history_generated)
                        } else {
                            stringResource(R.string.qr_history_scanned)
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = stringResource(CoreUiR.string.cancel),
                            tint = textColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = stringResource(R.string.qr_generator_title),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = qrHistoryEntity.content,
                    color = textColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                if (contentType.isOpenable) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                UrlUtils.openUrl(context, qrHistoryEntity.content)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = contentColorFor(accentColor)
                            ),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(text = stringResource(R.string.qr_open_link))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    qrBitmap?.let { bitmap ->
                                        shareQRCodeInternal(context, bitmap)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(text = stringResource(CoreUiR.string.share))
                            }

                            OutlinedButton(
                                onClick = {
                                    ClipboardUtils.copyToClipboard(context, qrHistoryEntity.content)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(text = stringResource(R.string.qr_copy_text))
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                qrBitmap?.let { bitmap ->
                                    shareQRCodeInternal(context, bitmap)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(text = stringResource(CoreUiR.string.share))
                        }

                        OutlinedButton(
                            onClick = {
                                ClipboardUtils.copyToClipboard(context, qrHistoryEntity.content)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(text = stringResource(R.string.qr_copy_text))
                        }
                    }
                }
            }
        }
    }
}

private fun generateQRCodeInternal(content: String, size: Int = 512): Bitmap {
    val hints =
        EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
    hints[EncodeHintType.MARGIN] = 1
    hints[EncodeHintType.ERROR_CORRECTION] =
        ErrorCorrectionLevel.H
    hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

    val writer = QRCodeWriter()
    val bitMatrix =
        writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

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

private fun shareQRCodeInternal(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(cachePath, "qr_code_${System.currentTimeMillis()}.png")

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
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
    } catch (_: Exception) {
        DiagnosticLog.error(DiagnosticEvent.QR_SHARE_FAILED)
    }
}
