package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.ContactTag
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tags

@Composable
fun TagAssignmentDialog(
    allTags: List<ContactTag>,
    assignedTagIds: Set<Long>,
    onApply: (Set<Long>) -> Unit,
    onCreateTag: (name: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    var localSelectedIds by remember { mutableStateOf(assignedTagIds) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredTags by remember(allTags, searchQuery) {
        derivedStateOf {
            if (searchQuery.isEmpty()) allTags
            else allTags.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.tags_selected_count,
                            localSelectedIds.size,
                            localSelectedIds.size,
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = stringResource(R.string.cancel),
                            tint = textColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SearchFieldSurface(
                    legacyColor = textColor.copy(alpha = 0.2f),
                    allowBackdrop = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = textColor,
                            fontSize = 16.sp,
                        ),
                        cursorBrush = SolidColor(accentColor),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                Icon(
                                    imageVector = Lucide.Tags,
                                    contentDescription = stringResource(R.string.search_tags),
                                    tint = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            stringResource(R.string.search_tags),
                                            color = textColor.copy(alpha = 0.6f),
                                            fontSize = 16.sp,
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (allTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_tags_available),
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else if (filteredTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_tags_found),
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(
                            items = filteredTags,
                            key = { it.id },
                        ) { tag ->
                            TagAssignmentItem(
                                tag = tag,
                                isSelected = localSelectedIds.contains(tag.id),
                                onToggle = {
                                    localSelectedIds = if (tag.id in localSelectedIds) {
                                        localSelectedIds - tag.id
                                    } else {
                                        localSelectedIds + tag.id
                                    }
                                },
                                accentColor = accentColor,
                                textColor = textColor,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ButtonClick(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.action_create_short),
                        fontSize = 18,
                    )

                    Button(
                        onClick = {
                            onApply(localSelectedIds)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = textColor,
                        ),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.apply),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        TagFormDialog(
            tag = null,
            appSettingsViewModel = appSettingsViewModel,
            onDismiss = { showCreateDialog = false },
            onSave = { name, colorHex ->
                onCreateTag(name, colorHex)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun TagAssignmentItem(
    tag: ContactTag,
    isSelected: Boolean,
    onToggle: () -> Unit,
    accentColor: Color,
    textColor: Color,
) {
    val tagColor = try {
        Color(tag.colorHex.toColorInt())
    } catch (_: IllegalArgumentException) {
        Color(0xFF2196F3)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tagColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.Tags,
                    contentDescription = null,
                    tint = tagColor,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = tag.name,
                fontWeight = FontWeight.Medium,
                color = textColor,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) accentColor else textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
