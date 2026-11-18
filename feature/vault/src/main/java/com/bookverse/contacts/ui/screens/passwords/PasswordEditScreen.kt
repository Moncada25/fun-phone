package com.bookverse.contacts.ui.screens.passwords

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.bookverse.contacts.feature.vault.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.data.password.PasswordCredential
import com.bookverse.contacts.domain.passwords.Totp
import com.bookverse.contacts.security.SensitiveClipboard
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SelectBottomSheet
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.PasswordsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordEditScreen(
    id: Long?,
    passwordsViewModel: PasswordsViewModel,
    appSettingsViewModel: AppSettingsViewModel,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
) {
    val draft = com.bookverse.contacts.ui.auth.encryptedDraft("password-draft-$id", appSettingsViewModel)
    val draftFields by draft.fields.collectAsStateWithLifecycle()
    com.bookverse.contacts.ui.auth.ReportDraftFailure(draft)
    var existing by remember(id) { mutableStateOf<PasswordCredential?>(null) }
    var loadComplete by remember(id) { mutableStateOf(id == null) }
    var name by draft.field("name", draftFields)
    var username by draft.field("username", draftFields)
    var password by draft.field("password", draftFields)
    var notes by draft.field("notes", draftFields)
    var url by draft.field("url", draftFields)
    var totpInput by draft.field("totp", draftFields)
    var showTotpScanner by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var nameTouched by rememberSaveable { mutableStateOf(false) }
    var usernameTouched by rememberSaveable { mutableStateOf(false) }
    var passwordTouched by rememberSaveable { mutableStateOf(false) }
    val folders by passwordsViewModel.folders.collectAsStateWithLifecycle()
    var selectedFolderId by rememberSaveable(id) { mutableStateOf<Long?>(null) }
    var showFolderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        existing = id?.let { passwordsViewModel.get(it) }
        loadComplete = true
    }
    LaunchedEffect(existing, loadComplete, draftFields != null) {
        if (loadComplete && draftFields != null) {
            draft.initialize(mapOf("name" to existing?.name.orEmpty(), "username" to existing?.username.orEmpty(), "password" to existing?.password.orEmpty(), "notes" to existing?.note.orEmpty(), "url" to existing?.url.orEmpty(), "totp" to existing?.totp.orEmpty()))
        }
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val strength = remember(password) { PasswordGenerator.strength(password) }
    val (strengthColor, strengthLabelRes) = remember(strength) {
        passwordStrengthInfo(strength)
    }
    val totpConfig = remember(totpInput) { Totp.parse(totpInput) }
    val totpInvalid = totpInput.isNotBlank() && totpConfig == null
    val reusedIds by passwordsViewModel.reusedPasswordIds.collectAsStateWithLifecycle()
    val passwordReused = remember(password, existing, reusedIds) {
        passwordsViewModel.isPasswordReused(password, existing?.id)
    }
    val readyToSave = name.isNotBlank() && username.isNotBlank() && password.isNotBlank() && !totpInvalid
    val context = LocalContext.current
    val snackHost = rememberAppSnackbarState()
    val passwordCopiedMessage = stringResource(R.string.password_copy_text)
    val totpCopiedMessage = stringResource(R.string.password_totp_copied)
    val totpScanInvalidMessage = stringResource(R.string.password_totp_scan_invalid)
    val totpScanSuccessMessage = stringResource(R.string.password_totp_scan_success)
    BackHandler { onBackClick() }

    fun copyPassword() {
        if (password.isBlank()) return
        SensitiveClipboard.copy(context, password)
        snackHost.show(passwordCopiedMessage, SnackbarType.Success)
    }

    fun copyTotpCode(code: String) {
        SensitiveClipboard.copy(context, code)
        snackHost.show(totpCopiedMessage, SnackbarType.Success)
    }

    fun savePassword() {
        val now = System.currentTimeMillis()
        passwordsViewModel.upsert(
            PasswordCredential(
                id = existing?.id ?: 0,
                name = name.trim(),
                username = username.trim(),
                password = password,
                note = notes.trim().takeIf { it.isNotBlank() },
                updatedAt = now,
                createdAt = existing?.createdAt ?: now,
                folderId = if (id == null) selectedFolderId else existing?.folderId,
                url = url.trim().takeIf { it.isNotBlank() },
                totp = totpConfig?.let(Totp::toUri),
            ),
        )
        draft.clear()
        onSaved()
    }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = stringResource(if (id == null) R.string.password_new else R.string.password_edit),
                onBackClick = onBackClick,
                actions = {
                    if (id == null && folders.isNotEmpty()) {
                        IconAction(
                            imageVector = Icons.Default.Folder,
                            contentDescription = stringResource(R.string.password_folder_action_description),
                            onClick = { showFolderDialog = true },
                        )
                    }
                },
            )
        },
        snackbarHost = { AppSnackbarHost(snackHost.hostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            if (!loadComplete) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.16f),
                )
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                ) {
                    val wideLayout = maxWidth >= 600.dp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 960.dp)
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        EditorIntroCard(accentColor)
                        if (wideLayout) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                AccountSection(
                                    name = name,
                                    onNameChange = { nameTouched = true; name = it },
                                    username = username,
                                    onUsernameChange = { usernameTouched = true; username = it },
                                    notes = notes,
                                    onNotesChange = { notes = it },
                                    url = url,
                                    onUrlChange = { url = it },
                                    nameError = nameTouched && name.isBlank(),
                                    usernameError = usernameTouched && username.isBlank(),
                                    accentColor = accentColor,
                                    modifier = Modifier.weight(1f),
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    SecuritySection(
                                        password = password,
                                        onPasswordChange = { passwordTouched = true; password = it },
                                        showPassword = showPassword,
                                        onToggleShow = { showPassword = !showPassword },
                                        onCopy = ::copyPassword,
                                        onGenerate = {
                                            passwordTouched = true
                                            password = PasswordGenerator.generate(PasswordPolicy())
                                        },
                                        passwordError = passwordTouched && password.isBlank(),
                                        strength = strength,
                                        strengthColor = strengthColor,
                                        strengthLabelRes = strengthLabelRes,
                                        reused = passwordReused,
                                        accentColor = accentColor,
                                    )
                                    TwoFactorSection(
                                        totpInput = totpInput,
                                        onTotpChange = { totpInput = it },
                                        config = totpConfig,
                                        onScan = { showTotpScanner = true },
                                        onCopyCode = ::copyTotpCode,
                                        accentColor = accentColor,
                                    )
                                }
                            }
                        } else {
                            AccountSection(
                                name = name,
                                onNameChange = { nameTouched = true; name = it },
                                username = username,
                                onUsernameChange = { usernameTouched = true; username = it },
                                notes = notes,
                                onNotesChange = { notes = it },
                                url = url,
                                onUrlChange = { url = it },
                                nameError = nameTouched && name.isBlank(),
                                usernameError = usernameTouched && username.isBlank(),
                                accentColor = accentColor,
                            )
                            SecuritySection(
                                password = password,
                                onPasswordChange = { passwordTouched = true; password = it },
                                showPassword = showPassword,
                                onToggleShow = { showPassword = !showPassword },
                                onCopy = ::copyPassword,
                                onGenerate = {
                                    passwordTouched = true
                                    password = PasswordGenerator.generate(PasswordPolicy())
                                },
                                passwordError = passwordTouched && password.isBlank(),
                                strength = strength,
                                strengthColor = strengthColor,
                                strengthLabelRes = strengthLabelRes,
                                reused = passwordReused,
                                accentColor = accentColor,
                            )
                            TwoFactorSection(
                                totpInput = totpInput,
                                onTotpChange = { totpInput = it },
                                config = totpConfig,
                                onScan = { showTotpScanner = true },
                                onCopyCode = ::copyTotpCode,
                                accentColor = accentColor,
                            )
                        }
                        Spacer(Modifier.height(128.dp))
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.ime)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(
                            onClick = ::savePassword,
                            enabled = readyToSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = contentColorFor(accentColor),
                            ),
                        ) {
                            Text(
                                stringResource(R.string.save_password),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTotpScanner) {
        TotpScannerDialog(
            accentColor = accentColor,
            onResult = { content ->
                showTotpScanner = false
                val scanned = Totp.parse(content)
                if (scanned == null) {
                    snackHost.show(totpScanInvalidMessage, SnackbarType.Error)
                } else {
                    totpInput = Totp.toUri(scanned)
                    snackHost.show(totpScanSuccessMessage, SnackbarType.Success)
                }
            },
            onDismiss = { showTotpScanner = false },
        )
    }

    if (showFolderDialog && id == null && folders.isNotEmpty()) {
        val folderOptions = listOf(
            PasswordFolderSelectionOption(
                id = null,
                label = stringResource(R.string.password_move_to_folder_none),
            ),
        ) + folders.map { folder ->
            PasswordFolderSelectionOption(folder.id, folder.name)
        }
        val selectedOption = folderOptions.firstOrNull { it.id == selectedFolderId } ?: folderOptions.first()
        SelectBottomSheet(
            onDismiss = { showFolderDialog = false },
            title = stringResource(R.string.password_folder_picker_title),
            options = folderOptions,
            selectedOption = selectedOption,
            onSelect = { option -> selectedFolderId = option.id },
            optionKey = { it.id ?: -1L },
            optionLabel = { it.label },
        )
    }
}

