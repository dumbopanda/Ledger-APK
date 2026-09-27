package com.dumbopanda.ledger.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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

    // For screens that show online/offline: a snapshot only updated when something else changed
    // left Home saying "offline" with Send disabled long after the network came back.
    fun onlineFlow(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(isOnline()) }
            override fun onLost(network: Network) { trySend(isOnline()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(isOnline()) }
        }
        trySend(isOnline())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
