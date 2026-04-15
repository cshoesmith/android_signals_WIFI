package com.example.androidsignalswifi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ScannedBle(
    val mac: String,
    val name: String,
    val rssi: Int,
    val estLat: Double,
    val estLon: Double,
    val totalWeight: Double,
    val deviceType: String
)

@SuppressLint("MissingPermission")
class BleSniffer(private val context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val _scannedBle = MutableStateFlow<List<ScannedBle>>(emptyList())
    val scannedBle: StateFlow<List<ScannedBle>> = _scannedBle

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    var currentLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            currentLocation = locationResult.lastLocation
        }
    }

    private val currentScans = mutableMapOf<String, ScannedBle>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { handleScanResult(it) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleSniffer", "Scan failed: $errorCode")
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val mac = device.address ?: return
        val name = device.name ?: "Unknown"
        val rssi = result.rssi
        val deviceType = result.scanRecord?.deviceName ?: "Unknown Type"

        // Weight updating approach based on RSSI and Location
        val location = currentLocation ?: return
        val lat = location.latitude
        val lon = location.longitude

        val weight = Math.pow(10.0, rssi / 20.0)

        // DB updates
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Check if exists
            val cursor = db.query(
                DatabaseHelper.TABLE_BLE,
                arrayOf(DatabaseHelper.COL_EST_LAT, DatabaseHelper.COL_EST_LON, DatabaseHelper.COL_TOTAL_WEIGHT),
                "${DatabaseHelper.COL_BLE_MAC} = ?",
                arrayOf(mac),
                null, null, null
            )

            var newEstLat = lat
            var newEstLon = lon
            var newTotalWeight = weight

            if (cursor.moveToFirst()) {
                val oldLat = cursor.getDouble(0)
                val oldLon = cursor.getDouble(1)
                val oldWeight = cursor.getDouble(2)

                newTotalWeight = oldWeight + weight
                newEstLat = (oldLat * oldWeight + lat * weight) / newTotalWeight
                newEstLon = (oldLon * oldWeight + lon * weight) / newTotalWeight
            }
            cursor.close()

            val values = ContentValues().apply {
                put(DatabaseHelper.COL_BLE_MAC, mac)
                put(DatabaseHelper.COL_BLE_NAME, name)
                put(DatabaseHelper.COL_EST_LAT, newEstLat)
                put(DatabaseHelper.COL_EST_LON, newEstLon)
                put(DatabaseHelper.COL_TOTAL_WEIGHT, newTotalWeight)
                put(DatabaseHelper.COL_LAST_RSSI, rssi)
                put(DatabaseHelper.COL_BLE_TYPE, deviceType)
                put(DatabaseHelper.COL_LAST_SEEN, System.currentTimeMillis())
            }

            db.insertWithOnConflict(DatabaseHelper.TABLE_BLE, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)

            val obsValues = ContentValues().apply {
                put(DatabaseHelper.COL_BSSID, mac)
                put(DatabaseHelper.COL_LAT, lat)
                put(DatabaseHelper.COL_LON, lon)
                put(DatabaseHelper.COL_RSSI, rssi)
                put(DatabaseHelper.COL_TIMESTAMP, System.currentTimeMillis())
            }
            db.insert(DatabaseHelper.TABLE_OBS, null, obsValues)
            db.setTransactionSuccessful()

            val scanned = ScannedBle(mac, name, rssi, newEstLat, newEstLon, newTotalWeight, deviceType)
            currentScans[mac] = scanned
            _scannedBle.value = currentScans.values.toList()

        } finally {
            db.endTransaction()
        }
    }

    fun startScanning() {
        if (bluetoothLeScanner == null) return
        if (_isScanning.value) return

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
            
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
        _isScanning.value = true
        Log.i("BleSniffer", "BLE scan started")
    }

    fun stopScanning() {
        if (bluetoothLeScanner == null) return
        if (!_isScanning.value) return
        bluetoothLeScanner.stopScan(scanCallback)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _isScanning.value = false
        Log.i("BleSniffer", "BLE scan stopped")
    }
}
