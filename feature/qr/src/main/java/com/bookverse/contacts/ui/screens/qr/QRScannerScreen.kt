package com.bookverse.contacts.ui.screens.qr

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.feature.qr.R
import com.bookverse.contacts.integration.QrCodeDecoder
import com.bookverse.contacts.integration.createQrCodeDecoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.ui.components.LoaderDialog
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.QRHistoryViewModel
import com.bookverse.contacts.data.qr.QRType
import com.bookverse.contacts.utils.ClipboardUtils
import com.bookverse.contacts.utils.UrlUtils
import com.bookverse.contacts.utils.ContactShareFormat
import com.bookverse.contacts.utils.QRContentClassifier
import com.bookverse.contacts.utils.QRContentType
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    onCreateContact: (contact: Contact, successMessage: String, account: ContactAccount) -> Unit,
    qrHistoryViewModel: QRHistoryViewModel,
    onContactSaved: ((Contact) -> Unit)? = null
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val accountToSaveContact by appSettingsViewModel.defaultContactAccount.collectAsStateWithLifecycle()
    val successMessage = stringResource(CoreUiR.string.add_contact_success)
    var hasCameraPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var scannedContent by remember { mutableStateOf<String?>(null) }
    var scannedContentType by remember { mutableStateOf<QRContentType?>(null) }
    var contactAdded by remember { mutableStateOf<Contact?>(null) }
    var isLoadingContact by remember { mutableStateOf(false) }
    var scanningEnabled by remember { mutableStateOf(true) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var historySaved by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { it ->
            scanningEnabled = false
            val galleryScanner = runCatching { createQrCodeDecoder() }.getOrNull()
            if (galleryScanner == null) {
                scanningEnabled = true
                Toast.makeText(
                    context,
                    resources.getString(R.string.qr_error_scanning),
                    Toast.LENGTH_SHORT
                ).show()
                return@let
            }
            try {
                val bitmap = checkNotNull(
                    context.contentResolver.openInputStream(it)?.use(BitmapFactory::decodeStream),
                )
                galleryScanner.decodeBitmap(
                    bitmap = bitmap,
                    onSuccess = { decoded ->
                        galleryScanner.close()
                        if (decoded != null) {
                            val rawValue = decoded.rawValue
                            val contentType = QRContentClassifier.classify(
                                rawValue,
                                detectedAsUrl = decoded.isUrl,
                            )
                            val isContactContent =
                                rawValue.startsWith(ContactShareFormat.CONTACT_OBJECT)
                            if (isContactContent && onContactSaved != null) {
                                try {
                                    val contactJson =
                                        rawValue.substring(ContactShareFormat.CONTACT_OBJECT.length)
                                    val jsonObject =
                                        JsonParser.parseString(contactJson).asJsonObject
                                    val gson = GsonBuilder().create()
                                    val phones: List<ContactPhone> =
                                        if (jsonObject.has("phones") && jsonObject.get("phones").isJsonArray) {
                                            val type = object :
                                                TypeToken<List<ContactPhone>>() {}.type
                                            gson.fromJson(jsonObject.get("phones"), type)
                                        } else emptyList()
                                    val social: List<String> =
                                        if (jsonObject.has("socialMedia") && jsonObject.get("socialMedia").isJsonArray) {
                                            val type =
                                                object : TypeToken<List<String>>() {}.type
                                            gson.fromJson(jsonObject.get("socialMedia"), type)
                                        } else emptyList()
                                    val remotePhotoUriStr = jsonObject.get("remotePhotoUri")
                                        ?.takeIf { !it.isJsonNull }?.asString
                                    val remotePhotoUri = remotePhotoUriStr?.let {
                                        runCatching { it.toUri() }.getOrNull()
                                    }
                                    val contact = Contact(
                                        id = "",
                                        name = jsonObject.get("name")?.asString ?: "",
                                        phones = phones,
                                        birthday = jsonObject.get("birthday")
                                            ?.takeIf { !it.isJsonNull }?.asLong,
                                        email = jsonObject.get("email")
                                            ?.takeIf { !it.isJsonNull }?.asString,
                                        company = jsonObject.get("company")
                                            ?.takeIf { !it.isJsonNull }?.asString,
                                        isFavorite = jsonObject.get("isFavorite")
                                            ?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
                                        socialMedia = social,
                                        photoUri = null,
                                        remotePhotoUri = remotePhotoUri
                                    )
                                    coroutineScope.launch {
                                        isLoadingContact = true
                                        try {
                                            val contactToSave = contact.copy(
                                                photoUri = null,
                                                remotePhotoUri = null,
                                                accounts = listOf(accountToSaveContact)
                                            )
                                            onCreateContact(
                                                contactToSave,
                                                successMessage,
                                                accountToSaveContact,
                                            )
                                            contactAdded = contactToSave
                                            onContactSaved(contactToSave)
                                        } finally {
                                            isLoadingContact = false
                                        }
                                    }
                                } catch (_: Exception) {
                                    scanningEnabled = true
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.contact_qr_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                scannedContent = rawValue
                                scannedContentType = contentType
                                historySaved = false
                            }
                        } else {
                            scanningEnabled = true
                            Toast.makeText(
                                context,
                                resources.getString(R.string.qr_error_scanning),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onFailure = {
                        galleryScanner.close()
                        scanningEnabled = true
                        Toast.makeText(
                            context,
                            resources.getString(R.string.qr_error_scanning),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                )
            } catch (_: Exception) {
                galleryScanner.close()
                scanningEnabled = true
                Toast.makeText(
                    context,
                    resources.getString(R.string.qr_error_scanning),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            showPermissionDialog = true
        }
    }
    LaunchedEffect(Unit) {
        isTorchEnabled = false
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = stringResource(CoreUiR.string.qr_scanner_title),
                onBackClick = onBackClick,
                actions = {
                    IconAction(
                        imageVector = Icons.Default.Image,
                        contentDescription = stringResource(R.string.qr_pick_image),
                        onClick = { imagePicker.launch("image/*") }
                    )
                    IconAction(
                        imageVector = if (isTorchEnabled) Icons.Default.FlashOff else Icons.Default.FlashOn,
                        contentDescription = if (isTorchEnabled) {
                            stringResource(R.string.qr_flash_off)
                        } else {
                            stringResource(R.string.qr_flash_on)
                        },
                        onClick = { isTorchEnabled = !isTorchEnabled }
                    )
                },
                appSettingsViewModel = appSettingsViewModel
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            if (contactAdded == null && scannedContent == null) {
                if (hasCameraPermission) {
                    QRCodeScanner(
                        onQrCodeScanned = { content, isUrlContent ->
                            if (!scanningEnabled) return@QRCodeScanner
                            val isContactContent = content.startsWith(ContactShareFormat.CONTACT_OBJECT)
                            if (isContactContent && onContactSaved != null) {
                                scanningEnabled = false
                                try {
                                    val contactJson =
                                        content.substring(ContactShareFormat.CONTACT_OBJECT.length)
                                    val jsonObject =
                                        JsonParser.parseString(contactJson).asJsonObject
                                    val gson = GsonBuilder().create()
                                    val phones: List<ContactPhone> =
                                        if (jsonObject.has("phones") && jsonObject.get("phones").isJsonArray) {
                                            val type = object :
                                                TypeToken<List<ContactPhone>>() {}.type
                                            gson.fromJson(jsonObject.get("phones"), type)
                                        } else emptyList()
                                    val social: List<String> =
                                        if (jsonObject.has("socialMedia") && jsonObject.get("socialMedia").isJsonArray) {
                                            val type = object : TypeToken<List<String>>() {}.type
                                            gson.fromJson(jsonObject.get("socialMedia"), type)
                                        } else emptyList()
                                    val remotePhotoUriStr = jsonObject.get("remotePhotoUri")
                                        ?.takeIf { !it.isJsonNull }?.asString
                                    val remotePhotoUri =
                                        remotePhotoUriStr?.let { runCatching { it.toUri() }.getOrNull() }
                                    val contact = Contact(
                                        id = "",
                                        name = jsonObject.get("name")?.asString ?: "",
                                        phones = phones,
                                        birthday = jsonObject.get("birthday")
                                            ?.takeIf { !it.isJsonNull }?.asLong,
                                        email = jsonObject.get("email")
                                            ?.takeIf { !it.isJsonNull }?.asString,
                                        company = jsonObject.get("company")
                                            ?.takeIf { !it.isJsonNull }?.asString,
                                        isFavorite = jsonObject.get("isFavorite")
                                            ?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
                                        socialMedia = social,
                                        photoUri = null,
                                        remotePhotoUri = remotePhotoUri
                                    )
                                    coroutineScope.launch {
                                        isLoadingContact = true
                                        try {
                                            val contactToSave = contact.copy(
                                                photoUri = null,
                                                remotePhotoUri = null,
                                                accounts = listOf(accountToSaveContact)
                                            )
                                            onCreateContact(
                                                contactToSave,
                                                successMessage,
                                                accountToSaveContact,
                                            )
                                            contactAdded = contactToSave
                                            onContactSaved(contactToSave)
                                        } finally {
                                            isLoadingContact = false
                                        }
                                    }
                                } catch (_: Exception) {
                                    scanningEnabled = true
                                    DiagnosticLog.error(DiagnosticEvent.QR_CONTACT_PARSE_FAILED)
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.contact_qr_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                scanningEnabled = false
                                scannedContent = content
                                scannedContentType = QRContentClassifier.classify(
                                    content,
                                    detectedAsUrl = isUrlContent,
                                )
                                historySaved = false
                            }
                        },
                        isTorchEnabled = isTorchEnabled,
                        isScanningEnabled = scanningEnabled,
                        accentColor = accentColor,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = stringResource(CoreUiR.string.qr_scanner_permission_message),
                            color = textColor,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = accentColor
                            ),
                        ) {
                            Text(
                                text = stringResource(CoreUiR.string.grant_permission),
                                color = textColor
                            )
                        }
                    }
                }
            }
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { Text(stringResource(CoreUiR.string.permission_required)) },
                    text = { Text(stringResource(CoreUiR.string.qr_scanner_permission_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            showPermissionDialog = false
                        }) {
                            Text(stringResource(CoreUiR.string.grant_permission))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionDialog = false }) {
                            Text(stringResource(CoreUiR.string.cancel))
                        }
                    }
                )
            }
            if (scannedContent != null) {
                QRContentDialog(
                    content = scannedContent!!,
                    contentType = scannedContentType
                        ?: QRContentClassifier.classify(scannedContent!!),
                    onDismiss = {
                        scannedContent = null
                        scannedContentType = null
                        historySaved = false
                        scanningEnabled = true
                    },
                    accentColor = accentColor,
                    onSaveToHistory = {
                        if (!historySaved) {
                            qrHistoryViewModel.addQRHistory(
                                content = scannedContent!!,
                                isUrl = (scannedContentType
                                    ?: QRContentClassifier.classify(scannedContent!!)).isOpenable,
                                type = QRType.SCANNED
                            )
                            historySaved = true
                        }
                    }
                )
            }
            if (isLoadingContact) {
                LoaderDialog(
                    message = stringResource(R.string.loading_contacts),
                )
            }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun QRCodeScanner(
    onQrCodeScanned: (String, Boolean) -> Unit,
    isTorchEnabled: Boolean,
    isScanningEnabled: Boolean,
    accentColor: Color
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    val cameraRef = remember { mutableStateOf<Camera?>(null) }
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val latestScanningEnabled = rememberUpdatedState(isScanningEnabled)
    val zoomRatio = remember { mutableFloatStateOf(1f) }
    val minZoom = remember { mutableFloatStateOf(1f) }
    val maxZoom = remember { mutableFloatStateOf(1f) }
    var scannerRetryKey by remember { mutableIntStateOf(0) }
    val scanner = remember(scannerRetryKey) {
        runCatching { createQrCodeDecoder() }
            .onFailure { DiagnosticLog.error(DiagnosticEvent.QR_SCANNER_INIT_FAILED) }
            .getOrNull()
    }
    DisposableEffect(scanner) {
        onDispose { scanner?.close() }
    }
    val scaleGestureDetector = remember(previewView) {
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val camera = cameraRef.value ?: return false
                val newZoom = (zoomRatio.floatValue * detector.scaleFactor)
                    .coerceIn(minZoom.floatValue, maxZoom.floatValue)
                camera.cameraControl.setZoomRatio(newZoom)
                zoomRatio.floatValue = newZoom
                return true
            }
        })
    }
    val tapGestureDetector = remember(previewView) {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val factory = previewView.meteringPointFactory
                val point = factory.createPoint(e.x, e.y)
                cameraRef.value?.cameraControl?.startFocusAndMetering(
                    FocusMeteringAction.Builder(point).build()
                )
                previewView.performClick()
                return true
            }
        })
    }
    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraProviderRef.value?.unbindAll()
            cameraRef.value?.cameraControl?.enableTorch(false)
            cameraRef.value = null
            cameraProviderRef.value = null
            cameraExecutor.shutdown()
        }
    }
    if (scanner == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.qr_scanner_error_title),
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.qr_scanner_error_message),
                    color = textColor,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { scannerRetryKey++ },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = accentColor
                    ),
                ) {
                    Text(
                        text = stringResource(CoreUiR.string.qr_scanner_retry),
                        color = textColor
                    )
                }
            }
        }
        return
    }
    AndroidView(
        factory = {
            previewView.apply {
                setOnTouchListener { _, event ->
                    val scaleHandled = scaleGestureDetector.onTouchEvent(event)
                    val tapHandled = if (event.pointerCount == 1) {
                        tapGestureDetector.onTouchEvent(event)
                    } else {
                        false
                    }
                    scaleHandled || tapHandled
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { view ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = view.surfaceProvider
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        QRCodeAnalyzer(
                            scanner = scanner,
                            isScanningEnabled = { latestScanningEnabled.value },
                        ) { content, isUrl ->
                            onQrCodeScanned(content, isUrl)
                        },
                    )
                }
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                cameraProviderRef.value = cameraProvider
                cameraRef.value = camera
                minZoom.floatValue = camera.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                maxZoom.floatValue = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                zoomRatio.floatValue = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
            } catch (_: Exception) {
                DiagnosticLog.error(DiagnosticEvent.QR_CAMERA_BIND_FAILED)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    LaunchedEffect(isTorchEnabled, cameraRef.value) {
        cameraRef.value?.cameraControl?.enableTorch(isTorchEnabled)
    }
}

class QRCodeAnalyzer(
    private val scanner: QrCodeDecoder,
    private val isScanningEnabled: () -> Boolean = { true },
    private val onQrCodeDetected: (String, Boolean) -> Unit
) : ImageAnalysis.Analyzer {
    private val processing = AtomicBoolean(false)

    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanningEnabled() || !processing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        scanner.decodeFrame(imageProxy) { decoded ->
            if (decoded != null && isScanningEnabled()) {
                onQrCodeDetected(decoded.rawValue, decoded.isUrl)
            }
            processing.set(false)
            imageProxy.close()
        }
    }
}

@Composable
fun QRContentDialog(
    content: String,
    contentType: QRContentType,
    onDismiss: () -> Unit,
    accentColor: Color,
    onSaveToHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {
            onSaveToHistory()
            onDismiss()
        },
        title = {
            Text(
                text = stringResource(
                    if (contentType.isOpenable) {
                        R.string.qr_content_link
                    } else {
                        R.string.qr_content_text
                    }
                ),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = content,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            if (contentType.isOpenable) {
                TextButton(
                    onClick = {
                        onSaveToHistory()
                        UrlUtils.openUrl(context, content)
                        onDismiss()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.qr_open_link),
                        color = accentColor
                    )
                }
            } else {
                TextButton(
                    onClick = {
                        onSaveToHistory()
                        ClipboardUtils.copyToClipboard(context, content)
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(R.string.qr_copy_text), color = accentColor)
                }
            }
        },
        dismissButton = {
            if (contentType.isOpenable) {
                TextButton(
                    onClick = {
                        onSaveToHistory()
                        ClipboardUtils.copyToClipboard(context, content)
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(R.string.qr_copy_text), color = accentColor)
                }
            } else {
                TextButton(onClick = {
                    onSaveToHistory()
                    onDismiss()
                }) {
                    Text(text = stringResource(CoreUiR.string.close), color = accentColor)
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
    )
}