private data class PasswordFolderSelectionOption(val id: Long?, val label: String)

@Composable
private fun EditorIntroCard(accentColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(48.dp).background(accentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = accentColor)
            }
            Column {
                Text(
                    stringResource(R.string.security_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.password_editor_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun AccountSection(
    name: String,
    onNameChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    nameError: Boolean,
    usernameError: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    var showServicePicker by rememberSaveable { mutableStateOf(false) }
    if (showServicePicker) {
        PasswordServicePickerSheet(
            selectedUrl = url,
            onPick = { service ->
                onUrlChange(service.url)
                if (name.isBlank()) onNameChange(service.name)
                showServicePicker = false
            },
            onDismiss = { showServicePicker = false },
        )
    }
    EditorSectionCard(
        title = stringResource(R.string.basic_data_title),
        icon = Icons.Default.AccountCircle,
        accentColor = accentColor,
        modifier = modifier,
    ) {
        EditorField(name, onNameChange, stringResource(CoreUiR.string.account), Icons.Default.Badge, nameError, R.string.password_account_required, accentColor, ImeAction.Next)
        EditorField(username, onUsernameChange, stringResource(CoreUiR.string.password_username), Icons.Default.AccountCircle, usernameError, R.string.password_username_required, accentColor, ImeAction.Next)
        EditorField(
            value = url,
            onValueChange = onUrlChange,
            label = stringResource(R.string.password_website),
            icon = Icons.Default.Language,
            isError = false,
            errorRes = null,
            accentColor = accentColor,
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Uri,
            supportingRes = R.string.password_website_hint,
            trailingIcon = {
                IconButton(onClick = { showServicePicker = true }) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.password_service_picker_action))
                }
            },
        )
        EditorField(notes, onNotesChange, stringResource(R.string.password_notes), Icons.Default.Description, false, null, accentColor, ImeAction.Done, singleLine = false)
    }
}

