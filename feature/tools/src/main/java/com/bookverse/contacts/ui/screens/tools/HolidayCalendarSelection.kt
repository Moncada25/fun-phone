package com.bookverse.contacts.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.domain.holidays.CalendarSource
import com.bookverse.contacts.feature.tools.R
import com.bookverse.contacts.core.ui.R as CoreUiR

@Composable
internal fun CalendarPermissionDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    accentColor: Color,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.holidays_calendar_permission_title)) },
        text = { Text(stringResource(R.string.holidays_calendar_permission_message)) },
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            ) {
                Text(stringResource(CoreUiR.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_not_now))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarSelectorSheet(
    calendars: List<CalendarSource>,
    selectedIds: Set<Long>,
    isLoading: Boolean,
    accentColor: Color,
    onApply: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val uniqueCalendars = remember(calendars) {
        calendars.distinctBy {
            it.visibleIdentity()
        }
    }
    val canonicalSelectedIds = remember(calendars, uniqueCalendars, selectedIds) {
        selectedIds.mapNotNull { selectedId ->
            calendars.firstOrNull { it.id == selectedId }?.let { selectedCalendar ->
                uniqueCalendars.firstOrNull {
                    it.visibleIdentity() == selectedCalendar.visibleIdentity()
                }?.id
            }
        }.toSet()
    }
    var temporarySelection by remember(uniqueCalendars, canonicalSelectedIds) {
        mutableStateOf(canonicalSelectedIds)
    }
    val groupedCalendars = remember(uniqueCalendars) {
        uniqueCalendars.groupBy { it.accountName.ifBlank { it.accountType } }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.32f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface,
                        RoundedCornerShape(2.dp),
                    ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.holidays_select_calendars),
                style = MaterialTheme.typography.headlineSmall,
            )
            when {
                isLoading -> Row(
                    modifier = Modifier.padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = accentColor,
                        strokeWidth = 2.dp,
                    )
                    Text(stringResource(R.string.holidays_loading_calendars))
                }

                uniqueCalendars.isEmpty() -> Text(
                    text = stringResource(R.string.holidays_no_calendars),
                    modifier = Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    groupedCalendars.forEach { (account, accountCalendars) ->
                        item(key = "account_$account") {
                            Text(
                                text = account.ifBlank { stringResource(CoreUiR.string.local_account) },
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        items(accountCalendars, key = CalendarSource::id) { calendar ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        temporarySelection = temporarySelection.toggle(calendar.id)
                                    }
                                    .semantics { role = Role.Checkbox }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = calendar.id in temporarySelection,
                                    onCheckedChange = {
                                        temporarySelection = temporarySelection.toggle(calendar.id)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurface,
                                        checkmarkColor = MaterialTheme.colorScheme.onBackground,
                                        disabledCheckedColor = accentColor.copy(alpha = 0.38f),
                                        disabledUncheckedColor = MaterialTheme.colorScheme.onSurface
                                            .copy(alpha = 0.38f),
                                        disabledIndeterminateColor = accentColor.copy(alpha = 0.38f),
                                    ),
                                )
                                Text(text = calendar.name, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(stringResource(CoreUiR.string.cancel))
                }
                Button(
                    onClick = {
                        onApply(temporarySelection)
                        onDismiss()
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    ),
                ) {
                    Text(stringResource(CoreUiR.string.apply))
                }
            }
        }
    }
}

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id

private fun CalendarSource.visibleIdentity(): Triple<String, String, String> =
    Triple(name.trim(), accountName.trim(), accountType.trim())
