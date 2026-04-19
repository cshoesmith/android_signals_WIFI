import os
content = """package com.example.androidsignalswifi

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Marker
import android.graphics.Color
import android.location.Location

import android.graphics.Paint
import org.osmdroid.views.overlay.Overlay

fun getBitmap(context: android.content.Context, id: Int): Bitmap? {
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
}

class UserLocationOverlay(private val location: Location) : Overlay() {
    private val point = GeoPoint(location.latitude, location.longitude)
    private val paintInner = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintOuter = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintBorder = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
    }
    private val animationStart = System.currentTimeMillis()
    private val starPath = android.graphics.Path()

    private fun updateStarPath(centerX: Float, centerY: Float, outerRadius: Float, innerRadius: Float) {
        starPath.reset()
        val numPoints = 5
        val angleIncrement = Math.PI * 2.0 / numPoints
        val halfAngleIncrement = angleIncrement / 2.0
        var currentAngle = -Math.PI / 2.0

        for (i in 0 until numPoints) {
            val outerX = centerX + (Math.cos(currentAngle).toFloat() * outerRadius)
            val outerY = centerY + (Math.sin(currentAngle).toFloat() * outerRadius)
            if (i == 0) {
                starPath.moveTo(outerX, outerY)
            } else {
                starPath.lineTo(outerX, outerY)
            }

            val innerAngle = currentAngle + halfAngleIncrement
            val innerX = centerX + (Math.cos(innerAngle).toFloat() * innerRadius)
            val innerY = centerY + (Math.sin(innerAngle).toFloat() * innerRadius)
            starPath.lineTo(innerX, innerY)

            currentAngle += angleIncrement
        }
        starPath.close()
    }

    override fun draw(c: Canvas, osmv: MapView, shadow: Boolean) {
        if (shadow) return
        val proj = osmv.projection
        val pt = android.graphics.Point()
        proj.toPixels(point, pt)

        val time = System.currentTimeMillis()
        val duration = 2000L
        val progress = ((time - animationStart) % duration) / duration.toFloat()

        // Rainbow cycle over 3 seconds
        val hue = ((time % 3000L) / 3000f) * 360f
        paintInner.color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))

        // Outer pulsing star ring
        val maxRadius = 120f
        val currentRadius = maxRadius * progress
        val alpha = (255 * (1f - progress)).toInt().coerceIn(0, 255)
        paintOuter.color = Color.HSVToColor(alpha, floatArrayOf(hue, 1f, 1f))   

        updateStarPath(pt.x.toFloat(), pt.y.toFloat(), currentRadius, currentRadius * 0.5f)
        c.drawPath(starPath, paintOuter)

        // Inner solid star
        updateStarPath(pt.x.toFloat(), pt.y.toFloat(), 25f, 12f)
        c.drawPath(starPath, paintInner)
        c.drawPath(starPath, paintBorder)

        // Request redraw to animate continuously
        osmv.postInvalidateOnAnimation()
    }
}

@Composable
fun MapScreen(
    aps: List<ScannedAp>,
    bles: List<ScannedBle> = emptyList(),
    cells: List<ScannedCell> = emptyList(),
    centerTrigger: Int,
    currentLocation: Location?,
    isScanning: Boolean = false,
    zoomInTrigger: Int = 0,
    zoomOutTrigger: Int = 0
) {
    val context = LocalContext.current
    Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))

    var mapViewState by remember { mutableStateOf<MapView?>(null) }
    var initialCenterSet by remember { mutableStateOf(false) }
    var openTooltipId by remember { mutableStateOf<String?>(null) }
    val lastManualMoveTime = remember { longArrayOf(0L) }

    LaunchedEffect(centerTrigger) {
        if (centerTrigger > 0) {
            lastManualMoveTime[0] = 0L
            val mv = mapViewState
            if (mv != null) {
                if (currentLocation != null) {
                    mv.controller.animateTo(GeoPoint(currentLocation.latitude, currentLocation.longitude))
                    if (mv.zoomLevelDouble < 18.0) mv.controller.setZoom(18.0)  
                } else if (aps.isNotEmpty()) {
                    mv.controller.animateTo(GeoPoint(aps[0].estLat, aps[0].estLon))
                    if (mv.zoomLevelDouble < 18.0) mv.controller.setZoom(18.0)  
                }
            }
        }
    }

    LaunchedEffect(zoomInTrigger) {
        if (zoomInTrigger > 0) {
            mapViewState?.controller?.zoomIn()
        }
    }

    LaunchedEffect(zoomOutTrigger) {
        if (zoomOutTrigger > 0) {
            mapViewState?.controller?.zoomOut()
        }
    }

    AndroidView(
        factory = { ctx ->
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
        },
        modifier = Modifier.fillMaxSize(),
        update = { mapView ->
            if (System.currentTimeMillis() - lastManualMoveTime[0] < 10000L) {
                return@AndroidView
            }

            org.osmdroid.views.overlay.InfoWindow.closeAllInfoWindowsOn(mapView)
            mapView.overlays.clear()

            var centerPoint: GeoPoint? = null
            val wifiBlueBmp = getBitmap(context, R.drawable.ic_cloud_wifi_blue)
            val wifiLightGreenBmp = getBitmap(context, R.drawable.ic_cloud_wifi_light_green)
            val wifiDarkGreenBmp = getBitmap(context, R.drawable.ic_cloud_wifi_dark_green)
            val bleBmp = getBitmap(context, R.drawable.ic_ble_device)
            val cellBmp = getBitmap(context, R.drawable.ic_cell_tower)
            
            val groupedAps = aps.groupBy { it.bssid.substringBeforeLast(":") }
                .entries
                .sortedBy { entry -> entry.value.maxOfOrNull { it.totalWeight } ?: 0.0 }

            for ((macPrefix, group) in groupedAps) {
                val primaryAp = group.maxByOrNull { it.totalWeight } ?: group[0]
                val point = GeoPoint(primaryAp.estLat, primaryAp.estLon)        

                if (centerPoint == null) centerPoint = point

                val hash = primaryAp.bssid.hashCode()
                val r = (hash and 0xFF0000) shr 16
                val g = (hash and 0x00FF00) shr 8
                val b = (hash and 0x0000FF)

                val radiusMeters = kotlin.math.max(10.0, 2000.0 / kotlin.math.max(10.0, primaryAp.totalWeight))

                val ssids = group.map { if(it.ssid.isNotEmpty()) it.ssid else "[Hidden]" }.distinct().joinToString(", ")
                val isSecured = group.any { it.isSecured }
                val titleText = "Vendor: ${VendorLookup.getVendor(primaryAp.bssid)} (Location Wt: ${group.sumOf { it.totalWeight }.toInt()})"
                val snippetText = group.joinToString("\\n") {
                    val secText = if (it.securityType.isNotEmpty()) it.securityType else if (it.isSecured) "Secured" else "Open"
                    val ssidName = it.ssid.takeIf { s -> s.isNotEmpty() } ?: "Hidden"
                    val freqText = "${it.frequency} MHz"
                    val stdText = it.wifiStandard
                    "[$ssidName] ${it.bssid} - $secText | $freqText | $stdText" 
                }

                val identifier = primaryAp.bssid
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
                if (openTooltipId == identifier && primaryAp.totalWeight <= 200.0) circle.showInfoWindow()

                if (primaryAp.totalWeight > 200.0) {
                    val isWep = group.any { it.securityType.contains("WEP", ignoreCase = true) }

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
                    if (openTooltipId == identifier) marker.showInfoWindow()
                }
            }

            for (ble in bles) {
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

            for (cell in cells) {
                val point = GeoPoint(cell.estLat, cell.estLon)
                if (centerPoint == null) centerPoint = point

                val radiusMeters = kotlin.math.max(50.0, 5000.0 / kotlin.math.max(10.0, cell.totalWeight))

                val identifier = cell.cellId
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
                if (openTooltipId == identifier) marker.showInfoWindow()
            }

            if (isScanning && currentLocation != null) {
                mapView.controller.setCenter(GeoPoint(currentLocation.latitude, currentLocation.longitude))
                initialCenterSet = true
            } else if (!initialCenterSet && centerPoint != null) {
                if (currentLocation != null) {
                    mapView.controller.setCenter(GeoPoint(currentLocation.latitude, currentLocation.longitude))
                } else {
                    mapView.controller.setCenter(centerPoint)
                }
                initialCenterSet = true
            }

            if (currentLocation != null) {
                mapView.overlays.add(UserLocationOverlay(currentLocation))      
            }

            mapView.invalidate()
        }
    )
}"""

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)
