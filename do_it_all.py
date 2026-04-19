import re

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r", encoding="utf-8") as f:
    text = f.read()

# 1. Add tooltip ID, interaction tracker to declarations
s1 = """    var mapViewState by remember { mutableStateOf<MapView?>(null) }
    var initialCenterSet by remember { mutableStateOf(false) }"""
r1 = """    var mapViewState by remember { mutableStateOf<MapView?>(null) }
    var initialCenterSet by remember { mutableStateOf(false) }
    var openTooltipId by remember { mutableStateOf<String?>(null) }
    val lastManualMoveTime = remember { longArrayOf(0L) }"""
text = text.replace(s1, r1)

# 2. Reset time when centerTrigger fires
s2 = """    LaunchedEffect(centerTrigger) {
        if (centerTrigger > 0) {
            val mv = mapViewState"""
r2 = """    LaunchedEffect(centerTrigger) {
        if (centerTrigger > 0) {
            lastManualMoveTime[0] = 0L
            val mv = mapViewState"""
text = text.replace(s2, r2)

# 3. Add gesture listener
s3 = """        factory = { ctx ->
            MapView(ctx).apply {
                mapViewState = this
                setBuiltInZoomControls(false)
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(18.0)
            }
        },"""
r3 = """        factory = { ctx ->
            MapView(ctx).apply {
                mapViewState = this
                setBuiltInZoomControls(false)
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(18.0)
                setOnTouchListener { _, _ ->
                    lastManualMoveTime[0] = System.currentTimeMillis()
                    false
                }
            }
        },"""
text = text.replace(s3, r3)

# 4. Modify update = { mapView -> to include the 10s wait and closeAll windows
s4 = """        update = { mapView ->
            mapView.overlays.clear()

            var centerPoint: GeoPoint? = null"""
r4 = """        update = { mapView ->
            if (System.currentTimeMillis() - lastManualMoveTime[0] < 10000L) {
                return@AndroidView
            }

            org.osmdroid.views.overlay.InfoWindow.closeAllInfoWindowsOn(mapView)
            mapView.overlays.clear()

            var centerPoint: GeoPoint? = null"""
text = text.replace(s4, r4)

# 5. Change getBitmapDrawable to getBitmap so Compose does not reuse bounds
s5_1 = """fun getBitmapDrawable(context: android.content.Context, id: Int): BitmapDrawable? {
    val drawable = ContextCompat.getDrawable(context, id) ?: return null        
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return BitmapDrawable(context.resources, bitmap)
}"""
r5_1 = """fun getBitmap(context: android.content.Context, id: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, id) ?: return null
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}"""
text = text.replace(s5_1, r5_1)

s5_2 = """            val wifiIconBlue = getBitmapDrawable(context, R.drawable.ic_cloud_wifi_blue)
            val wifiIconLightGreen = getBitmapDrawable(context, R.drawable.ic_cloud_wifi_light_green)
            val wifiIconDarkGreen = getBitmapDrawable(context, R.drawable.ic_cloud_wifi_dark_green)
            val bleIcon = getBitmapDrawable(context, R.drawable.ic_ble_device)  
            val cellIcon = getBitmapDrawable(context, R.drawable.ic_cell_tower)"""
r5_2 = """            val wifiBlueBmp = getBitmap(context, R.drawable.ic_cloud_wifi_blue)
            val wifiLightGreenBmp = getBitmap(context, R.drawable.ic_cloud_wifi_light_green)
            val wifiDarkGreenBmp = getBitmap(context, R.drawable.ic_cloud_wifi_dark_green)
            val bleBmp = getBitmap(context, R.drawable.ic_ble_device)
            val cellBmp = getBitmap(context, R.drawable.ic_cell_tower)"""
text = text.replace(s5_2, r5_2)

# 6. For APs: add click handlers, use new bitmap references
text = re.sub(r"""val circle = Polygon\(mapView\)\.apply \{.*?mapView\.overlays\.add\(circle\)""",
r"""val identifier = primaryAp.bssid
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
                }
                mapView.overlays.add(circle)
                if (openTooltipId == identifier && primaryAp.totalWeight <= 200.0) circle.showInfoWindow()""", text, flags=re.DOTALL)

