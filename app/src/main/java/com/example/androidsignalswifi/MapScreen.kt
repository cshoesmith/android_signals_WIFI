package com.example.androidsignalswifi

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
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.Marker
import android.graphics.Color
import android.location.Location

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.views.overlay.Overlay

// A geolocated cell-signal reading, quality 0 (weak) to 1 (strong).
data class HeatSample(val lat: Double, val lon: Double, val quality: Float)

private fun lerp(a: Int, b: Int, t: Float) = (a + (b - a) * t).toInt()

// Green = good signal (quality 1), red = poor (quality 0), through yellow.
fun heatColor(quality: Float): Int {
    val q = quality.coerceIn(0f, 1f)
    return if (q < 0.5f) {
        val t = q * 2f // red -> yellow
        Color.rgb(lerp(0xD3, 0xFF, t), lerp(0x2F, 0xC1, t), lerp(0x2F, 0x07, t))
    } else {
        val t = (q - 0.5f) * 2f // yellow -> green
        Color.rgb(lerp(0xFF, 0x2E, t), lerp(0xC1, 0x7D, t), lerp(0x07, 0x32, t))
    }
}

// Cloudy signal heatmap: an additive stack of soft radial blobs, one per sample.
class CellHeatmapOverlay(private val samples: List<HeatSample>) : Overlay() {
    private val paint = Paint().apply { isAntiAlias = true }
    override fun draw(c: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || samples.isEmpty()) return
        val proj = mapView.projection
        val rPx = proj.metersToEquatorPixels(45f).coerceAtLeast(20f)
        val pt = android.graphics.Point()
        val w = mapView.width
        val h = mapView.height
        for (s in samples) {
            proj.toPixels(GeoPoint(s.lat, s.lon), pt)
            if (pt.x < -rPx || pt.y < -rPx || pt.x > w + rPx || pt.y > h + rPx) continue
            val base = heatColor(s.quality)
            val center = Color.argb(120, Color.red(base), Color.green(base), Color.blue(base))
            val edge = Color.argb(0, Color.red(base), Color.green(base), Color.blue(base))
            paint.shader = RadialGradient(pt.x.toFloat(), pt.y.toFloat(), rPx, center, edge, Shader.TileMode.CLAMP)
            c.drawCircle(pt.x.toFloat(), pt.y.toFloat(), rPx, paint)
        }
        paint.shader = null
    }
}

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

// Draw a small lock badge inside a white circle at the icon's lower-right corner.
fun withLockBadge(base: Bitmap, badge: Bitmap): Bitmap {
    val out = base.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)
    val d = base.width * 0.55f
    val cx = base.width - d / 2f
    val cy = base.height - d / 2f
    val circlePaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
    canvas.drawCircle(cx, cy, d / 2f, circlePaint)
    val inset = d * 0.42f
    val dst = RectF(cx - inset, cy - inset, cx + inset, cy + inset)
    canvas.drawBitmap(badge, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
    return out
}

