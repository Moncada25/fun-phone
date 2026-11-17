package com.bookverse.contacts.security.autofill

internal object TrustedBrowsers {
    private val packages = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.chromium.chrome",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix",
        "org.mozilla.focus",
        "org.mozilla.klar",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.brave.browser_beta",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.sec.android.app.sbrowser",
        "com.sec.android.app.sbrowser.beta",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.kiwibrowser.browser",
        "org.bromite.bromite",
        "app.vanadium.browser",
        "com.ecosia.android",
        "org.torproject.torbrowser",
        "com.android.browser",
        "com.mi.globalbrowser",
        "com.huawei.browser",
    )

    fun isTrusted(packageName: String): Boolean = packageName in packages
}
