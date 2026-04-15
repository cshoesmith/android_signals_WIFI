package com.example.androidsignalswifi

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.location.Location
import android.os.Looper
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.CellInfoNr
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ScannedCell(
    val cellId: String,
    val networkType: String,
    val mccMnc: String,
    val rssi: Int,
    val estLat: Double,
    val estLon: Double,
    val totalWeight: Double,
    val lac: String = "",
    val owner: String = "",
    val pci: String = "",
    val band: String = ""
)

class CellSniffer(private val context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val _scannedCells = MutableStateFlow<List<ScannedCell>>(emptyList())
    val scannedCells: StateFlow<List<ScannedCell>> = _scannedCells

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var scanningJob: Job? = null
    var currentLocation: Location? = null

    private val currentScans = mutableMapOf<String, ScannedCell>()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            currentLocation = locationResult.lastLocation
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (_isScanning.value) return
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        _isScanning.value = true
        scanningJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val currentLoc = currentLocation
                if (currentLoc != null) {
                    try {
                        val allCellInfo = telephonyManager.allCellInfo
                        if (allCellInfo != null) {
                            processCells(allCellInfo, currentLoc)
                        }
                    } catch (e: Exception) {
                        Log.e("CellSniffer", "Error getting cell info", e)
                    }
                }
                delay(10000) // Scan every 10 seconds
            }
        }
    }

    fun stopScanning() {
        scanningJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _isScanning.value = false
    }

    private fun processCells(cells: List<CellInfo>, location: Location) {
        val lat = location.latitude
        val lon = location.longitude

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            cells.forEach { cell ->
                if (!cell.isRegistered) return@forEach // Only store cells the device is registered to or ignore? Let's track everything.

                var cellId = ""
                var networkType = ""
                var mccMnc = ""
                var lac = ""
                var rssi = 0

                when (cell) {
                    is CellInfoLte -> {
                        val identity = cell.cellIdentity
                        cellId = "${identity.ci}"
                        networkType = "LTE"
                        mccMnc = "${identity.mccString}${identity.mncString}"
                        lac = "${identity.tac}"
                        rssi = cell.cellSignalStrength.dbm
                    }
                    is CellInfoGsm -> {
                        val identity = cell.cellIdentity
                        cellId = "${identity.cid}"
                        networkType = "GSM"
                        mccMnc = "${identity.mccString}${identity.mncString}"
                        lac = "${identity.lac}"
                        rssi = cell.cellSignalStrength.dbm
                    }
                    is CellInfoWcdma -> {
                        val identity = cell.cellIdentity
                        cellId = "${identity.cid}"
                        networkType = "WCDMA"
                        mccMnc = "${identity.mccString}${identity.mncString}"
                        lac = "${identity.lac}"
                        rssi = cell.cellSignalStrength.dbm
                    }
                    is CellInfoNr -> {
                        val identity = cell.cellIdentity as CellIdentityNr
                        cellId = "${identity.nci}"
                        networkType = "NR (5G)"
                        mccMnc = "${identity.mccString}${identity.mncString}"
                        lac = "${identity.tac}"
                        rssi = cell.cellSignalStrength.dbm
                    }
                }

                if (cellId.isEmpty() || cellId == "2147483647") return@forEach // Invalid

                val weight = Math.pow(10.0, rssi / 20.0)

                val cursor = db.query(
                    DatabaseHelper.TABLE_CELLS,
                    arrayOf(DatabaseHelper.COL_EST_LAT, DatabaseHelper.COL_EST_LON, DatabaseHelper.COL_TOTAL_WEIGHT),
                    "${DatabaseHelper.COL_CELL_ID} = ?",
                    arrayOf(cellId),
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
                    put(DatabaseHelper.COL_CELL_ID, cellId)
                    put(DatabaseHelper.COL_CELL_NETWORK, networkType)
                    put(DatabaseHelper.COL_CELL_MCC_MNC, mccMnc)
                    put(DatabaseHelper.COL_CELL_LAC, lac)
                    put(DatabaseHelper.COL_EST_LAT, newEstLat)
                    put(DatabaseHelper.COL_EST_LON, newEstLon)
                    put(DatabaseHelper.COL_TOTAL_WEIGHT, newTotalWeight)
                    put(DatabaseHelper.COL_LAST_RSSI, rssi)
                    put(DatabaseHelper.COL_LAST_SEEN, System.currentTimeMillis())
                }
                
                db.insertWithOnConflict(DatabaseHelper.TABLE_CELLS, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)

                val obsValues = ContentValues().apply {
                    put(DatabaseHelper.COL_BSSID, cellId) // Using shared BSSID column for id matching
                    put(DatabaseHelper.COL_LAT, lat)
                    put(DatabaseHelper.COL_LON, lon)
                    put(DatabaseHelper.COL_RSSI, rssi)
                    put(DatabaseHelper.COL_TIMESTAMP, System.currentTimeMillis())
                }
                db.insert(DatabaseHelper.TABLE_OBS, null, obsValues)

                val scanned = ScannedCell(cellId, networkType, mccMnc, rssi, newEstLat, newEstLon, newTotalWeight, lac)
                currentScans[cellId] = scanned
            }
            db.setTransactionSuccessful()
            _scannedCells.value = currentScans.values.toList()
        } finally {
            db.endTransaction()
        }
    }
}
