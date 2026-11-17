package com.bookverse.contacts.security.autofill

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.Dataset
import android.view.WindowManager
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.App
import com.bookverse.contacts.R
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.password.PasswordCredential
import com.bookverse.contacts.data.password.PasswordRepository
import com.bookverse.contacts.data.password.PasswordStore
import com.bookverse.contacts.domain.passwords.CredentialMatchPolicy
import com.bookverse.contacts.domain.passwords.CredentialTarget
import com.bookverse.contacts.domain.passwords.KnownServices
import com.bookverse.contacts.security.CryptoManager
import com.bookverse.contacts.settings.Fonts
import com.bookverse.contacts.ui.activities.LocalizedAppCompatActivity
import com.bookverse.contacts.ui.auth.PasswordAuthDialogs
import com.bookverse.contacts.ui.auth.rememberPasswordAuthState
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VaultAutofillActivity : LocalizedAppCompatActivity() {
    private val appContainer by lazy { (application as App).container }
    private val settings: AppSettingsViewModel by viewModels {
        appContainer.appSettingsViewModelFactory
    }
    private val store: PasswordStore by lazy {
        PasswordRepository(AppDatabase.getInstance(applicationContext).passwordDao(), applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val request = readRequest()
        if (request == null) {
            cancel()
            return
        }
        setContent {
            var ready by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                settings.preloadSettings(this@VaultAutofillActivity)
                ready = true
            }
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            val darkModeStyle by settings.darkModeStyle.collectAsStateWithLifecycle()
            val accentSeed by settings.accentColor.collectAsStateWithLifecycle()
            val accentBarStyle by settings.accentBarStyle.collectAsStateWithLifecycle()
            val fontType by settings.fontType.collectAsStateWithLifecycle()
            val fontFamily = remember(fontType) { Fonts.getFontFamily(fontType) }
            val fontScale by settings.fontScale.collectAsStateWithLifecycle()
            val glassEffects by settings.glassEffects.collectAsStateWithLifecycle()
            ContactsAppTheme(
                accentSeed = accentSeed,
                fontFamily = fontFamily,
                fontScale = fontScale,
                themeMode = themeMode,
                darkModeStyle = darkModeStyle,
                accentBarStyle = accentBarStyle,
                glassEffects = glassEffects,
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (ready) {
                        VaultAutofillContent(
                            request = request,
                            settings = settings,
                            store = store,
                            onFill = ::deliver,
                            onSaved = ::finishSaved,
                            onCancel = ::cancel,
                        )
                    }
                }
            }
        }
    }

    private fun readRequest(): VaultAutofillRequest? {
        if (intent.getStringExtra(EXTRA_MODE) == MODE_SAVE) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return null
            val passwordEnc = intent.getByteArrayExtra(EXTRA_PASSWORD) ?: return null
            return VaultAutofillRequest.Save(
                target = CredentialTarget(packageName, intent.getStringExtra(EXTRA_DOMAIN)),
                usernameEnc = intent.getByteArrayExtra(EXTRA_USERNAME),
                passwordEnc = passwordEnc,
            )
        }
        val structure = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE, AssistStructure::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE)
        }
        val form = structure?.let(AutofillStructureParser::parse) ?: return null
        if (form.usernameId == null && form.passwordId == null) return null
        return VaultAutofillRequest.Fill(form)
    }

    @Suppress("DEPRECATION")
    private fun deliver(form: ParsedLoginForm, credential: PasswordCredential) {
        val presentation = VaultAutofillService.presentation(packageName, credential.name)
        val dataset = Dataset.Builder(presentation)
        form.usernameId?.let {
            dataset.setValue(it, AutofillValue.forText(credential.username), presentation)
        }
        form.passwordId?.let {
            dataset.setValue(it, AutofillValue.forText(credential.password), presentation)
        }
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset.build()),
        )
        finish()
    }

    private fun finishSaved() {
        Toast.makeText(this, R.string.autofill_save_done, Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        private const val EXTRA_MODE = "vault_autofill_mode"
        private const val EXTRA_PACKAGE = "vault_autofill_package"
        private const val EXTRA_DOMAIN = "vault_autofill_domain"
        private const val EXTRA_USERNAME = "vault_autofill_username"
        private const val EXTRA_PASSWORD = "vault_autofill_password"
        private const val MODE_SAVE = "save"

        internal fun fillIntent(context: Context): Intent =
            Intent(context, VaultAutofillActivity::class.java)

        internal fun saveIntent(
            context: Context,
            target: CredentialTarget,
            usernameEnc: ByteArray?,
            passwordEnc: ByteArray?,
        ): Intent = Intent(context, VaultAutofillActivity::class.java)
            .putExtra(EXTRA_MODE, MODE_SAVE)
            .putExtra(EXTRA_PACKAGE, target.packageName)
            .putExtra(EXTRA_DOMAIN, target.webDomain)
            .putExtra(EXTRA_USERNAME, usernameEnc)
            .putExtra(EXTRA_PASSWORD, passwordEnc)
    }
}

internal sealed interface VaultAutofillRequest {
    val target: CredentialTarget

    class Fill(val form: ParsedLoginForm) : VaultAutofillRequest {
        override val target: CredentialTarget get() = form.target
    }

    class Save(
        override val target: CredentialTarget,
        val usernameEnc: ByteArray?,
        val passwordEnc: ByteArray,
    ) : VaultAutofillRequest
}

private fun targetLabel(target: CredentialTarget): String =
    CredentialMatchPolicy.hostOf(target.webDomain) ?: target.packageName

