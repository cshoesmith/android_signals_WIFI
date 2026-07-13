package com.example.androidsignalswifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.URL
import kotlin.coroutines.resume

data class WifiTestResult(
    val connected: Boolean,
    val hasInternet: Boolean = false,
    val captivePortal: Boolean = false,
    val ipAddress: String = "",
    val gateway: String = "",
    val dns: String = "",
    val linkSpeedMbps: Int = -1,
    val latencyMs: Long = -1L,
    val error: String? = null
)

// On-demand connect to an open network, gather link info, probe for internet, then
// release the request so the OS drops the connection. Android 10+ shows a system
// approval dialog per SSID; there is no silent path on this target.
class WifiTester(context: Context) {
    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun testOpenNetwork(ssid: String, timeoutMs: Long = 25_000L): WifiTestResult {
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            // Do not require INTERNET so onAvailable still fires on captive-portal APs
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        // Callback runs on this background thread so the HTTP probe can block safely.
        val thread = HandlerThread("wifi-test").apply { start() }
        val handler = Handler(thread.looper)

        val result = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<WifiTestResult> { cont ->
                val callback = object : ConnectivityManager.NetworkCallback() {
                    private var done = false
                    override fun onAvailable(network: Network) {
                        if (done) return
                        done = true
                        val r = gather(network)
                        try { cm.unregisterNetworkCallback(this) } catch (_: Exception) {}
                        if (cont.isActive) cont.resume(r)
                    }
                    override fun onUnavailable() {
                        if (done) return
                        done = true
                        try { cm.unregisterNetworkCallback(this) } catch (_: Exception) {}
                        if (cont.isActive) cont.resume(
                            WifiTestResult(connected = false, error = "Declined or not reachable")
                        )
                    }
                }
                cont.invokeOnCancellation {
                    try { cm.unregisterNetworkCallback(callback) } catch (_: Exception) {}
                }
                try {
                    cm.requestNetwork(request, callback, handler)
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(
                        WifiTestResult(connected = false, error = e.message ?: "Request failed")
                    )
                }
            }
        } ?: WifiTestResult(connected = false, error = "Timed out")

        thread.quitSafely()
        return result
    }

    private fun gather(network: Network): WifiTestResult {
        val lp = cm.getLinkProperties(network)
        val ip = lp?.linkAddresses?.firstOrNull { it.address is Inet4Address }?.address?.hostAddress ?: ""
        val gateway = lp?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress ?: ""
        val dns = lp?.dnsServers?.mapNotNull { it.hostAddress }?.joinToString(", ") ?: ""

        var linkSpeed = -1
        val caps = cm.getNetworkCapabilities(network)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (caps?.transportInfo as? WifiInfo)?.let { linkSpeed = it.linkSpeed }
        }

        var hasInternet = false
        var captive = false
        var latency = -1L
        try {
            val url = URL("http://connectivitycheck.gstatic.com/generate_204")
            val start = System.currentTimeMillis()
            val conn = network.openConnection(url) as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.instanceFollowRedirects = false
            conn.useCaches = false
            val code = conn.responseCode
            latency = System.currentTimeMillis() - start
            conn.disconnect()
            if (code == 204) hasInternet = true else captive = true
        } catch (_: Exception) {
            hasInternet = false
        }

        return WifiTestResult(
            connected = true,
            hasInternet = hasInternet,
            captivePortal = captive,
            ipAddress = ip,
            gateway = gateway,
            dns = dns,
            linkSpeedMbps = linkSpeed,
            latencyMs = latency
        )
    }
}
