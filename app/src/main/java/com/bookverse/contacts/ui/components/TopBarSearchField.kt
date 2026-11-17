package com.bookverse.contacts.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.SearchX
import com.bookverse.contacts.ui.theme.LocalAppChromeStyle
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle

/**
 * Campo de búsqueda unificado para las barras superiores.
 *
 * Usa [LocalContentColor] para adaptarse al estilo de acento (minimal o seed)
 * de [MainTopBar] y [SecondaryTopBar]. El foco y el estado de búsqueda los
 * conserva la pantalla que lo aloja; este componente solo pinta y reporta.
 *
 * @param value texto actual.
 * @param onValueChange se invoca con cada cambio de texto.
 * @param placeholder texto visible cuando [value] está vacío.
 * @param focusRequester controla el foco del teclado.
 * @param onClose se invoca al pulsar limpiar con el campo ya vacío.
 * @param leadingIcon icono decorativo inicial.
 * @param clearContentDescription descripción localizada del botón limpiar.
 */
@Composable
fun TopBarSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    onClose: () -> Unit,
    clearContentDescription: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = Lucide.Search,
    leadingContentDescription: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        keyboardType = KeyboardType.Text,
    ),
) {
    val modern = LocalAppChromeStyle.current == AccentBarStyle.MODERN
    val contentColor = if (modern) MaterialTheme.colorScheme.onSurface else LocalContentColor.current
    val hintColor = if (modern) MaterialTheme.colorScheme.onSurfaceVariant else contentColor.copy(alpha = 0.6f)
    val fieldHeight = if (modern) Modifier.heightIn(min = 48.dp) else Modifier.height(48.dp)
    val fieldStyle = if (modern) MaterialTheme.typography.bodyLarge else LocalTextStyle.current.copy(fontSize = 17.sp)
    SearchFieldSurface(
        legacyColor = contentColor.copy(alpha = 0.15f),
        modifier = modifier
            .fillMaxWidth()
            .padding(end = if (modern) 0.dp else 16.dp)
            .then(fieldHeight),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = fieldStyle.copy(color = contentColor),
            cursorBrush = SolidColor(contentColor),
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .then(fieldHeight)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = if (modern) 16.dp else 12.dp, end = 4.dp),
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = leadingContentDescription,
                        tint = hintColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(if (modern) 12.dp else 8.dp))
                    Box(Modifier.weight(1f).padding(vertical = if (modern) 12.dp else 0.dp)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = hintColor,
                                style = fieldStyle,
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                    IconButton(onClick = {
                        if (value.isNotEmpty()) {
                            onValueChange("")
                        } else {
                            onClose()
                        }
                    }) {
                        Icon(
                            imageVector = Lucide.SearchX,
                            contentDescription = clearContentDescription,
                            tint = hintColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
        )
    }
}