@Composable
private fun SecuritySection(
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onToggleShow: () -> Unit,
    onCopy: () -> Unit,
    onGenerate: () -> Unit,
    passwordError: Boolean,
    strength: Int,
    strengthColor: Color,
    @androidx.annotation.StringRes strengthLabelRes: Int,
    reused: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    EditorSectionCard(
        title = stringResource(R.string.security_section_title),
        icon = Icons.Default.Security,
        accentColor = accentColor,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.password_password)) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                Row {
                    IconButton(
                        onClick = onCopy,
                        enabled = password.isNotBlank(),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.password_copy_action))
                    }
                    IconButton(onClick = onToggleShow, modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(
                                if (showPassword) R.string.toggle_hide else R.string.toggle_reveal,
                            ),
                        )
                    }
                }
            },
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation(PASSWORD_MASK_CHARACTER)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            isError = passwordError,
            supportingText = if (passwordError) {
                { Text(stringResource(R.string.password_value_required)) }
            } else {
                null
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = editorFieldColors(accentColor),
        )
        OutlinedButton(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, accentColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.password_generate_new),
                tint = accentColor,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.password_generate_action),
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
        StrengthBar(strength, strengthColor, strengthLabelRes, accentColor)
        if (reused) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    stringResource(R.string.password_reused_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun EditorSectionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = accentColor)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isError: Boolean,
    @androidx.annotation.StringRes errorRes: Int?,
    accentColor: Color,
    imeAction: ImeAction,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    @androidx.annotation.StringRes supportingRes: Int? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = if (isError && errorRes != null) {
            { Text(stringResource(errorRes)) }
        } else if (supportingRes != null) {
            { Text(stringResource(supportingRes)) }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = RoundedCornerShape(14.dp),
        colors = editorFieldColors(accentColor),
    )
}

@Composable
internal fun editorFieldColors(accentColor: Color) = TextFieldDefaults.colors(
    focusedIndicatorColor = accentColor,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
    focusedLabelColor = accentColor,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = accentColor,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
)

@Composable
private fun StrengthBar(
    strength: Int,
    color: Color,
    @androidx.annotation.StringRes labelRes: Int,
    accentColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.password_strength),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = { strength / 10f },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = color,
            trackColor = accentColor.copy(alpha = 0.15f),
        )
    }
}
