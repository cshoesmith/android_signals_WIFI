import re
with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace("val radiusMeters = kotlin.math.max(5.0, 500.0 / kotlin.math.max(10.0, ble.totalWeight))", "val radiusMeters = kotlin.math.max(2.0, 20.0 / kotlin.math.max(1.0, ble.totalWeight))")

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w", encoding="utf-8") as f:
    f.write(text)
