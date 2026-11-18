package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.data.model.SocialMediaType

@Composable
fun SocialMediaButton(
    type: SocialMediaType,
    isSelected: Boolean,
    fullColor: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    val networkColor = type.color

    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {}.selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (fullColor) networkColor else MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) accentColor else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = type.icon),
                contentDescription = null,
                tint = if (fullColor) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = type.socialMediaName,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
