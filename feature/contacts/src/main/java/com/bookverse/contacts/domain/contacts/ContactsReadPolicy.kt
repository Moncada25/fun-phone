package com.bookverse.contacts.domain.contacts

object ContactsReadPolicy {
    inline fun <T> readIfGranted(
        isGranted: Boolean,
        read: () -> List<T>,
    ): List<T> = if (isGranted) read() else emptyList()
}
