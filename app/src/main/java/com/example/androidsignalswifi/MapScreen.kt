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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Marker
import android.graphics.Color
import android.location.Location

import android.graphics.Paint
import org.osmdroid.views.overlay.Overlay

fun getBitmapDrawable(context: android.content.Context, id: Int): BitmapDrawable? {
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
fun MapScreen(aps: List<ScannedAp>, centerTrigger: Int, currentLocation: Location?, zoomInTrigger: Int = 0, zoomOutTrigger: Int = 0) {
    val context = LocalContext.current
    Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))

    var mapViewState by remember { mutableStateOf<MapView?>(null) }
    var initialCenterSet by remember { mutableStateOf(false) }

    LaunchedEffect(centerTrigger) {
        if (centerTrigger > 0) {
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
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { mapView ->
            mapView.overlays.clear()

            var centerPoint: GeoPoint? = null
            val wifiIconBlue = getBitmapDrawable(context, R.drawable.ic_cloud_wifi_blue)
            val wifiIconLightGreen = getBitmapDrawable(context, R.drawable.ic_cloud_wifi_light_green)
            val wifiIconDarkGreen = getBitmapDrawable(context, R.drawable.ic_cloud_wifi_dark_green)

            // Group APs by the first 5 octets of their MAC address (BSSID)
            // e.g. "00:11:22:33:44:55" and "00:11:22:33:44:56" become group "00:11:22:33:44"
            // Then sort them by highest confidence so the smaller/more accurate spots draw on top
            val groupedAps = aps.groupBy { it.bssid.substringBeforeLast(":") }
                .entries
                .sortedBy { entry -> entry.value.maxOfOrNull { it.totalWeight } ?: 0.0 }

            for ((macPrefix, group) in groupedAps) {
                // Use the AP with the highest confidence to anchor the router's physical position
                val primaryAp = group.maxByOrNull { it.totalWeight } ?: group[0]
                val point = GeoPoint(primaryAp.estLat, primaryAp.estLon)
                
                if (centerPoint == null) centerPoint = point

                val hash = primaryAp.bssid.hashCode()
                val r = (hash and 0xFF0000) shr 16
                val g = (hash and 0x00FF00) shr 8
                val b = (hash and 0x0000FF)

                val radiusMeters = kotlin.math.max(10.0, 2000.0 / kotlin.math.max(10.0, primaryAp.totalWeight))

                // Combine SSIDs and statuses for the display
                val ssids = group.map { if(it.ssid.isNotEmpty()) it.ssid else "[Hidden]" }.distinct().joinToString(", ")
                val isSecured = group.any { it.isSecured }
                val securityText = if (isSecured) "Secured" else "Open"

                val titleText = "Vendor: ${VendorLookup.getVendor(primaryAp.bssid)} (Location Wt: ${group.sumOf { it.totalWeight }.toInt()})"
                val snippetText = group.joinToString("\n") {
                    val secText = if (it.securityType.isNotEmpty()) it.securityType else if (it.isSecured) "Secured" else "Open"
                    val ssidName = it.ssid.takeIf { s -> s.isNotEmpty() } ?: "Hidden"
                    val freqText = "${it.frequency} MHz"
                    val stdText = it.wifiStandard
                    "[$ssidName] ${it.bssid} - $secText | $freqText | $stdText" 
                }

                val circle = Polygon(mapView).apply {
                    points = Polygon.pointsAsCircle(point, radiusMeters)
                    fillPaint.color = Color.argb(100, r, g, b)
                    outlinePaint.color = Color.argb(255, r, g, b)
                    outlinePaint.strokeWidth = 3.0f
                    title = titleText
                    snippet = snippetText
                }
                mapView.overlays.add(circle)

                if (primaryAp.totalWeight > 200.0) {
                    val isWep = group.any { it.securityType.contains("WEP", ignoreCase = true) }

                    val marker = Marker(mapView).apply {
                        position = point
                        title = circle.title
                        snippet = circle.snippet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        
                        val selectedIcon = if (!isSecured) {
                            wifiIconLightGreen
                        } else if (isWep) {
                            wifiIconDarkGreen
                        } else {
                            wifiIconBlue
                        }

                        if (selectedIcon != null) {
                            icon = selectedIcon
                        }
                    }
                    mapView.overlays.add(marker)
                }
            }

            // Only set center on the VERY FIRST load
            if (!initialCenterSet && centerPoint != null) {
                if (currentLocation != null) {
                    mapView.controller.setCenter(GeoPoint(currentLocation.latitude, currentLocation.longitude))
                } else {
                    mapView.controller.setCenter(centerPoint)
                }
                initialCenterSet = true
            }

            // Topmost user location beacon
            if (currentLocation != null) {
                mapView.overlays.add(UserLocationOverlay(currentLocation))
            }

            mapView.invalidate()
        }
    )
}


