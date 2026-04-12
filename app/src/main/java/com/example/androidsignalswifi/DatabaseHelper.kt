package com.example.androidsignalswifi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "wifi_sniffer.db"
        private const val DATABASE_VERSION = 1
        
        const val TABLE_APS = "access_points"
        const val COL_BSSID = "bssid"
        const val COL_SSID = "ssid"
        const val COL_FREQUENCY = "frequency"
        const val COL_EST_LAT = "est_lat"
        const val COL_EST_LON = "est_lon"
        const val COL_TOTAL_WEIGHT = "total_weight"
        const val COL_LAST_SEEN = "last_seen"

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
            "CREATE TABLE $TABLE_APS (" +
            "$COL_BSSID TEXT PRIMARY KEY," +
            "$COL_SSID TEXT," +
            "$COL_FREQUENCY INTEGER," +
            "$COL_EST_LAT REAL," +
            "$COL_EST_LON REAL," +
            "$COL_TOTAL_WEIGHT REAL," +
            "$COL_LAST_SEEN INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE $TABLE_OBS (" +
            "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
            "$COL_BSSID TEXT," +
            "$COL_LAT REAL," +
            "$COL_LON REAL," +
            "$COL_RSSI INTEGER," +
            "$COL_TIMESTAMP INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OBS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APS")
        onCreate(db)
    }

    fun cleanOldData() {
        val oneYearAgo = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 365)
        val db = writableDatabase
        db.delete(TABLE_OBS, "$COL_TIMESTAMP < ?", arrayOf(oneYearAgo.toString()))
        db.delete(TABLE_APS, "$COL_LAST_SEEN < ?", arrayOf(oneYearAgo.toString()))
    }
}
