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

@Composable
fun MapScreen(aps: List<ScannedAp>, centerTrigger: Int, currentLocation: Location?) {
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

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                mapViewState = this
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(18.0)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { mapView ->
            mapView.overlays.clear()

            var centerPoint: GeoPoint? = null
            val wifiIcon = getBitmapDrawable(context, R.drawable.ic_wifi_pin)

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
                    val marker = Marker(mapView).apply {
                        position = point
                        title = circle.title
                        snippet = circle.snippet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        if (wifiIcon != null) {
                            icon = wifiIcon
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

            mapView.invalidate()
        }
    )
}


