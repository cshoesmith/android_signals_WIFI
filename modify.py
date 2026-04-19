import re
with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r") as f: content = f.read()

# 1. Add openTooltipId state
content = content.replace("var initialCenterSet by remember { mutableStateOf(false) }",
    "var initialCenterSet by remember { mutableStateOf(false) }\n    var openTooltipId by remember { mutableStateOf<String?>(null) }")

# 2. Add click handler for Wi-Fi Polygons
wifi_poly_old = """                val circle = Polygon(mapView).apply {
                    points = Polygon.pointsAsCircle(point, radiusMeters)
                    fillPaint.color = Color.argb(100, r, g, b)
                    outlinePaint.color = Color.argb(255, r, g, b)
                    outlinePaint.strokeWidth = 3.0f
                    title = titleText
                    snippet = snippetText
                }"""
wifi_poly_new = """                val identifier = primaryAp.bssid
                val circle = Polygon(mapView).apply {
                    id = identifier
                    points = Polygon.pointsAsCircle(point, radiusMeters)
                    fillPaint.color = Color.argb(100, r, g, b)
                    outlinePaint.color = Color.argb(255, r, g, b)
                    outlinePaint.strokeWidth = 3.0f
                    title = titleText
                    snippet = snippetText
                    setOnClickListener { polygon, mv, pos ->
                        openTooltipId = if (openTooltipId == identifier) null else identifier
                        true
                    }
                }"""
content = content.replace(wifi_poly_old, wifi_poly_new)

# WiFi marker
wifi_marker_old = """                    val marker = Marker(mapView).apply {
                        position = point
                        title = circle.title
                        snippet = circle.snippet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                        val selectedIcon = if (!isSecured) {"""
wifi_marker_new = """                    val marker = Marker(mapView).apply {
                        id = identifier
                        position = point
                        title = circle.title
                        snippet = circle.snippet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setOnMarkerClickListener { m, mv ->
                            openTooltipId = if (openTooltipId == identifier) null else identifier
                            true
                        }

                        val selectedIcon = if (!isSecured) {"""
content = content.replace(wifi_marker_old, wifi_marker_new)

# Show open wifi tooltip
content = content.replace("mapView.overlays.add(circle)\n\n                if (primaryAp.totalWeight > 200.0) {",
                          "mapView.overlays.add(circle)\n                if (openTooltipId == identifier) mapView.post { circle.showInfoWindow() }\n\n                if (primaryAp.totalWeight > 200.0) {")
content = content.replace("mapView.overlays.add(marker)\n                }",
                          "mapView.overlays.add(marker)\n                    if (openTooltipId == identifier) mapView.post { marker.showInfoWindow() }\n                }")

# 3. Add click handler for BLE Polygons
ble_poly_old = """                val circle = Polygon(mapView).apply {
                    points = Polygon.pointsAsCircle(point, radiusMeters)
                    fillPaint.color = Color.argb(100, 128, 0, 128) // Purple for BLE
                    outlinePaint.color = Color.argb(255, 128, 0, 128)
                    outlinePaint.strokeWidth = 3.0f
                    title = "BLE: ${ble.name.takeIf { it != "Unknown" } ?: ble.deviceType}"
                    snippet = "MAC: ${ble.mac}\\nLocation Wt: ${ble.totalWeight.toInt()}"
                }"""
ble_poly_new = """                val identifier = ble.mac
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
                }"""
content = content.replace(ble_poly_old, ble_poly_new)

ble_marker_old = """                    val marker = Marker(mapView).apply {
                        position = point
                        title = circle.title
                        snippet = circle.snippet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        if (bleIcon != null) {"""
ble_marker_new = """                    val marker = Marker(mapView).apply {
                        id = identifier
                        position = point
                        title = circle.title
                        snippet = circle.snippet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setOnMarkerClickListener { m, mv ->
                            openTooltipId = if (openTooltipId == identifier) null else identifier
                            true
                        }
                        if (bleIcon != null) {"""
content = content.replace(ble_marker_old, ble_marker_new)

content = content.replace("mapView.overlays.add(circle)\n\n                if (ble.totalWeight > 20.0) {",
                          "mapView.overlays.add(circle)\n                if (openTooltipId == identifier) mapView.post { circle.showInfoWindow() }\n\n                if (ble.totalWeight > 20.0) {")

content = content.replace("mapView.overlays.add(marker)\n                }",
                          "mapView.overlays.add(marker)\n                    if (openTooltipId == identifier) mapView.post { marker.showInfoWindow() }\n                }")

# 4. Add click handler for Cell Polygons
cell_poly_old = """                val circle = Polygon(mapView).apply {
                    points = Polygon.pointsAsCircle(point, radiusMeters)
                    fillPaint.color = Color.argb(60, 255, 69, 0) // Red-orange for Cellular
                    outlinePaint.color = Color.argb(200, 255, 69, 0)
                    outlinePaint.strokeWidth = 3.0f
                    title = "${cell.owner} ${cell.networkType} Cell: ${cell.cellId}"
                    snippet = "MCC/MNC: ${cell.mccMnc}\\nLAC/TAC: ${cell.lac}\\nPCI: ${cell.pci}\\nBand: ${cell.band}\\nLocation Wt: ${cell.totalWeight.toInt()}"
                }"""
cell_poly_new = """                val identifier = cell.cellId
                val circle = Polygon(mapView).apply {
                    id = identifier
                    points = Polygon.pointsAsCircle(point, radiusMeters)
                    fillPaint.color = Color.argb(60, 255, 69, 0) // Red-orange for Cellular
                    outlinePaint.color = Color.argb(200, 255, 69, 0)
                    outlinePaint.strokeWidth = 3.0f
                    title = "${cell.owner} ${cell.networkType} Cell: ${cell.cellId}"
                    snippet = "MCC/MNC: ${cell.mccMnc}\\nLAC/TAC: ${cell.lac}\\nPCI: ${cell.pci}\\nBand: ${cell.band}\\nLocation Wt: ${cell.totalWeight.toInt()}"
                    setOnClickListener { polygon, mv, pos ->
                        openTooltipId = if (openTooltipId == identifier) null else identifier
                        true
                    }
                }"""
content = content.replace(cell_poly_old, cell_poly_new)

cell_marker_old = """                val marker = Marker(mapView).apply {
                    position = point
                    title = circle.title
                    snippet = circle.snippet
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // Anchor bottom for tower
                    if (cellIcon != null) {"""
cell_marker_new = """                val marker = Marker(mapView).apply {
                    id = identifier
                    position = point
                    title = circle.title
                    snippet = circle.snippet
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // Anchor bottom for tower
                    setOnMarkerClickListener { m, mv ->
                        openTooltipId = if (openTooltipId == identifier) null else identifier
                        true
                    }
                    if (cellIcon != null) {"""
content = content.replace(cell_marker_old, cell_marker_new)

content = content.replace("mapView.overlays.add(circle)\n\n                // Always draw cell tower markers,",
                          "mapView.overlays.add(circle)\n                if (openTooltipId == identifier) mapView.post { circle.showInfoWindow() }\n\n                // Always draw cell tower markers,")

content = content.replace("mapView.overlays.add(marker)\n            }",
                          "mapView.overlays.add(marker)\n                if (openTooltipId == identifier) mapView.post { marker.showInfoWindow() }\n            }")


with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w") as f: f.write(content)
