package com.bookverse.contacts.ui.screens.tools

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.AppSnackbarState
import com.bookverse.contacts.ui.components.CollageLayout
import com.bookverse.contacts.ui.components.CollageStyleDialog
import com.bookverse.contacts.ui.components.ColorPickerDialog
import com.bookverse.contacts.ui.components.DrawingCanvas
import com.bookverse.contacts.ui.components.OptionsDialog
import com.bookverse.contacts.ui.components.OptionsDialogItem
import com.bookverse.contacts.ui.components.PaintControlsEvent
import com.bookverse.contacts.ui.components.PaintControlsState
import com.bookverse.contacts.ui.components.PaintToolCategory
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.TopControlsBar
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.components.renderCollageBitmap
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.DrawingAction
import com.bookverse.contacts.ui.viewmodel.DrawingEvent
import com.bookverse.contacts.ui.viewmodel.DrawingInputMode
import com.bookverse.contacts.ui.viewmodel.DrawingViewModel
import com.bookverse.contacts.ui.viewmodel.PathType
import com.bookverse.contacts.utils.ImageProcessingResult
import com.bookverse.contacts.utils.ImageProcessor
import com.bookverse.contacts.utils.ImageSizingPolicy
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class CollageDraft(
    val layout: CollageLayout,
    val photos: List<Bitmap>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaintScreen(onBackClick: () -> Unit, appSettingsViewModel: AppSettingsViewModel) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val backgroundColor = MaterialTheme.colorScheme.background

    val viewModel = viewModel<DrawingViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val onAccent = contentColorFor(accentColor)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val rootView = LocalView.current
    val snackbarState = rememberAppSnackbarState()

    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var textToAdd by remember { mutableStateOf("") }
    var textSize by remember { mutableFloatStateOf(48f) }
    var canvasBounds by remember { mutableStateOf<Rect?>(null) }
    var textColor by remember { mutableStateOf(Color.Unspecified) }
    var textBackgroundColor by remember { mutableStateOf(Color.White) }
    var textHasBackground by remember { mutableStateOf(false) }
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showTextBgColorPicker by remember { mutableStateOf(false) }
    val textSizeRange = 24f..256f
    var showCollageDialog by remember { mutableStateOf(false) }
    var selectedCollageLayout by remember { mutableStateOf<CollageLayout?>(null) }
    var isLoadingImage by remember { mutableStateOf(false) }
    var isLoadingCollage by remember { mutableStateOf(false) }
    var collageDraft by remember { mutableStateOf<CollageDraft?>(null) }
    var isRenderingCollage by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var selectedToolCategory by rememberSaveable { mutableStateOf(PaintToolCategory.PENCIL) }
    var selectedShape by rememberSaveable { mutableStateOf(PathType.CIRCLE) }
    var shapeFilled by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val handleBack: () -> Unit = {
        when {
            showMoreOptions -> showMoreOptions = false
            showOptionsDialog -> showOptionsDialog = false
            showTextDialog -> {
                showTextDialog = false
                textToAdd = ""
            }
            showCollageDialog -> showCollageDialog = false
            state.pendingText != null -> viewModel.onAction(DrawingAction.OnCancelTextPlacement)
            state.pendingShape != null -> viewModel.onAction(DrawingAction.OnCancelShapePlacement)
            state.isImagePlacementMode -> viewModel.onAction(DrawingAction.OnCancelImagePlacement)
            else -> onBackClick()
        }
    }
    BackHandler(enabled = collageDraft == null, onBack = handleBack)

    LaunchedEffect(backgroundColor) {
        viewModel.onAction(DrawingAction.OnSetBackgroundColor(backgroundColor))
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DrawingEvent.BlurFailed -> snackbarState.show(
                    message = resources.getString(R.string.paint_blur_error),
                    type = SnackbarType.Error,
                )
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isLoadingImage = true
            scope.launch {
                when (
                    val result = ImageProcessor.decode(
                        context = context,
                        uri = uri,
                        target = ImageSizingPolicy.paint(canvasWidth, canvasHeight),
                    )
                ) {
                    is ImageProcessingResult.Success -> viewModel.onAction(
                        DrawingAction.OnImageSelected(
                            bitmap = result.bitmap.asImageBitmap(),
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                        ),
                    )

                    ImageProcessingResult.Failure -> snackbarState.show(
                        message = resources.getString(R.string.paint_import_error),
                        type = SnackbarType.Error,
                    )
                }
                isLoadingImage = false
            }
        }
    }

    val collagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(4)
    ) { uris ->
        val layout = selectedCollageLayout
        if (layout == null || uris.isEmpty()) {
            selectedCollageLayout = null
            return@rememberLauncherForActivityResult
        }
        if (uris.size < layout.requiredPhotos) {
            snackbarState.show(
                message = resources.getQuantityString(
                    R.plurals.paint_collage_not_enough_photos,
                    layout.requiredPhotos,
                    layout.requiredPhotos,
                ),
                type = SnackbarType.Warning
            )
            selectedCollageLayout = null
            return@rememberLauncherForActivityResult
        }
        val selectedUris = uris.take(layout.requiredPhotos)
        val cw = canvasWidth
        val ch = canvasHeight
        isLoadingCollage = true
        scope.launch(Dispatchers.IO) {
            var bitmaps: List<Bitmap> = emptyList()
            var handedOffToEditor = false
            try {
                val target = ImageSizingPolicy.collage(
                    requiredPhotos = layout.requiredPhotos,
                    outputWidth = cw,
                    outputHeight = ch,
                )
                bitmaps = selectedUris.mapNotNull { uri ->
                    when (val result = ImageProcessor.decode(context, uri, target)) {
                        is ImageProcessingResult.Success -> result.bitmap
                        ImageProcessingResult.Failure -> null
                    }
                }
                if (bitmaps.size < layout.requiredPhotos) {
                    withContext(Dispatchers.Main) {
                        isLoadingCollage = false
                        snackbarState.show(
                            message = resources.getString(R.string.paint_collage_error),
                            type = SnackbarType.Error
                        )
                        selectedCollageLayout = null
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    isLoadingCollage = false
                    collageDraft = CollageDraft(layout = layout, photos = bitmaps)
                    selectedCollageLayout = null
                    handedOffToEditor = true
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                DiagnosticLog.error(DiagnosticEvent.PAINT_COLLAGE_LOAD_FAILED)
                withContext(Dispatchers.Main) {
                    isLoadingCollage = false
                    snackbarState.show(
                        message = resources.getString(R.string.paint_collage_error),
                        type = SnackbarType.Error
                    )
                    selectedCollageLayout = null
                }
            } finally {
                if (!handedOffToEditor) {
                    bitmaps.forEach { bitmap ->
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                }
            }
        }
    }

    val controlsEnabled = !isLoadingImage &&
        !isLoadingCollage &&
        !showOptionsDialog &&
        !state.isImagePlacementMode &&
        state.pendingShape == null &&
        state.pendingText == null &&
        !state.isBlurProcessing

    if (showOptionsDialog) {
        OptionsDialog(
            title = stringResource(R.string.save_options),
            options = listOf(
                stringResource(R.string.save_locally),
                stringResource(R.string.share)
            ),
            onOptionSelected = { index ->
                val bounds = canvasBounds
                val capturedBitmap = captureCanvasBitmap(rootView, bounds)
                if (capturedBitmap == null) {
                    snackbarState.show(
                        message = resources.getString(R.string.drawing_saved_error),
                        type = SnackbarType.Error
                    )
                } else {
                    when (index) {
                        0 -> saveDrawingToGallery(
                            context,
                            capturedBitmap,
                            snackbarState
                        )

                        1 -> shareDrawing(context, capturedBitmap, snackbarState)
                    }
                }
                showOptionsDialog = false
            },
            onDismiss = { showOptionsDialog = false },
        )
    }

    if (showMoreOptions) {
        OptionsDialog(
            title = stringResource(R.string.more_options),
            options = listOf(
                OptionsDialogItem(
                    label = stringResource(R.string.paint_import_image),
                    icon = Icons.Default.Image,
                    enabled = controlsEnabled,
                    onClick = {
                        if (canvasWidth == 0 || canvasHeight == 0) {
                            snackbarState.show(
                                message = resources.getString(R.string.paint_import_unavailable),
                                type = SnackbarType.Warning,
                            )
                        } else {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        }
                    },
                ),
                OptionsDialogItem(
                    label = stringResource(R.string.save_drawing),
                    icon = Icons.Default.Save,
                    enabled = !state.isBlurProcessing,
                    onClick = { showOptionsDialog = true },
                ),
                OptionsDialogItem(
                    label = stringResource(R.string.cd_redo),
                    icon = Icons.AutoMirrored.Filled.Redo,
                    enabled = state.canRedo && !state.isBlurProcessing,
                    onClick = { viewModel.onAction(DrawingAction.OnRedoClick) },
                ),
            ),
            onDismiss = { showMoreOptions = false },
        )
    }

    if (showCollageDialog) {
        CollageStyleDialog(
            onLayoutSelected = { layout ->
                showCollageDialog = false
                selectedCollageLayout = layout
                collagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { showCollageDialog = false },
            accentColor = accentColor
        )
    }

    collageDraft?.let { draft ->
        CollageEditorScreen(
            layout = draft.layout,
            photos = draft.photos,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            isRendering = isRenderingCollage,
            onCancel = {
                recycleBitmaps(draft.photos)
                collageDraft = null
            },
            onConfirm = { transforms ->
                if (!isRenderingCollage) {
                    isRenderingCollage = true
                    val (outputWidth, outputHeight) = collageOutputSize(canvasWidth, canvasHeight)
                    scope.launch(Dispatchers.Default) {
                        var collageBitmap: Bitmap? = null
                        try {
                            collageBitmap = renderCollageBitmap(
                                photos = draft.photos,
                                layout = draft.layout,
                                width = outputWidth,
                                height = outputHeight,
                                transforms = transforms,
                            )
                        } catch (_: Exception) {
                            DiagnosticLog.error(DiagnosticEvent.PAINT_COLLAGE_RENDER_FAILED)
                        } finally {
                            recycleBitmaps(draft.photos)
                        }

                        withContext(Dispatchers.Main) {
                            isRenderingCollage = false
                            collageDraft = null
                            if (collageBitmap != null) {
                                viewModel.onAction(
                                    DrawingAction.OnImageSelected(
                                        bitmap = collageBitmap.asImageBitmap(),
                                        canvasWidth = canvasWidth,
                                        canvasHeight = canvasHeight,
                                    )
                                )
                            } else {
                                snackbarState.show(
                                    message = resources.getString(R.string.paint_collage_error),
                                    type = SnackbarType.Error,
                                )
                            }
                        }
                    }
                }
            },
            appSettingsViewModel = appSettingsViewModel,
        )
        return
    }

    val onControlsEvent: (PaintControlsEvent) -> Unit = { event ->
        when (event) {
            is PaintControlsEvent.SelectCategory -> {
                selectedToolCategory = event.category
                val inputMode = when (event.category) {
                    PaintToolCategory.PENCIL -> DrawingInputMode.PENCIL
                    PaintToolCategory.ERASER -> DrawingInputMode.ERASER
                    PaintToolCategory.BLUR -> DrawingInputMode.BLUR
                    PaintToolCategory.TEXT,
                    PaintToolCategory.SHAPES,
                    PaintToolCategory.CANVAS,
                    -> DrawingInputMode.NONE
                }
                viewModel.onAction(DrawingAction.OnSelectInputMode(inputMode))
                if (event.category == PaintToolCategory.TEXT) {
                    textToAdd = ""
                    textColor = state.selectedColor
                    textHasBackground = false
                    textBackgroundColor = Color.White
                    textSize = textSize.coerceIn(textSizeRange.start, textSizeRange.endInclusive)
                    showTextDialog = true
                }
            }
            is PaintControlsEvent.SelectBrushStyle -> viewModel.onAction(
                DrawingAction.OnSelectBrushStyle(event.style),
            )
            is PaintControlsEvent.SelectColor -> viewModel.onAction(
                DrawingAction.OnSelectColor(event.color),
            )
            is PaintControlsEvent.SelectBrushSize -> viewModel.onAction(
                DrawingAction.OnSelectBrushSize(event.size),
            )
            is PaintControlsEvent.SelectBlurBrushSize -> viewModel.onAction(
                DrawingAction.OnSelectBlurBrushSize(event.size),
            )
            is PaintControlsEvent.SelectBlurIntensity -> viewModel.onAction(
                DrawingAction.OnSelectBlurIntensity(event.intensity),
            )
            is PaintControlsEvent.SelectShape -> selectedShape = event.shape
            is PaintControlsEvent.ToggleShapeFill -> shapeFilled = event.filled
            PaintControlsEvent.AddShape -> viewModel.onAction(
                DrawingAction.OnPrepareShape(
                    type = selectedShape,
                    color = state.selectedColor,
                    brushSize = state.brushSize,
                    isFilled = shapeFilled,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                ),
            )
            is PaintControlsEvent.FillCanvas -> viewModel.onAction(
                DrawingAction.OnFillCanvas(event.color),
            )
            PaintControlsEvent.OpenCollage -> {
                if (canvasWidth == 0 || canvasHeight == 0) {
                    snackbarState.show(
                        message = resources.getString(R.string.paint_import_unavailable),
                        type = SnackbarType.Warning,
                    )
                } else {
                    showCollageDialog = true
                }
            }
            PaintControlsEvent.ClearCanvas -> viewModel.onAction(DrawingAction.OnClearCanvasClick)
        }
    }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.paint_title),
                onBackClick = handleBack,
                actions = {
                    IconAction(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.cd_undo),
                        onClick = { viewModel.onAction(DrawingAction.OnUndoClick) },
                        enabled = state.canUndo && !state.isBlurProcessing,
                    )
                    IconAction(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        onClick = { showMoreOptions = true },
                        enabled = !state.isBlurProcessing,
                    )
                },
                appSettingsViewModel = appSettingsViewModel
            )
        },
        snackbarHost = {
            AppSnackbarHost(state = snackbarState.hostState)
        },
        bottomBar = {
            TopControlsBar(
                state = PaintControlsState(
                    category = selectedToolCategory,
                    selectedColor = state.selectedColor,
                    brushSize = state.brushSize,
                    brushStyle = state.brushStyle,
                    blurBrushSize = state.blurBrushSize,
                    blurIntensity = state.blurIntensity,
                    selectedShape = selectedShape,
                    shapeFilled = shapeFilled,
                    accentColor = accentColor,
                    enabled = controlsEnabled,
                ),
                onEvent = onControlsEvent,
                appSettingsViewModel = appSettingsViewModel,
                backgroundColor = backgroundColor,
                initiallyExpanded = false,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        canvasWidth = size.width
                        canvasHeight = size.height
                    }
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInRoot()
                        canvasBounds = Rect(
                            bounds.left.roundToInt(),
                            bounds.top.roundToInt(),
                            bounds.right.roundToInt(),
                            bounds.bottom.roundToInt()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                DrawingCanvas(
                    state = state,
                    onAction = viewModel::onAction,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isImagePlacementMode && state.importedImage != null
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = surfaceColor.copy(alpha = 0.96f),
                            contentColor = onSurface,
                            tonalElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.paint_image_adjust_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.onAction(DrawingAction.OnCancelImagePlacement)
                                        },
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.8f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.paint_cancel_image)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = stringResource(R.string.cancel))
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.onAction(DrawingAction.OnConfirmImagePlacement)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentColor,
                                            contentColor = onAccent
                                        ),
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.paint_confirm_image)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = stringResource(R.string.action_continue))
                                    }
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.pendingShape != null
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = surfaceColor.copy(alpha = 0.96f),
                            contentColor = onSurface,
                            tonalElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.paint_shape_adjust_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.onAction(DrawingAction.OnCancelShapePlacement)
                                        },
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.8f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.paint_cancel_shape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = stringResource(R.string.cancel))
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.onAction(DrawingAction.OnConfirmShapePlacement)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentColor,
                                            contentColor = onAccent
                                        ),
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.paint_confirm_shape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = stringResource(R.string.action_continue))
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    val pendingTextState = state.pendingText
                    androidx.compose.animation.AnimatedVisibility(
                        visible = pendingTextState != null
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = surfaceColor.copy(alpha = 0.96f),
                            contentColor = onSurface
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.paint_text_adjust_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                pendingTextState?.let { pending ->
                                    Text(
                                        text = "\"${pending.text}\"",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = onSurface
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.onAction(DrawingAction.OnCancelTextPlacement)
                                        },
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.8f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.paint_cancel_text)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = stringResource(R.string.cancel))
                                    }
                                    Button(
                                        onClick = {
                                            val fallbackPosition = Offset(
                                                x = canvasWidth / 2f,
                                                y = canvasHeight / 2f
                                            )
                                            if (pendingTextState != null) {
                                                textSize = pendingTextState.size.coerceIn(
                                                    textSizeRange.start,
                                                    textSizeRange.endInclusive
                                                )
                                            }
                                            viewModel.onAction(
                                                DrawingAction.OnConfirmTextPlacement(
                                                    fallbackPosition = fallbackPosition
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentColor,
                                            contentColor = onAccent
                                        ),
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.paint_confirm_text)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = stringResource(R.string.action_continue))
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoadingImage || isLoadingCollage || state.isBlurProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }
            }

        }
    }

    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = {
                showTextDialog = false
                textToAdd = ""
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(24.dp),
            properties = DialogProperties(dismissOnClickOutside = true),
            title = { Text(text = stringResource(R.string.add_text)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = textToAdd,
                        onValueChange = { textToAdd = it },
                        label = { Text(stringResource(R.string.paint_add_text_label)) },
                        singleLine = false,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onBackground,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground,
                            cursorColor = accentColor,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            selectionColors = TextSelectionColors(
                                handleColor = accentColor,
                                backgroundColor = accentColor.copy(alpha = 0.4f)
                            )
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        minLines = 1,
                        maxLines = 6
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.paint_text_color),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(textColor, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                .clickable { showTextColorPicker = true }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.paint_text_background),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurface
                        )
                        Switch(
                            checked = textHasBackground,
                            onCheckedChange = { textHasBackground = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = surfaceColor,
                                checkedTrackColor = accentColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    if (textHasBackground) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.paint_text_background_color),
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(textBackgroundColor, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                    .clickable { showTextBgColorPicker = true }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sanitizedSize = textSize.coerceIn(
                            textSizeRange.start,
                            textSizeRange.endInclusive
                        )
                        viewModel.onAction(
                            DrawingAction.OnPrepareText(
                                text = textToAdd.trim(),
                                color = textColor,
                                size = sanitizedSize,
                                backgroundColor = if (textHasBackground) textBackgroundColor else null
                            )
                        )
                        showTextDialog = false
                        textToAdd = ""
                    },
                    enabled = textToAdd.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = surfaceColor
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(text = stringResource(R.string.apply), color = onSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTextDialog = false
                    textToAdd = ""
                }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = accentColor
                    )
                }
            }
        )
    }

    if (showTextColorPicker) {
        ColorPickerDialog(
            initialColor = textColor,
            onColorSelected = {
                textColor = it
                showTextColorPicker = false
            },
            onDismiss = { showTextColorPicker = false },
            appSettingsViewModel = appSettingsViewModel,
            title = stringResource(R.string.paint_text_color)
        )
    }

    if (showTextBgColorPicker) {
        ColorPickerDialog(
            initialColor = textBackgroundColor,
            onColorSelected = {
                textBackgroundColor = it
                showTextBgColorPicker = false
            },
            onDismiss = { showTextBgColorPicker = false },
            appSettingsViewModel = appSettingsViewModel,
            title = stringResource(R.string.paint_text_background_color)
        )
    }
}

