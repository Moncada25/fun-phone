package com.bookverse.contacts.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.settings.Country
import contacts.core.entities.PhoneEntity
import com.bookverse.contacts.core.ui.R as CoreUiR

data class PhoneFieldState(
    val phone: ContactPhone,
    val country: Country?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumbersField(
    phones: List<ContactPhone>,
    onPhonesChange: (List<ContactPhone>) -> Unit,
    countrySelected: Country?,
    onCountrySelected: ((Country?) -> Unit)? = null
) {
    val context = LocalContext.current

    var phoneStates by remember {
        mutableStateOf(
            if (phones.isEmpty()) {
                listOf(
                    PhoneFieldState(
                        ContactPhone("", PhoneEntity.Type.MOBILE, countrySelected?.code),
                        countrySelected
                    )
                )
            } else {
                phones.map { phone ->
                    val phoneCountry = if (phone.countryCode != null) {
                        Countries.getCountryList(context).find { it.code == phone.countryCode }
                    } else {
                        null
                    }
                    PhoneFieldState(phone, phoneCountry ?: countrySelected)
                }
            }
        )
    }

    val phoneTypes = listOf(
        PhoneEntity.Type.MOBILE to stringResource(CoreUiR.string.phone_type_mobile),
        PhoneEntity.Type.HOME to stringResource(CoreUiR.string.phone_type_home),
        PhoneEntity.Type.WORK to stringResource(CoreUiR.string.phone_type_work),
        PhoneEntity.Type.MAIN to stringResource(CoreUiR.string.phone_type_main)
    )

    fun updatePhones() {
        val validPhones = phoneStates.map { phoneState ->
            phoneState.phone.copy(countryCode = phoneState.country?.code)
        }
        onPhonesChange(validPhones)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        phoneStates.forEachIndexed { index, phoneState ->
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = phoneState.phone.number,
                        onValueChange = { newNumber ->
                            phoneStates = phoneStates.toMutableList().apply {
                                set(
                                    index, phoneState.copy(
                                        phone = phoneState.phone.copy(number = newNumber)
                                    )
                                )
                            }
                            updatePhones()
                        },
                        label = stringResource(R.string.phone_placeholder),
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.weight(1f),
                        showCountryFlag = true,
                        selectedCountry = phoneState.country,
                        onCountrySelected = { country ->
                            phoneStates = phoneStates.toMutableList().apply {
                                set(
                                    index, phoneState.copy(
                                        phone = phoneState.phone.copy(countryCode = country?.code),
                                        country = country
                                    )
                                )
                            }
                            updatePhones()
                            onCountrySelected?.invoke(country)
                        },
                        isError = phoneState.phone.number.isBlank()
                    )

                    if (phoneStates.size > 1) {
                        IconButton(
                            onClick = {
                                phoneStates = phoneStates.toMutableList().apply {
                                    removeAt(index)
                                }
                                updatePhones()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = stringResource(R.string.cd_remove_phone),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = phoneTypes.find { it.first == phoneState.phone.type }?.second
                            ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 4.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        phoneTypes.forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    phoneStates = phoneStates.toMutableList().apply {
                                        set(
                                            index, phoneState.copy(
                                                phone = phoneState.phone.copy(type = type)
                                            )
                                        )
                                    }
                                    updatePhones()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (index < phoneStates.size - 1) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (phoneStates.size < 5) {
            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = {
                    phoneStates = phoneStates + PhoneFieldState(
                        ContactPhone("", PhoneEntity.Type.MOBILE, countrySelected?.code),
                        countrySelected
                    )
                    updatePhones()
                },
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
            ) {
                Icon(
                    imageVector = Icons.Default.AddIcCall,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_phone_number))
            }
        }
    }
}
