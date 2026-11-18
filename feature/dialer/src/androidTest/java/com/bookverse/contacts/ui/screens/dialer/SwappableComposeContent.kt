package com.bookverse.contacts.ui.screens.dialer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SwappableComposeContent(private val install: (@Composable () -> Unit) -> Unit) {
    private var current by mutableStateOf<(@Composable () -> Unit)?>(null)
    private var installed = false

    fun show(content: @Composable () -> Unit) {
        current = content
        if (!installed) {
            installed = true
            install { current?.invoke() }
        }
    }
}
