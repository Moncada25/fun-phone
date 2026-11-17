package com.bookverse.contacts.security.autofill

import android.app.assist.AssistStructure
import android.view.View
import android.view.autofill.AutofillId
import com.bookverse.contacts.domain.passwords.CredentialTarget

internal class ParsedLoginForm(
    val target: CredentialTarget,
    val usernameId: AutofillId?,
    val passwordId: AutofillId?,
    val usernameText: String?,
    val passwordText: String?,
)

internal object AutofillStructureParser {
    private class Candidate(
        val node: AssistStructure.ViewNode,
        val webDomain: String?,
        val snapshot: AutofillFieldSnapshot,
    )

    fun parse(structure: AssistStructure): ParsedLoginForm? {
        val packageName = structure.activityComponent?.packageName ?: return null
        val candidates = mutableListOf<Candidate>()
        for (index in 0 until structure.windowNodeCount) {
            collect(structure.getWindowNodeAt(index).rootViewNode, null, candidates)
        }
        if (candidates.isEmpty()) return null
        val indexes = AutofillFieldClassifier.classify(candidates.map { it.snapshot })
        val username = indexes.username?.let(candidates::get)
        val password = indexes.password?.let(candidates::get)
        if (username == null && password == null) return null
        val webDomain = (password ?: username)?.webDomain
            ?.takeIf { TrustedBrowsers.isTrusted(packageName) }
        return ParsedLoginForm(
            target = CredentialTarget(packageName = packageName, webDomain = webDomain),
            usernameId = username?.node?.autofillId,
            passwordId = password?.node?.autofillId,
            usernameText = username?.node?.textValue(),
            passwordText = password?.node?.textValue(),
        )
    }

    private fun collect(
        node: AssistStructure.ViewNode,
        inheritedDomain: String?,
        out: MutableList<Candidate>,
    ) {
        val domain = node.webDomain?.takeIf { it.isNotBlank() } ?: inheritedDomain
        if (node.autofillId != null && node.autofillType == View.AUTOFILL_TYPE_TEXT) {
            val attributes = node.htmlInfo?.attributes
                ?.associate { it.first.lowercase() to it.second }
                .orEmpty()
            val isHtmlInput = node.htmlInfo?.tag.equals("input", ignoreCase = true)
            out += Candidate(
                node = node,
                webDomain = domain,
                snapshot = AutofillFieldSnapshot(
                    hints = node.autofillHints?.toList().orEmpty(),
                    htmlType = attributes["type"].takeIf { isHtmlInput },
                    htmlAutocomplete = attributes["autocomplete"],
                    htmlName = listOfNotNull(attributes["name"], attributes["id"])
                        .joinToString(" ")
                        .takeIf { it.isNotBlank() },
                    inputType = node.inputType,
                    idEntry = node.idEntry,
                    hintText = node.hint,
                ),
            )
        }
        for (index in 0 until node.childCount) {
            collect(node.getChildAt(index), domain, out)
        }
    }

    private fun AssistStructure.ViewNode.textValue(): String? =
        autofillValue?.takeIf { it.isText }?.textValue?.toString()
}
