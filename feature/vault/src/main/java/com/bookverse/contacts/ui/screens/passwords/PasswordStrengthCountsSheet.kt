package com.bookverse.contacts.ui.screens.passwords

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.ui.components.CountBreakdownSheet
import com.bookverse.contacts.ui.components.CountInfoRow

private data class PasswordStrengthCount(
    val icon: ImageVector,
    val label: String,
    val count: Int,
    val color: Color,
)

private data class PasswordStrengthLevel(
    val minScore: Int,
    val maxScore: Int,
    val labelRes: Int,
)

@Composable
internal fun PasswordStrengthCountsSheet(
    passwords: List<String>,
    accentColor: Color,
    reusedCount: Int = 0,
    onDismiss: () -> Unit,
) {
    val strengthLevels = listOf(
        PasswordStrengthLevel(0, 0, R.string.strength_prohibited),
        PasswordStrengthLevel(1, 1, R.string.strength_very_weak),
        PasswordStrengthLevel(2, 2, R.string.strength_weak),
        PasswordStrengthLevel(3, 3, R.string.strength_fair),
        PasswordStrengthLevel(4, 5, R.string.strength_medium),
        PasswordStrengthLevel(6, 6, R.string.strength_good),
        PasswordStrengthLevel(7, 7, R.string.strength_strong),
        PasswordStrengthLevel(8, 8, R.string.strength_very_strong),
        PasswordStrengthLevel(9, 10, R.string.strength_excellent),
    )
    val strengthCounts = strengthLevels.map { level ->
        PasswordStrengthCount(
            icon = if (level.minScore >= 7) Icons.Default.Lock else Icons.Default.LockOpen,
            label = stringResource(level.labelRes),
            count = passwords.count {
                PasswordGenerator.strength(it) in level.minScore..level.maxScore
            },
            color = passwordStrengthInfo(level.minScore).first,
        )
    }

    CountBreakdownSheet(
        title = stringResource(R.string.password_strength_sheet_title),
        rows = strengthCounts.map { item ->
            CountInfoRow(
                icon = item.icon,
                label = item.label,
                count = item.count,
                iconTint = item.color,
            )
        } + CountInfoRow(
            icon = Icons.Default.Warning,
            label = stringResource(R.string.password_reused_count_label),
            count = reusedCount,
            iconTint = MaterialTheme.colorScheme.error,
        ),
        accentColor = accentColor,
        onDismiss = onDismiss,
    )
}
