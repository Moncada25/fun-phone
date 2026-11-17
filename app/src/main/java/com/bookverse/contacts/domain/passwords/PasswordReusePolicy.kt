package com.bookverse.contacts.domain.passwords

import com.bookverse.contacts.data.password.PasswordCredential

object PasswordReusePolicy {
    fun reusedIds(credentials: List<PasswordCredential>): Set<Long> =
        credentials
            .asSequence()
            .filter { !it.isDeleted && it.password.isNotEmpty() }
            .groupBy { it.password }
            .values
            .filter { it.size > 1 }
            .flatMapTo(mutableSetOf()) { group -> group.map { it.id } }

    fun isReused(password: String, ownId: Long?, credentials: List<PasswordCredential>): Boolean =
        password.isNotEmpty() &&
            credentials.any { !it.isDeleted && it.id != ownId && it.password == password }
}
