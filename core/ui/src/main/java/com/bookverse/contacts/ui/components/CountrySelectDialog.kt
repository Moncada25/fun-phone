package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.settings.Country

@Composable
fun CountrySelectDialog(
    onDismiss: () -> Unit,
    onCountrySelected: (Country?) -> Unit,
    selectedCountry: Country?,
    showCountryCode: Boolean = false,
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val accentColor = MaterialTheme.colorScheme.primary
    val listState = rememberLazyListState()
    val countries = Countries.getCountryList(LocalContext.current)
    val popularCodes = Countries.popularCountryCodes.toSet()
    val countriesByIso = countries.associateBy { it.countryCode }
    val popularCountries = Countries.popularCountryCodes.mapNotNull { countriesByIso[it] }
    val otherCountries = countries
        .filterNot { it.countryCode in popularCodes }
        .sortedBy { it.name }
    val popularHeader = stringResource(R.string.country_section_popular)
    val otherHeader = stringResource(R.string.country_section_other)
    val fastScrollerSections = remember(showCountryCode, popularCountries, otherCountries, popularHeader, otherHeader) {
        val sections = mutableListOf<Pair<String, Int>>()
        var index = 0
        if (showCountryCode) {
            index++
        }
        if (popularCountries.isNotEmpty()) {
            sections += popularHeader to index
            index++
            if (popularCountries.isNotEmpty()) {
                sections += "★" to index
                index += popularCountries.size
            }
        }
        if (otherCountries.isNotEmpty()) {
            sections += otherHeader to index
            index++
            var lastLetter: Char? = null
            otherCountries.forEach { country ->
                val letter = country.name.firstOrNull()?.uppercaseChar() ?: '#'
                if (letter != lastLetter) {
                    sections += letter.toString() to index
                    lastLetter = letter
                }
                index++
            }
        }
        sections
    }
    val showFastScroller = fastScrollerSections.isNotEmpty() && countries.size > 20

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = if (showCountryCode) stringResource(R.string.country_code_selector)
                    else stringResource(R.string.country_selector),
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (showCountryCode) {
                            item {
                                NoCountryCodeRow(
                                    isSelected = selectedCountry == null,
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    onSelect = {
                                        onCountrySelected(null)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        if (popularCountries.isNotEmpty()) {
                            stickyHeader {
                                CountrySectionHeader(
                                    title = popularHeader,
                                    backgroundColor = backgroundColor,
                                    accentColor = accentColor
                                )
                            }
                            items(popularCountries, key = { it.countryCode }) { country ->
                                CountryOptionRow(
                                    country = country,
                                    isSelected = selectedCountry?.countryCode == country.countryCode,
                                    showCountryCode = showCountryCode,
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    onClick = {
                                        onCountrySelected(country)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        if (otherCountries.isNotEmpty()) {
                            stickyHeader {
                                CountrySectionHeader(
                                    title = otherHeader,
                                    backgroundColor = backgroundColor,
                                    accentColor = accentColor
                                )
                            }
                            items(otherCountries, key = { it.countryCode }) { country ->
                                CountryOptionRow(
                                    country = country,
                                    isSelected = selectedCountry?.countryCode == country.countryCode,
                                    showCountryCode = showCountryCode,
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    onClick = {
                                        onCountrySelected(country)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }

                    if (showFastScroller) {
                        FastScroller(
                            listState = listState,
                            sections = fastScrollerSections,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp),
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountrySectionHeader(
    title: String,
    backgroundColor: Color,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = accentColor
        )
    }
}

@Composable
private fun NoCountryCodeRow(
    isSelected: Boolean,
    accentColor: Color,
    textColor: Color,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable { onSelect() }
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "🌍",
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Text(
                    text = stringResource(R.string.no_country_code),
                    color = if (isSelected) accentColor else textColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.cd_selected),
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CountryOptionRow(
    country: Country,
    isSelected: Boolean,
    showCountryCode: Boolean,
    accentColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = country.flag,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Text(
                    text = if (showCountryCode) "${country.name} (${country.code})" else country.name,
                    color = if (isSelected) accentColor else textColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.cd_selected),
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
