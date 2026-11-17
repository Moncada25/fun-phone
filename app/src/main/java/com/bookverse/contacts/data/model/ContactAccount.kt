package com.bookverse.contacts.data.model

import android.accounts.Account
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactAccount(
    val name: String,
    val type: String,
    val displayName: String = name
) : Parcelable {
    companion object {

        const val LOCAL = "Local Account"
        const val WHATSAPP = "com.whatsapp"

        val LOCAL_ACCOUNT = ContactAccount(
            name = "Local Account",
            type = "com.android.local",
            displayName = LOCAL
        )

        val ACCOUNTS_BLACKLIST = listOf(
            "WhatsApp",
            "Meet",
            "WhatsApp Business"
        )

        fun fromAccount(account: Account?): ContactAccount {
            if (account == null) {
                return LOCAL_ACCOUNT
            }

            return ContactAccount(
                name = account.name,
                type = account.type,
                displayName = account.name
            )
        }
    }

    val isLocal: Boolean
        get() = name == LOCAL_ACCOUNT.name && type == LOCAL_ACCOUNT.type

    fun toAccount(): Account? {
        return if (isLocal) null else Account(name, type)
    }
}
