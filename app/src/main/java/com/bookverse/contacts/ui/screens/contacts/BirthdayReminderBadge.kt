package com.bookverse.contacts.ui.screens.contacts

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bookverse.contacts.R
import com.bookverse.contacts.service.NotificationService
import com.bookverse.contacts.service.birthday.BirthdayReminderAccess
import com.bookverse.contacts.service.birthday.BirthdayReminderAccessIssue
import com.bookverse.contacts.service.birthday.BirthdayReminderNotifier
import com.bookverse.contacts.service.birthday.BirthdayReminderScheduler
import com.bookverse.contacts.utils.PermissionUtils

@Composable
fun BirthdayReminderBadge(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var accessIssue by remember {
        mutableStateOf(
            BirthdayReminderAccess.issue(context, BirthdayReminderNotifier.CHANNEL_ID),
        )
    }

    fun refreshAccess() {
        BirthdayReminderNotifier(context).createChannel()
        accessIssue = BirthdayReminderAccess.issue(context, BirthdayReminderNotifier.CHANNEL_ID)
        if (accessIssue == null) {
            BirthdayReminderScheduler(context).scheduleNext(catchUpToday = true)
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        refreshAccess()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshAccess()
    }

    LaunchedEffect(Unit) {
        refreshAccess()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val ready = accessIssue == null
    BirthdayReminderBadgeContent(
        ready = ready,
        accentColor = accentColor,
        modifier = modifier,
        onClick = {
            when (accessIssue) {
                BirthdayReminderAccessIssue.NOTIFICATION_PERMISSION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        PermissionUtils.openAppSettings(context)
                    }
                }
                BirthdayReminderAccessIssue.EXACT_ALARM_PERMISSION -> {
                    NotificationService.createAlarmPermissionIntent(context)?.let(settingsLauncher::launch)
                }
                BirthdayReminderAccessIssue.NOTIFICATION_SETTINGS -> {
                    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        putExtra(Settings.EXTRA_CHANNEL_ID, BirthdayReminderNotifier.CHANNEL_ID)
                    }
                    settingsLauncher.launch(intent)
                }
                null -> Unit
            }
        },
    )
}

@Composable
internal fun BirthdayReminderBadgeContent(
    ready: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (ready) {
        accentColor.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (ready) accentColor else MaterialTheme.colorScheme.onErrorContainer

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (ready) {
                    Icons.Default.NotificationsActive
                } else {
                    Icons.Outlined.NotificationsOff
                },
                contentDescription = null,
            )
            Text(
                text = stringResource(
                    if (ready) R.string.birthday_reminders_active
                    else R.string.birthday_reminders_enable,
                ),
                fontSize = 12.sp,
            )
        }
    }
}
