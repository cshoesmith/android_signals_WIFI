import re
with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r", encoding="utf-8") as f:
    text = f.read()

old_block = """            for (ble in bles) {
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

new_block = """            for (ble in bles) {
                val point = GeoPoint(ble.estLat, ble.estLon)
                if (centerPoint == null) centerPoint = point

                val identifier = ble.mac

                val marker = Marker(mapView).apply {
                    id = identifier
                    position = point
                    title = "BLE: ${ble.name.takeIf { it != "Unknown" } ?: ble.deviceType}"
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
            }"""

text = text.replace(old_block, new_block)

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w", encoding="utf-8") as f:
    f.write(text)
