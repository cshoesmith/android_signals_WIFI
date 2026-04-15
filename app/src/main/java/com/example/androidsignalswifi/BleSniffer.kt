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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    private var scanJob: Job? = null
    private val scanOnMs = 10_000L
    private val scanOffMs = 5_000L

    private var stateUpdateJob: Job? = null
    private var stateDirty = false

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

    init {
        loadPersisted()
    }

    private fun loadPersisted() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = dbHelper.readableDatabase
            val cursor = db.query(DatabaseHelper.TABLE_BLE, null, null, null, null, null, null)
            val macIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BLE_MAC)
            val nameIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BLE_NAME)
            val rssiIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LAST_RSSI)
            val latIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EST_LAT)
            val lonIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EST_LON)
            val weightIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TOTAL_WEIGHT)
            val typeIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BLE_TYPE)

            while (cursor.moveToNext()) {
                val mac = cursor.getString(macIdx)
                val ble = ScannedBle(
                    mac = mac,
                    name = cursor.getString(nameIdx),
                    rssi = cursor.getInt(rssiIdx),
                    estLat = cursor.getDouble(latIdx),
                    estLon = cursor.getDouble(lonIdx),
                    totalWeight = cursor.getDouble(weightIdx),
                    deviceType = cursor.getString(typeIdx)
                )
                currentScans[mac] = ble
            }
            cursor.close()
            _scannedBle.value = currentScans.values.toList()
        }
    }

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

    private fun classifyDevice(result: ScanResult): String {
        val record = result.scanRecord

        // 1) Manufacturer-specific data (company ID)
        val mfgData = record?.manufacturerSpecificData
        if (mfgData != null && mfgData.size() > 0) {
            for (i in 0 until mfgData.size()) {
                val companyId = mfgData.keyAt(i)
                val data = mfgData.valueAt(i)
                when (companyId) {
                    0x004C -> { // Apple
                        if (data != null && data.isNotEmpty()) {
                            return when (data[0].toInt() and 0xFF) {
                                0x02 -> "iBeacon"
                                0x05 -> "AirDrop"
                                0x07 -> "AirPods"
                                0x09 -> "AirPlay"
                                0x10 -> "Apple Nearby"
                                0x12 -> "FindMy / AirTag"
                                0x0C -> "Apple Handoff"
                                else -> "Apple Device"
                            }
                        }
                        return "Apple Device"
                    }
                    0x0075 -> return "Samsung Device"
                    0x0006 -> return "Microsoft Device"
                    0x00E0 -> return "Google Device"
                    0x0059 -> return "Nordic Semiconductor"
                    0x0131, 0x038F -> return "Xiaomi Device"
                    0x0157 -> return "Huami / Mi Band"
                    0x0087 -> return "Garmin Device"
                    0x000A -> return "Qualcomm Device"
                    0x0310 -> return "Tile Tracker"
                }
            }
        }

        // 2) GATT service UUIDs
        val serviceUuids = record?.serviceUuids?.map {
            it.uuid.toString().uppercase().substring(4, 8)
        } ?: emptyList()
        for (short in serviceUuids) {
            when (short) {
                "180D" -> return "Heart Rate Monitor"
                "1812" -> return "HID (Keyboard/Mouse)"
                "181A" -> return "Environment Sensor"
                "1810" -> return "Blood Pressure Monitor"
                "1808" -> return "Glucose Meter"
                "1816" -> return "Cycling Sensor"
                "1814" -> return "Running Speed Sensor"
                "181D" -> return "Weight Scale"
                "1809" -> return "Thermometer"
                "FD6F" -> return "Exposure Notification"
                "FE2C" -> return "Google Fast Pair"
                "FEAA" -> return "Eddystone Beacon"
            }
        }

        // 3) Device name heuristics
        val lname = result.device.name?.lowercase() ?: ""
        if (lname.isNotEmpty()) {
            return when {
                lname.contains("watch") -> "Smartwatch"
                lname.contains("band") || lname.contains("fit") -> "Fitness Band"
                lname.contains("buds") || lname.contains("pods") || lname.contains("earbuds") -> "Earbuds"
                lname.contains("headphone") || lname.contains("earphone") -> "Headphones"
                lname.contains("speaker") || lname.contains("soundbar") -> "Speaker"
                lname.contains("tv") || lname.contains("roku") || lname.contains("chromecast") || lname.contains("firestick") -> "TV / Streaming"
                lname.contains("scale") -> "Smart Scale"
                lname.contains("toothbrush") -> "Smart Toothbrush"
                lname.contains("light") || lname.contains("bulb") || lname.contains("lamp") || lname.contains("hue") -> "Smart Light"
                lname.contains("lock") -> "Smart Lock"
                lname.contains("tile") || lname.contains("tag") || lname.contains("tracker") -> "Tracker"
                lname.contains("thermostat") || lname.contains("nest") -> "Thermostat"
                lname.contains("plug") || lname.contains("switch") -> "Smart Plug"
                lname.contains("cam") -> "Camera"
                lname.contains("printer") -> "Printer"
                lname.contains("keyboard") -> "Keyboard"
                lname.contains("mouse") -> "Mouse"
                lname.contains("pen") || lname.contains("stylus") -> "Stylus"
                lname.contains("galaxy") || lname.contains("pixel") || lname.contains("iphone") -> "Phone"
                else -> "BLE Device"
            }
        }

        return "BLE Device"
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val mac = device.address ?: return
        val name = device.name ?: "Unknown"
        val rssi = result.rssi
        val deviceType = classifyDevice(result)

        // Weight updating approach based on RSSI and Location
        val location = currentLocation ?: return
        val lat = location.latitude
        val lon = location.longitude

        val weight = Math.max(1.0, 100.0 + rssi)

        // DB updates on IO thread
        CoroutineScope(Dispatchers.IO).launch {
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
                scheduleStateUpdate()

            } finally {
                db.endTransaction()
            }
        }
    }

    private fun scheduleStateUpdate() {
        stateDirty = true
        if (stateUpdateJob?.isActive == true) return
        stateUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            delay(2000L)
            if (stateDirty) {
                _scannedBle.value = currentScans.values.toList()
                stateDirty = false
            }
        }
    }

    fun startScanning() {
        if (bluetoothLeScanner == null) return
        if (_isScanning.value) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        _isScanning.value = true
        Log.i("BleSniffer", "BLE scan started (interval: ${scanOnMs / 1000}s on / ${scanOffMs / 1000}s off)")

        scanJob = CoroutineScope(Dispatchers.Main).launch {
            while (_isScanning.value) {
                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
                Log.d("BleSniffer", "BLE scan window started")
                delay(scanOnMs)

                bluetoothLeScanner.stopScan(scanCallback)
                Log.d("BleSniffer", "BLE scan window paused")
                delay(scanOffMs)
            }
        }
    }

    fun stopScanning() {
        if (bluetoothLeScanner == null) return
        if (!_isScanning.value) return
        scanJob?.cancel()
        scanJob = null
        bluetoothLeScanner.stopScan(scanCallback)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _isScanning.value = false
        Log.i("BleSniffer", "BLE scan stopped")
    }
}
