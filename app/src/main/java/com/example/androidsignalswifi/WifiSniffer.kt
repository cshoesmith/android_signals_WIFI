package com.example.androidsignalswifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ScannedAp(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val estLat: Double,
    val estLon: Double,
    val isSecured: Boolean,
    val totalWeight: Double,
    val securityType: String,
    val frequency: Int,
    val wifiStandard: String,
    val capabilities: String
)

class WifiSniffer(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val _scannedAps = MutableStateFlow<List<ScannedAp>>(emptyList())
    val scannedAps: StateFlow<List<ScannedAp>> = _scannedAps

    private val _lastScanTime = MutableStateFlow(0L)
    val lastScanTime: StateFlow<Long> = _lastScanTime

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var scanningJob: Job? = null
    var currentLocation: Location? = null

    init {
        loadPersistedAps()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            currentLocation = locationResult.lastLocation
        }
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                processScanResults()
            }
        }
    }

    @SuppressLint("Range")
    private fun loadPersistedAps() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = dbHelper.readableDatabase
            val cursor = db.query(DatabaseHelper.TABLE_APS, null, null, null, null, null, DatabaseHelper.COL_LAST_SEEN + " DESC")
            val loadedList = mutableListOf<ScannedAp>()
            
            while (cursor.moveToNext()) {
                val bssid = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_BSSID)) ?: continue
                val ssid = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_SSID)) ?: ""
                val estLat = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COL_EST_LAT))
                val estLon = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COL_EST_LON))
                val totalWeight = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COL_TOTAL_WEIGHT))
                val isSecuredInt = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COL_IS_SECURED))
                val lastRssi = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COL_LAST_RSSI))
                val secTypeIndex = cursor.getColumnIndex(DatabaseHelper.COL_SECURITY_TYPE)
                val securityType = if (secTypeIndex >= 0) cursor.getString(secTypeIndex) ?: "" else ""

                val freqIndex = cursor.getColumnIndex(DatabaseHelper.COL_FREQUENCY)
                val freq = if (freqIndex >= 0) cursor.getInt(freqIndex) else 0

                val stdIndex = cursor.getColumnIndex(DatabaseHelper.COL_WIFI_STANDARD)
                val wifiStandard = if (stdIndex >= 0) cursor.getString(stdIndex) ?: "Unknown" else "Unknown"

                val capIndex = cursor.getColumnIndex(DatabaseHelper.COL_CAPABILITIES)
                val caps = if (capIndex >= 0) cursor.getString(capIndex) ?: "" else ""

                loadedList.add(ScannedAp(bssid, ssid, lastRssi, estLat, estLon, isSecuredInt == 1, totalWeight, securityType, freq, wifiStandard, caps))
            }
            cursor.close()
            _scannedAps.value = loadedList
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (scanningJob?.isActive == true) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        context.registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        _isScanning.value = true
        scanningJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                _lastScanTime.value = System.currentTimeMillis()
                wifiManager.startScan()
                delay(10000L)
            }
        }
    }

    fun stopScanning() {
        _isScanning.value = false
        scanningJob?.cancel()
        scanningJob = null
        try {
            context.unregisterReceiver(wifiScanReceiver)
        } catch (e: Exception) {}
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission", "Range")
    private fun processScanResults() {
        val results = wifiManager.scanResults
        val loc = currentLocation ?: return
        val ts = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            val db = dbHelper.writableDatabase
            val updatedMap = _scannedAps.value.associateBy { it.bssid }.toMutableMap()

            for (result in results) {
                val bssid = result.BSSID ?: continue
                val ssid = result.SSID ?: ""
                val rssi = result.level
                val freq = result.frequency
                val caps = result.capabilities ?: ""
                val isSecured = caps.contains("WEP") || caps.contains("WPA") || caps.contains("EAP") || caps.contains("SAE") || caps.contains("OWE")

                val wifiStandard = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    when (result.wifiStandard) {
                        android.net.wifi.ScanResult.WIFI_STANDARD_11AX -> "Wi-Fi 6 (802.11ax)"
                        android.net.wifi.ScanResult.WIFI_STANDARD_11AC -> "Wi-Fi 5 (802.11ac)"
                        android.net.wifi.ScanResult.WIFI_STANDARD_11N -> "Wi-Fi 4 (802.11n)"
                        1 /* WIFI_STANDARD_LEGACY */ -> "Legacy (802.11a/b/g)"
                        8 /* WIFI_STANDARD_11BE on API 33+ */ -> "Wi-Fi 7 (802.11be)"
                        else -> "Unknown"
                    }
                } else {
                    "Unknown"
                }

                val securityType = when {
                    caps.contains("WPA3") || caps.contains("SAE") -> "WPA3"
                    caps.contains("WPA2") -> "WPA2"
                    caps.contains("WPA") -> "WPA"
                    caps.contains("WEP") -> "WEP"
                    caps.contains("OWE") -> "OWE"
                    caps.contains("EAP") -> "EAP"
                    else -> "Open"
                }


                val newWeight = Math.max(1.0, 100.0 + rssi)

                val cursor = db.query(
                    DatabaseHelper.TABLE_APS,
                    arrayOf(DatabaseHelper.COL_EST_LAT, DatabaseHelper.COL_EST_LON, DatabaseHelper.COL_TOTAL_WEIGHT),
                    "${DatabaseHelper.COL_BSSID} = ?",
                    arrayOf(bssid),
                    null, null, null
                )

                var estLat = loc.latitude
                var estLon = loc.longitude
                var totalWeight = newWeight

                if (cursor.moveToFirst()) {
                    val oldLat = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COL_EST_LAT))
                    val oldLon = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COL_EST_LON))
                    val oldWeight = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COL_TOTAL_WEIGHT))

                    totalWeight = oldWeight + newWeight
                    estLat = (oldLat * oldWeight + loc.latitude * newWeight) / totalWeight
                    estLon = (oldLon * oldWeight + loc.longitude * newWeight) / totalWeight
                }
                cursor.close()

                val apValues = ContentValues().apply {
                    put(DatabaseHelper.COL_BSSID, bssid)
                    put(DatabaseHelper.COL_SSID, ssid)
                    put(DatabaseHelper.COL_FREQUENCY, freq)
                    put(DatabaseHelper.COL_EST_LAT, estLat)
                    put(DatabaseHelper.COL_EST_LON, estLon)
                    put(DatabaseHelper.COL_TOTAL_WEIGHT, totalWeight)
                    put(DatabaseHelper.COL_IS_SECURED, if (isSecured) 1 else 0)
                    put(DatabaseHelper.COL_LAST_RSSI, rssi)
                    put(DatabaseHelper.COL_LAST_SEEN, ts)
                    put(DatabaseHelper.COL_SECURITY_TYPE, securityType)
                    put(DatabaseHelper.COL_WIFI_STANDARD, wifiStandard)
                    put(DatabaseHelper.COL_CAPABILITIES, caps)
                }

                db.insertWithOnConflict(DatabaseHelper.TABLE_APS, null, apValues, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)

                val obsValues = ContentValues().apply {
                    put(DatabaseHelper.COL_BSSID, bssid)
                    put(DatabaseHelper.COL_LAT, loc.latitude)
                    put(DatabaseHelper.COL_LON, loc.longitude)
                    put(DatabaseHelper.COL_RSSI, rssi)
                    put(DatabaseHelper.COL_TIMESTAMP, ts)
                }
                db.insert(DatabaseHelper.TABLE_OBS, null, obsValues)

                updatedMap[bssid] = ScannedAp(bssid, ssid, rssi, estLat, estLon, isSecured, totalWeight, securityType, freq, wifiStandard, caps)
            }

            _scannedAps.value = updatedMap.values.sortedByDescending { it.rssi }
        }
    }
}



