package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.bookverse.contacts.core.ui.R

@Composable
fun PinInputDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    enabled: Boolean = true,
) {
    val accent = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    var pin by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true, securePolicy = SecureFlagPolicy.SecureOn)
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { idx ->
                        val filled = idx < pin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (filled) accent else accent.copy(alpha = 0.2f))
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "<")
                    )
                    rows.forEach { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            r.forEach { key ->
                                when (key) {
                                    "" -> Box(modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)) {}
                                    "<" -> Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(enabled = enabled, onClick = {
                                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        }) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = stringResource(R.string.clear),
                                                tint = textColor
                                            )
                                        }
                                    }

                                    else -> Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(accent.copy(alpha = 0.15f))
                                            .clickable(enabled = enabled) { if (pin.length < 4) pin += key },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            key,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.2f))
                    ) { Text(stringResource(R.string.cancel), color = textColor) }
                    Button(
                        onClick = { if (pin.length == 4) { onSubmit(pin); pin = "" } },
                        enabled = enabled && pin.length == 4,
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) { Text(stringResource(R.string.confirm), color = textColor) }
                }
            }
        }
    }
}
