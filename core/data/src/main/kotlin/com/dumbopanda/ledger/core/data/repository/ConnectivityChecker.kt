package com.dumbopanda.ledger.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A snapshot check, not a callback -- [BootRepository] needs one answer at the moment it
 * decides, unlike WorkManager's own `NetworkType.CONNECTED` constraint (§5.7), which watches
 * continuously. `NET_CAPABILITY_VALIDATED` (actual internet reachability, not just "a radio is
 * up") is deliberate: a phone connected to a captive portal or a dead Wi-Fi AP should be
 * treated as offline for §7.1's boot decision, the same way it would eventually fail a real
 * refresh call anyway -- this just answers that question up front instead of via a timeout.
 */
@Singleton
class ConnectivityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
