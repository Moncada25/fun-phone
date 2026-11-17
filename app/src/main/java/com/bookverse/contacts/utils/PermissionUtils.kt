package com.bookverse.contacts.utils

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.bookverse.contacts.R

object PermissionUtils {

    fun hasPermission(ctx: Context, perm: String) =
        ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

    fun contactsPermissions(): Array<String> = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.GET_ACCOUNTS,
    )

    fun hasContactsPermission(context: Context): Boolean {
        return contactsPermissions().all { hasPermission(context, it) }
    }

    fun hasCallLogPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.READ_CALL_LOG)
    }

    fun hasCallsPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.CALL_PHONE)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else true
    }

    fun hasPhoneStatePermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.READ_PHONE_STATE)
    }

    fun hasCalendarPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.READ_CALENDAR)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun isDefaultDialer(context: Context): Boolean {
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager?
        return tm?.defaultDialerPackage == context.packageName
    }

    fun requestSetAsDefaultDialer(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        onSuccess: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null
    ) {
        if (isDefaultDialer(context)) {
            onSuccess?.invoke()
            return
        }

        val activity = context as? Activity
        val roleManager = activity?.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            launcher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        } else {
            onFailure?.invoke()
        }
    }

    fun handleDefaultDialerResult(
        context: Context,
        resultCode: Int,
        onSuccess: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null,
        showToast: Boolean = true
    ) {
        val isSuccess = resultCode == Activity.RESULT_OK && isDefaultDialer(context)

        if (showToast) {
            val message = if (isSuccess) {
                context.getString(R.string.dialer_role_granted)
            } else {
                context.getString(R.string.dialer_role_not_granted)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }

        if (isSuccess) {
            onSuccess?.invoke()
        } else {
            onFailure?.invoke()
        }
    }
}

@Composable
fun rememberDefaultDialerLauncher(
    onSuccess: (() -> Unit)? = null,
    onFailure: (() -> Unit)? = null,
    showToast: Boolean = true
): ActivityResultLauncher<Intent> {
    val context = LocalContext.current

    return rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        PermissionUtils.handleDefaultDialerResult(
            context = context,
            resultCode = result.resultCode,
            onSuccess = onSuccess,
            onFailure = onFailure,
            showToast = showToast
        )
    }
}
