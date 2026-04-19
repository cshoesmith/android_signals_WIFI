import re
with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r", encoding="utf-8") as f:
    text = f.read()

s_old = """            for (ble in bles) {
                val point = GeoPoint(ble.estLat, ble.estLon)
                if (centerPoint == null) centerPoint = point

                val radiusMeters = kotlin.math.max(2.0, 20.0 / kotlin.math.max(1.0, ble.totalWeight))

                val identifier = ble.mac
                val circle = Polygon(mapView).apply {
                    id = identifier
                    points = Polygon.pointsAsCircle(point, radiusMeters)
                    fillPaint.color = Color.argb(100, 128, 0, 128) // Purple for BLE
                    outlinePaint.color = Color.argb(255, 128, 0, 128)
                    outlinePaint.strokeWidth = 3.0f
                    title = "BLE: ${ble.name.takeIf { it != "Unknown" } ?: ble.deviceType}"
                    snippet = "MAC: ${ble.mac}\\nLocation Wt: ${ble.totalWeight.toInt()}"
                    setOnClickListener { polygon, mv, pos ->
                        openTooltipId = if (openTooltipId == identifier) null else identifier
                        true
                    }
                }
                mapView.overlays.add(circle)
                if (openTooltipId == identifier && ble.totalWeight <= 20.0) circle.showInfoWindow()

                if (ble.totalWeight > 20.0) {
                    val marker = Marker(mapView).apply {
                        id = identifier
                        position = point
                        title = circle.title
                        snippet = circle.snippet
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
            }"""

s_new = """            for (ble in bles) {
                val point = GeoPoint(ble.estLat, ble.estLon)
                if (centerPoint == null) centerPoint = point

                val identifier = ble.mac
                val titleText = "BLE: ${ble.name.takeIf { it != "Unknown" } ?: ble.deviceType}"
                val snippetText = "MAC: ${ble.mac}\\nLocation Wt: ${ble.totalWeight.toInt()}"

                val marker = Marker(mapView).apply {
                    id = identifier
                    position = point
                    title = titleText
                    snippet = snippetText
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
            }"""
        
# Fix escaped characters for proper matching
s_old = s_old.replace("\\\\n", "\\n")
s_new = s_new.replace("\\\\n", "\\n")

text = text.replace(s_old, s_new)

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w", encoding="utf-8") as f:
    f.write(text)
