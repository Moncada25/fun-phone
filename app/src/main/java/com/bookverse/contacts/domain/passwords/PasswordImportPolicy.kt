package com.bookverse.contacts.domain.passwords

import com.bookverse.contacts.data.password.PasswordCredential

data class PasswordImportResult(val imported: Int, val skipped: Int)

data class PasswordImportPlan(val toImport: List<PasswordCredential>, val skipped: Int)

object PasswordImportPolicy {
    private data class Identity(val name: String, val username: String, val password: String)

    private fun PasswordCredential.identity() = Identity(name.trim(), username, password)

    fun plan(
        existing: List<PasswordCredential>,
        incoming: List<PasswordCredential>,
    ): PasswordImportPlan {
        val remaining = existing
            .filterNot { it.isDeleted }
            .groupingBy { it.identity() }
            .eachCount()
            .toMutableMap()
        val toImport = mutableListOf<PasswordCredential>()
        var skipped = 0
        for (credential in incoming) {
            val identity = credential.identity()
            val matches = remaining[identity] ?: 0
            if (matches > 0) {
                remaining[identity] = matches - 1
                skipped++
            } else {
                toImport += credential
            }
        }
        return PasswordImportPlan(toImport, skipped)
    }
}