text = re.sub(r"""val marker = Marker\(mapView\)\.apply \{.*?mapView\.overlays\.add\(marker\)""",
r"""val marker = Marker(mapView).apply {
                        id = identifier
                        position = point
                        title = circle.title
                        snippet = circle.snippet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setOnMarkerClickListener { m, mv ->
                            openTooltipId = if (openTooltipId == identifier) null else identifier
                            true
                        }

                        val selectedBmp = if (!isSecured) {
                            wifiLightGreenBmp
                        } else if (isWep) {
                            wifiDarkGreenBmp
                        } else {
                            wifiBlueBmp
                        }

                        if (selectedBmp != null) {
                            icon = BitmapDrawable(context.resources, selectedBmp)
                        }
                    }
                    mapView.overlays.add(marker)
                    if (openTooltipId == identifier) marker.showInfoWindow()""", text, flags=re.DOTALL, count=1)

# 7. For BLEs: Completely overwrite the BLE loop to just draw markers (no circles)
ble_old = r"""for \(ble in bles\) \{.*?for \(cell in cells\) \{"""
ble_new = """for (ble in bles) {
                val point = GeoPoint(ble.estLat, ble.estLon)
                if (centerPoint == null) centerPoint = point

                val identifier = ble.mac
                val fixedName = if(ble.name != "Unknown") ble.name else ble.deviceType
                
                val marker = Marker(mapView).apply {
                    id = identifier
                    position = point
                    title = "BLE: $fixedName"
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
text = re.sub(ble_old, ble_new, text, flags=re.DOTALL)

# 8. For Cells: update click handlers and icons
cell_sub_pattern = r"""val circle = Polygon\(mapView\)\.apply \{.*?mapView\.overlays\.add\(marker\)"""
cell_sub_repl = """val identifier = cell.cellId
                val circle = Polygon(mapView).apply {
                    id = identifier
                    points = Polygon.pointsAsCircle(point, radiusMeters)
                    fillPaint.color = Color.argb(60, 255, 69, 0)
                    outlinePaint.color = Color.argb(200, 255, 69, 0)
                    outlinePaint.strokeWidth = 3.0f
                    title = "${cell.owner} ${cell.networkType} Cell: ${cell.cellId}"
                    snippet = "MCC/MNC: ${cell.mccMnc}\\nLAC/TAC: ${cell.lac}\\nPCI: ${cell.pci}\\nBand: ${cell.band}\\nLocation Wt: ${cell.totalWeight.toInt()}"
                    setOnClickListener { polygon, mv, pos ->
                        openTooltipId = if (openTooltipId == identifier) null else identifier
                        true
                    }
                }
                mapView.overlays.add(circle)

                val marker = Marker(mapView).apply {
                    id = identifier
                    position = point
                    title = circle.title
                    snippet = circle.snippet
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { m, mv ->
                        openTooltipId = if (openTooltipId == identifier) null else identifier
                        true
                    }
                    if (cellBmp != null) {
                        icon = BitmapDrawable(context.resources, cellBmp)
                    }
                }

                mapView.overlays.add(marker)
                if (openTooltipId == identifier) marker.showInfoWindow()"""
text = re.sub(cell_sub_pattern, cell_sub_repl, text, flags=re.DOTALL)


# 9. Update the auto center logic if scanning
s_pause = """            // Only set center on the VERY FIRST load
            if (!initialCenterSet && centerPoint != null) {"""
r_pause = """            if (isScanning && currentLocation != null) {
                // Continuously keep the map centered when scanning is active
                mapView.controller.setCenter(GeoPoint(currentLocation.latitude, currentLocation.longitude))
                initialCenterSet = true
            } else if (!initialCenterSet && centerPoint != null) {"""
text = text.replace(s_pause, r_pause)

# 10. Add `isScanning` to signature
s_sig = """    currentLocation: Location?,
    zoomInTrigger: Int = 0,
    zoomOutTrigger: Int = 0
) {"""
r_sig = """    currentLocation: Location?,
    isScanning: Boolean = false,
    zoomInTrigger: Int = 0,
    zoomOutTrigger: Int = 0
) {"""
text = text.replace(s_sig, r_sig)

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w", encoding="utf-8") as f:
    f.write(text)