private fun collageOutputSize(canvasWidth: Int, canvasHeight: Int): Pair<Int, Int> {
    val safeWidth = canvasWidth.coerceAtLeast(1)
    val safeHeight = canvasHeight.coerceAtLeast(1)
    val upscale = max(
        1f,
        max(
            1080f / safeWidth.toFloat(),
            1080f / safeHeight.toFloat(),
        ),
    )
    return (safeWidth * upscale).roundToInt() to (safeHeight * upscale).roundToInt()
}

private fun recycleBitmaps(bitmaps: List<Bitmap>) {
    bitmaps.forEach { bitmap ->
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}

private fun captureCanvasBitmap(view: View, bounds: Rect?): Bitmap? {
    if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) return null
    return try {
        val fullBitmap = view.drawToBitmap(Bitmap.Config.ARGB_8888)
        val left = bounds.left.coerceIn(0, fullBitmap.width - 1)
        val top = bounds.top.coerceIn(0, fullBitmap.height - 1)
        val right = bounds.right.coerceIn(left + 1, fullBitmap.width)
        val bottom = bounds.bottom.coerceIn(top + 1, fullBitmap.height)
        val cropped = Bitmap.createBitmap(fullBitmap, left, top, right - left, bottom - top)
        if (cropped != fullBitmap) {
            fullBitmap.recycle()
        }
        cropped
    } catch (_: Exception) {
        DiagnosticLog.error(DiagnosticEvent.PAINT_CAPTURE_FAILED)
        null
    }
}