fun lightenBitmap(src: Bitmap, factor: Float = 0.35f): Bitmap {
    val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint()
    val cm = ColorMatrix()
    // Shift RGB channels toward white by factor
    cm.set(floatArrayOf(
        1f - factor, 0f, 0f, 0f, 255f * factor,
        0f, 1f - factor, 0f, 0f, 255f * factor,
        0f, 0f, 1f - factor, 0f, 255f * factor,
        0f, 0f, 0f, 1f, 0f
    ))
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(src, 0f, 0f, paint)
    return result
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
    zoomOutTrigger: Int = 0,
    cellHeatmapMode: Boolean = false,
    heatSamples: List<HeatSample> = emptyList(),
    onBleGroupClick: (groupKey: String, devices: List<ScannedBle>) -> Unit = { _, _ -> },
    onVisibleBoundsChanged: (BoundingBox) -> Unit = {}
) {
    val context = LocalContext.current
    // Load once; this hits SharedPreferences and must not run on every recomposition.
    remember { Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0)) }
    val currentOnVisibleBoundsChanged by rememberUpdatedState(onVisibleBoundsChanged)

    var mapViewState by remember { mutableStateOf<MapView?>(null) }
    var initialCenterSet by remember { mutableStateOf(false) }
    var openTooltipId by remember { mutableStateOf<String?>(null) }
    val lastManualMoveTime = remember { longArrayOf(0L) }

    // Cache drawables so bitmaps are decoded once and one drawable is shared by all markers.
    // Four wifi combinations: green/red for triangulation state, lock badge for security.
    val wifiIcons = remember {
        val green = getBitmap(context, R.drawable.ic_wifi_triangulated)
        val red = getBitmap(context, R.drawable.ic_wifi_learning)
        val lock = getBitmap(context, R.drawable.ic_lock_solid)
        val lockOpen = getBitmap(context, R.drawable.ic_lockopen_solid)
        fun make(base: Bitmap?, badge: Bitmap?): BitmapDrawable? =
            if (base != null && badge != null) BitmapDrawable(context.resources, withLockBadge(base, badge))
            else base?.let { BitmapDrawable(context.resources, it) }
        mapOf(
            "triSecured" to make(green, lock),
            "triOpen" to make(green, lockOpen),
            "learnSecured" to make(red, lock),
            "learnOpen" to make(red, lockOpen)
        )
    }
    val bleBmp = remember {
        val raw = getBitmap(context, R.drawable.ic_ble_device)
        if (raw != null) Bitmap.createScaledBitmap(raw, (raw.width * 1.25f).toInt(), (raw.height * 1.25f).toInt(), true) else null
    }
    val bleIcon = remember { bleBmp?.let { BitmapDrawable(context.resources, it) } }
    // Grouped BLE icon: 10% larger than individual + lighter shade
    val bleGroupIcon = remember {
        if (bleBmp != null) {
            val scaled = Bitmap.createScaledBitmap(bleBmp, (bleBmp.width * 1.1f).toInt(), (bleBmp.height * 1.1f).toInt(), true)
            BitmapDrawable(context.resources, lightenBitmap(scaled))
        } else null
    }
    val cellIcon = remember { getBitmap(context, R.drawable.ic_cell_tower)?.let { BitmapDrawable(context.resources, it) } }
    // Snapshot of what the overlays were last built from, so recompositions caused by
    // unrelated state (bounds reports, timer ticks) skip the expensive rebuild.
    val lastRendered = remember { arrayOfNulls<Any>(7) }

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
                // Track manual moves via an overlay so we don't block marker tap events
                overlays.add(object : Overlay() {
                    override fun onScroll(pEvent1: android.view.MotionEvent?, pEvent2: android.view.MotionEvent?, pDistanceX: Float, pDistanceY: Float, pMapView: MapView?): Boolean {
                        lastManualMoveTime[0] = System.currentTimeMillis()
                        return false
                    }
                    override fun onFling(pEvent1: android.view.MotionEvent?, pEvent2: android.view.MotionEvent?, pVelocityX: Float, pVelocityY: Float, pMapView: MapView?): Boolean {
                        lastManualMoveTime[0] = System.currentTimeMillis()
                        return false
                    }
                })
                // Report the visible map area whenever the user pans or zooms
                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        post { currentOnVisibleBoundsChanged(boundingBox) }
                        return false
                    }
                    override fun onZoom(event: ZoomEvent?): Boolean {
                        post { currentOnVisibleBoundsChanged(boundingBox) }
                        return false
                    }
                })
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { mapView ->
            val locKey = currentLocation?.let { "${it.latitude},${it.longitude}" }
            val dataChanged = lastRendered[0] !== aps || lastRendered[1] !== bles || lastRendered[2] !== cells ||
                lastRendered[3] != openTooltipId || lastRendered[4] != locKey ||
                lastRendered[5] != cellHeatmapMode || lastRendered[6] !== heatSamples
            if (dataChanged) {
            lastRendered[0] = aps
            lastRendered[1] = bles
            lastRendered[2] = cells
            lastRendered[3] = openTooltipId
            lastRendered[4] = locKey
            lastRendered[5] = cellHeatmapMode
            lastRendered[6] = heatSamples

            val manualMoveActive = System.currentTimeMillis() - lastManualMoveTime[0] < 10000L

            org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn(mapView)
            mapView.overlays.clear()

            // Re-add the scroll/fling tracking overlay (cleared above)
            mapView.overlays.add(object : Overlay() {
                override fun onScroll(pEvent1: android.view.MotionEvent?, pEvent2: android.view.MotionEvent?, pDistanceX: Float, pDistanceY: Float, pMapView: MapView?): Boolean {
                    lastManualMoveTime[0] = System.currentTimeMillis()
                    return false
                }
                override fun onFling(pEvent1: android.view.MotionEvent?, pEvent2: android.view.MotionEvent?, pVelocityX: Float, pVelocityY: Float, pMapView: MapView?): Boolean {
                    lastManualMoveTime[0] = System.currentTimeMillis()
                    return false
                }
            })

            var centerPoint: GeoPoint? = null

            if (cellHeatmapMode) {
                mapView.overlays.add(CellHeatmapOverlay(heatSamples))
            } else {

            // Group APs by MAC prefix, then merge groups that share an SSID so mesh nodes
            // and multi-radio routers collapse into one circle. Union-find over prefixes.
            val prefixGroups = aps.groupBy { it.bssid.substringBeforeLast(":") }
            val parent = HashMap<String, String>()
            prefixGroups.keys.forEach { parent[it] = it }
            fun find(k: String): String {
                var r = k
                while (parent[r] != r) r = parent[r]!!
                return r
            }
            val ssidToPrefix = HashMap<String, String>()
            for ((prefix, group) in prefixGroups) {
                for (ap in group) {
                    val ssid = ap.ssid.trim().lowercase()
                    if (ssid.isEmpty()) continue
                    val existing = ssidToPrefix[ssid]
                    if (existing == null) ssidToPrefix[ssid] = prefix
                    else parent[find(prefix)] = find(existing)
                }
            }
            val groupedAps = prefixGroups.entries
                .groupBy { find(it.key) }
                .map { (root, entries) -> root to entries.flatMap { it.value } }
                .sortedBy { (_, group) -> group.maxOfOrNull { it.totalWeight } ?: 0.0 }

            for ((macPrefix, group) in groupedAps) {
                val primaryAp = group.maxByOrNull { it.totalWeight } ?: group[0]
                val point = GeoPoint(primaryAp.estLat, primaryAp.estLon)

                if (centerPoint == null) centerPoint = point

                val titleText = "Vendor: ${VendorLookup.getVendor(primaryAp.bssid)} (Location Wt: ${group.sumOf { it.totalWeight }.toInt()})"
                val snippetText = group.joinToString("\n") {
                    val secText = if (it.securityType.isNotEmpty()) it.securityType else if (it.isSecured) "Secured" else "Open"
                    val ssidName = it.ssid.takeIf { s -> s.isNotEmpty() } ?: "Hidden"
                    val freqText = "${it.frequency} MHz"
                    val stdText = it.wifiStandard
                    val distText = "~${formatDistance(estimateWifiDistanceMeters(it.rssi, it.frequency))}"
                    "[$ssidName] ${it.bssid} - $secText | $freqText | $stdText | $distText"
                } + "\nLast seen: ${formatTimestamp(group.maxOf { it.lastSeen })}"

                val identifier = primaryAp.bssid
                // Icons instead of shaded circles: dense areas stay readable.
                // Green = triangulated, red = still learning; lock badge shows security.
                val isTriangulated = primaryAp.totalWeight > 200.0
                val isSecured = group.any { it.isSecured }
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
                    val key = (if (isTriangulated) "tri" else "learn") + (if (isSecured) "Secured" else "Open")
                    wifiIcons[key]?.let { icon = it }
                }
                mapView.overlays.add(marker)
                if (openTooltipId == identifier) marker.showInfoWindow()
            }

            for (cell in cells) {
                val point = GeoPoint(cell.estLat, cell.estLon)
                if (centerPoint == null) centerPoint = point

                val identifier = cell.cellId
                val marker = Marker(mapView).apply {
                    id = identifier
                    position = point
                    title = "${cell.owner} ${cell.networkType} Cell: ${cell.cellId}"
                    snippet = "MCC/MNC: ${cell.mccMnc}\nLAC/TAC: ${cell.lac}\nPCI: ${cell.pci}\nBand: ${cell.band}\nEst. distance: ~${formatDistance(estimateCellDistanceMeters(cell.rssi))} (rough)\nLocation Wt: ${cell.totalWeight.toInt()}\nLast seen: ${formatTimestamp(cell.lastSeen)}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { m, mv ->
                        openTooltipId = if (openTooltipId == identifier) null else identifier
                        true
                    }
                    if (cellIcon != null) {
                        icon = cellIcon
                    }
                }

                mapView.overlays.add(marker)
                if (openTooltipId == identifier) marker.showInfoWindow()
            }

            // BLE markers added AFTER cells so they render on top and are tappable
            // Group BLE devices by vendor (if known) or device name
            val bleGroups = bles.groupBy { ble ->
                val vendor = VendorLookup.getVendor(ble.mac)
                if (vendor != "Unknown") vendor
                else if (ble.name != "Unknown" && ble.name.isNotEmpty()) ble.name
                else ble.deviceType
            }

            for ((groupKey, group) in bleGroups) {
                // Use the device with highest weight as the representative location
                val primary = group.maxByOrNull { it.totalWeight } ?: group[0]
                val point = GeoPoint(primary.estLat, primary.estLon)
                if (centerPoint == null) centerPoint = point

                val isGroup = group.size > 1
                val identifier = if (isGroup) "ble_group_$groupKey" else primary.mac
                val fixedName = if (primary.name != "Unknown") primary.name else primary.deviceType

                val marker = Marker(mapView).apply {
                    id = identifier
                    position = point
                    if (isGroup) {
                        title = "BLE Group: $groupKey (${group.size} devices)"
                        snippet = "Devices: ${group.size}\nType: ${primary.deviceType}\nBest RSSI: ${group.maxOf { it.rssi }} dBm\nLast seen: ${formatTimestamp(group.maxOf { it.lastSeen })}"
                    } else {
                        title = "BLE: $fixedName"
                        snippet = "MAC: ${primary.mac}\nVendor: ${VendorLookup.getVendor(primary.mac)}\nType: ${primary.deviceType}\nRSSI: ${primary.rssi} dBm\nLocation Wt: ${primary.totalWeight.toInt()}\nLast seen: ${formatTimestamp(primary.lastSeen)}"
                    }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { m, mv ->
                        if (isGroup) {
                            onBleGroupClick(groupKey, group)
                        } else {
                            openTooltipId = if (openTooltipId == identifier) null else identifier
                        }
                        true
                    }
                    val drawable = if (isGroup) bleGroupIcon else bleIcon
                    if (drawable != null) {
                        icon = drawable
                    }
                }
                mapView.overlays.add(marker)
                if (!isGroup && openTooltipId == identifier) marker.showInfoWindow()
            }
            } // end normal (non-heatmap) overlays

            if (!manualMoveActive) {
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
            }

            if (currentLocation != null) {
                mapView.overlays.add(UserLocationOverlay(currentLocation))
            }

            mapView.invalidate()
            mapView.post { currentOnVisibleBoundsChanged(mapView.boundingBox) }
            }
        }
    )
}