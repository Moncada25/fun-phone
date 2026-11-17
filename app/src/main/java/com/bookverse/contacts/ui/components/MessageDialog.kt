package com.bookverse.contacts.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.R

@Composable
fun MessageDialog(
    title: String,
    message1: String = "",
    message2: String = "",
    buttonText: String = stringResource(R.string.understood),
    onButtonClick: () -> Unit = {},
    secondaryButtonText: String? = null,
    onSecondaryButtonClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                if (message1.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        message1,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                }

                if (message2.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        message2,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onButtonClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(buttonText)
                }
                secondaryButtonText?.let { label ->
                    TextButton(onClick = onSecondaryButtonClick) {
                        Text(label)
                    }
                }
            }
        }
    }
}
