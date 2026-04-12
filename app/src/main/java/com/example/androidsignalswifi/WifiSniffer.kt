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
    val estLon: Double
)

class WifiSniffer(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val _scannedAps = MutableStateFlow<List<ScannedAp>>(emptyList())
    val scannedAps: StateFlow<List<ScannedAp>> = _scannedAps

    private var scanningJob: Job? = null
    private var currentLocation: Location? = null

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

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (scanningJob?.isActive == true) return

        // Start location updates
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        // Register Wi-Fi receiver
        context.registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        // Start scanning loop
        scanningJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val success = wifiManager.startScan()
                Log.d("WifiSniffer", "startScan() initiated: $success")
                delay(10000L) // Request scan every 10 seconds
            }
        }
    }

    fun stopScanning() {
        scanningJob?.cancel()
        scanningJob = null
        try {
            context.unregisterReceiver(wifiScanReceiver)
        } catch (e: Exception) {
            Log.e("WifiSniffer", "Receiver not registered", e)
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission", "Range")
    private fun processScanResults() {
        val results = wifiManager.scanResults
        val loc = currentLocation ?: return
        val ts = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            val db = dbHelper.writableDatabase
            val updatedList = mutableListOf<ScannedAp>()

            for (result in results) {
                val bssid = result.BSSID ?: continue
                val ssid = result.SSID ?: ""
                val rssi = result.level
                val freq = result.frequency

                // Calculate weight based on RSSI (e.g., -40 dBm -> high weight, -100 dBm -> low weight)
                // Normalize RSSI to a scale of roughly 1 to 60 for weight
                val newWeight = Math.max(1.0, 100.0 + rssi) 

                // Fetch existing AP from DB
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

                // Insert or update AP
                val apValues = ContentValues().apply {
                    put(DatabaseHelper.COL_BSSID, bssid)
                    put(DatabaseHelper.COL_SSID, ssid)
                    put(DatabaseHelper.COL_FREQUENCY, freq)
                    put(DatabaseHelper.COL_EST_LAT, estLat)
                    put(DatabaseHelper.COL_EST_LON, estLon)
                    put(DatabaseHelper.COL_TOTAL_WEIGHT, totalWeight)
                    put(DatabaseHelper.COL_LAST_SEEN, ts)
                }
                
                db.insertWithOnConflict(
                    DatabaseHelper.TABLE_APS, 
                    null, 
                    apValues, 
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )

                // Insert observation
                val obsValues = ContentValues().apply {
                    put(DatabaseHelper.COL_BSSID, bssid)
                    put(DatabaseHelper.COL_LAT, loc.latitude)
                    put(DatabaseHelper.COL_LON, loc.longitude)
                    put(DatabaseHelper.COL_RSSI, rssi)
                    put(DatabaseHelper.COL_TIMESTAMP, ts)
                }
                db.insert(DatabaseHelper.TABLE_OBS, null, obsValues)

                updatedList.add(ScannedAp(bssid, ssid, rssi, estLat, estLon))
            }
            
            _scannedAps.value = updatedList.sortedByDescending { it.rssi }
        }
    }
}
