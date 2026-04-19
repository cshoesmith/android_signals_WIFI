import re
with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

s_old = """            val point = GeoPoint(ble.estLat, ble.estLon)
            if (centerPoint == null) centerPoint = point
            
            val radiusMeters = kotlin.math.max(5.0, 500.0 / kotlin.math.max(10.0, ble.totalWeight))
            
            val identifier = ble.mac"""

s_new = """            val point = GeoPoint(ble.estLat, ble.estLon)
            if (centerPoint == null) centerPoint = point
            
            val radiusMeters = kotlin.math.max(2.0, 100.0 / kotlin.math.max(2.0, ble.totalWeight))
            
            val identifier = ble.mac"""

content = content.replace(s_old, s_new)

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)
