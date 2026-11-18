package com.bookverse.contacts.ui.screens.settings

import com.bookverse.contacts.service.CallNotificationChannels
import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.bookverse.contacts.service.CallScreeningRole
import com.bookverse.contacts.service.NotificationService
import com.bookverse.contacts.utils.PermissionUtils
import com.bookverse.contacts.utils.rememberDefaultDialerLauncher

fun readPhoneStatus(context: Context): PhoneStatusUiState {
    val notificationsReady = CallNotificationChannels.readAvailability(context.applicationContext).isReady
    return PhoneStatusUiState(
        isDefaultDialer = PermissionUtils.isDefaultDialer(context),
        hasCallNotifications = notificationsReady,
        hasContactsPermission = PermissionUtils.hasContactsPermission(context),
        hasCallLogPermission = PermissionUtils.hasCallLogPermission(context),
        hasPhoneStatePermission = PermissionUtils.hasPhoneStatePermission(context),
        hasCallPhonePermission = PermissionUtils.hasCallsPermission(context),
        hasExactAlarmsPermission = NotificationService.canScheduleExactAlarms(context),
        hasCallScreening = CallScreeningRole.isActive(context),
        canRequestCallScreening = CallScreeningRole.isRoleAvailable(context),
        notificationsRequireSettings = requiresNotificationSettings(
            sdkInt = Build.VERSION.SDK_INT,
            runtimePermissionGranted = PermissionUtils.hasNotificationPermission(context),
            notificationsReady = notificationsReady,
        ),
    )
}

internal data class PhoneSetupActions(
    val controller: PhoneSetupController,
    val request: (PhoneStatusRequirement) -> Unit,
)

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

private fun PhoneStatusRequirement.permissions(): Array<String> = when (this) {
    PhoneStatusRequirement.CONTACTS -> PermissionUtils.contactsPermissions()
    PhoneStatusRequirement.CALL_LOG -> arrayOf(Manifest.permission.READ_CALL_LOG)
    PhoneStatusRequirement.PHONE_STATE -> arrayOf(Manifest.permission.READ_PHONE_STATE)
    PhoneStatusRequirement.CALL_PHONE -> arrayOf(Manifest.permission.CALL_PHONE)
    PhoneStatusRequirement.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }
    PhoneStatusRequirement.DEFAULT_DIALER -> emptyArray()
    PhoneStatusRequirement.EXACT_ALARMS -> emptyArray()
}

@Composable
internal fun rememberPhoneSetupActions(): PhoneSetupActions {
    val context = LocalContext.current
    val activity = remember(context) { context.activity() }
    val controller = rememberSaveable(
        saver = listSaver(
            save = { controller: PhoneSetupController -> controller.saveOutcomes() },
            restore = { PhoneSetupController(readPhoneStatus(context)).apply { restoreOutcomes(it) } },
        ),
    ) { PhoneSetupController(readPhoneStatus(context)) }

    fun finish(requirement: PhoneStatusRequirement, outcome: SetupOutcome) {
        controller.finishRequest(requirement, outcome, readPhoneStatus(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        // Derive the request from its result, including when Android restores the launcher after recreation.
        val requirement = PhoneStatusRequirement.entries.firstOrNull { requirement ->
            requirement.permissions().any { it in results }
        } ?: controller.uiState.requesting
        if (requirement != null) {
            val denied = requirement.permissions().filterNot { PermissionUtils.hasPermission(context, it) }
            val canRequestAgain = denied.all { permission ->
                activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
            finish(requirement, permissionResultOutcome(denied.isEmpty(), canRequestAgain))
        } else {
            controller.onResume(readPhoneStatus(context))
        }
    }

    val dialerLauncher = rememberDefaultDialerLauncher(
        onSuccess = { finish(PhoneStatusRequirement.DEFAULT_DIALER, SetupOutcome.GRANTED) },
        onFailure = { finish(PhoneStatusRequirement.DEFAULT_DIALER, SetupOutcome.DENIED) },
        showToast = false,
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        CallNotificationChannels.ensureCreated(context.applicationContext)
        controller.onResume(readPhoneStatus(context))
    }

    return PhoneSetupActions(controller) { requirement ->
        controller.refresh(readPhoneStatus(context))
        if (controller.beginRequest(requirement)) {
            try {
                when {
                    requirement == PhoneStatusRequirement.DEFAULT_DIALER -> {
                        val roleAvailable =
                            activity?.getSystemService(RoleManager::class.java)?.isRoleAvailable(RoleManager.ROLE_DIALER) == true
                        if (!roleAvailable) {
                            finish(requirement, SetupOutcome.UNAVAILABLE)
                        } else {
                            PermissionUtils.requestSetAsDefaultDialer(
                                context = activity ?: context,
                                launcher = dialerLauncher,
                                onSuccess = { finish(requirement, SetupOutcome.GRANTED) },
                                onFailure = { finish(requirement, SetupOutcome.UNAVAILABLE) },
                            )
                        }
                    }
                    requirement == PhoneStatusRequirement.NOTIFICATIONS &&
                        (controller.uiState.phoneStatus.notificationsRequireSettings ||
                            controller.uiState.outcome(requirement) == SetupOutcome.BLOCKED) -> {
                        CallNotificationChannels.openSettings(context)
                    }
                    requirement == PhoneStatusRequirement.EXACT_ALARMS -> {
                        if (NotificationService.canScheduleExactAlarms(context)) {
                            finish(requirement, SetupOutcome.GRANTED)
                        } else {
                            NotificationService.openAlarmPermissionSettings(context)
                        }
                    }
                    controller.uiState.outcome(requirement) == SetupOutcome.BLOCKED -> {
                        PermissionUtils.openAppSettings(context)
                    }
                    else -> permissionLauncher.launch(requirement.permissions().filterNot {
                        PermissionUtils.hasPermission(context, it)
                    }.toTypedArray())
                }
            } catch (_: android.content.ActivityNotFoundException) {
                finish(requirement, SetupOutcome.UNAVAILABLE)
            } catch (_: SecurityException) {
                finish(requirement, SetupOutcome.UNAVAILABLE)
            }
        }
    }
}
