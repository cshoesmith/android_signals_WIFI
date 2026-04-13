package com.example.androidsignalswifi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "wifi_sniffer.db"
        private const val DATABASE_VERSION = 4

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
            "CREATE TABLE ${TABLE_APS} (" +
            "${COL_BSSID} TEXT PRIMARY KEY," +
            "${COL_SSID} TEXT," +
            "${COL_FREQUENCY} INTEGER," +
            "${COL_EST_LAT} REAL," +
            "${COL_EST_LON} REAL," +
            "${COL_TOTAL_WEIGHT} REAL," +
            "${COL_IS_SECURED} INTEGER," +
            "${COL_LAST_RSSI} INTEGER," +
            "${COL_SECURITY_TYPE} TEXT," +
            "${COL_WIFI_STANDARD} TEXT," +
            "${COL_CAPABILITIES} TEXT," +
            "${COL_LAST_SEEN} INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE ${TABLE_OBS} (" +
            "${COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
            "${COL_BSSID} TEXT," +
            "${COL_LAT} REAL," +
            "${COL_LON} REAL," +
            "${COL_RSSI} INTEGER," +
            "${COL_TIMESTAMP} INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE ${TABLE_APS} ADD COLUMN ${COL_IS_SECURED} INTEGER DEFAULT 0")
                db.execSQL("ALTER TABLE ${TABLE_APS} ADD COLUMN ${COL_LAST_RSSI} INTEGER DEFAULT -100")
            } catch (e: Exception) {}
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE ${TABLE_APS} ADD COLUMN ${COL_SECURITY_TYPE} TEXT DEFAULT ''")
            } catch (e: Exception) {}
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE ${TABLE_APS} ADD COLUMN ${COL_WIFI_STANDARD} TEXT DEFAULT 'Unknown'")
                db.execSQL("ALTER TABLE ${TABLE_APS} ADD COLUMN ${COL_CAPABILITIES} TEXT DEFAULT ''")
            } catch (e: Exception) {}
        }
    }

    fun cleanOldData() {
        val oneYearAgo = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 365)
        val db = writableDatabase
        db.delete(TABLE_OBS, "${COL_TIMESTAMP} < ?", arrayOf(oneYearAgo.toString()))
        db.delete(TABLE_APS, "${COL_LAST_SEEN} < ?", arrayOf(oneYearAgo.toString()))
    }
}
