package com.bookverse.contacts.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class ConnectivityChecker(private val context: Context) {

    fun hasInternetConnection(): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            val hasInternet =
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated =
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            hasInternet && isValidated
        } catch (_: SecurityException) {
            // A stale/mis-merged manifest must show the offline state, never crash the app.
            false
        }
    }
}