@Composable
private fun VaultAutofillContent(
    request: VaultAutofillRequest,
    settings: AppSettingsViewModel,
    store: PasswordStore,
    onFill: (ParsedLoginForm, PasswordCredential) -> Unit,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val authState = rememberPasswordAuthState(settings)
    val unlocked by settings.protectedSessionUnlocked.collectAsStateWithLifecycle()
    val enabled by settings.passwordManagerEnabled.collectAsStateWithLifecycle()
    val authMethod by settings.passwordAuthMethod.collectAsStateWithLifecycle()
    var credentials by remember { mutableStateOf<List<PasswordCredential>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(enabled, authMethod) {
        if (!enabled || authMethod == null) {
            onCancel()
        } else if (!settings.protectedSessionUnlocked.value) {
            authState.handleProtectedAction(enabled, authMethod) {}
        }
    }
    LaunchedEffect(unlocked) {
        if (!unlocked) {
            credentials = null
            return@LaunchedEffect
        }
        try {
            val all = store.getAllForBackup()
            val matches = CredentialMatchPolicy.matching(all, request.target)
            val single = matches.singleOrNull()
            if (request is VaultAutofillRequest.Fill && single != null && CredentialMatchPolicy.matchesExactly(single.url, request.target)) {
                onFill(request.form, single)
            } else {
                credentials = all
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    PasswordAuthDialogs(authState)

    val loaded = credentials
    when {
        loadFailed -> CenteredMessage(stringResource(R.string.vault_load_failed), onCancel = onCancel)
        !unlocked -> CenteredMessage(
            message = stringResource(R.string.auth_required),
            actionLabel = stringResource(R.string.autofill_unlock_prompt),
            onAction = { authState.handleProtectedAction(enabled, authMethod) {} },
            onCancel = onCancel,
        )
        loaded == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        request is VaultAutofillRequest.Fill -> CredentialPicker(
            target = request.target,
            credentials = loaded,
            onPick = { onFill(request.form, it) },
            onCancel = onCancel,
        )
        request is VaultAutofillRequest.Save -> SaveCredentialForm(
            request = request,
            credentials = loaded,
            store = store,
            onSaved = onSaved,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    onCancel: () -> Unit,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun CredentialPicker(
    target: CredentialTarget,
    credentials: List<PasswordCredential>,
    onPick: (PasswordCredential) -> Unit,
    onCancel: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val label = targetLabel(target)
    val matches = remember(credentials, target) { CredentialMatchPolicy.matching(credentials, target) }
    val others = remember(credentials, matches, query) {
        val matchIds = matches.map { it.id }.toSet()
        credentials.filter { credential ->
            credential.id !in matchIds &&
                (query.isBlank() ||
                    credential.name.contains(query, ignoreCase = true) ||
                    credential.username.contains(query, ignoreCase = true))
        }
    }
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 16.dp)) {
        Text(
            stringResource(R.string.autofill_picker_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            stringResource(R.string.autofill_picker_target, label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (credentials.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.autofill_picker_empty))
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                label = { Text(stringResource(R.string.search_password)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (matches.isNotEmpty()) {
                    item(key = "matches-header") {
                        SectionLabel(stringResource(R.string.autofill_picker_matches))
                    }
                    items(matches, key = { "match-${it.id}" }) { credential ->
                        CredentialRow(credential, onClick = { onPick(credential) })
                    }
                }
                if (others.isNotEmpty()) {
                    item(key = "others-header") {
                        SectionLabel(stringResource(R.string.autofill_picker_others))
                        Text(
                            stringResource(R.string.autofill_picker_other_warning, label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    items(others, key = { "other-${it.id}" }) { credential ->
                        CredentialRow(credential, onClick = { onPick(credential) })
                    }
                }
            }
        }
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun CredentialRow(credential: PasswordCredential, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    credential.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    credential.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SaveCredentialForm(
    request: VaultAutofillRequest.Save,
    credentials: List<PasswordCredential>,
    store: PasswordStore,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val label = targetLabel(request.target)
    var name by remember { mutableStateOf(KnownServices.forTarget(request.target)?.name ?: label) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(request) {
        try {
            withContext(Dispatchers.IO) {
                username = CryptoManager.decrypt(context, request.usernameEnc)
                    ?.toString(Charsets.UTF_8)
                    .orEmpty()
                password = CryptoManager.decrypt(context, request.passwordEnc)?.toString(Charsets.UTF_8)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            failed = true
        }
    }

    val existing = remember(credentials, username, request) {
        CredentialMatchPolicy.matching(credentials, request.target)
            .firstOrNull { it.username == username.trim() }
    }
    val currentPassword = password
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(
                if (existing != null) R.string.autofill_save_update_title else R.string.autofill_save_title,
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (failed) {
            Text(stringResource(R.string.autofill_save_failed), color = MaterialTheme.colorScheme.error)
        }
        if (existing == null) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.account)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        }
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.password_username)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            Button(
                enabled = !saving && currentPassword != null &&
                    username.isNotBlank() && (existing != null || name.isNotBlank()),
                onClick = {
                    val secret = currentPassword ?: return@Button
                    saving = true
                    scope.launch {
                        try {
                            val now = System.currentTimeMillis()
                            store.upsert(
                                existing?.copy(password = secret) ?: PasswordCredential(
                                    name = name.trim(),
                                    username = username.trim(),
                                    password = secret,
                                    note = null,
                                    updatedAt = now,
                                    createdAt = now,
                                    url = CredentialMatchPolicy.targetUrl(request.target),
                                ),
                            )
                            onSaved()
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            saving = false
                            failed = true
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
