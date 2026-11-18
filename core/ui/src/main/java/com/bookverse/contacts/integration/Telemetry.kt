package com.bookverse.contacts.integration

interface Telemetry {
    fun setCollectionEnabled(enabled: Boolean)
    fun recordEvent(name: String, parameters: Map<String, String> = emptyMap())
}
