import re
with open('app/src/main/java/com/example/androidsignalswifi/DatabaseHelper.kt', 'r') as f:
    data = f.read()

data = data.replace('private const val DATABASE_VERSION = 4', 'private const val DATABASE_VERSION = 7')
consts = \"\"\"
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
\"\"\"

data = data.replace('const val TABLE_OBS = "observations"', consts + '\n        const val TABLE_OBS = "observations"')

create_ble = \"\"\"
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
\"\"\"
data = data.replace('        db.execSQL(\n            "CREATE TABLE ', create_ble + '\n        db.execSQL(\n            "CREATE TABLE IF NOT EXISTS ')

upgrade_code = \"\"\"
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
\"\"\"
target = '        if (oldVersion < 4) {\n            try {\n                db.execSQL("ALTER TABLE  ADD COLUMN  TEXT DEFAULT \'Unknown\'")\n                db.execSQL("ALTER TABLE  ADD COLUMN  TEXT DEFAULT \'\'")\n            } catch (e: Exception) {}\n        }'
data = data.replace(target, target + upgrade_code)

data = data.replace('db.delete(TABLE_APS, " < ?", arrayOf(oneYearAgo.toString()))', 'db.delete(TABLE_APS, " < ?", arrayOf(oneYearAgo.toString()))\n        try {\n            db.delete(TABLE_BLE, " < ?", arrayOf(oneYearAgo.toString()))\n            db.delete(TABLE_CELLS, " < ?", arrayOf(oneYearAgo.toString()))\n        } catch(e: Exception) {}')

with open('app/src/main/java/com/example/androidsignalswifi/DatabaseHelper.kt', 'w') as f:
    f.write(data)
