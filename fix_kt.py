import re

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r", encoding="utf-8") as f:
    text = f.read()

# Fix the broken multiline string & syntax issue in MapScreen.kt
bad_code = """                  val marker = Marker(mapView).apply {
                      id = identifier
                      position = point
                      title = "BLE: ${ble.name.takeIf { it != \\"Unknown\\" } ?:  
ble.deviceType}"
                      snippet = "MAC: ${ble.mac}
  Location Wt: ${ble.totalWeight.toInt()}"
                      setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)     
                      setOnMarkerClickListener { m, mv ->"""

fixed_code = """                  val marker = Marker(mapView).apply {
                      id = identifier
                      position = point
                      val fixedName = if(ble.name != "Unknown") ble.name else ble.deviceType
                      title = "BLE: $fixedName"
                      snippet = "MAC: ${ble.mac}\\nLocation Wt: ${ble.totalWeight.toInt()}"
                      setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)     
                      setOnMarkerClickListener { m, mv ->"""

# Quick block replace
pattern = r"val marker = Marker\(mapView\)\.apply \{.*?setOnMarkerClickListener \{ m, mv ->"
new_text = re.sub(pattern, fixed_code.replace("  ", " "), text, flags=re.DOTALL)

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w", encoding="utf-8") as f:
    f.write(new_text)
