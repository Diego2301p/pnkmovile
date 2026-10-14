package com.example.pnk

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/**
 * Monitors network connectivity changes and notifies callbacks.
 * Used for offline detection and automatic sync triggering.
 */
class NetworkHelper(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    var isOnline: Boolean = checkCurrentConnectivity()
        private set

    private var onAvailableCallback: (() -> Unit)? = null
    private var onLostCallback: (() -> Unit)? = null

    /**
     * Check if currently connected to the internet.
     */
    fun checkCurrentConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Register a network callback to receive connectivity change notifications.
     * @param onAvailable Called when network becomes available
     * @param onLost Called when network is lost
     */
    fun registerNetworkCallback(onAvailable: () -> Unit, onLost: () -> Unit) {
        this.onAvailableCallback = onAvailable
        this.onLostCallback = onLost

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val wasOffline = !isOnline
                isOnline = true
                if (wasOffline) {
                    onAvailableCallback?.invoke()
                }
            }

            override fun onLost(network: Network) {
                isOnline = checkCurrentConnectivity()
                if (!isOnline) {
                    onLostCallback?.invoke()
                }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                  capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val wasOffline = !isOnline
                isOnline = hasInternet
                if (hasInternet && wasOffline) {
                    onAvailableCallback?.invoke()
                } else if (!hasInternet && !wasOffline) {
                    onLostCallback?.invoke()
                }
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Unregister the network callback to prevent leaks.
     */
    fun unregisterCallback() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            networkCallback = null
        }
    }
}
