import re
with open("app/src/main/java/com/example/androidsignalswifi/CellSniffer.kt", "r") as f:
    content = f.read()

# Fix minimum totalWeight when loading from DB
old_load = """                val totalWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TOTAL_WEIGHT))"""
new_load = """                val totalWeight = kotlin.math.max(1.0, cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TOTAL_WEIGHT)))"""
content = content.replace(old_load, new_load)

with open("app/src/main/java/com/example/androidsignalswifi/CellSniffer.kt", "w") as f:
    f.write(content)
