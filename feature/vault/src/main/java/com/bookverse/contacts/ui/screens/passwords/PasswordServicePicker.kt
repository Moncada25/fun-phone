package com.bookverse.contacts.ui.screens.passwords

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.domain.passwords.CredentialMatchPolicy
import com.bookverse.contacts.domain.passwords.KnownService
import com.bookverse.contacts.domain.passwords.KnownServiceCategory
import com.bookverse.contacts.domain.passwords.KnownServices
import com.bookverse.contacts.feature.vault.R
import com.bookverse.contacts.ui.components.ButtonSheetScaffold
import com.bookverse.contacts.core.ui.R as CoreUiR

@Composable
internal fun PasswordServicePickerSheet(
    selectedUrl: String,
    onPick: (KnownService) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { KnownServices.search(query) }
    val grouped = remember(results) { results.groupBy(KnownService::category) }
    val selectedHost = remember(selectedUrl) {
        CredentialMatchPolicy.hostOf(selectedUrl)
    }
    ButtonSheetScaffold(
        title = stringResource(R.string.password_service_picker_title),
        subtitle = stringResource(R.string.password_service_picker_subtitle),
        onDismiss = onDismiss,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.password_service_search)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(CoreUiR.string.clear))
                    }
                }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(14.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (results.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.password_service_empty),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            KnownServiceCategory.entries.forEach { category ->
                val services = grouped[category].orEmpty()
                if (services.isNotEmpty()) {
                    item(key = "header-${category.name}") {
                        Text(
                            text = stringResource(categoryLabel(category)),
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(services, key = KnownService::id) { service ->
                        ServiceRow(
                            service = service,
                            selected = selectedHost != null && selectedHost == service.host,
                            onClick = { onPick(service) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(service: KnownService, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = service.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = service.host,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@StringRes
private fun categoryLabel(category: KnownServiceCategory): Int = when (category) {
    KnownServiceCategory.STREAMING -> R.string.password_service_category_streaming
    KnownServiceCategory.EMAIL_SOCIAL -> R.string.password_service_category_social
    KnownServiceCategory.SHOPPING -> R.string.password_service_category_shopping
    KnownServiceCategory.GAMING_DEV -> R.string.password_service_category_more
}
