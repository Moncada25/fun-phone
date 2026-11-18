package com.bookverse.contacts.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.theme.chromePolicy
import com.bookverse.contacts.utils.ContactUtils
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.flow.distinctUntilChanged

internal enum class AvatarCategory(@param:StringRes val labelRes: Int) {
    People(R.string.avatar_category_people),
    Professions(R.string.avatar_category_professions),
    Interests(R.string.avatar_category_interests),
    Fantasy(R.string.avatar_category_fantasy)
}

internal data class AvatarOption(
    val id: String,
    @param:DrawableRes val drawableRes: Int,
    @param:StringRes val labelRes: Int,
    val category: AvatarCategory
)

internal val avatarOptions = listOf(
    AvatarOption(
        "person_blue",
        R.drawable.avatar_person_blue,
        R.string.avatar_label_person_blue,
        AvatarCategory.People
    ),
    AvatarOption(
        "person_green",
        R.drawable.avatar_person_green,
        R.string.avatar_label_person_green,
        AvatarCategory.People
    ),
    AvatarOption(
        "person_orange",
        R.drawable.avatar_person_orange,
        R.string.avatar_label_person_orange,
        AvatarCategory.People
    ),
    AvatarOption(
        "person_violet",
        R.drawable.avatar_person_violet,
        R.string.avatar_label_person_violet,
        AvatarCategory.People
    ),
    AvatarOption(
        "person_gray",
        R.drawable.avatar_person_gray,
        R.string.avatar_label_person_gray,
        AvatarCategory.People
    ),
    AvatarOption(
        "person_pink",
        R.drawable.avatar_person_pink,
        R.string.avatar_label_person_pink,
        AvatarCategory.People
    ),
    AvatarOption(
        "person_teal",
        R.drawable.avatar_person_teal,
        R.string.avatar_label_person_teal,
        AvatarCategory.People
    ),
    AvatarOption(
        "person_yellow",
        R.drawable.avatar_person_yellow,
        R.string.avatar_label_person_yellow,
        AvatarCategory.People
    ),
    AvatarOption(
        "person_red",
        R.drawable.avatar_person_red,
        R.string.avatar_label_person_red,
        AvatarCategory.People
    ),
    AvatarOption(
        "doctor",
        R.drawable.avatar_doctor,
        R.string.avatar_label_doctor,
        AvatarCategory.Professions
    ),
    AvatarOption(
        "teacher",
        R.drawable.avatar_teacher,
        R.string.avatar_label_teacher,
        AvatarCategory.Professions
    ),
    AvatarOption(
        "engineer",
        R.drawable.avatar_engineer,
        R.string.avatar_label_engineer,
        AvatarCategory.Professions
    ),
    AvatarOption(
        "designer",
        R.drawable.avatar_designer,
        R.string.avatar_label_designer,
        AvatarCategory.Professions
    ),
    AvatarOption(
        "business",
        R.drawable.avatar_business,
        R.string.avatar_label_business,
        AvatarCategory.Professions
    ),
    AvatarOption(
        "chef",
        R.drawable.avatar_chef,
        R.string.avatar_label_chef,
        AvatarCategory.Professions
    ),
    AvatarOption(
        "scientist",
        R.drawable.avatar_scientist,
        R.string.avatar_label_scientist,
        AvatarCategory.Professions
    ),
    AvatarOption(
        "photographer",
        R.drawable.avatar_photographer,
        R.string.avatar_label_photographer,
        AvatarCategory.Professions
    ),
    AvatarOption(
        "firefighter",
        R.drawable.avatar_firefighter,
        R.string.avatar_label_firefighter,
        AvatarCategory.Professions
    ),
    AvatarOption(
        "artist",
        R.drawable.avatar_artist,
        R.string.avatar_label_artist,
        AvatarCategory.Interests
    ),
    AvatarOption(
        "musician",
        R.drawable.avatar_musician,
        R.string.avatar_label_musician,
        AvatarCategory.Interests
    ),
    AvatarOption(
        "gamer",
        R.drawable.avatar_gamer,
        R.string.avatar_label_gamer,
        AvatarCategory.Interests
    ),
    AvatarOption(
        "reader",
        R.drawable.avatar_reader,
        R.string.avatar_label_reader,
        AvatarCategory.Interests
    ),
    AvatarOption(
        "pet_lover",
        R.drawable.avatar_pet_lover,
        R.string.avatar_label_pet_lover,
        AvatarCategory.Interests
    ),
    AvatarOption(
        "traveler",
        R.drawable.avatar_traveler,
        R.string.avatar_label_traveler,
        AvatarCategory.Interests
    ),
    AvatarOption(
        "gardener",
        R.drawable.avatar_gardener,
        R.string.avatar_label_gardener,
        AvatarCategory.Interests
    ),
    AvatarOption(
        "cyclist",
        R.drawable.avatar_cyclist,
        R.string.avatar_label_cyclist,
        AvatarCategory.Interests
    ),
    AvatarOption(
        "cinema",
        R.drawable.avatar_cinema,
        R.string.avatar_label_cinema,
        AvatarCategory.Interests
    ),
    AvatarOption(
        "robot",
        R.drawable.avatar_robot,
        R.string.avatar_label_robot,
        AvatarCategory.Fantasy
    ),
    AvatarOption(
        "astronaut",
        R.drawable.avatar_astronaut,
        R.string.avatar_label_astronaut,
        AvatarCategory.Fantasy
    ),
    AvatarOption(
        "cat",
        R.drawable.avatar_cat,
        R.string.avatar_label_cat,
        AvatarCategory.Fantasy
    ),
    AvatarOption(
        "superhero",
        R.drawable.avatar_superhero,
        R.string.avatar_label_superhero,
        AvatarCategory.Fantasy
    ),
    AvatarOption(
        "emoji",
        R.drawable.avatar_emoji,
        R.string.avatar_label_emoji,
        AvatarCategory.Fantasy
    ),
    AvatarOption(
        "alien",
        R.drawable.avatar_alien,
        R.string.avatar_label_alien,
        AvatarCategory.Fantasy
    ),
    AvatarOption(
        "wizard",
        R.drawable.avatar_wizard,
        R.string.avatar_label_wizard,
        AvatarCategory.Fantasy
    ),
    AvatarOption(
        "dragon",
        R.drawable.avatar_dragon,
        R.string.avatar_label_dragon,
        AvatarCategory.Fantasy
    ),
    AvatarOption(
        "fox",
        R.drawable.avatar_fox,
        R.string.avatar_label_fox,
        AvatarCategory.Fantasy
    )
)

