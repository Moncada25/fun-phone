package com.bookverse.contacts.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.repository.BlockedNumberInfo
import com.bookverse.contacts.domain.contacts.BlockedNumberInputPolicy
import com.bookverse.contacts.domain.contacts.BlockedNumberInputResult
import com.bookverse.contacts.feature.settings.R
import com.bookverse.contacts.core.ui.R as CoreUiR

@Composable
internal fun BlockedNumbersSheet(
    blockedNumbers: List<BlockedNumberInfo>,
    accentColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    onDismiss: () -> Unit,
    onUnblockContact: (Contact) -> Unit,
    onUnblockNumber: (String) -> Unit,
    onBlockNumber: (String) -> Unit,
) {
    var pendingBlockedNumber by remember { mutableStateOf<BlockedNumberInfo?>(null) }
    var isAddingNumber by rememberSaveable { mutableStateOf(false) }
    val pending = pendingBlockedNumber
    val sheetTitle = when {
        isAddingNumber -> stringResource(R.string.blocked_numbers_add_title)
        pending == null -> pluralStringResource(
            R.plurals.blocked_numbers_title_count,
            blockedNumbers.size,
            blockedNumbers.size,
        )
        else -> stringResource(R.string.blocked_numbers_unblock_confirmation_title)
    }

    SettingsSheetScaffold(
        title = sheetTitle,
        onDismiss = {
            when {
                isAddingNumber -> isAddingNumber = false
                pendingBlockedNumber != null -> pendingBlockedNumber = null
                else -> onDismiss()
            }
        },
    ) {
        if (isAddingNumber) {
            BlockedNumberAddForm(
                existingNumbers = blockedNumbers.flatMap { info ->
                    listOf(info.displayNumber, info.normalizedNumber)
                },
                accentColor = accentColor,
                secondaryTextColor = secondaryTextColor,
                onBlockNumber = { number ->
                    onBlockNumber(number)
                    isAddingNumber = false
                },
            )
        } else if (pending != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.blocked_numbers_unblock_confirmation_message,
                        pending.contact?.name ?: pending.displayNumber,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondaryTextColor,
                )
                androidx.compose.material3.Button(
                    onClick = {
                        pending.contact?.let(onUnblockContact)
                            ?: onUnblockNumber(
                                pending.normalizedNumber ?: pending.displayNumber,
                            )
                        pendingBlockedNumber = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = androidx.compose.material3.contentColorFor(accentColor),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(CoreUiR.string.contact_unblock_action))
                }
            }
        } else if (blockedNumbers.isEmpty()) {
            BlockedNumbersEmptyState(accentColor, textColor, secondaryTextColor)
            BlockedNumberAddButton(accentColor = accentColor, onClick = { isAddingNumber = true })
        } else {
            BlockedNumberAddButton(accentColor = accentColor, onClick = { isAddingNumber = true })
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(blockedNumbers, key = { it.id }) { info ->
                    BlockedNumberRow(
                        info = info,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        onUnblock = { pendingBlockedNumber = info },
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedNumberAddButton(
    accentColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(BLOCKED_NUMBER_ADD_BUTTON_TAG),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = contentColorFor(accentColor),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.blocked_numbers_add_action))
    }
}

@Composable
private fun BlockedNumberAddForm(
    existingNumbers: List<String?>,
    accentColor: Color,
    secondaryTextColor: Color,
    onBlockNumber: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val validation = remember(input, existingNumbers) {
        BlockedNumberInputPolicy.validate(input, existingNumbers)
    }
    val errorMessage = when (validation) {
        is BlockedNumberInputResult.Valid -> null
        BlockedNumberInputResult.Empty -> stringResource(R.string.blocked_numbers_add_error_empty)
        BlockedNumberInputResult.InvalidCharacters ->
            stringResource(R.string.blocked_numbers_add_error_invalid)
        BlockedNumberInputResult.TooShort -> stringResource(
            R.string.blocked_numbers_add_error_short,
            BlockedNumberInputPolicy.MIN_DIGITS,
        )
        BlockedNumberInputResult.TooLong -> stringResource(
            R.string.blocked_numbers_add_error_long,
            BlockedNumberInputPolicy.MAX_DIGITS,
        )
        BlockedNumberInputResult.Duplicate ->
            stringResource(R.string.blocked_numbers_add_error_duplicate)
    }
    val visibleError = errorMessage?.takeIf { showError }

    fun submit() {
        when (validation) {
            is BlockedNumberInputResult.Valid -> onBlockNumber(validation.number)
            else -> showError = true
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.blocked_numbers_add_description),
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor,
        )
        OutlinedTextField(
            value = input,
            onValueChange = { value ->
                input = BlockedNumberInputPolicy.limitInput(value)
                if (showError) showError = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(BLOCKED_NUMBER_INPUT_TAG),
            label = { Text(stringResource(R.string.blocked_numbers_add_label)) },
            singleLine = true,
            isError = visibleError != null,
            supportingText = visibleError?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            shape = RoundedCornerShape(16.dp),
        )
        Button(
            onClick = ::submit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(BLOCKED_NUMBER_CONFIRM_TAG),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = contentColorFor(accentColor),
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(imageVector = Icons.Default.Block, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.blocked_numbers_add_confirm))
        }
    }
}

internal const val BLOCKED_NUMBER_ADD_BUTTON_TAG = "blocked_number_add_button"
internal const val BLOCKED_NUMBER_INPUT_TAG = "blocked_number_input"
internal const val BLOCKED_NUMBER_CONFIRM_TAG = "blocked_number_confirm"

@Composable
private fun BlockedNumbersEmptyState(
    accentColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = stringResource(R.string.settings_blocked_empty_icon),
            tint = accentColor.copy(alpha = 0.8f),
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = stringResource(R.string.blocked_numbers_empty_title),
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.blocked_numbers_empty_subtitle),
            color = secondaryTextColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BlockedNumberRow(
    info: BlockedNumberInfo,
    textColor: Color,
    secondaryTextColor: Color,
    onUnblock: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = info.contact?.name
                        ?: stringResource(CoreUiR.string.blocked_numbers_unknown),
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = info.displayNumber,
                    color = secondaryTextColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(onClick = onUnblock) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = stringResource(R.string.settings_unblock_number_icon),
                    tint = textColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(CoreUiR.string.contact_unblock_action),
                    color = textColor,
                )
            }
        }
    }
}
