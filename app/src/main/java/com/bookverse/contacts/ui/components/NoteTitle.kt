package com.bookverse.contacts.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.R

@Composable
fun NoteTitle(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: ((String) -> Unit)? = null,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    accentColor: Color,
    editable: Boolean = true
) {
    OutlinedTextField(
        value = value,
        enabled = editable,
        onValueChange = onValueChange ?: {},
        modifier = modifier.fillMaxWidth(0.9f),
        textStyle = TextStyle(
            fontSize = 22.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontFamily = MaterialTheme.typography.headlineLarge.fontFamily
        ),
        placeholder = {
            Text(
                stringResource(R.string.title),
                fontSize = 22.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = Color.Transparent,
            cursorColor = accentColor,
            focusedContainerColor = accentColor.copy(alpha = 0.2f),
            unfocusedContainerColor = accentColor.copy(alpha = 0.2f),
            disabledContainerColor = accentColor.copy(alpha = 0.2f),
            selectionColors = TextSelectionColors(
                handleColor = accentColor,
                backgroundColor = accentColor.copy(alpha = 0.4f)
            )
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        )
    )
}
