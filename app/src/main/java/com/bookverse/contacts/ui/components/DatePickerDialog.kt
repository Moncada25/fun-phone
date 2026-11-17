package com.bookverse.contacts.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.select_date),
    maxSelectableDate: Long? = null,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    val selectableDates = remember(maxSelectableDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return maxSelectableDate == null || utcTimeMillis <= maxSelectableDate
            }

            override fun isSelectableYear(year: Int): Boolean {
                return maxSelectableDate == null || year <= Calendar.getInstance().apply {
                    timeInMillis = maxSelectableDate
                }.get(Calendar.YEAR)
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate,
        selectableDates = selectableDates,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.95f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DatePicker(
                    title = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = title,
                                color = textColor,
                                fontSize = 20.sp
                            )
                        }
                    },
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                        todayDateBorderColor = accentColor,
                        todayContentColor = textColor,
                        selectedYearContainerColor = accentColor,
                        selectedDayContainerColor = accentColor,
                        containerColor = backgroundColor,
                        dateTextFieldColors = TextFieldDefaults.colors(
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = textColor,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = textColor,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { newDateMillis ->
                                val calendar = Calendar.getInstance().apply {
                                    timeInMillis = selectedDate
                                    val newCalendar = Calendar.getInstance().apply {
                                        timeInMillis = newDateMillis
                                        add(Calendar.HOUR_OF_DAY, 12)
                                    }
                                    set(Calendar.YEAR, newCalendar.get(Calendar.YEAR))
                                    set(Calendar.MONTH, newCalendar.get(Calendar.MONTH))
                                    set(
                                        Calendar.DAY_OF_MONTH,
                                        newCalendar.get(Calendar.DAY_OF_MONTH)
                                    )
                                }
                                onDateSelected(calendar.timeInMillis)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(text = stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}
