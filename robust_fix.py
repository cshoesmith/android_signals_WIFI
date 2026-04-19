import re

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r", encoding="utf-8") as f:
    text = f.read()

pattern = r"for \(ble in bles\) \{.*?for \(cell in cells\) \{"
replacement = """for (ble in bles) {
                val point = GeoPoint(ble.estLat, ble.estLon)
                if (centerPoint == null) centerPoint = point

                val identifier = ble.mac

                val marker = Marker(mapView).apply {
                    id = identifier
                    position = point
                    title = "BLE: ${ble.name.takeIf { it != \\"Unknown\\" } ?: ble.deviceType}"
                    snippet = "MAC: ${ble.mac}\\nLocation Wt: ${ble.totalWeight.toInt()}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { m, mv ->
                        openTooltipId = if (openTooltipId == identifier) null else identifier
                        true
                    }
                    if (bleBmp != null) {
                        icon = BitmapDrawable(context.resources, bleBmp)
                    }
                }
                mapView.overlays.add(marker)
                if (openTooltipId == identifier) marker.showInfoWindow()
            }

            for (cell in cells) {"""

new_text = re.sub(pattern, replacement, text, flags=re.DOTALL)

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w", encoding="utf-8") as f:
    f.write(new_text)