internal const val AVATAR_GRID_COLUMNS = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSelector(
    key: String? = null,
    onDismiss: () -> Unit,
    onPhotoSelected: (Uri?) -> Unit,
    onDeletePhoto: (() -> Unit)? = null,
    hasExistingPhoto: Boolean = false,
    existingPhotoUri: Uri? = null,
    appSettingsViewModel: AppSettingsViewModel
) {
    val context = LocalContext.current
    val accentColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground

    val instanceKey = key ?: remember { "photo_selector_${'$'}{System.currentTimeMillis()}" }

    var tempCameraUri by remember(instanceKey) { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember(instanceKey) { mutableStateOf<File?>(null) }
    var tempCropFile by remember(instanceKey) { mutableStateOf<File?>(null) }
    var hasCameraPermission by remember(instanceKey) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var showCameraPermissionDialog by remember(instanceKey) { mutableStateOf(false) }
    var showAvatarPicker by remember(instanceKey) { mutableStateOf(false) }
    var selectedCategory by remember(instanceKey) { mutableStateOf(AvatarCategory.People) }
    var showAvatarErrorDialog by remember(instanceKey) { mutableStateOf(false) }
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    val isMinimal = accentBarStyle.chromePolicy.neutralControls
    val themeBackground = if (isMinimal) {
        MaterialTheme.colorScheme.background
    } else {
        accentColor
    }

    val closeSheet: () -> Unit = {
        showAvatarPicker = false
        selectedCategory = AvatarCategory.People
        onDismiss()
    }

    fun createCropDestination(prefix: String): Uri {
        tempCropFile?.delete()
        val file = File.createTempFile(prefix, ".png", context.cacheDir)
        tempCropFile = file
        return Uri.fromFile(file)
    }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val resultCode = result.resultCode

        if (resultCode == android.app.Activity.RESULT_OK) {
            val croppedUri = data?.let { UCrop.getOutput(it) }
            croppedUri?.let {
                tempCropFile = null
                onPhotoSelected(it)
            } ?: run {
                tempCropFile?.delete()
                tempCropFile = null
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            showAvatarErrorDialog = true
        }
        if (resultCode != android.app.Activity.RESULT_OK) {
            tempCropFile?.delete()
            tempCropFile = null
        }
        tempCameraFile?.delete()
        tempCameraFile = null
        tempCameraUri = null
        closeSheet()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val destUri = createCropDestination("crop_${instanceKey.hashCode()}_")
            val intent = UCrop.of(selectedUri, destUri)
                .withOptions(
                    getOptions(
                        toolbarColor = themeBackground.toArgb(),
                        accentColor = accentColor.toArgb(),
                        textColor = textColor.toArgb(),
                        context = context,
                        maxDimension = ContactUtils.contactPhotoCropMaxDimension(context),
                    )
                ).getIntent(context)
            cropLauncher.launch(intent)
        } ?: run {
            closeSheet()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            tempCameraUri?.let { capturedUri ->
                val destUri = createCropDestination("crop_${instanceKey.hashCode()}_")
                val intent = UCrop.of(capturedUri, destUri)
                    .withOptions(
                        getOptions(
                            toolbarColor = themeBackground.toArgb(),
                            accentColor = accentColor.toArgb(),
                            textColor = textColor.toArgb(),
                            context = context,
                            maxDimension = ContactUtils.contactPhotoCropMaxDimension(context),
                        )
                    ).getIntent(context)
                cropLauncher.launch(intent)
            }
        } else {
            tempCameraFile?.delete()
            tempCameraFile = null
            tempCameraUri = null
            closeSheet()
        }
    }

    fun launchCamera() {
        val photoFile = File.createTempFile(
            "contact_photo_${'$'}{instanceKey.hashCode()}_",
            ".jpg",
            context.cacheDir,
        )
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            photoFile,
        )
        tempCameraFile = photoFile
        tempCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            launchCamera()
        } else {
            showCameraPermissionDialog = true
        }
    }

    BackHandler(enabled = showAvatarPicker) {
        showAvatarPicker = false
    }

    ButtonSheetScaffold(
        title = stringResource(
            if (showAvatarPicker) R.string.avatar_selector_title else R.string.cd_contact_photo,
        ),
        onDismiss = closeSheet,
        navigationIcon = if (showAvatarPicker) {
            {
                IconButton(
                    onClick = { showAvatarPicker = false },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = accentColor,
                    )
                }
            }
        } else {
            null
        },
    ) {
        if (showAvatarPicker) {
            AvatarPickerContent(
                categories = AvatarCategory.entries,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                onAvatarSelected = { option ->
                    val avatarUri = saveAvatarToStorage(
                        context = context,
                        optionId = option.id,
                        drawableResId = option.drawableRes,
                    )
                    if (avatarUri != null) {
                        onPhotoSelected(avatarUri)
                        showAvatarPicker = false
                        closeSheet()
                    } else {
                        showAvatarErrorDialog = true
                    }
                },
                onBack = { showAvatarPicker = false },
                showHeader = false,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionRow(
                    option = OptionsDialogItem(
                        label = stringResource(R.string.choose_from_gallery),
                        icon = Icons.Default.PhotoLibrary,
                        onClick = { galleryLauncher.launch("image/*") },
                    ),
                    onDismiss = {},
                )
                OptionRow(
                    option = OptionsDialogItem(
                        label = stringResource(R.string.take_photo),
                        icon = Icons.Default.PhotoCamera,
                        onClick = {
                            if (hasCameraPermission) {
                                launchCamera()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    ),
                    onDismiss = {},
                )
                OptionRow(
                    option = OptionsDialogItem(
                        label = stringResource(R.string.use_avatar),
                        icon = Icons.Default.Face,
                        onClick = {
                            selectedCategory = AvatarCategory.People
                            showAvatarPicker = true
                        },
                    ),
                    onDismiss = {},
                )

                if (hasExistingPhoto && existingPhotoUri != null) {
                    OptionRow(
                        option = OptionsDialogItem(
                            label = stringResource(R.string.crop_image),
                            icon = Icons.Default.Crop,
                            onClick = {
                                val destUri = createCropDestination("crop_")
                                val intent = UCrop.of(existingPhotoUri, destUri)
                                    .withOptions(
                                        getOptions(
                                            toolbarColor = themeBackground.toArgb(),
                                            accentColor = accentColor.toArgb(),
                                            textColor = textColor.toArgb(),
                                            context = context,
                                            maxDimension = ContactUtils.contactPhotoCropMaxDimension(context),
                                        )
                                    )
                                    .getIntent(context)
                                cropLauncher.launch(intent)
                            },
                        ),
                        onDismiss = {},
                    )
                }

                if (hasExistingPhoto && onDeletePhoto != null) {
                    OptionRow(
                        option = OptionsDialogItem(
                            label = stringResource(R.string.delete_photo),
                            icon = Icons.Default.DeleteOutline,
                            destructive = true,
                            onClick = {
                                onDeletePhoto()
                                closeSheet()
                            },
                        ),
                        onDismiss = {},
                    )
                }
            }
        }
    }

    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            title = { Text(stringResource(R.string.permission_required)) },
            text = { Text(stringResource(R.string.contact_photo_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    showCameraPermissionDialog = false
                }) {
                    Text(stringResource(R.string.grant_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAvatarErrorDialog) {
        MessageDialog(
            title = stringResource(R.string.avatar_generation_error_title),
            message1 = stringResource(R.string.avatar_generation_error_message),
            onButtonClick = { showAvatarErrorDialog = false },
            onDismiss = { showAvatarErrorDialog = false },
        )
    }
}

@Composable
internal fun AvatarPickerContent(
    categories: List<AvatarCategory>,
    selectedCategory: AvatarCategory,
    onCategorySelected: (AvatarCategory) -> Unit,
    onAvatarSelected: (AvatarOption) -> Unit,
    onBack: () -> Unit,
    showHeader: Boolean = true,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val selectedPage = categories.indexOf(selectedCategory).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedPage) { categories.size }
    val currentSelectedCategory by rememberUpdatedState(selectedCategory)

    LaunchedEffect(selectedPage) {
        if (pagerState.settledPage != selectedPage) {
            pagerState.animateScrollToPage(selectedPage)
        }
    }
    LaunchedEffect(pagerState, categories) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                categories.getOrNull(page)?.let { category ->
                    if (category != currentSelectedCategory) {
                        onCategorySelected(category)
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = accentColor,
                    )
                }
                Text(
                    text = stringResource(R.string.avatar_selector_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            lazyItems(categories, key = { it.name }) { category ->
                val selected = category == selectedCategory
                FilterChip(
                    selected = selected,
                    onClick = { onCategorySelected(category) },
                    label = { Text(text = stringResource(category.labelRes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) {
                            accentColor
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AVATAR_CATEGORY_PAGER_TAG),
            verticalAlignment = Alignment.Top,
        ) { page ->
            val pageOptions = avatarOptions.filter { it.category == categories[page] }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pageOptions.chunked(AVATAR_GRID_COLUMNS).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowOptions.forEach { option ->
                            AvatarCard(
                                option = option,
                                onClick = onAvatarSelected,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(AVATAR_GRID_COLUMNS - rowOptions.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

internal const val AVATAR_CATEGORY_PAGER_TAG = "avatar-category-pager"

@Composable
internal fun AvatarCard(
    option: AvatarOption,
    onClick: (AvatarOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Surface(
        onClick = { onClick(option) },
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Image(
                painter = painterResource(id = option.drawableRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(option.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

private fun saveAvatarToStorage(
    context: Context,
    optionId: String,
    @DrawableRes drawableResId: Int,
): Uri? {
    return try {
        val drawable = AppCompatResources.getDrawable(context, drawableResId) ?: return null
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 512
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 512
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        val avatarsDir = File(context.filesDir, "avatars").apply {
            if (!exists()) mkdirs()
        }
        val fileName = avatarFileName(optionId)
        val avatarFile = File(avatarsDir, fileName)
        FileOutputStream(avatarFile).use { stream ->
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            bitmap.compress(format, 100, stream)
            stream.flush()
        }
        avatarFile.toUri()
    } catch (_: Exception) {
        null
    }
}

internal fun avatarFileName(
    optionId: String,
    uniqueSuffix: String = UUID.randomUUID().toString(),
): String {
    val safeId = optionId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        .ifBlank { "custom" }
    return "avatar_${safeId}_${uniqueSuffix}.webp"
}

private fun getOptions(
    toolbarColor: Int,
    accentColor: Int,
    textColor: Int,
    context: Context,
    maxDimension: Int,
): UCrop.Options {
    return UCrop.Options().apply {
        setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL)
        setToolbarTitle(context.getString(R.string.crop_image))
        setToolbarColor(toolbarColor)
        setStatusBarLight(true)
        setToolbarWidgetColor(textColor)
        setActiveControlsWidgetColor(accentColor)
        setCompressionFormat(Bitmap.CompressFormat.PNG)
        setCompressionQuality(100)
        setMaxBitmapSize(maxDimension)
        withMaxResultSize(maxDimension, maxDimension)
    }
}
