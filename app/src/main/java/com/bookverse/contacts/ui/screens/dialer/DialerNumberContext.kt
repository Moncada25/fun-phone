package com.bookverse.contacts.ui.screens.dialer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed

@Immutable
internal data class DialerNumberContext(
    val callingCode: String? = null,
    val countryFlag: String? = null,
    val countryName: String? = null,
    val contactName: String? = null,
) {
    val isEmpty: Boolean
        get() = callingCode.isNullOrBlank() && contactName.isNullOrBlank()
}

@Composable
internal fun DialerNumberContextRow(
    context: DialerNumberContext,
    modifier: Modifier = Modifier,
    onCountryClick: (() -> Unit)? = null,
    onContactClick: (() -> Unit)? = null,
) {
    val durationMillis = LocalAnimationSpeed.current.durationMs.coerceAtMost(180)
    AnimatedVisibility(
        visible = !context.isEmpty || onCountryClick != null,
        modifier = modifier,
        enter = expandVertically(tween(durationMillis)) + fadeIn(tween(durationMillis)),
        exit = shrinkVertically(tween(durationMillis)) + fadeOut(tween(durationMillis)),
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val chipColor = colorScheme.onSurface.copy(
            alpha = if (colorScheme.surface.luminance() < 0.5f) 0.10f else 0.06f,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 28.dp)
                .testTag(DIALER_NUMBER_CONTEXT_TAG),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val callingCode = context.callingCode?.takeIf { it.isNotBlank() }
            if (callingCode != null || onCountryClick != null) {
                val countrySelectorLabel = stringResource(R.string.country_selector)
                val codeDescription = if (callingCode != null) {
                    stringResource(
                        R.string.dialer_calling_code_description,
                        callingCode,
                        context.countryName.orEmpty(),
                    )
                } else {
                    countrySelectorLabel
                }
                Text(
                    text = context.countryFlag ?: callingCode ?: countrySelectorLabel,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(chipColor)
                        .then(
                            if (onCountryClick != null) {
                                Modifier.clickable(onClick = onCountryClick)
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                        .testTag(DIALER_CALLING_CODE_TAG)
                        .clearAndSetSemantics {
                            contentDescription = codeDescription
                            if (onCountryClick != null) {
                                role = Role.Button
                                onClick(label = countrySelectorLabel) {
                                    onCountryClick()
                                    true
                                }
                            }
                        },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface.copy(alpha = 0.82f),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            val contactName = context.contactName?.takeIf { it.isNotBlank() }
            if (contactName != null) {
                val contactDescription = stringResource(
                    R.string.dialer_matched_contact_description,
                    contactName,
                )
                val callLabel = stringResource(R.string.call)
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(
                            if (onContactClick != null) {
                                Modifier
                                    .clip(CircleShape)
                                    .background(chipColor)
                                    .clickable(onClick = onContactClick)
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            } else {
                                Modifier
                            },
                        )
                        .testTag(DIALER_MATCHED_CONTACT_TAG)
                        .clearAndSetSemantics {
                            contentDescription = contactDescription
                            if (onContactClick != null) {
                                role = Role.Button
                                onClick(label = callLabel) {
                                    onContactClick()
                                    true
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = colorScheme.onSurface.copy(alpha = 0.72f),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = contactName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

internal const val DIALER_NUMBER_CONTEXT_TAG = "dialer_number_context"
internal const val DIALER_CALLING_CODE_TAG = "dialer_calling_code"
internal const val DIALER_MATCHED_CONTACT_TAG = "dialer_matched_contact"