private fun saveDrawingToGallery(
    context: Context,
    bitmap: Bitmap,
    snackbarState: AppSnackbarState
) {

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
    val filename = "Drawing_$timestamp.png"

    var fos: OutputStream? = null
    var success = false

    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        if (uri != null) {
            fos = context.contentResolver.openOutputStream(uri)
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                success = true
            }
        }
    } catch (_: Exception) {
        DiagnosticLog.error(DiagnosticEvent.PAINT_SAVE_FAILED)
        success = false
    } finally {
        fos?.close()
        bitmap.recycle()
    }

    val message = if (success) {
        context.getString(R.string.drawing_saved_success)
    } else {
        context.getString(R.string.drawing_saved_error)
    }
    snackbarState.show(
        message = message,
        type = if (success) SnackbarType.Success else SnackbarType.Error
    )
}

private fun shareDrawing(
    context: Context,
    bitmap: Bitmap,
    snackbarState: AppSnackbarState
) {
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs()

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
    val file = File(cachePath, "Drawing_$timestamp.png")

    try {
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
                context.getString(R.string.save_drawing)
            )
        )
    } catch (_: Exception) {
        DiagnosticLog.error(DiagnosticEvent.PAINT_SHARE_FAILED)
        snackbarState.show(
            message = context.getString(R.string.drawing_saved_error),
            type = SnackbarType.Error
        )
    } finally {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
