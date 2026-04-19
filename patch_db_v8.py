import re

with open('app/src/main/java/com/example/androidsignalswifi/DatabaseHelper.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Normalize line endings
text = text.replace('\r\n', '\n')

# Bump version to 8
text = text.replace('const val DATABASE_VERSION = 7', 'const val DATABASE_VERSION = 8')

# Find the end of onUpgrade to add version 8 migration
create_ble_and_cells = \"\"\"        if (oldVersion < 8) {
            try {
                db.execSQL("DROP TABLE IF EXISTS ")
                db.execSQL("DROP TABLE IF EXISTS ")
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
            } catch (e: Exception) {}
        }
    }

    fun cleanOldData\"\"\"

text = text.replace('    }\n\n    fun cleanOldData', create_ble_and_cells)

with open('app/src/main/java/com/example/androidsignalswifi/DatabaseHelper.kt', 'w', encoding='utf-8') as f:
    f.write(text)
