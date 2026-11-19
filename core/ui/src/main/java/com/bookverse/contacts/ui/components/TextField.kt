package com.bookverse.contacts.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.settings.Country

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
    singleLine: Boolean = true,
    isError: Boolean = false,
    showCountryFlag: Boolean = false,
    selectedCountry: Country? = null,
    onCountrySelected: ((Country?) -> Unit)? = null,
    leadingIcon : @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    var showCountryDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        isError = isError,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = if (showCountryFlag) {
            {
                Row(
                    modifier = Modifier
                        .clickable {
                            if (onCountrySelected != null) showCountryDialog = true
                        }
                        .padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedCountry?.flag ?: "🌍",
                        fontSize = 20.sp
                    )
                    if (selectedCountry != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedCountry.code,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        } else leadingIcon,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = KeyboardCapitalization.Sentences
        ),
        keyboardActions = if (onDone != null) {
            KeyboardActions(
                onDone = { if (value.isNotEmpty()) onDone.invoke() }
            )
        } else {
            KeyboardActions.Default
        },
        singleLine = singleLine,
        modifier = modifier,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = accentColor,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = accentColor,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = accentColor,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            selectionColors = TextSelectionColors(
                handleColor = accentColor,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            )
        )
    )

    if (showCountryDialog && onCountrySelected != null) {
        CountrySelectDialog(
            onDismiss = { showCountryDialog = false },
            onCountrySelected = { country ->
                onCountrySelected(country)
                showCountryDialog = false
            },
            showCountryCode = true,
            selectedCountry = selectedCountry
        )
    }
}
