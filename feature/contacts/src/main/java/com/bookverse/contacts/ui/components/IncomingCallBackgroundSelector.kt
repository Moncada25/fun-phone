package com.bookverse.contacts.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.bookverse.contacts.core.ui.R
import java.io.File

@Composable
fun IncomingCallBackgroundSelector(
    onDismiss: () -> Unit,
    onPreviewBackground: () -> Unit,
    onBackgroundSelected: (Uri) -> Unit,
    onDeleteBackground: (() -> Unit)?,
    hasExistingBackground: Boolean,
    titleRes: Int = R.string.incoming_call_background_title,
    subtitleRes: Int = R.string.incoming_call_background_description,
    removeLabelRes: Int = R.string.incoming_call_background_remove,
    previewLabelRes: Int = R.string.incoming_call_preview_title,
    cameraPermissionRes: Int = R.string.incoming_call_background_camera_permission,
    temporaryFilePrefix: String = "incoming_call_background_",
) {
    val context = LocalContext.current
    val accentColor = MaterialTheme.colorScheme.primary
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }

    fun closeSheet() {
        tempCameraFile?.delete()
        tempCameraFile = null
        tempCameraUri = null
        onDismiss()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onBackgroundSelected(uri)
            onDismiss()
        } else {
            closeSheet()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val selectedFile = tempCameraFile
        if (success && selectedFile != null) {
            tempCameraFile = null
            tempCameraUri = null
            onBackgroundSelected(selectedFile.toUri())
            onDismiss()
        } else {
            closeSheet()
        }
    }

    fun launchCamera() {
        val photoFile = File.createTempFile(
            temporaryFilePrefix,
            ".jpg",
            context.cacheDir,
        )
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile,
        )
        tempCameraFile = photoFile
        tempCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            showCameraPermissionDialog = true
        }
    }

    ButtonSheetScaffold(
        title = stringResource(titleRes),
        subtitle = stringResource(subtitleRes),
        onDismiss = ::closeSheet,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OptionRow(
                option = OptionsDialogItem(
                    label = stringResource(R.string.choose_from_gallery),
                    icon = Icons.Default.PhotoLibrary,
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ),
                onDismiss = {},
            )
            OptionRow(
                option = OptionsDialogItem(
                    label = stringResource(R.string.take_photo),
                    icon = Icons.Default.PhotoCamera,
                    onClick = {
                        if (
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            launchCamera()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                ),
                onDismiss = {},
            )
            if (hasExistingBackground) {
                OptionRow(
                    option = OptionsDialogItem(
                        label = stringResource(previewLabelRes),
                        icon = Icons.Default.Visibility,
                        onClick = {
                            onPreviewBackground()
                            closeSheet()
                        },
                    ),
                    onDismiss = {},
                )
            }
            if (hasExistingBackground && onDeleteBackground != null) {
                OptionRow(
                    option = OptionsDialogItem(
                        label = stringResource(removeLabelRes),
                        icon = Icons.Default.DeleteOutline,
                        destructive = true,
                        onClick = {
                            onDeleteBackground()
                            closeSheet()
                        },
                    ),
                    onDismiss = {},
                )
            }
        }
    }

    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            title = { Text(stringResource(R.string.permission_required)) },
            text = { Text(stringResource(cameraPermissionRes)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCameraPermissionDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                ) {
                    Text(stringResource(R.string.grant_permission))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCameraPermissionDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}
