package com.example.androidsignalswifi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "wifi_sniffer.db"
        const val DATABASE_VERSION = 7

        const val TABLE_APS = "access_points"
        const val COL_BSSID = "bssid"
        const val COL_SSID = "ssid"
        const val COL_FREQUENCY = "frequency"
        const val COL_EST_LAT = "est_lat"
        const val COL_EST_LON = "est_lon"
        const val COL_TOTAL_WEIGHT = "total_weight"
        const val COL_LAST_SEEN = "last_seen"
        const val COL_IS_SECURED = "is_secured"
        const val COL_LAST_RSSI = "last_rssi"
        const val COL_SECURITY_TYPE = "security_type"
        const val COL_WIFI_STANDARD = "wifi_standard"
        const val COL_CAPABILITIES = "capabilities"

        const val TABLE_BLE = "ble_devices"
        const val COL_BLE_MAC = "mac"
        const val COL_BLE_NAME = "name"
        const val COL_BLE_TYPE = "device_type"

        const val TABLE_CELLS = "cell_towers"
        const val COL_CELL_ID = "cell_id"
        const val COL_CELL_NETWORK = "network_type"
        const val COL_CELL_MCC_MNC = "mcc_mnc"
        const val COL_CELL_LAC = "lac"
        const val COL_CELL_OWNER = "owner"
        const val COL_CELL_PCI = "pci"
        const val COL_CELL_BAND = "band"

        const val TABLE_OBS = "observations"
        const val COL_ID = "id"
        // COL_BSSID
        const val COL_LAT = "lat"
        const val COL_LON = "lon"
        const val COL_RSSI = "rssi"
        const val COL_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE  (" +
            " TEXT PRIMARY KEY," +
            " TEXT," +
            " INTEGER," +
            " REAL," +
            " REAL," +
            " REAL," +
            " INTEGER," +
            " INTEGER," +
            " TEXT," +
            " TEXT," +
            " TEXT," +
            " INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS  (" +
            " TEXT PRIMARY KEY," +
            " TEXT," +
            " TEXT," +
            " REAL," +
            " REAL," +
            " REAL," +
            " INTEGER," +
            " INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS  (" +
            " TEXT PRIMARY KEY," +
            " TEXT," +
            " TEXT," +
            " TEXT," +
            " TEXT," +
            " TEXT," +
            " TEXT," +
            " REAL," +
            " REAL," +
            " REAL," +
            " INTEGER," +
            " INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE  (" +
            " INTEGER PRIMARY KEY AUTOINCREMENT," +
            " TEXT," +
            " REAL," +
            " REAL," +
            " INTEGER," +
            " INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE  ADD COLUMN  INTEGER DEFAULT 0")
                db.execSQL("ALTER TABLE  ADD COLUMN  INTEGER DEFAULT -100")
            } catch (e: Exception) {}
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE  ADD COLUMN  TEXT DEFAULT ''")
            } catch (e: Exception) {}
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE  ADD COLUMN  TEXT DEFAULT 'Unknown'")
                db.execSQL("ALTER TABLE  ADD COLUMN  TEXT DEFAULT ''")
            } catch (e: Exception) {}
        }
        if (oldVersion < 5) {
            try {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS  (" +
                    " TEXT PRIMARY KEY," +
                    " TEXT," +
                    " TEXT," +
                    " REAL," +
                    " REAL," +
                    " REAL," +
                    " INTEGER," +
                    " INTEGER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS  (" +
                    " TEXT PRIMARY KEY," +
                    " TEXT," +
                    " TEXT," +
                    " REAL," +
                    " REAL," +
                    " REAL," +
                    " INTEGER," +
                    " INTEGER)"
                )
            } catch (e: Exception) {}
        }
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE  ADD COLUMN  TEXT DEFAULT ''")
            } catch (e: Exception) {}
        }
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE  ADD COLUMN  TEXT DEFAULT ''")
                db.execSQL("ALTER TABLE  ADD COLUMN  TEXT DEFAULT ''")
                db.execSQL("ALTER TABLE  ADD COLUMN  TEXT DEFAULT ''")
            } catch (e: Exception) {}
        }
    }

    fun cleanOldData() {
        val oneYearAgo = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 365)
        val db = writableDatabase
        db.delete(TABLE_OBS, " < ?", arrayOf(oneYearAgo.toString()))
        db.delete(TABLE_APS, " < ?", arrayOf(oneYearAgo.toString()))
        try {
            db.delete(TABLE_BLE, " < ?", arrayOf(oneYearAgo.toString()))
            db.delete(TABLE_CELLS, " < ?", arrayOf(oneYearAgo.toString()))
        } catch(e: Exception) {}
    }
}
