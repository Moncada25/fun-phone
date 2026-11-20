package com.bookverse.contacts.data.repository

import android.Manifest
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.bookverse.contacts.utils.PermissionUtils

object ProfilePhoneNumbers {
    fun read(context: Context): List<String> {
        if (!PermissionUtils.hasPermission(context, Manifest.permission.READ_CONTACTS)) return emptyList()
        return try {
            context.contentResolver.query(
                Uri.withAppendedPath(
                    ContactsContract.Profile.CONTENT_URI,
                    ContactsContract.Contacts.Data.CONTENT_DIRECTORY,
                ),
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                null,
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        cursor.getString(0)?.takeIf(String::isNotBlank)?.let(::add)
                    }
                }
            }.orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }
}
