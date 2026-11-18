package com.bookverse.contacts.service

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import com.bookverse.contacts.utils.PermissionUtils

object CallScreeningRole {
    fun isRoleAvailable(context: Context): Boolean =
        context.getSystemService(RoleManager::class.java)
            ?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true

    fun isActive(context: Context): Boolean =
        PermissionUtils.isDefaultDialer(context) ||
            context.getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

    fun createRequestIntent(context: Context): Intent? =
        if (isRoleAvailable(context)) {
            context.getSystemService(RoleManager::class.java)
                ?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        } else {
            null
        }
}
